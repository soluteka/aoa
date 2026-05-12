package com.aoa.aix.application.port.out;

import com.aoa.aix.domain.model.LparstatSnapshot;
import java.util.List;

/**
 * Puerto de salida para obtener snapshots de lparstat.
 *
 * Devuelve una LISTA porque puede haber N LPARs configuradas
 * (caso SSH multi-host). Implementaciones single-host devuelven
 * una lista de 1 elemento.
 */
public interface LparstatCommandExecutor {
    List<LparstatSnapshot> executeAll();
}