# 模型上下文协议（MCP）:: Spring AI 参考文档

刚接触 MCP？可先阅读我们的《MCP 快速入门》指南，获取简明介绍与动手示例。

模型上下文协议（MCP）是一种标准化协议，使 AI 模型能够以结构化方式与外部工具和资源交互。
你可以把它理解为 AI 模型与现实世界之间的桥梁，让模型通过一致接口访问数据库、API、文件系统以及其他外部服务。
它支持多种传输机制，以便在不同环境中灵活使用。

MCP Java SDK 提供了 Model Context Protocol 的 Java 实现，使 AI 模型与工具之间能够通过同步与异步通信模式进行标准化交互。

Spring AI 通过专用 Boot Starter 与 MCP Java 注解对 MCP 提供了全面支持，使构建可无缝连接外部系统的复杂 AI 应用变得前所未有地简单。
这意味着 Spring 开发者可以同时参与 MCP 生态的两端：既可以构建消费 MCP 服务器的 AI 应用，也可以创建 MCP 服务器，将基于 Spring 的服务暴露给更广泛的 AI 社区。
你可以使用 Spring Initializer 为 AI 应用快速引导 MCP 支持。

## MCP Java SDK 架构

本节概述 MCP Java SDK 架构。
关于 Spring AI 的 MCP 集成，请参阅 Spring AI MCP Boot Starters 文档。

Java 版 MCP 实现采用三层架构，通过关注点分离提升可维护性与灵活性：

图 1：MCP 栈架构

### 客户端/服务端层（顶层）

顶层处理主要应用逻辑与协议操作：

- `McpClient`：管理客户端操作与服务器连接
- `McpServer`：处理服务端协议操作与客户端请求

这两个组件都使用下方会话层进行通信管理。

### 会话层（中间层）

中间层管理通信模式并维护连接状态：

- `McpSession`：核心会话管理接口
- `McpClientSession`：客户端专用会话实现
- `McpServerSession`：服务端专用会话实现

### 传输层（底层）

底层处理实际消息传输与序列化：

- `McpTransport`：管理 JSON-RPC 消息的序列化与反序列化
- 支持多种传输实现（STDIO、HTTP/SSE、Streamable-HTTP 等）
- 为所有更高层通信提供基础

## MCP 客户端

MCP 客户端是 Model Context Protocol（MCP）架构中的关键组件，负责建立并管理与 MCP 服务器的连接。它实现协议的客户端侧，处理以下能力：

- 协议版本协商，确保与服务器兼容
- 能力协商，确定可用功能
- 消息传输与 JSON-RPC 通信
- 工具发现与执行
- 资源访问与管理
- 提示（Prompt）系统交互

可选特性：

- Roots 管理
- Sampling 支持
- 同步与异步操作

传输选项：

- 基于 Stdio 的传输（用于进程间通信）
- 基于 Java `HttpClient` 的 SSE 客户端传输
- 基于 WebFlux 的 SSE 客户端传输（用于响应式 HTTP 流）

## MCP 服务端

MCP 服务端是 Model Context Protocol（MCP）架构中的基础组件，向客户端提供工具、资源与能力。它实现协议的服务端侧，负责：

- 服务端协议操作实现
- 工具暴露与发现
- 基于 URI 的资源管理与访问
- 提示模板提供与处理
- 与客户端进行能力协商
- 结构化日志与通知
- 并发客户端连接管理
- 同步与异步 API 支持

传输实现：

- Stdio、Streamable-HTTP、Stateless Streamable-HTTP、SSE

如需基于底层 MCP Client/Server API 的详细实现指导，请参阅 MCP Java SDK 文档。
如果希望使用 Spring Boot 简化配置，请使用下文介绍的 MCP Boot Starters。

## Spring AI MCP 集成

Spring AI 通过以下 Spring Boot Starter 提供 MCP 集成：

### 客户端 Starter

- `spring-ai-starter-mcp-client`：核心 Starter，提供 STDIO、基于 Servlet 的 Streamable-HTTP、Stateless Streamable-HTTP 与 SSE 支持
- `spring-ai-starter-mcp-client-webflux`：基于 WebFlux 的 Streamable-HTTP、Stateless Streamable-HTTP 与 SSE 传输实现

### 服务端 Starter

#### STDIO

服务器类型 | 依赖 | 配置属性
--- | --- | ---
标准输入/输出（STDIO） | `spring-ai-starter-mcp-server` | `spring.ai.mcp.server.stdio=true`

#### WebMVC

服务器类型 | 依赖 | 配置属性
--- | --- | ---
SSE WebMVC | `spring-ai-starter-mcp-server-webmvc` | `spring.ai.mcp.server.protocol=SSE` 或留空
Streamable-HTTP WebMVC | `spring-ai-starter-mcp-server-webmvc` | `spring.ai.mcp.server.protocol=STREAMABLE`
Stateless Streamable-HTTP WebMVC | `spring-ai-starter-mcp-server-webmvc` | `spring.ai.mcp.server.protocol=STATELESS`

#### WebMVC（响应式）

服务器类型 | 依赖 | 配置属性
--- | --- | ---
SSE WebFlux | `spring-ai-starter-mcp-server-webflux` | `spring.ai.mcp.server.protocol=SSE` 或留空
Streamable-HTTP WebFlux | `spring-ai-starter-mcp-server-webflux` | `spring.ai.mcp.server.protocol=STREAMABLE`
Stateless Streamable-HTTP WebFlux | `spring-ai-starter-mcp-server-webflux` | `spring.ai.mcp.server.protocol=STATELESS`

## Spring AI MCP 注解

除了以编程方式配置 MCP 客户端与服务端外，Spring AI 还通过 MCP Annotations 模块为 MCP 服务端与客户端提供基于注解的方法处理。
这种方式使用简洁的声明式 Java 注解编程模型，简化了 MCP 操作的创建与注册。

MCP Annotations 模块可帮助开发者：

- 通过简单注解创建 MCP 工具、资源与提示
- 以声明式方式处理客户端通知与请求
- 减少样板代码并提升可维护性
- 自动生成工具参数的 JSON Schema
- 访问特殊参数与上下文信息

关键特性包括：

- 服务端注解：`@McpTool`、`@McpResource`、`@McpPrompt`、`@McpComplete`
- 客户端注解：`@McpLogging`、`@McpSampling`、`@McpElicitation`、`@McpProgress`
- 特殊参数：`McpSyncServerExchange`、`McpAsyncServerExchange`、`McpTransportContext`、`McpMeta`
- 自动发现：支持配置包包含/排除规则的注解扫描
- Spring Boot 集成：与 MCP Boot Starters 无缝集成

## 其他资源

- MCP Annotations 文档
- MCP Client Boot Starters 文档
- MCP Server Boot Starters 文档
- MCP Utilities 文档
- Model Context Protocol 规范
- Tool Calling MCP Client Boot Starters
