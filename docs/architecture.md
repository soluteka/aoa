# Arquitectura — AOA AIX Logs Agent

**Versión:** 1.0.0
**Estilo arquitectónico:** Hexagonal (Ports & Adapters) + DDD ligero
**Stack:** Java 21 · Spring Boot 3.3 · OpenTelemetry SDK 1.43

---

## 1. Visión general

El agente es un proceso Java standalone que cumple un único propósito: **recolectar telemetría de hosts AIX y publicarla a un OTel Collector corporativo vía OTLP/gRPC**.

```
┌──────────────┐      SSH (lparstat)      ┌──────────────────┐      OTLP/gRPC      ┌──────────────────┐
│  AIX LPARs   │ ───────────────────────► │  AOA AIX Logs    │ ──────────────────► │  OTel Collector  │
│              │      Syslog (errpt)      │     Agent        │                     │   (corporativo)  │
│              │ ───────────────────────► │                  │                     │                  │
└──────────────┘                          └──────────────────┘                     └──────────────────┘
```

El agente **no persiste estado**, **no expone APIs de negocio**, **no enruta** a múltiples backends. Es un *shipper* especializado.

---

## 2. Principios

1. **Hexagonal:** el dominio no conoce frameworks ni protocolos.
2. **Single Responsibility:** ingesta + transformación + emisión OTLP.
3. **12-Factor App:** configuración por entorno, logs a stdout, sin estado.
4. **Fail-soft:** un LPAR caído no afecta a los demás (thread pool aislado, errores logueados).
5. **Observabilidad propia:** el agente se monitorea a sí mismo (Actuator + JVM metrics).

---

## 3. Capas

```
com.aoa.aix
│
├── domain                            ← Núcleo, sin dependencias externas
│   ├── model
│   │   ├── LparstatSnapshot          (immutable, @Value)
│   │   ├── MemoryAlert
│   │   ├── ErrptEvent
│   │   └── Severity (enum)
│   └── service
│       └── MemoryAlertEvaluator      (reglas de umbrales)
│
├── application                       ← Casos de uso (orquestación)
│   ├── port
│   │   ├── in
│   │   │   ├── IngestLparstatUseCase
│   │   │   └── IngestErrptUseCase
│   │   └── out
│   │       ├── LparstatCommandExecutor
│   │       ├── OtlpMetricPublisher
│   │       └── OtlpLogPublisher
│   └── service
│       ├── LparstatIngestService
│       └── ErrptIngestService
│
└── infrastructure                    ← Adapters (frameworks, I/O)
    ├── inbound
    │   ├── schedule
    │   │   └── LparstatScheduler     (@Scheduled fixedDelay)
    │   └── syslog
    │       ├── SyslogUdpServer       (Netty UDP :5140)
    │       └── SyslogTcpServer       (Netty TCP :5141)
    │
    ├── outbound
    │   ├── ssh
    │   │   └── SshLparstatExecutor   (@Profile("ssh"), SSHJ)
    │   ├── file
    │   │   └── FileLparstatExecutor  (@Profile("!ssh"), tests/dev)
    │   ├── parser
    │   │   ├── LparstatXmlParser     (Jackson XML)
    │   │   └── ErrptLineParser
    │   └── otlp
    │       ├── OtlpMetricPublisherAdapter
    │       └── OtlpLogPublisherAdapter
    │
    ├── config
    │   ├── OpenTelemetryConfig
    │   ├── AixProperties             (@ConfigurationProperties)
    │   ├── SshProperties
    │   └── SyslogConfig
    │
    └── dto
        └── LparstatModel             (XML binding root)
```

### Regla de dependencias

```
infrastructure ──► application ──► domain
              (NUNCA al revés)
```

---

## 4. Flujo de datos

### 4.1 Flujo lparstat (pull)

```
LparstatScheduler
   │ @Scheduled cada N segundos
   ▼
LparstatIngestService.collectAndIngest()
   │
   ├──► LparstatCommandExecutor.executeAll()
   │       │
   │       └──► SshLparstatExecutor
   │              ├─ por cada LPAR en aix.lpars[]
   │              ├─ SSH connect → ejecuta `lparstat -X 1 1`
   │              ├─ LparstatXmlParser.parse(xml)
   │              └─ devuelve List<LparstatSnapshot>
   │
   ├──► OtlpMetricPublisher.publishSnapshot(snap)   (12 métricas Gauge)
   │
   └──► MemoryAlertEvaluator.evaluate(snap)
           │
           └──► OtlpMetricPublisher.publishAlert(alert)  (métrica aix.alert)
```

### 4.2 Flujo errpt (push)

```
AIX  ── syslog ──►  SyslogUdpServer / SyslogTcpServer
                            │
                            ▼
                  ErrptLineParser → ErrptEvent
                            │
                            ▼
                  ErrptIngestService.ingest(event)
                            │
                            ▼
                  OtlpLogPublisherAdapter.publish(event)  → OTLP Logs
```

---

## 5. Concurrencia

| Componente | Modelo |
|---|---|
| `LparstatScheduler` | Hilo único (`scheduling-1`), dispara el ciclo |
| `SshLparstatExecutor` | Pool fijo `ssh-lparstat-N` (N = nº LPARs, cap 10) |
| `SyslogUdpServer` | Netty event loop |
| `SyslogTcpServer` | Netty event loop |
| OTLP exporter | Batch interno del SDK (no bloquea hilos del agente) |

Aislamiento: una LPAR lenta no bloquea a otras (timeouts SSH configurables).

---

## 6. Resiliencia

| Escenario | Comportamiento |
|---|---|
| LPAR no responde | Timeout → log WARN → siguiente ciclo |
| Collector caído | SDK OTel reintenta con backoff, descarta tras buffer lleno |
| XML malformado | Parser devuelve `null` → snapshot omitido |
| Syslog inválido | Parser devuelve `Optional.empty()` → línea descartada |
| OOM | `-XX:+ExitOnOutOfMemoryError` → K8s reinicia |

---

## 7. Configuración (perfiles Spring)

| Perfil | Uso |
|---|---|
| `dev` | Desarrollo local, logs DEBUG, intervalos cortos |
| `ssh` | Activa `SshLparstatExecutor` (excluye `FileLparstatExecutor`) |
| `prod` | Producción: validación estricta de `known_hosts`, logs JSON |

Combinaciones típicas:
- Local con archivos de muestra: `dev`
- Local contra `aix-mock` SSH: `dev,ssh`
- Producción: `prod,ssh`

---

## 8. Contrato externo

- **Hacia Collector:** ver [`otlp-contract.md`](otlp-contract.md)
- **Hacia AIX (SSH):** ver [`ssh-setup-aix.md`](ssh-setup-aix.md)
- **Hacia AIX (syslog):** ver [`syslog-setup-aix.md`](syslog-setup-aix.md)

---

## 9. Decisiones de arquitectura (ADR resumido)

| # | Decisión | Justificación |
|---|---|---|
| 1 | Hexagonal | Aislar dominio de OTel/SSH/Spring; facilita testing y reemplazo de adapters |
| 2 | Pull SSH para lparstat | No requiere instalación de agentes en AIX (solo usuario + llave) |
| 3 | Push syslog para errpt | AIX ya genera errpt como syslog; cero códig