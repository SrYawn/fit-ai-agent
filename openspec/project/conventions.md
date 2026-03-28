# 开发约定

## 代码组织
- Java 类尽量保持在现有功能包中。
- 不要把 controller、graph、agent、tool 等职责混在同一个类里。
- 在与现有代码一致的前提下，优先使用构造器注入。

## 命名与风格
- Java 代码使用 4 个空格缩进。
- 类名使用 `UpperCamelCase`。
- 方法和字段使用 `lowerCamelCase`。
- 包名统一使用 `com.zsr` 下的小写命名。

## Agent 与工作流规则
- 每个 Agent 可见的工具面应尽量收敛。
- 外部依赖缺失或输入不足时，优先使用显式状态来驱动降级。
- 禁止让 Agent 猜测用户标识、用户名或健康数据。

## 文档规则
- 项目稳定知识放在 `openspec/project/`。
- 交付流程规则放在 `openspec/workflows/`。
- 具体需求的方案、实现和验证证据放在 `openspec/changes/<change-id>/`。
