# kip-gateway K8s POC

这套目录现在是 `codex/k8s-poc` 的 K8s 切换入口，工作流会把镜像切到 ACR 上的 SHA tag，并在部署前后处理 Nacos 配置与健康检查。

最小部署顺序：

```bash
./scripts/ops/nacos-config.sh verify kip-gateway-k8s.yml deploy/k8s/nacos/kip-gateway-k8s.yml
./scripts/ops/k8s-secret.sh auto
./scripts/ops/k8s-apply.sh
./scripts/ops/k8s-smoke.sh
```

如果你确实要发布新的 Nacos 配置，再把 `verify` 换成 `put`：

```bash
./scripts/ops/nacos-config.sh put kip-gateway-k8s.yml deploy/k8s/nacos/kip-gateway-k8s.yml
```

当前 K8s secret 仍然需要：
- `REDIS_PASSWORD`
- `AUTH_JWT_SECRET`

`deploy/k8s/secret.yaml` 现在只是模板，不再携带真实值；真实值由 GitHub Actions secrets 或本地环境变量提供。
