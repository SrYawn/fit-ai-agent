# Agent 工作规则

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

## 范围控制
- 如果答案已经存在于 `openspec/` 中，不要依赖猜测。
- 不要把重要项目知识只留在聊天记录里。
- 如果实现结果与原设计不一致，必须记录到 `decision-log.md`。

## 本地启动规则
- 使用数据库相关 MCP 工具前，先通过 `docker/start.sh` 确保 MySQL 已启动。
- `fitness-db-mcp-server` 虽然是 stdio 模式，但如果 `fitness-mysql` 没启动，仍可能报 `Failed to obtain JDBC Connection`。
- 本地默认数据库配置：
  - 数据库：`fitness_db`
  - 用户名：`fitness_user`
  - 密码：`fitness_pass`

## 快速入口
- OpenSpec 索引：`openspec/index.md`
- 项目概览：`openspec/project/overview.md`
- 交付流程：`openspec/workflows/delivery-lifecycle.md`
- Change ID 规范：`openspec/workflows/change-id-convention.md`
