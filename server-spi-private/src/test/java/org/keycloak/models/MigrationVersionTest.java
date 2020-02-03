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
        Assert.assertEquals(1, version_100Beta1.getMajor());
        Assert.assertEquals(0, version_100Beta1.getMinor());
        Assert.assertEquals(0, version_100Beta1.getMicro());

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
        Assert.assertEquals(2, version_211CR1.getMajor());
        Assert.assertEquals(1, version_211CR1.getMinor());
        Assert.assertEquals(1, version_211CR1.getMicro());
        Assert.assertEquals("CR1", version_211CR1.getQualifier());

        ModelVersion version_211 = new ModelVersion("2.1.1");

        ModelVersion version50Snapshot = new ModelVersion("5.0.0-SNAPSHOT");
        Assert.assertEquals(5, version50Snapshot.getMajor());
        Assert.assertEquals(0, version50Snapshot.getMinor());
        Assert.assertEquals(0, version50Snapshot.getMicro());
        Assert.assertNull(version50Snapshot.getQualifier());

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

        ModelVersion versionPipeline = new ModelVersion("8.0.2-REL-20200130-143126");
        Assert.assertEquals(8, versionPipeline.getMajor());
        Assert.assertEquals(0, versionPipeline.getMinor());
        Assert.assertEquals(2, versionPipeline.getMicro());
        Assert.assertNull(versionPipeline.getQualifier());

        ModelVersion versionPipeline2 = new ModelVersion("9.1.2-SNAPSHOT-stage-20191125-003440");
        Assert.assertEquals(9, versionPipeline2.getMajor());
        Assert.assertEquals(1, versionPipeline2.getMinor());
        Assert.assertEquals(2, versionPipeline2.getMicro());
        Assert.assertNull(versionPipeline2.getQualifier());

        ModelVersion versionProduct = new ModelVersion("7.0.0.redhat-00002");
        Assert.assertEquals(7, versionProduct.getMajor());
        Assert.assertEquals(0, versionProduct.getMinor());
        Assert.assertEquals(0, versionProduct.getMicro());
        Assert.assertNull(versionProduct.getQualifier());
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
