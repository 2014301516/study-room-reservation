const express = require('express');
const pool = require('../config/db');
const { adminRequired } = require('../middleware/auth');

const router = express.Router();

// ========== 数据看板 ==========

router.get('/dashboard', adminRequired, async (req, res) => {
  try {
    const today = new Date().toISOString().split('T')[0];
    // 今日统计
    const [[todayStats]] = await pool.query(
      `SELECT COUNT(*) as total_reservations,
              SUM(CASE WHEN status IN ('checked_in','using','temp_leave') THEN 1 ELSE 0 END) as active_users,
              SUM(CASE WHEN status = 'absent' THEN 1 ELSE 0 END) as no_shows
       FROM reservations WHERE date = ?`, [today]
    );
    // 座位总体利用率
    const [[seatStats]] = await pool.query(
      `SELECT COUNT(*) as total,
              SUM(CASE WHEN current_status = 'occupied' THEN 1 ELSE 0 END) as occupied,
              SUM(CASE WHEN current_status IN ('reserved','temp_leave') THEN 1 ELSE 0 END) as reserved
       FROM seats WHERE current_status != 'maintenance'`
    );
    const utilization = seatStats.total > 0 ? Math.round((seatStats.occupied + seatStats.reserved) / seatStats.total * 100) : 0;

    // 各区域使用情况
    const [areaUsage] = await pool.query(
      `SELECT a.name, a.building, a.floor,
              COUNT(s.id) as total,
              SUM(CASE WHEN s.current_status IN ('reserved','occupied','temp_leave') THEN 1 ELSE 0 END) as used
       FROM areas a LEFT JOIN seats s ON s.area_id = a.id
       WHERE a.status = 'active' GROUP BY a.id ORDER BY a.sort_order`
    );

    // 近7天预约趋势
    const [trend] = await pool.query(
      `SELECT date, COUNT(*) as cnt FROM reservations
       WHERE date >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) GROUP BY date ORDER BY date`
    );

    // 今日时段分布
    const [hourlyData] = await pool.query(
      `SELECT start_time, COUNT(*) as cnt FROM reservations
       WHERE date = ? GROUP BY start_time ORDER BY start_time`, [today]
    );

    // 近日违规数
    const [[violationStats]] = await pool.query(
      `SELECT COUNT(*) as total, SUM(CASE WHEN status = 'active' THEN 1 ELSE 0 END) as active FROM violations`
    );

    res.json({
      code: 200, data: {
        today: { ...todayStats, utilization },
        areaUsage,
        trend,
        hourly: hourlyData,
        violations: violationStats
      }
    });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误: ' + err.message });
  }
});

// ========== 座位管理 ==========

