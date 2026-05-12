package com.aoa.aix.application.port.out;

/**
 * Excepción específica para fallas al ejecutar lparstat remotamente.
 *
 * Separar este tipo de error permite que el caso de uso decida la política:
 *   - Loguear como warn y continuar con la siguiente LPAR.
 *   - Emitir una métrica/log OTLP de "host unreachable".
 *   - No tumbar el ciclo de muestreo completo por un fallo puntual.
 */
public class LparstatExecutionException extends Exception {

    public LparstatExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public LparstatExecutionException(String message) {
        super(message);
    }
}