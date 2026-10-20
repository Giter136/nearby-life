#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# 服务器端部署：从镜像仓库拉取并重启
#
#   cd /opt/hmdp-compose && ./deploy/deploy.sh
#
# 前置：
#   1. 已 docker login <registry>
#   2. .env 里的 IMAGE_REGISTRY / TAG 指向要部署的版本
#
# 服务器上不需要源码构建工具：这个脚本不会 build，只拉取运行。
# ---------------------------------------------------------------------------
set -euo pipefail

cd "$(dirname "$0")/.."

if [ ! -f .env ]; then
  echo "缺少 .env（至少需要 MYSQL_ROOT_PASSWORD / MYSQL_PASSWORD / IMAGE_REGISTRY / TAG）" >&2
  exit 1
fi

echo "==> 当前目标版本"
grep -E '^(IMAGE_REGISTRY|TAG)=' .env || true

echo "==> 拉取镜像"
docker compose pull

echo "==> 重建容器（不构建）"
docker compose up -d --no-build

echo "==> 状态"
docker compose ps
