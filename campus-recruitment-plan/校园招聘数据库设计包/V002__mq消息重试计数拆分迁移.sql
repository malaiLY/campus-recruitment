-- V002: migrate mq_message_log retry columns for existing databases
-- Date: 2026-05-29

USE campus_recruitment;

-- 1) add send_retry_count when missing
SET @send_col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mq_message_log'
      AND COLUMN_NAME = 'send_retry_count'
);
SET @add_send_col_sql := IF(
    @send_col_exists = 0,
    'ALTER TABLE mq_message_log ADD COLUMN send_retry_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''生产侧发送重试次数'' AFTER consume_status',
    'SELECT 1'
);
PREPARE stmt_add_send_col FROM @add_send_col_sql;
EXECUTE stmt_add_send_col;
DEALLOCATE PREPARE stmt_add_send_col;

-- 2) add consume_retry_count when missing
SET @consume_col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mq_message_log'
      AND COLUMN_NAME = 'consume_retry_count'
);
SET @add_consume_col_sql := IF(
    @consume_col_exists = 0,
    'ALTER TABLE mq_message_log ADD COLUMN consume_retry_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''消费侧重试次数'' AFTER send_retry_count',
    'SELECT 1'
);
PREPARE stmt_add_consume_col FROM @add_consume_col_sql;
EXECUTE stmt_add_consume_col;
DEALLOCATE PREPARE stmt_add_consume_col;

-- 3) backfill from legacy retry_count if that old column still exists
SET @legacy_retry_col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mq_message_log'
      AND COLUMN_NAME = 'retry_count'
);
SET @backfill_sql := IF(
    @legacy_retry_col_exists > 0,
    'UPDATE mq_message_log SET send_retry_count = IFNULL(retry_count, 0), consume_retry_count = IFNULL(retry_count, 0) WHERE send_retry_count = 0 AND consume_retry_count = 0',
    'SELECT 1'
);
PREPARE stmt_backfill FROM @backfill_sql;
EXECUTE stmt_backfill;
DEALLOCATE PREPARE stmt_backfill;

-- 4) add retry scan index when missing
SET @retry_idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mq_message_log'
      AND INDEX_NAME = 'idx_mq_send_retry'
);
SET @add_retry_idx_sql := IF(
    @retry_idx_exists = 0,
    'CREATE INDEX idx_mq_send_retry ON mq_message_log(send_status, update_time, send_retry_count)',
    'SELECT 1'
);
PREPARE stmt_add_retry_idx FROM @add_retry_idx_sql;
EXECUTE stmt_add_retry_idx;
DEALLOCATE PREPARE stmt_add_retry_idx;
