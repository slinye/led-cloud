-- 节目发布记录表
CREATE TABLE IF NOT EXISTS t_program_publish (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    program_id BIGINT NOT NULL COMMENT '节目ID',
    version INT DEFAULT 1 COMMENT '发布版本号',
    snapshot JSON COMMENT '发布时节目快照(名称+素材列表)',
    operator VARCHAR(50) COMMENT '发布人',
    remark VARCHAR(255) COMMENT '发布备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_program_id (program_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
