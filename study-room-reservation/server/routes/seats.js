const express = require('express');
const pool = require('../config/db');
const { authRequired } = require('../middleware/auth');

const router = express.Router();

// 获取所有区域
router.get('/areas', authRequired, async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT * FROM areas WHERE status = "active" ORDER BY sort_order');
    res.json({ code: 200, data: rows });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 获取某区域的所有座位（含实时状态统计）
router.get('/area/:areaId', authRequired, async (req, res) => {
  try {
    const { areaId } = req.params;
    const [area] = await pool.query('SELECT * FROM areas WHERE id = ?', [areaId]);
    if (area.length === 0) return res.json({ code: 404, message: '区域不存在' });

    const [seats] = await pool.query(
      'SELECT * FROM seats WHERE area_id = ? ORDER BY row_num, col_num', [areaId]
    );

    // 统计
    const stats = { total: seats.length, available: 0, reserved: 0, occupied: 0, temp_leave: 0, maintenance: 0 };
    seats.forEach(s => { stats[s.current_status] = (stats[s.current_status] || 0) + 1; });

    res.json({ code: 200, data: { area: area[0], seats, stats } });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 获取单个座位详情
router.get('/:id', authRequired, async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT s.*, a.name as area_name, a.building, a.floor
       FROM seats s JOIN areas a ON s.area_id = a.id WHERE s.id = ?`, [req.params.id]
    );
    if (rows.length === 0) return res.json({ code: 404, message: '座位不存在' });
    res.json({ code: 200, data: rows[0] });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 获取全馆座位统计概览
router.get('/stats/overview', authRequired, async (req, res) => {
  try {
    const [total] = await pool.query('SELECT COUNT(*) as cnt FROM seats');
    const [byStatus] = await pool.query('SELECT current_status, COUNT(*) as cnt FROM seats GROUP BY current_status');
    const [byArea] = await pool.query(
      'SELECT a.name, a.building, a.floor, COUNT(s.id) as total FROM areas a LEFT JOIN seats s ON s.area_id = a.id WHERE a.status="active" GROUP BY a.id ORDER BY a.sort_order'
    );
    const statusMap = {};
    byStatus.forEach(r => { statusMap[r.current_status] = r.cnt; });
    res.json({ code: 200, data: { total: total[0].cnt, byStatus: statusMap, byArea } });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

module.exports = router;
