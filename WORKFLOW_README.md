# 健身 AI Agent 工作流系统

## 概述

本系统基于 **Spring AI Alibaba StateGraph 框架**实现了一个多 Agent 协作的健身 AI 工作流，能够根据用户输入自动识别意图并执行相应的处理流程。

## 系统架构

### 核心框架

使用 Spring AI Alibaba 的 `com.alibaba.cloud.ai.graph` 框架：
- **StateGraph**：工作流状态图
- **ReactAgent**：响应式 Agent（支持工具调用）
- **NodeAction**：自定义节点动作
- **CompiledGraph**：编译后的可执行图

### Agent 组件

系统包含以下 ReactAgent：

1. **IntentRecognitionAgent（意图识别）**
   - 通过简单的关键词路由节点识别用户意图
   - 意图类型：
     - `plan_generation`：计划生成
     - `action_guidance`：动作指导
     - `chat`：闲聊

2. **UserProfileAgent（用户画像）**
   - 查询用户数据，生成用户画像
   - 工具：MCP 数据库工具
   - 输出键：`user_profile`

3. **PlanGenerationAgent（计划生成）**
   - 生成个性化健身计划
   - 工具：RAG 检索 + MCP 工具
   - 输出键：`final_result`

4. **ActionGuidanceAgent（动作指导）**
   - 提供健身动作指导
   - 工具：RAG 检索（injury-recovery + exercises）+ 图片搜索
   - 输出键：`final_result`

5. **ChatAgent（闲聊）**
   - 处理问候和闲聊
   - 输出键：`final_result`

### 工作流程图

```
START
  ↓
intent_router (NodeAction)
  ↓
┌─────────┴─────────┬──────────────┐
│                   │              │
plan_generation    action_guidance  chat
│                   │              │
user_profile       │              │
│                   │              │
plan_generation    │              │
│                   │              │
└─────────┬─────────┴──────────────┘
          ↓
         END
```

### StateGraph 实现细节

```java
// 1. 定义状态管理策略
KeyStrategyFactory keyStrategyFactory = () -> {
    HashMap<String, KeyStrategy> strategies = new HashMap<>();
    strategies.put("user_input", new ReplaceStrategy());
    strategies.put("user_id", new ReplaceStrategy());
    strategies.put("intent", new ReplaceStrategy());
    strategies.put("user_profile", new ReplaceStrategy());
    strategies.put("final_result", new ReplaceStrategy());
    return strategies;
};

// 2. 创建 StateGraph
StateGraph workflow = new StateGraph(keyStrategyFactory);

// 3. 添加节点
workflow.addNode("intent_router", node_async(new IntentRouterNode()));
workflow.addNode("user_profile", userProfileAgent.asNode(true, false));
workflow.addNode("plan_generation", planAgent.asNode(true, false));
workflow.addNode("action_guidance", actionAgent.asNode(true, false));
workflow.addNode("chat", chatAgent.asNode(true, false));

// 4. 定义边
workflow.addEdge(StateGraph.START, "intent_router");
workflow.addConditionalEdges(
    "intent_router",
    edge_async(state -> state.value("_condition_result", "chat").toString()),
    Map.of(
        "plan_generation", "user_profile",
        "action_guidance", "action_guidance",
        "chat", "chat"
    )
);
workflow.addEdge("user_profile", "plan_generation");
workflow.addEdge("plan_generation", StateGraph.END);
workflow.addEdge("action_guidance", StateGraph.END);
workflow.addEdge("chat", StateGraph.END);

// 5. 编译图
CompiledGraph compiledGraph = workflow.compile(CompileConfig.builder().build());
```

## API 接口

### 执行工作流

**接口地址**：`POST /api/fitness/workflow/execute`

**请求参数**：
- `userInput`（必填）：用户输入内容
- `userId`（可选）：用户ID，默认为 1

**示例请求**：

```bash
# 计划生成示例
curl -X POST "http://localhost:8080/api/fitness/workflow/execute" \
  -d "userInput=帮我制定一个减肥计划" \
  -d "userId=1"

# 动作指导示例
curl -X POST "http://localhost:8080/api/fitness/workflow/execute" \
  -d "userInput=深蹲的正确姿势是什么"

# 闲聊示例
curl -X POST "http://localhost:8080/api/fitness/workflow/execute" \
  -d "userInput=你好"
```

## 技术实现

### 核心技术栈

