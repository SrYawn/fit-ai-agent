# CLAUDE.md

你当前工作的项目是：**fit-ai-agent** —— 一个基于多 Agent 图编排的智能健身助手系统。

## 主要事实来源

项目中所有长期有效的上下文、架构说明、流程规范和变更记录，都统一放在 `openspec/` 下。

开始处理任务时，先阅读 `openspec/index.md`，再按当前任务类型加载相关的项目级或流程级文档。

## 技术栈

### 后端
- Java 21 + Spring Boot 3.5.11
- Spring AI 1.1.2 + Spring AI Alibaba 1.1.2.0（DashScope 模型接入）
- Spring AI Alibaba Agent Framework（图编排）
- Ollama（本地模型支持）
- Elasticsearch 8.12（向量存储 + 知识检索）
- PGVector（备用向量存储）
- MySQL 8.0（用户数据、业务数据）
- Spring AI MCP Client（工具调用）
- Knife4j / OpenAPI 3（API 文档）
- Lombok、Hutool、iText PDF

### 前端
- React 18 + Vite 6 + Tailwind CSS 3
- React Router DOM 6
- React Markdown（消息渲染）
- 位于 `frontend/` 目录，独立的 npm 项目

### 基础设施（Docker）
- MySQL 8.0（端口 3306）
- Elasticsearch 8.12（端口 9200）
- Kibana 8.12（端口 5601）
- Adminer（端口 8080，数据库管理 UI）

## 核心架构

### Agent 体系（`agent/` 包）
- `IntentRecognitionAgent` — 意图识别，路由到对应子 Agent
- `ActionGuidanceAgent` — 动作指导
- `PlanGenerationAgent` — 健身计划生成
- `CompanionMotivationAgent` — 情感激励陪伴
- `UserProfileAgent` — 用户画像管理
- `ChatAgent` — 通用对话
- `YuManus` — 通用 ReAct Agent

### 图编排（`graph/` 包）
- `FitnessWorkflowGraph` — 基于 Spring AI Alibaba Agent Framework 的状态图工作流

### RAG（`rag/` 包）
- `FitnessDocumentLoader` — 健身知识文档加载
- `QueryRewriter` — 查询改写
- `MyTokenTextSplitter` — 文本分割
- `MyKeywordEnricher` — 关键词增强

### Tools（`tools/` 包）
- 知识检索、情绪检测、意图路由、Web 搜索/抓取
- PDF 生成、文件操作、资源下载、终端操作

### MCP 服务
- `fitness-db-mcp-server/` — 数据库操作 MCP（stdio 模式）
- `yu-image-search-mcp-server/` — 图片搜索 MCP（HTTP 模式）

## 必须遵循的工作方式

对于任何非简单改动：
1. 阅读本次任务相关的 OpenSpec 文件。
2. 在 `openspec/changes/<change-id>/` 下创建或更新变更目录。
3. 在改代码前，先补充或确认设计说明。
4. 实施代码修改。
5. 执行验证。
6. 更新 `implementation.md` 和 `test-report.md`。
7. 如果长期项目知识发生变化，同步更新 `openspec/project/` 或 `openspec/workflows/` 下的对应文档。

## 技术约束

- 优先使用 Spring AI 抽象，例如 `ChatModel`、`ChatClient`、advisors 和 tools。
- 除非明确需要，不要绕过 Spring AI 直接调用模型 API。
- Agent 编排使用 Spring AI Alibaba Agent Framework 的图（Graph）模式，不要引入其他编排框架。
- 遵循项目现有的包结构和命名规范。
- 改动应保持小而安全，可维护优先。
- 前端修改遵循现有的 React + Tailwind 风格，不引入额外 UI 库。

## 本地依赖规则

- 使用数据库相关 MCP 工具前，先通过 `docker/start.sh` 确保 MySQL 和 Elasticsearch 已启动。
- `fitness-db-mcp-server` 虽然是 stdio 模式，但仍依赖 `fitness-mysql` 容器可用。
- 本地默认数据库配置：
  - 数据库：`fitness_db`
  - 用户名：`fitness_user`
  - 密码：`fitness_pass`
- Elasticsearch 本地地址：`http://localhost:9200`（无认证）

## 完整的启动顺序

```bash
# 1. 启动 Docker 服务（MySQL + Elasticsearch + Kibana + Adminer）
cd docker && ./start.sh

# 2. 启动图片搜索 MCP 服务
cd yu-image-search-mcp-server && mvn spring-boot:run &

# 3. 启动前端开发服务器（可选）
cd frontend && npm run dev &

# 4. 启动主应用
mvn spring-boot:run
```

## 快速入口

- `openspec/index.md` — OpenSpec 总索引
- `openspec/project/overview.md` — 项目概览
- `openspec/project/architecture.md` — 架构说明
- `openspec/workflows/delivery-lifecycle.md` — 交付生命周期
- `openspec/workflows/change-id-convention.md` — 变更 ID 规范
