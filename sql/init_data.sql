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
(1, 'Java 开发规范', 'java-standard', 'Java 后端开发命名、分层、异常和日志规范。', 2, 'PUBLIC', 2, 'NORMAL', 3, 12),
(2, 'MySQL 使用规范', 'mysql-standard', '索引、事务、SQL 审查与慢查询处理规范。', 2, 'PUBLIC', 2, 'NORMAL', 3, 8),
(3, 'Redis 使用规范', 'redis-standard', '缓存设计、Key 命名、过期策略和穿透击穿处理。', 2, 'PUBLIC', 2, 'NORMAL', 3, 7),
(4, '新人培训', 'newcomer-training', '新人入职第一周学习路径和团队基础资料。', 2, 'PUBLIC', 1, 'NORMAL', 3, 10),
(5, '项目部署文档', 'deploy-docs', '本地启动、测试环境部署和常见启动失败处理。', 2, 'PUBLIC', 4, 'NORMAL', 3, 9),
(6, '常见问题 FAQ', 'faq', '团队高频问题、答疑沉淀和处理建议。', 2, 'PUBLIC', 1, 'NORMAL', 3, 15);

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
(12,6,'代码提交规范','commit.md','md',1024,'mock/commit.md','代码提交信息应包含类型和说明，例如 feat: 新增知识库问答接口，fix: 修复文档解析异常。一次提交只做一类变更，避免混入无关格式化。','提交信息和提交粒度规范。','提交规范,commit,代码质量','SUCCESS','SUCCESS','NORMAL',2),
(13,1,'Java 异常处理与日志规范','java-exception-log.md','md',4096,'mock/java-exception-log.md','Java 服务异常处理必须区分业务异常、参数异常、依赖异常和系统异常。业务异常用于表达可预期的业务状态，例如权限不足、资源不存在、状态不允许流转，应该返回明确错误码和可读提示；系统异常代表程序缺陷或基础设施故障，需要记录完整堆栈并告警。Controller 层不直接捕获大段异常，统一交给 GlobalExceptionHandler 处理。Service 层只在需要补充业务语义、回滚事务或降级时捕获异常，捕获后必须保留原始 cause，禁止吞掉异常。日志分为 info、warn、error 三类：info 记录关键业务路径和外部调用摘要；warn 记录可恢复但需要关注的异常，例如重试、限流、降级；error 记录会影响用户结果或数据一致性的异常。日志内容必须包含 traceId、用户或租户标识、业务主键、外部系统名称和耗时，禁止打印密码、token、身份证、手机号全量等敏感信息。对于批处理任务，需要在开始、结束、失败和跳过数据时记录数量统计，便于排查。','Java 异常分类、统一处理、日志级别和敏感信息保护规范。','Java,异常处理,日志,GlobalExceptionHandler,traceId','SUCCESS','SUCCESS','NORMAL',2),
(14,2,'MySQL 慢查询排查手册','mysql-slow-query.md','md',4096,'mock/mysql-slow-query.md','慢查询排查应先确认问题范围：是单个 SQL 慢、某个接口慢，还是数据库整体负载高。第一步查看接口日志中的 SQL 耗时、返回行数和调用频率，确认是否存在 N+1 查询、循环内查询或一次性加载过多数据。第二步使用 explain 分析执行计划，重点关注 type、possible_keys、key、rows、filtered 和 Extra。type 如果是 ALL 或 index，通常意味着全表扫描或索引扫描，需要结合 where 条件优化索引。Extra 中出现 Using temporary 或 Using filesort，要检查排序字段、分组字段和联合索引顺序。联合索引设计遵循等值条件在前、范围条件靠后、排序字段尽量纳入索引的原则。对于分页查询，深分页应避免 limit offset 过大，可改为基于游标或上一页最大 id 查询。对于统计类查询，如果实时性要求不高，应优先使用汇总表或异步统计。上线前必须在接近生产数据量的环境验证执行计划，避免小数据量下误判性能。','MySQL 慢查询定位流程、explain 字段解读和索引优化建议。','MySQL,慢查询,explain,索引,深分页','SUCCESS','SUCCESS','NORMAL',2),
(15,3,'Redis 缓存一致性设计指南','redis-consistency.md','md',4096,'mock/redis-consistency.md','缓存一致性设计应先明确数据是否允许短暂不一致。对于用户资料、配置项、商品详情等读多写少数据，推荐采用先更新数据库、再删除缓存的策略。删除缓存失败必须记录重试任务，避免旧缓存长期存在。对于高并发写场景，不建议同时更新数据库和缓存，因为并发顺序可能导致旧值覆盖新值。对于强一致要求较高的场景，应缩短缓存过期时间，并在写入后通过消息队列或 binlog 订阅进行二次删除。缓存 Key 应包含业务域、对象类型和版本，例如 order:detail:v2:1001，结构变更时通过版本隔离旧缓存。缓存值建议存储必要字段，避免把大对象直接塞入 Redis。热点 Key 需要设置逻辑过期或互斥重建，防止缓存击穿。空值缓存可以缓解穿透，但必须设置较短 TTL，并区分真实空数据和异常查询。所有缓存变更需要评估 TTL、内存占用、穿透风险、击穿风险和降级方案。','Redis 缓存一致性、删除缓存、TTL、热点 Key 和空值缓存设计。','Redis,缓存一致性,TTL,热点Key,缓存穿透','SUCCESS','SUCCESS','NORMAL',2),
(16,4,'新人本地开发环境搭建指南','newcomer-local-env.md','md',4096,'mock/newcomer-local-env.md','新人搭建本地开发环境时，先确认基础软件版本：JDK 使用团队指定 LTS 版本，Maven 使用统一 settings 文件，Node 与 npm 版本跟随前端项目说明，MySQL 和 Redis 使用本地或 docker-compose 均可。拉取代码后先阅读 README、环境变量说明和数据库初始化脚本，不要直接修改默认配置文件中的公共参数。后端启动前需要创建数据库、执行 schema.sql 和 init_data.sql，确认 application.yml 中数据源、Redis 地址和上传目录正确。前端启动前执行 npm install，再运行 npm run dev，并确认代理地址指向本地后端。首次启动失败优先检查端口占用、数据库权限、Redis 连接、JDK 版本和 Maven 依赖下载。完成启动后，新人需要用演示账号登录，验证仪表盘、知识空间、文档上传、智能问答和操作日志等核心页面。遇到问题时应记录错误日志、复现步骤、当前分支、环境版本和已尝试方案，再发给导师或提交到未解决问题。','新人本地环境准备、项目启动、验证路径和问题记录规范。','新人,本地环境,JDK,Maven,Node,启动','SUCCESS','SUCCESS','NORMAL',2),
(17,5,'生产发布前检查清单','release-checklist.md','md',4096,'mock/release-checklist.md','生产发布前必须完成发布单、代码合并、配置确认、数据库变更审核、回滚方案和通知计划。代码层面需要确认目标分支已合并最新主干，CI 构建和自动化测试通过，关键接口完成自测和联调。数据库变更必须提供可重复执行的 SQL，包含索引、字段默认值、历史数据处理和回滚脚本，禁止在高峰期执行大表阻塞 DDL。配置层面需要确认环境变量、第三方服务地址、开关配置、限流参数和日志级别，不允许把本地配置带到生产。发布过程中先部署灰度实例，观察健康检查、错误日志、核心接口耗时、数据库连接池、Redis 命中率和 JVM 指标。灰度稳定后再分批扩大发布范围。发布后至少观察 30 分钟，确认无异常告警和用户反馈。若出现核心链路不可用、错误率持续升高或数据异常，必须立即执行回滚方案，并保留现场日志用于复盘。','生产发布前检查、灰度验证、监控观察和回滚规范。','发布,生产,灰度,回滚,检查清单','SUCCESS','SUCCESS','NORMAL',2),
(18,6,'线上问题反馈模板','incident-feedback-template.md','md',4096,'mock/incident-feedback-template.md','线上问题反馈需要包含问题现象、影响范围、发生时间、用户或租户、复现步骤、相关截图、请求参数、traceId 和初步判断。问题现象要描述用户实际感知，例如页面白屏、接口 500、数据未刷新、登录失败，而不是只写系统异常。影响范围需要说明是单个用户、某个部门、全部用户，还是某个知识空间或文档类型。排查时先判断是否能复现，再根据 traceId 串联前端日志、网关日志、后端日志和数据库慢查询。反馈给研发时应附上环境、浏览器、账号角色、请求时间和具体操作路径。临时处理方案、根因分析和长期修复方案要分开记录，避免把 workaround 当成最终解决。问题关闭前必须确认用户验收、监控恢复和相关文档是否需要补充。如果问题暴露知识库缺失，应同步创建未解决问题或新增 FAQ，避免同类问题重复出现。','线上问题反馈字段、排查路径、临时处理和知识沉淀要求。','FAQ,线上问题,反馈模板,traceId,复盘','SUCCESS','SUCCESS','NORMAL',2);

