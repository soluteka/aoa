package com.aoa.aix.infrastructure.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "lparstat")
@JsonIgnoreProperties(ignoreUnknown = true)
public class LparstatModel {

    @JacksonXmlProperty(localName = "system_configuration")
    private SystemConfiguration systemConfiguration;

    @JacksonXmlProperty(localName = "utilization")
    private Utilization utilization;

    @JacksonXmlProperty(localName = "memory")
    private Memory memory;

    // ---- Compatibilidad con código existente ----
    public Cpu getCpu() {
        if (utilization == null) return null;
        Cpu c = new Cpu();
        c.setUser(utilization.getUserPct());
        c.setSys(utilization.getSysPct());
        c.setIdle(utilization.getIdlePct());
        return c;
    }

    public Lpar getLpar() {
        if (utilization == null) return null;
        Lpar l = new Lpar();
        l.setEntitled(systemConfiguration != null ? systemConfiguration.getEntitledCapacity() : 0.0);
        l.setPhysicalUsed(utilization.getPhysc());
        return l;
    }

    // ---- DTOs anidados según XML real ----
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SystemConfiguration {
        private String hostname;
        @JacksonXmlProperty(localName = "partition_name")   private String partitionName;
        @JacksonXmlProperty(localName = "partition_number") private int partitionNumber;
        private String type;
        private String mode;
        @JacksonXmlProperty(localName = "entitled_capacity")  private double entitledCapacity;
        @JacksonXmlProperty(localName = "online_virtual_cpus") private int onlineVirtualCpus;
        @JacksonXmlProperty(localName = "online_memory")      private int onlineMemory;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Utilization {
        @JacksonXmlProperty(localName = "user_pct") private double userPct;
        @JacksonXmlProperty(localName = "sys_pct")  private double sysPct;
        @JacksonXmlProperty(localName = "wait_pct") private double waitPct;
        @JacksonXmlProperty(localName = "idle_pct") private double idlePct;
        private double physc;
        @JacksonXmlProperty(localName = "entc_pct") private double entcPct;
        private double lbusy;
        private double app;
        private long vcsw;
        private long phint;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Memory {
        @JacksonXmlProperty(localName = "real_mb")               private long realMb;
        @JacksonXmlProperty(localName = "paging_space_mb")       private long pagingSpaceMb;
        @JacksonXmlProperty(localName = "paging_space_used_pct") private double pagingUsedPct;
        @JacksonXmlProperty(localName = "paging_rate")           private double pagingRate;
    }

    // ---- Wrappers legacy para no romper LparstatXmlParser ----
    @Data public static class Cpu  { private double user; private double sys; private double idle; }
    @Data public static class Lpar { private double entitled; private double physicalUsed; }
}