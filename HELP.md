# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/3.5.14/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.5.14/maven-plugin/build-image.html)
* [Spring Integration Test Module Reference Guide](https://docs.spring.io/spring-integration/reference/testing.html)
* [Spring Integration HTTP Module Reference Guide](https://docs.spring.io/spring-integration/reference/http.html)
* [Spring Web](https://docs.spring.io/spring-boot/3.5.14/reference/web/servlet.html)
* [Spring Integration](https://docs.spring.io/spring-boot/3.5.14/reference/messaging/spring-integration.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Integrating Data](https://spring.io/guides/gs/integration/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

### Para tener en cuenta en la implentacion AIX
Dependencia en SyslogMessageHandler

En AIX configurar el syslog para reenviar al agente:
*.err  @ip-agente (UDP 514 / aquí 5140)
O directo: errpt -c | logger -p user.err -t errpt

Habilita @Scheduled en tu clase principal:

java
Copy
@SpringBootApplication
@EnableScheduling
public class AoaLogsAgentApplication { ... }

Resumen del flujo
Origen AIX	Mecanismo	Componente Java	Salida
errpt → logger	Syslog UDP/TCP (5140/5141)	SyslogIngestConfig + SyslogMessageHandler	OTLP Logs
lparstat -X	Ejecución periódica + Jackson XML	LparstatService	OTLP Metrics

Cómo se logra la resiliencia / no perder telemetría
Mecanismo	Cómo se implementa
Separación por entorno	application-dev.yml (Windows) y application-prod.yml (AWS), activado con SPRING_PROFILES_ACTIVE=prod
Endpoint configurable	otel.exporter.otlp.endpoint (puede venir de variable de entorno en AWS)
Retry con backoff exponencial	RetryPolicy aplicado a los exporters OTLP (gRPC o HTTP)
Buffer en memoria	BatchLogRecordProcessor + PeriodicMetricReader con cola (maxQueueSize) — si la red cae, los datos quedan en memoria y se reintentan
Flush al apagar	shutdownHook ejecuta shutdown().join(...) para vaciar buffers
Compresión / headers / timeouts	Configurables por perfil
6) Cómo ejecutar por entorno
Desarrollo (Windows):

powershell
Copy
$env:SPRING_PROFILES_ACTIVE="dev"
mvn spring-boot:run
Producción (AWS):

bash
Copy
export SPRING_PROFILES_ACTIVE=prod
export OTEL_EXPORTER_OTLP_ENDPOINT=https://otlp.aws.tu-nube.com:4317
export OTEL_EXPORTER_OTLP_HEADERS="x-api-key=XXXX,tenant=aix"
java -jar aoa_logs_agent-0.0.1-SNAPSHOT.jar
✅ Con esto el agente:

Usa el endpoint correcto por entorno.
Reintenta automáticamente con backoff si la nube falla.
Conserva los indicadores en memoria (cola batch) hasta que el OTLP vuelva a estar disponible — sin perder observabilidad.