INSERT INTO kb_document_chunk(document_id, space_id, chunk_index, content, token_count, embedding_text, vector_id, status)
SELECT id, space_id, 0, content_text, CHAR_LENGTH(content_text), content_text, CONCAT('init-vector-', id, '-0'), 'NORMAL' FROM kb_document;

INSERT INTO kb_faq(space_id, document_id, question, answer, tags, create_type, status) VALUES
(1,1,'Controller 里可以写复杂业务逻辑吗？','不建议。Controller 只做参数接收、校验和响应封装，复杂业务应下沉到 Service。','Java,分层','MANUAL','NORMAL'),
(5,9,'项目启动失败先查什么？','优先检查端口占用、数据库和 Redis 连接、profile 配置、依赖版本和 JDK 版本。','启动失败,部署','MANUAL','NORMAL'),
(1,13,'业务异常和系统异常有什么区别？','业务异常是可预期的业务状态，应返回明确错误码；系统异常通常代表程序缺陷或基础设施故障，需要记录堆栈并告警。','Java,异常处理','MANUAL','NORMAL'),
(2,14,'慢查询排查先看哪些 explain 字段？','重点看 type、key、rows、filtered 和 Extra。若出现全表扫描、Using temporary 或 Using filesort，需要结合条件和排序优化索引。','MySQL,慢查询','MANUAL','NORMAL'),
(3,15,'缓存一致性推荐什么策略？','读多写少场景推荐先更新数据库、再删除缓存；删除失败要记录重试任务，强一致场景可结合消息队列或 binlog 二次删除。','Redis,缓存一致性','MANUAL','NORMAL'),
(4,16,'新人本地启动失败怎么记录问题？','记录错误日志、复现步骤、当前分支、环境版本和已尝试方案，再发给导师或沉淀到未解决问题。','新人,本地环境','MANUAL','NORMAL'),
(5,17,'生产发布前必须准备什么？','必须准备发布单、配置确认、数据库变更审核、回滚方案、灰度验证和通知计划。','发布,回滚','MANUAL','NORMAL'),
(6,18,'线上问题反馈要包含哪些信息？','需要包含问题现象、影响范围、发生时间、账号角色、复现步骤、截图、请求参数、traceId 和初步判断。','FAQ,线上问题','MANUAL','NORMAL');

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
UPDATE kb_space s
SET document_count = (
    SELECT COUNT(*) FROM kb_document d WHERE d.space_id = s.id AND d.status = 'NORMAL'
), qa_count = (
    SELECT COUNT(*) FROM kb_faq f WHERE f.space_id = s.id AND f.status = 'NORMAL'
);
