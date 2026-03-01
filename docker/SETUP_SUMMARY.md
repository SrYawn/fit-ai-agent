# Docker 部署完成总结

## 已完成的工作

### 1. Docker 服务配置

创建了 `docker/` 目录，包含以下文件：

#### docker-compose.yml
配置了三个服务：
- **Elasticsearch (9200, 9300)**: 向量数据库，数据持久化到 `es_data` volume
- **Kibana (5601)**: ES 可视化平台
- **MySQL (3306)**: 关系数据库，数据持久化到 `mysql_data` volume

所有服务通过 `fitness-network` 网络互联，配置了健康检查。

#### init-db/01-init-schema.sql
创建了三个数据表：
1. **user_profile**: 用户基本信息（年龄、性别、身高、体重、健身目标、健身水平）
2. **user_injury**: 用户伤病信息（伤病类型、部位、严重程度、恢复状态）
3. **training_record**: 用户训练记录（训练日期、类型、运动名称、组数、次数、重量、完成状态）

#### init-db/02-sample-data.sql
插入了示例数据，包含 3 个用户、2 条伤病记录、5 条训练记录。

#### start.sh / stop.sh
便捷的启动和停止脚本。

#### README.md
详细的使用说明文档。

### 2. 应用配置更新

#### application.yml
- 禁用了 Ollama 自动配置（OllamaChatAutoConfiguration, OllamaEmbeddingAutoConfiguration）
- 禁用了 Elasticsearch 向量存储自动配置（避免 bean 冲突）
- 配置了 MySQL 数据源连接
- 配置了 Elasticsearch 连接地址

#### application-local.yml
- 添加了 MySQL 连接配置（fitness_user/fitness_pass）
- 添加了 Elasticsearch 连接配置（http://localhost:9200）

#### pom.xml
- 添加了 MySQL 驱动依赖（mysql-connector-j）

## 启动服务

```bash
cd docker
./start.sh
```

或者：

```bash
cd docker
docker compose up -d
```

## 验证服务

### Elasticsearch
```bash
curl http://localhost:9200
curl http://localhost:9200/_cluster/health
```

### MySQL
```bash
docker exec -it fitness-mysql mysql -ufitness_user -pfitness_pass fitness_db -e "SHOW TABLES;"
```

### Kibana
浏览器访问: http://localhost:5601

## 数据持久化

- Elasticsearch 数据存储在 Docker volume `es_data`
- MySQL 数据存储在 Docker volume `mysql_data`
- 容器停止或删除后，数据不会丢失
- 只有执行 `docker compose down -v`（或旧版 `docker-compose down -v`）才会删除数据

## 向量模型配置

- 已禁用 Ollama 自动配置
- 统一使用 Dashscope（阿里云）向量模型
- 配置在 `application-local.yml` 中的 API key: sk-b1c2232c742a448489c4c8a7c12603cb

## 后续工作

1. 实现 Elasticsearch 向量存储的 VectorStore Bean（替代当前的 SimpleVectorStore）
2. 配置 Spring AI Elasticsearch 向量存储参数（dimensions, distance-type 等）
3. 实现 MySQL MCP 服务，用于 Agent 获取用户画像数据
4. 基于用户基本信息、伤病信息、训练记录生成用户画像
5. 使用用户画像辅助 Agent 制定个性化健身计划
