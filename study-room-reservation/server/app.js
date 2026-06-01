require('dotenv').config();
const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const initSocket = require('./socket');

// 路由
const authRoutes = require('./routes/auth');
const seatRoutes = require('./routes/seats');
const reservationRoutes = require('./routes/reservations');
const adminRoutes = require('./routes/admin');
const checkinRoutes = require('./routes/checkin');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: { origin: '*', methods: ['GET', 'POST'] }
});

// 中间件
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// 静态资源
app.use('/uploads', express.static('uploads'));

// 路由挂载
app.use('/api/auth', authRoutes);
app.use('/api/seats', seatRoutes);
app.use('/api/reservations', reservationRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/checkin', checkinRoutes);

// 健康检查
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', time: new Date().toISOString() });
});

// 404
app.use((req, res) => {
  res.status(404).json({ code: 404, message: '接口不存在' });
});

// 全局错误处理
app.use((err, req, res, next) => {
  console.error('[Error]', err.stack);
  res.status(500).json({ code: 500, message: '服务器内部错误' });
});

// WebSocket 初始化
initSocket(io);

// 启动
const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
  console.log(`\n  🏫 校园自习室预约系统 - 服务端`);
  console.log(`  📡 运行端口: http://localhost:${PORT}`);
  console.log(`  🔌 WebSocket: ws://localhost:${PORT}\n`);
});
