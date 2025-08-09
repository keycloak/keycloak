package org.keycloak.it.cli.dist;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
public class OpenTelemetryDistTest {

    @Test
    @Launch({"start-dev", "--log-level=io.opentelemetry:fine", "--otel-traces-enabled=true", "--otel-logs-enabled=true"})
    void enabled(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStartedDevMode();
        TracingDistTest.assertTracingEnabled(cliResult);
        cliResult.assertMessage("traceId=, parentId=, spanId=, sampled=");
    }
}
