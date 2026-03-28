# Fit AI Agent 项目介绍

## 1. 项目定位

这是一个基于 `Spring Boot + Spring AI + Spring AI Alibaba StateGraph + MCP + RAG` 构建的健身智能体后端系统。

它的核心目标不是做一个单纯的聊天接口，而是把用户请求识别、任务路由、用户画像生成、知识检索、动作指导、计划生成、外部工具调用这些能力组织成一个可编排的多 Agent 系统。

从当前代码实现来看，这个项目主要承担三类能力：

1. 健身工作流总入口
2. 健身知识库的构建与检索
3. 通过 MCP 接入数据库和图片搜索等外部能力

---

## 2. 总体架构概览

系统可以理解成 6 层：

1. 接口入口层：Controller，对外暴露 HTTP / SSE 接口
2. 工作流编排层：`FitnessWorkflowGraph`，根据状态图决定任务走向
3. Agent 执行层：多个基于 ReAct/Tool Calling 的专用 Agent
4. Tool 工具层：本地工具、RAG 检索工具、终止工具、意图路由工具、MCP 工具
5. 数据与知识层：MySQL 用户数据、Elasticsearch 向量知识库、本地 Markdown 文档
6. 外部扩展层：DashScope 大模型、MCP Server、搜索/抓取/图片能力

用一条主链路概括就是：

`Controller -> WorkflowGraph -> Intent Agent -> 路由 -> 业务 Agent -> Tool/MCP/RAG -> 结果返回`

---

## 3. 从顶层入口往下看

### 3.1 应用启动层

项目启动类是 [FitAiAgentApplication.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/FitAiAgentApplication.java)。

它是标准 Spring Boot 启动入口，但这里有一个重要设计点：

- 显式排除了 `DataSourceAutoConfiguration`
- 说明主应用当前更偏“AI 编排应用”，而不是强依赖主进程直接连数据库
- 数据库查询能力主要通过独立的 `fitness-db-mcp-server` 来提供

这代表项目采用了“主应用 + MCP 子服务”的扩展式设计，而不是把所有能力都塞进一个进程里。

### 3.2 Controller 入口层

当前主要有 4 个 Controller：

1. [FitnessWorkflowController.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/controller/FitnessWorkflowController.java)
2. [UserProfileController.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/controller/UserProfileController.java)
3. [FitnessKnowledgeController.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/controller/FitnessKnowledgeController.java)
4. [HealthController.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/controller/HealthController.java)

其中你提到的 `controller` 层后台入口，核心确实是 `FitnessWorkflowController`。

它承担两个主接口：

- `POST /api/fitness/workflow/execute`
- `GET /api/fitness/workflow/execute/stream`

这两个接口的职责很清晰：

- 接收 `userInput` 和可选 `userId`
- 调用 `FitnessWorkflowGraph`
- 返回同步结果，或者通过 `SseEmitter` 返回流式结果

也就是说，Controller 层在这个项目里基本不做业务判断，它是一个“请求接入 + 委派执行”的薄入口层。

这是比较好的职责划分，业务智能集中在更下层，而不是散落在接口代码里。

---

## 4. 工作流编排层：系统真正的中枢

### 4.1 核心类

中枢是 [FitnessWorkflowGraph.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/graph/FitnessWorkflowGraph.java)。

这个类是当前系统最关键的架构节点，因为它负责：

- 构建 StateGraph
- 定义状态字段
- 组装不同 Agent 及其工具集
- 定义节点与条件边
- 执行同步 / 流式工作流

### 4.2 状态设计

当前工作流主要维护以下状态键：

- `user_input`
- `user_id`
- `intent`
- `user_profile`
- `final_result`

所有键都使用 `ReplaceStrategy`，说明当前状态模型是“覆盖式状态”，而不是“累加式事件流状态”。

这种设计优点是简单直接，适合当前三路分支的工作流。
缺点是中间过程信息保留较弱，比如：

- 每一步调用了哪些工具
- 检索到了哪些知识
- 失败发生在哪个节点
- token 消耗、重试情况、耗时统计

这些目前没有沉淀为正式状态。

### 4.3 节点与路由

当前图的执行逻辑可以概括为：

1. `intent_recognition`
2. 按意图分支
3. 如果是计划生成，则先走 `user_profile`
4. 再走 `plan_generation`
5. 如果是动作指导，直接走 `action_guidance`
6. 如果是闲聊，直接走 `chat`

工作流结构如下：

