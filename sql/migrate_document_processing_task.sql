USE zhiyan_kb_agent;

CREATE TABLE IF NOT EXISTS document_processing_task (
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
  INDEX idx_document (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