router.get('/seats', adminRequired, async (req, res) => {
  try {
    const { area_id } = req.query;
    let sql = `SELECT s.*, a.name as area_name, a.building, a.floor FROM seats s JOIN areas a ON s.area_id = a.id`;
    const params = [];
    if (area_id) { sql += ' WHERE s.area_id = ?'; params.push(area_id); }
    sql += ' ORDER BY a.sort_order, s.row_num, s.col_num';
    const [rows] = await pool.query(sql, params);
    res.json({ code: 200, data: rows });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

router.post('/seats', adminRequired, async (req, res) => {
  try {
    const { area_id, seat_number, row_num, col_num, has_outlet, has_lamp, is_window } = req.body;
    await pool.query(
      'INSERT INTO seats (area_id, seat_number, row_num, col_num, has_outlet, has_lamp, is_window) VALUES (?,?,?,?,?,?,?)',
      [area_id, seat_number, row_num || 0, col_num || 0, has_outlet || 0, has_lamp || 0, is_window || 0]
    );
    res.json({ code: 200, message: '座位添加成功' });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

router.put('/seats/:id', adminRequired, async (req, res) => {
  try {
    const { seat_number, row_num, col_num, has_outlet, has_lamp, is_window, current_status } = req.body;
    await pool.query(
      'UPDATE seats SET seat_number=?, row_num=?, col_num=?, has_outlet=?, has_lamp=?, is_window=?, current_status=? WHERE id=?',
      [seat_number, row_num, col_num, has_outlet || 0, has_lamp || 0, is_window || 0, current_status, req.params.id]
    );
    res.json({ code: 200, message: '更新成功' });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

router.delete('/seats/:id', adminRequired, async (req, res) => {
  try {
    await pool.query('DELETE FROM seats WHERE id = ?', [req.params.id]);
    res.json({ code: 200, message: '座位已删除' });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// ========== 预约管理 ==========

router.get('/reservations', adminRequired, async (req, res) => {
  try {
    const { status, date, page = 1, pageSize = 15 } = req.query;
    let sql = `SELECT r.*, s.seat_number, a.name as area_name, u.real_name, u.student_id
               FROM reservations r
               JOIN seats s ON r.seat_id = s.id
               JOIN areas a ON s.area_id = a.id
               JOIN users u ON r.user_id = u.id WHERE 1=1`;
    const params = [];
    if (status) { sql += ' AND r.status = ?'; params.push(status); }
    if (date) { sql += ' AND r.date = ?'; params.push(date); }
    sql += ' ORDER BY r.date DESC, r.start_time DESC LIMIT ? OFFSET ?';
    params.push(Number(pageSize), (Number(page) - 1) * Number(pageSize));

    const [rows] = await pool.query(sql, params);
    const [[{ total }]] = await pool.query(
      `SELECT COUNT(*) as total FROM reservations WHERE 1=1${status ? ' AND status = ?' : ''}${date ? ' AND date = ?' : ''}`,
      [...(status ? [status] : []), ...(date ? [date] : [])]
    );
    res.json({ code: 200, data: { list: rows, total, page: Number(page), pageSize: Number(pageSize) } });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// ========== 用户管理 ==========

router.get('/users', adminRequired, async (req, res) => {
  try {
    const { role, page = 1, pageSize = 15 } = req.query;
    let sql = 'SELECT id, username, real_name, student_id, phone, email, role, status, created_at FROM users WHERE 1=1';
    const params = [];
    if (role) { sql += ' AND role = ?'; params.push(role); }
    sql += ' ORDER BY created_at DESC LIMIT ? OFFSET ?';
    params.push(Number(pageSize), (Number(page) - 1) * Number(pageSize));
    const [rows] = await pool.query(sql, params);
    const [[{ total }]] = await pool.query(
      `SELECT COUNT(*) as total FROM users WHERE 1=1${role ? ' AND role = ?' : ''}`,
      role ? [role] : []
    );
    res.json({ code: 200, data: { list: rows, total, page: Number(page), pageSize: Number(pageSize) } });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

router.put('/users/:id', adminRequired, async (req, res) => {
  try {
    const { status } = req.body;
    await pool.query('UPDATE users SET status = ? WHERE id = ?', [status, req.params.id]);
    res.json({ code: 200, message: '更新成功' });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// ========== 违规管理 ==========

router.get('/violations', adminRequired, async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT v.*, u.real_name, u.student_id FROM violations v
       JOIN users u ON v.user_id = u.id ORDER BY v.created_at DESC LIMIT 100`
    );
    res.json({ code: 200, data: rows });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

router.post('/violations', adminRequired, async (req, res) => {
  try {
    const { user_id, reservation_id, type, description, penalty_end } = req.body;
    await pool.query(
      'INSERT INTO violations (user_id, reservation_id, type, description, penalty_end) VALUES (?,?,?,?,?)',
      [user_id, reservation_id || null, type, description || '', penalty_end || null]
    );
    // 自动检查：3次活跃违规 → 封禁7天
    const [[{ cnt }]] = await pool.query(
      'SELECT COUNT(*) as cnt FROM violations WHERE user_id = ? AND status = "active"', [user_id]
    );
    if (cnt >= 3) {
      const banEnd = new Date(); banEnd.setDate(banEnd.getDate() + 7);
      await pool.query('UPDATE users SET status = "banned" WHERE id = ?', [user_id]);
      await pool.query(
        'INSERT INTO violations (user_id, type, description, penalty_end) VALUES (?, "misconduct", ?, ?)',
        [user_id, '累计违规3次，系统自动封禁7天', banEnd.toISOString().split('T')[0]]
      );
    }
    res.json({ code: 200, message: '违规记录已添加' });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

router.put('/violations/:id', adminRequired, async (req, res) => {
  try {
    const { status } = req.body;
    await pool.query('UPDATE violations SET status = ? WHERE id = ?', [status, req.params.id]);
    res.json({ code: 200, message: '更新成功' });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// ========== 公告管理 ==========

router.get('/notices', adminRequired, async (req, res) => {
  const [rows] = await pool.query('SELECT * FROM notices ORDER BY is_top DESC, created_at DESC');
  res.json({ code: 200, data: rows });
});

router.post('/notices', adminRequired, async (req, res) => {
  const { title, content, is_top } = req.body;
  await pool.query('INSERT INTO notices (title, content, publisher, is_top) VALUES (?,?,?,?)',
    [title, content, '管理员', is_top || 0]);
  res.json({ code: 200, message: '公告发布成功' });
});

router.delete('/notices/:id', adminRequired, async (req, res) => {
  await pool.query('DELETE FROM notices WHERE id = ?', [req.params.id]);
  res.json({ code: 200, message: '公告已删除' });
});

module.exports = router;
