#!/bin/bash
# Mock de lparstat -X para pruebas
# Acepta cualquier argumento (incluyendo -X) y devuelve XML válido

# Generar valores ligeramente variables para que cada ejecución sea distinta
USER_PCT=$(awk -v min=10 -v max=40 'BEGIN{srand(); print min+rand()*(max-min)}')
SYS_PCT=$(awk -v min=5  -v max=20 'BEGIN{srand()+1; print min+rand()*(max-min)}')
IDLE_PCT=$(awk -v u="$USER_PCT" -v s="$SYS_PCT" 'BEGIN{print 100-u-s}')
PGSP_PCT=$(awk -v min=20 -v max=85 'BEGIN{srand()+2; print min+rand()*(max-min)}')
PAGING=$(awk -v min=0 -v max=200 'BEGIN{srand()+3; print min+rand()*(max-min)}')

cat <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<lparstat>
  <system_configuration>
    <hostname>aix-mock-01</hostname>
    <partition_name>lpar_mock</partition_name>
    <partition_number>1</partition_number>
    <type>Shared-SMT-8</type>
    <mode>Uncapped</mode>
    <entitled_capacity>2.00</entitled_capacity>
    <online_virtual_cpus>4</online_virtual_cpus>
    <online_memory>16384</online_memory>
  </system_configuration>
  <utilization>
    <user_pct>${USER_PCT}</user_pct>
    <sys_pct>${SYS_PCT}</sys_pct>
    <wait_pct>0.5</wait_pct>
    <idle_pct>${IDLE_PCT}</idle_pct>
    <physc>0.45</physc>
    <entc_pct>22.5</entc_pct>
    <lbusy>15.2</lbusy>
    <app>3.55</app>
    <vcsw>1250</vcsw>
    <phint>5</phint>
  </utilization>
  <memory>
    <real_mb>16384</real_mb>
    <paging_space_mb>8192</paging_space_mb>
    <paging_space_used_pct>${PGSP_PCT}</paging_space_used_pct>
    <paging_rate>${PAGING}</paging_rate>
  </memory>
</lparstat>
EOF