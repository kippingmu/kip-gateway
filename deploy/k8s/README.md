# kip-gateway K8s POC

最小部署顺序：

```bash
kubectl -n kip-poc apply -f deploy/k8s/configmap.yaml
kubectl -n kip-poc apply -f deploy/k8s/secret.yaml
kubectl -n kip-poc apply -f deploy/k8s/service.yaml
kubectl -n kip-poc apply -f deploy/k8s/deployment.yaml
```

如果更新了 Nacos 中的 `kip-gateway-k8s.yml`，再执行：

```bash
kubectl -n kip-poc rollout restart deployment/kip-gateway
kubectl -n kip-poc rollout status deployment/kip-gateway --timeout=180s
```

当前 POC 里 `kip-gateway-secret` 至少要包含：
- `REDIS_PASSWORD`
- `AUTH_JWT_SECRET`
