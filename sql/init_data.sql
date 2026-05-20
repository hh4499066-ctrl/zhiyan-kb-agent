USE zhiyan_kb_agent;

INSERT IGNORE INTO sys_department(id, name, parent_id, leader_id, description, status) VALUES
(1, 'R&D Center', 0, NULL, 'Enterprise R&D knowledge and collaboration department', 'ENABLED'),
(2, 'Backend Team', 1, NULL, 'Java, database, cache and server-side standards', 'ENABLED'),
(3, 'Frontend Team', 1, NULL, 'Vue, frontend engineering and interaction standards', 'ENABLED'),
(4, 'QA & Ops Team', 1, NULL, 'Testing, deployment, monitoring and incident handling', 'ENABLED');

INSERT IGNORE INTO sys_user(id, username, password, real_name, email, phone, role, department_id, status) VALUES
(1, 'admin', '$2b$10$GoFKJ2R5Fphu/eqLOK7qeOx5Jq74AUz9dh4uHMxuWYAkUjIYPN0Zy', 'Administrator', 'admin@demo.com', '13800000001', 'admin', 1, 'ENABLED'),
(2, 'manager', '$2b$10$GoFKJ2R5Fphu/eqLOK7qeOx5Jq74AUz9dh4uHMxuWYAkUjIYPN0Zy', 'Knowledge Manager', 'manager@demo.com', '13800000002', 'kb_manager', 2, 'ENABLED'),
(3, 'zhangsan', '$2b$10$GoFKJ2R5Fphu/eqLOK7qeOx5Jq74AUz9dh4uHMxuWYAkUjIYPN0Zy', 'Zhang San', 'zhangsan@demo.com', '13800000003', 'employee', 2, 'ENABLED'),
(4, 'newcomer', '$2b$10$GoFKJ2R5Fphu/eqLOK7qeOx5Jq74AUz9dh4uHMxuWYAkUjIYPN0Zy', 'Newcomer', 'newcomer@demo.com', '13800000004', 'newcomer', 2, 'ENABLED');

INSERT IGNORE INTO kb_space(id, name, code, description, owner_id, visibility, department_id, status, document_count, qa_count) VALUES
(1, 'Java Development Standards', 'java-standard', 'Java backend naming, layering, exception and logging standards.', 2, 'PUBLIC', 2, 'NORMAL', 0, 0),
(2, 'MySQL Usage Standards', 'mysql-standard', 'Indexing, transactions, SQL review and slow query handling.', 2, 'PUBLIC', 2, 'NORMAL', 0, 0),
(3, 'Redis Usage Standards', 'redis-standard', 'Cache design, key naming, expiration strategy and penetration handling.', 2, 'PUBLIC', 2, 'NORMAL', 0, 0),
(4, 'Newcomer Training', 'newcomer-training', 'First-week onboarding path and team basics.', 2, 'PUBLIC', 1, 'NORMAL', 0, 0),
(5, 'Deployment Docs', 'deploy-docs', 'Local startup, test deployment and common failure handling.', 2, 'PUBLIC', 4, 'NORMAL', 0, 0),
(6, 'FAQ', 'faq', 'Frequently asked questions and troubleshooting suggestions.', 2, 'PUBLIC', 1, 'NORMAL', 0, 0);

INSERT IGNORE INTO kb_space_member(space_id, user_id, member_role, status) VALUES
(1,2,'MANAGER','NORMAL'),(2,2,'MANAGER','NORMAL'),(3,2,'MANAGER','NORMAL'),(4,2,'MANAGER','NORMAL'),(5,2,'MANAGER','NORMAL'),(6,2,'MANAGER','NORMAL'),
(1,3,'READER','NORMAL'),(2,3,'READER','NORMAL'),(3,3,'READER','NORMAL'),(4,4,'READER','NORMAL');