```text
START
  -> intent_recognition
     -> user_profile -> plan_generation -> END
     -> action_guidance -> END
     -> chat -> END
```

更准确地说，条件边会把 `plan_generation` 意图先路由到 `user_profile` 节点，再继续到真正的计划生成节点。

### 4.4 这个编排层的价值

这一层把系统从“单 Agent 问答”提升成了“多阶段任务系统”：

- 先分类请求
- 再走不同执行路径
- 计划生成前强制补充用户画像
- 不同路径使用不同工具集

这意味着项目已经具备一个比较完整的 Agent Orchestration 雏形。

---

## 5. Agent 执行层：系统的业务智能主体

### 5.1 Agent 基础抽象

Agent 分三层抽象：

1. [BaseAgent.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/agent/BaseAgent.java)
2. [ReActAgent.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/agent/ReActAgent.java)
3. [ToolCallAgent.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/agent/ToolCallAgent.java)

#### BaseAgent

提供通用运行框架：

- agent 状态管理
- 步数控制
- 消息上下文维护
- 同步执行 `run`
- SSE 流式执行 `runStream`
- 执行完后的清理

#### ReActAgent

把一次 step 拆成：

- `think()`
- `act()`

这是很典型的 ReAct 模式。

#### ToolCallAgent

这是当前最关键的 Agent 基类，负责：

- 调用 LLM
- 解析工具调用意图
- 手动执行工具调用
- 把工具结果回写进消息历史
- 在 `doTerminate` 出现时结束任务

这里有一个很重要的设计：项目禁用了 Spring AI 的内置自动工具执行，改成自己维护 Tool Calling 流程。

这样做的好处是：

- 你能更精细地控制消息历史
- 终止逻辑由自己掌控
- 后续更容易扩展审计、日志、限流、工具权限

### 5.2 具体业务 Agent

#### 1. IntentRecognitionAgent

文件：[IntentRecognitionAgent.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/agent/IntentRecognitionAgent.java)

职责：

- 分析用户输入
- 将请求分类成 `plan_generation` / `action_guidance` / `chat`
- 通过 `routeToIntent` 工具把结果写到外部工具对象里

它本质是“路由 Agent”。

#### 2. UserProfileAgent

文件：[UserProfileAgent.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/agent/UserProfileAgent.java)

职责：

- 借助 MCP 数据库工具查用户数据
- 汇总训练记录、基础信息、行为特征
- 生成用户画像文本

它是计划生成链路里的“上下文增强器”。

#### 3. PlanGenerationAgent

文件：[PlanGenerationAgent.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/agent/PlanGenerationAgent.java)

职责：

- 接收用户画像
- 接收用户需求
- 使用 `searchKnowledge` 检索训练和饮食知识
- 生成个性化训练计划

它体现了项目最核心的价值：`用户数据 + 领域知识 + LLM 推理` 的三者融合。

#### 4. ActionGuidanceAgent

文件：[ActionGuidanceAgent.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/agent/ActionGuidanceAgent.java)

职责：

- 针对动作、康复、姿势问题提供指导
- 检索康复和动作知识
- 调 MCP 图片搜索工具找示范图片

它比计划生成更偏“单问题深答复”型 Agent。

#### 5. ChatAgent

文件：[ChatAgent.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/agent/ChatAgent.java)

职责：

- 处理问候、感谢、轻度闲聊
- 避免所有请求都走复杂工作流

它像系统里的兜底交互层。

#### 6. YuManus

文件：[YuManus.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/agent/YuManus.java)

这是一个更通用的“超级 Agent”实验入口。

和工作流式的专用 Agent 不同，它更像：

- 不限制场景
- 拥有更多工具
- 依赖自主规划解决复杂任务

它代表项目里另一种设计方向：从固定流程式 Agent，走向通用自治 Agent。

---

## 6. Tool 工具层：Agent 的能力接口

### 6.1 工具注册中心

工具装配由 [ToolRegistration.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/tools/ToolRegistration.java) 完成。

它把工具分成三组：

1. `localTools`
2. `mcpTools`
3. `allTools`

这是个非常关键的设计，因为它让不同 Agent 可以按需拿不同能力边界的工具集，而不是所有 Agent 都拿到全部权限。

### 6.2 本地工具

当前本地工具包括：

- `FileOperationTool`
- `WebSearchTool`
- `WebScrapingTool`
- `ResourceDownloadTool`
- `TerminalOperationTool`
- `PDFGenerationTool`
- `TerminateTool`
- `KnowledgeSearchTool`

它们覆盖了：

