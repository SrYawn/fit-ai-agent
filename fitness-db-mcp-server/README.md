# Fitness Database MCP Server

基于 Spring AI MCP Server 的健身数据库查询服务，通过 stdio 方式部署。

## 功能

提供以下三个工具供 AI 模型调用：

1. **getUserProfile** - 查询用户基本信息
   - 参数：username (用户名)
   - 返回：用户的年龄、性别、身高、体重、健身目标、健身水平等信息

2. **getUserInjuries** - 查询用户伤病信息
   - 参数：userId (用户ID)
   - 返回：用户的所有伤病记录，包括伤病类型、部位、严重程度、恢复状态等

3. **getUserTrainingRecords** - 查询用户训练记录
   - 参数：userId (用户ID), startDate (可选), endDate (可选)
   - 返回：用户的训练历史，包括运动名称、组数、次数、重量、时长、消耗卡路里等

## 数据库配置

默认连接配置（可在 application.yml 中修改）：
- 数据库：fitness_db
- 地址：localhost:3306
- 用户名：root
- 密码：root

## 构建和运行

### 1. 构建项目

```bash
./mvnw clean package
```

### 2. 以 stdio 模式运行

```bash
java -jar target/fitness-db-mcp-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=stdio
```

### 3. 配置到 Claude Desktop

在 Claude Desktop 的配置文件中添加：

```json
{
  "mcpServers": {
    "fitness-db": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/fitness-db-mcp-server-0.0.1-SNAPSHOT.jar",
        "--spring.profiles.active=stdio"
      ]
    }
  }
}
```

## 技术栈

- Spring Boot 3.5.11
- Spring AI 1.0.0
- Spring AI MCP Server
- MySQL Connector
- Lombok

## 项目结构

```
fitness-db-mcp-server/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/zsr/fitnessdbmcpserver/
│   │   │       ├── FitnessDbMcpServerApplication.java
│   │   │       └── tools/
│   │   │           └── FitnessDbTool.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-stdio.yml
│   │       └── application-sse.yml
│   └── test/
└── pom.xml
```
