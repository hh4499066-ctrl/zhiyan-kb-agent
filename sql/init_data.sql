USE zhiyan_kb_agent;

INSERT INTO sys_department(id, name, parent_id, leader_id, description, status) VALUES
(1, '研发中心', 0, 2, '企业研发知识沉淀与协作部门', 'ENABLED'),
(2, '后端组', 1, 2, 'Java、数据库、缓存与服务端规范', 'ENABLED'),
(3, '前端组', 1, 3, 'Vue、前端工程化与交互规范', 'ENABLED'),
(4, '测试运维组', 1, 4, '测试、部署、监控与故障处理', 'ENABLED');

INSERT INTO sys_user(id, username, password, real_name, email, phone, role, department_id, status) VALUES
(1, 'admin', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '管理员', 'admin@demo.com', '13800000001', 'admin', 1, 'ENABLED'),
(2, 'manager', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '知识库管理员', 'manager@demo.com', '13800000002', 'kb_manager', 2, 'ENABLED'),
(3, 'zhangsan', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '张三', 'zhangsan@demo.com', '13800000003', 'employee', 2, 'ENABLED'),
(4, 'newcomer', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '新人同学', 'newcomer@demo.com', '13800000004', 'newcomer', 2, 'ENABLED');

INSERT INTO kb_space(id, name, code, description, owner_id, visibility, department_id, status, document_count, qa_count) VALUES
(1, 'Java 开发规范', 'java-standard', 'Java 后端开发命名、分层、异常和日志规范。', 2, 'PUBLIC', 2, 'NORMAL', 2, 12),
(2, 'MySQL 使用规范', 'mysql-standard', '索引、事务、SQL 审查与慢查询处理规范。', 2, 'PUBLIC', 2, 'NORMAL', 2, 8),
(3, 'Redis 使用规范', 'redis-standard', '缓存设计、Key 命名、过期策略和穿透击穿处理。', 2, 'PUBLIC', 2, 'NORMAL', 2, 7),
(4, '新人培训', 'newcomer-training', '新人入职第一周学习路径和团队基础资料。', 2, 'PUBLIC', 1, 'NORMAL', 2, 10),
(5, '项目部署文档', 'deploy-docs', '本地启动、测试环境部署和常见启动失败处理。', 2, 'PUBLIC', 4, 'NORMAL', 2, 9),
(6, '常见问题 FAQ', 'faq', '团队高频问题、答疑沉淀和处理建议。', 2, 'PUBLIC', 1, 'NORMAL', 2, 15);

INSERT INTO kb_space_member(space_id, user_id, member_role, status) VALUES
(1,2,'MANAGER','NORMAL'),(2,2,'MANAGER','NORMAL'),(3,2,'MANAGER','NORMAL'),(4,2,'MANAGER','NORMAL'),(5,2,'MANAGER','NORMAL'),(6,2,'MANAGER','NORMAL'),
(1,3,'READER','NORMAL'),(2,3,'READER','NORMAL'),(3,3,'READER','NORMAL'),(4,4,'READER','NORMAL');

