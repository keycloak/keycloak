/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.quarkus.runtime.services.metrics;

import org.keycloak.quarkus.runtime.configuration.AbstractConfigurationTest;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class JGroupsMeterFilterTest extends AbstractConfigurationTest {

    private static final String OPTION = "--spi-cache-embedded--default--metrics-rename-cluster-label=";

    @Test
    public void preservesLegacyLabelByDefault() {
        initConfig();
        Meter.Id id = meter("vendor.jgroups_tcp_get_num_bytes_sent", Tags.of("cluster", "ISPN"));
        assertSame(id, new JGroupsMeterFilter().map(id));
    }

    @Test
    public void preservesLegacyLabelWhenDisabled() {
        ConfigArgsConfigSource.setCliArgs(OPTION + "false");
        initConfig();
        Meter.Id id = meter("vendor.jgroups_tcp_get_num_bytes_sent", Tags.of("cluster", "ISPN"));
        assertSame(id, new JGroupsMeterFilter().map(id));
    }

    @Test
    public void renamesOnlyJGroupsClusterLabel() {
        ConfigArgsConfigSource.setCliArgs(OPTION + "true");
        initConfig();
        var filter = new JGroupsMeterFilter();
        Meter.Id id = meter("vendor.jgroups_tcp_get_num_bytes_sent",
                Tags.of("cluster", "custom-channel", "node", "node-1", "cache_manager", "keycloak"));
        Meter.Id mapped = filter.map(id);
        assertNull(mapped.getTag("cluster"));
        assertEquals("custom-channel", mapped.getTag("jgroups_cluster"));
        assertEquals("node-1", mapped.getTag("node"));
        assertEquals("keycloak", mapped.getTag("cache_manager"));
        assertEquals(id.getName(), mapped.getName());
        assertEquals(id.getType(), mapped.getType());
        assertEquals(id.getBaseUnit(), mapped.getBaseUnit());
        assertEquals(id.getDescription(), mapped.getDescription());
        assertEquals(mapped, filter.map(mapped));

        for (String name : new String[] { "vendor.cluster_size", "http.server.requests", "vendor.jgroupsOther", "vendor_jgroups_other" }) {
            Meter.Id unrelated = meter(name, Tags.of("cluster", "platform-cluster"));
            assertEquals(unrelated, filter.map(unrelated));
        }
        Meter.Id withoutCluster = meter("vendor.jgroups_tcp_get_num_bytes_sent", Tags.of("node", "node-1"));
        assertEquals(withoutCluster, filter.map(withoutCluster));
    }

    @Test
    public void acceptsEnvironmentOptionAndExportsRenamedLabel() {
        putEnvVar("KC_SPI_CACHE_EMBEDDED__DEFAULT__METRICS_RENAME_CLUSTER_LABEL", "true");
        initConfig();
        var registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        try {
            registry.config().meterFilter(new JGroupsMeterFilter());
            registry.gauge("vendor.jgroups_tcp_get_num_bytes_sent", Tags.of("cluster", "ISPN"), 42);
            String scrape = registry.scrape();
            assertTrue(scrape, scrape.contains("vendor_jgroups_tcp_get_num_bytes_sent{"));
            assertTrue(scrape, scrape.contains("jgroups_cluster=\"ISPN\""));
            assertFalse(scrape, scrape.contains("{cluster=") || scrape.contains(",cluster="));
        } finally {
            registry.close();
        }
    }

    private static Meter.Id meter(String name, Tags tags) {
        return new Meter.Id(name, tags, "bytes", "Test metric", Meter.Type.GAUGE);
    }
}
