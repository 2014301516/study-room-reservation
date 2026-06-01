const express = require('express');
const { v4: uuidv4 } = require('uuid');
const QRCode = require('qrcode');
const pool = require('../config/db');
const { authRequired } = require('../middleware/auth');

const router = express.Router();

// 获取我的预约列表
router.get('/my', authRequired, async (req, res) => {
  try {
    const { status, page = 1, pageSize = 10 } = req.query;
    let sql = `SELECT r.*, s.seat_number, s.row_num, s.col_num, a.name as area_name, a.building, a.floor
               FROM reservations r
               JOIN seats s ON r.seat_id = s.id
               JOIN areas a ON s.area_id = a.id
               WHERE r.user_id = ?`;
    const params = [req.user.id];
    if (status) { sql += ' AND r.status = ?'; params.push(status); }
    sql += ' ORDER BY r.date DESC, r.start_time DESC LIMIT ? OFFSET ?';
    params.push(Number(pageSize), (Number(page) - 1) * Number(pageSize));

    const [rows] = await pool.query(sql, params);
    const [[{ total }]] = await pool.query(
      `SELECT COUNT(*) as total FROM reservations WHERE user_id = ?${status ? ' AND status = ?' : ''}`,
      status ? [req.user.id, status] : [req.user.id]
    );
    res.json({ code: 200, data: { list: rows, total, page: Number(page), pageSize: Number(pageSize) } });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误: ' + err.message });
  }
});