INSERT INTO kb_document(id, space_id, title, original_filename, file_type, file_size, file_url, content_text, summary, keywords, parse_status, vector_status, status, uploader_id) VALUES
(1,1,'Java 接口命名规范','java-api.md','md',1024,'mock/java-api.md','Java 接口命名应表达业务语义。Controller 接口路径使用小写中划线，Service 方法使用动词开头，例如 createUser、updateDocument。DTO 用于请求参数，VO 用于响应展示。禁止在 Controller 中堆叠业务逻辑，复杂逻辑必须下沉到 Service。','接口命名、分层和 DTO/VO 使用规范。','Java,接口,命名,DTO,VO','SUCCESS','SUCCESS','NORMAL',2),
(2,1,'Spring Boot 项目分层规范','spring-layer.md','md',1024,'mock/spring-layer.md','Spring Boot 项目建议分为 controller、service、mapper、entity、dto、vo、config、common 等包。Controller 只做参数校验和结果返回，Service 负责业务编排，Mapper 负责数据库访问。统一异常由 GlobalExceptionHandler 处理。','Spring Boot 分层和职责边界。','Spring Boot,分层,Service,Mapper','SUCCESS','SUCCESS','NORMAL',2),
(3,2,'MySQL 索引使用规范','mysql-index.md','md',1024,'mock/mysql-index.md','MySQL 索引应优先建立在高选择性字段、关联字段和排序字段上。联合索引遵循最左前缀原则，避免在索引列上使用函数。慢查询需要通过 explain 分析 type、key、rows 和 extra。','MySQL 索引设计和慢查询分析规范。','MySQL,索引,explain,慢查询','SUCCESS','SUCCESS','NORMAL',2),
(4,2,'MySQL 事务使用规范','mysql-tx.md','md',1024,'mock/mysql-tx.md','事务边界应放在 Service 层，避免事务中执行耗时外部调用。默认使用 READ COMMITTED 或 REPEATABLE READ，并注意死锁重试。批量更新要控制单次数据量。','事务边界、隔离级别和死锁处理。','MySQL,事务,死锁','SUCCESS','SUCCESS','NORMAL',2),
(5,3,'Redis 缓存使用规范','redis-cache.md','md',1024,'mock/redis-cache.md','Redis Key 命名建议使用 业务域:对象:标识 的结构，例如 user:profile:1001。缓存必须设置合理过期时间。热点数据要考虑击穿保护，空值缓存可缓解缓存穿透。','Redis Key、过期和缓存穿透击穿规范。','Redis,缓存,Key,过期','SUCCESS','SUCCESS','NORMAL',2),
(6,3,'Redis 分布式锁规范','redis-lock.md','md',1024,'mock/redis-lock.md','分布式锁必须设置过期时间和唯一请求标识，释放锁时校验标识。业务执行时间可能超过锁过期时间时需要续期。优先使用成熟客户端实现。','Redis 分布式锁安全使用规范。','Redis,分布式锁,续期','SUCCESS','SUCCESS','NORMAL',2),
(7,4,'新人入职第一周学习指南','newcomer-week1.md','md',1024,'mock/newcomer-week1.md','新人第一周需要了解团队组织结构、项目代码仓库、分支开发规范、启动本地环境、阅读 Java/MySQL/Redis 规范，并完成一个简单需求。遇到问题应先查询知识库，再记录未解决问题。','新人第一周学习路径。','新人,学习路径,入职','SUCCESS','SUCCESS','NORMAL',2),
(8,4,'团队研发流程介绍','dev-flow.md','md',1024,'mock/dev-flow.md','需求开发流程包括需求评审、技术方案、编码、自测、Code Review、联调、测试和发布。所有变更必须关联任务编号，提交信息应清晰描述变更内容。','团队研发流程说明。','研发流程,Code Review,发布','SUCCESS','SUCCESS','NORMAL',2),
(9,5,'项目启动失败常见原因','startup-failed.md','md',1024,'mock/startup-failed.md','项目启动失败时优先检查端口占用、数据库连接、Redis 连接、配置文件 profile、依赖版本和本地 JDK 版本。端口冲突可修改 server.port 或释放占用进程。','项目启动失败排查清单。','启动失败,端口,配置,JDK','SUCCESS','SUCCESS','NORMAL',2),
(10,5,'测试环境部署流程','deploy-test.md','md',1024,'mock/deploy-test.md','测试环境部署需要先执行构建，上传 jar 包，更新配置，重启服务，检查健康接口和关键日志。发布后需要通知测试人员验证核心流程。','测试环境部署步骤。','部署,测试环境,健康检查','SUCCESS','SUCCESS','NORMAL',2),
(11,6,'Git 分支开发规范','git-branch.md','md',1024,'mock/git-branch.md','Git 分支命名建议使用 feature/任务号-描述、fix/任务号-描述、hotfix/描述。开发前从主干拉取最新代码，提交前解决冲突并通过本地测试。','Git 分支命名和协作规范。','Git,分支,协作','SUCCESS','SUCCESS','NORMAL',2),
(12,6,'代码提交规范','commit.md','md',1024,'mock/commit.md','代码提交信息应包含类型和说明，例如 feat: 新增知识库问答接口，fix: 修复文档解析异常。一次提交只做一类变更，避免混入无关格式化。','提交信息和提交粒度规范。','提交规范,commit,代码质量','SUCCESS','SUCCESS','NORMAL',2);

