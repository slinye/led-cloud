-- =============================================
-- LED Cloud 微服务架构 数据库初始化脚本
-- =============================================

-- 确保客户端和服务端编码一致，避免中文乱码
SET NAMES utf8mb4;

-- 创建 Seata 事务管理数据库
CREATE DATABASE IF NOT EXISTS seata DEFAULT CHARACTER SET utf8mb4;

-- 使用 led_management 数据库
USE led_management;

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    role VARCHAR(20) DEFAULT 'VIEWER',
    status INT DEFAULT 1,
    wechat_openid VARCHAR(100) NULL UNIQUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- LED屏幕表
CREATE TABLE IF NOT EXISTS t_screen (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    ip_address VARCHAR(50),
    resolution_width INT DEFAULT 1920,
    resolution_height INT DEFAULT 1080,
    brightness INT DEFAULT 80,
    status VARCHAR(20) DEFAULT 'offline',
    location VARCHAR(200),
    model VARCHAR(100),
    mqtt_client_id VARCHAR(100) UNIQUE,
    last_heartbeat DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 内容表
CREATE TABLE IF NOT EXISTS t_content (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200),
    type VARCHAR(50),
    file_path VARCHAR(500),
    file_size BIGINT,
    thumbnail_path VARCHAR(500),
    text_content TEXT,
    font_size INT,
    font_color VARCHAR(20),
    bg_color VARCHAR(20),
    scroll_speed INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 节目表
CREATE TABLE IF NOT EXISTS t_program (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200),
    description TEXT,
    status VARCHAR(20) DEFAULT 'draft',
    schedule_type VARCHAR(20),
    schedule_time DATETIME,
    schedule_end_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 节目-内容关联表
CREATE TABLE IF NOT EXISTS t_program_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    program_id BIGINT NOT NULL,
    content_id BIGINT NOT NULL,
    sort_order INT DEFAULT 1,
    duration INT DEFAULT 10,
    FOREIGN KEY (program_id) REFERENCES t_program(id) ON DELETE CASCADE,
    FOREIGN KEY (content_id) REFERENCES t_content(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 播放日志表
CREATE TABLE IF NOT EXISTS t_play_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    screen_id BIGINT,
    program_id BIGINT,
    action VARCHAR(50),
    message VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 系统设置表
CREATE TABLE IF NOT EXISTS t_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) UNIQUE,
    setting_value TEXT,
    description VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 审计日志表
CREATE TABLE IF NOT EXISTS t_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    module VARCHAR(50),
    action VARCHAR(50),
    description VARCHAR(255),
    ip VARCHAR(50),
    cost_time BIGINT,
    request_method VARCHAR(10),
    request_url VARCHAR(500),
    request_params TEXT,
    response_result TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 屏幕分组表
CREATE TABLE IF NOT EXISTS t_screen_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    description VARCHAR(255),
    area VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 屏幕-分组关联表
CREATE TABLE IF NOT EXISTS t_screen_group_rel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    screen_id BIGINT NOT NULL,
    FOREIGN KEY (group_id) REFERENCES t_screen_group(id) ON DELETE CASCADE,
    FOREIGN KEY (screen_id) REFERENCES t_screen(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化管理员账号 (admin / admin123)
INSERT IGNORE INTO t_user (username, password, nickname, role, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'ADMIN', 1);

-- 初始化示例屏幕
INSERT IGNORE INTO t_screen (id, name, ip_address, status, mqtt_client_id) VALUES
(1, '展厅主屏', '192.168.1.100', 'offline', 'SCREEN-001'),
(2, '走廊屏-2号', '192.168.1.101', 'offline', 'SCREEN-002'),
(3, '走廊屏-3号', '192.168.1.102', 'offline', 'SCREEN-003'),
(4, '大厅屏-1号', '192.168.1.103', 'offline', 'SCREEN-004'),
(5, '大厅屏-2号', '192.168.1.104', 'offline', 'SCREEN-005'),
(6, '会议室屏', '192.168.1.105', 'offline', 'SCREEN-006');

-- =============================================
-- Seata 分布式事务所需表
-- =============================================
USE seata;

CREATE TABLE IF NOT EXISTS global_table (
    xid VARCHAR(128) NOT NULL,
    transaction_id BIGINT,
    status TINYINT NOT NULL,
    application_id VARCHAR(32),
    transaction_service_group VARCHAR(32),
    transaction_name VARCHAR(128),
    timeout INT,
    begin_time BIGINT,
    application_data VARCHAR(2000),
    gmt_create DATETIME,
    gmt_modified DATETIME,
    PRIMARY KEY (xid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS branch_table (
    xid VARCHAR(128) NOT NULL,
    branch_id BIGINT NOT NULL,
    transaction_id BIGINT,
    resource_group_id VARCHAR(32),
    resource_id VARCHAR(256),
    branch_type VARCHAR(8),
    status TINYINT,
    client_id VARCHAR(64),
    application_data VARCHAR(2000),
    gmt_create DATETIME,
    gmt_modified DATETIME,
    PRIMARY KEY (xid, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lock_table (
    row_key VARCHAR(128) NOT NULL,
    xid VARCHAR(128),
    transaction_id BIGINT,
    branch_id BIGINT NOT NULL,
    resource_id VARCHAR(256),
    table_name VARCHAR(32),
    pk VARCHAR(36),
    status TINYINT,
    gmt_create DATETIME,
    gmt_modified DATETIME,
    PRIMARY KEY (row_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seata 需要各业务库也有 undo_log 表
USE led_management;

CREATE TABLE IF NOT EXISTS undo_log (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    branch_id BIGINT(20) NOT NULL,
    xid VARCHAR(100) NOT NULL,
    context VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB NOT NULL,
    log_status INT(11) NOT NULL,
    log_created DATETIME NOT NULL,
    log_modified DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
