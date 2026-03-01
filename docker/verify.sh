#!/bin/bash

# Docker 服务验证脚本

set -euo pipefail

echo "=========================================="
echo "Docker 服务验证"
echo "=========================================="
echo ""

# 兼容 docker compose (plugin) 与 docker-compose (legacy)
compose() {
    if docker compose version > /dev/null 2>&1; then
        docker compose "$@"
        return
    fi
    if command -v docker-compose > /dev/null 2>&1; then
        docker-compose "$@"
        return
    fi
    echo "❌ 未找到 docker compose / docker-compose 命令"
    exit 1
}

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker 未运行，请先启动 Docker"
    exit 1
fi

echo "✅ Docker 正在运行"
echo ""

# 进入 docker 目录
cd "$(dirname "$0")"

# 检查容器状态
echo "容器状态:"
compose ps
echo ""

# 检查 Elasticsearch
echo "检查 Elasticsearch..."
if curl -s http://localhost:9200 > /dev/null; then
    echo "✅ Elasticsearch 运行正常"
    curl -s http://localhost:9200/_cluster/health | python3 -m json.tool 2>/dev/null || echo "  (健康检查响应已接收)"
else
    echo "❌ Elasticsearch 未响应"
fi
echo ""

# 检查 Kibana
echo "检查 Kibana..."
if curl -s http://localhost:5601/api/status > /dev/null; then
    echo "✅ Kibana 运行正常"
else
    echo "❌ Kibana 未响应"
fi
echo ""

# 检查 MySQL
echo "检查 MySQL..."
if docker exec fitness-mysql mysqladmin ping -h localhost -u root -proot123456 > /dev/null 2>&1; then
    echo "✅ MySQL 运行正常"
    echo ""
    echo "数据库列表:"
    docker exec fitness-mysql mysql -u root -proot123456 -e "SHOW DATABASES;" 2>/dev/null | grep -v "mysql\|information_schema\|performance_schema\|sys"
    echo ""
    echo "fitness_db 表列表:"
    docker exec fitness-mysql mysql -u root -proot123456 fitness_db -e "SHOW TABLES;" 2>/dev/null
    echo ""
    echo "用户数据统计:"
    docker exec fitness-mysql mysql -u root -proot123456 fitness_db -e "SELECT COUNT(*) as user_count FROM user_profile;" 2>/dev/null
    docker exec fitness-mysql mysql -u root -proot123456 fitness_db -e "SELECT COUNT(*) as injury_count FROM user_injury;" 2>/dev/null
    docker exec fitness-mysql mysql -u root -proot123456 fitness_db -e "SELECT COUNT(*) as training_count FROM training_record;" 2>/dev/null
else
    echo "❌ MySQL 未响应"
fi
echo ""

# 检查 Adminer
echo "检查 Adminer..."
if curl -s http://localhost:8080 > /dev/null; then
    echo "✅ Adminer 运行正常"
else
    echo "❌ Adminer 未响应"
fi
echo ""

echo "=========================================="
echo "验证完成"
echo "=========================================="
echo ""
echo "访问地址:"
echo "  Elasticsearch: http://localhost:9200"
echo "  Kibana:        http://localhost:5601"
echo "  MySQL:         localhost:3306 (fitness_db)"
echo "  Adminer:       http://localhost:8080"
echo ""
