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

import java.security.Principal;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.authentication.authenticators.browser.SpnegoAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AccountPasswordPage;
import org.keycloak.testsuite.pages.BypassKerberosPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.openqa.selenium.WebDriver;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractKerberosTest {

    protected String KERBEROS_APP_URL = "http://localhost:8081/kerberos-portal";

    protected KeycloakSPNegoSchemeFactory spnegoSchemeFactory;
    protected ResteasyClient client;

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected BypassKerberosPage bypassPage;

    @WebResource
    protected AccountPasswordPage changePasswordPage;

    protected abstract CommonKerberosConfig getKerberosConfig();
    protected abstract KeycloakRule getKeycloakRule();
    protected abstract AssertEvents getAssertEvents();

    @Before
    public void before() {
        CommonKerberosConfig kerberosConfig = getKerberosConfig();
        spnegoSchemeFactory = new KeycloakSPNegoSchemeFactory(kerberosConfig);
        initHttpClient(true);
        removeAllUsers();
    }

    @After
    public void after() {
        client.close();
        client = null;
    }


    @Test
    public void spnegoNotAvailableTest() throws Exception {
        initHttpClient(false);

        SpnegoAuthenticator.bypassChallengeJavascript = true;
        driver.navigate().to(KERBEROS_APP_URL);
        String kcLoginPageLocation = driver.getCurrentUrl();

        Response response = client.target(kcLoginPageLocation).request().get();
        Assert.assertEquals(401, response.getStatus());
        Assert.assertEquals(KerberosConstants.NEGOTIATE, response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE));
        String responseText = response.readEntity(String.class);
        responseText.contains("Log in to test");
        response.close();
        SpnegoAuthenticator.bypassChallengeJavascript = false;
    }


    protected void spnegoLoginTestImpl() throws Exception {
        KeycloakRule keycloakRule = getKeycloakRule();
        AssertEvents events = getAssertEvents();

        Response spnegoResponse = spnegoLogin("hnelson", "secret");
        Assert.assertEquals(302, spnegoResponse.getStatus());

        events.expectLogin()
                .client("kerberos-app")
                .user(keycloakRule.getUser("test", "hnelson").getId())
                .detail(Details.REDIRECT_URI, KERBEROS_APP_URL)
                //.detail(Details.AUTH_METHOD, "spnego")
                .detail(Details.USERNAME, "hnelson")
                .assertEvent();

        String location = spnegoResponse.getLocation().toString();
        driver.navigate().to(location);

        String pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains("Kerberos Test") && pageSource.contains("Kerberos servlet secured content"));

        spnegoResponse.close();
        events.clear();
    }

    // KEYCLOAK-2102
    @Test
    public void spnegoCaseInsensitiveTest() throws Exception {
        KeycloakRule keycloakRule = getKeycloakRule();
        AssertEvents events = getAssertEvents();

        Response spnegoResponse = spnegoLogin("MyDuke", "theduke");
        Assert.assertEquals(302, spnegoResponse.getStatus());

        events.expectLogin()
                .client("kerberos-app")
                .user(keycloakRule.getUser("test", "myduke").getId())
                .detail(Details.REDIRECT_URI, KERBEROS_APP_URL)
                        //.detail(Details.AUTH_METHOD, "spnego")
                .detail(Details.USERNAME, "myduke")
                .assertEvent();

        String location = spnegoResponse.getLocation().toString();
        driver.navigate().to(location);

        String pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains("Kerberos Test") && pageSource.contains("Kerberos servlet secured content"));

        spnegoResponse.close();
        events.clear();
    }


    @Test
    public void usernamePasswordLoginTest() throws Exception {
        KeycloakRule keycloakRule = getKeycloakRule();
        AssertEvents events = getAssertEvents();

        // Change editMode to READ_ONLY
        updateProviderEditMode(UserFederationProvider.EditMode.READ_ONLY);

        // Login with username/password from kerberos
        changePasswordPage.open();
        // Only needed if you are providing a click thru to bypass kerberos.  Currently there is a javascript
        // to forward the user if kerberos isn't enabled.
        //bypassPage.isCurrent();
        //bypassPage.clickContinue();
        loginPage.assertCurrent();
        loginPage.login("jduke", "theduke");
        changePasswordPage.assertCurrent();

        // Bad existing password
        changePasswordPage.changePassword("theduke-invalid", "newPass", "newPass");
        Assert.assertTrue(driver.getPageSource().contains("Invalid existing password."));

        // Change password is not possible as editMode is READ_ONLY
        changePasswordPage.changePassword("theduke", "newPass", "newPass");
        Assert.assertTrue(driver.getPageSource().contains("You can't update your password as your account is read only"));

        // Change editMode to UNSYNCED
        updateProviderEditMode(UserFederationProvider.EditMode.UNSYNCED);

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

       // Assert SPNEGO login still with the old password as mode is unsynced
        events.clear();
        Response spnegoResponse = spnegoLogin("jduke", "theduke");
        Assert.assertEquals(302, spnegoResponse.getStatus());
        String redirect = spnegoResponse.getLocation().toString();
        events.expectLogin()
                .client("kerberos-app")
                .user(keycloakRule.getUser("test", "jduke").getId())
                .detail(Details.REDIRECT_URI, KERBEROS_APP_URL)
                //.detail(Details.AUTH_METHOD, "spnego")
                .detail(Details.USERNAME, "jduke")
                .assertEvent();
        spnegoResponse.close();
    }

    @Test
    public void credentialDelegationTest() throws Exception {
        // Add kerberos delegation credential mapper
        getKeycloakRule().update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                ProtocolMapperModel protocolMapper = UserSessionNoteMapper.createClaimMapper(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME,
                        KerberosConstants.GSS_DELEGATION_CREDENTIAL,
                        KerberosConstants.GSS_DELEGATION_CREDENTIAL, "String",
                        true, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME,
                        true, false);

                ClientModel kerberosApp = appRealm.getClientByClientId("kerberos-app");
                kerberosApp.addProtocolMapper(protocolMapper);
            }

        });

        // SPNEGO login
        spnegoLoginTestImpl();

        // Assert servlet authenticated to LDAP with delegated credential
        driver.navigate().to(KERBEROS_APP_URL + KerberosCredDelegServlet.CRED_DELEG_TEST_PATH);
        String pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains("LDAP Data: Horatio Nelson"));

        // Remove kerberos delegation credential mapper
        getKeycloakRule().update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                ClientModel kerberosApp = appRealm.getClientByClientId("kerberos-app");
                ProtocolMapperModel toRemove = kerberosApp.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME);
                kerberosApp.removeProtocolMapper(toRemove);
            }

        });

        // Clear driver and login again. I can't invoke LDAP now as GSS Credential is not in accessToken
        driver.manage().deleteAllCookies();
        spnegoLoginTestImpl();
        driver.navigate().to(KERBEROS_APP_URL + KerberosCredDelegServlet.CRED_DELEG_TEST_PATH);
        pageSource = driver.getPageSource();
        Assert.assertFalse(pageSource.contains("LDAP Data: Horatio Nelson"));
        Assert.assertTrue(pageSource.contains("LDAP Data: ERROR"));
    }



    protected Response spnegoLogin(String username, String password) {
        SpnegoAuthenticator.bypassChallengeJavascript = true;
        driver.navigate().to(KERBEROS_APP_URL);
        String kcLoginPageLocation = driver.getCurrentUrl();

        // Request for SPNEGO login sent with Resteasy client
        spnegoSchemeFactory.setCredentials(username, password);
        Response response = client.target(kcLoginPageLocation).request().get();
        SpnegoAuthenticator.bypassChallengeJavascript = false;
        if (response.getStatus() == 302) {
            if (response.getLocation() == null) return response;
            String uri = response.getLocation().toString();
            if (uri.contains("login-actions/required-action")) {
                response = client.target(uri).request().get();
            }
        }
        return response;

    }


    protected void initHttpClient(boolean useSpnego) {
        if (client != null) {
            after();
        }

        DefaultHttpClient httpClient = (DefaultHttpClient) new HttpClientBuilder().build();
        httpClient.getAuthSchemes().register(AuthPolicy.SPNEGO, spnegoSchemeFactory);

        if (useSpnego) {
            Credentials fake = new Credentials() {

                public String getPassword() {
                    return null;
                }

                public Principal getUserPrincipal() {
                    return null;
                }

            };

            httpClient.getCredentialsProvider().setCredentials(
                    new AuthScope(null, -1, null),
                    fake);
        }

        ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient);
        client = new ResteasyClientBuilder().httpEngine(engine).build();
    }


    protected void removeAllUsers() {
        KeycloakRule keycloakRule = getKeycloakRule();

        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);

            RealmModel appRealm = manager.getRealm("test");
            List<UserModel> users = session.userStorage().getUsers(appRealm, true);
            for (UserModel user : users) {
                if (!user.getUsername().equals(AssertEvents.DEFAULT_USERNAME)) {
                    session.userStorage().removeUser(appRealm, user);
                }
            }

            Assert.assertEquals(1, session.userStorage().getUsers(appRealm, true).size());
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }


    protected void assertUser(String expectedUsername, String expectedEmail, String expectedFirstname, String expectedLastname, boolean updateProfileActionExpected) {
        KeycloakRule keycloakRule = getKeycloakRule();

        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);
            RealmModel appRealm = manager.getRealm("test");

            UserModel user = session.users().getUserByUsername(expectedUsername, appRealm);
            Assert.assertNotNull(user);
            Assert.assertEquals(user.getEmail(), expectedEmail);
            Assert.assertEquals(user.getFirstName(), expectedFirstname);
            Assert.assertEquals(user.getLastName(), expectedLastname);

            if (updateProfileActionExpected) {
                Assert.assertEquals(UserModel.RequiredAction.UPDATE_PROFILE.toString(), user.getRequiredActions().iterator().next());
            } else {
                Assert.assertTrue(user.getRequiredActions().isEmpty());
            }
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }


    protected void updateProviderEditMode(UserFederationProvider.EditMode editMode) {
        KeycloakRule keycloakRule = getKeycloakRule();

        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel realm = session.realms().getRealm("test");
            UserFederationProviderModel kerberosProviderModel = realm.getUserFederationProviders().get(0);
            kerberosProviderModel.getConfig().put(LDAPConstants.EDIT_MODE, editMode.toString());
            realm.updateUserFederationProvider(kerberosProviderModel);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }
}
