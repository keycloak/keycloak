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

package org.keycloak.testsuite.federation.kerberos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.models.AuthenticationFlowBindings;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.kerberos.LDAPProviderKerberosConfig;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.util.KerberosRule;
import org.keycloak.testsuite.KerberosEmbeddedServer;

/**
 * Test for the LDAPStorageProvider with kerberos enabled (kerberos with LDAP integration)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
public class KerberosLdapTest extends AbstractKerberosSingleRealmTest {

    private static final String PROVIDER_CONFIG_LOCATION = "classpath:kerberos/kerberos-ldap-connection.properties";

    @ClassRule
    public static KerberosRule kerberosRule = new KerberosRule(PROVIDER_CONFIG_LOCATION, KerberosEmbeddedServer.DEFAULT_KERBEROS_REALM);


    @Override
    protected KerberosRule getKerberosRule() {
        return kerberosRule;
    }


    @Override
    protected CommonKerberosConfig getKerberosConfig() {
        return new LDAPProviderKerberosConfig(getUserStorageConfiguration());
    }

    @Override
    protected ComponentRepresentation getUserStorageConfiguration() {
        return getUserStorageConfiguration("kerberos-ldap", LDAPStorageProviderFactory.PROVIDER_NAME);
    }



    @Test
    public void spnegoLoginTest() throws Exception {
        assertSuccessfulSpnegoLogin("hnelson", "hnelson", "secret");

        // Assert user was imported and hasn't any required action on him. Profile info is synced from LDAP
        assertUser("hnelson", "hnelson@keycloak.org", "Horatio", "Nelson", false);
    }

    @Test
    public void testClientOverrideFlowUsingBrowserHttpChallenge() throws Exception {
        List<AuthenticationExecutionInfoRepresentation> executions = testRealmResource().flows().getExecutions("http challenge");

        for (AuthenticationExecutionInfoRepresentation execution : executions) {
            if ("basic-auth".equals(execution.getProviderId())) {
                execution.setRequirement("ALTERNATIVE");
                testRealmResource().flows().updateExecutions("http challenge", execution);
            }
            if ("auth-spnego".equals(execution.getProviderId())) {
                execution.setRequirement("ALTERNATIVE");
                testRealmResource().flows().updateExecutions("http challenge", execution);
            }
        }


        Map<String, String> flows = new HashMap<>();
        AuthenticationFlowRepresentation flow = testRealmResource().flows().getFlows().stream().filter(flowRep -> flowRep.getAlias().equalsIgnoreCase("http challenge")).findAny().get();

        flows.put(AuthenticationFlowBindings.BROWSER_BINDING, flow.getId());

        ClientRepresentation client = testRealmResource().clients().findByClientId("kerberos-app-challenge").get(0);

        client.setAuthenticationFlowBindingOverrides(flows);

        testRealmResource().clients().get(client.getId()).update(client);

        assertSuccessfulSpnegoLogin(client.getClientId(),"hnelson", "hnelson", "secret");
    }

    @Test
    public void validatePasswordPolicyTest() throws Exception{
         updateProviderEditMode(UserStorageProvider.EditMode.WRITABLE);

         changePasswordPage.open();
         loginPage.login("jduke", "theduke");

         updateProviderValidatePasswordPolicy(true);
         changePasswordPage.changePassword("theduke", "jduke", "jduke");
         Assert.assertTrue(driver.getPageSource().contains("Invalid"));

         updateProviderValidatePasswordPolicy(false);
         changePasswordPage.changePassword("theduke", "jduke", "jduke");
         Assert.assertTrue(driver.getPageSource().contains("Your password has been updated."));

         // Change password back
         changePasswordPage.open();
         changePasswordPage.changePassword("jduke", "theduke", "theduke");
    }

    @Test
    public void writableEditModeTest() throws Exception {
        // Change editMode to WRITABLE
        updateProviderEditMode(UserStorageProvider.EditMode.WRITABLE);

        // Login with username/password from kerberos
        changePasswordPage.open();
        // Only needed if you are providing a click thru to bypass kerberos.  Currently there is a javascript
        // to forward the user if kerberos isn't enabled.
        //bypassPage.isCurrent();
        //bypassPage.clickContinue();
        loginPage.assertCurrent();
        loginPage.login("jduke", "theduke");
        Assert.assertTrue(changePasswordPage.isCurrent());

        // Successfully change password now
        changePasswordPage.changePassword("theduke", "newPass", "newPass");
        Assert.assertTrue(driver.getPageSource().contains("Your password has been updated."));
        changePasswordPage.logout();

        // Only needed if you are providing a click thru to bypass kerberos.  Currently there is a javascript
        // to forward the user if kerberos isn't enabled.
        //bypassPage.isCurrent();
        //bypassPage.clickContinue();

        // Login with old password doesn't work, but with new password works
        loginPage.login("jduke", "theduke");
        Assert.assertTrue(loginPage.isCurrent());
        loginPage.login("jduke", "newPass");
        changePasswordPage.assertCurrent();
        changePasswordPage.logout();

        // Assert SPNEGO login with the new password as mode is writable
        events.clear();
        Response spnegoResponse = spnegoLogin("jduke", "newPass");
        org.keycloak.testsuite.Assert.assertEquals(302, spnegoResponse.getStatus());
        org.keycloak.testsuite.Assert.assertEquals(302, spnegoResponse.getStatus());
        List<UserRepresentation> users = testRealmResource().users().search("jduke", 0, 1);
        String userId = users.get(0).getId();
        events.expectLogin()
                .client("kerberos-app")
                .user(userId)
                .detail(Details.USERNAME, "jduke")
                .assertEvent();

        String codeUrl = spnegoResponse.getLocation().toString();

        assertAuthenticationSuccess(codeUrl);

        // Change password back
        changePasswordPage.open();
        loginPage.login("jduke", "newPass");
        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword("newPass", "theduke", "theduke");
    }
}
