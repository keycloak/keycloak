/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.operator.testsuite.unit;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPeer;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.DatabaseSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.FeatureSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HostnameSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HttpManagementSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.ServiceMonitorSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.TracingSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.TransactionsSpec;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImport;
import org.keycloak.operator.testsuite.utils.K8sUtils;
import org.keycloak.operator.update.UpdateStrategy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class CRSerializationTest {

    private static final Map<String, String> CUSTOM_INGRESS_ANNOTATION = Map.of(
            "myAnnotation", "myValue",
            "anotherAnnotation", "anotherValue"
    );

    @Test
    public void testDeserialization() {
        Keycloak keycloak = Serialization.unmarshal(this.getClass().getResourceAsStream("/test-serialization-keycloak-cr.yml"), Keycloak.class);

        assertEquals("my-hostname", keycloak.getSpec().getHostnameSpec().getHostname());
        assertEquals("my-image", keycloak.getSpec().getImage());
        assertEquals("my-tls-secret", keycloak.getSpec().getHttpSpec().getTlsSecret());
        assertFalse(keycloak.getSpec().getIngressSpec().isIngressEnabled());
        assertEquals("nginx", keycloak.getSpec().getIngressSpec().getIngressClassName());
        assertEquals(CUSTOM_INGRESS_ANNOTATION, keycloak.getSpec().getIngressSpec().getAnnotations());

        final TransactionsSpec transactionsSpec = keycloak.getSpec().getTransactionsSpec();
        assertThat(transactionsSpec, notNullValue());
        assertThat(transactionsSpec.isXaEnabled(), notNullValue());
        assertThat(transactionsSpec.isXaEnabled(), CoreMatchers.is(false));

        List<ValueOrSecret> serverConfiguration = keycloak.getSpec().getAdditionalOptions();

        assertNotNull(serverConfiguration);
        assertFalse(serverConfiguration.isEmpty());
        assertThat(serverConfiguration, hasItem(hasProperty("name", is("key1"))));

        DatabaseSpec databaseSpec = keycloak.getSpec().getDatabaseSpec();
        assertNotNull(databaseSpec);
        assertEquals("vendor", databaseSpec.getVendor());
        assertEquals("database", databaseSpec.getDatabase());
        assertEquals("host", databaseSpec.getHost());
        assertEquals(123, databaseSpec.getPort());
        assertEquals("url", databaseSpec.getUrl());
        assertEquals("schema", databaseSpec.getSchema());
        assertEquals(1, databaseSpec.getPoolInitialSize());
        assertEquals(2, databaseSpec.getPoolMinSize());
        assertEquals(3, databaseSpec.getPoolMaxSize());
        assertEquals("usernameSecret", databaseSpec.getUsernameSecret().getName());
        assertEquals("usernameSecretKey", databaseSpec.getUsernameSecret().getKey());
        assertEquals("passwordSecret", databaseSpec.getPasswordSecret().getName());
        assertEquals("passwordSecretKey", databaseSpec.getPasswordSecret().getKey());

        HttpManagementSpec managementSpec = keycloak.getSpec().getHttpManagementSpec();
        assertNotNull(managementSpec);
        assertEquals(9003, managementSpec.getPort());

        assertEquals(50,keycloak.getSpec().getReadinessProbeSpec().getProbePeriodSeconds());
        assertEquals(3,keycloak.getSpec().getReadinessProbeSpec().getProbeFailureThreshold());
        assertEquals(60,keycloak.getSpec().getLivenessProbeSpec().getProbePeriodSeconds());
        assertEquals(1,keycloak.getSpec().getLivenessProbeSpec().getProbeFailureThreshold());
        assertEquals(40,keycloak.getSpec().getStartupProbeSpec().getProbePeriodSeconds());
        assertEquals(2,keycloak.getSpec().getStartupProbeSpec().getProbeFailureThreshold());
        assertEquals("MY_ENV_VAR", keycloak.getSpec().getEnv().get(0).getName());
        assertEquals("--- {}\n", Serialization.asYaml(keycloak.getSpec().getUpdateSpec().getSchedulingSpec()));
        assertEquals("x", keycloak.getSpec().getSchedulingSpec().getPriorityClassName());
    }

    @Test
    public void featureSpecification() {
        Keycloak keycloak = Serialization.unmarshal(this.getClass().getResourceAsStream("/test-serialization-keycloak-cr.yml"), Keycloak.class);

        final FeatureSpec featureSpec = keycloak.getSpec().getFeatureSpec();
        assertThat(featureSpec, notNullValue());

        final List<String> enabledFeatures = featureSpec.getEnabledFeatures();
        assertThat(enabledFeatures.size(), CoreMatchers.is(2));
        assertThat(enabledFeatures.get(0), CoreMatchers.is("docker"));
        assertThat(enabledFeatures.get(1), CoreMatchers.is("authorization"));

        final List<String> disabledFeatures = featureSpec.getDisabledFeatures();
        assertThat(disabledFeatures.size(), CoreMatchers.is(2));
        assertThat(disabledFeatures.get(0), CoreMatchers.is("admin"));
        assertThat(disabledFeatures.get(1), CoreMatchers.is("step-up-authentication"));
    }

    @Test
    public void hostnameSpecification() {
        Keycloak keycloak = Serialization.unmarshal(this.getClass().getResourceAsStream("/test-serialization-keycloak-cr.yml"), Keycloak.class);

        HostnameSpec hostnameSpec = keycloak.getSpec().getHostnameSpec();
        assertThat(hostnameSpec, notNullValue());

        assertThat(hostnameSpec.getHostname(), is("my-hostname"));
        assertThat(hostnameSpec.getAdmin(), is("my-admin-hostname"));
        assertThat(hostnameSpec.getAdminUrl(), is("https://www.my-admin-hostname.org:8448/something"));
        assertThat(hostnameSpec.isStrict(), is(true));
        assertThat(hostnameSpec.isStrictBackchannel(), is(true));

        keycloak = Serialization.unmarshal(this.getClass().getResourceAsStream("/empty-podtemplate-keycloak.yml"), Keycloak.class);

        hostnameSpec = keycloak.getSpec().getHostnameSpec();
        assertThat(hostnameSpec, notNullValue());
        assertThat(hostnameSpec.getHostname(), is("example.com"));
        assertThat(hostnameSpec.getAdmin(), nullValue());
        assertThat(hostnameSpec.getAdminUrl(), nullValue());
        assertThat(hostnameSpec.isStrict(), nullValue());
        assertThat(hostnameSpec.isStrictBackchannel(), nullValue());
    }

    @Test
    public void resourcesSpecification() {
        Keycloak keycloak = Serialization.unmarshal(this.getClass().getResourceAsStream("/test-serialization-keycloak-cr.yml"), Keycloak.class);

        ResourceRequirements resourceRequirements = keycloak.getSpec().getResourceRequirements();
        assertThat(resourceRequirements, notNullValue());

        assertThat(resourceRequirements.getClaims(), is(Collections.emptyList()));
        assertThat(resourceRequirements.getAdditionalProperties(), is(Collections.emptyMap()));

        // Requests
        assertThat(resourceRequirements.getRequests(), notNullValue());
        final var reqCpuQuantity = resourceRequirements.getRequests().get("cpu");
        assertThat(reqCpuQuantity, notNullValue());
        assertThat(reqCpuQuantity.getAmount(), is("500"));
        assertThat(reqCpuQuantity.getFormat(), is("m"));
        final var reqMemQuantity = resourceRequirements.getRequests().get("memory");
        assertThat(reqMemQuantity, notNullValue());
        assertThat(reqMemQuantity.getAmount(), is("500"));
        assertThat(reqMemQuantity.getFormat(), is("M"));

        // Limits
        assertThat(resourceRequirements.getLimits(), notNullValue());
        final var limitCpuQuantity = resourceRequirements.getLimits().get("cpu");
        assertThat(limitCpuQuantity, notNullValue());
        assertThat(limitCpuQuantity.getAmount(), is("2"));
        assertThat(limitCpuQuantity.getFormat(), emptyString());
        final var limitMemQuantity = resourceRequirements.getLimits().get("memory");
        assertThat(limitMemQuantity, notNullValue());
        assertThat(limitMemQuantity.getAmount(), is("1500"));
        assertThat(limitMemQuantity.getFormat(), is("M"));
    }

    @Test
    public void tracingSpecification() {
        Keycloak keycloak = Serialization.unmarshal(this.getClass().getResourceAsStream("/test-serialization-keycloak-cr.yml"), Keycloak.class);

        TracingSpec tracing = keycloak.getSpec().getTracingSpec();
        assertThat(tracing, notNullValue());

        assertThat(tracing.getEnabled(), is(true));
        assertThat(tracing.getEndpoint(), is("http://my-tracing:4317"));
        assertThat(tracing.getServiceName(), is("my-best-keycloak"));
        assertThat(tracing.getProtocol(), is("http/protobuf"));
        assertThat(tracing.getSamplerType(), is("parentbased_traceidratio"));
        assertThat(tracing.getSamplerRatio(), is(0.01));
        assertThat(tracing.getCompression(), is("gzip"));

        var attributes = tracing.getResourceAttributes();
        assertThat(attributes, notNullValue());

        assertThat(attributes.size(), is(2));
        assertThat(attributes, hasEntry("service.namespace", "keycloak-namespace"));
        assertThat(attributes, hasEntry("service.name", "custom-service-name"));

        var additionalOptions = keycloak.getSpec().getAdditionalOptions().stream().collect(Collectors.toMap(ValueOrSecret::getName, e -> e));
        assertNotNull(additionalOptions);
        assertThat(additionalOptions.isEmpty(), is(false));
        assertThat(additionalOptions, hasEntry("tracing-header-Authorization", new ValueOrSecret("tracing-header-Authorization", new SecretKeySelector("tracing-secret", "token", false))));
        assertThat(additionalOptions, hasEntry("tracing-header-X-Org-Id", new ValueOrSecret("tracing-header-X-Org-Id", "my-org-id")));
    }

    @Test
    public void resourcesSpecificationOnlyLimit() {
        final Keycloak keycloak = K8sUtils.getResourceFromFile("test-serialization-keycloak-cr-with-empty-list.yml", Keycloak.class);

        ResourceRequirements resourceRequirements = keycloak.getSpec().getResourceRequirements();
        assertThat(resourceRequirements, notNullValue());
        assertThat(resourceRequirements.getRequests(), is(Collections.emptyMap()));

        assertThat(resourceRequirements.getLimits(), notNullValue());
        final var limitCpuQuantity = resourceRequirements.getLimits().get("cpu");
        assertThat(limitCpuQuantity, notNullValue());
        assertThat(limitCpuQuantity.getAmount(), is("3"));
        assertThat(limitCpuQuantity.getFormat(), emptyString());
        final var limitMemQuantity = resourceRequirements.getLimits().get("memory");
        assertThat(limitMemQuantity, notNullValue());
        assertThat(limitMemQuantity.getAmount(), is("5"));
        assertThat(limitMemQuantity.getFormat(), is("Gi"));
    }

    @Test
    public void resourcesSpecificationRealmImport() {
        final KeycloakRealmImport keycloak = K8sUtils.getResourceFromFile("test-serialization-realmimport-cr.yml", KeycloakRealmImport.class);

        ResourceRequirements resourceRequirements = keycloak.getSpec().getResourceRequirements();
        assertThat(resourceRequirements, notNullValue());

        var requests = resourceRequirements.getRequests();
        assertThat(requests, notNullValue());
        assertThat(requests, is(Collections.emptyMap()));

        var limits = resourceRequirements.getLimits();
        assertThat(limits, notNullValue());
        final var limitCpuQuantity = limits.get("cpu");
        assertThat(limitCpuQuantity, notNullValue());
        assertThat(limitCpuQuantity.getAmount(), is("4"));
        assertThat(limitCpuQuantity.getFormat(), emptyString());
        final var limitMemQuantity = limits.get("memory");
        assertThat(limitMemQuantity, notNullValue());
        assertThat(limitMemQuantity.getAmount(), is("8"));
        assertThat(limitMemQuantity.getFormat(), is("Gi"));
    }

    @Test
    public void testNetworkPolicy() {
        var keycloak = Serialization.unmarshal(this.getClass().getResourceAsStream("/test-serialization-keycloak-cr.yml"), Keycloak.class);
        var networkPolicySpec = keycloak.getSpec().getNetworkPolicySpec();
        assertNotNull(networkPolicySpec);
        assertTrue(networkPolicySpec.isNetworkPolicyEnabled());
        assertNetworkPolicyRules(networkPolicySpec.getHttpRules());
        assertNetworkPolicyRules(networkPolicySpec.getHttpsRules());
        assertNetworkPolicyRules(networkPolicySpec.getManagementRules());
    }

    @Test
    public void testUpdateStrategy() {
        var keycloak = Serialization.unmarshal(this.getClass().getResourceAsStream("/test-serialization-keycloak-cr.yml"), Keycloak.class);
        var updateSpec = keycloak.getSpec().getUpdateSpec();
        assertNotNull(updateSpec);
        var updateStrategy = updateSpec.getStrategy();
        assertNotNull(updateStrategy);
        assertEquals(UpdateStrategy.AUTO, updateStrategy);
    }

    @Test
    public void testInvalidUpdateStrategy() {
        var thrown = assertThrows(IllegalArgumentException.class,
                () -> Serialization.unmarshal(this.getClass().getResourceAsStream("/test-serialization-keycloak-cr-invalid-update.yml"), Keycloak.class));
        assertTrue(thrown.getMessage().contains("Cannot deserialize value of type `org.keycloak.operator.update.UpdateStrategy` from String \"abc\""));
    }

    @Test
    public void testUpdateStrategyRevision() {
        var keycloak = Serialization.unmarshal(this.getClass().getResourceAsStream("/test-serialization-keycloak-cr.yml"), Keycloak.class);
        var updateSpec = keycloak.getSpec().getUpdateSpec();
        assertNotNull(updateSpec);
        var revision = updateSpec.getRevision();
        assertNotNull(revision);
        assertEquals("1", revision);
    }

    @Test
    public void serviceMonitorSpecification() {
        Keycloak keycloak = Serialization.unmarshal(this.getClass().getResourceAsStream("/test-serialization-keycloak-cr.yml"), Keycloak.class);

        ServiceMonitorSpec serviceMonitorSpec = keycloak.getSpec().getServiceMonitorSpec();
        assertThat(serviceMonitorSpec, notNullValue());

        assertThat(serviceMonitorSpec.isEnabled(), is(true));
        assertThat(serviceMonitorSpec.getInterval(), is(ServiceMonitorSpec.DEFAULT_INTERVAL));
        assertThat(serviceMonitorSpec.getScrapeTimeout(), is(ServiceMonitorSpec.DEFAULT_SCRAPE_TIMEOUT));
    }

    private static void assertNetworkPolicyRules(Collection<NetworkPolicyPeer> rules) {
        assertNotNull(rules);
        assertEquals(3, rules.size());
        for (var peer : rules) {
            assertNotNull(peer);
            if (peer.getPodSelector() != null) {
                assertEquals("frontend", peer.getPodSelector().getMatchLabels().get("role"));
                continue;
            }
            if (peer.getNamespaceSelector() != null) {
                assertEquals("myproject", peer.getNamespaceSelector().getMatchLabels().get("project"));
                continue;
            }
            if (peer.getIpBlock() != null) {
                assertEquals("172.17.0.0/16", peer.getIpBlock().getCidr());
                var except = peer.getIpBlock().getExcept();
                assertEquals(1, except.size());
                assertEquals("172.17.1.0/24", except.get(0));
                continue;
            }
            fail();
        }
    }
    @Test
    public void testNoAutoMountServiceAccountToken() {
        var keycloak = Serialization.unmarshal(this.getClass().getResourceAsStream("/test-serialization-keycloak-cr-without-automount.yml"), Keycloak.class);
        var keycloakSpec = keycloak.getSpec();
        assertNotNull(keycloakSpec);
        assertFalse(keycloakSpec.getAutomountServiceAccountToken());
    }
}
