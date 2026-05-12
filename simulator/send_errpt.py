#!/usr/bin/env python3
"""Simula eventos errpt de AIX hacia el agente vía syslog UDP."""
import socket, time, random, sys

HOST = sys.argv[1] if len(sys.argv) > 1 else "127.0.0.1"
PORT = int(sys.argv[2]) if len(sys.argv) > 2 else 5140

# (PRI = facility*8 + severity).  user.err = 1*8 + 3 = 11
SAMPLES = [
    # Hardware PERM → debe mapear a FATAL
    "<11>Nov 26 10:15:22 aix-lpar01 errpt: LABEL: DISK_ERR4 "
    "IDENTIFIER: AA8AB241 CLASS: H TYPE: PERM RESOURCE: hdisk0 "
    "DESCRIPTION: DISK OPERATION ERROR",

    # Hardware TEMP → ERROR
    "<12>Nov 26 10:15:23 aix-lpar01 errpt: LABEL: SCSI_ERR1 "
    "IDENTIFIER: B2C3D4E5 CLASS: H TYPE: TEMP RESOURCE: scsi0 "
    "DESCRIPTION: TEMPORARY SCSI BUS ERROR",

    # Software PERM → ERROR
    "<11>Nov 26 10:15:24 aix-lpar01 errpt: LABEL: SYSPROC "
    "IDENTIFIER: 11223344 CLASS: S TYPE: PERM RESOURCE: SYSPROC "
    "DESCRIPTION: SOFTWARE PROGRAM ABNORMALLY TERMINATED",

    # Operator INFO → INFO
    "<14>Nov 26 10:15:25 aix-lpar01 errpt: LABEL: OPMSG "
    "IDENTIFIER: AA8AB241 CLASS: O TYPE: INFO RESOURCE: OPERATOR "
    "DESCRIPTION: OPERATOR NOTIFICATION",

    # PERF → WARN
    "<12>Nov 26 10:15:26 aix-lpar01 errpt: LABEL: CPU_HIGH "
    "IDENTIFIER: 99887766 CLASS: S TYPE: PERF RESOURCE: cpu0 "
    "DESCRIPTION: CPU UTILIZATION EXCEEDED THRESHOLD",
]

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
print(f"Enviando eventos errpt simulados a {HOST}:{PORT} (Ctrl+C para detener)")
try:
    while True:
        msg = random.choice(SAMPLES)
        sock.sendto(msg.encode("utf-8"), (HOST, PORT))
        print(f"→ {msg[:90]}…")
        time.sleep(2)
except KeyboardInterrupt:
    print("\nDetenido.")