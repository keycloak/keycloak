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

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
public class TracingDistTest {

    private void assertTracingEnabled(CLIResult result) {
        result.assertMessage("opentelemetry");
        result.assertMessage("service.name=\"keycloak\"");
    }

    private void assertTracingDisabled(CLIResult result) {
        result.assertMessage("opentelemetry");
        result.assertNoMessage("service.name=\"keycloak\"");
        assertSamplingDisabled(result);
    }

    private void assertSamplingDisabled(CLIResult result) {
        result.assertNoMessage("Failed to export spans.");
        result.assertNoMessage("Connection refused: localhost/127.0.0.1:4317");
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
    @Launch({"start-dev", "--tracing-service-name=should-fail"})
    void disabledOption(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        cliResult.assertError("Disabled option: '--tracing-service-name'. Available only when Tracing is enabled");
    }

    @Test
    @Order(3)
    @Launch({"start-dev", "--features-disabled=opentelemetry", "--tracing-enabled=true"})
    void disabledFeature(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        cliResult.assertError("Disabled option: '--tracing-enabled'. Available only when 'opentelemetry' feature is enabled");
    }

    @Test
    @Order(4)
    @Launch({"start-dev", "--tracing-enabled=false", "--tracing-endpoint=something"})
    void disabledTracing(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        cliResult.assertError("Disabled option: '--tracing-endpoint'. Available only when Tracing is enabled");
    }

    @Test
    @Order(5)
    @Launch({"build", "--db=dev-file", "--tracing-enabled=true"})
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
        // Initial system logs do not have any tracing data
        cliResult.assertMessage("traceId=, parentId=, spanId=, sampled=");
    }

    @Test
    @Launch({"start", "--hostname-strict=false", "--http-enabled=true", "--optimized", "--tracing-sampler-ratio=0.0", "--log-level=io.opentelemetry:fine"})
    void samplingDisabledViaRatioZero(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        cliResult.assertStarted();
        assertTracingEnabled(cliResult);
        assertSamplingDisabled(cliResult);
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
    @Launch({"start", "--hostname-strict=false", "--http-enabled=true", "--optimized", "--tracing-sampler-ratio=1.1"})
    void wrongSamplerRatio(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        cliResult.assertError("Ratio in 'tracing-sampler-ratio' option must be a double value in interval [0,1].");
    }

    @Test
    @Launch({"start", "--hostname-strict=false", "--http-enabled=true", "--optimized", "--tracing-compression=wrong"})
    void wrongCompression(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        cliResult.assertError("Invalid value for option '--tracing-compression': wrong. Expected values are: gzip, none");
    }

    @Test
    @Launch({"start", "--hostname-strict=false", "--http-enabled=true", "--optimized", "--log-console-include-trace=false"})
    void hideTracingInfoInConsoleLog(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        // Initial system logs do not have any tracing data
        cliResult.assertNoMessage("traceId=, parentId=, spanId=, sampled=");
        cliResult.assertStarted();
    }

    @Test
    @Launch({"start", "--hostname-strict=false", "--http-enabled=true", "--optimized", "--log-level=io.opentelemetry:fine", "--tracing-service-name=my-service"})
    void differentServiceName(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        cliResult.assertMessage("opentelemetry");
        cliResult.assertMessage("service.name=\"my-service\"");

        cliResult.assertStarted();
    }

    @Test
    @Launch({"start", "--hostname-strict=false", "--http-enabled=true", "--optimized", "--log-level=io.opentelemetry:fine", "--tracing-resource-attributes=service.name=new-service"})
    void serviceNameResourceAttributes(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        // the default value should be used
        assertTracingEnabled(cliResult);

        cliResult.assertStarted();
    }

    @Test
    @Launch({"start", "--hostname-strict=false", "--http-enabled=true", "--optimized", "--log-level=io.opentelemetry:fine", "--tracing-resource-attributes=some.key1=some.val1,some.key2=some.val2",})
    void resourceAttributes(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        assertTracingEnabled(cliResult);

        cliResult.assertMessage("some.key1=\"some.val1\"");
        cliResult.assertMessage("some.key2=\"some.val2\"");

        cliResult.assertStarted();
    }

    @Test
    @Launch({"start", "--hostname-strict=false", "--http-enabled=true", "--optimized", "--log-level=io.opentelemetry:fine", "--tracing-header-Authorization=\"Bearer asdlkfjadsflkj\"", "--tracing-header-Host=localhost:8080"})
    void headers(CLIResult cliResult) {
        assertTracingEnabled(cliResult);

        // There is no message in the attributes about headers
        cliResult.assertStarted();
    }
}
