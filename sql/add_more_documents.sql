USE zhiyan_kb_agent;

INSERT INTO kb_document(space_id, title, original_filename, file_type, file_size, file_url, content_text, summary, keywords, parse_status, vector_status, status, uploader_id)
SELECT seed.space_id, seed.title, seed.original_filename, 'md', 4096, seed.file_url, seed.content_text, seed.summary, seed.keywords, 'SUCCESS', 'SUCCESS', 'NORMAL', 2
FROM (
  SELECT 1 AS space_id, 'Java 异常处理与日志规范' AS title, 'java-exception-log.md' AS original_filename, 'mock/java-exception-log.md' AS file_url,
  'Java 服务异常处理必须区分业务异常、参数异常、依赖异常和系统异常。业务异常用于表达可预期的业务状态，例如权限不足、资源不存在、状态不允许流转，应该返回明确错误码和可读提示；系统异常代表程序缺陷或基础设施故障，需要记录完整堆栈并告警。Controller 层不直接捕获大段异常，统一交给 GlobalExceptionHandler 处理。Service 层只在需要补充业务语义、回滚事务或降级时捕获异常，捕获后必须保留原始 cause，禁止吞掉异常。日志分为 info、warn、error 三类：info 记录关键业务路径和外部调用摘要；warn 记录可恢复但需要关注的异常，例如重试、限流、降级；error 记录会影响用户结果或数据一致性的异常。日志内容必须包含 traceId、用户或租户标识、业务主键、外部系统名称和耗时，禁止打印密码、token、身份证、手机号全量等敏感信息。对于批处理任务，需要在开始、结束、失败和跳过数据时记录数量统计，便于排查。' AS content_text,
  'Java 异常分类、统一处理、日志级别和敏感信息保护规范。' AS summary, 'Java,异常处理,日志,GlobalExceptionHandler,traceId' AS keywords
  UNION ALL SELECT 2, 'MySQL 慢查询排查手册', 'mysql-slow-query.md', 'mock/mysql-slow-query.md',
  '慢查询排查应先确认问题范围：是单个 SQL 慢、某个接口慢，还是数据库整体负载高。第一步查看接口日志中的 SQL 耗时、返回行数和调用频率，确认是否存在 N+1 查询、循环内查询或一次性加载过多数据。第二步使用 explain 分析执行计划，重点关注 type、possible_keys、key、rows、filtered 和 Extra。type 如果是 ALL 或 index，通常意味着全表扫描或索引扫描，需要结合 where 条件优化索引。Extra 中出现 Using temporary 或 Using filesort，要检查排序字段、分组字段和联合索引顺序。联合索引设计遵循等值条件在前、范围条件靠后、排序字段尽量纳入索引的原则。对于分页查询，深分页应避免 limit offset 过大，可改为基于游标或上一页最大 id 查询。对于统计类查询，如果实时性要求不高，应优先使用汇总表或异步统计。上线前必须在接近生产数据量的环境验证执行计划，避免小数据量下误判性能。',
  'MySQL 慢查询定位流程、explain 字段解读和索引优化建议。', 'MySQL,慢查询,explain,索引,深分页'
  UNION ALL SELECT 3, 'Redis 缓存一致性设计指南', 'redis-consistency.md', 'mock/redis-consistency.md',
  '缓存一致性设计应先明确数据是否允许短暂不一致。对于用户资料、配置项、商品详情等读多写少数据，推荐采用先更新数据库、再删除缓存的策略。删除缓存失败必须记录重试任务，避免旧缓存长期存在。对于高并发写场景，不建议同时更新数据库和缓存，因为并发顺序可能导致旧值覆盖新值。对于强一致要求较高的场景，应缩短缓存过期时间，并在写入后通过消息队列或 binlog 订阅进行二次删除。缓存 Key 应包含业务域、对象类型和版本，例如 order:detail:v2:1001，结构变更时通过版本隔离旧缓存。缓存值建议存储必要字段，避免把大对象直接塞入 Redis。热点 Key 需要设置逻辑过期或互斥重建，防止缓存击穿。空值缓存可以缓解穿透，但必须设置较短 TTL，并区分真实空数据和异常查询。所有缓存变更需要评估 TTL、内存占用、穿透风险、击穿风险和降级方案。',
  'Redis 缓存一致性、删除缓存、TTL、热点 Key 和空值缓存设计。', 'Redis,缓存一致性,TTL,热点Key,缓存穿透'
  UNION ALL SELECT 4, '新人本地开发环境搭建指南', 'newcomer-local-env.md', 'mock/newcomer-local-env.md',
  '新人搭建本地开发环境时，先确认基础软件版本：JDK 使用团队指定 LTS 版本，Maven 使用统一 settings 文件，Node 与 npm 版本跟随前端项目说明，MySQL 和 Redis 使用本地或 docker-compose 均可。拉取代码后先阅读 README、环境变量说明和数据库初始化脚本，不要直接修改默认配置文件中的公共参数。后端启动前需要创建数据库、执行 schema.sql 和 init_data.sql，确认 application.yml 中数据源、Redis 地址和上传目录正确。前端启动前执行 npm install，再运行 npm run dev，并确认代理地址指向本地后端。首次启动失败优先检查端口占用、数据库权限、Redis 连接、JDK 版本和 Maven 依赖下载。完成启动后，新人需要用演示账号登录，验证仪表盘、知识空间、文档上传、智能问答和操作日志等核心页面。遇到问题时应记录错误日志、复现步骤、当前分支、环境版本和已尝试方案，再发给导师或提交到未解决问题。',
  '新人本地环境准备、项目启动、验证路径和问题记录规范。', '新人,本地环境,JDK,Maven,Node,启动'
  UNION ALL SELECT 5, '生产发布前检查清单', 'release-checklist.md', 'mock/release-checklist.md',
  '生产发布前必须完成发布单、代码合并、配置确认、数据库变更审核、回滚方案和通知计划。代码层面需要确认目标分支已合并最新主干，CI 构建和自动化测试通过，关键接口完成自测和联调。数据库变更必须提供可重复执行的 SQL，包含索引、字段默认值、历史数据处理和回滚脚本，禁止在高峰期执行大表阻塞 DDL。配置层面需要确认环境变量、第三方服务地址、开关配置、限流参数和日志级别，不允许把本地配置带到生产。发布过程中先部署灰度实例，观察健康检查、错误日志、核心接口耗时、数据库连接池、Redis 命中率和 JVM 指标。灰度稳定后再分批扩大发布范围。发布后至少观察 30 分钟，确认无异常告警和用户反馈。若出现核心链路不可用、错误率持续升高或数据异常，必须立即执行回滚方案，并保留现场日志用于复盘。',
  '生产发布前检查、灰度验证、监控观察和回滚规范。', '发布,生产,灰度,回滚,检查清单'
  UNION ALL SELECT 6, '线上问题反馈模板', 'incident-feedback-template.md', 'mock/incident-feedback-template.md',
  '线上问题反馈需要包含问题现象、影响范围、发生时间、用户或租户、复现步骤、相关截图、请求参数、traceId 和初步判断。问题现象要描述用户实际感知，例如页面白屏、接口 500、数据未刷新、登录失败，而不是只写系统异常。影响范围需要说明是单个用户、某个部门、全部用户，还是某个知识空间或文档类型。排查时先判断是否能复现，再根据 traceId 串联前端日志、网关日志、后端日志和数据库慢查询。反馈给研发时应附上环境、浏览器、账号角色、请求时间和具体操作路径。临时处理方案、根因分析和长期修复方案要分开记录，避免把 workaround 当成最终解决。问题关闭前必须确认用户验收、监控恢复和相关文档是否需要补充。如果问题暴露知识库缺失，应同步创建未解决问题或新增 FAQ，避免同类问题重复出现。',
  '线上问题反馈字段、排查路径、临时处理和知识沉淀要求。', 'FAQ,线上问题,反馈模板,traceId,复盘'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM kb_document existing WHERE existing.title = seed.title AND existing.status = 'NORMAL'
);

