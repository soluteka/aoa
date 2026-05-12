# SSH Setup en AIX — AOA AIX Logs Agent

**Audiencia:** equipo de infraestructura AIX
**Objetivo:** crear un usuario lectura-solo en cada LPAR para que el agente ejecute `lparstat` vía SSH con autenticación por llave pública.

---

## 1. Prerrequisitos

- AIX 7.1 o superior con OpenSSH instalado y habilitado
- Acceso `root` o equivalente para crear usuario
- Llave pública del agente (provista por el equipo de Observabilidad)

---

## 2. Crear usuario de servicio

```bash
# Como root en cada LPAR
mkuser \
  pgrp=staff \
  home=/home/aoa_reader \
  shell=/usr/bin/ksh \
  gecos="AOA Observability Read-Only" \
  aoa_reader

# Bloquear login por contraseña (solo llave)
chuser account_locked=false login=true rlogin=true aoa_reader
passwd -f aoa_reader   # forzar expiración para que no se pueda loguear por pwd
```

---

## 3. Instalar llave pública

```bash
su - aoa_reader
mkdir -p ~/.ssh && chmod 700 ~/.ssh
cat >> ~/.ssh/authorized_keys <<'EOF'
ssh-ed25519 AAAAC3Nz...<llave_publica_entregada_por_observabilidad>... aoa-agent@corp
EOF
chmod 600 ~/.ssh/authorized_keys
```

> ⚠️ Solicitar la llave pública al equipo de Observabilidad. Nunca compartir la llave privada.

---

## 4. Restringir el shell (recomendado)

Limitar al usuario a ejecutar **solo** `lparstat` y `errpt`. Editar `~/.ssh/authorized_keys` anteponiendo:

```
command="/usr/local/bin/aoa-restricted.sh",no-port-forwarding,no-X11-forwarding,no-agent-forwarding,no-pty ssh-ed25519 AAAAC3Nz...
```

Crear el wrapper `/usr/local/bin/aoa-restricted.sh`:

```bash
#!/usr/bin/ksh
case "$SSH_ORIGINAL_COMMAND" in
  "lparstat -X 1 1") exec lparstat -X 1 1 ;;
  "errpt -a")         exec errpt -a ;;
  ) echo "Command not allowed"; exit 1 ;;
esac
```

```bash
chmod 755 /usr/local/bin/aoa-restricted.sh
chown root:system /usr/local/bin/aoa-restricted.sh
```

---

## 5. Validar conectividad

Desde un host con la llave privada del agente:

```bash
ssh -i /path/to/id_ed25519 aoa_reader@<lpar-ip> "lparstat -X 1 1"
```

Debe devolver XML válido. Si falla, revisar:

- `/var/adm/messages` en AIX
- `/etc/ssh/sshd_config`: `PubkeyAuthentication yes`, `PasswordAuthentication no`
- Permisos: `~/.ssh` (700), `authorized_keys` (600), home (755)

---

## 6. Generar `known_hosts` para el agente

Desde el servidor donde correrá el agente:

```bash
ssh-keyscan -t ed25519,rsa -p 22 \
  10.10.20.11 10.10.20.12 10.10.20.13 \
  >> /etc/aoa/keys/known_hosts
chmod 644 /etc/aoa/keys/known_hosts
```

Este archivo se monta como `Secret` o `ConfigMap` en el contenedor del agente.

---

## 7. Rotación de llaves

| Acción | Frecuencia |
|---|---|
| Rotar par de llaves | cada 12 meses |
| Auditar `authorized_keys` | trimestral |
| Revisar `lastlog aoa_reader` | mensual |

Procedimiento de rotación:

1. Observabilidad genera nuevo par.
2. Se agrega la nueva pública a `authorized_keys` (sin borrar la vieja).
3. Se actualiza el `Secret` del agente y se redespliega.
4. Tras validar, se elimina la llave vieja de `authorized_keys`.

---

## 8. Firewall

Permitir tráfico SSH desde la subred del agente hacia cada LPAR:

```
ALLOW TCP 22 FROM <subnet-observability> TO <lpars-aix>
```

---

## 9. Checklist de entrega ✅

- [ ] Usuario `aoa_reader` creado en todas las LPARs objetivo
- [ ] Llave pública instalada en `~/.ssh/authorized_keys`
- [ ] Restricted shell activo (opcional pero recomendado)
- [ ] `known_hosts` generado y entregado a Observabilidad
- [ ] Firewall abierto desde subred del agente
- [ ] Validación `ssh ... "lparstat -X 1 1"` OK desde un host de prueba