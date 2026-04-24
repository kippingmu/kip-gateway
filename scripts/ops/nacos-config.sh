#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

usage() {
  cat <<'EOF'
Usage:
  nacos-config.sh get [dataId]
  nacos-config.sh put <dataId> <file|--stdin>
  nacos-config.sh verify <dataId> <file|--stdin>
EOF
}

die() {
  echo "[error] $*" >&2
  exit 1
}

ACTION="${1:-get}"
TARGET="${2:-kip-gateway-k8s.yml}"
INPUT_PATH="${3:-}"

case "${ACTION}" in
  -h|--help|help)
    usage
    exit 0
    ;;
esac

DATA_ID="${TARGET}"
NACOS_ADDR="${NACOS_ADDR:-10.42.0.125:8848}"
NACOS_USER="${NACOS_USER:-nacos}"
NACOS_PASS="${NACOS_PASS:-nacos8848}"
NACOS_GROUP="${NACOS_GROUP:-K8S_POC}"
NACOS_NAMESPACE="${NACOS_NAMESPACE:-74a3fe73-35c1-474e-ade8-9bc460b3f398}"

nacos_token() {
  curl -fsS -X POST "http://${NACOS_ADDR}/nacos/v1/auth/users/login" \
    -d "username=${NACOS_USER}&password=${NACOS_PASS}" | \
    python3 -c 'import json,sys; print(json.load(sys.stdin)["accessToken"])'
}

fetch_remote_to_file() {
  local output_file="$1"
  curl -fsS -G "http://${NACOS_ADDR}/nacos/v1/cs/configs" \
    --data-urlencode "accessToken=${TOKEN}" \
    --data-urlencode "tenant=${NACOS_NAMESPACE}" \
    --data-urlencode "group=${NACOS_GROUP}" \
    --data-urlencode "dataId=${DATA_ID}" \
    -o "${output_file}"
}

files_match() {
  python3 - "$1" "$2" <<'PY'
import pathlib
import sys

left = pathlib.Path(sys.argv[1]).read_text().rstrip()
right = pathlib.Path(sys.argv[2]).read_text().rstrip()
sys.exit(0 if left == right else 1)
PY
}

TOKEN="$(nacos_token)"

case "${ACTION}" in
  get)
    fetch_remote_to_file /dev/stdout
    ;;
  put|verify)
    if [ -z "${INPUT_PATH}" ]; then
      INPUT_PATH="${ROOT}/deploy/k8s/nacos/kip-gateway-k8s.yml"
    fi

    CLEANUP_LOCAL=false
    if [ "${INPUT_PATH}" = "--stdin" ]; then
      LOCAL_FILE="$(mktemp)"
      CLEANUP_LOCAL=true
      cat > "${LOCAL_FILE}"
    else
      LOCAL_FILE="${INPUT_PATH}"
      [ -f "${LOCAL_FILE}" ] || die "file not found: ${LOCAL_FILE}"
    fi

    REMOTE_FILE="$(mktemp)"
    cleanup() {
      rm -f "${REMOTE_FILE}"
      if [ "${CLEANUP_LOCAL}" = true ]; then
        rm -f "${LOCAL_FILE}"
      fi
    }
    trap cleanup EXIT

    if [ "${ACTION}" = "put" ]; then
      curl -fsS -X POST "http://${NACOS_ADDR}/nacos/v1/cs/configs" \
        --data-urlencode "accessToken=${TOKEN}" \
        --data-urlencode "tenant=${NACOS_NAMESPACE}" \
        --data-urlencode "group=${NACOS_GROUP}" \
        --data-urlencode "dataId=${DATA_ID}" \
        --data-urlencode "type=yaml" \
        --data-urlencode "content@${LOCAL_FILE}" >/dev/null
    fi

    for _ in 1 2 3 4 5; do
      fetch_remote_to_file "${REMOTE_FILE}"
      if files_match "${LOCAL_FILE}" "${REMOTE_FILE}"; then
        echo "[ok] Nacos config ${ACTION} for ${DATA_ID}"
        exit 0
      fi
      sleep 2
    done

    echo "[error] Nacos config mismatch: ${DATA_ID}" >&2
    diff -u "${REMOTE_FILE}" "${LOCAL_FILE}" || true
    exit 1
    ;;
  *)
    usage
    exit 1
    ;;
esac
