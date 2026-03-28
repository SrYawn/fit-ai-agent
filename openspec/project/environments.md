# 环境说明

## 本地开发环境
- 主 API 默认地址：`http://localhost:8123/api`
- MySQL：`localhost:3306`
- Elasticsearch：`localhost:9200`
- Kibana：`localhost:5601`
- Adminer：`http://localhost:8080`

## 推荐启动顺序
1. 在 `docker/` 目录启动基础依赖。
2. 启动 `yu-image-search-mcp-server`。
3. 启动主 API 服务。

## 关键依赖说明
- `fitness-db-mcp-server` 虽然使用 stdio 模式，由 MCP 集成自动拉起，但仍要求 MySQL 预先可用。
- 如果 `fitness-mysql` 没有运行，数据库工具调用可能报出 `Failed to obtain JDBC Connection`。
- 根应用会在启动阶段初始化 `yu-image-search-mcp-server` 的 SSE MCP 连接；如果 `http://localhost:8127/sse` 不可用，主 API 可能在启动时超时失败。
- 本地默认数据库配置：
  - 数据库：`fitness_db`
  - 用户名：`fitness_user`
  - 密码：`fitness_pass`

## 常用命令
- 启动基础依赖：`cd docker && ./start.sh`
- 启动图片搜索 MCP：`cd yu-image-search-mcp-server && java -jar target/fit-image-search-mcp-server-0.0.1-SNAPSHOT.jar`
- 停止基础依赖：`cd docker && ./stop.sh`
- 启动主应用：`./mvnw spring-boot:run`
