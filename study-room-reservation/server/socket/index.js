/**
 * WebSocket 实时通信模块
 * 推送座位状态变更，实现多位用户同时查看座位图时实时更新
 */

function initSocket(io) {
  // 在线用户统计
  let onlineUsers = 0;

  io.on('connection', (socket) => {
    onlineUsers++;
    console.log(`[WebSocket] 用户连接 (在线: ${onlineUsers})`);

    // 加入座位监控房间
    socket.on('watch-area', (areaId) => {
      socket.join(`area-${areaId}`);
      console.log(`[WebSocket] 用户加入区域 ${areaId} 监控`);
    });

    // 离开座位监控
    socket.on('unwatch-area', (areaId) => {
      socket.leave(`area-${areaId}`);
    });

    // 座位状态变更广播
    socket.on('seat-status-change', (data) => {
      // data: { area_id, seat_id, seat_number, old_status, new_status }
      io.to(`area-${data.area_id}`).emit('seat-updated', data);
      // 同时广播给全局（供统计看板使用）
      io.emit('global-seat-update', data);
    });

    socket.on('disconnect', () => {
      onlineUsers--;
      console.log(`[WebSocket] 用户断开 (在线: ${onlineUsers})`);
    });
  });

  console.log('[WebSocket] 初始化完成');
}

module.exports = initSocket;
