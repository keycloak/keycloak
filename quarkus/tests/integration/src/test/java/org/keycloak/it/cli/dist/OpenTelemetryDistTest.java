package org.keycloak.it.cli.dist;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
public class OpenTelemetryDistTest {

    @Test
    @Launch({"start-dev", "--log-level=io.opentelemetry:fine", "--otel-traces-enabled=true", "--otel-logs-enabled=true", "--otel-metrics-enabled=true"})
    void enabled(CLIResult result) {
        result.assertStartedDevMode();
        TracingDistTest.assertTracingEnabled(result);
        result.assertMessage("traceId=, parentId=, spanId=, sampled=");
    }

    @Test
    @Launch({"start-dev", "--metrics-enabled=true", "--otel-metrics-export-interval=5s"})
    void metricsExportPeriod(CLIResult result) {
        result.assertStartedDevMode();
        assertThat(Configuration.getConfigValue("quarkus.otel.metric.export.interval").getValue(), is("5s"));
    }
}
