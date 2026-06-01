const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');
require('dotenv').config();

async function run() {
  const conn = await mysql.createConnection({
    host: process.env.DB_HOST || 'localhost',
    port: process.env.DB_PORT || 3306,
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '123',
    multipleStatements: true
  });

  try {
    const sql = fs.readFileSync(path.join(__dirname, '..', 'database', 'schema.sql'), 'utf8');
    // Remove USE and CREATE DATABASE since we connect directly
    const cleaned = sql
      .replace(/CREATE DATABASE[^;]*;/gi, '')
      .replace(/USE study_room[^;]*;/gi, '');

    await conn.query(`CREATE DATABASE IF NOT EXISTS study_room DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`);
    await conn.query(`USE study_room`);
    await conn.query(cleaned);
    console.log('✅ 数据库初始化成功！');
  } catch (err) {
    console.error('❌ 导入失败:', err.message);
    // Try statement by statement
    console.log('尝试逐条执行...');
    const sql = fs.readFileSync(path.join(__dirname, '..', 'database', 'schema.sql'), 'utf8')
      .replace(/CREATE DATABASE[^;]*;/gi, '')
      .replace(/USE study_room[^;]*;/gi, '');

    await conn.query(`CREATE DATABASE IF NOT EXISTS study_room DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`);
    await conn.query(`USE study_room`);

    const statements = sql.split(';').filter(s => s.trim());
    for (let i = 0; i < statements.length; i++) {
      const stmt = statements[i].trim();
      if (!stmt) continue;
      try {
        await conn.query(stmt);
      } catch (e) {
        console.log(`  ⚠ 第${i + 1}条: ${e.message} (${stmt.slice(0, 80)}...)`);
      }
    }
    console.log('✅ 逐条导入完成');
  }

  // Verify
  const [tables] = await conn.query(`SHOW TABLES`);
  console.log('📋 数据表:', tables.map(t => Object.values(t)[0]).join(', '));

  const [[{cnt}]] = await conn.query(`SELECT COUNT(*) as cnt FROM seats`);
  console.log(`🪑 座位数据: ${cnt} 条`);

  await conn.end();
}

run();
