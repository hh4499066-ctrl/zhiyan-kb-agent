# 智研库：企业研发知识库与 AI 智能协作体平台

智研库面向企业、工作室和研发团队，用于集中管理项目资料、研发文档、开发规范、培训资料和 FAQ，并通过 RAG、短期记忆、长期记忆和 Mock AI 智能体降低检索、培训和重复答疑成本。

## 项目背景

团队知识常分散在文档、聊天记录、个人笔记和代码仓库中，新人入职和研发答疑成本较高。本项目把知识空间、文档解析、分块、混合检索、智能问答、未解决问题沉淀和数据看板串成闭环，适合作为企业级期末综合项目演示。

## 功能模块

- 认证与权限：登录、退出、当前用户、用户管理、角色菜单。
- 接口安全：基于 `@RequireRole` 的接口级角色校验，管理员接口和知识库写操作会在后端拦截。
- 基础管理：部门管理、知识空间管理、文档管理、FAQ 管理。
- RAG 能力：文档解析、固定长度分块、Mock Embedding、Mock VectorStore、关键词 + 向量混合检索。
- AI 智能问答：默认全库自动检索，知识空间选择仅作为可选范围过滤；支持 Query Rewriting、短期记忆、长期记忆召回、引用来源、置信度、未解决问题沉淀和历史会话恢复。
- AI 文档处理：文档摘要、关键词、适用人群、阅读建议、FAQ 生成。
- 新人助手：按岗位生成 7 天学习路径和推荐文档。
- 数据看板：空间、文档、Chunk、问答、未解决问题、趋势、满意度。
- 操作审计：通过 AOP 自动记录 POST/PUT/DELETE 操作到 `operation_log`，管理员可在前端查看。

## 技术栈

- 后端：Java 17、Spring Boot 3、Spring Web、Spring Validation、MyBatis-Plus、MySQL 8、Redis、Lombok、Hutool、Maven。
- 前端：Vue 3、Vite、TypeScript、Element Plus、Axios、Pinia、Vue Router、ECharts。
- AI/RAG：`LLMClient`、`MockLLMClient`、`EmbeddingClient`、`MockEmbeddingClient`、`MockVectorStoreService`。

## 目录结构

```text
zhiyan-kb-agent
├─ backend              Spring Boot 后端
├─ frontend             Vue 3 前端
├─ sql                  schema.sql 与 init_data.sql
├─ docs                 API 文档
├─ docker-compose.yml   MySQL + Redis 演示环境
└─ README.md
```

## 数据库初始化

方式一：使用 Docker Compose：

```bash
docker compose up -d
```

方式二：手动创建 MySQL 8 数据库后执行：

```bash
mysql -uroot -p123456 < sql/schema.sql
mysql -uroot -p123456 < sql/init_data.sql
```

Redis 默认连接：`localhost:6379`。MySQL 默认连接：`localhost:3306/zhiyan_kb_agent`，账号密码为 `root / 123456`，可在 [application.yml](backend/src/main/resources/application.yml) 修改。

## 启动方式

后端：

```bash
cd backend
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

访问：`http://localhost:5173`

## 演示账号

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| admin | 123456 | 管理员 |
| manager | 123456 | 知识库管理员 |
| zhangsan | 123456 | 普通员工 |
| newcomer | 123456 | 新人 |

初始化密码以 SHA-256 形式保存；系统新增/重置用户使用 BCrypt 加密，登录逻辑兼容两种格式，便于演示数据导入。

## AI Mock 模式说明

默认无需 API Key。`MockLLMClient` 根据 Prompt 内容返回摘要、FAQ、学习计划或问答结果；`MockEmbeddingClient` 将文本转换为固定维度向量；`MockVectorStoreService` 在应用启动时从 `kb_document_chunk` 重建内存向量索引。

问答流程：

1. 读取当前会话短期记忆，包含历史摘要和最近窗口对话。
2. 使用 Query Rewriting 改写上下文依赖问题。
3. 召回用户长期记忆。
4. 在知识空间内进行混合检索。
5. 构建包含长期记忆、短期记忆、知识片段和问题的 Prompt。
6. 生成答案、引用来源和置信度。
7. 无命中时自动写入未解决问题。

