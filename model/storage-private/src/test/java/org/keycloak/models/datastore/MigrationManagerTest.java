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

package org.keycloak.models.datastore;

import org.keycloak.migration.ModelVersion;

import org.junit.Test;

import static org.keycloak.storage.datastore.DefaultMigrationManager.RHSSO_VERSION_7_0_KEYCLOAK_VERSION;
import static org.keycloak.storage.datastore.DefaultMigrationManager.RHSSO_VERSION_7_1_KEYCLOAK_VERSION;
import static org.keycloak.storage.datastore.DefaultMigrationManager.RHSSO_VERSION_7_2_KEYCLOAK_VERSION;
import static org.keycloak.storage.datastore.DefaultMigrationManager.RHSSO_VERSION_7_3_KEYCLOAK_VERSION;
import static org.keycloak.storage.datastore.DefaultMigrationManager.RHSSO_VERSION_7_4_KEYCLOAK_VERSION;
import static org.keycloak.storage.datastore.DefaultMigrationManager.convertRHSSOVersionToKeycloakVersion;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MigrationManagerTest {

    @Test
    public void testRHSSOVersionToKeycloakVersionConversion() {
        assertThat(convertRHSSOVersionToKeycloakVersion("7.0.0.GA"), is(equalTo(RHSSO_VERSION_7_0_KEYCLOAK_VERSION)));
        assertThat(convertRHSSOVersionToKeycloakVersion("7.0.1.GA"), is(equalTo(RHSSO_VERSION_7_0_KEYCLOAK_VERSION)));
        assertThat(convertRHSSOVersionToKeycloakVersion("7.0.2.GA"), is(equalTo(RHSSO_VERSION_7_0_KEYCLOAK_VERSION)));
        
        assertThat(convertRHSSOVersionToKeycloakVersion("7.1.0.GA"), is(equalTo(RHSSO_VERSION_7_1_KEYCLOAK_VERSION)));
        assertThat(convertRHSSOVersionToKeycloakVersion("7.1.1.GA"), is(equalTo(RHSSO_VERSION_7_1_KEYCLOAK_VERSION)));
        assertThat(convertRHSSOVersionToKeycloakVersion("7.1.2.GA"), is(equalTo(RHSSO_VERSION_7_1_KEYCLOAK_VERSION)));

        assertThat(convertRHSSOVersionToKeycloakVersion("7.2.0.GA"), is(equalTo(RHSSO_VERSION_7_2_KEYCLOAK_VERSION)));
        assertThat(convertRHSSOVersionToKeycloakVersion("7.2.1.GA"), is(equalTo(RHSSO_VERSION_7_2_KEYCLOAK_VERSION)));
        assertThat(convertRHSSOVersionToKeycloakVersion("7.2.2.GA"), is(equalTo(RHSSO_VERSION_7_2_KEYCLOAK_VERSION)));

        assertThat(convertRHSSOVersionToKeycloakVersion("7.3.0.GA"), is(equalTo(RHSSO_VERSION_7_3_KEYCLOAK_VERSION)));
        assertThat(convertRHSSOVersionToKeycloakVersion("7.3.1.GA"), is(equalTo(RHSSO_VERSION_7_3_KEYCLOAK_VERSION)));
        assertThat(convertRHSSOVersionToKeycloakVersion("7.3.2.GA"), is(equalTo(RHSSO_VERSION_7_3_KEYCLOAK_VERSION)));
        assertThat(convertRHSSOVersionToKeycloakVersion("7.3.10.GA"), is(equalTo(RHSSO_VERSION_7_3_KEYCLOAK_VERSION)));

        assertThat(convertRHSSOVersionToKeycloakVersion("7.4.0.GA"), is(equalTo(RHSSO_VERSION_7_4_KEYCLOAK_VERSION)));
        assertThat(convertRHSSOVersionToKeycloakVersion("7.4.15.GA"), is(equalTo(RHSSO_VERSION_7_4_KEYCLOAK_VERSION)));

        // check the conversion doesn't change version for keycloak
        assertThat(convertRHSSOVersionToKeycloakVersion("7.0.0"), is(nullValue()));
        assertThat(convertRHSSOVersionToKeycloakVersion("8.0.0"), is(nullValue()));

        // check for CD releases
        assertThat(convertRHSSOVersionToKeycloakVersion("6"), is(equalTo(new ModelVersion("6.0.0"))));
        assertThat(convertRHSSOVersionToKeycloakVersion("7"), is(equalTo(new ModelVersion("7.0.0"))));
        assertThat(convertRHSSOVersionToKeycloakVersion("10"), is(equalTo(new ModelVersion("10.0.0"))));
    }
}
