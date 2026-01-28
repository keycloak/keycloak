/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.utils.KeycloakDistribution;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DistributionTest(keepAlive = true,
        requestPort = 9000,
        containerExposedPorts = {8080, 9000})
@Tag(DistributionTest.SLOW)
public class MetricsDistTest {

    @Test
    @Launch({ "start-dev" })
    void testMetricsEndpointNotEnabled(KeycloakDistribution distribution) {
        assertThrows(IOException.class, () -> when().get("/metrics"), "Connection refused must be thrown");
        assertThrows(IOException.class, () -> when().get("/q/metrics"), "Connection refused must be thrown");

        distribution.setRequestPort(8080);

        when().get("/metrics").then()
                .statusCode(404);
        when().get("/q/metrics").then()
                .statusCode(404);
    }

    @Test
    @Launch({ "start-dev", "--metrics-enabled=true" })
    void testMetricsEndpoint(CLIResult cliResult) {
        // See https://github.com/keycloak/keycloak/issues/36927
        cliResult.assertNoMessage("A MeterFilter is being configured after a Meter has been registered to this registry.");

        // Send one request to populate some of the HTTP metrics that are not available on an instance on startup
        when().get("/metrics").then()
                .statusCode(200);

        when().get("/metrics").then()
                .statusCode(200)

                // Test metrics used in Observability guides are present
                // SLIs
                .body(containsString("TYPE http_server_requests_seconds summary"))

                // JVM metrics
                .body(containsString("TYPE jvm_info counter"))
                .body(containsString("TYPE jvm_memory_committed_bytes gauge"))
                .body(containsString("TYPE jvm_memory_used_bytes gauge"))
                .body(containsString("TYPE jvm_gc_pause_seconds_max gauge"))
                .body(containsString("TYPE jvm_gc_pause_seconds summary"))
                .body(containsString("TYPE jvm_gc_overhead gauge"))

                // Database metrics
                .body(containsString("TYPE agroal_available_count gauge"))
                .body(containsString("TYPE agroal_active_count gauge"))
                .body(containsString("TYPE agroal_awaiting_count gauge"))

                // HTTP metrics
                .body(containsString("TYPE http_server_active_requests gauge"))
                .body(containsString("TYPE http_server_bytes_written summary"))
                .body(containsString("TYPE http_server_bytes_read summary"))

                // Clustering and networking
                .body(containsString("TYPE vendor_cluster_size gauge"))

                // Embedded Infinispan
                .body(containsString("TYPE vendor_statistics_approximate_entries gauge"))
                .body(containsString("TYPE vendor_statistics_approximate_entries_unique gauge"))
                .body(containsString("TYPE vendor_statistics_store_times_seconds summary"))
                .body(containsString("TYPE vendor_statistics_hit_times_seconds summary"))
                .body(containsString("TYPE vendor_statistics_miss_times_seconds summary"))
                .body(containsString("TYPE vendor_statistics_remove_hit_times_seconds summary"))
                .body(containsString("TYPE vendor_statistics_remove_miss_times_seconds summary"))
                .body(containsString("TYPE vendor_statistics_evictions gauge"))
                .body(containsString("TYPE vendor_lock_manager_number_of_locks_held gauge"))
                .body(containsString("TYPE vendor_transactions_prepare_times_seconds summary"))
                .body(containsString("TYPE vendor_transactions_rollback_times_seconds summary"))
                .body(containsString("TYPE vendor_transactions_commit_times_seconds summary"))

                // Caffeine Metrics
                .body(containsString("TYPE cache_gets counter"))
                .body(containsString("TYPE cache_size gauge"))
                .body(containsString("TYPE cache_evictions summary"))

                // Test histograms are not available without explicitly enabling the option
                .body(not(containsString("vendor_statistics_miss_times_seconds_bucket")));

        when().get("/health").then()
                .statusCode(404);
    }

