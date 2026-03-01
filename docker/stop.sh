#!/bin/bash

# Docker 服务停止脚本

set -euo pipefail

echo "正在停止 Docker 服务..."
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
    exit 1
}

# 检查 Docker daemon 是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ 无法连接到 Docker daemon，请先启动 Docker Desktop"
    exit 1
fi

# 停止服务
compose stop

echo ""
echo "服务已停止"
echo ""
echo "如需完全删除容器（保留数据）: docker compose down"
echo "如需删除容器和数据: docker compose down -v"
echo ""
