#!/usr/bin/env bash
set -euo pipefail
NACOS_ADDR="10.42.0.125:8848"
NACOS_USER="nacos"
NACOS_PASS="nacos8848"
GROUP="DEFAULT_GROUP"
TENANT=""
DEFAULT_DATA_ID="kip-gateway-dev.yml"

usage() {
  echo "Usage: $0 get [dataId]"
  echo "       $0 put [dataId] <file>"
  echo "       $0 put [dataId] --stdin"
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
  *)
    usage
    exit 1
    ;;
esac
