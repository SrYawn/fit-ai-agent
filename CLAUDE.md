# CLAUDE.md

你当前工作的项目是：**fit-ai-agent**。

## 主要事实来源

项目中所有长期有效的上下文、架构说明、流程规范和变更记录，都统一放在 `openspec/` 下。

开始处理任务时，先阅读 `openspec/index.md`，再按当前任务类型加载相关的项目级或流程级文档。

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

- 这是一个基于 Java、Spring Boot、Spring AI 和 Spring AI Alibaba 的后端项目。
- 优先使用 Spring AI 抽象，例如 `ChatModel`、`ChatClient`、advisors 和 tools。
- 除非明确需要，不要绕过 Spring AI 直接调用模型 API。
- 遵循项目现有的包结构和命名规范。
- 改动应保持小而安全，可维护优先。

## 本地依赖规则

- 使用数据库相关 MCP 工具前，先通过 `docker/start.sh` 确保 MySQL 已启动。
- `fitness-db-mcp-server` 虽然是 stdio 模式，但仍依赖 `fitness-mysql` 可用。
- 本地默认数据库配置：
  - 数据库：`fitness_db`
  - 用户名：`fitness_user`
  - 密码：`fitness_pass`

## 完整的启动顺序：
  1. 启动 Docker 服务（MySQL + Elasticsearch）：                                              
  cd docker && ./start.sh                                                                  
  2. 启动 yu-image-search-mcp-server：                                                        
  cd yu-image-search-mcp-server && mvn spring-boot:run &                                      
  3. 启动主应用：                                                                             
  mvn spring-boot:run 

## 快速入口

- `openspec/index.md`
- `openspec/project/overview.md`
- `openspec/workflows/delivery-lifecycle.md`
- `openspec/workflows/change-id-convention.md`
