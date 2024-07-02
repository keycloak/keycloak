/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.it.cli.dist;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
public class TracingDistTest {

    private void assertTracingEnabled(CLIResult result) {
        result.assertMessage("io.opentelemetry");
        result.assertMessage("service.name=\"Keycloak\"");
    }

    private void assertTracingDisabled(CLIResult result) {
        result.assertMessage("io.opentelemetry");
        result.assertNoMessage("service.name=\"Keycloak\"");
    }

    @Test
    @Order(1)
    @Launch({"start-dev", "--log-level=io.opentelemetry:fine"})
    void disabled(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        cliResult.assertStartedDevMode();
        assertTracingDisabled(cliResult);
    }

    @Test
    @Order(2)
    @Launch({"start-dev", "--tracing-enabled=true", "--tracing-jdbc-enabled=true", "--log-level=io.opentelemetry:fine"})
    void enabledJdbc(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        cliResult.assertStartedDevMode();
        assertTracingEnabled(cliResult);
    }

    @Test
    @Order(3)
    @Launch({"build", "--tracing-enabled=true"})
    void buildTracingEnabled(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        cliResult.assertBuild();
    }

    @Test
    @Launch({"start", "--hostname-strict=false", "--http-enabled=true", "--optimized", "--log-level=io.opentelemetry:fine"})
    void enabled(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        cliResult.assertStarted();
        assertTracingEnabled(cliResult);
    }

    @Test
    @Launch({"start", "--hostname-strict=false", "--http-enabled=true", "--optimized", "--tracing-endpoint=http://endpoint:8888", "--log-level=io.opentelemetry:fine"})
    void differentEndpoint(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        cliResult.assertStarted();
        assertTracingEnabled(cliResult);
    }

    @Test
    @Launch({"start", "--hostname-strict=false", "--http-enabled=true", "--optimized", "--tracing-endpoint="})
    void emptyEndpoint(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        cliResult.assertError("URL specified in 'tracing-endpoint' option must not be empty.");
    }

    @Test
    @Launch({"start", "--hostname-strict=false", "--http-enabled=true", "--optimized", "--tracing-endpoint=ht://wrong"})
    void invalidUrl(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        cliResult.assertError("URL specified in 'tracing-endpoint' option is invalid.");
    }

    @Test
    @Launch({"start", "--hostname-strict=false", "--http-enabled=true", "--optimized", "--tracing-protocol=http/protobuf", "--log-level=io.opentelemetry:fine"})
    void protocolHttpProtobuf(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        assertTracingEnabled(cliResult);
    }

    @Test
    @Launch({"start", "--hostname-strict=false", "--http-enabled=true", "--optimized", "--tracing-protocol=wrong"})
    void unknownProtocol(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        cliResult.assertError("Invalid value for option '--tracing-protocol': wrong. Expected values are: grpc, http/protobuf");
    }

    @Test
    @Launch({"start", "--hostname-strict=false", "--http-enabled=true", "--optimized", "--tracing-sampler-ratio=0.0"})
    void wrongSamplerRatio(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        cliResult.assertError("Ratio in 'tracing-sampler-ratio' option must be a double value in interval <0,1).");
    }
}