INSERT IGNORE INTO kb_document(id, space_id, title, original_filename, file_type, file_size, file_url, content_text, summary, keywords, parse_status, vector_status, status, uploader_id) VALUES
(1,1,'Java API Naming Standards','java-api.md','md',1024,'mock/java-api.md','Java API paths should use lowercase kebab-case. Service methods should start with verbs such as createUser and updateDocument. Use DTOs for request parameters and VOs for response views. Avoid complex business logic in controllers.','API naming, layering and DTO/VO usage.','Java,API,DTO,VO','SUCCESS','SUCCESS','NORMAL',2),
(2,1,'Spring Boot Layering Standards','spring-layer.md','md',1024,'mock/spring-layer.md','Spring Boot projects should separate controller, service, mapper, entity, dto, vo, config and common packages. Controllers validate parameters and return responses. Services orchestrate business logic. Mappers access the database.','Spring Boot layering and responsibility boundaries.','Spring Boot,layering,Service,Mapper','SUCCESS','SUCCESS','NORMAL',2),
(3,2,'MySQL Indexing Standards','mysql-index.md','md',1024,'mock/mysql-index.md','MySQL indexes should prioritize selective columns, join columns and ordering columns. Use EXPLAIN to inspect type, key, rows and Extra. Avoid functions on indexed columns.','MySQL index design and slow query analysis.','MySQL,index,explain','SUCCESS','SUCCESS','NORMAL',2),
(4,2,'MySQL Transaction Standards','mysql-tx.md','md',1024,'mock/mysql-tx.md','Transaction boundaries belong in the service layer. Avoid slow external calls inside transactions. Batch updates should control size and handle deadlock retry.','Transaction boundaries, isolation and deadlock handling.','MySQL,transaction,deadlock','SUCCESS','SUCCESS','NORMAL',2),
(5,3,'Redis Cache Standards','redis-cache.md','md',1024,'mock/redis-cache.md','Redis keys should include domain, object type and identifier. Cache entries need reasonable TTL. Hot keys and null-value cache should be handled deliberately.','Redis key naming, TTL and cache penetration handling.','Redis,cache,TTL','SUCCESS','SUCCESS','NORMAL',2),
(6,3,'Redis Lock Standards','redis-lock.md','md',1024,'mock/redis-lock.md','Distributed locks must set expiration and unique request tokens. Release locks only after token validation. Prefer mature client implementations.','Safe Redis distributed lock usage.','Redis,lock,token','SUCCESS','SUCCESS','NORMAL',2),
(7,4,'Newcomer First Week Guide','newcomer-week1.md','md',1024,'mock/newcomer-week1.md','Newcomers should understand team structure, repository layout, branch workflow, local environment, Java/MySQL/Redis standards and complete a simple task in the first week.','Newcomer first-week learning path.','newcomer,onboarding,learning','SUCCESS','SUCCESS','NORMAL',2),
(8,4,'R&D Workflow','dev-flow.md','md',1024,'mock/dev-flow.md','The R&D workflow includes requirement review, technical design, coding, self-testing, code review, integration, QA and release. Every change should link to a task.','Team development workflow.','workflow,code review,release','SUCCESS','SUCCESS','NORMAL',2),
(9,5,'Startup Failure Checklist','startup-failed.md','md',1024,'mock/startup-failed.md','When startup fails, check port conflicts, MySQL connection, Redis connection, profile configuration, dependency versions and JDK version first.','Startup failure troubleshooting checklist.','startup,port,config,JDK','SUCCESS','SUCCESS','NORMAL',2),
(10,5,'Test Deployment Process','deploy-test.md','md',1024,'mock/deploy-test.md','Test deployment requires build, jar upload, configuration update, service restart, health checks and key log review. Notify QA after release.','Test environment deployment steps.','deployment,test,health check','SUCCESS','SUCCESS','NORMAL',2),
(11,6,'Git Branch Standards','git-branch.md','md',1024,'mock/git-branch.md','Branch names should use feature/task-description, fix/task-description or hotfix/description. Pull latest main before development and pass local tests before commit.','Git branch naming and collaboration.','Git,branch,collaboration','SUCCESS','SUCCESS','NORMAL',2),
(12,6,'Commit Message Standards','commit.md','md',1024,'mock/commit.md','Commit messages should include type and clear description, for example feat: add knowledge QA API or fix: handle document parsing exception. Keep one change type per commit.','Commit message and commit scope standards.','commit,quality','SUCCESS','SUCCESS','NORMAL',2);

