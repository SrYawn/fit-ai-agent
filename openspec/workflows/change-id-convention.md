# Change ID 命名规范

## 目的
每个非简单需求都应对应 `openspec/changes/` 下的一个目录。目录名需要稳定、可读、可排序。

## 格式

使用：

`YYYY-MM-short-topic`

示例：
- `2025-03-workflow-profile-streaming`
- `2025-03-action-guidance-retrieval-tuning`
- `2025-04-db-mcp-connection-hardening`

## 规则
- 以年份和月份开头，方便按时间排序。
- 仅使用小写字母、数字和连字符。
- topic 保持简短，并围绕行为或目标命名。
- 不要包含用户名、工单系统噪音，或 `fix-bug` 这种过于模糊的名称。
- 同一个需求仍在持续迭代时，优先复用已有变更目录。

## 何时拆分新目录
出现以下情况时，建议新建一个 change-id：
- 验收目标已经不是同一个需求
- 方案方向发生了实质变化
- 工作内容已经不属于同一批次交付

## 推荐用法

使用辅助脚本：

`bash scripts/create_change.sh 2025-03-example-change`

脚本会基于 `openspec/templates/` 自动创建标准文件。
