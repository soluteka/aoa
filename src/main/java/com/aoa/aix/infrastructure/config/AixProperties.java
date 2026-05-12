package com.aoa.aix.infrastructure.config;

import com.aoa.aix.domain.model.LparTarget;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Propiedades tipadas del agente AOA-AIX.
 *
 * Lee del application.yml la lista de LPARs objetivo y los parámetros
 * operativos (intervalos de muestreo, configuración SSH, comando lparstat).
 *
 * Esta clase es el ÚNICO punto donde Spring se acopla a la configuración:
 * el resto de la aplicación recibe LparTarget como objeto de dominio puro.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aix")
public class AixProperties {

    /** Lista de LPARs AIX que el agente debe monitorear. */
    private List<LparTarget> lpars = new ArrayList<>();

    /** Configuración de conexión SSH (compartida entre todas las LPARs). */
    private Ssh ssh = new Ssh();

    /** Configuración del recolector lparstat. */
    private Lparstat lparstat = new Lparstat();

    /** Configuración del servicio de alertas predictivas de memoria. */
    private Memory memory = new Memory();

    // -------------------------------------------------------------
    // Subgrupos de propiedades
    // -------------------------------------------------------------

    @Data
    public static class Ssh {
        /** Puerto SSH estándar en las LPARs (típicamente 22). */
        private int port = 22;

        /** Timeout de conexión en milisegundos. */
        private int connectTimeoutMs = 5000;

        /** Timeout de lectura del comando remoto en milisegundos. */
        private int readTimeoutMs = 10000;

        /** Comando a ejecutar para obtener métricas en formato XML. */
        private String command = "lparstat -X";

        /**
         * Si es true, omite la verificación de host keys (NO recomendado en prod).
         * En producción debe ser false y usar known_hosts.
         */
        private boolean skipHostKeyVerification = false;

        /** Ruta al archivo known_hosts cuando skipHostKeyVerification = false. */
        private String knownHostsPath;
    }

    @Data
    public static class Lparstat {
        /** Intervalo de muestreo en milisegundos (por defecto 30s). */
        private long intervalMs = 30000;
    }

    @Data
    public static class Memory {
        /** Intervalo de evaluación de alertas de memoria (por defecto 60s). */
        private long intervalMs = 60000;
    }
	
	@Data
	public static class Sampler {	
		private long intervalMs = 30000;
		private int maxParallel = 8;
	}

	private Sampler sampler = new Sampler();
}