INSERT IGNORE INTO kb_document_chunk(document_id, space_id, chunk_index, content, token_count, embedding_text, vector_id, status)
SELECT id, space_id, 0, content_text, CHAR_LENGTH(content_text), content_text, CONCAT('init-vector-', id, '-0'), 'NORMAL'
FROM kb_document;

INSERT IGNORE INTO kb_faq(space_id, document_id, question, answer, tags, create_type, status) VALUES
(1,1,'Can controllers contain complex business logic?','No. Controllers should receive parameters, validate inputs and wrap responses. Complex business logic belongs in services.','Java,layering','MANUAL','NORMAL'),
(5,9,'What should I check first when startup fails?','Check port conflicts, database and Redis connectivity, active profile, dependency versions and JDK version first.','startup,deployment','MANUAL','NORMAL'),
(2,3,'Which EXPLAIN fields matter for slow query analysis?','Focus on type, key, rows, filtered and Extra. Full table scans, temporary tables and filesort usually require index or query optimization.','MySQL,explain','MANUAL','NORMAL'),
(3,5,'What is the recommended cache consistency strategy?','For read-heavy data, update the database first and then delete the cache. Failed cache deletion should be retried.','Redis,cache consistency','MANUAL','NORMAL'),
(4,7,'How should newcomers record local setup issues?','Record error logs, reproduction steps, current branch, environment versions and attempted fixes before asking for help.','newcomer,local setup','MANUAL','NORMAL');

INSERT IGNORE INTO chat_session(id, user_id, space_id, title, status) VALUES
('demo-session-1',3,1,'Java Standards Q&A','NORMAL'),
('demo-session-2',4,4,'Newcomer Assistant','NORMAL');

INSERT IGNORE INTO chat_record(user_id, space_id, session_id, question, rewritten_question, answer, references_json, confidence, unresolved, favorite) VALUES
(3,1,'demo-session-1','Can controllers contain business logic?','Can controllers contain complex business logic?','No. Business logic should be placed in services.','[]',0.88,0,1),
(3,5,'demo-session-2','What should I do when startup fails?','How to troubleshoot startup failure?','Check port, database, Redis, profile and JDK version first.','[]',0.90,0,0);

INSERT IGNORE INTO chat_feedback(record_id, user_id, helpful, comment) VALUES
(1,3,1,'Clear answer'),(2,3,1,'Useful checklist');

INSERT IGNORE INTO user_long_term_memory(user_id, memory_type, content, embedding_text, vector_id, status) VALUES
(3,'IDENTITY','I am a Java backend developer. Prefer Java examples for code questions.','I am a Java backend developer. Prefer Java examples for code questions.','memory-init-1','NORMAL'),
(3,'PREFERENCE','I am not familiar with Redis. Please explain Redis topics plainly.','I am not familiar with Redis. Please explain Redis topics plainly.','memory-init-2','NORMAL');

INSERT IGNORE INTO unresolved_question(user_id, space_id, question, rewritten_question, reason, status) VALUES
(3,2,'How should internal read-write splitting middleware be configured?','How should internal read-write splitting middleware be configured?','No internal middleware configuration document exists yet.','PENDING'),
(4,4,'What is the probation assessment standard for newcomers?','What is the probation assessment standard for newcomers?','The newcomer training space lacks probation assessment standards.','PENDING');

INSERT IGNORE INTO onboarding_plan(user_id, role_type, title, description, plan_content, recommended_documents) VALUES
(4,'backend','Backend newcomer 7-day learning path','Initial demo learning plan.','Day 1: Understand team and project structure\nDay 2: Read Java standards\nDay 3: Read MySQL standards\nDay 4: Read Redis standards\nDay 5: Start project locally\nDay 6: Complete a simple task\nDay 7: Submit code and join code review','Newcomer First Week Guide, Java API Naming Standards, MySQL Indexing Standards');

UPDATE kb_space s
SET document_count = (
    SELECT COUNT(*) FROM kb_document d WHERE d.space_id = s.id AND d.status = 'NORMAL'
), qa_count = (
    SELECT COUNT(*) FROM kb_faq f WHERE f.space_id = s.id AND f.status = 'NORMAL'
);
