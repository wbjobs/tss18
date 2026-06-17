SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE ticket_instance
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER payload;

CREATE TABLE IF NOT EXISTS saga_compensation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_id BIGINT NOT NULL COMMENT '工单ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  saga_id VARCHAR(50) NOT NULL COMMENT 'Saga事务ID',
  step VARCHAR(50) NOT NULL COMMENT '步骤名称:VALIDATE/EVALUATE_CONDITION/UPDATE_STATE/RECORD_TRACE/NOTIFY_WEBHOOK',
  status VARCHAR(20) NOT NULL COMMENT '状态:STARTED/COMPLETED/COMPENSATING/COMPENSATED/FAILED',
  from_state_id VARCHAR(50) NOT NULL COMMENT '源状态ID',
  from_state_name VARCHAR(100) NOT NULL COMMENT '源状态名称',
  to_state_id VARCHAR(50) NOT NULL COMMENT '目标状态ID',
  to_state_name VARCHAR(100) NOT NULL COMMENT '目标状态名称',
  transition_id VARCHAR(50) COMMENT '转移ID',
  trigger_source VARCHAR(50) COMMENT '触发源',
  snapshot_payload TEXT COMMENT '快照payload',
  error_message TEXT COMMENT '错误信息',
  snapshot_version BIGINT NOT NULL COMMENT '快照版本号',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  compensated_at DATETIME COMMENT '补偿完成时间',
  INDEX idx_ticket_id (ticket_id),
  INDEX idx_saga_id (saga_id),
  INDEX idx_status (status),
  INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Saga补偿日志表';

SET FOREIGN_KEY_CHECKS = 1;
