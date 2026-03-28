# 依赖说明

## 运行时依赖
- MySQL：
  - 用户画像、伤病和训练记录 MCP 工具所必需。
- Elasticsearch：
  - 健身知识检索相关能力所必需。
- 图片搜索 MCP 服务：
  - 返回图片参考的场景所必需。

## 关键配置文件
- 根应用 MCP 配置：`src/main/resources/mcp-servers.json`
- DB MCP 配置：`fitness-db-mcp-server/src/main/resources/application.yml`
- 根应用配置：`src/main/resources/application.yml`

## 依赖故障时的预期行为
- MySQL 不可用：
  - 用户画像生成必须显式降级。
  - 计划生成不得伪装成基于真实画像的个性化计划。
- 图片搜索不可用：
  - 如果流程允许，应显式降级为无图回复。
- Elasticsearch 不可用：
  - 基于 RAG 的回答质量可能下降或直接失败，取决于具体功能路径。
