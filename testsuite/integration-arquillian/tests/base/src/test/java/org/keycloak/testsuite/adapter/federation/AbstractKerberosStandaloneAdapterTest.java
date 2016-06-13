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

import java.util.Arrays;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.federation.kerberos.KerberosConfig;
import org.keycloak.federation.kerberos.KerberosFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

/**
 * Test of KerberosFederationProvider (Kerberos not backed by LDAP)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractKerberosStandaloneAdapterTest extends AbstractKerberosAdapterTest {

    protected static final String PROVIDER_CONFIG_LOCATION = "kerberos-standalone-connection.properties";

    @Before
    public void init() throws Exception{
        Map<String,String> ldapConfig = getConfig();
        UserFederationProviderRepresentation userFederationProviderRepresentation = new UserFederationProviderRepresentation();
        userFederationProviderRepresentation.setProviderName(KerberosFederationProviderFactory.PROVIDER_NAME);
        userFederationProviderRepresentation.setConfig(ldapConfig);
        userFederationProviderRepresentation.setPriority(0);
        userFederationProviderRepresentation.setDisplayName("kerberos-standalone");
        userFederationProviderRepresentation.setFullSyncPeriod(-1);
        userFederationProviderRepresentation.setChangedSyncPeriod(-1);
        userFederationProviderRepresentation.setLastSync(0);
        
        RealmRepresentation realmRepresentation = testRealmResource().toRepresentation();
        realmRepresentation.setUserFederationProviders(Arrays.asList(userFederationProviderRepresentation));
        realmRepresentation.setEventsEnabled(true); 
        testRealmResource().update(realmRepresentation);        
    }
    
    @Override
    protected CommonKerberosConfig getKerberosConfig(UserFederationProviderModel model) {
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
