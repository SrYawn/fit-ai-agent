# Fitness Database MCP Server 使用指南

## 快速开始

### 1. 准备数据库

确保 MySQL 数据库已启动,并执行初始化脚本:

```bash
# 初始化数据库结构
mysql -u root -p < ../docker/init-db/01-init-schema.sql

# (可选) 插入示例数据
mysql -u root -p < sample-data.sql
```

### 2. 配置数据库连接

编辑 `src/main/resources/application.yml` 修改数据库连接信息:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fitness_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: root
    password: your_password
```

### 3. 构建项目

```bash
./mvnw clean package -DskipTests
```

### 4. 运行服务

#### 方式一: 使用启动脚本

```bash
./run-stdio.sh
```

#### 方式二: 直接运行 JAR

```bash
java -jar target/fitness-db-mcp-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=stdio
```

### 5. 配置 Claude Desktop

将 `claude-desktop-config.json` 中的配置添加到 Claude Desktop 配置文件中:

**macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`

**Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "fitness-db": {
      "command": "java",
      "args": [
        "-jar",
        "/完整路径/fitness-db-mcp-server-0.0.1-SNAPSHOT.jar",
        "--spring.profiles.active=stdio"
      ],
      "env": {
        "SPRING_DATASOURCE_URL": "jdbc:mysql://localhost:3306/fitness_db",
        "SPRING_DATASOURCE_USERNAME": "root",
        "SPRING_DATASOURCE_PASSWORD": "your_password"
      }
    }
  }
}
```

重启 Claude Desktop 后即可使用。

## 可用工具

### 1. getUserProfile

查询用户基本信息

**参数:**
- `username` (String): 用户名

**示例:**
```
请查询用户 zhangsan 的基本信息
```

**返回信息:**
- 用户ID
- 用户名
- 年龄
- 性别
- 身高
- 体重
- 健身目标
- 健身水平

### 2. getUserInjuries

查询用户伤病信息

**参数:**
- `userId` (Long): 用户ID

**示例:**
```
查询用户ID为1的伤病记录
```

**返回信息:**
- 伤病记录ID
- 伤病类型
- 伤病部位
- 严重程度
- 详细描述
- 恢复状态
- 受伤日期

### 3. getUserTrainingRecords

查询用户训练记录

**参数:**
- `userId` (Long): 用户ID (必填)
- `startDate` (String): 开始日期,格式 YYYY-MM-DD (可选)
- `endDate` (String): 结束日期,格式 YYYY-MM-DD (可选)

**示例:**
```
查询用户ID为1在2026年2月的训练记录
```

**返回信息:**
- 训练记录ID
- 训练日期
- 训练类型
- 运动名称
- 组数
- 次数
- 重量
- 持续时间
- 消耗卡路里
- 完成状态
- 备注

## 使用示例

在 Claude Desktop 中,你可以这样提问:

1. **查询用户信息:**
   - "帮我查询用户 zhangsan 的基本信息"
   - "zhangsan 的身高体重是多少?"

2. **查询伤病信息:**
   - "查看用户ID为1的伤病记录"
   - "这个用户有哪些伤病?"

3. **查询训练记录:**
   - "查询用户ID为1最近的训练记录"
   - "查询用户1在2026年2月20日到2月28日的训练情况"
   - "这个用户最近做了哪些训练?"

## 故障排查

### 连接数据库失败

1. 检查 MySQL 是否运行: `mysql -u root -p`
2. 检查数据库是否存在: `SHOW DATABASES;`
3. 检查用户权限
4. 确认连接配置正确

### MCP 服务无法启动

1. 检查 Java 版本: `java -version` (需要 Java 21+)
2. 检查 JAR 文件是否存在
3. 查看日志输出排查错误

### Claude Desktop 无法识别工具

1. 确认配置文件路径正确
2. 确认 JSON 格式正确
3. 重启 Claude Desktop
4. 检查 MCP 服务是否正常运行

## 技术架构

- **Spring Boot 3.5.11**: 应用框架
- **Spring AI 1.0.0**: AI 集成框架
- **Spring AI MCP Server**: MCP 协议实现
- **Spring JDBC**: 数据库访问
- **MySQL Connector**: MySQL 驱动
- **Lombok**: 代码简化

## 项目结构

```
fitness-db-mcp-server/
├── src/
│   ├── main/
│   │   ├── java/com/zsr/fitnessdbmcpserver/
│   │   │   ├── FitnessDbMcpServerApplication.java  # 主应用类
│   │   │   └── tools/
│   │   │       └── FitnessDbTool.java              # 工具实现
│   │   └── resources/
│   │       ├── application.yml                      # 主配置
│   │       ├── application-stdio.yml                # stdio 模式配置
│   │       └── application-sse.yml                  # SSE 模式配置
│   └── test/
├── pom.xml                                          # Maven 配置
├── README.md                                        # 项目说明
├── USAGE.md                                         # 使用指南
├── run-stdio.sh                                     # 启动脚本
├── sample-data.sql                                  # 示例数据
└── claude-desktop-config.json                       # Claude Desktop 配置示例
```

## 扩展开发

### 添加新工具

1. 在 `FitnessDbTool.java` 中添加新方法
2. 使用 `@Tool` 注解标记方法
3. 使用 `@ToolParam` 注解标记参数
4. 实现查询逻辑
5. 重新构建项目

示例:

```java
@Tool(description = "Query user's latest training record")
public String getLatestTraining(@ToolParam(description = "User ID") Long userId) {
    // 实现逻辑
}
```

### 修改数据库连接

可以通过环境变量覆盖配置:

```bash
export SPRING_DATASOURCE_URL="jdbc:mysql://your-host:3306/fitness_db"
export SPRING_DATASOURCE_USERNAME="your-username"
export SPRING_DATASOURCE_PASSWORD="your-password"
```

## 许可证

本项目基于 Spring Boot 和 Spring AI 构建,遵循相应的开源许可证。