- 文件读写
- 搜索
- 网页抓取
- 资源下载
- 终端执行
- PDF 生成
- 知识库检索
- 任务终止

### 6.3 专用工具

#### IntentRouteTool

文件：[IntentRouteTool.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/tools/IntentRouteTool.java)

它不是传统业务工具，而是“工作流通信工具”。

它的作用不是给用户返回知识，而是把意图识别结果带回图编排层。

#### KnowledgeSearchTool

文件：[KnowledgeSearchTool.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/tools/KnowledgeSearchTool.java)

它把底层 `FitnessKnowledgeService` 封装成一个可被 LLM 调用的 Tool，是 RAG 能力进入 Agent 推理流程的关键桥梁。

#### TerminateTool

文件：[TerminateTool.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/tools/TerminateTool.java)

它用于让 Agent 明确结束执行。

这一点很重要，因为当前系统是“工具调用驱动”的循环式 Agent，如果没有显式终止标记，就容易多走无意义 step。

---

## 7. 数据层与知识层

### 7.1 用户数据侧：MCP 数据库服务

主应用本身没有直接在业务代码里使用 Repository/DAO 查询用户数据，而是通过独立子模块 `fitness-db-mcp-server` 暴露工具：

- `getUserProfile`
- `getUserInjuries`
- `getUserTrainingRecords`

对应实现见：

- [FitnessDbTool.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/fitness-db-mcp-server/src/main/java/com/zsr/fitnessdbmcpserver/tools/FitnessDbTool.java)

这种设计的好处：

- 主应用和数据服务解耦
- AI 工具调用边界更清晰
- 后续可以更方便切换为远端服务或独立部署

### 7.2 知识库侧：本地文档 + 向量检索

知识库能力由 [FitnessKnowledgeService.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/service/FitnessKnowledgeService.java) 提供。

核心流程：

1. 从 `src/main/resources/fitness-docs` 加载 Markdown 文档
2. 给文档附加分类元数据
3. 使用 `TokenTextSplitter` 切分
4. 写入 Elasticsearch 向量库
5. 在查询时按 `query + category` 做向量检索

相关实现：

- [FitnessDocumentLoader.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/rag/FitnessDocumentLoader.java)
- [MyTokenTextSplitter.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/rag/MyTokenTextSplitter.java)
- [QueryRewriter.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/rag/QueryRewriter.java)
- [MyKeywordEnricher.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/rag/MyKeywordEnricher.java)

### 7.3 当前知识分类

项目内置知识分类有：

- `exercise`
- `injury-recovery`
- `nutrition`
- `training-plan`
- `body-knowledge`

这些分类对应本地 Markdown 文档目录。

这说明当前 RAG 不是通用知识库，而是一个明确垂直领域的健身知识库。

---

## 8. 配置与运行支撑

### 8.1 核心配置

主配置文件是 [application.yml](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/resources/application.yml)。

里面定义了：

- Spring Boot 应用名与端口
- DashScope 模型配置
- MySQL 连接参数
- Elasticsearch 向量库参数
- MCP client 配置
- Swagger / Knife4j 配置
- Search API Key

### 8.2 模型配置

[ChatModelConfig.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/config/ChatModelConfig.java) 把 `dashScopeChatModel` 设置成主 `ChatModel`。

也就是说，当前主执行模型默认是 DashScope。

### 8.3 跨域配置

[CorsConfig.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/java/com/zsr/fitaiagent/config/CorsConfig.java) 对所有路径放开了跨域访问。

从开发调试角度是方便的，但在生产环境里安全边界会偏松。

### 8.4 Docker 辅助环境

`docker` 目录提供了配套依赖服务：

- Elasticsearch
- Kibana
- MySQL
- Adminer

这表明项目已经考虑了本地联调环境，而不是只停留在代码实验阶段。

---

## 9. 当前系统的设计亮点

### 9.1 不是单一聊天接口，而是流程化 Agent 系统

项目最大的优点，是已经从“调用大模型返回文本”迈向“多 Agent 编排”。

它具备：

- 意图分流
- 路由决策
- 专用 Agent
- 不同工具边界
- RAG 与外部工具结合

这让系统更像一个任务执行平台，而不是问答 Demo。

### 9.2 主业务链路比较清晰

`工作流入口 -> 意图识别 -> 专用 Agent -> 工具 -> 结果`

这条线非常适合后续继续扩展：

- 新增营养分析 Agent
- 新增康复评估 Agent
- 新增训练记录总结 Agent

### 9.3 RAG 与用户画像是分层接入的

