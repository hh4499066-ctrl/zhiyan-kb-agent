CREATE DATABASE IF NOT EXISTS zhiyan_kb_agent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE zhiyan_kb_agent;

DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS onboarding_plan;
DROP TABLE IF EXISTS unresolved_question;
DROP TABLE IF EXISTS user_long_term_memory;
DROP TABLE IF EXISTS chat_feedback;
DROP TABLE IF EXISTS chat_record;
DROP TABLE IF EXISTS chat_session;
DROP TABLE IF EXISTS kb_faq;
DROP TABLE IF EXISTS kb_document_chunk;
DROP TABLE IF EXISTS document_processing_task;
DROP TABLE IF EXISTS kb_document;
DROP TABLE IF EXISTS kb_space_member;
DROP TABLE IF EXISTS kb_space;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_department;

CREATE TABLE sys_department (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  parent_id BIGINT DEFAULT 0,
  leader_id BIGINT NULL,
  description VARCHAR(255),
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_parent (parent_id),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  real_name VARCHAR(80) NOT NULL,
  email VARCHAR(120),
  phone VARCHAR(30),
  role VARCHAR(30) NOT NULL,
  department_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_role (role),
  INDEX idx_department (department_id),
  INDEX idx_status (status),
  CONSTRAINT fk_user_department FOREIGN KEY (department_id) REFERENCES sys_department(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE kb_space (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  code VARCHAR(80) NOT NULL UNIQUE,
  description VARCHAR(500),
  owner_id BIGINT NOT NULL,
  visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
  department_id BIGINT,
  status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  document_count INT NOT NULL DEFAULT 0,
  qa_count INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_owner (owner_id),
  INDEX idx_department (department_id),
  INDEX idx_status (status),
  CONSTRAINT fk_space_owner FOREIGN KEY (owner_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
  CONSTRAINT fk_space_department FOREIGN KEY (department_id) REFERENCES sys_department(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE kb_space_member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  space_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  member_role VARCHAR(30) NOT NULL DEFAULT 'READER',
  status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_space_user (space_id, user_id),
  INDEX idx_user (user_id),
  CONSTRAINT fk_space_member_space FOREIGN KEY (space_id) REFERENCES kb_space(id) ON DELETE CASCADE,
  CONSTRAINT fk_space_member_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE kb_document (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  space_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  file_type VARCHAR(20) NOT NULL,
  file_size BIGINT NOT NULL DEFAULT 0,
  file_url VARCHAR(500) NOT NULL,
  content_text MEDIUMTEXT,
  summary TEXT,
  keywords VARCHAR(500),
  parse_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  vector_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  uploader_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_space_status (space_id, status),
  INDEX idx_uploader (uploader_id),
  CONSTRAINT fk_document_space FOREIGN KEY (space_id) REFERENCES kb_space(id) ON DELETE RESTRICT,
  CONSTRAINT fk_document_uploader FOREIGN KEY (uploader_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
  FULLTEXT KEY ft_content (title, content_text)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE kb_document_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  space_id BIGINT NOT NULL,
  chunk_index INT NOT NULL,
  content TEXT NOT NULL,
  token_count INT DEFAULT 0,
  embedding_text TEXT,
  vector_id VARCHAR(100),
  status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_document (document_id),
  UNIQUE KEY uk_document_chunk_index (document_id, chunk_index),
  INDEX idx_space_status (space_id, status),
  CONSTRAINT fk_chunk_document FOREIGN KEY (document_id) REFERENCES kb_document(id) ON DELETE CASCADE,
  CONSTRAINT fk_chunk_space FOREIGN KEY (space_id) REFERENCES kb_space(id) ON DELETE RESTRICT,
  FULLTEXT KEY ft_chunk (content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE document_processing_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  file_url VARCHAR(500) NOT NULL,
  file_type VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  retry_count INT NOT NULL DEFAULT 0,
  max_retries INT NOT NULL DEFAULT 3,
  last_error VARCHAR(1000),
  last_process_time DATETIME,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_status_retry (status, retry_count),
  INDEX idx_document (document_id),
  CONSTRAINT fk_processing_task_document FOREIGN KEY (document_id) REFERENCES kb_document(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE kb_faq (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  space_id BIGINT NOT NULL,
  document_id BIGINT,
  question VARCHAR(500) NOT NULL,
  answer TEXT NOT NULL,
  tags VARCHAR(255),
  create_type VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
  status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_space_status (space_id, status),
  INDEX idx_document (document_id),
  UNIQUE KEY uk_faq_document_question (document_id, question),
  CONSTRAINT fk_faq_space FOREIGN KEY (space_id) REFERENCES kb_space(id) ON DELETE RESTRICT,
  CONSTRAINT fk_faq_document FOREIGN KEY (document_id) REFERENCES kb_document(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chat_session (
  id VARCHAR(80) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  space_id BIGINT,
  title VARCHAR(120) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_status (user_id, status),
  CONSTRAINT fk_chat_session_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
  CONSTRAINT fk_chat_session_space FOREIGN KEY (space_id) REFERENCES kb_space(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chat_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  space_id BIGINT NOT NULL,
  session_id VARCHAR(80) NOT NULL,
  question VARCHAR(1000) NOT NULL,
  rewritten_question VARCHAR(1200),
  answer MEDIUMTEXT,
  references_json MEDIUMTEXT,
  confidence DECIMAL(5,2),
  unresolved TINYINT(1) NOT NULL DEFAULT 0,
  favorite TINYINT(1) NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_time (user_id, create_time),
  INDEX idx_space_time (space_id, create_time),
  INDEX idx_session (session_id),
  INDEX idx_unresolved (unresolved),
  UNIQUE KEY uk_chat_user_session_question (user_id, session_id, question(191)),
  CONSTRAINT fk_chat_record_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
  CONSTRAINT fk_chat_record_space FOREIGN KEY (space_id) REFERENCES kb_space(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chat_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  record_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  helpful TINYINT(1) NOT NULL,
  comment VARCHAR(500),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_record (record_id),
  INDEX idx_user (user_id),
  UNIQUE KEY uk_feedback_record_user (record_id, user_id),
  CONSTRAINT fk_chat_feedback_record FOREIGN KEY (record_id) REFERENCES chat_record(id) ON DELETE CASCADE,
  CONSTRAINT fk_chat_feedback_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_long_term_memory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  memory_type VARCHAR(30) NOT NULL,
  content VARCHAR(1000) NOT NULL,
  embedding_text TEXT,
  vector_id VARCHAR(100),
  status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_status (user_id, status),
  UNIQUE KEY uk_memory_user_type_content (user_id, memory_type, content(191)),
  CONSTRAINT fk_memory_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE unresolved_question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  space_id BIGINT NOT NULL,
  question VARCHAR(1000) NOT NULL,
  rewritten_question VARCHAR(1200),
  reason VARCHAR(500),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  resolver_id BIGINT,
  resolve_note VARCHAR(500),
  related_document_id BIGINT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_space_status (space_id, status),
  INDEX idx_user (user_id),
  INDEX idx_resolver (resolver_id),
  UNIQUE KEY uk_unresolved_user_space_question (user_id, space_id, question(191)),
  CONSTRAINT fk_unresolved_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
  CONSTRAINT fk_unresolved_space FOREIGN KEY (space_id) REFERENCES kb_space(id) ON DELETE RESTRICT,
  CONSTRAINT fk_unresolved_resolver FOREIGN KEY (resolver_id) REFERENCES sys_user(id) ON DELETE SET NULL,
  CONSTRAINT fk_unresolved_document FOREIGN KEY (related_document_id) REFERENCES kb_document(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE onboarding_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_type VARCHAR(30) NOT NULL,
  title VARCHAR(150) NOT NULL,
  description VARCHAR(500),
  plan_content TEXT,
  recommended_documents TEXT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user (user_id),
  UNIQUE KEY uk_onboarding_user_title (user_id, title),
  CONSTRAINT fk_onboarding_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  module_name VARCHAR(80),
  operation VARCHAR(120),
  detail VARCHAR(1000),
  ip VARCHAR(60),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_time (user_id, create_time),
  CONSTRAINT fk_operation_log_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
