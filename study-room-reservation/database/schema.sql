-- ============================================
-- 校园自习室预约系统 - 数据库设计
-- ============================================

CREATE DATABASE IF NOT EXISTS study_room DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE study_room;

-- 1. 用户表
CREATE TABLE users (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  username    VARCHAR(50)  NOT NULL UNIQUE,
  password    VARCHAR(255) NOT NULL,
  real_name   VARCHAR(50)  NOT NULL,
  student_id  VARCHAR(20)  NOT NULL UNIQUE,
  phone       VARCHAR(20),
  email       VARCHAR(100),
  role        ENUM('admin','student') DEFAULT 'student',
  avatar      VARCHAR(255) DEFAULT '',
  status      ENUM('active','banned') DEFAULT 'active',
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. 区域表（楼栋/楼层/区域）
CREATE TABLE areas (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  building    VARCHAR(50)  NOT NULL,
  floor       INT          NOT NULL,
  description VARCHAR(255),
  sort_order  INT DEFAULT 0,
  status      ENUM('active','inactive') DEFAULT 'active',
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 3. 座位表
CREATE TABLE seats (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  area_id       INT NOT NULL,
  seat_number   VARCHAR(20) NOT NULL,
  row_num       INT DEFAULT 0,
  col_num       INT DEFAULT 0,
  has_outlet    TINYINT(1) DEFAULT 0,
  has_lamp      TINYINT(1) DEFAULT 0,
  is_window     TINYINT(1) DEFAULT 0,
  current_status ENUM('available','reserved','occupied','temp_leave','maintenance') DEFAULT 'available',
  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (area_id) REFERENCES areas(id),
  UNIQUE KEY uk_area_seat (area_id, seat_number)
) ENGINE=InnoDB;

-- 4. 预约表
CREATE TABLE reservations (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  user_id       INT NOT NULL,
  seat_id       INT NOT NULL,
  date          DATE NOT NULL,
  start_time    ENUM('08:00','09:00','10:00','11:00','12:00','13:00','14:00','15:00','16:00','17:00','18:00','19:00','20:00','21:00') NOT NULL,
  end_time      ENUM('09:00','10:00','11:00','12:00','13:00','14:00','15:00','16:00','17:00','18:00','19:00','20:00','21:00','22:00') NOT NULL,
  status        ENUM('reserved','checked_in','using','temp_leave','completed','cancelled','absent') DEFAULT 'reserved',
  qrcode_token  VARCHAR(100),
  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (seat_id) REFERENCES seats(id)
) ENGINE=InnoDB;

-- 5. 签到表
CREATE TABLE checkins (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  reservation_id  INT NOT NULL,
  user_id         INT NOT NULL,
  seat_id         INT NOT NULL,
  checkin_time    DATETIME,
  checkout_time   DATETIME,
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (reservation_id) REFERENCES reservations(id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (seat_id) REFERENCES seats(id)
) ENGINE=InnoDB;

-- 6. 违规记录表
CREATE TABLE violations (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  user_id         INT NOT NULL,
  reservation_id  INT,
  type            ENUM('late','no_show','misconduct') NOT NULL,
  description     VARCHAR(500),
  penalty_end     DATE,
  status          ENUM('active','resolved') DEFAULT 'active',
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (reservation_id) REFERENCES reservations(id)
) ENGINE=InnoDB;

-- 7. 公告表
CREATE TABLE notices (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(200) NOT NULL,
  content     TEXT,
  publisher   VARCHAR(50),
  is_top      TINYINT(1) DEFAULT 0,
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ============================================
-- 初始数据
-- ============================================

-- 管理员
INSERT INTO users (username, password, real_name, student_id, role) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', '000000', 'admin');
-- 密码: admin123

-- 测试学生
INSERT INTO users (username, password, real_name, student_id, phone, role) VALUES
('zhangsan', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '张三', '2021001', '13800001001', 'student'),
('lisi', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '李四', '2021002', '13800001002', 'student'),
('wangwu', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '王五', '2021003', '13800001003', 'student');

-- 区域
INSERT INTO areas (name, building, floor, description, sort_order) VALUES
('一楼北区', '图书馆主楼', 1, '安静自习区，靠窗视野好', 1),
('一楼南区', '图书馆主楼', 1, '配有台灯和插座', 2),
('二楼东区', '图书馆主楼', 2, '小组学习区，可轻声讨论', 3),
('三楼研修间', '图书馆主楼', 3, '独立小房间，需预约使用', 4);

-- 生成座位：一楼北区 4行×6列 = 24座
INSERT INTO seats (area_id, seat_number, row_num, col_num, has_outlet, has_lamp, is_window) VALUES
(1,'A-01',1,1,1,1,1),(1,'A-02',1,2,1,1,1),(1,'A-03',1,3,1,1,1),(1,'A-04',1,4,1,1,1),(1,'A-05',1,5,1,1,1),(1,'A-06',1,6,1,1,1),
(1,'A-07',2,1,1,1,0),(1,'A-08',2,2,1,1,0),(1,'A-09',2,3,1,1,0),(1,'A-10',2,4,1,1,0),(1,'A-11',2,5,1,1,0),(1,'A-12',2,6,1,1,0),
(1,'A-13',3,1,1,1,0),(1,'A-14',3,2,1,1,0),(1,'A-15',3,3,1,1,0),(1,'A-16',3,4,1,1,0),(1,'A-17',3,5,1,1,0),(1,'A-18',3,6,1,1,0),
(1,'A-19',4,1,0,1,1),(1,'A-20',4,2,0,1,1),(1,'A-21',4,3,0,1,1),(1,'A-22',4,4,0,1,1),(1,'A-23',4,5,0,1,1),(1,'A-24',4,6,0,1,1);

-- 一楼南区 3行×8列 = 24座
INSERT INTO seats (area_id, seat_number, row_num, col_num, has_outlet, has_lamp, is_window) VALUES
(2,'B-01',1,1,1,0,0),(2,'B-02',1,2,1,0,0),(2,'B-03',1,3,1,0,0),(2,'B-04',1,4,1,0,0),(2,'B-05',1,5,1,0,0),(2,'B-06',1,6,1,0,0),(2,'B-07',1,7,1,0,0),(2,'B-08',1,8,1,0,0),
(2,'B-09',2,1,1,0,0),(2,'B-10',2,2,1,0,0),(2,'B-11',2,3,1,0,0),(2,'B-12',2,4,1,0,0),(2,'B-13',2,5,1,0,0),(2,'B-14',2,6,1,0,0),(2,'B-15',2,7,1,0,0),(2,'B-16',2,8,1,0,0),
(2,'B-17',3,1,1,0,1),(2,'B-18',3,2,1,0,1),(2,'B-19',3,3,1,0,1),(2,'B-20',3,4,1,0,1),(2,'B-21',3,5,1,0,1),(2,'B-22',3,6,1,0,1),(2,'B-23',3,7,1,0,1),(2,'B-24',3,8,1,0,1);

-- 二楼东区 4行×5列 = 20座 + 8个讨论桌
INSERT INTO seats (area_id, seat_number, row_num, col_num, has_outlet, has_lamp, is_window) VALUES
(3,'C-01',1,1,1,0,1),(3,'C-02',1,2,1,0,1),(3,'C-03',1,3,1,0,1),(3,'C-04',1,4,1,0,1),(3,'C-05',1,5,1,0,1),
(3,'C-06',2,1,1,0,0),(3,'C-07',2,2,1,0,0),(3,'C-08',2,3,1,0,0),(3,'C-09',2,4,1,0,0),(3,'C-10',2,5,1,0,0),
(3,'C-11',3,1,1,0,0),(3,'C-12',3,2,1,0,0),(3,'C-13',3,3,1,0,0),(3,'C-14',3,4,1,0,0),(3,'C-15',3,5,1,0,0),
(3,'C-16',4,1,1,0,0),(3,'C-17',4,2,1,0,0),(3,'C-18',4,3,1,0,0),(3,'C-19',4,4,1,0,0),(3,'C-20',4,5,1,0,0);

-- 三楼研修间 8间
INSERT INTO seats (area_id, seat_number, row_num, col_num, has_outlet, has_lamp, is_window) VALUES
(4,'R-01',1,1,1,1,1),(4,'R-02',1,2,1,1,1),(4,'R-03',1,3,1,1,1),(4,'R-04',1,4,1,1,1),
(4,'R-05',2,1,1,1,0),(4,'R-06',2,2,1,1,0),(4,'R-07',2,3,1,1,0),(4,'R-08',2,4,1,1,0);

-- 公告
INSERT INTO notices (title, content, publisher, is_top) VALUES
('图书馆自习室使用须知', '请同学们自觉遵守自习室管理规定，预约后按时签到，离开时请签退释放座位。连续3次违规将限制预约7天。', '管理员', 1),
('期末考试周延长开放通知', '期末考试期间（6月15日-6月30日），自习室开放时间延长至23:00。', '管理员', 1);

-- 模拟一些已预约的座位（用于展示效果）
UPDATE seats SET current_status = 'reserved' WHERE area_id = 1 AND seat_number IN ('A-01','A-05','A-12');
UPDATE seats SET current_status = 'occupied' WHERE area_id = 1 AND seat_number IN ('A-03','A-08');
UPDATE seats SET current_status = 'temp_leave' WHERE area_id = 2 AND seat_number = 'B-03';
UPDATE seats SET current_status = 'maintenance' WHERE area_id = 2 AND seat_number = 'B-15';
UPDATE seats SET current_status = 'occupied' WHERE area_id = 3 AND seat_number IN ('C-06','C-07');

-- 模拟几条预约记录
INSERT INTO reservations (user_id, seat_id, date, start_time, end_time, status, qrcode_token) VALUES
(2, 1, CURDATE(), '08:00', '12:00', 'reserved', UUID()),
(2, 5, CURDATE(), '14:00', '18:00', 'reserved', UUID()),
(3, 25, CURDATE(), '08:00', '12:00', 'checked_in', UUID()),
(4, 45, CURDATE(), '10:00', '12:00', 'checked_in', UUID());