INSERT INTO kb_document_chunk(document_id, space_id, chunk_index, content, token_count, embedding_text, vector_id, status)
SELECT id, space_id, 0, content_text, CHAR_LENGTH(content_text), content_text, CONCAT('init-vector-', id, '-0'), 'NORMAL' FROM kb_document;

INSERT INTO kb_faq(space_id, document_id, question, answer, tags, create_type, status) VALUES
(1,1,'Controller 里可以写复杂业务逻辑吗？','不建议。Controller 只做参数接收、校验和响应封装，复杂业务应下沉到 Service。','Java,分层','MANUAL','NORMAL'),
(5,9,'项目启动失败先查什么？','优先检查端口占用、数据库和 Redis 连接、profile 配置、依赖版本和 JDK 版本。','启动失败,部署','MANUAL','NORMAL');

INSERT INTO chat_session(id, user_id, space_id, title, status) VALUES
(1,3,1,'Java 规范问答','NORMAL'),
(2,4,4,'新人入职助手','NORMAL');

INSERT INTO chat_record(user_id, space_id, session_id, question, rewritten_question, answer, references_json, confidence, unresolved, favorite) VALUES
(3,1,'demo-session-1','Controller 可以写业务逻辑吗？','Controller 可以写复杂业务逻辑吗？','不建议，业务逻辑应放在 Service 层。','[]',0.88,0,1),
(3,5,'demo-session-2','项目启动失败怎么办？','项目启动失败怎么办？','先检查端口、数据库、Redis、profile 和 JDK 版本。','[]',0.90,0,0);

INSERT INTO chat_feedback(record_id, user_id, helpful, comment) VALUES
(1,3,1,'回答清楚'),(2,3,1,'排查步骤有用');

INSERT INTO user_long_term_memory(user_id, memory_type, content, embedding_text, vector_id, status) VALUES
(3,'IDENTITY','我是 Java 后端开发者，回答代码问题时优先使用 Java 示例','我是 Java 后端开发者，回答代码问题时优先使用 Java 示例','mock-memory-1','NORMAL'),
(3,'PREFERENCE','我对 Redis 不熟，请解释得通俗一点','我对 Redis 不熟，请解释得通俗一点','mock-memory-2','NORMAL');

INSERT INTO unresolved_question(user_id, space_id, question, rewritten_question, reason, status) VALUES
(3,2,'公司内部读写分离中间件怎么配置？','公司内部读写分离中间件怎么配置？','知识库暂无内部中间件配置文档','PENDING'),
(4,4,'新人试用期考核标准是什么？','新人试用期考核标准是什么？','新人培训空间缺少考核标准文档','PENDING');

INSERT INTO onboarding_plan(user_id, role_type, title, description, plan_content, recommended_documents) VALUES
(4,'后端','后端新人 7 天学习路径','初始化演示学习计划','第 1 天：了解团队项目结构\n第 2 天：阅读 Java 开发规范\n第 3 天：阅读 MySQL 使用规范\n第 4 天：阅读 Redis 使用规范\n第 5 天：本地启动项目\n第 6 天：完成简单需求\n第 7 天：提交代码并 Code Review','新人入职第一周学习指南、Java 接口命名规范、MySQL 索引使用规范');
