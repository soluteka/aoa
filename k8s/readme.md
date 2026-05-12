# Manifests Kubernetes — AOA AIX Logs Agent

Recursos K8s para desplegar el agente en el clúster corporativo.

## Recursos

| Archivo | Descripción |
|---|---|
| `namespace.yaml` | Namespace `observability` |
| `configmap.yaml` | `application.yml` con lista de LPARs y umbrales |
| `secret.example.yaml` | Plantilla del Secret SSH (no se commitea con valores reales) |
| `deployment.yaml` | Deployment del agente (1 réplica, non-root, read-only FS) |
| `service.yaml` | ClusterIP exponiendo 8081/TCP, 5140/UDP, 5141/TCP |
| `networkpolicy.yaml` | Reglas ingress/egress |
| `kustomization.yaml` | Empaquetado con Kustomize |

## Despliegue rápido

### 1. Crear el Secret (no versionado)

```bash
kubectl create namespace observability --dry-run=client -o yaml | kubectl apply -f -

kubectl -n observability create secret generic aoa-aix-logs-agent-ssh \
  --from-file=id_rsa=/secure/path/id_rsa \
  --from-file=known_hosts=/secure/path/known_hosts \
  --from-literal=ssh-user=aoa_reader
```

### 2. Aplicar manifests

```bash
kubectl apply -k k8s/
```

### 3. Verificar

```bash
kubectl -n observability get pods -l app.kubernetes.io/name=aoa-aix-logs-agent
kubectl -n observability logs -l app.kubernetes.io/name=aoa-aix-logs-agent -f
kubectl -n observability get svc aoa-aix-logs-agent
```

### 4. Probar health

```bash
kubectl -n observability port-forward svc/aoa-aix-logs-agent 8081:8081
curl http://localhost:8081/actuator/health
```

## Personalización por entorno

Crear overlays con Kustomize:

```
k8s/
├── base/                     ← lo de este directorio
└── overlays/
    ├── dev/
    │   └── kustomization.yaml
    ├── qa/
    │   └── kustomization.yaml
    └── prod/
        └── kustomization.yaml
```

Ejemplo `overlays/prod/kustomization.yaml`:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
namespace: observability
resources:
  - ../../base
images:
  - name: ghcr.io/<org>/aoa-aix-logs-agent
    newTag: "1.0.0"
patches:
  - path: replicas-prod.yaml
```

## Puntos a coordinar con infra

1. **CIDR de LPARs AIX** en `networkpolicy.yaml` (actualmente `10.10.20.0/24`).
2. **Endpoint OTLP** del Collector corporativo en `deployment.yaml` (`OTEL_EXPORTER_OTLP_ENDPOINT`).
3. **Exposición externa** del Service si AIX está fuera del clúster (LoadBalancer/NodePort/Ingress TCP-UDP).
4. **StorageClass** y **PullSecret** del registry corporativo.
5. **PodDisruptionBudget** y **HPA** si la operación lo requiere.

## Seguridad

- ✅ `runAsNonRoot: true`
- ✅ `readOnlyRootFilesystem: true`
- ✅ `allowPrivilegeEscalation: false`
- ✅ `capabilities.drop: [ALL]`
- ✅ `seccompProfile: RuntimeDefault`
- ✅ Llave SSH montada con `mode 0400` desde Secret
- ✅ NetworkPolicy restrictiva ingress + egress