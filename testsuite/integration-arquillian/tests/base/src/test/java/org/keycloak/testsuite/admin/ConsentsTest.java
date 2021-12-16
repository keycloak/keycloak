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

package org.keycloak.testsuite.admin;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ConsentPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.AbstractTestRealmKeycloakTest.TEST_REALM_NAME;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.admin.ApiUtil.findClientByClientId;
import static org.keycloak.testsuite.admin.ApiUtil.resetUserPassword;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.OAuthClient.AccessTokenResponse;
import org.keycloak.testsuite.util.OAuthClient.AuthorizationEndpointResponse;
import org.openqa.selenium.By;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
public class ConsentsTest extends AbstractKeycloakTest {

    final static String REALM_PROV_NAME = "provider";
    final static String REALM_CONS_NAME = "consumer";

    final static String IDP_OIDC_ALIAS = "kc-oidc-idp";
    final static String IDP_OIDC_PROVIDER_ID = "keycloak-oidc";

    final static String CLIENT_ID = "brokerapp";
    final static String CLIENT_SECRET = "secret";

    final static String USER_LOGIN = "testuser";
    final static String USER_EMAIL = "user@localhost.com";
    final static String USER_PASSWORD = "password";
    final static String USER_FIRSTNAME = "User";
    final static String USER_LASTNAME = "Tester";

    protected RealmRepresentation createProviderRealm() {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(REALM_PROV_NAME);
        realm.setEnabled(true);

        return realm;
    }

    protected RealmRepresentation createConsumerRealm() {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(REALM_CONS_NAME);
        realm.setEnabled(true);

        return realm;
    }

