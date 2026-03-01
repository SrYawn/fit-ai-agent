#!/bin/bash

# Docker 服务启动脚本

set -euo pipefail

echo "正在启动 Docker 服务..."
echo ""

# 进入 docker 目录
cd "$(dirname "$0")"

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
    echo "请安装 Docker Desktop（推荐）或安装 docker-compose。"
    exit 1
}

# 检查 Docker daemon 是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ 无法连接到 Docker daemon。"
    echo ""
    echo "请先启动 Docker："
    echo "  - macOS: 打开 Docker Desktop，并等待其状态变为 Running"
    echo "  - 或命令行: open -a Docker"
    exit 1
fi

# 启动服务
compose up -d

echo ""
echo "等待服务启动..."
sleep 5

# 检查服务状态
echo ""
echo "服务状态:"
compose ps

echo ""
echo "=========================================="
echo "服务访问地址:"
echo "=========================================="
echo "Elasticsearch: http://localhost:9200"
echo "Kibana:        http://localhost:5601"
echo "MySQL:         localhost:3306"
echo "Adminer:       http://localhost:8080"
echo "  - Database:  fitness_db"
echo "  - Username:  fitness_user"
echo "  - Password:  fitness_pass"
echo "=========================================="
echo ""
echo "查看日志: docker compose logs -f"
echo "停止服务: docker compose stop"
echo "删除服务: docker compose down"
echo ""
