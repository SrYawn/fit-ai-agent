# 实现记录

## 改动文件
- `src/main/java/com/zsr/fitaiagent/controller/FitnessWorkflowController.java`
- `src/main/java/com/zsr/fitaiagent/graph/FitnessWorkflowGraph.java`
- `src/main/java/com/zsr/fitaiagent/agent/ToolCallAgent.java`
- `src/main/java/com/zsr/fitaiagent/agent/PlanGenerationAgent.java`
- `src/main/java/com/zsr/fitaiagent/agent/ActionGuidanceAgent.java`
- `src/main/java/com/zsr/fitaiagent/agent/ChatAgent.java`
- `openspec/project/overview.md`
- `openspec/project/architecture.md`

## 行为变化
- `POST /api/fitness/workflow/execute` 由纯字符串改为结构化 JSON，返回执行状态、意图、节点、画像状态和最终结果。
- `GET /api/api/fitness/workflow/execute/stream` 由 `workflow/node/result` 主导的节点事件流，改为以 `token` 为主体的 SSE 输出。
- 流式 payload 固定包含 `event`、`node`、`status`、`sequence`、`done`，并按需附带 `intent`、`profileStatus`、`content`。
- 非正文信息收敛为 `metadata` 事件，正文完成时发送 `done` 事件，异常场景发送 `error` 事件。
- 最终回答节点新增“同步完成工具调用准备 + 流式输出最终答案”的两阶段执行方式。

## 与设计不一致的地方
- 无。

## 后续说明
- 当前真实流式发生在最终用户可见回答阶段；工具调用和前置节点仍然通过同步执行完成准备。
- `done` 事件仍附带完整聚合后的 `content`，便于前端在丢包或重连时做最终态兜底。
