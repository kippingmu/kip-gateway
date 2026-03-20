#!/usr/bin/env bash
set -euo pipefail
CONTAINER_NAME="kip-gateway"
LINES="${1:-120}"
docker logs --tail "$LINES" "$CONTAINER_NAME"
