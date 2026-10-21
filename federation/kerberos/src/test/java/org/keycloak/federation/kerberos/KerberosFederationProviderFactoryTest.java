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

import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for KerberosFederationProviderFactory validation functionality.
 */
public class KerberosFederationProviderFactoryTest {

    @Test
    public void testValidateConfigurationTrimsValues() throws Exception {
        KerberosFederationProviderFactory factory = new KerberosFederationProviderFactory();
        
        ComponentModel config = new ComponentModel();
        MultivaluedHashMap<String, String> configMap = new MultivaluedHashMap<>();
        configMap.add(KerberosConstants.SERVER_PRINCIPAL, "  myPrincipal  ");
        configMap.add(KerberosConstants.KERBEROS_REALM, "  MYREALM.COM  ");
        configMap.add(KerberosConstants.KEYTAB, "  /path/to/keytab  ");
        config.setConfig(configMap);

        // Mock session and realm (not used in current implementation)
        KeycloakSession session = null;
        RealmModel realm = null;

        // Call validateConfiguration
        factory.validateConfiguration(session, realm, config);

        // Verify values are trimmed
        Assert.assertEquals("myPrincipal", config.getConfig().getFirst(KerberosConstants.SERVER_PRINCIPAL));
        Assert.assertEquals("MYREALM.COM", config.getConfig().getFirst(KerberosConstants.KERBEROS_REALM));
        Assert.assertEquals("/path/to/keytab", config.getConfig().getFirst(KerberosConstants.KEYTAB));
    }

    @Test
    public void testValidateConfigurationHandlesNullValues() throws Exception {
        KerberosFederationProviderFactory factory = new KerberosFederationProviderFactory();
        
        ComponentModel config = new ComponentModel();
        MultivaluedHashMap<String, String> configMap = new MultivaluedHashMap<>();
        config.setConfig(configMap);

        // Mock session and realm (not used in current implementation)
        KeycloakSession session = null;
        RealmModel realm = null;

        // Call validateConfiguration - should not throw exception
        factory.validateConfiguration(session, realm, config);

        // Verify null values remain null
        Assert.assertNull(config.getConfig().getFirst(KerberosConstants.SERVER_PRINCIPAL));
        Assert.assertNull(config.getConfig().getFirst(KerberosConstants.KERBEROS_REALM));
        Assert.assertNull(config.getConfig().getFirst(KerberosConstants.KEYTAB));
    }

    @Test
    public void testValidateConfigurationHandlesAlreadyTrimmedValues() throws Exception {
        KerberosFederationProviderFactory factory = new KerberosFederationProviderFactory();
        
        ComponentModel config = new ComponentModel();
        MultivaluedHashMap<String, String> configMap = new MultivaluedHashMap<>();
        configMap.add(KerberosConstants.SERVER_PRINCIPAL, "myPrincipal");
        configMap.add(KerberosConstants.KERBEROS_REALM, "MYREALM.COM");
        configMap.add(KerberosConstants.KEYTAB, "/path/to/keytab");
        config.setConfig(configMap);

        // Mock session and realm (not used in current implementation)
        KeycloakSession session = null;
        RealmModel realm = null;

        // Call validateConfiguration
        factory.validateConfiguration(session, realm, config);

        // Verify values remain unchanged
        Assert.assertEquals("myPrincipal", config.getConfig().getFirst(KerberosConstants.SERVER_PRINCIPAL));
        Assert.assertEquals("MYREALM.COM", config.getConfig().getFirst(KerberosConstants.KERBEROS_REALM));
        Assert.assertEquals("/path/to/keytab", config.getConfig().getFirst(KerberosConstants.KEYTAB));
    }
} 