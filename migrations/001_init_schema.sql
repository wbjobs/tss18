-- =============================================
-- 数据库初始化脚本
-- 版本: 001
-- 描述: 创建核心表结构和初始化数据
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------
-- 1. 租户信息表
-- ---------------------------------------------
DROP TABLE IF EXISTS `tenant_info`;
CREATE TABLE `tenant_info` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '租户名称',
  `code` VARCHAR(50) NOT NULL UNIQUE COMMENT '租户编码',
  `enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_tenant_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户信息表';

-- ---------------------------------------------
-- 2. 状态机定义表
-- ---------------------------------------------
DROP TABLE IF EXISTS `state_machine_def`;
CREATE TABLE `state_machine_def` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `name` VARCHAR(100) NOT NULL COMMENT '状态机名称',
  `description` VARCHAR(500) COMMENT '状态机描述',
  `definition_json` LONGTEXT COMMENT '状态机定义JSON',
  `version` INT DEFAULT 1 COMMENT '版本号',
  `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态:DRAFT/PUBLISHED/OFFLINE',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='状态机定义表';

-- ---------------------------------------------
-- 3. 工单实例表
-- ---------------------------------------------
DROP TABLE IF EXISTS `ticket_instance`;
CREATE TABLE `ticket_instance` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `state_machine_id` BIGINT NOT NULL COMMENT '状态机ID',
  `title` VARCHAR(200) NOT NULL COMMENT '工单标题',
  `business_key` VARCHAR(100) NOT NULL COMMENT '业务主键',
  `current_state_id` VARCHAR(50) NOT NULL COMMENT '当前状态ID',
  `current_state_name` VARCHAR(100) NOT NULL COMMENT '当前状态名称',
  `payload` JSON COMMENT '业务数据负载',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_tenant_business` (`tenant_id`, `business_key`),
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_state_machine_id` (`state_machine_id`),
  INDEX `idx_current_state` (`current_state_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单实例表';

-- ---------------------------------------------
-- 4. 状态转移轨迹表
-- ---------------------------------------------
DROP TABLE IF EXISTS `state_transition_trace`;
CREATE TABLE `state_transition_trace` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `ticket_id` BIGINT NOT NULL COMMENT '工单ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `from_state_id` VARCHAR(50) NOT NULL COMMENT '源状态ID',
  `from_state_name` VARCHAR(100) NOT NULL COMMENT '源状态名称',
  `to_state_id` VARCHAR(50) NOT NULL COMMENT '目标状态ID',
  `to_state_name` VARCHAR(100) NOT NULL COMMENT '目标状态名称',
  `operator_id` BIGINT COMMENT '操作人ID',
  `operator_name` VARCHAR(100) COMMENT '操作人名称',
  `trigger_source` VARCHAR(50) NOT NULL COMMENT '触发源:MANUAL/CALLBACK/AUTO',
  `remark` VARCHAR(500) COMMENT '备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_ticket_id` (`ticket_id`),
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='状态转移轨迹表';

-- ---------------------------------------------
-- 5. Webhook配置表
-- ---------------------------------------------
DROP TABLE IF EXISTS `webhook_config`;
CREATE TABLE `webhook_config` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `name` VARCHAR(100) NOT NULL COMMENT 'Webhook名称',
  `url` VARCHAR(500) NOT NULL COMMENT '回调地址',
  `secret_key` VARCHAR(100) NOT NULL COMMENT '签名密钥',
  `events` VARCHAR(500) COMMENT '订阅事件列表,逗号分隔',
  `enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
  `retry_count` INT DEFAULT 3 COMMENT '重试次数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Webhook配置表';

-- ---------------------------------------------
-- 6. Webhook调用日志表
-- ---------------------------------------------
DROP TABLE IF EXISTS `webhook_log`;
CREATE TABLE `webhook_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `webhook_id` BIGINT NOT NULL COMMENT 'Webhook配置ID',
  `url` VARCHAR(500) NOT NULL COMMENT '调用地址',
  `event` VARCHAR(50) NOT NULL COMMENT '事件类型',
  `payload` LONGTEXT COMMENT '请求payload',
  `status` VARCHAR(20) NOT NULL COMMENT '状态:SUCCESS/FAILED/RETRYING',
  `response` LONGTEXT COMMENT '响应内容',
  `retry_times` INT DEFAULT 0 COMMENT '重试次数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_webhook_id` (`webhook_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Webhook调用日志表';

-- ---------------------------------------------
-- 7. 初始化数据
-- ---------------------------------------------

-- 初始化默认租户
INSERT INTO `tenant_info` (`name`, `code`, `enabled`) VALUES ('默认租户', 'default', 1);

-- 初始化示例状态机 - 工单审批流程
INSERT INTO `state_machine_def` (`tenant_id`, `name`, `description`, `definition_json`, `version`, `status`) 
VALUES (1, '工单审批流程', '标准的工单审批工作流，包含创建、审批中、已批准、已拒绝、已完成等状态', 
'{
  "nodes": [
    {
      "id": "start",
      "name": "开始",
      "type": "START",
      "x": 100,
      "y": 200,
      "color": "#52c41a",
      "permissions": []
    },
    {
      "id": "pending",
      "name": "待审批",
      "type": "NORMAL",
      "x": 300,
      "y": 200,
      "color": "#1890ff",
      "permissions": ["ticket:view", "ticket:approve"]
    },
    {
      "id": "approved",
      "name": "已批准",
      "type": "NORMAL",
      "x": 500,
      "y": 120,
      "color": "#52c41a",
      "permissions": ["ticket:view", "ticket:process"]
    },
    {
      "id": "rejected",
      "name": "已拒绝",
      "type": "NORMAL",
      "x": 500,
      "y": 280,
      "color": "#ff4d4f",
      "permissions": ["ticket:view"]
    },
    {
      "id": "completed",
      "name": "已完成",
      "type": "END",
      "x": 700,
      "y": 200,
      "color": "#8c8c8c",
      "permissions": ["ticket:view"]
    }
  ],
  "transitions": [
    {
      "id": "t1",
      "name": "创建工单",
      "sourceStateId": "start",
      "targetStateId": "pending",
      "condition": "",
      "triggerSource": "MANUAL"
    },
    {
      "id": "t2",
      "name": "审批通过",
      "sourceStateId": "pending",
      "targetStateId": "approved",
      "condition": "#approveResult == true",
      "triggerSource": "MANUAL"
    },
    {
      "id": "t3",
      "name": "审批拒绝",
      "sourceStateId": "pending",
      "targetStateId": "rejected",
      "condition": "#approveResult == false",
      "triggerSource": "MANUAL"
    },
    {
      "id": "t4",
      "name": "处理完成",
      "sourceStateId": "approved",
      "targetStateId": "completed",
      "condition": "",
      "triggerSource": "MANUAL"
    },
    {
      "id": "t5",
      "name": "重新提交",
      "sourceStateId": "rejected",
      "targetStateId": "pending",
      "condition": "",
      "triggerSource": "MANUAL"
    },
    {
      "id": "t6",
      "name": "自动完成",
      "sourceStateId": "approved",
      "targetStateId": "completed",
      "condition": "#autoComplete == true",
      "triggerSource": "CALLBACK",
      "callbackSource": "crm_approval"
    }
  ]
}', 1, 'PUBLISHED');

SET FOREIGN_KEY_CHECKS = 1;
