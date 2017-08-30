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

package org.keycloak.testsuite.model;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.migration.ModelVersion;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MigrationVersionTest {

    @Test
    public void testVersion() {
        ModelVersion version_100Beta1 = new ModelVersion("1.0.0.Beta1");
        Assert.assertEquals(version_100Beta1.getMajor(), 1);
        Assert.assertEquals(version_100Beta1.getMinor(), 0);
        Assert.assertEquals(version_100Beta1.getMicro(), 0);
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
        ModelVersion version_211 = new ModelVersion("2.1.1");

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

    }
}
