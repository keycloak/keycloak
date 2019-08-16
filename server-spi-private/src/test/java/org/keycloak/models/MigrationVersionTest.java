/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.migration.ModelVersion;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.keycloak.migration.MigrationModelManager.*;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MigrationVersionTest {

    @Test
    public void testVersion() {
        ModelVersion version_100Beta1 = new ModelVersion("1.0.0.Beta1-SNAPSHOT");
        Assert.assertEquals(version_100Beta1.getMajor(), 1);
        Assert.assertEquals(version_100Beta1.getMinor(), 0);
        Assert.assertEquals(version_100Beta1.getMicro(), 0);
        Assert.assertTrue(version_100Beta1.isSnapshot());
        ModelVersion version_100CR1 = new ModelVersion("1.0.0.CR1");
        ModelVersion version_100 = new ModelVersion("1.0.0");
        ModelVersion version_110Beta1 = new ModelVersion("1.1.0.Beta1");
        ModelVersion version_110CR1 = new ModelVersion("1.1.0.CR1");
        ModelVersion version_110 = new ModelVersion("1.1.0");

        ModelVersion version_120CR1 = new ModelVersion("1.2.0.CR1");
        ModelVersion version_130Beta1 = new ModelVersion("1.3.0.Beta1");
        ModelVersion version_130 = new ModelVersion("1.3.0");
        ModelVersion version_140 = new ModelVersion("1.4.0");

        ModelVersion version_211CR1 = new ModelVersion("2.1.1.CR1");
        Assert.assertEquals(version_211CR1.getMajor(), 2);
        Assert.assertEquals(version_211CR1.getMinor(), 1);
        Assert.assertEquals(version_211CR1.getMicro(), 1);
        Assert.assertEquals(version_211CR1.getQualifier(), "CR1");
        Assert.assertFalse(version_211CR1.isSnapshot());
        ModelVersion version_211 = new ModelVersion("2.1.1");

        ModelVersion version50Snapshot = new ModelVersion("5.0.0-SNAPSHOT");
        Assert.assertEquals(version50Snapshot.getMajor(), 5);
        Assert.assertEquals(version50Snapshot.getMinor(), 0);
        Assert.assertEquals(version50Snapshot.getMicro(), 0);
        Assert.assertNull(version50Snapshot.getQualifier());
        Assert.assertTrue(version50Snapshot.isSnapshot());

        Assert.assertFalse(version_100Beta1.lessThan(version_100Beta1));
        Assert.assertTrue(version_100Beta1.lessThan(version_100CR1));
        Assert.assertTrue(version_100Beta1.lessThan(version_100));
        Assert.assertTrue(version_100Beta1.lessThan(version_110Beta1));
        Assert.assertTrue(version_100Beta1.lessThan(version_110CR1));
        Assert.assertTrue(version_100Beta1.lessThan(version_110));
        Assert.assertFalse(version_110Beta1.lessThan(version_100CR1));
        Assert.assertFalse(version_130Beta1.lessThan(version_120CR1));
        Assert.assertTrue(version_130Beta1.lessThan(version_130));
        Assert.assertTrue(version_130Beta1.lessThan(version_140));
        Assert.assertFalse(version_211CR1.lessThan(version_140));
        Assert.assertTrue(version_140.lessThan(version_211CR1));

        Assert.assertFalse(version_211.lessThan(version_110CR1));

        Assert.assertTrue(version_211CR1.lessThan(version50Snapshot));

    }

    @Test
    public void testRHSSOVersionToKeycloakVersionConversion() {
        Assert.assertThat(convertRHSSOVersionToKeycloakVersion("7.0.0.GA"), is(equalTo(RHSSO_VERSION_7_0_KEYCLOAK_VERSION)));
        Assert.assertThat(convertRHSSOVersionToKeycloakVersion("7.0.1.GA"), is(equalTo(RHSSO_VERSION_7_0_KEYCLOAK_VERSION)));
        Assert.assertThat(convertRHSSOVersionToKeycloakVersion("7.0.2.GA"), is(equalTo(RHSSO_VERSION_7_0_KEYCLOAK_VERSION)));
        
        Assert.assertThat(convertRHSSOVersionToKeycloakVersion("7.1.0.GA"), is(equalTo(RHSSO_VERSION_7_1_KEYCLOAK_VERSION)));
        Assert.assertThat(convertRHSSOVersionToKeycloakVersion("7.1.1.GA"), is(equalTo(RHSSO_VERSION_7_1_KEYCLOAK_VERSION)));
        Assert.assertThat(convertRHSSOVersionToKeycloakVersion("7.1.2.GA"), is(equalTo(RHSSO_VERSION_7_1_KEYCLOAK_VERSION)));

        Assert.assertThat(convertRHSSOVersionToKeycloakVersion("7.2.0.GA"), is(equalTo(RHSSO_VERSION_7_2_KEYCLOAK_VERSION)));
        Assert.assertThat(convertRHSSOVersionToKeycloakVersion("7.2.1.GA"), is(equalTo(RHSSO_VERSION_7_2_KEYCLOAK_VERSION)));
        Assert.assertThat(convertRHSSOVersionToKeycloakVersion("7.2.2.GA"), is(equalTo(RHSSO_VERSION_7_2_KEYCLOAK_VERSION)));

        Assert.assertThat(convertRHSSOVersionToKeycloakVersion("7.3.0.GA"), is(equalTo(RHSSO_VERSION_7_3_KEYCLOAK_VERSION)));
        Assert.assertThat(convertRHSSOVersionToKeycloakVersion("7.3.1.GA"), is(equalTo(RHSSO_VERSION_7_3_KEYCLOAK_VERSION)));
        Assert.assertThat(convertRHSSOVersionToKeycloakVersion("7.3.2.GA"), is(equalTo(RHSSO_VERSION_7_3_KEYCLOAK_VERSION)));
        Assert.assertThat(convertRHSSOVersionToKeycloakVersion("7.3.10.GA"), is(equalTo(RHSSO_VERSION_7_3_KEYCLOAK_VERSION)));

        // check the conversion doesn't change version for keycloak
        Assert.assertThat(convertRHSSOVersionToKeycloakVersion("7.0.0"), is(nullValue()));
        Assert.assertThat(convertRHSSOVersionToKeycloakVersion("8.0.0"), is(nullValue()));

        // check for CD releases
        Assert.assertThat(convertRHSSOVersionToKeycloakVersion("6"), is(equalTo(new ModelVersion("6.0.0"))));
        Assert.assertThat(convertRHSSOVersionToKeycloakVersion("7"), is(equalTo(new ModelVersion("7.0.0"))));
        Assert.assertThat(convertRHSSOVersionToKeycloakVersion("10"), is(equalTo(new ModelVersion("10.0.0"))));
    }
}
