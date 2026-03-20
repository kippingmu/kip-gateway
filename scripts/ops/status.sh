#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DEPLOY_PATH="/home/xiaoshichuan/apps/kip-gateway"
CONTAINER_NAME="kip-gateway"
HEALTH_URL="http://127.0.0.1:9527/actuator/health"
STATE_FILE="$DEPLOY_PATH/.deploy-state/current_image_tag"

cd "$ROOT"
echo '[git]'
git status -sb
echo
echo '[head]'
git log --oneline -n 3
echo
if [ -f "$STATE_FILE" ]; then
  echo '[deploy-tag]'
  cat "$STATE_FILE"
  echo
  echo
fi
echo '[container]'
docker ps --filter "name=^/${CONTAINER_NAME}$" --format 'table {{.Names}}\t{{.Image}}\t{{.Ports}}\t{{.Status}}'
echo
echo '[health]'
curl -fsS "$HEALTH_URL" || true
echo
