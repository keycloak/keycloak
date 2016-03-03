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

package org.keycloak.testsuite.federation;

import java.net.URL;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.keycloak.events.Details;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.federation.ldap.LDAPFederationProviderFactory;
import org.keycloak.federation.ldap.kerberos.LDAPProviderKerberosConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.rule.KerberosRule;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.utils.CredentialHelper;

/**
 * Test of LDAPFederationProvider (Kerberos backed by LDAP)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosLdapTest extends AbstractKerberosTest {

    private static final String PROVIDER_CONFIG_LOCATION = "kerberos/kerberos-ldap-connection.properties";

    private static UserFederationProviderModel ldapModel = null;

    private static KerberosRule kerberosRule = new KerberosRule(PROVIDER_CONFIG_LOCATION);

    private static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            CredentialHelper.setAlternativeCredential(manager.getSession(), CredentialRepresentation.KERBEROS, appRealm);
            URL url = getClass().getResource("/kerberos-test/kerberos-app-keycloak.json");
            keycloakRule.createApplicationDeployment()
                    .name("kerberos-portal").contextPath("/kerberos-portal")
                    .servletClass(KerberosCredDelegServlet.class).adapterConfigPath(url.getPath())
                    .role("user").deployApplication();

            Map<String,String> ldapConfig = kerberosRule.getConfig();
            ldapModel = appRealm.addUserFederationProvider(LDAPFederationProviderFactory.PROVIDER_NAME, ldapConfig, 0, "kerberos-ldap", -1, -1, 0);
        }
    }) {

        @Override
        protected void importRealm() {
            server.importRealm(getClass().getResourceAsStream("/kerberos-test/kerberosrealm.json"));
        }

    };

    @ClassRule
    public static TestRule chain = RuleChain
            .outerRule(kerberosRule)
            .around(keycloakRule);

    @Rule
    public WebRule webRule = new WebRule(this);

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);

    @Override
    protected CommonKerberosConfig getKerberosConfig() {
        return new LDAPProviderKerberosConfig(ldapModel);
    }

    @Override
    protected KeycloakRule getKeycloakRule() {
        return keycloakRule;
    }

    @Override
    protected AssertEvents getAssertEvents() {
        return events;
    }


    @Test
    public void spnegoLoginTest() throws Exception {
        spnegoLoginTestImpl();

        // Assert user was imported and hasn't any required action on him. Profile info is synced from LDAP
        assertUser("hnelson", "hnelson@keycloak.org", "Horatio", "Nelson", false);
    }


    @Test
    public void writableEditModeTest() throws Exception {
        KeycloakRule keycloakRule = getKeycloakRule();
        AssertEvents events = getAssertEvents();

        // Change editMode to WRITABLE
        updateProviderEditMode(UserFederationProvider.EditMode.WRITABLE);

        // Login with username/password from kerberos
        changePasswordPage.open();
        // Only needed if you are providing a click thru to bypass kerberos.  Currently there is a javascript
        // to forward the user if kerberos isn't enabled.
        //bypassPage.isCurrent();
        //bypassPage.clickContinue();
        loginPage.assertCurrent();
        loginPage.login("jduke", "theduke");
        changePasswordPage.assertCurrent();

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
        loginPage.assertCurrent();
        loginPage.login("jduke", "newPass");
        changePasswordPage.assertCurrent();
        changePasswordPage.logout();

        // Assert SPNEGO login with the new password as mode is writable
        events.clear();
        Response spnegoResponse = spnegoLogin("jduke", "newPass");
        Assert.assertEquals(302, spnegoResponse.getStatus());
        events.expectLogin()
                .client("kerberos-app")
                .user(keycloakRule.getUser("test", "jduke").getId())
                .detail(Details.REDIRECT_URI, KERBEROS_APP_URL)
                //.detail(Details.AUTH_METHOD, "spnego")
                .detail(Details.USERNAME, "jduke")
                .assertEvent();

        // Change password back
        changePasswordPage.open();
        // Only needed if you are providing a click thru to bypass kerberos.  Currently there is a javascript
        // to forward the user if kerberos isn't enabled.
        //bypassPage.isCurrent();
        //bypassPage.clickContinue();

        loginPage.login("jduke", "newPass");
        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword("newPass", "theduke", "theduke");
        Assert.assertTrue(driver.getPageSource().contains("Your password has been updated."));
        changePasswordPage.logout();

        spnegoResponse.close();
        events.clear();
    }

}
