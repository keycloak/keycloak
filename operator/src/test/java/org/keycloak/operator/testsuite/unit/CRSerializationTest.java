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

import io.fabric8.kubernetes.client.utils.Serialization;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.DatabaseSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.FeatureSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.TransactionsSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HostnameSpec;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CRSerializationTest {

    @Test
    public void testDeserialization() {
        Keycloak keycloak = Serialization.unmarshal(this.getClass().getResourceAsStream("/test-serialization-keycloak-cr.yml"), Keycloak.class);

        assertEquals("my-hostname", keycloak.getSpec().getHostnameSpec().getHostname());
        assertEquals("my-image", keycloak.getSpec().getImage());
        assertEquals("my-tls-secret", keycloak.getSpec().getHttpSpec().getTlsSecret());
        assertFalse(keycloak.getSpec().getIngressSpec().isIngressEnabled());

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
}