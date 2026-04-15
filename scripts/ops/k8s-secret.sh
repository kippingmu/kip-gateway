#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/scripts/ops/k8s-env.sh"

MODE="${1:-auto}"
SECRET_TEMPLATE="${K8S_SECRET_TEMPLATE:-${ROOT}/deploy/k8s/secret.yaml}"
SECRET_NAME="${K8S_SECRET_NAME:-kip-gateway-secret}"

secret_exists() {
  kubectl -n "$K8S_NAMESPACE" get secret "$SECRET_NAME" >/dev/null 2>&1
}

verify_secret_values() {
  local secret_json
  secret_json="$(kubectl -n "$K8S_NAMESPACE" get secret "$SECRET_NAME" -o json)"
  local secret_file
  secret_file="$(mktemp)"
  trap 'rm -f "$secret_file"' RETURN
  printf '%s' "$secret_json" >"$secret_file"
  python3 - "$REDIS_PASSWORD" "$AUTH_JWT_SECRET" "$secret_file" <<'PY'
from base64 import b64decode
import json
import sys
from pathlib import Path

expected_redis = sys.argv[1]
expected_jwt = sys.argv[2]
payload = json.loads(Path(sys.argv[3]).read_text())
data = payload.get("data", {})

missing = [key for key in ("REDIS_PASSWORD", "AUTH_JWT_SECRET") if key not in data or not data[key]]
if missing:
    raise SystemExit(f"Missing secret keys: {', '.join(missing)}")

actual_redis = b64decode(data["REDIS_PASSWORD"]).decode()
actual_jwt = b64decode(data["AUTH_JWT_SECRET"]).decode()

if expected_redis and actual_redis != expected_redis:
    raise SystemExit("REDIS_PASSWORD does not match the provided value")
if expected_jwt and actual_jwt != expected_jwt:
    raise SystemExit("AUTH_JWT_SECRET does not match the provided value")

print("Secret verified")
PY
}

apply_secret() {
  if [ -z "$REDIS_PASSWORD" ] || [ -z "$AUTH_JWT_SECRET" ]; then
    echo "REDIS_PASSWORD and AUTH_JWT_SECRET are required to apply the secret" >&2
    exit 1
  fi

  apply_rendered "$SECRET_TEMPLATE"
}

case "$MODE" in
  auto)
    if [ -n "$REDIS_PASSWORD" ] && [ -n "$AUTH_JWT_SECRET" ]; then
      apply_secret
    else
      verify_secret_values
    fi
    ;;
  apply)
    apply_secret
    ;;
  verify)
    if secret_exists; then
      verify_secret_values
    else
      echo "Secret ${SECRET_NAME} does not exist in namespace ${K8S_NAMESPACE}" >&2
      exit 1
    fi
    ;;
  *)
    echo "Usage: $0 [auto|apply|verify]" >&2
    exit 1
    ;;
esac
