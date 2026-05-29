-- ============================================================
-- 校园智能招聘与面试预约平台
-- MySQL 8.x 初始化脚本
-- File: V001__初始化数据库结构.sql
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS campus_recruitment
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE campus_recruitment;

-- ============================================================
-- 1. Drop tables in reverse dependency order
-- ============================================================

DROP TABLE IF EXISTS mq_message_log;
DROP TABLE IF EXISTS message;
DROP TABLE IF EXISTS interview_booking;
DROP TABLE IF EXISTS interview_slot;
DROP TABLE IF EXISTS application_log;
DROP TABLE IF EXISTS job_application;
DROP TABLE IF EXISTS job_view_history;
DROP TABLE IF EXISTS job_favorite;
DROP TABLE IF EXISTS job_tag;
DROP TABLE IF EXISTS job;
DROP TABLE IF EXISTS resume;
DROP TABLE IF EXISTS file_object;
DROP TABLE IF EXISTS company_audit;
DROP TABLE IF EXISTS company_profile;
DROP TABLE IF EXISTS student_skill;
DROP TABLE IF EXISTS student_profile;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS login_log;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS sys_config;
DROP TABLE IF EXISTS sys_dict;
DROP TABLE IF EXISTS sys_user;

-- ============================================================
-- 2. System user and RBAC tables
-- ============================================================

