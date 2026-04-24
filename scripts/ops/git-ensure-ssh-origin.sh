#!/usr/bin/env bash
set -euo pipefail

REMOTE="${KIP_GATEWAY_REMOTE:-git@github.com:kippingmu/kip-gateway.git}"
REPO_DIR="$(git rev-parse --show-toplevel)"

git -C "${REPO_DIR}" remote set-url origin "${REMOTE}"
git -C "${REPO_DIR}" remote -v