计划生成并不是直接靠 prompt 胡编，而是结合了：

- 用户画像
- 知识库内容
- 训练目标

这比纯大模型输出更接近“专业领域助手”。

### 9.4 MCP 的引入很有扩展性

把数据库查询、图片搜索等能力做成 MCP 服务，有几个长期价值：

- 工具边界标准化
- 独立演进
- 便于接更多外部系统
- 便于后续多 Agent 共用

---

## 10. 当前架构里值得注意的问题

这一部分不是说系统“不好”，而是从后续工程化角度看，已经到了值得继续打磨的阶段。

### 10.1 主应用的数据源配置存在设计上的摇摆

一方面启动类排除了 `DataSourceAutoConfiguration`，表示主应用不依赖数据库自动配置。

另一方面 [application.yml](/Users/zhusirui/IdeaProjects/fit-ai-agent/src/main/resources/application.yml) 又配置了 MySQL 数据源。

这说明当前架构在“两种路线”之间并存：

- 路线 A：主应用自己直连数据库
- 路线 B：数据库能力完全由 MCP 子服务提供

建议后续明确一种主路线，否则会让维护者对系统边界产生困惑。

### 10.2 StateGraph 在每次请求时动态构建

`FitnessWorkflowGraph` 的 `executeWorkflow` 和 `executeWorkflowStream` 都会先调用 `buildWorkflow()`。

这意味着每个请求都会：

- 新建 ChatClient
- 新建工具数组组合
- 新建多个 Agent
- 重新编译一遍图

对于演示没问题，但在高并发或稳定运行场景里，这是明显可以优化的地方。

### 10.3 Agent 的输出结果仍偏“文本黑盒”

当前系统主要把最终结果当字符串处理，缺少结构化中间产物，比如：

- 标准化意图识别结果
- 用户画像 DTO
- 训练计划 JSON schema
- 动作指导结构化字段

这会带来几个问题：

- 前端难以精细展示
- 后续难做审计与分析
- Agent 之间的可组合性不够强

### 10.4 工具权限边界偏宽

`allTools` 里包含：

- 文件写入
- 下载
- 网页抓取
- 搜索
- 终端执行

对于通用 Agent 或未来开放接口来说，这些工具组合权限比较大。

尤其 `TerminalOperationTool` 风险很高，因为它允许模型直接执行 shell 命令。

### 10.5 SSE 实现是“工作流状态流”，不是“真实 token 流”

当前流式接口主要是监听 `NodeOutput`，再从状态中提取：

- 意图
- 最终结果

它能让前端感知进度，但还不是严格意义上的 LLM token streaming。

如果后面要做更强交互感，这部分还有提升空间。

### 10.6 Prompt 与分类枚举存在潜在不一致

知识目录使用的是 `exercise`，但 `ActionGuidanceAgent` 的 prompt 中写的是 `exercises`。

如果模型严格按 prompt 调工具，可能导致检索分类不匹配。

这是一个很典型的“代码常量和 prompt 文本脱节”问题。

### 10.7 用户画像流程对数据库查询方式比较脆弱

`UserProfileAgent` 的 prompt 里提到如果不知道用户名，可以尝试 `zhangsan`、`lisi`、`wangwu` 等常见用户名。

这更像演示环境的 prompt workaround，而不是稳定设计。

一旦真实数据不符合这个假设，画像质量会明显下降。

### 10.8 MCP 数据库查询里有动态 SQL 拼接

[FitnessDbTool.java](/Users/zhusirui/IdeaProjects/fit-ai-agent/fitness-db-mcp-server/src/main/java/com/zsr/fitnessdbmcpserver/tools/FitnessDbTool.java) 的 `getUserTrainingRecords` 使用字符串拼接日期过滤条件。

虽然 `userId` 使用了参数绑定，但日期部分仍然建议改成参数化查询，避免后续扩展时留下安全隐患。

### 10.9 异常处理与可观测性还不够工程化

当前不少地方出错后直接返回：

- `"执行失败"`
- `"执行错误"`
- `"Error xxx"`

对于 demo 足够，但如果后续要接前端、日志平台、监控系统，需要更统一的错误模型、trace id、阶段耗时、工具调用日志。

---

## 11. 后续可以重点优化的方向

下面这些是我认为最值得投入的优化点，按优先级从高到低排列。

### 11.1 优先级 P0：把核心业务结果结构化

建议先把以下结果从自由文本升级成结构化对象：

- 意图识别结果
- 用户画像结果
- 训练计划结果
- 动作指导结果