    protected List<ClientRepresentation> createProviderClients() {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setName(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);
        client.setEnabled(true);
        client.setConsentRequired(true);

        client.setRedirectUris(Collections.singletonList(getAuthRoot() +
                "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_OIDC_ALIAS + "/endpoint/*"));

        client.setAdminUrl(getAuthRoot() +
                "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_OIDC_ALIAS + "/endpoint");

        return Collections.singletonList(client);
    }

    protected IdentityProviderRepresentation setUpIdentityProvider() {
        IdentityProviderRepresentation idp = createIdentityProvider(IDP_OIDC_ALIAS, IDP_OIDC_PROVIDER_ID);

        Map<String, String> config = idp.getConfig();

        config.put("clientId", CLIENT_ID);
        config.put("clientSecret", CLIENT_SECRET);
        config.put("prompt", "login");
        config.put("authorizationUrl", getAuthRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/auth");
        config.put("tokenUrl", getAuthRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/token");
        config.put("logoutUrl", getAuthRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/logout");
        config.put("userInfoUrl", getAuthRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/userinfo");
        config.put("defaultScope", "email profile");
        config.put("backchannelSupported", "true");

        return idp;
    }

    protected String getUserLogin() {
        return USER_LOGIN;
    }

    protected String getUserPassword() {
        return USER_PASSWORD;
    }

    protected String getUserEmail() {
        return USER_EMAIL;
    }

    protected String getUserFirstName() {
        return USER_FIRSTNAME;
    }

    protected String getUserLastName() {
        return USER_LASTNAME;
    }
    protected String providerRealmName() {
        return REALM_PROV_NAME;
    }

    protected String consumerRealmName() {
        return REALM_CONS_NAME;
    }

    protected String getIDPAlias() {
        return IDP_OIDC_ALIAS;
    }

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected LoginPage accountLoginPage;

    @Page
    protected ConsentPage consentPage;

    @Page
    protected AppPage appPage;

    @Page
    protected ErrorPage errorPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation providerRealm = createProviderRealm();
        RealmRepresentation consumerRealm = createConsumerRealm();
        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);

        testRealms.add(providerRealm);
        testRealms.add(consumerRealm);
        testRealms.add(realmRepresentation);
    }

    @Before
    public void createUser() {
        log.debug("creating user for realm " + providerRealmName());

        UserRepresentation user = new UserRepresentation();
        user.setUsername(getUserLogin());
        user.setEmail(getUserEmail());
        user.setFirstName(getUserFirstName());
        user.setLastName(getUserLastName());
        user.setEmailVerified(true);
        user.setEnabled(true);

        RealmResource realmResource = adminClient.realm(providerRealmName());
        String userId = createUserWithAdminClient(realmResource, user);

        resetUserPassword(realmResource.users().get(userId), getUserPassword(), false);
    }

    @Before
    public void addIdentityProviderToProviderRealm() {
        log.debug("adding identity provider to realm " + consumerRealmName());

        RealmResource realm = adminClient.realm(consumerRealmName());
        realm.identityProviders().create(setUpIdentityProvider());
    }

    @Before
    public void addClients() {
        List<ClientRepresentation> clients = createProviderClients();
        if (clients != null) {
            RealmResource providerRealm = adminClient.realm(providerRealmName());
            for (ClientRepresentation client : clients) {
                log.debug("adding client " + client.getName() + " to realm " + providerRealmName());

                providerRealm.clients().create(client);
            }
        }
    }

    protected String getAuthRoot() {
        return suiteContext.getAuthServerInfo().getContextRoot().toString();
    }

    protected IdentityProviderRepresentation createIdentityProvider(String alias, String providerId) {
        IdentityProviderRepresentation identityProviderRepresentation = new IdentityProviderRepresentation();

        identityProviderRepresentation.setAlias(alias);
        identityProviderRepresentation.setDisplayName(providerId);
        identityProviderRepresentation.setProviderId(providerId);
        identityProviderRepresentation.setEnabled(true);

        return identityProviderRepresentation;
    }

    private void waitForPage(String title) {
        long startAt = System.currentTimeMillis();

        while (!driver.getTitle().toLowerCase().contains(title)
                && System.currentTimeMillis() - startAt < 200) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException ignore) {}
        }
    }

    @After
    public void cleanUser() {
        String userId = adminClient.realm(providerRealmName()).users().search(getUserLogin()).get(0).getId();
        adminClient.realm(providerRealmName()).users().delete(userId);
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testConsents() {
        driver.navigate().to(getAccountUrl(consumerRealmName()));

        log.debug("Clicking social " + getIDPAlias());
        accountLoginPage.clickSocial(getIDPAlias());

        if (!driver.getCurrentUrl().contains("/auth/realms/" + providerRealmName() + "/")) {
            log.debug("Not on provider realm page, url: " + driver.getCurrentUrl());
        }

        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + providerRealmName() + "/"));

        log.debug("Logging in");
        accountLoginPage.login(getUserLogin(), getUserPassword());

        waitForPage("grant access");

        Assert.assertTrue(consentPage.isCurrent());
        consentPage.confirm();

        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + consumerRealmName() + "/"));

        UsersResource consumerUsers = adminClient.realm(consumerRealmName()).users();
        Assert.assertTrue("There must be at least one user", consumerUsers.count() > 0);

        List<UserRepresentation> users = consumerUsers.search("", 0, 5);

        UserRepresentation foundUser = null;
        for (UserRepresentation user : users) {
            if (user.getUsername().equals(getUserLogin()) && user.getEmail().equals(getUserEmail())) {
                foundUser = user;
                break;
            }
        }

        Assert.assertNotNull("There must be user " + getUserLogin() + " in realm " + consumerRealmName(),
                foundUser);

        // get user with the same username from provider realm
        RealmResource providerRealm = adminClient.realm(providerRealmName());
        users = providerRealm.users().search(null, foundUser.getFirstName(), foundUser.getLastName(), null, 0, 1);
        Assert.assertEquals("Same user should be in provider realm", 1, users.size());

        String userId = users.get(0).getId();
        UserResource userResource = providerRealm.users().get(userId);

        // list consents
        List<Map<String, Object>> consents = userResource.getConsents();
        Assert.assertEquals("There should be one consent", 1, consents.size());

        Map<String, Object> consent = consents.get(0);
        Assert.assertEquals("Consent should be given to " + CLIENT_ID, CLIENT_ID, consent.get("clientId"));

        // list sessions. Single client should be in user session
        List<UserSessionRepresentation> sessions = userResource.getUserSessions();
        Assert.assertEquals("There should be one active session", 1, sessions.size());
        Assert.assertEquals("There should be one client in user session", 1, sessions.get(0).getClients().size());

        // revoke consent
        userResource.revokeConsent(CLIENT_ID);

        // list consents
        consents = userResource.getConsents();
        Assert.assertEquals("There should be no consents", 0, consents.size());

        // list sessions
        sessions = userResource.getUserSessions();
        Assert.assertEquals("There should be one active session", 1, sessions.size());
        Assert.assertEquals("There should be no client in user session", 0, sessions.get(0).getClients().size());
    }

    /**
     * KEYCLOAK-18954
     */
    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testRetrieveConsentsForUserWithClientsWithGrantedOfflineAccess() throws Exception {

        RealmResource providerRealm = adminClient.realm(providerRealmName());

        RealmRepresentation providerRealmRep = providerRealm.toRepresentation();
        providerRealmRep.setAccountTheme("keycloak");
        providerRealm.update(providerRealmRep);

        ClientRepresentation providerAccountRep = providerRealm.clients().findByClientId("account").get(0);

        // add offline_scope to default account-console client scope
        ClientScopeRepresentation offlineAccessScope = providerRealm.getDefaultOptionalClientScopes().stream()
                .filter(csr -> csr.getName().equals(OAuth2Constants.OFFLINE_ACCESS)).findFirst().get();
        providerRealm.clients().get(providerAccountRep.getId()).removeOptionalClientScope(offlineAccessScope.getId());
        providerRealm.clients().get(providerAccountRep.getId()).addDefaultClientScope(offlineAccessScope.getId());

        // enable consent required to explicitly grant offline access
        providerAccountRep.setConsentRequired(true);
        providerAccountRep.setDirectAccessGrantsEnabled(true); // for offline token retrieval
        providerRealm.clients().get(providerAccountRep.getId()).update(providerAccountRep);

        List<UserRepresentation> searchResult = providerRealm.users().search(getUserLogin());
        UserRepresentation user = searchResult.get(0);

        driver.navigate().to(getAccountUrl(providerRealmName()));

        waitForPage("Sign in to provider");
        log.debug("Logging in");
        accountLoginPage.login(getUserLogin(), getUserPassword());

        waitForPage("grant access");
        log.debug("Grant consent for offline_access");
        Assert.assertTrue(consentPage.isCurrent());
        consentPage.confirm();

        waitForPage("keycloak account console");

        // disable consent required again to enable direct grant token retrieval.
        providerAccountRep.setConsentRequired(false);
        providerRealm.clients().get(providerAccountRep.getId()).update(providerAccountRep);

        log.debug("Obtain offline_token");
        OAuthClient.AccessTokenResponse response = oauth.realm(providerRealmRep.getRealm())
                .clientId(providerAccountRep.getClientId())
                .scope(OAuth2Constants.SCOPE_OPENID +" " + OAuth2Constants.SCOPE_PROFILE + " " + OAuth2Constants.OFFLINE_ACCESS)
                .doGrantAccessTokenRequest(null, getUserLogin(), getUserPassword());
        assertNotNull(response.getRefreshToken());

        log.debug("Check for Offline Token in consents");
        List<Map<String, Object>> consents = providerRealm.users().get(user.getId()).getConsents();
        assertFalse("Consents should not be empty", consents.isEmpty());

        assertTrue(consents.toString().contains("Offline Token"));
    }

    @Test
    public void testConsentCancel() {
        // setup account client to require consent
        RealmResource providerRealm = adminClient.realm(providerRealmName());
        ClientResource accountClient = findClientByClientId(providerRealm, "account");

        ClientRepresentation clientRepresentation = accountClient.toRepresentation();
        clientRepresentation.setConsentRequired(true);
        accountClient.update(clientRepresentation);

        // setup correct realm
        accountPage.setAuthRealm(providerRealmName());

        // navigate to account console and login
        accountPage.navigateTo();
        loginPage.form().login(getUserLogin(), getUserPassword());

        consentPage.assertCurrent();

        consentPage.cancel();

        // check an error page after cancelling the consent
        errorPage.assertCurrent();
        assertEquals("No access", errorPage.getError());

        // follow the link "back to application"
        errorPage.clickBackToApplication();

        loginPage.form().login(getUserLogin(), getUserPassword());
        consentPage.confirm();

        // successful login
        accountPage.assertCurrent();
    }

    @Test
    public void clientConsentRequiredAfterLogin() {
        oauth.realm(TEST_REALM_NAME).clientId("test-app");
        AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(response.getCode(), "password");

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        EventRepresentation loginEvent = events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
        String sessionId = loginEvent.getSessionId();

        ClientRepresentation clientRepresentation = adminClient.realm(TEST_REALM_NAME).clients().findByClientId("test-app").get(0);
        try {
            clientRepresentation.setConsentRequired(true);
            adminClient.realm(TEST_REALM_NAME).clients().get(clientRepresentation.getId()).update(clientRepresentation);

            events.clear();

            // try to refresh the token
            // this fails as client no longer has requested consent from user
            AccessTokenResponse refreshTokenResponse = oauth.doRefreshTokenRequest(accessTokenResponse.getRefreshToken(), "password");
            Assert.assertEquals(OAuthErrorException.INVALID_SCOPE, refreshTokenResponse.getError());
            Assert.assertEquals("Client no longer has requested consent from user", refreshTokenResponse.getErrorDescription());

            events.expectRefresh(accessTokenResponse.getRefreshToken(), sessionId).clearDetails().error(Errors.INVALID_TOKEN).assertEvent();
        } finally {
            clientRepresentation.setConsentRequired(false);
            adminClient.realm(TEST_REALM_NAME).clients().get(clientRepresentation.getId()).update(clientRepresentation);
        }
    }

    @Test
    public void testConsentWithAdditionalClientAttributes() {
        // setup account client to require consent
        RealmResource providerRealm = adminClient.realm(providerRealmName());
        ClientResource accountClient = findClientByClientId(providerRealm, "account");

        ClientRepresentation clientRepresentation = accountClient.toRepresentation();
        clientRepresentation.setConsentRequired(true);
        clientRepresentation.getAttributes().put(ClientModel.LOGO_URI,"https://www.keycloak.org/resources/images/keycloak_logo_480x108.png");
        clientRepresentation.getAttributes().put(ClientModel.POLICY_URI,"https://www.keycloak.org/policy");
        clientRepresentation.getAttributes().put(ClientModel.TOS_URI,"https://www.keycloak.org/tos");
        accountClient.update(clientRepresentation);

        // setup correct realm
        accountPage.setAuthRealm(providerRealmName());

        // navigate to account console and login
        accountPage.navigateTo();
        loginPage.form().login(getUserLogin(), getUserPassword());

        consentPage.assertCurrent();
        assertTrue("logoUri must be presented", driver.findElement(By.xpath("//img[@src='https://www.keycloak.org/resources/images/keycloak_logo_480x108.png']")).isDisplayed());
        assertTrue("policyUri must be presented", driver.findElement(By.xpath("//a[@href='https://www.keycloak.org/policy']")).isDisplayed());
        assertTrue("tosUri must be presented", driver.findElement(By.xpath("//a[@href='https://www.keycloak.org/tos']")).isDisplayed());

        consentPage.confirm();

        // successful login
        accountPage.assertCurrent();
    }

    private String getAccountUrl(String realmName) {
        return getAuthRoot() + "/auth/realms/" + realmName + "/account";
    }
}
