/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package test.org.keycloak.quarkus.services.health;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class MetricsEnabledProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> config = new HashMap<>();
        config.put("kc.http-enabled", "true");
        config.put("kc.cluster", "local");
        config.put("kc.hostname-strict", "false");
        config.put("kc.db", "dev-mem");
        config.put("kc.db-connect-timeout", "250ms");
        config.put("kc.health-enabled", "true");
        config.put("kc.metrics-enabled", "true");
        config.put("kc.cache", "local");
        config.put("kc.server-async-bootstrap", "false");
        config.put("quarkus.micrometer.export.prometheus.path", "/prom/metrics");
        config.put("quarkus.class-loading.removed-artifacts", "io.quarkus:quarkus-jdbc-oracle,io.quarkus:quarkus-jdbc-oracle-deployment"); // config works a bit odd in unit tests, so this is to ensure we exclude Oracle to avoid ClassNotFound ex
        return config;
    }

}
