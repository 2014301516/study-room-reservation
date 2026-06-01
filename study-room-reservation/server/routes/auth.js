const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const pool = require('../config/db');
const { authRequired, JWT_SECRET } = require('../middleware/auth');

const router = express.Router();

// 注册
router.post('/register', async (req, res) => {
  try {
    const { username, password, real_name, student_id, phone, email } = req.body;
    if (!username || !password || !real_name || !student_id) {
      return res.json({ code: 400, message: '请填写必填字段' });
    }
    const [exist] = await pool.query('SELECT id FROM users WHERE username = ? OR student_id = ?', [username, student_id]);
    if (exist.length > 0) {
      return res.json({ code: 400, message: '用户名或学号已存在' });
    }
    const hashed = await bcrypt.hash(password, 10);
    await pool.query(
      'INSERT INTO users (username, password, real_name, student_id, phone, email) VALUES (?,?,?,?,?,?)',
      [username, hashed, real_name, student_id, phone || '', email || '']
    );
    res.json({ code: 200, message: '注册成功' });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误: ' + err.message });
  }
});

// 登录
router.post('/login', async (req, res) => {
  try {
    const { username, password } = req.body;
    const [rows] = await pool.query('SELECT * FROM users WHERE username = ?', [username]);
    if (rows.length === 0) {
      return res.json({ code: 400, message: '用户名或密码错误' });
    }
    const user = rows[0];
    if (user.status === 'banned') {
      return res.json({ code: 403, message: '账号已被封禁，请联系管理员' });
    }
    const match = await bcrypt.compare(password, user.password);
    if (!match) {
      return res.json({ code: 400, message: '用户名或密码错误' });
    }
    const token = jwt.sign(
      { id: user.id, username: user.username, role: user.role, real_name: user.real_name },
      JWT_SECRET, { expiresIn: '7d' }
    );
    res.json({
      code: 200,
      data: {
        token,
        user: { id: user.id, username: user.username, role: user.role, real_name: user.real_name, student_id: user.student_id, avatar: user.avatar }
      }
    });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误: ' + err.message });
  }
});

// 获取个人信息
router.get('/profile', authRequired, async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT id, username, real_name, student_id, phone, email, role, avatar, status, created_at FROM users WHERE id = ?', [req.user.id]);
    if (rows.length === 0) return res.json({ code: 404, message: '用户不存在' });
    res.json({ code: 200, data: rows[0] });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 更新个人信息
router.put('/profile', authRequired, async (req, res) => {
  try {
    const { phone, email } = req.body;
    await pool.query('UPDATE users SET phone = ?, email = ? WHERE id = ?', [phone || '', email || '', req.user.id]);
    res.json({ code: 200, message: '更新成功' });
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

module.exports = router;
