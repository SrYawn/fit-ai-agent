# OpenSpec 索引

`openspec/` 是本项目的结构化事实来源，用于沉淀项目上下文、交付流程和需求变更记录。

## 每次必读
- `project/overview.md`
- `project/architecture.md`
- `project/environments.md`
- `workflows/delivery-lifecycle.md`
- `workflows/change-id-convention.md`

## 按任务类型补充阅读
- 后端工作流或 Agent 相关改动：
  - `project/architecture.md`
  - `project/conventions.md`
  - `workflows/testing-policy.md`
- MCP、MySQL 或外部工具相关改动：
  - `project/dependencies.md`
  - `project/external-systems.md`
  - `project/environments.md`
  - `workflows/incident-debugging.md`
- 文档或流程规范改动：
  - `workflows/documentation-policy.md`

## 变更记录
- 每个非简单需求，都应在 `changes/<change-id>/` 下创建或维护一个目录。
- 每个变更目录的必备文件：
  - `request.md`
  - `design.md`
  - `tasks.md`
  - `implementation.md`
  - `test-report.md`
  - `decision-log.md`

## 执行规则
1. 先阅读本次任务所需的项目级规范文件。
2. 创建或更新当前变更目录。
3. 在修改代码前先写或补充设计说明。
4. 实施代码改动。
5. 执行验证。
6. 回填实现记录和测试证据。
7. 如果架构、环境、依赖或长期规范发生变化，同时更新对应的项目级文档。
