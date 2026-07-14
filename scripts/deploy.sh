#!/bin/bash
# ============================================
# LED Cloud 一键部署脚本
# 用法: bash scripts/deploy.sh [dev|staging|prod]
# ============================================
set -e

ENV=${1:-dev}
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

echo "========================================"
echo "  LED Cloud 部署脚本"
echo "  环境: ${ENV}"
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================"

# ────────── 颜色定义 ──────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# ────────── 加载环境变量 ──────────
ENV_FILE=".env.${ENV}"
if [ -f "$ENV_FILE" ]; then
    echo -e "${GREEN}[1/6]${NC} 加载环境变量: ${ENV_FILE}"
    set -a
    source "$ENV_FILE"
    set +a
else
    echo -e "${YELLOW}[!]${NC} 未找到 ${ENV_FILE}，使用默认值"
fi

# ────────── 选择 compose 文件 ──────────
case $ENV in
    prod)
        COMPOSE_FILE="docker-compose.prod.yml"
        ;;
    staging)
        COMPOSE_FILE="docker-compose.prod.yml"
        ;;
    dev|*)
        COMPOSE_FILE="docker-compose.yml"
        ;;
esac

# ────────── 后端编译 ──────────
echo -e "${GREEN}[2/6]${NC} Maven 编译后端..."
mvn clean package -DskipTests \
    -pl led-common,led-gateway,led-auth,led-device,led-content,led-schedule \
    -am --batch-mode --no-transfer-progress
echo -e "${GREEN}  ✓ Maven 编译完成${NC}"

# ────────── 前端编译 ──────────
echo -e "${GREEN}[3/6]${NC} 编译前端..."
cd frontend
npm ci --silent
npm run build
cd "$PROJECT_DIR"
echo -e "${GREEN}  ✓ 前端编译完成${NC}"

# ────────── Docker 构建 ──────────
echo -e "${GREEN}[4/6]${NC} Docker 镜像构建..."
# 先构建基础镜像
docker build -t led-management-backend:latest ./docker/backend

# 根据环境选择构建方式
if [ "$COMPOSE_FILE" = "docker-compose.prod.yml" ]; then
    # 生产环境：按 prod compose 构建
    DOCKER_REGISTRY="${DOCKER_REGISTRY:-led-cloud}"

    # 构建各服务镜像（不含基础服务 + 依赖 prod compose 的 image 名）
    docker build -t "${DOCKER_REGISTRY}/led-gateway:latest"   ./led-gateway
    docker build -t "${DOCKER_REGISTRY}/led-auth:latest"      ./led-auth
    docker build -t "${DOCKER_REGISTRY}/led-device:latest"    ./led-device
    docker build -t "${DOCKER_REGISTRY}/led-content:latest"   ./led-content
    docker build -t "${DOCKER_REGISTRY}/led-schedule:latest"  ./led-schedule
    docker build -t "${DOCKER_REGISTRY}/led-sentinel:latest"  ./docker/sentinel
    docker build -t "${DOCKER_REGISTRY}/led-frontend:latest"  ./frontend
else
    # 开发环境：直接用 docker compose build
    docker compose -f "$COMPOSE_FILE" build gateway auth device content schedule frontend sentinel
fi
echo -e "${GREEN}  ✓ Docker 镜像构建完成${NC}"

# ────────── 镜像推送 (仅 staging/prod) ──────────
if [ "$ENV" != "dev" ]; then
    echo -e "${GREEN}[5/6]${NC} 推送镜像到 Registry..."
    REGISTRY="${DOCKER_REGISTRY:-led-cloud}"

    for svc in led-gateway led-auth led-device led-content led-schedule led-sentinel led-frontend; do
        echo "  推送 ${REGISTRY}/${svc}:latest ..."
        docker push "${REGISTRY}/${svc}:latest"
    done
    echo -e "${GREEN}  ✓ 镜像推送完成${NC}"
else
    echo -e "${YELLOW}[5/6]${NC} 跳过镜像推送 (dev 环境)"
fi

# ────────── 启动服务 ──────────
echo -e "${GREEN}[6/6]${NC} 启动/更新服务..."
docker compose -f "$COMPOSE_FILE" up -d --remove-orphans

# 清理旧镜像
docker image prune -f

echo ""
echo "========================================"
echo -e "  ${GREEN}✓ 部署完成!${NC}"
echo "  环境: ${ENV}"
echo "  前端: http://localhost"
echo "========================================"
echo ""
echo "服务状态:"
docker compose -f "$COMPOSE_FILE" ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
