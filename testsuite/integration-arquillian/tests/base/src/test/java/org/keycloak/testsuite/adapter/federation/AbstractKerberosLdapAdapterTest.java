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
import org.keycloak.events.Details;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.federation.ldap.LDAPFederationProviderFactory;
import org.keycloak.federation.ldap.kerberos.LDAPProviderKerberosConfig;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;

/**
 * Test of LDAPFederationProvider (Kerberos backed by LDAP)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractKerberosLdapAdapterTest extends AbstractKerberosAdapterTest {

    private static final String PROVIDER_CONFIG_LOCATION = "kerberos-ldap-connection.properties";
    
    @Before
    public void init() throws Exception{
        Map<String,String> ldapConfig = getConfig();
        UserFederationProviderRepresentation userFederationProviderRepresentation = new UserFederationProviderRepresentation();
        userFederationProviderRepresentation.setProviderName(LDAPFederationProviderFactory.PROVIDER_NAME);
        userFederationProviderRepresentation.setConfig(ldapConfig);
        userFederationProviderRepresentation.setPriority(0);
        userFederationProviderRepresentation.setDisplayName("kerberos-ldap");
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
        return new LDAPProviderKerberosConfig(model);
    }

    @Test
    public void spnegoLoginTest() throws Exception {
        spnegoLoginTestImpl();
        // Assert user was imported and hasn't any required action on him. Profile info is synced from LDAP
        assertUser("hnelson", "hnelson@keycloak.org", "Horatio", "Nelson", false);
    }

    @Test
    public void writableEditModeTest() throws Exception {

        // Change editMode to WRITABLE
        updateProviderEditMode(UserFederationProvider.EditMode.WRITABLE);

        // Login with username/password from kerberos
        changePasswordPage.navigateTo();
        loginPage.isCurrent();
        loginPage.form().login("jduke", "theduke");
        changePasswordPage.isCurrent();

        // Successfully change password now
        changePasswordPage.changePasswords("theduke", "newPass", "newPass");
        Assert.assertTrue(driver.getPageSource().contains("Your password has been updated."));
        changePasswordPage.logOut();

        // Login with old password doesn't work, but with new password works
        loginPage.form().login("jduke", "theduke");
        loginPage.isCurrent();
        loginPage.form().login("jduke", "newPass");
        changePasswordPage.isCurrent();
        changePasswordPage.logOut();

        // Assert SPNEGO login with the new password as mode is writable
        events.clear();
        Response spnegoResponse = spnegoLogin("jduke", "newPass");
        Assert.assertEquals(302, spnegoResponse.getStatus());
        UserRepresentation user = ApiUtil.findUserByUsername(testRealmResource(), "jduke");
        events.expectLogin()
                .client("kerberos-app")
                .user(user != null ? user.getId() : null)
                .detail(Details.REDIRECT_URI, kerberosPortal.toString())
                //.detail(Details.AUTH_METHOD, "spnego")
                .detail(Details.USERNAME, "jduke")
                .assertEvent();

        // Change password back
        changePasswordPage.navigateTo();;

        loginPage.form().login("jduke", "newPass");
        changePasswordPage.isCurrent();
        changePasswordPage.changePasswords("newPass", "theduke", "theduke");
        Assert.assertTrue(driver.getPageSource().contains("Your password has been updated."));
        changePasswordPage.logOut();

        spnegoResponse.close();
        events.clear();
    }
    
    protected String getConnectionPropertiesLocation() {
        return PROVIDER_CONFIG_LOCATION;
    }
}
