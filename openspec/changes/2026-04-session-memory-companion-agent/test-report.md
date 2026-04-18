# 测试报告

## 执行环境

- 日期：2026-04-11
- Profile：local
- JDK：21.0.6
- 依赖服务：MySQL（Docker）、Elasticsearch（Docker）、yu-image-search-mcp-server（手动启动，端口 8127）
- LLM：DashScope qwen-plus

## 编译验证

- `mvnw compile` 通过，无错误

## 代码修复（测试过程中发现）

1. `SessionMemoryIntegrationTest.java` 第 61 行：`Math.min(20sponse1.result().length())` → `Math.min(200, response1.result().length())`（拼写错误）
2. `FitnessWorkflowGraph.java`：`executeWorkflow()` 和 `executeWorkflowStream()` 在 `sessionId` 为 null 时未自动生成 UUID，已补充 `resolvedSessionId` 逻辑，与 Controller 层保持一致

## 功能一：多轮会话支持（SessionMemoryIntegrationTest）

| # | 测试名称 | 结果 | 说明 |
|---|---------|------|------|
| 1 | testSessionIdGeneration | PASS | sessionId 为 null 时自动生成（修复后通过） |
| 2 | testMultiTurnConversation_PlanGeneration | PASS | 第一轮"帮我制定减肥计划"→ plan_generation，第二轮"加上饮食建议"→ plan_generation，意图承接正确 |
| 3 | testMultiTurnConversation_EmotionTracking | PASS | 两轮情绪对话均路由到 chat，上下文连贯 |
| 4 | testSessionIsolation | PASS | 不同 sessionId 的对话互不干扰，session1 第二轮正确承接减肥上下文 |
| 5 | testSessionIdInResponse | PASS | 传入的 sessionId 在响应中原样返回 |

**总计：5/5 通过，耗时 5 分 33 秒**

## 功能二：陪伴激励助手（CompanionMotivationAgentIntegrationTest）

| # | 测试名称 | 结果 | 说明 |
|---|---------|------|------|
| 1 | testSimpleGreeting | PASS | 问候路由到 chat，有回复内容（虽然 API 欠费导致回复为错误信息，但断言条件宽松通过） |
| 2 | testEmotionExpression_Fatigue | FAIL | DashScope API 欠费（Arrearage），LLM 无法生成共情回复 |
| 3 | testPositiveFeedback | FAIL | 同上，LLM 无法生成鼓励回复 |
| 4 | testTrainingStateInquiry | PASS | 路由和状态断言通过 |
| 5 | testSafetyBoundary_InjuryScenario | FAIL | 同上，LLM 无法生成就医建议 |
| 6 | testRoutingDistinction | FAIL | 意图识别 Agent 也受 API 欠费影响，"帮我制定增肌计划"被错误路由到 chat |
| 7 | testMotivationKnowledgeRetrieval | FAIL | 同上，LLM 无法生成平台期建议 |

**总计：2/7 通过，5/7 失败，耗时 6.6 秒**

## 失败根因分析

所有失败均由 DashScope API 返回 HTTP 400 `Arrearage`（账户欠费）导致：

```
HTTP 400 - {"code":"Arrearage","message":"Access denied, please make sure your account is in good standing."}
```

Agent 在 LLM 调用失败后走入错误处理分支，返回错误信息字符串而非正常回复，导致内容断言失败。**非代码逻辑问题。**

## 待办

- [ ] DashScope 账户充值后，重新执行 CompanionMotivationAgentIntegrationTest 全部 7 个用例
- [ ] 确认所有内容断言（共情、鼓励、就医建议、平台期建议）在正常 LLM 回复下通过
