const express = require('express');
const pool = require('../config/db');
const { authRequired } = require('../middleware/auth');

const router = express.Router();

// 扫码签到（通过二维码token）
router.post('/scan', authRequired, async (req, res) => {
  const conn = await pool.getConnection();
  try {
    const { token } = req.body;
    if (!token) return res.json({ code: 400, message: '缺少二维码信息' });

    let reservationId, qrcodeToken;
    try {
      const data = JSON.parse(token);
      reservationId = data.reservation_id;
      qrcodeToken = data.token;
    } catch {
      return res.json({ code: 400, message: '二维码格式无效' });
    }

    const [rows] = await conn.query(
      'SELECT * FROM reservations WHERE id = ? AND qrcode_token = ?',
      [reservationId, qrcodeToken]
    );
    if (rows.length === 0) return res.json({ code: 404, message: '无效的二维码' });

    const r = rows[0];
    if (r.status !== 'reserved') {
      return res.json({ code: 400, message: r.status === 'completed' ? '该预约已完成' : r.status === 'cancelled' ? '该预约已取消' : '已签到，请勿重复操作' });
    }

    // 检查签到时间是否合理（预约开始前30分钟到开始后30分钟）
    const now = new Date();
    const [startH, startM] = r.start_time.split(':').map(Number);
    const slotStart = new Date(r.date); slotStart.setHours(startH, startM, 0);
    const diffMin = (now - slotStart) / 60000;

    let isLate = false;
    if (diffMin > 30) isLate = true; // 迟到30分钟以上

    await conn.query('UPDATE reservations SET status = "checked_in" WHERE id = ?', [r.id]);
    await conn.query('UPDATE seats SET current_status = "occupied" WHERE id = ?', [r.seat_id]);
    await conn.query('INSERT INTO checkins (reservation_id, user_id, seat_id, checkin_time) VALUES (?,?,?,NOW())',
      [r.id, r.user_id, r.seat_id]);

    if (isLate) {
      await conn.query(
        'INSERT INTO violations (user_id, reservation_id, type, description) VALUES (?,?,?,?)',
        [r.user_id, r.id, 'late', `预约 ${r.date} ${r.start_time} 迟到签到`]
      );
    }

    conn.release();
    res.json({
      code: 200,
      message: '签到成功，请对号入座' + (isLate ? '（迟到已记录）' : ''),
      data: { seat_number: r.seat_number, is_late: isLate }
    });
  } catch (err) {
    conn.release();
    res.status(500).json({ code: 500, message: '服务器错误: ' + err.message });
  }
});

module.exports = router;
