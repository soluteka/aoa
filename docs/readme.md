# AOA AIX Logs Agent

Agente Java que recolecta telemetría desde hosts **IBM AIX** (LPARs) y la publica vía **OTLP/gRPC** a un OpenTelemetry Collector corporativo.

- **Métricas:** `lparstat` (CPU, LPAR, paging)
- **Logs:** `errpt` (vía Syslog UDP/TCP)
- **Alertas:** evaluación local de umbrales sobre paging

> Este componente **no incluye** Collector, Prometheus ni Grafana. Solo emite OTLP hacia la plataforma corporativa de observabilidad.

---

## 🚀 Quick Start

### Requisitos

- Java 21
- Maven 3.9+
- Docker (opcional para empaquetado)
- Acceso SSH (llave privada) a los LPARs AIX objetivo
- Endpoint OTLP/gRPC del Collector corporativo

### Build local

```bash
mvn clean package -DskipTests
java -jar target/aoa-aix-logs-agent-*.jar
```

### Run con Docker

```bash
docker build -t aoa-aix-logs-agent:1.0.0 .
docker run --rm \
  -e OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector.obs.svc:4317 \
  -e SPRING_PROFILES_ACTIVE=prod,ssh \
  -e DEPLOYMENT_ENV=prod \
  -e AIX_SSH_USER=aoa_reader \
  -e AIX_SSH_KEY_PATH=/etc/aoa/keys/id_rsa \
  -v /path/to/keys:/etc/aoa/keys:ro \
  -v /path/to/application-prod.yml:/app/config/application.yml:ro \
  -p 5140:5140/udp -p 5141:5141 -p 8081:8081 \
  aoa-aix-logs-agent:1.0.0
```

---

## ⚙️ Configuración

### Variables de entorno

| Variable | Obligatoria | Default | Descripción |
|---|---|---|---|
| `OTEL_EXPORTER_OTLP_ENDPOINT` | ✅ | `http://otel-collector:4317` | URL del Collector OTLP/gRPC |
| `SPRING_PROFILES_ACTIVE` | ✅ | `prod,ssh` | Perfiles activos |
| `DEPLOYMENT_ENV` | ✅ | `prod` | `dev` \| `qa` \| `prod` |
| `AIX_SSH_USER` | ✅ | — | Usuario lectura-solo en AIX |
| `AIX_SSH_KEY_PATH` | ✅ | `/etc/aoa/keys/id_rsa` | Llave privada SSH |
| `AIX_SCHEDULER_INTERVAL` | ❌ | `10` | Intervalo de muestreo (s) |
| `AIX_SYSLOG_UDP_PORT` | ❌ | `5140` | Puerto syslog UDP |
| `AIX_SYSLOG_TCP_PORT` | ❌ | `5141` | Puerto syslog TCP |

### `application.yml`

Ver [`src/main/resources/application.yml`](src/main/resources/application.yml) y los perfiles `application-dev.yml`, `application-prod.yml`.

Bloque clave — lista de LPARs a muestrear:

```yaml
aix:
  lpars:
    - name: lpar-prod-01
      host: 10.10.20.11
      port: 22
      environment: prod
    - name: lpar-prod-02
      host: 10.10.20.12
      port: 22
      environment: prod
```

---

## 📡 Contrato OTLP

Métricas y logs emitidos están documentados en **[`docs/otlp-contract.md`](docs/otlp-contract.md)**. Cualquier cambio breaking requiere bump mayor.

Resumen:

- **12 métricas Gauge** bajo prefijo `aix.*`
- **Logs errpt** con scope `aoa.aix.agent.errpt`
- **Alertas** como métrica `aix.alert` con atributos `alert.*`

---

## 🏗️ Arquitectura

Arquitectura hexagonal (ports & adapters). Ver [`docs/architecture.md`](docs/architecture.md).

```
domain → application (ports) → infrastructure (adapters)
```

Adapters principales:
- **Inbound:** `LparstatScheduler`, `SyslogUdpServer`, `SyslogTcpServer`
- **Outbound:** `SshLparstatExecutor`, `OtlpMetricPublisherAdapter`, `OtlpLogPublisherAdapter`

---

## 🔐 Seguridad

- Autenticación SSH **solo por llave privada**.
- En producción `aix.ssh.skip-host-key-verification: false` (obligatorio `known_hosts`).
- Usuario AIX restringido a comandos `lparstat`, `errpt` vía `sudoers` o restricted shell.
- Sin credenciales en imagen ni en `application.yml` versionado.
- Llaves montadas como `Secret` (Kubernetes) o `docker secret`.

Detalles operativos:
- [`docs/ssh-setup-aix.md`](docs/ssh-setup-aix.md) — alta de usuario y llave en AIX
- [`docs/syslog-setup-aix.md`](docs/syslog-setup-aix.md) — configuración de `/etc/syslog.conf` para errpt

---

## 🩺 Observabilidad del agente

| Endpoint | Descripción |
|---|---|
| `GET /actuator/health` | Estado general |
| `GET /actuator/health/readiness` | Readiness (K8s) |
| `GET /actuator/health/liveness` | Liveness (K8s) |
| `GET /actuator/info` | Versión |

Métricas JVM (`process.runtime.jvm.*`) también se emiten vía OTLP.

---

## 🧪 Tests

```bash
mvn test
```

Cobertura mínima exigida: parser XML, evaluador de alertas, mapping a OTel attributes.

---

## 🛣️ Roadmap

- [ ] Soporte `vmstat` / `iostat`
- [ ] Reglas de alertas dinámicas (recargables sin reinicio)
- [ ] TLS/mTLS hacia el Collector
- [ ] Buffer local persistente ante caída del Collector

---

## 📦 Releases

- `v1.0.0` — Release inicial. Métricas lparstat + logs errpt + alertas paging.

---

## 👥 Equipo

- **Owner:** Observabilidad AOA
- **Slack:** `#aoa-observability`
- **Issues:** vía Jira proyecto `OBS`

                                                                             *
* Este es un equipo de uso Privado. Su ingreso solo es permitido para usuarios*
* autorizados. La utilizacion por usuarios no autorizados esta prohibida.     *
* El uso no autorizado o impropio de este sistema puede causar sanciones      *
* disciplinarias y acciones civiles y penales.                                *
* Accesando a este sistema el usuario esta de acuerdo y acepta estos terminos *
* condiciones. Si usted no es un usuario autorizado o no esta de acuerdo con  *
* las condiciones listadas termine su uso inmediatamente.                     *
*                                                                             *
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *