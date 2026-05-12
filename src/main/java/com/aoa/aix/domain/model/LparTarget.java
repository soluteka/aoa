package com.aoa.aix.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa una LPAR AIX objetivo del agente de monitoreo.
 *
 * Es un objeto de dominio puro: no depende de Spring ni de ninguna
 * tecnología de infraestructura. Es inmutable en intención y se usa
 * como contexto para:
 *   - Ejecutar lparstat -X vía SSH.
 *   - Resolver el origen de mensajes syslog entrantes.
 *   - Enriquecer las señales OTLP (logs y métricas) con atributos
 *     que permiten al backend distinguir cuál LPAR generó la señal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LparTarget {

    /** Nombre lógico de la LPAR. Se usa como host.name en OTLP. */
    private String name;

    /** IP o hostname de red para conexión SSH y resolución syslog. */
    private String host;

    /** Usuario de la cuenta de servicio del agente en la LPAR. */
    private String sshUser;

    /**
     * Ruta absoluta a la llave privada SSH para autenticación.
     * En modelo de credenciales común, todas las LPARs apuntan a la misma ruta.
     * En modelo segregado, cada LPAR apunta a una ruta distinta.
     */
    private String sshKey;

    /** Ambiente: prod, dev, qa. Mapea a deployment.environment en OTLP. */
    private String environment;

    /** Frame del Power donde reside la LPAR. Contexto del hipervisor. */
    private String frame;
}