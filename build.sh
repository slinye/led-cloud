#!/bin/bash
# ============================================
# LED Cloud 一键构建脚本
# 用法:
#   ./build.sh              # 编译后端 + 构建所有 Docker 镜像
#   ./build.sh --skip-build # 跳过 Maven/NPM，仅 Docker build
#   ./build.sh --no-cache   # Docker 构建不使用缓存
#   ./build.sh --service gateway  # 仅构建指定服务
# ============================================
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

# ---------- 颜色输出 ----------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

info()  { echo -e "${CYAN}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
err()   { echo -e "${RED}[ERR]${NC}   $*"; }

# ---------- 参数解析 ----------
SKIP_BUILD=false
NO_CACHE=""
TARGET_SERVICE=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-build) SKIP_BUILD=true; shift ;;
    --no-cache)   NO_CACHE="--no-cache"; shift ;;
    --service)    TARGET_SERVICE="$2"; shift 2 ;;
    --help|-h)
      echo "用法: $0 [选项]"
      echo "  --skip-build    跳过 Maven/NPM 编译"
      echo "  --no-cache      Docker 构建不使用缓存"
      echo "  --service NAME  仅构建指定服务 (gateway/auth/device/content/schedule/frontend)"
      echo "  --help          显示帮助"
      exit 0
      ;;
    *) err "未知参数: $1"; exit 1 ;;
  esac
done

# ---------- 步骤 1: 编译 ----------
if [ "$SKIP_BUILD" = false ]; then
  info "=== 步骤 1/3: 后端编译 (Maven) ==="
  mvn clean package -DskipTests \
    -pl led-common,led-gateway,led-auth,led-device,led-content,led-schedule \
    -am --batch-mode --no-transfer-progress
  ok "Maven 编译完成"

  info "=== 步骤 2/3: 前端编译 (npm) ==="
  cd frontend
  npm ci 2>/dev/null || npm install
  npm run build
  cd "$PROJECT_ROOT"
  ok "前端编译完成"
else
  info "=== 跳过编译步骤 ==="
fi

# ---------- 步骤 2: Docker 构建 ----------
info "=== 步骤 3/3: Docker 镜像构建 ==="

# 先构建基础镜像
info "Building base image: led-cloud-base:latest"
docker build $NO_CACHE -t led-cloud-base:latest ./docker/java-base
ok "led-cloud-base:latest"

# 服务定义: 名称 -> Dockerfile 目录
build_service() {
  local name="$1"
  local context="$2"
  info "Building $name ..."
  docker build $NO_CACHE -t led-cloud/${name}:latest "$context"
  ok "led-cloud/${name}:latest"
}

if [ -n "$TARGET_SERVICE" ]; then
  case "$TARGET_SERVICE" in
    gateway)   build_service "led-gateway"   "./led-gateway" ;;
    auth)      build_service "led-auth"      "./led-auth" ;;
    device)    build_service "led-device"    "./led-device" ;;
    content)   build_service "led-content"   "./led-content" ;;
    schedule)  build_service "led-schedule"  "./led-schedule" ;;
    sentinel)  build_service "led-sentinel"  "./docker/sentinel" ;;
    frontend)  build_service "led-frontend"  "./frontend" ;;
    *) err "未知服务: $TARGET_SERVICE"; exit 1 ;;
  esac
else
  # Sentinel Dashboard
  docker build $NO_CACHE -t led-cloud/led-sentinel:latest ./docker/sentinel
  ok "led-cloud/led-sentinel:latest"

  # 五个 Java 微服务 (可并行)
  build_service "led-gateway"   "./led-gateway"
  build_service "led-auth"      "./led-auth"
  build_service "led-device"    "./led-device"
  build_service "led-content"   "./led-content"
  build_service "led-schedule"  "./led-schedule"

  # 前端
  build_service "led-frontend"  "./frontend"
fi

# ---------- 完成 ----------
echo ""
echo "========================================"
echo -e "  ${GREEN}构建完成!${NC}"
echo ""
echo -e "  基础镜像:  ${CYAN}led-cloud-base:latest${NC}"
echo -e "  启动所有服务: ${CYAN}docker compose up -d${NC}"
echo -e "  查看日志:     ${CYAN}docker compose logs -f${NC}"
echo -e "  停止服务:     ${CYAN}docker compose down${NC}"
echo "========================================"
