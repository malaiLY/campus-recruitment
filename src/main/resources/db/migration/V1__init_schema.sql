-- ============================================================
-- 鏍″洯鏅鸿兘鎷涜仒涓庨潰璇曢绾﹀钩鍙?-- MySQL 8.x 鍒濆鍖栬剼鏈?-- File: V001__鍒濆鍖栨暟鎹簱缁撴瀯.sql
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '鐢ㄦ埛ID',
  username VARCHAR(64) NOT NULL COMMENT '鐢ㄦ埛鍚?,
  password_hash VARCHAR(255) NOT NULL COMMENT '瀵嗙爜鍝堝笇',
  nickname VARCHAR(64) DEFAULT NULL COMMENT '鏄电О',
  phone VARCHAR(20) DEFAULT NULL COMMENT '鎵嬫満鍙?,
  email VARCHAR(128) DEFAULT NULL COMMENT '閭',
  avatar_url VARCHAR(512) DEFAULT NULL COMMENT '澶村儚URL',
  user_type VARCHAR(32) NOT NULL COMMENT '鐢ㄦ埛绫诲瀷锛歋TUDENT/COMPANY/ADMIN',
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '鐘舵€侊細NORMAL/DISABLED/PENDING',
  last_login_time DATETIME(3) DEFAULT NULL COMMENT '鏈€杩戠櫥褰曟椂闂?,
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '鏇存柊鏃堕棿',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鍒涘缓浜?,
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鏇存柊浜?,
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎锛?鏈垹闄わ紝1宸插垹闄?,
  remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  UNIQUE KEY uk_sys_user_phone (phone),
  UNIQUE KEY uk_sys_user_email (email),
  KEY idx_sys_user_type_status (user_type, status),
  KEY idx_sys_user_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢ㄦ埛琛?;

