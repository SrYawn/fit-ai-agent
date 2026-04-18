# 架构说明

## 运行拓扑
- 主 API 服务：
  - 路径：`src/main/java/com/zsr/fitaiagent`
  - 职责：工作流编排、控制器、Agent、本地工具、SSE 输出。
- 数据库 MCP 服务：
  - 路径：`fitness-db-mcp-server/`
  - 职责：通过 MCP 对外暴露基于 MySQL 的数据查询工具。
- 图片搜索 MCP 服务：
  - 路径：`yu-image-search-mcp-server/`
  - 职责：通过 SSE MCP 暴露图片搜索能力。

## 内部分层
- `controller/`：HTTP 和 SSE 入口。
- `graph/`：工作流图构建与编排。
- `agent/`：意图识别、画像生成、计划生成、动作指导、陪伴激励等 Agent。
- `tools/`：本地工具与工具注册逻辑。
- `rag/`：健身知识检索与索引支持。
- `chatmemory/`：对话记忆存储（内存级 session 记忆、文件持久化记忆）。
- `config/`：Spring 与集成配置。

## 关键设计约束
- 意图路由必须足够稳定，保证工作流能够正确分支。
- 用户画像生成必须优先使用真实数据库数据。
- 若画像落地失败，下游计划生成必须显式降级。
- 普通工作流接口应返回结构化 JSON 结果，避免前端从长文本中反推状态。
- SSE 接口应以 token 级增量文本为主，并在 payload 中附带节点元数据；工作流阶段信息只能作为元数据，不能替代文本流本身。
- 多轮会话通过 sessionId 维护 session 级上下文记忆（内存级，不跨会话持久化），历史以文本注入 agent 的 userPrompt，不修改 BaseAgent 生命周期。
- 陪伴激励 Agent 采用分层 Prompt 工程体系（角色约束 / 情境感知 / 激励策略 / 安全控制），结合 MCP 训练数据 + RAG 激励知识 + LLM 情绪检测。

## 跨模块依赖规则
- 根服务通过 `src/main/resources/mcp-servers.json` 发现并接入 MCP 工具。
- `fitness-db-mcp-server` 依赖可访问的 MySQL。
- `yu-image-search-mcp-server` 在涉及图片搜索的端到端场景中需要单独启动。
