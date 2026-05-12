# OTLP Contract — AOA AIX Logs Agent

**Versión:** 1.0.0
**Componente emisor:** `aoa-aix-logs-agent`
**Protocolo:** OTLP / gRPC
**Endpoint (configurable):** `OTEL_EXPORTER_OTLP_ENDPOINT` (default `http://otel-collector:4317`)
**Señales emitidas:** Metrics, Logs

Este documento es el **contrato formal** entre el agente AIX y la plataforma corporativa de observabilidad. Cualquier cambio en métricas, atributos o naming debe versionarse aquí y comunicarse al equipo de plataforma.

---

## 1. Resource Attributes

Atributos enviados a nivel de `Resource` (aplican a todas las señales del proceso).

| Atributo | Tipo | Ejemplo | Origen | Obligatorio |
|---|---|---|---|---|
| `service.name` | string | `aoa-aix-logs-agent` | fijo | ✅ |
| `service.version` | string | `1.0.0` | Maven `@project.version@` | ✅ |
| `service.instance.id` | string | UUID | SDK | ✅ |
| `deployment.environment` | string | `prod` \| `qa` \| `dev` | env `DEPLOYMENT_ENV` | ✅ |
| `aix.agent.version` | string | `1.0.0` | Maven | ✅ |
| `telemetry.sdk.name` | string | `opentelemetry` | SDK | auto |
| `telemetry.sdk.language` | string | `java` | SDK | auto |
| `telemetry.sdk.version` | string | `1.43.0` | SDK | auto |

---

## 2. Métricas

**Instrumentation scope:** `aoa.aix.agent`
**Tipo:** todas son `Gauge` (`DoubleGauge`).
**Frecuencia de emisión:** según `aix.scheduler.interval-seconds` (default 10s).

### 2.1 CPU

| Métrica | Unidad | Descripción | Atributos |
|---|---|---|---|
| `aix.cpu.user` | `%` | Porcentaje CPU en modo usuario | `host.name`, `lpar.name` |
| `aix.cpu.sys` | `%` | Porcentaje CPU en modo sistema | `host.name`, `lpar.name` |
| `aix.cpu.idle` | `%` | Porcentaje CPU ociosa | `host.name`, `lpar.name` |
| `aix.cpu.wait` | `%` | Porcentaje CPU en wait I/O | `host.name`, `lpar.name` |

### 2.2 LPAR

| Métrica | Unidad | Descripción | Atributos |
|---|---|---|---|
| `aix.lpar.physc` | `{cores}` | CPUs físicas consumidas | `host.name`, `lpar.name` |
| `aix.lpar.entc_pct` | `%` | % de capacidad entitled usada | `host.name`, `lpar.name` |
| `aix.lpar.lbusy` | `%` | Logical CPU busy | `host.name`, `lpar.name` |
| `aix.lpar.app` | `{cores}` | Available pool processors | `host.name`, `lpar.name` |
| `aix.lpar.entitled_capacity` | `{cores}` | Entitled capacity configurada | `host.name`, `lpar.name` |

### 2.3 Memoria / Paging

| Métrica | Unidad | Descripción | Atributos |
|---|---|---|---|
| `aix.memory.paging.used_pct` | `%` | Paging space usado | `host.name`, `lpar.name` |
| `aix.memory.paging.rate` | `{pages}/s` | Tasa de paginación | `host.name`, `lpar.name` |

### 2.4 Alertas

| Métrica | Unidad | Descripción | Atributos |
|---|---|---|---|
| `aix.alert` | `1` | Alerta emitida tras evaluar umbrales. El `value` es el valor que disparó la alerta. | `host.name`, `alert.metric`, `alert.reason`, `alert.severity`, `alert.threshold` |

**Valores de `alert.severity`:** `INFO`, `WARN`, `CRITICAL`
**Valores de `alert.reason` (catálogo inicial):**
- `PAGING_SPACE_HIGH`
- `PAGING_RATE_CRITICAL`
- `CPU_USER_HIGH` *(futuro)*
- `ENTC_PCT_HIGH` *(futuro)*

---

## 3. Logs (errpt)

**Instrumentation scope:** `aoa.aix.agent.errpt`
**Severity mapping:**

| errpt TYPE | OTel SeverityNumber | SeverityText |
|---|---|---|
| `INFO` | 9 | `INFO` |
| `PEND` | 13 | `WARN` |
| `PERF` | 13 | `WARN` |
| `PERM` | 17 | `ERROR` |
| `TEMP` | 13 | `WARN` |
| `UNKN` | 13 | `WARN` |

**Body:** texto plano de la línea `errpt` original.

**Atributos del log record:**

| Atributo | Tipo | Descripción |
|---|---|---|
| `host.name` | string | LPAR/host de origen |
| `lpar.name` | string | Nombre lógico de la LPAR |
| `errpt.id` | string | Identifier (col 1) |
| `errpt.timestamp` | string | Timestamp original AIX |
| `errpt.type` | string | TYPE (INFO/PERM/...) |
| `errpt.class` | string | CLASS (H/S/O/U) |
| `errpt.resource` | string | RESOURCE_NAME |
| `errpt.label` | string | Etiqueta descriptiva |

---

## 4. Cardinalidad esperada

| Dimensión | Estimado |
|---|---|
| LPARs por agente | ≤ 50 |
| Time series por LPAR | 11 métricas + N alertas |
| Cardinalidad total por agente | ≈ 600 series |
| Eventos errpt por LPAR/día | 10 – 500 (pico) |

---

## 5. Versionado del contrato

| Versión | Fecha | Cambios |
|---|---|---|
| 1.0.0 | 2026-05-11 | Versión inicial — 11 métricas + alertas + logs errpt |

**Política:** cambios breaking (rename/eliminación de métrica o atributo) requieren bump mayor y notificación al equipo de plataforma con 2 sprints de anticipación.

---

## 6. Contacto

- **Owner:** Equipo Observabilidad AOA
- **Repo:** `<registry-corp>/observability/aoa-aix-logs-agent`
- **Slack:** `#aoa-observability`