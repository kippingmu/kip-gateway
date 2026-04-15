# K8s Cutover Baseline for `kip-gateway`

This is the repo-local quick reference for agents working on the `codex/k8s-poc` cutover.
The shared server-side source of truth already exists at:
- `/Users/xiaoshichuan/icodex/server-st/kip/agent-server-baseline.md`
- `/Users/xiaoshichuan/icodex/server-st/kip/agent-server-baseline.yaml`
- `/home/xiaoshichuan/kip-shared/kip/scripts/shared/`

## Core Facts

- Kubernetes namespace: `kip-poc`
- Deployment name: `kip-gateway`
- Service name: `kip-gateway`
- Image repository: `registry.cn-hangzhou.aliyuncs.com/kip-app/kip-gateway`
- K8s profile port: `9527`
- Nacos config dataId: `kip-gateway-k8s.yml`
- Nacos group: `K8S_POC`
- Nacos namespace: `74a3fe73-35c1-474e-ade8-9bc460b3f398`
- Nacos server: `10.42.0.125:8848`
- K8s secret name: `kip-gateway-secret`
- Required secret keys: `REDIS_PASSWORD`, `AUTH_JWT_SECRET`

## Shared Entry Points

- `scripts/ops/k8s-secret.sh`
- `scripts/ops/k8s-apply.sh`
- `scripts/ops/k8s-smoke.sh`
- `scripts/ops/nacos-config.sh`

## Cutover Order

1. Sync or verify `deploy/k8s/nacos/kip-gateway-k8s.yml`.
2. Apply or verify `kip-gateway-secret`.
3. Apply the rendered configmap, service, and deployment.
4. Wait for rollout.
5. Port-forward the Service and verify health plus gateway route IDs.

## Smoke Expectations

- Health endpoint: `/actuator/health`
- Route endpoint: `/actuator/gateway/routes`
- Expected route IDs: `auth-api`, `kip-auth`, `app-api`, `kip-api`, `kip-app`

## Notes

- `deploy/k8s/secret.yaml` is now a template, not a live secret file.
- `push` deployments default to publishing the repo-local Nacos config because the live `K8S_POC` content was already drifting.
- `workflow_dispatch` can still run in `verify` mode for comparison-only checks.
