# 真实企业部署结构

``` 生产常用
k8s/
 ├── namespace.yaml
 ├── deployment.yaml
 ├── service.yaml
 ├── configmap.yaml
 ├── secret.yaml
 ├── ingress.yaml
 └── hpa.yaml
```

# 或者使用Helm Charts

``` 生产常用
helm chart/
 ├── templates/
 │   ├── deployment.yaml
 │   ├── service.yaml
 │   ├── ingress.yaml
 │   ├── configmap.yaml
 │   └── hpa.yaml
 └── values.yaml
```

# 微服务常用
```
demo-user-svc/                      # 一个微服务（demo-user-svc）的 K8s 部署包根目录
└── k8s/                            # K8s 清单目录（建议与应用代码仓库同放）
    ├── 00-namespace.yaml           # 命名空间：提供逻辑隔离与统一管理边界
    ├── 10-configmap.yaml           # 非敏感配置：Spring profile/JVM 参数/日志级别等
    ├── 11-secret.yaml              # 敏感配置：DB 密码、Token 等（建议后续接外部密钥系统）
    ├── 20-serviceaccount.yaml      # Pod 身份：RBAC/是否挂载 token 等安全控制
    ├── 30-deployment.yaml          # 核心：定义 Pod（镜像、资源、探针、滚动更新、优雅停机）
    ├── 40-service.yaml             # 服务发现：用 selector 选中 Pod，提供稳定 ClusterIP 与负载均衡
    ├── 50-ingress.yaml             # 对外入口：域名/路径路由到 Service（需要 Ingress Controller）
    ├── 60-hpa.yaml                 # 弹性伸缩：根据 CPU 等指标自动扩缩容 Deployment 副本
    ├── 70-pdb.yaml                 # 可用性预算：节点维护/驱逐时保证至少 N 个 Pod 可用
    └── 80-networkpolicy.yaml       # 网络隔离：限制入/出站流量（需 CNI 支持 NetworkPolicy）
```