CREATE TABLE IF NOT EXISTS offline_message_location (
    msg_id VARCHAR(64) PRIMARY KEY COMMENT '消息ID',
    user_id BIGINT NOT NULL COMMENT '接收者ID',
    server_address VARCHAR(50) NOT NULL COMMENT '存储服务器地址',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-未读, 0-已删除',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='离线消息位置表';
