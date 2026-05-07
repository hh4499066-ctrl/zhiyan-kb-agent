# 智研库 API 文档

所有接口统一返回：

```json
{ "code": 200, "message": "success", "data": {} }
```

除 `POST /api/auth/login` 外，请求需携带：

```http
Authorization: Bearer <token>
```

## 认证

- `POST /api/auth/login`：登录，参数 `{ "username": "admin", "password": "123456" }`
- `POST /api/auth/logout`：退出登录
- `GET /api/auth/me`：当前用户

## 管理接口

- 用户：`GET/POST/PUT/DELETE /api/users`，`PUT /api/users/{id}/status`，`PUT /api/users/{id}/reset-password`
- 部门：`GET/POST/PUT/DELETE /api/departments`，`GET /api/departments/{id}/users`
- 知识空间：`GET/POST/PUT/DELETE /api/spaces`，`GET /api/spaces/{id}`
- FAQ：`GET/POST/PUT/DELETE /api/faqs`

## 文档

- `GET /api/documents?spaceId=1`：文档列表
- `POST /api/documents/upload`：上传文档，`multipart/form-data`，字段：`spaceId`、`title`、`file`
- `GET /api/documents/{id}`：文档详情
- `PUT /api/documents/{id}`：编辑文档
- `DELETE /api/documents/{id}`：逻辑删除文档
- `POST /api/documents/{id}/parse`：重新解析
- `GET /api/documents/{id}/chunks`：查看分块
- `POST /api/documents/{id}/ai-summary`：AI 摘要
- `POST /api/documents/{id}/generate-faq`：AI 生成 FAQ

## 智能问答

`POST /api/chat/ask`

```json
{
  "spaceId": null,
  "sessionId": "web-demo",
  "question": "项目启动失败怎么办？",
  "useMemory": true,
  "topK": 5
}
```

返回包含：

- `answer`：答案
- `rewrittenQuestion`：改写后问题
- `references`：引用文档片段
- `confidence`：置信度
- `sessionId`：会话 ID
- `recordId`：问答记录 ID
- `unresolved`：是否未解决

`spaceId` 可为空。为空时系统执行全库自动检索；传入具体空间 ID 时表示限制检索范围，但若当前空间只命中低相关内容，后端会自动跨空间兜底检索更相关的知识片段。

其他问答接口：

- `GET /api/chat/sessions`：从问答记录聚合历史会话
- `POST /api/chat/sessions`
- `DELETE /api/chat/sessions/{id}`
- `DELETE /api/chat/sessions/{sessionId}/memory`
- `GET /api/chat/records`：可传 `sessionId` 恢复某个历史会话的消息
- `GET /api/chat/records/{id}`
- `POST /api/chat/records/{id}/feedback`
- `PUT /api/chat/records/{id}/favorite`

## 记忆、新人、未解决问题、看板

- 长期记忆：`GET/POST/PUT/DELETE /api/memories`
- 新人助手：`POST /api/onboarding/generate-plan`，`GET /api/onboarding/plans`
- 未解决问题：`GET /api/unresolved`，`PUT /api/unresolved/{id}/resolve`，`PUT /api/unresolved/{id}/ignore`
- 看板：`GET /api/dashboard/overview`
- 操作日志：`GET /api/operation-logs`，仅 `admin` 可访问，支持 `moduleName/page/size` 查询参数
- AI 配置：`GET /api/ai-config`，仅 `admin` 可访问，返回当前 AI 模式、provider、baseUrl、模型名和 Key 是否已配置，不返回 Key 明文。

DeepSeek 真实模式默认模型为 `deepseek-v4-flash`。如果接口返回模型名不支持，请把环境变量 `DEEPSEEK_CHAT_MODEL` 改为当前账号支持的模型，例如 `deepseek-v4-pro`。

## 权限说明

后端通过 `@RequireRole` 做接口级权限校验：

- `admin`：全部管理能力。
- `kb_manager`：知识空间、文档、FAQ、未解决问题处理等知识库管理能力。
- `employee/newcomer`：浏览知识空间、文档、FAQ、智能问答、长期记忆和新人助手。

系统通过 AOP 自动记录 `POST/PUT/DELETE/PATCH` 操作到 `operation_log`，登录接口除外。
