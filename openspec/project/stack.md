# 技术栈

## 语言与框架
- Java 21
- Spring Boot 3.x
- Spring AI
- Spring AI Alibaba Graph
- Maven Wrapper

## 数据与基础设施
- MySQL 8.0
- Elasticsearch 8.x
- Kibana
- Adminer
- Docker Compose

## 集成方式
- HTTP REST
- SSE
- 基于 stdio 的 MCP
- 基于 SSE 的 MCP

## 运维说明
- 本地开发优先采用 Docker 拉起基础依赖。
- 主应用与各 MCP 模块统一使用仓库中的 Maven Wrapper 构建。
