package com.aoa.aix.infrastructure.config;

import com.aoa.aix.infrastructure.inbound.syslog.SyslogMessageHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.integration.syslog.inbound.TcpSyslogReceivingChannelAdapter;
import org.springframework.integration.syslog.inbound.UdpSyslogReceivingChannelAdapter;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SyslogIngestConfig {

    public static final String SYSLOG_CHANNEL = "syslogInputChannel";

    private final SyslogMessageHandler syslogMessageHandler;

    @Value("${aix.syslog.udp-port:1514}")
    private int udpPort;

    @Value("${aix.syslog.tcp-port:1514}")
    private int tcpPort;

    @Bean(name = SYSLOG_CHANNEL)
    public DirectChannel syslogInputChannel() {
        DirectChannel channel = new DirectChannel();
        channel.subscribe(syslogMessageHandler);
        log.info("✅ SyslogMessageHandler subscribed to '{}'", SYSLOG_CHANNEL);
        return channel;
    }

    @PostConstruct
    public void init() {
        log.info("SyslogIngestConfig: udpPort={}, tcpPort={}", udpPort, tcpPort);
    }

    // ── UDP ──
    @Bean
    public UnicastReceivingChannelAdapter udpReceiver() {
        UnicastReceivingChannelAdapter r = new UnicastReceivingChannelAdapter(udpPort);
        r.setAutoStartup(false);
        return r;
    }

    @Bean(initMethod = "start")
    public UdpSyslogReceivingChannelAdapter udpSyslogAdapter(
            UnicastReceivingChannelAdapter udpReceiver) {
        UdpSyslogReceivingChannelAdapter a = new UdpSyslogReceivingChannelAdapter();
        a.setUdpAdapter(udpReceiver);
        a.setOutputChannelName(SYSLOG_CHANNEL);   // ← resolución por nombre
        a.setAutoStartup(true);
        log.info("UDP Syslog adapter configured on port {} → channel '{}'", udpPort, SYSLOG_CHANNEL);
        return a;
    }

    // ── TCP ──
    @Bean
    public AbstractServerConnectionFactory tcpConnectionFactory() {
        return new TcpNioServerConnectionFactory(tcpPort);
    }

    @Bean(initMethod = "start")
    public TcpSyslogReceivingChannelAdapter tcpSyslogAdapter(
            AbstractServerConnectionFactory tcpConnectionFactory) {
        TcpSyslogReceivingChannelAdapter a = new TcpSyslogReceivingChannelAdapter();
        a.setConnectionFactory(tcpConnectionFactory);
        a.setOutputChannelName(SYSLOG_CHANNEL);   // ← resolución por nombre
        a.setAutoStartup(true);
        log.info("TCP Syslog adapter configured on port {} → channel '{}'", tcpPort, SYSLOG_CHANNEL);
        return a;
    }
}