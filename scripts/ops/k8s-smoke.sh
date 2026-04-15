#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/scripts/ops/k8s-env.sh"

PORT_FORWARD_LOG="$(mktemp)"
HEALTH_JSON="$(mktemp)"
ROUTES_JSON="$(mktemp)"
PORT_FORWARD_PID=""

cleanup() {
  if [ -n "$PORT_FORWARD_PID" ] && kill -0 "$PORT_FORWARD_PID" >/dev/null 2>&1; then
    kill "$PORT_FORWARD_PID" >/dev/null 2>&1 || true
    wait "$PORT_FORWARD_PID" >/dev/null 2>&1 || true
  fi
  rm -f "$PORT_FORWARD_LOG"
  rm -f "$HEALTH_JSON" "$ROUTES_JSON"
}

trap cleanup EXIT

kubectl -n "$K8S_NAMESPACE" port-forward "svc/${K8S_SERVICE}" "${SMOKE_PORT}:9527" --address 127.0.0.1 >"$PORT_FORWARD_LOG" 2>&1 &
PORT_FORWARD_PID=$!

for _ in $(seq 1 30); do
  if curl -fsS "http://127.0.0.1:${SMOKE_PORT}${SMOKE_HEALTH_PATH}" >"$HEALTH_JSON" 2>/dev/null; then
    break
  fi
  sleep 2
done

if ! curl -fsS "http://127.0.0.1:${SMOKE_PORT}${SMOKE_HEALTH_PATH}" >"$HEALTH_JSON"; then
  echo "Health endpoint did not become ready" >&2
  cat "$PORT_FORWARD_LOG" >&2 || true
  exit 1
fi

python3 - "$HEALTH_JSON" <<'PY'
import json
import sys
from pathlib import Path

health = json.loads(Path(sys.argv[1]).read_text())
if health.get("status") != "UP":
    raise SystemExit(f"Unexpected health status: {health.get('status')}")
print("Health verified")
PY

curl -fsS "http://127.0.0.1:${SMOKE_PORT}${SMOKE_ROUTES_PATH}" >"$ROUTES_JSON"

python3 - "$SMOKE_EXPECTED_ROUTE_IDS" "$ROUTES_JSON" <<'PY'
import json
import sys
from pathlib import Path

expected = [item.strip() for item in sys.argv[1].split(",") if item.strip()]
routes = json.loads(Path(sys.argv[2]).read_text())

route_ids = []
for route in routes:
    if isinstance(route, dict):
        for key in ("id", "routeId", "route_id"):
            if key in route and route[key]:
                route_ids.append(route[key])
                break

missing = [route_id for route_id in expected if route_id not in route_ids]
if missing:
    raise SystemExit(f"Missing expected routes: {', '.join(missing)}")

print("Route ids verified: " + ", ".join(route_ids))
PY
