# Spring AI Alibaba Graph

## 什么是 Spring AI Alibaba Graph

Spring AI Alibaba Graph 是一个面向 Java 开发者的**工作流与多智能体框架**，用于构建由多个 AI 模型或多个步骤组成的复杂应用。

Spring AI Alibaba Graph 作为 Agent Framework 的底层核心引擎。它提供了用于构建智能体的原子组件，具备可中断与可编排能力，灵活性很高，但学习成本也相对较高。相比之下，Agent Framework 构建在 Graph 之上，通过 ReactAgent、SequentialAgent 等概念屏蔽底层复杂性。

更多细节请查看官网[文档](https://java2ai.com/docs/frameworks/graph-core/quick-start)。

## 核心概念与类

Graph 与 Spring Boot 生态深度集成，提供声明式 API 来编排工作流。开发者可以将 AI 应用中的每个步骤抽象为一个节点（Node），并以有向图（Graph）的形式连接这些节点，从而创建可定制的执行流。相比传统单智能体（单轮问答）方案，Spring AI Alibaba Graph 支持更复杂的多步骤任务流，有助于解决**单一大模型不足以完成复杂任务**的问题。

框架核心包括：**StateGraph**（用于定义节点与边的状态图）、**Node**（节点，封装具体操作或模型调用）、**Edge**（边，表示节点之间的迁移）、以及 **OverAllState**（全局状态，在整个流程中承载共享数据）。这些设计使得在工作流中进行状态管理与流程控制更加方便。

1. StateGraph
   用于定义工作流的主类。
   你可以添加节点（addNode）与边（addEdge、addConditionalEdges）。
   支持条件路由、子图与校验。
   可编译为 CompiledGraph 以执行。
2. Node
   表示工作流中的单个步骤（例如模型调用、数据转换）。
   节点可异步，并可封装 LLM 调用或自定义逻辑。
3. Edge
   表示节点之间的迁移。
   可为条件边，根据当前状态决定下一个节点。
4. OverAllState
   可序列化的中心状态对象，承载工作流全部数据。
   支持基于 key 的状态合并/更新策略。
   用于 checkpoint（检查点）、恢复执行与节点间传参。
5. CompiledGraph
   StateGraph 的可执行形态。
   负责实际执行、状态迁移与结果流式输出。
   支持中断、并行节点与 checkpoint。
6. InterruptableAction
   用于可中断图执行动作的接口。
   提供两个钩子点：`interrupt()`（执行前）和 `interruptAfter()`（执行后）。
   适用于 human-in-the-loop（人在回路）场景、审批工作流与多轮对话。

## 使用方式（典型流程）
- 定义 StateGraph：在 Spring 配置中定义 StateGraph Bean，添加节点（每个节点封装一次模型调用或逻辑），并用边连接。
- 配置状态：使用 OverAllStateFactory 定义初始状态与 key 策略。
- 执行：图会被编译并执行，状态沿节点和边流转，并由条件逻辑决定路径。
- 集成：通常通过 Spring Boot 应用中的 REST Controller 或 Service 对外暴露。

## 中断支持

Spring AI Alibaba Graph 支持在特定节点中断工作流执行，支持 human-in-the-loop 场景。

### InterruptableAction 接口

```java
public interface InterruptableAction {
    // 在节点执行前调用 - 可以阻止执行
    Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config);

    // 在节点执行后调用 - 可以检查结果并中断
    default Optional<InterruptionMetadata> interruptAfter(String nodeId, OverAllState state,
            Map<String, Object> actionResult, RunnableConfig config) {
        return Optional.empty();
    }
}
```

### 使用示例

```java
public class ReviewAction implements AsyncNodeActionWithConfig, InterruptableAction {

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        return CompletableFuture.completedFuture(Map.of("result", "generated_content"));
    }

    @Override
    public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
        return Optional.empty(); // 执行前不中断
    }

    @Override
    public Optional<InterruptionMetadata> interruptAfter(String nodeId, OverAllState state,
            Map<String, Object> actionResult, RunnableConfig config) {
        // 执行后中断，供人工审核
        return Optional.of(InterruptionMetadata.builder(nodeId, state)
            .addMetadata("reason", "needs_review")
            .addMetadata("content", actionResult.get("result"))
            .build());
    }
}
```
