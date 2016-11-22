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

package org.keycloak.testsuite.adapter.federation;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.federation.kerberos.KerberosConfig;
import org.keycloak.federation.kerberos.KerberosFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Map;

/**
 * Test of KerberosFederationProvider (Kerberos not backed by LDAP)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractKerberosStandaloneAdapterTest extends AbstractKerberosAdapterTest {

    public static MultivaluedHashMap<String, String> toComponentConfig(Map<String, String> ldapConfig) {
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        for (Map.Entry<String, String> entry : ldapConfig.entrySet()) {
            config.add(entry.getKey(), entry.getValue());

        }
        return config;
    }

    protected static final String PROVIDER_CONFIG_LOCATION = "kerberos-standalone-connection.properties";

    @Before
    public void init() throws Exception{
        RealmRepresentation realmRepresentation = testRealmResource().toRepresentation();
        Map<String,String> ldapConfig = getConfig();
        ComponentRepresentation component = new ComponentRepresentation();
        component.setName("kerberos-standalone");
        component.setParentId(realmRepresentation.getId());
        component.setProviderId(KerberosFederationProviderFactory.PROVIDER_NAME);
        component.setProviderType(UserStorageProvider.class.getName());
        component.setConfig(toComponentConfig(ldapConfig));
        component.getConfig().putSingle("priority", "0");

        testRealmResource().components().add(component);
        realmRepresentation.setEventsEnabled(true);
        testRealmResource().update(realmRepresentation);        
    }
    
    @Override
    protected CommonKerberosConfig getKerberosConfig(UserStorageProviderModel model) {
        return new KerberosConfig(model);
    }
    
    @Override
    protected String getConnectionPropertiesLocation() {
        return PROVIDER_CONFIG_LOCATION;
    }

    @Test
    public void spnegoLoginTest() throws Exception {
        spnegoLoginTestImpl();
        // Assert user was imported and hasn't any required action on him
        assertUser("hnelson", "hnelson@" + getConfig().get(KerberosConstants.KERBEROS_REALM).toLowerCase(), null, null, false);
    }

    @Test
    @Override
    public void usernamePasswordLoginTest() throws Exception {
        super.usernamePasswordLoginTest();
    }

    @Test
    public void updateProfileEnabledTest() throws Exception {
        // Switch updateProfileOnFirstLogin to on
        RealmRepresentation realm = testRealmResource().toRepresentation();
        UserFederationProviderRepresentation kerberosProviderRepresentation = realm.getUserFederationProviders().get(0);
        kerberosProviderRepresentation.getConfig().put(KerberosConstants.UPDATE_PROFILE_FIRST_LOGIN, "true");
        testRealmResource().update(realm);

        // Assert update profile page is displayed
        Response spnegoResponse = spnegoLogin("hnelson", "secret");
        Assert.assertEquals(200, spnegoResponse.getStatus());
        String responseText = spnegoResponse.readEntity(String.class);
        Assert.assertTrue(responseText.contains("You need to update your user profile to activate your account."));
        Assert.assertTrue(responseText.contains("hnelson@" + getConfig().get(KerberosConstants.KERBEROS_REALM).toLowerCase()));
        spnegoResponse.close();

        // Assert user was imported and has required action on him
        assertUser("hnelson", "hnelson@" + getConfig().get(KerberosConstants.KERBEROS_REALM).toLowerCase(), null, null, true);

        // Switch updateProfileOnFirstLogin to off

        kerberosProviderRepresentation = realm.getUserFederationProviders().get(0);
        kerberosProviderRepresentation.getConfig().put(KerberosConstants.UPDATE_PROFILE_FIRST_LOGIN, "false");
        testRealmResource().update(realm);
    }
}