INSERT INTO kb_document_chunk(document_id, space_id, chunk_index, content, token_count, embedding_text, vector_id, status)
SELECT d.id, d.space_id, 0, d.content_text, CHAR_LENGTH(d.content_text), d.content_text, CONCAT('supplement-vector-', d.id, '-0'), 'NORMAL'
FROM kb_document d
WHERE d.title IN ('Java 异常处理与日志规范', 'MySQL 慢查询排查手册', 'Redis 缓存一致性设计指南', '新人本地开发环境搭建指南', '生产发布前检查清单', '线上问题反馈模板')
  AND NOT EXISTS (
    SELECT 1 FROM kb_document_chunk c WHERE c.document_id = d.id AND c.chunk_index = 0 AND c.status = 'NORMAL'
  );

UPDATE kb_space s
SET document_count = (
  SELECT COUNT(*) FROM kb_document d WHERE d.space_id = s.id AND d.status = 'NORMAL'
)
WHERE s.id BETWEEN 1 AND 6;

INSERT INTO kb_faq(space_id, document_id, question, answer, tags, create_type, status)
SELECT seed.space_id, d.id, seed.question, seed.answer, seed.tags, 'MANUAL', 'NORMAL'
FROM (
  SELECT 1 AS space_id, 'Java 异常处理与日志规范' AS title, '业务异常和系统异常有什么区别？' AS question, '业务异常是可预期的业务状态，应返回明确错误码；系统异常通常代表程序缺陷或基础设施故障，需要记录堆栈并告警。' AS answer, 'Java,异常处理' AS tags
  UNION ALL SELECT 2, 'MySQL 慢查询排查手册', '慢查询排查先看哪些 explain 字段？', '重点看 type、key、rows、filtered 和 Extra。若出现全表扫描、Using temporary 或 Using filesort，需要结合条件和排序优化索引。', 'MySQL,慢查询'
  UNION ALL SELECT 3, 'Redis 缓存一致性设计指南', '缓存一致性推荐什么策略？', '读多写少场景推荐先更新数据库、再删除缓存；删除失败要记录重试任务，强一致场景可结合消息队列或 binlog 二次删除。', 'Redis,缓存一致性'
  UNION ALL SELECT 4, '新人本地开发环境搭建指南', '新人本地启动失败怎么记录问题？', '记录错误日志、复现步骤、当前分支、环境版本和已尝试方案，再发给导师或沉淀到未解决问题。', '新人,本地环境'
  UNION ALL SELECT 5, '生产发布前检查清单', '生产发布前必须准备什么？', '必须准备发布单、配置确认、数据库变更审核、回滚方案、灰度验证和通知计划。', '发布,回滚'
  UNION ALL SELECT 6, '线上问题反馈模板', '线上问题反馈要包含哪些信息？', '需要包含问题现象、影响范围、发生时间、账号角色、复现步骤、截图、请求参数、traceId 和初步判断。', 'FAQ,线上问题'
) seed
JOIN kb_document d ON d.title = seed.title AND d.status = 'NORMAL'
WHERE NOT EXISTS (
  SELECT 1 FROM kb_faq f WHERE f.question = seed.question AND f.status = 'NORMAL'
);