例如训练计划至少可以拆成：

- `goal`
- `cycleWeeks`
- `weeklyFrequency`
- `schedule`
- `dietAdvice`
- `warnings`

这样做的收益最大：

- 前端更容易渲染
- 更容易做导出 PDF / 日历 / 打卡计划
- 更容易评估模型输出质量

### 11.2 优先级 P0：缓存或单例化工作流编译结果

当前 `buildWorkflow()` 每次请求都会执行，建议优化为：

- 应用启动时构建一次
- 或按配置变化懒加载一次
- Agent 需要有无状态化改造后再复用

这会直接降低请求开销，让系统更像真正的服务。

### 11.3 优先级 P0：收紧工具权限

建议把工具按安全级别分层：

- 安全工具：知识检索、终止、意图路由
- 业务工具：数据库查询、图片搜索
- 高风险工具：文件写入、下载、终端执行、网页抓取

然后只给特定 Agent 开必要工具，不要默认给 `allTools`。

如果未来有开放式 Agent 接口，这一点几乎是必须做的。

### 11.4 优先级 P1：统一常量与 prompt 语义

建议把这些内容抽成统一枚举/常量：

- 意图类型
- 知识分类
- 工具名
- 状态键名

并让 prompt 生成依赖这些常量，避免出现 `exercise` / `exercises` 这种漂移。

### 11.5 优先级 P1：把用户画像改成“确定性查询 + LLM 总结”

当前用户画像更像“让 LLM 自己想办法查库”。

更稳的方式是：

1. 后端先确定性查询用户基础资料、伤病、训练记录
2. 组装成标准上下文对象
3. 让 LLM 专注做总结、归纳、洞察

这样会明显提高：

- 稳定性
- 可测试性
- 可解释性

### 11.6 优先级 P1：增强工作流状态

建议把状态拓展成更完整的执行上下文，例如：

- `retrieved_docs`
- `tool_trace`
- `execution_stage`
- `error_code`
- `latency_ms`
- `model_name`

这样以后做调试面板、回放、监控会轻松很多。

### 11.7 优先级 P1：完善观测与日志

建议加入：

- 请求级 trace id
- 每个节点耗时
- 每次工具调用耗时和参数摘要
- 模型输入输出摘要
- 错误分类统计

这会让系统从“能跑”升级到“好维护”。

### 11.8 优先级 P2：增强 RAG 检索链路

当前 RAG 已经可用，但还可以继续升级：

- 使用 `QueryRewriter` 真正接入查询改写流程
- 引入关键词增强和 metadata enrichment
- 做混合检索而不只是向量检索
- 引入 rerank
- 增加知识来源引用

如果以后要提升专业可信度，这一块非常值得做。

### 11.9 优先级 P2：让流式输出更细粒度

可以把流式输出分成：

- 节点开始
- 工具调用开始/结束
- 检索完成
- 文本 token 流
- 节点结束

这样前端就能真正展示“AI 正在做什么”，用户感知会更好。

### 11.10 优先级 P2：补齐测试

当前已有部分工具测试，但从系统层面看，还缺：

- 工作流路由测试
- Agent 集成测试
- MCP 联调测试
- 知识库检索质量测试
- 端到端接口测试

建议至少给 `FitnessWorkflowGraph` 和关键 Agent 增加集成测试。

---

## 12. 如果后面继续演进，我建议的目标形态

我会把这个项目往下面这个方向演进：

### 阶段一：稳住当前主链路

- 修正常量/分类不一致
- 收紧工具权限
- 固化用户画像查询流程
- 给训练计划输出加结构化 schema

### 阶段二：强化工程化

- 缓存工作流编译结果
- 增加 trace/log/metrics
- 统一错误模型
- 增加集成测试

### 阶段三：扩展智能体能力

- 新增营养规划 Agent
- 新增伤病风险评估 Agent
- 新增训练复盘 Agent
- 引入更完整的长期记忆和用户历史

到这一步，这个系统就会从“健身 AI demo”逐渐变成“健身智能服务中台”。

---

## 13. 一句话总结

这个项目当前已经具备了一个多 Agent 健身智能体后端的核心雏形：

- 上层有清晰入口
- 中间有工作流编排
- 下层有专用 Agent、RAG、MCP 工具
- 旁路有知识库和外部服务支撑

它最值得继续投入的方向，不是简单再加几个 prompt，而是把现有流程继续工程化、结构化、可观测化。这样它的可维护性、可靠性和扩展性都会上一个台阶。
