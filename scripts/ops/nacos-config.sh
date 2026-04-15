#!/usr/bin/env bash
set -euo pipefail
NACOS_ADDR="${NACOS_ADDR:-10.42.0.125:8848}"
NACOS_USER="${NACOS_USER:-nacos}"
NACOS_PASS="${NACOS_PASS:-nacos8848}"
GROUP="${NACOS_GROUP:-DEFAULT_GROUP}"
TENANT="${NACOS_NAMESPACE:-}"
DEFAULT_DATA_ID="${DEFAULT_DATA_ID:-kip-gateway-dev.yml}"

usage() {
  echo "Usage: $0 get [dataId]"
  echo "       $0 put [dataId] <file>"
  echo "       $0 put [dataId] --stdin"
  echo "       $0 verify [dataId] <file>"
}

token() {
  curl -fsS -X POST "http://${NACOS_ADDR}/nacos/v1/auth/users/login" \
    -d "username=${NACOS_USER}&password=${NACOS_PASS}" | \
    python3 -c 'import json,sys; print(json.load(sys.stdin)["accessToken"])'
}

ACTION="${1:-get}"
DATA_ID="${2:-$DEFAULT_DATA_ID}"
ACCESS_TOKEN="$(token)"

case "$ACTION" in
  get)
    curl -fsS -G "http://${NACOS_ADDR}/nacos/v1/cs/configs" \
      --data-urlencode "accessToken=${ACCESS_TOKEN}" \
      --data-urlencode "tenant=${TENANT}" \
      --data-urlencode "group=${GROUP}" \
      --data-urlencode "dataId=${DATA_ID}"
    ;;
  put)
    INPUT="${3:-}"
    TMP=""
    if [ "$INPUT" = "--stdin" ] || [ -z "$INPUT" ]; then
      TMP="$(mktemp)"
      cat > "$TMP"
      INPUT="$TMP"
    fi
    test -f "$INPUT"
    curl -fsS -X POST "http://${NACOS_ADDR}/nacos/v1/cs/configs" \
      --data-urlencode "accessToken=${ACCESS_TOKEN}" \
      --data-urlencode "tenant=${TENANT}" \
      --data-urlencode "group=${GROUP}" \
      --data-urlencode "dataId=${DATA_ID}" \
      --data-urlencode "type=yaml" \
      --data-urlencode "content@${INPUT}"
    if [ -n "$TMP" ]; then
      rm -f "$TMP"
    fi
    ;;
  verify)
    INPUT="${3:-}"
    if [ -z "$INPUT" ] || [ ! -f "$INPUT" ]; then
      echo "verify requires a local file path" >&2
      exit 1
    fi
    TMP_REMOTE="$(mktemp)"
    trap 'rm -f "$TMP_REMOTE"' EXIT
    curl -fsS -G "http://${NACOS_ADDR}/nacos/v1/cs/configs" \
      --data-urlencode "accessToken=${ACCESS_TOKEN}" \
      --data-urlencode "tenant=${TENANT}" \
      --data-urlencode "group=${GROUP}" \
      --data-urlencode "dataId=${DATA_ID}" > "$TMP_REMOTE"
    if cmp -s "$TMP_REMOTE" "$INPUT"; then
      echo "Nacos config verified: ${DATA_ID}"
    else
      echo "Nacos config mismatch: ${DATA_ID}" >&2
      diff -u "$TMP_REMOTE" "$INPUT" || true
      exit 1
    fi
    ;;
  *)
    usage
    exit 1
    ;;
esac
