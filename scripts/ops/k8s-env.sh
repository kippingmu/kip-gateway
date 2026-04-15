#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

export K8S_NAMESPACE="${K8S_NAMESPACE:-kip-poc}"
export K8S_DEPLOYMENT="${K8S_DEPLOYMENT:-kip-gateway}"
export K8S_SERVICE="${K8S_SERVICE:-kip-gateway}"
export K8S_MANIFEST_DIR="${K8S_MANIFEST_DIR:-${ROOT}/deploy/k8s}"

export IMAGE_REPO="${IMAGE_REPO:-registry.cn-hangzhou.aliyuncs.com/kip-app/kip-gateway}"
export IMAGE_TAG="${IMAGE_TAG:-$(git -C "${ROOT}" rev-parse --short=12 HEAD)}"
export FULL_IMAGE="${FULL_IMAGE:-${IMAGE_REPO}:${IMAGE_TAG}}"

export ROLLOUT_TIMEOUT="${ROLLOUT_TIMEOUT:-300s}"
export SMOKE_PORT="${SMOKE_PORT:-19527}"
export SMOKE_HEALTH_PATH="${SMOKE_HEALTH_PATH:-/actuator/health}"
export SMOKE_ROUTES_PATH="${SMOKE_ROUTES_PATH:-/actuator/gateway/routes}"
export SMOKE_EXPECTED_ROUTE_IDS="${SMOKE_EXPECTED_ROUTE_IDS:-auth-api,kip-auth,app-api,kip-api,kip-app}"

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-k8s}"
export SERVER_PORT="${SERVER_PORT:-9527}"
export NACOS_SERVER_ADDR="${NACOS_SERVER_ADDR:-10.42.0.125:8848}"
export NACOS_NAMESPACE="${NACOS_NAMESPACE:-74a3fe73-35c1-474e-ade8-9bc460b3f398}"
export NACOS_GROUP="${NACOS_GROUP:-K8S_POC}"
export NACOS_CONFIG_ENABLED="${NACOS_CONFIG_ENABLED:-true}"
export NACOS_DISCOVERY_ENABLED="${NACOS_DISCOVERY_ENABLED:-false}"
export NACOS_REGISTER_ENABLED="${NACOS_REGISTER_ENABLED:-false}"
export NACOS_AUTO_REGISTRATION_ENABLED="${NACOS_AUTO_REGISTRATION_ENABLED:-false}"
export KIP_APP_BASE_URL="${KIP_APP_BASE_URL:-http://kip-app:8010}"
export KIP_AUTH_BASE_URL="${KIP_AUTH_BASE_URL:-http://kip-auth:5001}"
export REDIS_HOST="${REDIS_HOST:-kip-redis}"
export REDIS_PORT="${REDIS_PORT:-6379}"
export REDIS_PASSWORD="${REDIS_PASSWORD:-}"
export AUTH_JWT_SECRET="${AUTH_JWT_SECRET:-}"

render_template() {
  local template_file="$1"
  python3 - "$template_file" <<'PY'
from pathlib import Path
import os
import sys
from string import Template

template_path = Path(sys.argv[1])
sys.stdout.write(Template(template_path.read_text()).substitute(os.environ))
PY
}

apply_rendered() {
  local template_file="$1"
  render_template "$template_file" | kubectl -n "$K8S_NAMESPACE" apply -f -
}
