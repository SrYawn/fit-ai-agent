# Docker 服务部署说明

本目录包含 fit-ai-agent 项目所需的 Docker 服务配置。

## 服务列表

### 1. Elasticsearch (端口 9200, 9300)
- 作为向量数据库，存储健身知识向量
- 数据持久化到 Docker volume: `es_data`
- 禁用了安全认证以简化开发环境配置

### 2. Kibana (端口 5601)
- Elasticsearch 可视化平台
- 访问地址: http://localhost:5601

### 3. MySQL (端口 3306)
- 数据库名: `fitness_db`
- Root 密码: `root123456`
- 应用用户: `fitness_user` / `fitness_pass`
- 数据持久化到 Docker volume: `mysql_data`

### 4. Adminer (端口 8080)
- MySQL 轻量级可视化管理工具
- 访问地址: http://localhost:8080
- 登录建议：
  - 系统: `MySQL`
  - 服务器: `mysql`（容器内）或 `host.docker.internal` / `127.0.0.1`（宿主机）
  - 用户名: `fitness_user`（或 `root`）
  - 密码: `fitness_pass`（或 `root123456`）
  - 数据库: `fitness_db`

#### 数据表结构

**user_profile** - 用户基本信息表
- 存储用户年龄、性别、身高、体重、健身目标、健身水平等

**user_injury** - 用户伤病信息表
- 存储用户伤病类型、部位、严重程度、恢复状态等

**training_record** - 用户训练记录表
- 存储训练日期、类型、运动名称、组数、次数、重量、完成状态等

## 快速启动

### 启动所有服务
```bash
cd docker
docker compose up -d
```

### 查看服务状态
```bash
docker compose ps
```

### 查看服务日志
```bash
# 查看所有服务日志
docker compose logs -f

# 查看特定服务日志
docker compose logs -f elasticsearch
docker compose logs -f mysql
docker compose logs -f kibana
docker compose logs -f adminer
```

### 停止服务
```bash
docker compose stop
```

### 停止并删除容器（保留数据）
```bash
docker compose down
```

### 停止并删除容器和数据卷
```bash
docker compose down -v
```

## 服务验证

### Elasticsearch
```bash
curl http://localhost:9200
curl http://localhost:9200/_cluster/health
```

### MySQL
```bash
docker exec -it fitness-mysql mysql -uroot -proot123456 -e "SHOW DATABASES;"
docker exec -it fitness-mysql mysql -uroot -proot123456 fitness_db -e "SHOW TABLES;"
```

### Kibana
浏览器访问: http://localhost:5601

### Adminer
浏览器访问: http://localhost:8080

## 数据持久化

所有数据都通过 Docker volumes 持久化存储：
- `es_data`: Elasticsearch 数据
- `mysql_data`: MySQL 数据

即使容器停止或删除，数据仍然保留。只有执行 `docker compose down -v`（或旧版 `docker-compose down -v`）才会删除数据。

## 应用配置

在 `application-local.yml` 中配置连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fitness_db
    username: fitness_user
    password: fitness_pass

  elasticsearch:
    uris: http://localhost:9200
```