短期记忆采用 Redis 存储，超过窗口后会把较早对话压缩成摘要，再保留最近 N 轮对话，避免 Prompt 无限增长。

## 接入真实大模型

项目已内置 DeepSeek OpenAI 兼容接口实现：`DeepSeekLLMClient`。不要把 API Key 写入代码或提交到仓库，建议用环境变量。

PowerShell 启动示例：

```powershell
$env:ZHIYAN_AI_MODE="real"
$env:DEEPSEEK_API_KEY="你的 DeepSeek API Key"
$env:DEEPSEEK_BASE_URL="https://api.deepseek.com"
$env:DEEPSEEK_CHAT_MODEL="deepseek-v4-flash"

cd backend
mvn spring-boot:run
```

恢复 Mock 模式：

```powershell
$env:ZHIYAN_AI_MODE="mock"
```

当前 DeepSeek 接入用于真实 LLM 回答、文档摘要、FAQ 和新人计划生成。Embedding 仍保留 Mock 实现，因为 DeepSeek 主要提供 Chat Completions；如果后续接入独立 Embedding 服务，只需替换 `EmbeddingClient`。

如果你的 DeepSeek 控制台提示模型名不支持，请以接口返回为准。当前项目默认使用 `deepseek-v4-flash`；需要更强推理效果时可改为 `deepseek-v4-pro`。

在 IDEA 的 Run Configuration 配置环境变量时，注意模型名和 Key 前后不要带空格。后端会自动 `trim()` 常见空白字符，但仍建议写成：

```text
ZHIYAN_AI_MODE=real;DEEPSEEK_API_KEY=你的Key;DEEPSEEK_BASE_URL=https://api.deepseek.com;DEEPSEEK_CHAT_MODEL=deepseek-v4-flash
```

后续接其他真实模型时保留业务层不变：

- 实现新的 `LLMClient`，替换 `MockLLMClient`。
- 实现新的 `EmbeddingClient`，替换 `MockEmbeddingClient`。
- 替换 `VectorStoreService` 为 Milvus、Pinecone、Elasticsearch 或 OpenSearch。
- 可在 `DocumentParseService` 接入 PDF/DOCX 真实解析器。

## 项目亮点

1. 不是普通知识库 CRUD，而是完整的企业研发知识协作平台。
2. 通过 RAG 实现基于企业内部文档的智能问答，降低幻觉。
3. 通过短期记忆实现多轮对话上下文连贯。
4. 通过长期记忆实现跨会话个性化回答。
5. 通过 Query Rewriting 解决“它、这个、那怎么做”等上下文依赖问题。
6. 通过混合检索提升召回效果。
7. 通过未解决问题沉淀形成知识库优化闭环。
8. 通过新人入职助手降低团队培训成本。

## 可答辩话术

本项目的核心不是文档 CRUD，而是把企业研发文档转化为可检索、可引用、可持续优化的知识协作智能体。文档上传后会解析、清洗、分块并生成 Mock 向量，问答时结合短期记忆、长期记忆、查询重写和混合检索构建 Prompt。系统在无法回答时不会编造，而是自动沉淀为未解决问题，提醒知识库管理员补充文档，从而形成企业知识库持续优化闭环。

## 构建验证

已验证：

```bash
cd backend && mvn -DskipTests compile
cd frontend && npm run build
```

前端构建可能提示 ECharts/Element Plus chunk 体积较大，不影响演示运行。

## 权限矩阵

| 能力 | admin | kb_manager | employee | newcomer |
| --- | --- | --- | --- | --- |
| 用户/部门管理 | 支持 | 不支持 | 不支持 | 不支持 |
| 知识空间写操作 | 支持 | 支持 | 不支持 | 不支持 |
| 文档上传/解析/删除 | 支持 | 支持 | 不支持 | 不支持 |
| FAQ 写操作 | 支持 | 支持 | 不支持 | 不支持 |
| 智能问答 | 支持 | 支持 | 支持 | 支持 |
| 长期记忆 | 支持 | 支持 | 支持 | 支持 |
| 新人学习路径 | 支持 | 支持 | 支持 | 支持 |
| 未解决问题处理 | 支持 | 支持 | 不支持 | 不支持 |
| 操作日志查看 | 支持 | 不支持 | 不支持 | 不支持 |
