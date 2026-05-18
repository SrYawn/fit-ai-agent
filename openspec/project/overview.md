# 项目概览

## 目标
本仓库实现了一个围绕多 Agent 图编排构建的智能健身助手系统。系统的核心能力是理解用户的健身相关请求，通过意图识别将其路由到合适的 Agent 处理流程，结合 RAG 知识库、用户数据库和 MCP 工具进行支撑，生成训练计划、动作指导、情感激励或闲聊回复等结果。

## 主要模块
- **后端主应用**：Spring Boot 3.5.11 API，基于 Spring AI Alibaba Agent Framework 进行图编排，协调各类 Agent。
- **前端应用**（`frontend/`）：React 18 + Vite + Tailwind CSS 构建的用户交互界面，支持对话、登录等功能。
- **fitness-db-mcp-server/**：基于 MySQL 的 MCP 服务（stdio 模式），提供用户画像、伤病和训练记录查询能力。
- **yu-image-search-mcp-server/**：图片搜索 MCP 服务（HTTP 模式），提供健身动作图片检索。
- **docker/**：本地基础设施编排，包括 MySQL 8.0、Elasticsearch 8.12、Kibana 8.12 和 Adminer。

## 主要运行链路
1. 用户通过前端界面发送消息（携带 userId 和 sessionId）。
2. 后端接收请求，进入图编排工作流。
3. `IntentRecognitionAgent` 执行意图识别。
4. 根据意图路由到对应子 Agent：
   - 计划生成 → `UserProfileAgent` → `PlanGenerationAgent`
   - 动作指导 → `ActionGuidanceAgent`
   - 情感激励 → `CompanionMotivationAgent`
   - 通用对话 → `ChatAgent`
5. 各 Agent 按需调用 MCP 工具（数据库查询、图片搜索）和 RAG 知识检索。
6. 普通接口返回结构化 JSON；流式接口返回 token 级 SSE，并在 payload 中附带节点元数据。

## 当前重点
- 保证用户画像尽可能基于真实数据落地。
- 缺少关键信息时优先明确降级，不伪造个性化内容。
- 保持工作流进度和外部依赖失败的可观测性。
- 前端交互体验的完善（登录、对话流、Markdown 渲染）。
