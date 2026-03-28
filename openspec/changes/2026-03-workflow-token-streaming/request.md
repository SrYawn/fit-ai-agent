# 需求说明

## 摘要
- 将 `/api/fitness/workflow/execute` 的普通返回封装为结构化 JSON。
- 将 `/api/fitness/workflow/execute/stream` 从“节点级伪流式事件”改为“token 级真实 SSE 文本流”。
- 保留节点信息，但通过 payload 字段标记 token 所属节点，而不是把节点本身当作前端主事件。

## 背景
- 当前流式接口按 `workflow/node/result` 等阶段推送事件，最终正文一次性塞在 `result` 事件里。
- 这种返回方式本质上不是 token 流，前端只能按工作流阶段消费，无法像正常 LLM 流式输出那样逐段渲染。
- 用户期望普通接口直接拿到结构化结果，流式接口则持续收到增量文本，同时还能知道文本来自哪个节点。

## 范围
- 调整工作流普通接口响应结构。
- 调整工作流流式接口 SSE 事件模型。
- 为最终回答节点补充真实增量文本输出能力。
- 更新相关 OpenSpec 和项目级文档。

## 非范围
- 前端渲染逻辑改造。
- StateGraph 本身的能力扩展或替换。
- 用户画像、计划生成、动作指导的业务提示词重构。

## 验收标准
- `POST /api/fitness/workflow/execute` 返回 JSON，至少包含执行状态、识别意图、执行节点和最终结果。
- `GET /api/fitness/workflow/execute/stream` 的正文输出以多个 token 事件推送，而不是单个 `result` 事件一次性返回完整答案。
- 每个 token 事件的 payload 都带有节点标识，前端无需依赖 `workflow/node/result` 事件名来判断来源。
- 保留必要的开始、路由、完成、错误等元数据事件，但它们不承载整段最终正文。
