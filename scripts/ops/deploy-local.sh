#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DEPLOY_PATH="/home/xiaoshichuan/apps/kip-gateway"
IMAGE_REPO="registry.cn-hangzhou.aliyuncs.com/kip-app/kip-gateway"
HEALTH_URL="http://127.0.0.1:9527/actuator/health"
TAG="${1:-}"

cd "$ROOT"
if [ -z "$TAG" ]; then
  TAG="$(git rev-parse --short=12 HEAD)"
fi

docker image inspect "${IMAGE_REPO}:${TAG}" >/dev/null
mkdir -p "$DEPLOY_PATH/.deploy-state"
cd "$DEPLOY_PATH"
export IMAGE_TAG="$TAG"
docker compose up -d
printf '%s' "$TAG" > "$DEPLOY_PATH/.deploy-state/current_image_tag"
sleep 3
curl -fsS "$HEALTH_URL" || true
echo
