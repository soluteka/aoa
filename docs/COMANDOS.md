# 📘 Compendio de comandos — `aoa_logs_agent`

Referencia consolidada de todos los comandos usados durante la implementación del agente AOA AIX Logs Agent.
Cada comando incluye su propósito y equivalente en **PowerShell/Windows** y **Bash/Linux**.

---

## 📑 Índice

- [🐳 Docker](#-docker)
- [☕ Maven](#-maven)
- [🔀 Git — básico](#-git--básico)
- [⚙️ Git — configuración](#️-git--configuración)
- [🌿 Git — ramas y bundles](#-git--ramas-y-bundles)
- [📁 Sistema de archivos](#-sistema-de-archivos)
- [🗜️ Compresión y copia](#️-compresión-y-copia)
- [🔍 Búsqueda de texto](#-búsqueda-de-texto)
- [🌐 Red y conectividad](#-red-y-conectividad)
- [☕ Java y certificados](#-java-y-certificados)
- [📦 Instalación y verificación de herramientas](#-instalación-y-verificación-de-herramientas)
- [☸️ Kubernetes](#️-kubernetes)
- [❤️ Health checks HTTP](#️-health-checks-http)

---

## 🐳 Docker

| Propósito | PowerShell / Windows | Bash / Linux |
|---|---|---|
| Construir imagen sin caché | `docker compose build --no-cache aoa-logs-agent` | `docker compose build --no-cache aoa-logs-agent` |
| Levantar reconstruyendo imágenes | `docker compose up --build` | `docker compose up --build` |
| Levantar en segundo plano | `docker compose up -d` | `docker compose up -d` |
| Detener y limpiar (incluye volúmenes) | `docker compose down -v` | `docker compose down -v` |
| Ver logs en vivo | `docker compose logs -f aoa-logs-agent` | `docker compose logs -f aoa-logs-agent` |
| Listar contenedores activos | `docker ps` | `docker ps` |
| Abrir shell dentro del contenedor | `docker exec -it aoa-logs-agent sh` | `docker exec -it aoa-logs-agent sh` |

---

## ☕ Maven

| Propósito | PowerShell / Windows | Bash / Linux |
|---|---|---|
| Compilar | `.\mvnw clean compile` | `./mvnw clean compile` |
| Ejecutar tests unitarios | `.\mvnw test` | `./mvnw test` |
| Empaquetar JAR (sin tests) | `.\mvnw package -DskipTests` | `./mvnw package -DskipTests` |
| Ejecutar con perfil específico | `.\mvnw spring-boot:run '-Dspring-boot.run.profiles=dev,ssh'` | `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,ssh` |
| Obtener versión del proyecto | `.\mvnw -q -DforceStdout help:evaluate -Dexpression=project.version` | `./mvnw -q -DforceStdout help:evaluate -Dexpression=project.version` |

---

## 🔀 Git — básico

| Propósito | PowerShell / Windows | Bash / Linux |
|---|---|---|
| Inicializar repo local | `git init` | `git init` |
| Ver estado | `git status` | `git status` |
| Agregar todos los cambios | `git add .` | `git add .` |
| Crear commit | `git commit -m "feat: descripción"` | `git commit -m "feat: descripción"` |
| Renombrar rama actual a `main` | `git branch -M main` | `git branch -M main` |
| Agregar remoto `origin` | `git remote add origin https://github.com/desarrolloaoa/aoa.git` | `git remote add origin https://github.com/desarrolloaoa/aoa.git` |
| Cambiar URL del remoto | `git remote set-url origin https://github.com/desarrolloaoa/aoa.git` | `git remote set-url origin https://github.com/desarrolloaoa/aoa.git` |
| Listar remotos | `git remote -v` | `git remote -v` |
| Primer push vinculando rama | `git push -u origin main` | `git push -u origin main` |
| Forzar push (tras amend) | `git push -u origin main --force` | `git push -u origin main --force` |
| Bajar cambios | `git pull` | `git pull` |
| Clonar repo | `git clone https://github.com/desarrolloaoa/aoa.git` | `git clone https://github.com/desarrolloaoa/aoa.git` |
| Historial resumido | `git log --oneline -5` | `git log --oneline -5` |
| Autor/email del último commit | `git log -1 --format="%an <%ae>"` | `git log -1 --format='%an <%ae>'` |

---

## ⚙️ Git — configuración

| Propósito | PowerShell / Windows | Bash / Linux |
|---|---|---|
| Configurar nombre global | `git config --global user.name "desarrolloaoa"` | `git config --global user.name 'desarrolloaoa'` |
| Configurar email noreply | `git config --global user.email "ID+usuario@users.noreply.github.com"` | `git config --global user.email 'ID+usuario@users.noreply.github.com'` |
| Reescribir último commit con nuevo autor | `git commit --amend --reset-author --no-edit` | `git commit --amend --reset-author --no-edit` |
| Pull con rebase por defecto | `git config --global pull.rebase true` | `git config --global pull.rebase true` |
| Proxy HTTP corporativo | `git config --global http.proxy http://proxy.corp:8080` | `git config --global http.proxy http://proxy.corp:8080` |

---

## 🌿 Git — ramas y bundles

| Propósito | PowerShell / Windows | Bash / Linux |
|---|---|---|
| Crear y cambiar a rama nueva | `git checkout -b qa-corporativo` | `git checkout -b qa-corporativo` |
| Crear bundle portable del repo | `git bundle create aoa-agent.bundle --all` | `git bundle create aoa-agent.bundle --all` |
| Resetear local al estado del remoto | `git fetch origin; git reset --hard origin/main` | `git fetch origin && git reset --hard origin/main` |

---

## 📁 Sistema de archivos

| Propósito | PowerShell / Windows | Bash / Linux |
|---|---|---|
| Cambiar de directorio | `cd C:\Desarrollo\Observa\aoa_logs` | `cd /home/user/desarrollo/aoa_logs` |
| Subir un nivel | `cd ..` | `cd ..` |
| Listar contenido | `Get-ChildItem` (alias `ls`, `dir`) | `ls -la` |
| Crear directorio con padres | `New-Item -ItemType Directory -Path ".github\workflows" -Force` | `mkdir -p .github/workflows` |
| Crear archivo vacío | `New-Item -ItemType File -Path ".github\workflows\ci.yml" -Force` | `touch .github/workflows/ci.yml` |
| Eliminar carpeta recursivamente | `Remove-Item .\aoa -Recurse -Force` | `rm -rf ./aoa` |
| Verificar existencia de ruta | `Test-Path C:\ruta\archivo` | `test -e /ruta/archivo && echo OK` |
| Limpiar pantalla | `Clear-Host` (alias `cls`) | `clear` |

---

## 🗜️ Compresión y copia

| Propósito | PowerShell / Windows | Bash / Linux |
|---|---|---|
| Comprimir carpeta a ZIP | `Compress-Archive -Path .\aoa_logs_agent -DestinationPath aoa.zip -Force` | `zip -r aoa.zip aoa_logs_agent` |
| Copiar con exclusiones | `robocopy origen destino /E /XD target .git /XF *.log` | `rsync -av --exclude='target' --exclude='.git' --exclude='*.log' origen/ destino/` |
| Listar contenido de un ZIP | `[System.IO.Compression.ZipFile]::OpenRead('archivo.zip').Entries` | `unzip -l archivo.zip` |
| Descomprimir ZIP | `Expand-Archive -Path archivo.zip -DestinationPath .\destino` | `unzip archivo.zip -d ./destino` |

---

## 🔍 Búsqueda de texto

| Propósito | PowerShell / Windows | Bash / Linux |
|---|---|---|
| Buscar texto en archivos | `Select-String -Path "*.yml" -Pattern "password"` | `grep -r 'password' --include='*.yml' .` |
| Buscar secretos potenciales | `Select-String -Path *.yml,*.properties -Pattern "BEGIN.*PRIVATE KEY\|password:\|token:"` | `grep -rE 'BEGIN.*PRIVATE KEY\|password:\|token:' .` |

---

## 🌐 Red y conectividad

| Propósito | PowerShell / Windows | Bash / Linux |
|---|---|---|
| Probar puerto TCP | `Test-NetConnection <host> -Port 4317` | `nc -zv <host> 4317` |
| Probar SSH a LPAR AIX | `ssh -i .\keys\id_rsa aoa_reader@<ip> "lparstat -X 1 1"` | `ssh -i ./keys/id_rsa aoa_reader@<ip> 'lparstat -X 1 1'` |
| Ver proxy WinHTTP | `netsh winhttp show proxy` | `echo $http_proxy; echo $https_proxy` |
| Ver variables de proxy | `$env:HTTP_PROXY; $env:HTTPS_PROXY` | `env \| grep -i proxy` |

---

## ☕ Java y certificados

| Propósito | PowerShell / Windows | Bash / Linux |
|---|---|---|
| Verificar versión de Java | `java -version` | `java -version` |
| Importar certificado corporativo al truststore | `keytool -import -trustcacerts -keystore "$env:JAVA_HOME\lib\security\cacerts" -storepass changeit -alias corp-ca -file corp-ca.crt` | `keytool -import -trustcacerts -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -alias corp-ca -file corp-ca.crt` |

---

## 📦 Instalación y verificación de herramientas

| Propósito | PowerShell / Windows | Bash / Linux |
|---|---|---|
| Instalar Git | `winget install --id Git.Git -e` | `sudo apt install git` / `sudo yum install git` |
| Verificar Git | `git --version` | `git --version` |
| Verificar Docker | `docker version` | `docker version` |
| Verificar Maven | `mvn -v` | `mvn -v` |

---

## ☸️ Kubernetes

| Propósito | PowerShell / Windows | Bash / Linux |
|---|---|---|
| Crear namespace | `kubectl apply -f k8s/namespace.yaml` | `kubectl apply -f k8s/namespace.yaml` |
| Aplicar manifests con Kustomize | `kubectl apply -k k8s/` | `kubectl apply -k k8s/` |
| Crear Secret SSH | `kubectl -n observability create secret generic aoa-aix-logs-agent-ssh --from-file=id_rsa=.\id_rsa --from-literal=ssh-user=aoa_reader` | `kubectl -n observability create secret generic aoa-aix-logs-agent-ssh --from-file=id_rsa=./id_rsa --from-literal=ssh-user=aoa_reader` |
| Listar pods por etiqueta | `kubectl -n observability get pods -l app.kubernetes.io/name=aoa-aix-logs-agent` | `kubectl -n observability get pods -l app.kubernetes.io/name=aoa-aix-logs-agent` |
| Ver logs en vivo | `kubectl -n observability logs -l app.kubernetes.io/name=aoa-aix-logs-agent -f` | `kubectl -n observability logs -l app.kubernetes.io/name=aoa-aix-logs-agent -f` |
| Port-forward del Service | `kubectl -n observability port-forward svc/aoa-aix-logs-agent 8081:8081` | `kubectl -n observability port-forward svc/aoa-aix-logs-agent 8081:8081` |

---

## ❤️ Health checks HTTP

| Propósito | PowerShell / Windows | Bash / Linux |
|---|---|---|
| Probar endpoint `/actuator/health` | `Invoke-WebRequest http://localhost:8081/actuator/health` | `curl http://localhost:8081/actuator/health` |
| Probar con formato JSON pretty | `Invoke-RestMethod http://localhost:8081/actuator/health \| ConvertTo-Json` | `curl -s http://localhost:8081/actuator/health \| jq` |

---

## 📌 Tips rápidos

- En **PowerShell**, los argumentos con `=` que contienen comas se deben **envolver en comillas simples**, p.ej. `'-Dspring-boot.run.profiles=dev,ssh'`.
- Para tareas con exclusiones complejas en Windows, **`robocopy`** es más confiable que `Compress-Archive` en pipeline.
- Si **GitHub bloquea el push** por email privado (`GH007`), usa el email `ID+usuario@users.noreply.github.com` y reescribe el commit con `git commit --amend --reset-author --no-edit`.
- En entornos corporativos siempre verifica primero: **proxy**, **certificado SSL corp** y **DNS** antes de pelear con la herramienta.

---

_Última actualización: 2026-05-12 — Compendio generado para el proyecto **aoa_logs_agent**._
