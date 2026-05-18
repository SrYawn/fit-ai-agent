# 架构说明

## 运行拓扑
- **主 API 服务**：
  - 路径：`src/main/java/com/zsr/fitaiagent`
  - 技术：Java 21 + Spring Boot 3.5.11 + Spring AI 1.1.2 + Spring AI Alibaba 1.1.2.0
  - 职责：工作流图编排、控制器、Agent、本地工具、RAG、SSE 输出。
- **前端应用**：
  - 路径：`frontend/`
  - 技术：React 18 + Vite 6 + Tailwind CSS 3 + React Router 6
  - 职责：用户交互界面，对话、登录、Markdown 消息渲染。
- **数据库 MCP 服务**：
  - 路径：`fitness-db-mcp-server/`
  - 模式：stdio
  - 职责：通过 MCP 对外暴露基于 MySQL 的数据查询工具（用户画像、训练记录、伤病信息）。
- **图片搜索 MCP 服务**：
  - 路径：`yu-image-search-mcp-server/`
  - 模式：HTTP（Spring Boot 应用）
  - 职责：通过 MCP 暴露健身动作图片搜索能力。

## 内部分层
- `controller/`：HTTP 和 SSE 入口。
  - `FitnessWorkflowController` — 主工作流接口（普通 + 流式）
  - `FitnessKnowledgeController` — 知识库管理接口
  - `AuthController` — 用户认证（BCrypt 密码校验）
  - `UserProfileController` — 用户画像接口
  - `HealthController` — 健康检查
- `graph/`：工作流图构建与编排（基于 Spring AI Alibaba Agent Framework）。
  - `FitnessWorkflowGraph` — 核心状态图，定义节点和边的路由逻辑
- `agent/`：各业务 Agent 实现。
  - `BaseAgent` — Agent 基类，定义生命周期
  - `IntentRecognitionAgent` — 意图识别与路由
  - `UserProfileAgent` — 用户画像生成
  - `PlanGenerationAgent` — 训练计划生成
  - `ActionGuidanceAgent` — 动作指导
  - `CompanionMotivationAgent` — 情感激励陪伴
  - `ChatAgent` — 通用对话
  - `ReActAgent` / `ToolCallAgent` — 通用工具调用 Agent
  - `YuManus` — 通用 ReAct Agent（类 Manus 架构）
- `tools/`：本地工具与工具注册逻辑。
  - `KnowledgeSearchTool` — RAG 知识检索
  - `MotivationKnowledgeSearchTool` — 激励知识检索
  - `EmotionDetectionTool` — 情绪检测
  - `IntentRouteTool` — 意图路由工具
  - `WebSearchTool` / `WebScrapingTool` — 网络搜索与抓取
  - `PDFGenerationTool` — PDF 报告生成
  - `FileOperationTool` / `ResourceDownloadTool` — 文件与资源操作
  - `TerminalOperationTool` / `TerminateTool` — 终端操作与终止控制
  - `ToolRegistration` — 工具统一注册
- `rag/`：健身知识检索与索引支持。
  - `FitnessDocumentLoader` — 文档加载（Markdown 格式）
  - `QueryRewriter` — 查询改写优化
  - `MyTokenTextSplitter` — 自定义文本分割
  - `MyKeywordEnricher` — 关键词增强
- `chatmemory/`：对话记忆存储。
  - `InMemorySessionChatMemory` — 内存级 session 记忆
  - `FileBasedChatMemory` — 文件持久化记忆
- `advisor/`：Spring AI Advisor 扩展。
  - `MyLoggerAdvisor` — 日志记录
  - `ReReadingAdvisor` — Re-Reading 策略增强
- `config/`：Spring 与集成配置。
  - `ChatModelConfig` — 模型配置
  - `ChatMemoryConfig` — 记忆配置
  - `CorsConfig` — 跨域配置

## 关键设计约束
- 意图路由必须足够稳定，保证工作流能够正确分支。
- 用户画像生成必须优先使用真实数据库数据。
- 若画像落地失败，下游计划生成必须显式降级。
- 普通工作流接口应返回结构化 JSON 结果，避免前端从长文本中反推状态。
- SSE 接口应以 token 级增量文本为主，并在 payload 中附带节点元数据；工作流阶段信息只能作为元数据，不能替代文本流本身。
- 多轮会话通过 sessionId 维护 session 级上下文记忆（内存级，不跨会话持久化），历史以文本注入 agent 的 userPrompt，不修改 BaseAgent 生命周期。
- 陪伴激励 Agent 采用分层 Prompt 工程体系（角色约束 / 情境感知 / 激励策略 / 安全控制），结合 MCP 训练数据 + RAG 激励知识 + LLM 情绪检测。
- 用户认证采用简单的 BCrypt 密码校验，通过 JdbcTemplate 直接查询，无 Spring Security 过滤链。

## 跨模块依赖规则
- 根服务通过 `src/main/resources/mcp-servers.json` 发现并接入 MCP 工具。
- `fitness-db-mcp-server` 依赖可访问的 MySQL（Docker 容器 `fitness-mysql`）。
- `yu-image-search-mcp-server` 在涉及图片搜索的端到端场景中需要单独启动。
- 前端通过 Vite 代理或 CORS 配置访问后端 API。
- Elasticsearch 用于 RAG 向量存储，需通过 Docker 启动。
