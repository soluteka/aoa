package com.aoa.aix.infrastructure.outbound.parser;

import com.aoa.aix.domain.model.LparstatSnapshot;
import com.aoa.aix.infrastructure.dto.LparstatModel;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
public class LparstatXmlParser {

    private final XmlMapper xmlMapper = new XmlMapper();
    {
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /** Compat: parser legado (host == lparName, env desconocido). */
    public LparstatSnapshot parse(String xml, String host) {
        return parse(xml, host, host, "unknown");
    }

    /** Parser multi-LPAR: incluye nombre lógico y entorno. */
    public LparstatSnapshot parse(String xml, String host, String lparName, String environment) {
        try {
            LparstatModel m = xmlMapper.readValue(xml, LparstatModel.class);

            LparstatModel.Utilization u = m.getUtilization();
            LparstatModel.SystemConfiguration sc = m.getSystemConfiguration();
            LparstatModel.Memory mem = m.getMemory();

            return LparstatSnapshot.builder()
                    .timestamp(Instant.now())
                    .host(host)
                    .lparName(lparName == null ? host : lparName)
                    .environment(environment == null ? "unknown" : environment)

                    // CPU
                    .cpuUser(u != null ? u.getUserPct() : 0.0)
                    .cpuSys (u != null ? u.getSysPct()  : 0.0)
                    .cpuIdle(u != null ? u.getIdlePct() : 0.0)
                    .cpuWait(u != null ? u.getWaitPct() : 0.0)

                    // LPAR
                    .entitledCapacity(sc != null ? sc.getEntitledCapacity() : 0.0)
                    .physicalCpuUsed(u != null ? u.getPhysc()   : 0.0)
                    .entcPct        (u != null ? u.getEntcPct() : 0.0)
                    .lbusy          (u != null ? u.getLbusy()   : 0.0)
                    .app            (u != null ? u.getApp()     : 0.0)

                    // Memory
                    .pagingSpaceUsedPct(mem != null ? mem.getPagingUsedPct() : 0.0)
                    .pagingRate        (mem != null ? mem.getPagingRate()    : 0.0)

                    .build();
        } catch (Exception e) {
            log.error("Error parsing lparstat XML for [{}]: {}", lparName, e.getMessage());
            return null;
        }
    }
}