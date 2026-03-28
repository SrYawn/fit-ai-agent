# 外部系统

## MySQL
- 用途：存储用户画像、伤病和训练记录。
- 本地来源：Docker 服务 `fitness-mysql`。
- 接入路径：`fitness-db-mcp-server`。

## Elasticsearch 与 Kibana
- 用途：健身知识索引与检索。
- 本地来源：`docker/docker-compose.yml` 中的 Docker 服务。

## MCP 服务
- `fitness-db-mcp-server`：
  - 传输方式：stdio
  - 依赖：MySQL
- `yu-image-search-mcp-server`：
  - 传输方式：SSE
  - 依赖：独立进程启动
  - 当前根应用行为：启动时即连接 `http://localhost:8127/sse`，因此本地联调和接口测试前要先保证该服务可用

## 外部搜索 API
- 某些本地工具依赖配置好的 API Key 或外部服务。
- 若某次需求依赖这些工具，应在对应变更记录中注明前置条件和降级方案。