    @Test
    @Launch({ "start-dev", "--metrics-enabled=true", "--cache-metrics-histograms-enabled=true", "--http-metrics-slos=5,10,25,50,250,500", "--http-metrics-histograms-enabled=true" })
    void testMetricsEndpointWithCacheMetricsHistograms() {
        when().get("/metrics").then()
                .statusCode(200)
                .body(containsString("vendor_statistics_miss_times_seconds_bucket"));

        // histograms are only available at the second request as they then contain the metrics of the first request
        when().get("/metrics").then()
                .statusCode(200)
                .body(containsString("http_server_requests_seconds_bucket{method=\"GET\",outcome=\"SUCCESS\",status=\"200\",uri=\"/metrics\",le=\"0.005\"}"))
                .body(containsString("http_server_requests_seconds_bucket{method=\"GET\",outcome=\"SUCCESS\",status=\"200\",uri=\"/metrics\",le=\"0.005592405\"}"));

    }

    @Test
    @Launch({ "start-dev", "--metrics-enabled=true", "--tracing-enabled=true" })
    void testMetricsEndpointWithCacheMetricsHistogramsAndExemplars(KeycloakDistribution distribution) {
        runClientCredentialGrantWithUnknownClientId(distribution);

        distribution.setRequestPort(9000);
        // Exemplars are only present when metrics and traces are enabled
        given().accept("application/openmetrics-text; version=1.0.0; charset=utf-8");
        when().get("/metrics").then()
                .statusCode(200)
                // http_server_requests_seconds_count{method="GET",outcome="CLIENT_ERROR",status="404",uri="NOT_FOUND"} 7.0 # {span_id="59fb88a687095d04",trace_id="a4d15d4deaa6f6ee7ac2da092f292925"} 1.0 1743780073.651
                .body(matchesPattern("(?s).*http_server_requests_seconds_count.*,trace_id=.*"));

    }

    @Test
    @Launch({ "start-dev", "--metrics-enabled=true", "--features=user-event-metrics", "--event-metrics-user-enabled=true" })
    void testMetricsEndpointWithUserEventMetrics(KeycloakDistribution distribution) {
        runClientCredentialGrantWithUnknownClientId(distribution);

        distribution.setRequestPort(9000);
        when().get("/metrics").then()
                .statusCode(200)
                .body(containsString("keycloak_user_events_total{error=\"client_not_found\",event=\"client_login\",realm=\"master\"}"));

    }

    @Test
    @Launch({ "start-dev", "--metrics-enabled=true", "--features=user-event-metrics", "--event-metrics-user-enabled=false" })
    void testMetricsEndpointWithoutUserEventMetrics(KeycloakDistribution distribution) {
        runClientCredentialGrantWithUnknownClientId(distribution);

        distribution.setRequestPort(9000);
        when().get("/metrics").then()
                .statusCode(200)
                .body(not(containsString("keycloak_user_events_total{error=\"client_not_found\",event=\"client_login\",realm=\"master\"}")));

    }

    private static void runClientCredentialGrantWithUnknownClientId(KeycloakDistribution distribution) {
        distribution.setRequestPort(8080);
        given().formParam("grant_type", "client_credentials")
                .formParam("client_id", "unknown")
                .formParam("client_secret", "unknown").
                when().post("/realms/master/protocol/openid-connect/token")
                .then()
                .statusCode(401);
    }

    @Test
    void testUsingRelativePath(KeycloakDistribution distribution) {
        for (String relativePath : List.of("/auth", "/auth/", "auth")) {
            distribution.run("start-dev", "--metrics-enabled=true", "--http-management-relative-path=" + relativePath);
            if (!relativePath.endsWith("/")) {
                relativePath = relativePath + "/";
            }
            when().get(relativePath + "metrics").then().statusCode(200);
            distribution.stop();
        }
    }

    @Test
    void testMultipleRequests(KeycloakDistribution distribution) throws Exception {
        for (String relativePath : List.of("/", "/auth/", "auth")) {
            distribution.run("start-dev", "--metrics-enabled=true", "--http-management-relative-path=" + relativePath);
            CompletableFuture<Void> future = CompletableFuture.completedFuture(null);

            for (int i = 0; i < 3; i++) {
                future = CompletableFuture.allOf(CompletableFuture.runAsync(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 200; i++) {
                            String metricsPath = "metrics";

                            if (!relativePath.endsWith("/")) {
                                metricsPath = "/" + metricsPath;
                            }

                            when().get(relativePath + metricsPath).then().statusCode(200);
                        }
                    }
                }), future);
            }

            future.get(5, TimeUnit.MINUTES);

            distribution.stop();
        }
    }

}