- **Spring Boot 3.5.11**
- **Spring AI 1.1.2**
- **Spring AI Alibaba 1.1.2.0**
- **DashScope（通义千问）**
- **Elasticsearch（向量存储）**
- **MCP（Model Context Protocol）**

### 关键特性

1. **RAG（检索增强生成）**
   - 使用 Elasticsearch 作为向量数据库
   - 支持按类别过滤检索（injury-recovery、exercises、nutrition 等）
   - 自动将检索结果融入 Agent 的推理过程

2. **MCP 工具集成**
   - 数据库查询工具（fitness-db-mcp-server）
   - 图片搜索工具（yu-image-search-mcp-server）

3. **ReAct 模式**
   - 所有 Agent 都基于 ReAct（Reasoning and Acting）模式
   - 支持多轮思考和工具调用
   - 自动管理对话上下文

## 配置说明

### 必需配置

在 `application.yml` 中配置：

```yaml
# DashScope API Key
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}

# Elasticsearch 配置
spring:
  elasticsearch:
    uris: http://localhost:9200

# MCP 服务器配置
spring:
  ai:
    mcp:
      client:
        servers:
          fitness-db:
            command: java
            args:
              - -jar
              - fitness-db-mcp-server/target/fitness-db-mcp-server.jar
          image-search:
            command: java
            args:
              - -jar
              - yu-image-search-mcp-server/target/yu-image-search-mcp-server.jar
```

## 使用示例

### 1. 制定健身计划

**用户输入**：
```
帮我制定一个为期8周的增肌训练计划
```

**系统处理流程**：
1. 意图识别 → `plan_generation`
2. 查询用户画像（用户ID=1）
3. RAG 检索增肌相关知识
4. 生成个性化增肌计划

**输出示例**：
```
【增肌训练计划】

训练目标：增加肌肉量，提升力量水平
训练周期：8周
训练频率：每周4-5次，每次60-90分钟

第1-2周（适应期）：
- 周一：胸部+三头肌
  - 杠铃卧推 4组×8-10次
  - 哑铃飞鸟 3组×10-12次
  ...

饮食建议：
- 每日热量盈余：+300-500卡路里
- 蛋白质摄入：体重×2g
...
```

### 2. 学习健身动作

**用户输入**：
```
腰部康复训练有哪些动作
```

**系统处理流程**：
1. 意图识别 → `action_guidance`
2. RAG 检索 injury-recovery 类别知识
3. RAG 检索 exercises 类别知识
4. 调用图片搜索工具
5. 生成动作指导

**输出示例**：
```
【腰部康复训练动作指导】

1. 猫式伸展（Cat-Cow Stretch）
   - 动作描述：四肢着地，交替拱背和塌腰
   - 目标肌群：腰部核心肌群
   - 注意事项：动作缓慢，避免突然发力
   - 示范图片：[图片链接]

2. 鸟狗式（Bird Dog）
   ...
```

### 3. 闲聊对话

**用户输入**：
```
你好
```

**输出示例**：
```
你好！😊 我是你的健身助手，很高兴为你服务！
需要我帮你制定健身计划，或者了解某个健身动作吗？
```

## 扩展开发

### 添加新的 Agent

1. 继承 `ToolCallAgent` 类
2. 定义 `SYSTEM_PROMPT`
3. 实现业务逻辑方法
4. 在 `FitnessWorkflowService` 中集成

### 添加新的工具

1. 创建工具类，使用 `@Tool` 注解
2. 在 `ToolRegistration` 中注册
3. 在相应的 Agent 中配置工具

### 添加新的意图类型

1. 在 `IntentRecognitionAgent` 的 `SYSTEM_PROMPT` 中添加新意图
2. 在 `IntentRouteTool` 中添加验证逻辑
3. 在 `FitnessWorkflowService` 中添加处理方法

## 注意事项

1. **RAG 检索优化**：
   - 计划生成时，query 只传用户需求关键词，不包含用户画像
   - 动作指导时，分别检索 injury-recovery 和 exercises 类别

2. **Agent 步骤限制**：
   - 意图识别：最多 5 步
   - 计划生成：最多 10 步
   - 动作指导：最多 12 步
   - 闲聊：最多 3 步

3. **错误处理**：
   - 所有 Agent 都有异常捕获机制
   - 工作流服务层统一处理错误

## 未来优化方向

1. 支持流式输出（SSE）
2. 添加对话历史管理
3. 支持多轮对话上下文
4. 添加用户反馈机制
5. 优化 RAG 检索策略
