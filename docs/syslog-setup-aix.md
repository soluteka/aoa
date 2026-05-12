# Syslog Setup en AIX (errpt) — AOA AIX Logs Agent

**Audiencia:** equipo de infraestructura AIX
**Objetivo:** configurar AIX para enviar eventos `errpt` vía syslog al agente (`UDP 5140` o `TCP 5141`).

---

## 1. Arquitectura

```
errpt  ──►  errnotify (ODM)  ──►  logger ──►  syslogd  ──►  agente (UDP/TCP)
```

AIX **no envía errpt como syslog por defecto**. Hay que configurar un *notify object* en ODM que reaccione a cada entrada nueva del errlog y la inyecte en syslog vía `logger`.

---

## 2. Configurar `syslogd` para enviar a remoto

Editar `/etc/syslog.conf` y agregar al final:

```
# AOA AIX Logs Agent
local0.info     @<ip-agente>          # UDP
# o para TCP (AIX 7.2 TL3+):
# local0.info   @@<ip-agente>:5141
```

Aplicar cambios:

```bash
refresh -s syslogd
```

> El agente escucha por defecto UDP 5140 y TCP 5141. Si se usa el puerto por defecto de syslog (514), ajustar `aix.syslog.udp-port`/`tcp-port` en `application.yml`.

> ⚠️ Para puertos no estándar (5140/5141), AIX requiere especificarlos con `@host:puerto` (UDP) o `@@host:puerto` (TCP). Si la versión de AIX no lo soporta, usar `rsyslog` como sidecar o redirigir 514→5140 vía iptables/firewall del host.

---

## 3. Crear notify object para errpt → syslog

Crear archivo `/tmp/aoa_errnotify.add`:

```
errnotify:
  en_pid = 0
  en_name = "aoa_observability"
  en_persistenceflg = 1
  en_method = "/usr/bin/logger -p local0.info -t errpt \"$(errpt -a -l \$1 | tr '\n' ' ')\""
```

Registrar en ODM:

```bash
odmadd /tmp/aoa_errnotify.add
```

Validar:

```bash
odmget -q "en_name='aoa_observability'" errnotify
```

---

## 4. Probar el flujo

Generar un evento errpt de prueba:

```bash
errlogger "AOA test event from $(hostname)"
```

Verificar en el host del agente:

```bash
# UDP
sudo tcpdump -i any -A udp port 5140

# TCP
sudo tcpdump -i any -A tcp port 5141
```

Debe verse el mensaje con el label `errpt` y el cuerpo del evento.

---

## 5. Formato esperado por el agente

El parser del agente espera la línea con este patrón mínimo:

```
<134>May 12 10:23:45 lpar-prod-01 errpt: IDENTIFIER TIMESTAMP T C RESOURCE_NAME DESCRIPTION
```

Campos extraídos:

| Campo errpt | Atributo OTLP |
|---|---|
| `IDENTIFIER` | `errpt.id` |
| `TIMESTAMP` | `errpt.timestamp` |
| `T` (TYPE) | `errpt.type` |
| `C` (CLASS) | `errpt.class` |
| `RESOURCE_NAME` | `errpt.resource` |
| `DESCRIPTION` | `errpt.label` |
| `hostname` (syslog header) | `host.name` |

---

## 6. Filtrado (opcional)

Para evitar ruido, filtrar tipos `INFO` en AIX modificando el método del notify:

```
en_method = "T=$(errpt -l \$1 | awk 'NR==2{print \$3}'); [ \"$T\" != \"INFO\" ] && /usr/bin/logger -p local0.info -t errpt \"$(errpt -a -l \$1 | tr '\n' ' ')\""
```

---

## 7. Firewall

Permitir tráfico desde las LPARs hacia el agente:

```
ALLOW UDP 5140 FROM <lpars-aix> TO <agente>
ALLOW TCP 5141 FROM <lpars-aix> TO <agente>
```

---

## 8. Revertir / desinstalar

```bash
# Quitar notify object
odmdelete -o errnotify -q "en_name='aoa_observability'"

# Quitar línea de /etc/syslog.conf y refrescar
refresh -s syslogd
```

---

## 9. Checklist de entrega ✅

- [ ] `local0.info` redirigido al agente en `/etc/syslog.conf`
- [ ] `syslogd` refrescado
- [ ] `errnotify` `aoa_observability` registrado en ODM
- [ ] Prueba con `errlogger` recibida en el agente
- [ ] Firewall abierto desde LPARs hacia agente (5140/UDP, 5141/TCP)
- [ ] Documentado el procedimiento en el runbook corporativo