// 检查座位在指定时间段是否可预约
router.get('/check-available/:seatId', authRequired, async (req, res) => {
  try {
    const { seatId } = req.params;
    const { date } = req.query;
    const [conflicts] = await pool.query(
      `SELECT start_time, end_time FROM reservations
       WHERE seat_id = ? AND date = ? AND status NOT IN ('cancelled','completed','absent')`,
      [seatId, date || new Date().toISOString().split('T')[0]]
    );
    // 所有可选时间段 08:00-22:00，每小时一段
    const allSlots = [];
    for (let h = 8; h < 22; h++) {
      const start = String(h).padStart(2, '0') + ':00';
      const end = String(h + 1).padStart(2, '0') + ':00';
      const booked = conflicts.some(c => c.start_time < end && c.end_time > start);
      allSlots.push({ start, end, available: !booked });
    }
    res.json({ code: 200, data: allSlots });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 创建预约
router.post('/', authRequired, async (req, res) => {
  const conn = await pool.getConnection();
  try {
    const { seat_id, date, start_time, end_time } = req.body;
    if (!seat_id || !date || !start_time || !end_time) {
      return res.json({ code: 400, message: '参数不完整' });
    }

    // 检查是否在黑名单
    const [[{ cnt }]] = await conn.query(
      `SELECT COUNT(*) as cnt FROM violations
       WHERE user_id = ? AND status = 'active' AND penalty_end >= CURDATE()`, [req.user.id]
    );
    if (cnt > 0) {
      conn.release();
      return res.json({ code: 403, message: '您当前处于违规处罚期，无法预约' });
    }

    // 检查座位是否存在且可用
    const [seatRows] = await conn.query('SELECT * FROM seats WHERE id = ?', [seat_id]);
    if (seatRows.length === 0) { conn.release(); return res.json({ code: 404, message: '座位不存在' }); }
    if (seatRows[0].current_status === 'maintenance') {
      conn.release(); return res.json({ code: 400, message: '该座位正在维护中' });
    }

    // 检查时间冲突
    const [conflicts] = await conn.query(
      `SELECT id FROM reservations WHERE seat_id = ? AND date = ? AND status NOT IN ('cancelled','completed','absent')
       AND start_time < ? AND end_time > ?`, [seat_id, date, end_time, start_time]
    );
    if (conflicts.length > 0) {
      conn.release(); return res.json({ code: 400, message: '该时间段已被他人预约' });
    }

    // 检查用户是否在同一时间段已有预约
    const [myConflicts] = await conn.query(
      `SELECT id FROM reservations WHERE user_id = ? AND date = ? AND status NOT IN ('cancelled','completed','absent')
       AND start_time < ? AND end_time > ?`, [req.user.id, date, end_time, start_time]
    );
    if (myConflicts.length > 0) {
      conn.release(); return res.json({ code: 400, message: '您在该时间段已有其他预约' });
    }

    const qrcode_token = uuidv4();
    const [result] = await conn.query(
      'INSERT INTO reservations (user_id, seat_id, date, start_time, end_time, qrcode_token) VALUES (?,?,?,?,?,?)',
      [req.user.id, seat_id, date, start_time, end_time, qrcode_token]
    );

    // 更新座位状态为已预约
    await conn.query('UPDATE seats SET current_status = "reserved" WHERE id = ? AND current_status = "available"', [seat_id]);

    conn.release();

    // 获取预约详情
    const [detail] = await pool.query(
      `SELECT r.*, s.seat_number, s.row_num, s.col_num, a.name as area_name, a.building, a.floor
       FROM reservations r JOIN seats s ON r.seat_id = s.id JOIN areas a ON s.area_id = a.id
       WHERE r.id = ?`, [result.insertId]
    );

    // 生成QR码
    const qrDataUrl = await QRCode.toDataURL(
      JSON.stringify({ reservation_id: result.insertId, token: qrcode_token, seat: detail[0].seat_number, date, time: `${start_time}-${end_time}` })
    );

    res.json({ code: 200, data: { ...detail[0], qrcode_data_url: qrDataUrl }, message: '预约成功' });
  } catch (err) {
    conn.release();
    res.status(500).json({ code: 500, message: '服务器错误: ' + err.message });
  }
});

// 取消预约
router.put('/:id/cancel', authRequired, async (req, res) => {
  const conn = await pool.getConnection();
  try {
    const [rows] = await conn.query('SELECT * FROM reservations WHERE id = ? AND user_id = ?', [req.params.id, req.user.id]);
    if (rows.length === 0) { conn.release(); return res.json({ code: 404, message: '预约不存在' }); }
    if (!['reserved'].includes(rows[0].status)) {
      conn.release(); return res.json({ code: 400, message: '当前状态不可取消' });
    }
    await conn.query('UPDATE reservations SET status = "cancelled" WHERE id = ?', [req.params.id]);
    // 释放座位
    await conn.query('UPDATE seats SET current_status = "available" WHERE id = ?', [rows[0].seat_id]);
    conn.release();
    res.json({ code: 200, message: '已取消预约' });
  } catch (err) {
    conn.release();
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 签到
router.post('/:id/checkin', authRequired, async (req, res) => {
  const conn = await pool.getConnection();
  try {
    const [rows] = await conn.query('SELECT * FROM reservations WHERE id = ? AND user_id = ?', [req.params.id, req.user.id]);
    if (rows.length === 0) { conn.release(); return res.json({ code: 404, message: '预约不存在' }); }
    const r = rows[0];
    if (!['reserved'].includes(r.status)) {
      conn.release(); return res.json({ code: 400, message: '当前状态不可签到' });
    }
    await conn.query('UPDATE reservations SET status = "checked_in" WHERE id = ?', [req.params.id]);
    await conn.query('UPDATE seats SET current_status = "occupied" WHERE id = ?', [r.seat_id]);
    await conn.query('INSERT INTO checkins (reservation_id, user_id, seat_id, checkin_time) VALUES (?,?,?,NOW())',
      [r.id, req.user.id, r.seat_id]);
    conn.release();
    res.json({ code: 200, message: '签到成功，请就座' });
  } catch (err) {
    conn.release();
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 暂离
router.put('/:id/leave', authRequired, async (req, res) => {
  const conn = await pool.getConnection();
  try {
    const [rows] = await conn.query('SELECT * FROM reservations WHERE id = ? AND user_id = ?', [req.params.id, req.user.id]);
    if (rows.length === 0) { conn.release(); return res.json({ code: 404, message: '预约不存在' }); }
    if (!['checked_in'].includes(rows[0].status)) {
      conn.release(); return res.json({ code: 400, message: '仅在已签到状态可暂离' });
    }
    await conn.query('UPDATE reservations SET status = "temp_leave" WHERE id = ?', [req.params.id]);
    await conn.query('UPDATE seats SET current_status = "temp_leave" WHERE id = ?', [rows[0].seat_id]);
    conn.release();
    res.json({ code: 200, message: '已暂离，座位为您保留15分钟' });
  } catch (err) {
    conn.release();
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 签退
router.post('/:id/checkout', authRequired, async (req, res) => {
  const conn = await pool.getConnection();
  try {
    const [rows] = await conn.query('SELECT * FROM reservations WHERE id = ? AND user_id = ?', [req.params.id, req.user.id]);
    if (rows.length === 0) { conn.release(); return res.json({ code: 404, message: '预约不存在' }); }
    if (!['checked_in', 'temp_leave'].includes(rows[0].status)) {
      conn.release(); return res.json({ code: 400, message: '当前状态不可签退' });
    }
    await conn.query('UPDATE reservations SET status = "completed" WHERE id = ?', [req.params.id]);
    await conn.query('UPDATE seats SET current_status = "available" WHERE id = ?', [rows[0].seat_id]);
    await conn.query('UPDATE checkins SET checkout_time = NOW() WHERE reservation_id = ? AND checkout_time IS NULL',
      [req.params.id]);
    conn.release();
    res.json({ code: 200, message: '签退成功，感谢使用' });
  } catch (err) {
    conn.release();
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 获取预约二维码
router.get('/:id/qrcode', authRequired, async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT r.*, s.seat_number, a.name as area_name FROM reservations r
       JOIN seats s ON r.seat_id = s.id JOIN areas a ON s.area_id = a.id
       WHERE r.id = ? AND r.user_id = ?`, [req.params.id, req.user.id]
    );
    if (rows.length === 0) return res.json({ code: 404, message: '预约不存在' });
    const r = rows[0];
    const qrData = JSON.stringify({ reservation_id: r.id, token: r.qrcode_token, seat: r.seat_number, date: r.date, time: `${r.start_time}-${r.end_time}` });
    const qrDataUrl = await QRCode.toDataURL(qrData);
    res.json({ code: 200, data: { reservation_id: r.id, qrcode_data_url: qrDataUrl } });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

module.exports = router;