CREATE TABLE sys_role (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '瑙掕壊ID',
  role_code VARCHAR(64) NOT NULL COMMENT '瑙掕壊缂栫爜',
  role_name VARCHAR(64) NOT NULL COMMENT '瑙掕壊鍚嶇О',
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '鐘舵€?,
  sort INT NOT NULL DEFAULT 0 COMMENT '鎺掑簭',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '鏇存柊鏃堕棿',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鍒涘缓浜?,
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鏇存柊浜?,
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎',
  remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_code (role_code),
  KEY idx_sys_role_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='瑙掕壊琛?;

CREATE TABLE sys_menu (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '鑿滃崟ID',
  parent_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '鐖剁骇鑿滃崟ID',
  menu_name VARCHAR(64) NOT NULL COMMENT '鑿滃崟鍚嶇О',
  menu_type VARCHAR(32) NOT NULL DEFAULT 'MENU' COMMENT '绫诲瀷锛欴IR/MENU/BUTTON/API',
  path VARCHAR(255) DEFAULT NULL COMMENT '鍓嶇璺敱',
  component VARCHAR(255) DEFAULT NULL COMMENT '鍓嶇缁勪欢',
  permission VARCHAR(128) DEFAULT NULL COMMENT '鏉冮檺鏍囪瘑',
  visible TINYINT NOT NULL DEFAULT 1 COMMENT '鏄惁鏄剧ず',
  sort INT NOT NULL DEFAULT 0 COMMENT '鎺掑簭',
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '鐘舵€?,
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '鏇存柊鏃堕棿',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鍒涘缓浜?,
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鏇存柊浜?,
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎',
  remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_menu_permission (permission),
  KEY idx_sys_menu_parent (parent_id),
  KEY idx_sys_menu_type_status (menu_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鑿滃崟鏉冮檺琛?;

CREATE TABLE sys_user_role (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '鐢ㄦ埛ID',
  role_id BIGINT UNSIGNED NOT NULL COMMENT '瑙掕壊ID',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_role (user_id, role_id),
  KEY idx_sys_user_role_user (user_id),
  KEY idx_sys_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢ㄦ埛瑙掕壊鍏宠仈琛?;

CREATE TABLE sys_role_menu (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  role_id BIGINT UNSIGNED NOT NULL COMMENT '瑙掕壊ID',
  menu_id BIGINT UNSIGNED NOT NULL COMMENT '鑿滃崟ID',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_menu (role_id, menu_id),
  KEY idx_sys_role_menu_role (role_id),
  KEY idx_sys_role_menu_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='瑙掕壊鑿滃崟鍏宠仈琛?;

-- ============================================================
-- 3. Student tables
-- ============================================================

CREATE TABLE student_profile (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '瀛︾敓璧勬枡ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '鐢ㄦ埛ID',
  real_name VARCHAR(64) NOT NULL COMMENT '鐪熷疄濮撳悕',
  gender VARCHAR(16) DEFAULT NULL COMMENT '鎬у埆锛歁ALE/FEMALE/UNKNOWN',
  school VARCHAR(128) NOT NULL COMMENT '瀛︽牎',
  major VARCHAR(128) NOT NULL COMMENT '涓撲笟',
  grade VARCHAR(32) DEFAULT NULL COMMENT '骞寸骇',
  education VARCHAR(32) DEFAULT NULL COMMENT '瀛﹀巻',
  city VARCHAR(64) DEFAULT NULL COMMENT '鏈熸湜鍩庡競',
  job_intention VARCHAR(255) DEFAULT NULL COMMENT '姹傝亴鎰忓悜',
  advantage TEXT DEFAULT NULL COMMENT '涓汉浼樺娍',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '鏇存柊鏃堕棿',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鍒涘缓浜?,
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鏇存柊浜?,
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎',
  remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
  PRIMARY KEY (id),
  UNIQUE KEY uk_student_profile_user (user_id),
  KEY idx_student_profile_school_major (school, major),
  KEY idx_student_profile_city (city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='瀛︾敓璧勬枡琛?;

CREATE TABLE student_skill (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '瀛︾敓鎶€鑳絀D',
  student_id BIGINT UNSIGNED NOT NULL COMMENT '瀛︾敓鐢ㄦ埛ID',
  skill_name VARCHAR(64) NOT NULL COMMENT '鎶€鑳藉悕绉?,
  skill_level VARCHAR(32) DEFAULT NULL COMMENT '鐔熺粌绋嬪害',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '鏇存柊鏃堕棿',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎',
  PRIMARY KEY (id),
  UNIQUE KEY uk_student_skill (student_id, skill_name),
  KEY idx_student_skill_name (skill_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='瀛︾敓鎶€鑳借〃';

-- ============================================================
-- 4. Company tables
-- ============================================================

CREATE TABLE company_profile (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '浼佷笟璧勬枡ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '浼佷笟鐢ㄦ埛ID',
  company_name VARCHAR(128) NOT NULL COMMENT '浼佷笟鍚嶇О',
  industry VARCHAR(64) DEFAULT NULL COMMENT '琛屼笟',
  scale VARCHAR(64) DEFAULT NULL COMMENT '瑙勬ā',
  city VARCHAR(64) DEFAULT NULL COMMENT '鍩庡競',
  address VARCHAR(255) DEFAULT NULL COMMENT '璇︾粏鍦板潃',
  contact_name VARCHAR(64) NOT NULL COMMENT '鑱旂郴浜?,
  contact_phone VARCHAR(20) NOT NULL COMMENT '鑱旂郴鐢佃瘽',
  license_file_id BIGINT UNSIGNED DEFAULT NULL COMMENT '浼佷笟璧勮川鏂囦欢ID',
  audit_status VARCHAR(32) NOT NULL DEFAULT 'UNVERIFIED' COMMENT '瀹℃牳鐘舵€?,
  audit_reason VARCHAR(500) DEFAULT NULL COMMENT '鏈€杩戝鏍稿師鍥?,
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '鏇存柊鏃堕棿',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鍒涘缓浜?,
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鏇存柊浜?,
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎',
  remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
  PRIMARY KEY (id),
  UNIQUE KEY uk_company_profile_user (user_id),
  KEY idx_company_profile_audit_status (audit_status),
  KEY idx_company_profile_name (company_name),
  KEY idx_company_profile_city_industry (city, industry)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='浼佷笟璧勬枡琛?;

CREATE TABLE company_audit (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '浼佷笟瀹℃牳璁板綍ID',
  company_id BIGINT UNSIGNED NOT NULL COMMENT '浼佷笟璧勬枡ID',
  audit_status VARCHAR(32) NOT NULL COMMENT '瀹℃牳缁撴灉锛欰PPROVED/REJECTED',
  audit_reason VARCHAR(500) DEFAULT NULL COMMENT '瀹℃牳鍘熷洜',
  auditor_id BIGINT UNSIGNED DEFAULT NULL COMMENT '瀹℃牳浜篒D',
  audit_time DATETIME(3) DEFAULT NULL COMMENT '瀹℃牳鏃堕棿',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (id),
  KEY idx_company_audit_company (company_id),
  KEY idx_company_audit_status_time (audit_status, audit_time),
  KEY idx_company_audit_auditor (auditor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='浼佷笟瀹℃牳璁板綍琛?;

-- ============================================================
-- 5. File and resume tables
-- ============================================================

CREATE TABLE file_object (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '鏂囦欢ID',
  owner_id BIGINT UNSIGNED NOT NULL COMMENT '涓婁紶浜篒D',
  biz_type VARCHAR(32) NOT NULL COMMENT '涓氬姟绫诲瀷锛歊ESUME/AVATAR/LICENSE/JOB_ATTACHMENT',
  bucket_name VARCHAR(64) NOT NULL COMMENT 'MinIO Bucket',
  object_name VARCHAR(255) NOT NULL COMMENT '瀵硅薄鍚嶇О',
  original_name VARCHAR(255) NOT NULL COMMENT '鍘熷鏂囦欢鍚?,
  file_size BIGINT UNSIGNED NOT NULL COMMENT '鏂囦欢澶у皬锛屽崟浣嶅瓧鑺?,
  content_type VARCHAR(128) NOT NULL COMMENT 'MIME绫诲瀷',
  file_ext VARCHAR(32) NOT NULL COMMENT '鏂囦欢鍚庣紑',
  access_url VARCHAR(512) DEFAULT NULL COMMENT '璁块棶URL',
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '鐘舵€侊細NORMAL/DELETED',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '鏇存柊鏃堕棿',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鍒涘缓浜?,
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鏇存柊浜?,
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎',
  remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
  PRIMARY KEY (id),
  UNIQUE KEY uk_file_object_name (bucket_name, object_name),
  KEY idx_file_owner_biz (owner_id, biz_type),
  KEY idx_file_status (status),
  KEY idx_file_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鏂囦欢瀵硅薄琛?;

CREATE TABLE resume (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '绠€鍘咺D',
  student_id BIGINT UNSIGNED NOT NULL COMMENT '瀛︾敓鐢ㄦ埛ID',
  resume_name VARCHAR(128) NOT NULL COMMENT '绠€鍘嗗悕绉?,
  file_id BIGINT UNSIGNED NOT NULL COMMENT '鏂囦欢ID',
  is_default TINYINT NOT NULL DEFAULT 0 COMMENT '鏄惁榛樿绠€鍘?,
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '鐘舵€侊細NORMAL/DISABLED',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '鏇存柊鏃堕棿',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鍒涘缓浜?,
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鏇存柊浜?,
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎',
  remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
  PRIMARY KEY (id),
  KEY idx_resume_student (student_id),
  KEY idx_resume_file (file_id),
  KEY idx_resume_student_default (student_id, is_default),
  KEY idx_resume_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绠€鍘嗚〃';

-- ============================================================
-- 6. Job tables
-- ============================================================

CREATE TABLE job (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '宀椾綅ID',
  company_id BIGINT UNSIGNED NOT NULL COMMENT '浼佷笟璧勬枡ID',
  title VARCHAR(128) NOT NULL COMMENT '宀椾綅鍚嶇О',
  category VARCHAR(64) DEFAULT NULL COMMENT '宀椾綅鍒嗙被',
  city VARCHAR(64) NOT NULL COMMENT '宸ヤ綔鍩庡競',
  salary_min INT UNSIGNED DEFAULT NULL COMMENT '鏈€浣庤柂璧?,
  salary_max INT UNSIGNED DEFAULT NULL COMMENT '鏈€楂樿柂璧?,
  salary_unit VARCHAR(32) NOT NULL DEFAULT 'DAY' COMMENT '钖祫鍗曚綅锛欴AY/MONTH',
  education VARCHAR(32) DEFAULT NULL COMMENT '瀛﹀巻瑕佹眰',
  experience VARCHAR(32) DEFAULT NULL COMMENT '缁忛獙瑕佹眰',
  description TEXT NOT NULL COMMENT '宀椾綅鎻忚堪',
  requirement TEXT NOT NULL COMMENT '宀椾綅瑕佹眰',
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '宀椾綅鐘舵€?,
  audit_reason VARCHAR(500) DEFAULT NULL COMMENT '瀹℃牳鍘熷洜',
  expire_time DATETIME(3) DEFAULT NULL COMMENT '杩囨湡鏃堕棿',
  view_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '娴忚鏁?,
  apply_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '鎶曢€掓暟',
  favorite_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '鏀惰棌鏁?,
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '鏇存柊鏃堕棿',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鍒涘缓浜?,
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鏇存柊浜?,
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎',
  remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
  PRIMARY KEY (id),
  KEY idx_job_company (company_id),
  KEY idx_job_status_city (status, city),
  KEY idx_job_category_status (category, status),
  KEY idx_job_expire_time (expire_time),
  KEY idx_job_create_time (create_time),
  KEY idx_job_hot (status, view_count, apply_count, favorite_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宀椾綅琛?;

CREATE TABLE job_tag (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '宀椾綅鏍囩ID',
  job_id BIGINT UNSIGNED NOT NULL COMMENT '宀椾綅ID',
  tag_name VARCHAR(64) NOT NULL COMMENT '鏍囩鍚嶇О',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎',
  PRIMARY KEY (id),
  UNIQUE KEY uk_job_tag (job_id, tag_name),
  KEY idx_job_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宀椾綅鏍囩琛?;

CREATE TABLE job_favorite (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '鏀惰棌ID',
  student_id BIGINT UNSIGNED NOT NULL COMMENT '瀛︾敓鐢ㄦ埛ID',
  job_id BIGINT UNSIGNED NOT NULL COMMENT '宀椾綅ID',
  favorite_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鏀惰棌鏃堕棿',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎',
  PRIMARY KEY (id),
  UNIQUE KEY uk_job_favorite_student_job (student_id, job_id),
  KEY idx_job_favorite_job (job_id),
  KEY idx_job_favorite_student_time (student_id, favorite_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宀椾綅鏀惰棌琛?;

CREATE TABLE job_view_history (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '娴忚璁板綍ID',
  student_id BIGINT UNSIGNED DEFAULT NULL COMMENT '瀛︾敓鐢ㄦ埛ID锛屾父瀹㈠彲涓虹┖',
  job_id BIGINT UNSIGNED NOT NULL COMMENT '宀椾綅ID',
  view_ip VARCHAR(64) DEFAULT NULL COMMENT '娴忚IP',
  view_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '娴忚鏃堕棿',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (id),
  KEY idx_job_view_student_time (student_id, view_time),
  KEY idx_job_view_job_time (job_id, view_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宀椾綅娴忚璁板綍琛?;

-- ============================================================
-- 7. Application tables
-- ============================================================

CREATE TABLE job_application (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '鎶曢€扞D',
  student_id BIGINT UNSIGNED NOT NULL COMMENT '瀛︾敓鐢ㄦ埛ID',
  company_id BIGINT UNSIGNED NOT NULL COMMENT '浼佷笟璧勬枡ID',
  job_id BIGINT UNSIGNED NOT NULL COMMENT '宀椾綅ID',
  resume_id BIGINT UNSIGNED NOT NULL COMMENT '绠€鍘咺D',
  status VARCHAR(32) NOT NULL DEFAULT 'DELIVERED' COMMENT '鎶曢€掔姸鎬?,
  apply_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鎶曢€掓椂闂?,
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '鏇存柊鏃堕棿',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鍒涘缓浜?,
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鏇存柊浜?,
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎',
  remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
  PRIMARY KEY (id),
  UNIQUE KEY uk_application_student_job (student_id, job_id),
  KEY idx_application_student_status (student_id, status),
  KEY idx_application_company_status (company_id, status),
  KEY idx_application_job_status (job_id, status),
  KEY idx_application_resume (resume_id),
  KEY idx_application_apply_time (apply_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宀椾綅鎶曢€掕〃';

CREATE TABLE application_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '鎶曢€掔姸鎬佹棩蹇桰D',
  application_id BIGINT UNSIGNED NOT NULL COMMENT '鎶曢€扞D',
  before_status VARCHAR(32) DEFAULT NULL COMMENT '鍙樻洿鍓嶇姸鎬?,
  after_status VARCHAR(32) NOT NULL COMMENT '鍙樻洿鍚庣姸鎬?,
  operator_id BIGINT UNSIGNED NOT NULL COMMENT '鎿嶄綔浜篒D',
  operator_type VARCHAR(32) NOT NULL COMMENT '鎿嶄綔浜虹被鍨嬶細STUDENT/COMPANY/ADMIN/SYSTEM',
  reason VARCHAR(500) DEFAULT NULL COMMENT '鍙樻洿鍘熷洜',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (id),
  KEY idx_application_log_application (application_id),
  KEY idx_application_log_operator (operator_id, operator_type),
  KEY idx_application_log_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鎶曢€掔姸鎬佹棩蹇楄〃';

-- ============================================================
-- 8. Interview tables
-- ============================================================

CREATE TABLE interview_slot (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '闈㈣瘯鍦烘ID',
  company_id BIGINT UNSIGNED NOT NULL COMMENT '浼佷笟璧勬枡ID',
  job_id BIGINT UNSIGNED NOT NULL COMMENT '宀椾綅ID',
  title VARCHAR(128) NOT NULL COMMENT '鍦烘鏍囬',
  start_time DATETIME(3) NOT NULL COMMENT '寮€濮嬫椂闂?,
  end_time DATETIME(3) NOT NULL COMMENT '缁撴潫鏃堕棿',
  capacity INT UNSIGNED NOT NULL COMMENT '鎬诲悕棰?,
  remain_count INT UNSIGNED NOT NULL COMMENT '鍓╀綑鍚嶉',
  interview_type VARCHAR(32) NOT NULL DEFAULT 'ONLINE' COMMENT '闈㈣瘯鏂瑰紡锛歄NLINE/OFFLINE',
  location VARCHAR(255) DEFAULT NULL COMMENT '闈㈣瘯鍦扮偣鎴栦細璁摼鎺?,
  description VARCHAR(1000) DEFAULT NULL COMMENT '闈㈣瘯璇存槑',
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN' COMMENT '鐘舵€侊細OPEN/FULL/CLOSED/EXPIRED',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '鏇存柊鏃堕棿',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鍒涘缓浜?,
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鏇存柊浜?,
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎',
  remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
  PRIMARY KEY (id),
  KEY idx_slot_job_status_time (job_id, status, start_time),
  KEY idx_slot_company_status (company_id, status),
  KEY idx_slot_start_time (start_time),
  KEY idx_slot_end_time (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='闈㈣瘯鍦烘琛?;

CREATE TABLE interview_booking (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '闈㈣瘯棰勭害ID',
  slot_id BIGINT UNSIGNED NOT NULL COMMENT '鍦烘ID',
  application_id BIGINT UNSIGNED NOT NULL COMMENT '鎶曢€扞D',
  student_id BIGINT UNSIGNED NOT NULL COMMENT '瀛︾敓鐢ㄦ埛ID',
  company_id BIGINT UNSIGNED NOT NULL COMMENT '浼佷笟璧勬枡ID',
  job_id BIGINT UNSIGNED NOT NULL COMMENT '宀椾綅ID',
  status VARCHAR(32) NOT NULL DEFAULT 'BOOKED' COMMENT '棰勭害鐘舵€侊細BOOKED/CANCELED/FINISHED',
  booking_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '棰勭害鏃堕棿',
  cancel_reason VARCHAR(500) DEFAULT NULL COMMENT '鍙栨秷鍘熷洜',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '鏇存柊鏃堕棿',
  create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鍒涘缓浜?,
  update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '鏇存柊浜?,
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎',
  remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='闈㈣瘯棰勭害琛?;

-- ============================================================
-- 9. Message and MQ tables
-- ============================================================

CREATE TABLE message (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '娑堟伅ID',
  message_id VARCHAR(128) NOT NULL COMMENT '涓氬姟娑堟伅鍞竴ID锛岀敤浜庡箓绛?,
  receiver_id BIGINT UNSIGNED NOT NULL COMMENT '鎺ユ敹浜篒D',
  sender_id BIGINT UNSIGNED DEFAULT NULL COMMENT '鍙戦€佷汉ID',
  message_type VARCHAR(32) NOT NULL COMMENT '娑堟伅绫诲瀷锛欰PPLICATION/AUDIT/INTERVIEW/SYSTEM',
  title VARCHAR(128) NOT NULL COMMENT '鏍囬',
  content TEXT NOT NULL COMMENT '鍐呭',
  business_type VARCHAR(64) DEFAULT NULL COMMENT '鍏宠仈涓氬姟绫诲瀷',
  business_id BIGINT UNSIGNED DEFAULT NULL COMMENT '鍏宠仈涓氬姟ID',
  read_status VARCHAR(32) NOT NULL DEFAULT 'UNREAD' COMMENT '闃呰鐘舵€侊細UNREAD/READ/DELETED',
  read_time DATETIME(3) DEFAULT NULL COMMENT '闃呰鏃堕棿',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '鏇存柊鏃堕棿',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎',
  PRIMARY KEY (id),
  UNIQUE KEY uk_message_message_id (message_id),
  KEY idx_message_receiver_status (receiver_id, read_status),
  KEY idx_message_receiver_time (receiver_id, create_time),
  KEY idx_message_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绔欏唴淇¤〃';

CREATE TABLE mq_message_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'MQ娑堟伅鏃ュ織ID',
  message_id VARCHAR(128) NOT NULL COMMENT '娑堟伅鍞竴ID',
  message_type VARCHAR(64) NOT NULL COMMENT '娑堟伅绫诲瀷',
  business_id BIGINT UNSIGNED DEFAULT NULL COMMENT '涓氬姟ID',
  send_status VARCHAR(32) DEFAULT NULL COMMENT '鐢熶骇渚у彂閫佺姸鎬侊細SENDING/RETRYING/SENT/SEND_FAILED',
  send_exchange VARCHAR(128) DEFAULT NULL COMMENT '鍙戦€佷氦鎹㈡満',
  send_routing_key VARCHAR(128) DEFAULT NULL COMMENT '鍙戦€佽矾鐢遍敭',
  message_body TEXT DEFAULT NULL COMMENT '鍘熷娑堟伅浣?JSON)',
  consume_status VARCHAR(32) NOT NULL DEFAULT 'INIT' COMMENT '娑堣垂渚х姸鎬侊細INIT/CONSUMED/CONSUME_FAILED',
  send_retry_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '鐢熶骇渚у彂閫侀噸璇曟鏁?,
  consume_retry_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '娑堣垂渚ч噸璇曟鏁?,
  error_message TEXT DEFAULT NULL COMMENT '閿欒淇℃伅',
  consume_time DATETIME(3) DEFAULT NULL COMMENT '娑堣垂鏃堕棿',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (id),
  UNIQUE KEY uk_mq_message_id (message_id),
  KEY idx_mq_status_time (consume_status, create_time),
  KEY idx_mq_send_retry (send_status, update_time, send_retry_count),
  KEY idx_mq_business (message_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MQ娑堟伅娑堣垂鏃ュ織琛?;

-- ============================================================
-- 10. Logs and configuration tables
-- ============================================================

CREATE TABLE operation_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '鎿嶄綔鏃ュ織ID',
  user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '鎿嶄綔浜篒D',
  username VARCHAR(64) DEFAULT NULL COMMENT '鎿嶄綔浜虹敤鎴峰悕',
  module VARCHAR(64) NOT NULL COMMENT '妯″潡',
  operation VARCHAR(128) NOT NULL COMMENT '鎿嶄綔',
  request_method VARCHAR(16) DEFAULT NULL COMMENT '璇锋眰鏂规硶',
  request_url VARCHAR(255) DEFAULT NULL COMMENT '璇锋眰URL',
  request_ip VARCHAR(64) DEFAULT NULL COMMENT '璇锋眰IP',
  request_param TEXT DEFAULT NULL COMMENT '璇锋眰鍙傛暟锛屾敞鎰忚劚鏁?,
  result_status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '缁撴灉鐘舵€?,
  error_message TEXT DEFAULT NULL COMMENT '閿欒淇℃伅',
  cost_time BIGINT UNSIGNED DEFAULT NULL COMMENT '鑰楁椂姣',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (id),
  KEY idx_operation_log_user_time (user_id, create_time),
  KEY idx_operation_log_module_time (module, create_time),
  KEY idx_operation_log_status_time (result_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鎿嶄綔鏃ュ織琛?;

CREATE TABLE login_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '鐧诲綍鏃ュ織ID',
  user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '鐢ㄦ埛ID',
  username VARCHAR(64) NOT NULL COMMENT '鐢ㄦ埛鍚?,
  user_type VARCHAR(32) DEFAULT NULL COMMENT '鐢ㄦ埛绫诲瀷',
  login_ip VARCHAR(64) DEFAULT NULL COMMENT '鐧诲綍IP',
  user_agent VARCHAR(512) DEFAULT NULL COMMENT 'User-Agent',
  status VARCHAR(32) NOT NULL COMMENT '鐧诲綍缁撴灉锛歋UCCESS/FAIL',
  fail_reason VARCHAR(255) DEFAULT NULL COMMENT '澶辫触鍘熷洜',
  login_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鐧诲綍鏃堕棿',
  PRIMARY KEY (id),
  KEY idx_login_log_user_time (user_id, login_time),
  KEY idx_login_log_username_time (username, login_time),
  KEY idx_login_log_status_time (status, login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐧诲綍鏃ュ織琛?;

CREATE TABLE sys_dict (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '瀛楀吀ID',
  dict_type VARCHAR(64) NOT NULL COMMENT '瀛楀吀绫诲瀷',
  dict_key VARCHAR(64) NOT NULL COMMENT '瀛楀吀閿?,
  dict_value VARCHAR(128) NOT NULL COMMENT '瀛楀吀鍊?,
  sort INT NOT NULL DEFAULT 0 COMMENT '鎺掑簭',
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '鐘舵€?,
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '鏇存柊鏃堕棿',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎',
  remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_dict_type_key (dict_type, dict_key),
  KEY idx_sys_dict_type_status (dict_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绯荤粺瀛楀吀琛?;

CREATE TABLE sys_config (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '閰嶇疆ID',
  config_key VARCHAR(128) NOT NULL COMMENT '閰嶇疆閿?,
  config_value VARCHAR(1000) NOT NULL COMMENT '閰嶇疆鍊?,
  config_desc VARCHAR(255) DEFAULT NULL COMMENT '閰嶇疆璇存槑',
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '鐘舵€?,
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '鍒涘缓鏃堕棿',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '鏇存柊鏃堕棿',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎',
  remark VARCHAR(500) DEFAULT NULL COMMENT '澶囨敞',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_config_key (config_key),
  KEY idx_sys_config_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绯荤粺閰嶇疆琛?;

-- ============================================================
-- 11. Initial data
-- ============================================================

INSERT INTO sys_role (id, role_code, role_name, status, sort, remark) VALUES
(1, 'ADMIN', '绠＄悊鍛?, 'NORMAL', 1, '绯荤粺绠＄悊鍛?),
(2, 'STUDENT', '瀛︾敓', 'NORMAL', 2, '瀛︾敓鐢ㄦ埛'),
(3, 'COMPANY', '浼佷笟HR', 'NORMAL', 3, '浼佷笟鎷涜仒鐢ㄦ埛');

-- 榛樿绠＄悊鍛?-- password_hash 璇锋浛鎹负鐪熷疄 BCrypt 鍝堝笇銆傜ず渚嬪€间笉鏄彲鐢ㄥ瘑鐮併€?INSERT INTO sys_user (
  id, username, password_hash, nickname, user_type, status, remark
) VALUES (
  1, 'admin', '$2a$10$REPLACE_WITH_BCRYPT_HASH', '绯荤粺绠＄悊鍛?, 'ADMIN', 'NORMAL', '榛樿绠＄悊鍛橈紝璇蜂慨鏀瑰瘑鐮?
);

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 鏉冮檺鑿滃崟鍒濆鍖栵紝鍖呭惈鏍稿績 API 鏉冮檺鏍囪瘑
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, permission, visible, sort, status) VALUES
(1, 0, '绯荤粺绠＄悊', 'DIR', '/admin/system', NULL, NULL, 1, 1, 'NORMAL'),
(2, 0, '鎷涜仒绠＄悊', 'DIR', '/admin/recruitment', NULL, NULL, 1, 2, 'NORMAL'),
(3, 0, '瀛︾敓涓績', 'DIR', '/student', NULL, NULL, 1, 3, 'NORMAL'),
(4, 0, '浼佷笟涓績', 'DIR', '/company', NULL, NULL, 1, 4, 'NORMAL'),

(100, 1, '鐢ㄦ埛绠＄悊', 'MENU', '/admin/users', 'admin/user/index', 'user:manage', 1, 100, 'NORMAL'),
(101, 1, '鎿嶄綔鏃ュ織', 'MENU', '/admin/logs/operation', 'admin/log/operation', 'log:view', 1, 101, 'NORMAL'),

(200, 2, '浼佷笟瀹℃牳', 'MENU', '/admin/company-audits', 'admin/company/audit', 'company:audit', 1, 200, 'NORMAL'),
(201, 2, '宀椾綅瀹℃牳', 'MENU', '/admin/job-audits', 'admin/job/audit', 'job:audit', 1, 201, 'NORMAL'),

(300, 3, '绠€鍘嗕笂浼?, 'API', NULL, NULL, 'resume:upload', 0, 300, 'NORMAL'),
(301, 3, '鎶曢€掑矖浣?, 'API', NULL, NULL, 'application:create', 0, 301, 'NORMAL'),
(302, 3, '棰勭害闈㈣瘯', 'API', NULL, NULL, 'interview:booking:create', 0, 302, 'NORMAL'),

(400, 4, '鍙戝竷宀椾綅', 'API', NULL, NULL, 'job:create', 0, 400, 'NORMAL'),
(401, 4, '淇敼宀椾綅', 'API', NULL, NULL, 'job:update', 0, 401, 'NORMAL'),
(402, 4, '鏌ョ湅鎶曢€?, 'API', NULL, NULL, 'application:view:company', 0, 402, 'NORMAL'),
(403, 4, '鍒涘缓闈㈣瘯鍦烘', 'API', NULL, NULL, 'interview:slot:create', 0, 403, 'NORMAL');

-- 瑙掕壊鎺堟潈
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
-- ADMIN
(1, 1), (1, 2), (1, 100), (1, 101), (1, 200), (1, 201),
(1, 300), (1, 301), (1, 302), (1, 400), (1, 401), (1, 402), (1, 403),
-- STUDENT
(2, 3), (2, 300), (2, 301), (2, 302),
-- COMPANY
(3, 4), (3, 400), (3, 401), (3, 402), (3, 403);

-- 鏁版嵁瀛楀吀
INSERT INTO sys_dict (dict_type, dict_key, dict_value, sort, remark) VALUES
('user_type', 'STUDENT', '瀛︾敓', 1, '鐢ㄦ埛绫诲瀷'),
('user_type', 'COMPANY', '浼佷笟', 2, '鐢ㄦ埛绫诲瀷'),
('user_type', 'ADMIN', '绠＄悊鍛?, 3, '鐢ㄦ埛绫诲瀷'),

('user_status', 'NORMAL', '姝ｅ父', 1, '鐢ㄦ埛鐘舵€?),
('user_status', 'DISABLED', '绂佺敤', 2, '鐢ㄦ埛鐘舵€?),
('user_status', 'PENDING', '寰呭畬鍠?, 3, '鐢ㄦ埛鐘舵€?),

('company_audit_status', 'UNVERIFIED', '鏈璇?, 1, '浼佷笟璁よ瘉鐘舵€?),
('company_audit_status', 'PENDING', '寰呭鏍?, 2, '浼佷笟璁よ瘉鐘舵€?),
('company_audit_status', 'APPROVED', '璁よ瘉閫氳繃', 3, '浼佷笟璁よ瘉鐘舵€?),
('company_audit_status', 'REJECTED', '璁よ瘉鎷掔粷', 4, '浼佷笟璁よ瘉鐘舵€?),

('job_status', 'DRAFT', '鑽夌', 1, '宀椾綅鐘舵€?),
('job_status', 'PENDING_REVIEW', '寰呭鏍?, 2, '宀椾綅鐘舵€?),
('job_status', 'PUBLISHED', '宸插彂甯?, 3, '宀椾綅鐘舵€?),
('job_status', 'REJECTED', '瀹℃牳鎷掔粷', 4, '宀椾綅鐘舵€?),
('job_status', 'OFFLINE', '宸蹭笅鏋?, 5, '宀椾綅鐘舵€?),
('job_status', 'EXPIRED', '宸茶繃鏈?, 6, '宀椾綅鐘舵€?),

('application_status', 'DELIVERED', '宸叉姇閫?, 1, '鎶曢€掔姸鎬?),
('application_status', 'VIEWED', '宸叉煡鐪?, 2, '鎶曢€掔姸鎬?),
('application_status', 'INTERVIEW_INVITED', '閭€绾﹂潰璇?, 3, '鎶曢€掔姸鎬?),
('application_status', 'BOOKED', '宸查绾?, 4, '鎶曢€掔姸鎬?),
('application_status', 'REJECTED', '涓嶅悎閫?, 5, '鎶曢€掔姸鎬?),
('application_status', 'CANCELED', '宸插彇娑?, 6, '鎶曢€掔姸鎬?),
('application_status', 'FINISHED', '宸插畬鎴?, 7, '鎶曢€掔姸鎬?),

('interview_slot_status', 'OPEN', '寮€鏀鹃绾?, 1, '闈㈣瘯鍦烘鐘舵€?),
('interview_slot_status', 'FULL', '宸茬害婊?, 2, '闈㈣瘯鍦烘鐘舵€?),
('interview_slot_status', 'CLOSED', '宸插叧闂?, 3, '闈㈣瘯鍦烘鐘舵€?),
('interview_slot_status', 'EXPIRED', '宸茶繃鏈?, 4, '闈㈣瘯鍦烘鐘舵€?),

('interview_booking_status', 'BOOKED', '宸查绾?, 1, '棰勭害鐘舵€?),
('interview_booking_status', 'CANCELED', '宸插彇娑?, 2, '棰勭害鐘舵€?),
('interview_booking_status', 'FINISHED', '宸插畬鎴?, 3, '棰勭害鐘舵€?),

('education', 'JUNIOR_COLLEGE', '澶т笓', 1, '瀛﹀巻'),
('education', 'BACHELOR', '鏈', 2, '瀛﹀巻'),
('education', 'MASTER', '纭曞＋', 3, '瀛﹀巻'),
('education', 'DOCTOR', '鍗氬＋', 4, '瀛﹀巻'),

('salary_unit', 'DAY', '澶?, 1, '钖祫鍗曚綅'),
('salary_unit', 'MONTH', '鏈?, 2, '钖祫鍗曚綅'),

('job_category', 'backend', '鍚庣寮€鍙?, 1, '宀椾綅鍒嗙被'),
('job_category', 'frontend', '鍓嶇寮€鍙?, 2, '宀椾綅鍒嗙被'),
('job_category', 'algorithm', '绠楁硶', 3, '宀椾綅鍒嗙被'),
('job_category', 'testing', '娴嬭瘯寮€鍙?, 4, '宀椾綅鍒嗙被'),
('job_category', 'product', '浜у搧缁忕悊', 5, '宀椾綅鍒嗙被');

-- 绯荤粺閰嶇疆
INSERT INTO sys_config (config_key, config_value, config_desc) VALUES
('file.resume.max_size_mb', '10', '绠€鍘嗘渶澶т笂浼犲ぇ灏忥紝鍗曚綅MB'),
('file.resume.allowed_ext', 'pdf,doc,docx', '绠€鍘嗗厑璁镐笂浼犳牸寮?),
('interview.cancel_before_hours', '2', '闈㈣瘯棰勭害鏈€鏅氬彇娑堟椂闂达紝鍗曚綅灏忔椂'),
('cache.job_detail.ttl_minutes', '30', '宀椾綅璇︽儏缂撳瓨鏃堕棿锛屽崟浣嶅垎閽?),
('security.login.token_ttl_hours', '24', '鐧诲綍Token鏈夋晥鏃堕棿锛屽崟浣嶅皬鏃?);

SET FOREIGN_KEY_CHECKS = 1;
