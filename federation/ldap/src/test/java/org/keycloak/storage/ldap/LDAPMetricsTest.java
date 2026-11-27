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

package org.keycloak.storage.ldap;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.profile.CommaSeparatedListProfileConfigResolver;
import org.keycloak.provider.ProviderConfigProperty;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LDAPMetricsTest {

    private static final String POOL_PROTOCOL = "com.sun.jndi.ldap.connect.pool.protocol";
    private Profile originalProfile;
    private String originalPoolProtocol;
    private PrometheusMeterRegistry registry;
    private LDAPStorageProviderFactory factory;
    private Timer timer;

    @Before
    public void setUp() {
        originalProfile = Profile.getInstance();
        originalPoolProtocol = System.getProperty(POOL_PROTOCOL);
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        Metrics.addRegistry(registry);
        factory = new LDAPStorageProviderFactory();
    }

    @After
    public void tearDown() {
        factory.close();
        if (timer != null) {
            Metrics.globalRegistry.remove(timer);
        }
        Metrics.removeRegistry(registry);
        registry.close();
        if (originalProfile == null) {
            Profile.reset();
        } else {
            Profile.init(originalProfile.getName(), originalProfile.getFeatures(), originalProfile.getEnablements());
        }
        if (originalPoolProtocol == null) {
            System.clearProperty(POOL_PROTOCOL);
        } else {
            System.setProperty(POOL_PROTOCOL, originalPoolProtocol);
        }
    }

    @Test
    public void publishesDefaultHistogramWhenEnabled() throws Exception {
        initializeFactory(true, true, Map.of("metricsHistogramsEnabled", "true"));
        assertTrue(registry.scrape().contains("keycloak_ldap_requests_seconds_bucket"));
    }

    @Test
    public void publishesConfiguredHistogramSlots() throws Exception {
        initializeFactory(true, true, Map.of("metricsHistogramSlots", "5, 10,,25"));
        String scrape = registry.scrape();
        assertTrue(scrape.contains("keycloak_ldap_requests_seconds_bucket{le=\"0.005\"}"));
        assertTrue(scrape.contains("keycloak_ldap_requests_seconds_bucket{le=\"0.01\"}"));
        assertTrue(scrape.contains("keycloak_ldap_requests_seconds_bucket{le=\"0.025\"}"));
    }

    @Test
    public void omitsHistogramsByDefault() throws Exception {
        initializeFactory(true, true, Map.of());
        assertTrue(registry.scrape().contains("keycloak_ldap_requests_seconds_count"));
        assertFalse(registry.scrape().contains("keycloak_ldap_requests_seconds_bucket"));
    }

    @Test
    public void disablesMetricsWhenFeatureIsDisabled() throws Exception {
        initializeFactory(false, true, histogramOptions());
        assertMetricsDisabled();
    }

    @Test
    public void disablesMetricsWhenGlobalMetricsAreDisabled() throws Exception {
        initializeFactory(true, false, histogramOptions());
        assertMetricsDisabled();
    }

    @Test
    public void disablesMetricsWhenSpiMetricsAreDisabled() throws Exception {
        Map<String, String> options = new HashMap<>(histogramOptions());
        options.put("metricsEnabled", "false");
        initializeFactory(true, true, options);
        assertMetricsDisabled();
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidHistogramSlots() throws Exception {
        initializeFactory(true, true, Map.of("metricsHistogramSlots", "5,not-a-number"));
    }

    private static Map<String, String> histogramOptions() {
        return Map.of("metricsHistogramsEnabled", "true", "metricsHistogramSlots", "5,10,25");
    }

    private void assertMetricsDisabled() {
        assertNull(timer);
        assertNull(registry.find("keycloak.ldap.requests").timer());
    }

    @SuppressWarnings("unchecked")
    private void initializeFactory(boolean featureEnabled, boolean globalMetricsEnabled, Map<String, String> options) throws Exception {
        String feature = Profile.Feature.LDAP_METRICS.getUnversionedKey();
        Profile.configure(new CommaSeparatedListProfileConfigResolver(
                featureEnabled ? feature : "", featureEnabled ? "" : feature));
        Map<String, String> providerOptions = new HashMap<>(options);
        // Avoid installing a JVM-wide JNDI object factory builder in a unit test.
        providerOptions.put("secureReferral", "false");
        Config.Scope root = scope(Map.of("metrics-enabled", Boolean.toString(globalMetricsEnabled)), null);
        factory.init(scope(providerOptions, root));

        // MeterProvider registers lazily; use the provider produced by init() to record a request.
        Field field = LDAPStorageProviderFactory.class.getDeclaredField("ldapRequestTimer");
        field.setAccessible(true);
        Meter.MeterProvider<Timer> provider = (Meter.MeterProvider<Timer>) field.get(factory);
        if (provider != null) {
            timer = provider.withTags();
            timer.record(8, TimeUnit.MILLISECONDS);
        }
    }

    private static Config.Scope scope(Map<String, String> options, Config.Scope root) {
        return new Config.AbstractScope() {
            @Override
            public String get(String key) {
                return options.get(key);
            }

            @Override
            public Config.Scope scope(String... names) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<String> getPropertyNames() {
                return options.keySet();
            }

            @Override
            public Config.Scope root() {
                return root == null ? this : root;
            }
        };
    }

    @Test
    public void exposesHistogramOptionsInProviderMetadata() {
        List<ProviderConfigProperty> properties = new LDAPStorageProviderFactory().getConfigMetadata();

        ProviderConfigProperty histogramsEnabled = properties.stream()
                .filter(property -> "metricsHistogramsEnabled".equals(property.getName()))
                .findFirst()
                .orElseThrow();
        ProviderConfigProperty histogramSlots = properties.stream()
                .filter(property -> "metricsHistogramSlots".equals(property.getName()))
                .findFirst()
                .orElseThrow();

        assertTrue("false".equals(histogramsEnabled.getDefaultValue()));
        assertTrue(histogramSlots.getType().equals(ProviderConfigProperty.STRING_TYPE));
    }
}
