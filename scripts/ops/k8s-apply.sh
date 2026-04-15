#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/scripts/ops/k8s-env.sh"

for manifest in configmap.yaml service.yaml deployment.yaml; do
  apply_rendered "${K8S_MANIFEST_DIR}/${manifest}"
done

kubectl -n "$K8S_NAMESPACE" rollout status "deployment/${K8S_DEPLOYMENT}" --timeout="$ROLLOUT_TIMEOUT"
