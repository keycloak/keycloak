/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.federation.kerberos;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;

/**
 * Tests for CommonKerberosConfig trimming functionality.
 */
public class CommonKerberosConfigTest {

    @Test
    public void testServerPrincipalIsTrimmed() {
        ComponentModel componentModel = new ComponentModel();
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.add(KerberosConstants.SERVER_PRINCIPAL, "  myPrincipal  ");
        componentModel.setConfig(config);

        TestKerberosConfig kerberosConfig = new TestKerberosConfig(componentModel);
        Assert.assertEquals("myPrincipal", kerberosConfig.getServerPrincipal());
    }

    @Test
    public void testKerberosRealmIsTrimmed() {
        ComponentModel componentModel = new ComponentModel();
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.add(KerberosConstants.KERBEROS_REALM, "  MYREALM.COM  ");
        componentModel.setConfig(config);

        TestKerberosConfig kerberosConfig = new TestKerberosConfig(componentModel);
        Assert.assertEquals("MYREALM.COM", kerberosConfig.getKerberosRealm());
    }

    @Test
    public void testKeyTabIsTrimmed() {
        ComponentModel componentModel = new ComponentModel();
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.add(KerberosConstants.KEYTAB, "  /path/to/keytab  ");
        componentModel.setConfig(config);

        TestKerberosConfig kerberosConfig = new TestKerberosConfig(componentModel);
        Assert.assertEquals("/path/to/keytab", kerberosConfig.getKeyTab());
    }

    @Test
    public void testNullValuesAreHandled() {
        ComponentModel componentModel = new ComponentModel();
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        componentModel.setConfig(config);

        TestKerberosConfig kerberosConfig = new TestKerberosConfig(componentModel);
        Assert.assertNull(kerberosConfig.getServerPrincipal());
        Assert.assertNull(kerberosConfig.getKerberosRealm());
        Assert.assertNull(kerberosConfig.getKeyTab());
    }

    @Test
    public void testEmptyValuesAreHandled() {
        ComponentModel componentModel = new ComponentModel();
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.add(KerberosConstants.SERVER_PRINCIPAL, "");
        config.add(KerberosConstants.KERBEROS_REALM, "");
        config.add(KerberosConstants.KEYTAB, "");
        componentModel.setConfig(config);

        TestKerberosConfig kerberosConfig = new TestKerberosConfig(componentModel);
        Assert.assertEquals("", kerberosConfig.getServerPrincipal());
        Assert.assertEquals("", kerberosConfig.getKerberosRealm());
        Assert.assertEquals("", kerberosConfig.getKeyTab());
    }

    /**
     * Test implementation of CommonKerberosConfig for testing purposes.
     */
    private static class TestKerberosConfig extends CommonKerberosConfig {
        public TestKerberosConfig(ComponentModel componentModel) {
            super(componentModel);
        }
    }
} 