CREATE TABLE sys_user (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(64) NOT NULL COMMENT '用户名',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
  nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
  phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  avatar_url VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
  user_type VARCHAR(32) NOT NULL COMMENT '用户类型：STUDENT/COMPANY/ADMIN',
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL/DISABLED/PENDING',
  last_login_time DATETIME(3) DEFAULT NULL COMMENT '最近登录时间',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  UNIQUE KEY uk_sys_user_phone (phone),
  UNIQUE KEY uk_sys_user_email (email),
  KEY idx_sys_user_type_status (user_type, status),
  KEY idx_sys_user_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE sys_role (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
  role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '状态',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_code (role_code),
  KEY idx_sys_role_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

CREATE TABLE sys_menu (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  parent_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父级菜单ID',
  menu_name VARCHAR(64) NOT NULL COMMENT '菜单名称',
  menu_type VARCHAR(32) NOT NULL DEFAULT 'MENU' COMMENT '类型：DIR/MENU/BUTTON/API',
  path VARCHAR(255) DEFAULT NULL COMMENT '前端路由',
  component VARCHAR(255) DEFAULT NULL COMMENT '前端组件',
  permission VARCHAR(128) DEFAULT NULL COMMENT '权限标识',
  visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '状态',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_menu_permission (permission),
  KEY idx_sys_menu_parent (parent_id),
  KEY idx_sys_menu_type_status (menu_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单权限表';

CREATE TABLE sys_user_role (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_role (user_id, role_id),
  KEY idx_sys_user_role_user (user_id),
  KEY idx_sys_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';

CREATE TABLE sys_role_menu (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  menu_id BIGINT UNSIGNED NOT NULL COMMENT '菜单ID',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_menu (role_id, menu_id),
  KEY idx_sys_role_menu_role (role_id),
  KEY idx_sys_role_menu_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色菜单关联表';

-- ============================================================
-- 3. Student tables
-- ============================================================

CREATE TABLE student_profile (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '学生资料ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
  gender VARCHAR(16) DEFAULT NULL COMMENT '性别：MALE/FEMALE/UNKNOWN',
  school VARCHAR(128) NOT NULL COMMENT '学校',
  major VARCHAR(128) NOT NULL COMMENT '专业',
  grade VARCHAR(32) DEFAULT NULL COMMENT '年级',
  education VARCHAR(32) DEFAULT NULL COMMENT '学历',
  city VARCHAR(64) DEFAULT NULL COMMENT '期望城市',
  job_intention VARCHAR(255) DEFAULT NULL COMMENT '求职意向',
  advantage TEXT DEFAULT NULL COMMENT '个人优势',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_student_profile_user (user_id),
  KEY idx_student_profile_school_major (school, major),
  KEY idx_student_profile_city (city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生资料表';

CREATE TABLE student_skill (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '学生技能ID',
  student_id BIGINT UNSIGNED NOT NULL COMMENT '学生用户ID',
  skill_name VARCHAR(64) NOT NULL COMMENT '技能名称',
  skill_level VARCHAR(32) DEFAULT NULL COMMENT '熟练程度',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_student_skill (student_id, skill_name),
  KEY idx_student_skill_name (skill_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生技能表';

-- ============================================================
-- 4. Company tables
-- ============================================================

CREATE TABLE company_profile (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '企业资料ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '企业用户ID',
  company_name VARCHAR(128) NOT NULL COMMENT '企业名称',
  industry VARCHAR(64) DEFAULT NULL COMMENT '行业',
  scale VARCHAR(64) DEFAULT NULL COMMENT '规模',
  city VARCHAR(64) DEFAULT NULL COMMENT '城市',
  address VARCHAR(255) DEFAULT NULL COMMENT '详细地址',
  contact_name VARCHAR(64) NOT NULL COMMENT '联系人',
  contact_phone VARCHAR(20) NOT NULL COMMENT '联系电话',
  license_file_id BIGINT UNSIGNED DEFAULT NULL COMMENT '企业资质文件ID',
  audit_status VARCHAR(32) NOT NULL DEFAULT 'UNVERIFIED' COMMENT '审核状态',
  audit_reason VARCHAR(500) DEFAULT NULL COMMENT '最近审核原因',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_company_profile_user (user_id),
  KEY idx_company_profile_audit_status (audit_status),
  KEY idx_company_profile_name (company_name),
  KEY idx_company_profile_city_industry (city, industry)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业资料表';

CREATE TABLE company_audit (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '企业审核记录ID',
  company_id BIGINT UNSIGNED NOT NULL COMMENT '企业资料ID',
  audit_status VARCHAR(32) NOT NULL COMMENT '审核结果：APPROVED/REJECTED',
  audit_reason VARCHAR(500) DEFAULT NULL COMMENT '审核原因',
  auditor_id BIGINT UNSIGNED DEFAULT NULL COMMENT '审核人ID',
  audit_time DATETIME(3) DEFAULT NULL COMMENT '审核时间',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_company_audit_company (company_id),
  KEY idx_company_audit_status_time (audit_status, audit_time),
  KEY idx_company_audit_auditor (auditor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企业审核记录表';

-- ============================================================
-- 5. File and resume tables
-- ============================================================

CREATE TABLE file_object (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  owner_id BIGINT UNSIGNED NOT NULL COMMENT '上传人ID',
  biz_type VARCHAR(32) NOT NULL COMMENT '业务类型：RESUME/AVATAR/LICENSE/JOB_ATTACHMENT',
  bucket_name VARCHAR(64) NOT NULL COMMENT 'MinIO Bucket',
  object_name VARCHAR(255) NOT NULL COMMENT '对象名称',
  original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
  file_size BIGINT UNSIGNED NOT NULL COMMENT '文件大小，单位字节',
  content_type VARCHAR(128) NOT NULL COMMENT 'MIME类型',
  file_ext VARCHAR(32) NOT NULL COMMENT '文件后缀',
  access_url VARCHAR(512) DEFAULT NULL COMMENT '访问URL',
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL/DELETED',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_file_object_name (bucket_name, object_name),
  KEY idx_file_owner_biz (owner_id, biz_type),
  KEY idx_file_status (status),
  KEY idx_file_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件对象表';

CREATE TABLE resume (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '简历ID',
  student_id BIGINT UNSIGNED NOT NULL COMMENT '学生用户ID',
  resume_name VARCHAR(128) NOT NULL COMMENT '简历名称',
  file_id BIGINT UNSIGNED NOT NULL COMMENT '文件ID',
  is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认简历',
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL/DISABLED',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_resume_student (student_id),
  KEY idx_resume_file (file_id),
  KEY idx_resume_student_default (student_id, is_default),
  KEY idx_resume_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简历表';

-- ============================================================
-- 6. Job tables
-- ============================================================

CREATE TABLE job (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  company_id BIGINT UNSIGNED NOT NULL COMMENT '企业资料ID',
  title VARCHAR(128) NOT NULL COMMENT '岗位名称',
  category VARCHAR(64) DEFAULT NULL COMMENT '岗位分类',
  city VARCHAR(64) NOT NULL COMMENT '工作城市',
  salary_min INT UNSIGNED DEFAULT NULL COMMENT '最低薪资',
  salary_max INT UNSIGNED DEFAULT NULL COMMENT '最高薪资',
  salary_unit VARCHAR(32) NOT NULL DEFAULT 'DAY' COMMENT '薪资单位：DAY/MONTH',
  education VARCHAR(32) DEFAULT NULL COMMENT '学历要求',
  experience VARCHAR(32) DEFAULT NULL COMMENT '经验要求',
  description TEXT NOT NULL COMMENT '岗位描述',
  requirement TEXT NOT NULL COMMENT '岗位要求',
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '岗位状态',
  audit_reason VARCHAR(500) DEFAULT NULL COMMENT '审核原因',
  expire_time DATETIME(3) DEFAULT NULL COMMENT '过期时间',
  view_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览数',
  apply_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '投递数',
  favorite_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '收藏数',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_job_company (company_id),
  KEY idx_job_status_city (status, city),
  KEY idx_job_category_status (category, status),
  KEY idx_job_expire_time (expire_time),
  KEY idx_job_create_time (create_time),
  KEY idx_job_hot (status, view_count, apply_count, favorite_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位表';

CREATE TABLE job_tag (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '岗位标签ID',
  job_id BIGINT UNSIGNED NOT NULL COMMENT '岗位ID',
  tag_name VARCHAR(64) NOT NULL COMMENT '标签名称',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_job_tag (job_id, tag_name),
  KEY idx_job_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位标签表';

CREATE TABLE job_favorite (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  student_id BIGINT UNSIGNED NOT NULL COMMENT '学生用户ID',
  job_id BIGINT UNSIGNED NOT NULL COMMENT '岗位ID',
  favorite_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '收藏时间',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_job_favorite_student_job (student_id, job_id),
  KEY idx_job_favorite_job (job_id),
  KEY idx_job_favorite_student_time (student_id, favorite_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位收藏表';

CREATE TABLE job_view_history (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '浏览记录ID',
  student_id BIGINT UNSIGNED DEFAULT NULL COMMENT '学生用户ID，游客可为空',
  job_id BIGINT UNSIGNED NOT NULL COMMENT '岗位ID',
  view_ip VARCHAR(64) DEFAULT NULL COMMENT '浏览IP',
  view_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '浏览时间',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_job_view_student_time (student_id, view_time),
  KEY idx_job_view_job_time (job_id, view_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位浏览记录表';

-- ============================================================
-- 7. Application tables
-- ============================================================

CREATE TABLE job_application (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '投递ID',
  student_id BIGINT UNSIGNED NOT NULL COMMENT '学生用户ID',
  company_id BIGINT UNSIGNED NOT NULL COMMENT '企业资料ID',
  job_id BIGINT UNSIGNED NOT NULL COMMENT '岗位ID',
  resume_id BIGINT UNSIGNED NOT NULL COMMENT '简历ID',
  status VARCHAR(32) NOT NULL DEFAULT 'DELIVERED' COMMENT '投递状态',
  apply_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '投递时间',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_application_student_job (student_id, job_id),
  KEY idx_application_student_status (student_id, status),
  KEY idx_application_company_status (company_id, status),
  KEY idx_application_job_status (job_id, status),
  KEY idx_application_resume (resume_id),
  KEY idx_application_apply_time (apply_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位投递表';

CREATE TABLE application_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '投递状态日志ID',
  application_id BIGINT UNSIGNED NOT NULL COMMENT '投递ID',
  before_status VARCHAR(32) DEFAULT NULL COMMENT '变更前状态',
  after_status VARCHAR(32) NOT NULL COMMENT '变更后状态',
  operator_id BIGINT UNSIGNED NOT NULL COMMENT '操作人ID',
  operator_type VARCHAR(32) NOT NULL COMMENT '操作人类型：STUDENT/COMPANY/ADMIN/SYSTEM',
  reason VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_application_log_application (application_id),
  KEY idx_application_log_operator (operator_id, operator_type),
  KEY idx_application_log_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='投递状态日志表';

-- ============================================================
-- 8. Interview tables
-- ============================================================

CREATE TABLE interview_slot (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '面试场次ID',
  company_id BIGINT UNSIGNED NOT NULL COMMENT '企业资料ID',
  job_id BIGINT UNSIGNED NOT NULL COMMENT '岗位ID',
  title VARCHAR(128) NOT NULL COMMENT '场次标题',
  start_time DATETIME(3) NOT NULL COMMENT '开始时间',
  end_time DATETIME(3) NOT NULL COMMENT '结束时间',
  capacity INT UNSIGNED NOT NULL COMMENT '总名额',
  remain_count INT UNSIGNED NOT NULL COMMENT '剩余名额',
  interview_type VARCHAR(32) NOT NULL DEFAULT 'ONLINE' COMMENT '面试方式：ONLINE/OFFLINE',
  location VARCHAR(255) DEFAULT NULL COMMENT '面试地点或会议链接',
  description VARCHAR(1000) DEFAULT NULL COMMENT '面试说明',
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN/FULL/CLOSED/EXPIRED',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_slot_job_status_time (job_id, status, start_time),
  KEY idx_slot_company_status (company_id, status),
  KEY idx_slot_start_time (start_time),
  KEY idx_slot_end_time (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试场次表';

CREATE TABLE interview_booking (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '面试预约ID',
  slot_id BIGINT UNSIGNED NOT NULL COMMENT '场次ID',
  application_id BIGINT UNSIGNED NOT NULL COMMENT '投递ID',
  student_id BIGINT UNSIGNED NOT NULL COMMENT '学生用户ID',
  company_id BIGINT UNSIGNED NOT NULL COMMENT '企业资料ID',
  job_id BIGINT UNSIGNED NOT NULL COMMENT '岗位ID',
  status VARCHAR(32) NOT NULL DEFAULT 'BOOKED' COMMENT '预约状态：BOOKED/CANCELED/FINISHED',
  booking_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '预约时间',
  cancel_reason VARCHAR(500) DEFAULT NULL COMMENT '取消原因',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  active_slot_student VARCHAR(128) GENERATED ALWAYS AS (
    CASE WHEN status = 'BOOKED' THEN CONCAT(slot_id, '-', student_id) ELSE NULL END
  ) STORED,
  active_application_id BIGINT UNSIGNED GENERATED ALWAYS AS (
    CASE WHEN status = 'BOOKED' THEN application_id ELSE NULL END
  ) STORED,
  UNIQUE KEY uk_active_booking_slot_student (active_slot_student),
  UNIQUE KEY uk_active_booking_application (active_application_id),
  KEY idx_booking_student_status (student_id, status),
  KEY idx_booking_company_status (company_id, status),
  KEY idx_booking_job_status (job_id, status),
  KEY idx_booking_time (booking_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试预约表';

-- ============================================================
-- 9. Message and MQ tables
-- ============================================================

CREATE TABLE message (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  message_id VARCHAR(128) NOT NULL COMMENT '业务消息唯一ID，用于幂等',
  receiver_id BIGINT UNSIGNED NOT NULL COMMENT '接收人ID',
  sender_id BIGINT UNSIGNED DEFAULT NULL COMMENT '发送人ID',
  message_type VARCHAR(32) NOT NULL COMMENT '消息类型：APPLICATION/AUDIT/INTERVIEW/SYSTEM',
  title VARCHAR(128) NOT NULL COMMENT '标题',
  content TEXT NOT NULL COMMENT '内容',
  business_type VARCHAR(64) DEFAULT NULL COMMENT '关联业务类型',
  business_id BIGINT UNSIGNED DEFAULT NULL COMMENT '关联业务ID',
  read_status VARCHAR(32) NOT NULL DEFAULT 'UNREAD' COMMENT '阅读状态：UNREAD/READ/DELETED',
  read_time DATETIME(3) DEFAULT NULL COMMENT '阅读时间',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_message_message_id (message_id),
  KEY idx_message_receiver_status (receiver_id, read_status),
  KEY idx_message_receiver_time (receiver_id, create_time),
  KEY idx_message_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内信表';

CREATE TABLE mq_message_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'MQ消息日志ID',
  message_id VARCHAR(128) NOT NULL COMMENT '消息唯一ID',
  message_type VARCHAR(64) NOT NULL COMMENT '消息类型',
  business_id BIGINT UNSIGNED DEFAULT NULL COMMENT '业务ID',
  send_status VARCHAR(32) DEFAULT NULL COMMENT '生产侧发送状态：SENDING/SENT/SEND_FAILED',
  consume_status VARCHAR(32) NOT NULL DEFAULT 'INIT' COMMENT '消费侧状态：INIT/CONSUMED/CONSUME_FAILED',
  retry_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '重试次数',
  error_message TEXT DEFAULT NULL COMMENT '错误信息',
  consume_time DATETIME(3) DEFAULT NULL COMMENT '消费时间',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_mq_message_id (message_id),
  KEY idx_mq_status_time (consume_status, create_time),
  KEY idx_mq_business (message_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MQ消息消费日志表';

-- ============================================================
-- 10. Logs and configuration tables
-- ============================================================

CREATE TABLE operation_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '操作日志ID',
  user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人ID',
  username VARCHAR(64) DEFAULT NULL COMMENT '操作人用户名',
  module VARCHAR(64) NOT NULL COMMENT '模块',
  operation VARCHAR(128) NOT NULL COMMENT '操作',
  request_method VARCHAR(16) DEFAULT NULL COMMENT '请求方法',
  request_url VARCHAR(255) DEFAULT NULL COMMENT '请求URL',
  request_ip VARCHAR(64) DEFAULT NULL COMMENT '请求IP',
  request_param TEXT DEFAULT NULL COMMENT '请求参数，注意脱敏',
  result_status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '结果状态',
  error_message TEXT DEFAULT NULL COMMENT '错误信息',
  cost_time BIGINT UNSIGNED DEFAULT NULL COMMENT '耗时毫秒',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_operation_log_user_time (user_id, create_time),
  KEY idx_operation_log_module_time (module, create_time),
  KEY idx_operation_log_status_time (result_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';

CREATE TABLE login_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '登录日志ID',
  user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '用户ID',
  username VARCHAR(64) NOT NULL COMMENT '用户名',
  user_type VARCHAR(32) DEFAULT NULL COMMENT '用户类型',
  login_ip VARCHAR(64) DEFAULT NULL COMMENT '登录IP',
  user_agent VARCHAR(512) DEFAULT NULL COMMENT 'User-Agent',
  status VARCHAR(32) NOT NULL COMMENT '登录结果：SUCCESS/FAIL',
  fail_reason VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
  login_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '登录时间',
  PRIMARY KEY (id),
  KEY idx_login_log_user_time (user_id, login_time),
  KEY idx_login_log_username_time (username, login_time),
  KEY idx_login_log_status_time (status, login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录日志表';

CREATE TABLE sys_dict (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '字典ID',
  dict_type VARCHAR(64) NOT NULL COMMENT '字典类型',
  dict_key VARCHAR(64) NOT NULL COMMENT '字典键',
  dict_value VARCHAR(128) NOT NULL COMMENT '字典值',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '状态',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_dict_type_key (dict_type, dict_key),
  KEY idx_sys_dict_type_status (dict_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统字典表';

CREATE TABLE sys_config (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  config_key VARCHAR(128) NOT NULL COMMENT '配置键',
  config_value VARCHAR(1000) NOT NULL COMMENT '配置值',
  config_desc VARCHAR(255) DEFAULT NULL COMMENT '配置说明',
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '状态',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_config_key (config_key),
  KEY idx_sys_config_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';

-- ============================================================
-- 11. Initial data
-- ============================================================

INSERT INTO sys_role (id, role_code, role_name, status, sort, remark) VALUES
(1, 'ADMIN', '管理员', 'NORMAL', 1, '系统管理员'),
(2, 'STUDENT', '学生', 'NORMAL', 2, '学生用户'),
(3, 'COMPANY', '企业HR', 'NORMAL', 3, '企业招聘用户');

-- 默认管理员
-- password_hash 请替换为真实 BCrypt 哈希。示例值不是可用密码。
INSERT INTO sys_user (
  id, username, password_hash, nickname, user_type, status, remark
) VALUES (
  1, 'admin', '$2a$10$REPLACE_WITH_BCRYPT_HASH', '系统管理员', 'ADMIN', 'NORMAL', '默认管理员，请修改密码'
);

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 权限菜单初始化，包含核心 API 权限标识
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, permission, visible, sort, status) VALUES
(1, 0, '系统管理', 'DIR', '/admin/system', NULL, NULL, 1, 1, 'NORMAL'),
(2, 0, '招聘管理', 'DIR', '/admin/recruitment', NULL, NULL, 1, 2, 'NORMAL'),
(3, 0, '学生中心', 'DIR', '/student', NULL, NULL, 1, 3, 'NORMAL'),
(4, 0, '企业中心', 'DIR', '/company', NULL, NULL, 1, 4, 'NORMAL'),

(100, 1, '用户管理', 'MENU', '/admin/users', 'admin/user/index', 'user:manage', 1, 100, 'NORMAL'),
(101, 1, '操作日志', 'MENU', '/admin/logs/operation', 'admin/log/operation', 'log:view', 1, 101, 'NORMAL'),

(200, 2, '企业审核', 'MENU', '/admin/company-audits', 'admin/company/audit', 'company:audit', 1, 200, 'NORMAL'),
(201, 2, '岗位审核', 'MENU', '/admin/job-audits', 'admin/job/audit', 'job:audit', 1, 201, 'NORMAL'),

(300, 3, '简历上传', 'API', NULL, NULL, 'resume:upload', 0, 300, 'NORMAL'),
(301, 3, '投递岗位', 'API', NULL, NULL, 'application:create', 0, 301, 'NORMAL'),
(302, 3, '预约面试', 'API', NULL, NULL, 'interview:booking:create', 0, 302, 'NORMAL'),

(400, 4, '发布岗位', 'API', NULL, NULL, 'job:create', 0, 400, 'NORMAL'),
(401, 4, '修改岗位', 'API', NULL, NULL, 'job:update', 0, 401, 'NORMAL'),
(402, 4, '查看投递', 'API', NULL, NULL, 'application:view:company', 0, 402, 'NORMAL'),
(403, 4, '创建面试场次', 'API', NULL, NULL, 'interview:slot:create', 0, 403, 'NORMAL');

-- 角色授权
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
-- ADMIN
(1, 1), (1, 2), (1, 100), (1, 101), (1, 200), (1, 201),
(1, 300), (1, 301), (1, 302), (1, 400), (1, 401), (1, 402), (1, 403),
-- STUDENT
(2, 3), (2, 300), (2, 301), (2, 302),
-- COMPANY
(3, 4), (3, 400), (3, 401), (3, 402), (3, 403);

-- 数据字典
INSERT INTO sys_dict (dict_type, dict_key, dict_value, sort, remark) VALUES
('user_type', 'STUDENT', '学生', 1, '用户类型'),
('user_type', 'COMPANY', '企业', 2, '用户类型'),
('user_type', 'ADMIN', '管理员', 3, '用户类型'),

('user_status', 'NORMAL', '正常', 1, '用户状态'),
('user_status', 'DISABLED', '禁用', 2, '用户状态'),
('user_status', 'PENDING', '待完善', 3, '用户状态'),

('company_audit_status', 'UNVERIFIED', '未认证', 1, '企业认证状态'),
('company_audit_status', 'PENDING', '待审核', 2, '企业认证状态'),
('company_audit_status', 'APPROVED', '认证通过', 3, '企业认证状态'),
('company_audit_status', 'REJECTED', '认证拒绝', 4, '企业认证状态'),

('job_status', 'DRAFT', '草稿', 1, '岗位状态'),
('job_status', 'PENDING_REVIEW', '待审核', 2, '岗位状态'),
('job_status', 'PUBLISHED', '已发布', 3, '岗位状态'),
('job_status', 'REJECTED', '审核拒绝', 4, '岗位状态'),
('job_status', 'OFFLINE', '已下架', 5, '岗位状态'),
('job_status', 'EXPIRED', '已过期', 6, '岗位状态'),

('application_status', 'DELIVERED', '已投递', 1, '投递状态'),
('application_status', 'VIEWED', '已查看', 2, '投递状态'),
('application_status', 'INTERVIEW_INVITED', '邀约面试', 3, '投递状态'),
('application_status', 'BOOKED', '已预约', 4, '投递状态'),
('application_status', 'REJECTED', '不合适', 5, '投递状态'),
('application_status', 'CANCELED', '已取消', 6, '投递状态'),
('application_status', 'FINISHED', '已完成', 7, '投递状态'),

('interview_slot_status', 'OPEN', '开放预约', 1, '面试场次状态'),
('interview_slot_status', 'FULL', '已约满', 2, '面试场次状态'),
('interview_slot_status', 'CLOSED', '已关闭', 3, '面试场次状态'),
('interview_slot_status', 'EXPIRED', '已过期', 4, '面试场次状态'),

('interview_booking_status', 'BOOKED', '已预约', 1, '预约状态'),
('interview_booking_status', 'CANCELED', '已取消', 2, '预约状态'),
('interview_booking_status', 'FINISHED', '已完成', 3, '预约状态'),

('education', 'JUNIOR_COLLEGE', '大专', 1, '学历'),
('education', 'BACHELOR', '本科', 2, '学历'),
('education', 'MASTER', '硕士', 3, '学历'),
('education', 'DOCTOR', '博士', 4, '学历'),

('salary_unit', 'DAY', '天', 1, '薪资单位'),
('salary_unit', 'MONTH', '月', 2, '薪资单位'),

('job_category', 'backend', '后端开发', 1, '岗位分类'),
('job_category', 'frontend', '前端开发', 2, '岗位分类'),
('job_category', 'algorithm', '算法', 3, '岗位分类'),
('job_category', 'testing', '测试开发', 4, '岗位分类'),
('job_category', 'product', '产品经理', 5, '岗位分类');

-- 系统配置
INSERT INTO sys_config (config_key, config_value, config_desc) VALUES
('file.resume.max_size_mb', '10', '简历最大上传大小，单位MB'),
('file.resume.allowed_ext', 'pdf,doc,docx', '简历允许上传格式'),
('interview.cancel_before_hours', '2', '面试预约最晚取消时间，单位小时'),
('cache.job_detail.ttl_minutes', '30', '岗位详情缓存时间，单位分钟'),
('security.login.token_ttl_hours', '24', '登录Token有效时间，单位小时');

SET FOREIGN_KEY_CHECKS = 1;
