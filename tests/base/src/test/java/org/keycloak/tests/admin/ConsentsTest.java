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

package org.keycloak.tests.admin;

import java.util.List;
import java.util.Map;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventMatchers;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ConsentPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.hamcrest.MatcherAssert;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import static org.keycloak.tests.utils.admin.AdminApiUtil.findClientByClientId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@KeycloakIntegrationTest
public class ConsentsTest {

    @InjectRealm(ref = "user")
    ManagedRealm userRealm;

    @InjectRealm(ref = "consumer", config = ConsumerRealmConf.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm consumerRealm;

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD)
    ManagedRealm providerRealm;

    @InjectUser(ref = "user", realmRef = "user", config = UserRealmUserConf.class)
    ManagedUser userFromUserRealm;

    @InjectUser(ref = "provider", realmRef = "provider", config = ProviderRealmUserConf.class)
    ManagedUser userFromProviderRealm;

    @InjectClient(realmRef = "provider", config = ProviderRealmClientConf.class)
    ManagedClient providerRealmClient;

    @InjectOAuthClient(ref = "user", realmRef = "user")
    OAuthClient userRealmOAuth;

    @InjectOAuthClient(ref = "consumer", realmRef = "consumer")
    OAuthClient consumerRealmOAuth;

    @InjectOAuthClient(ref = "provider", realmRef = "provider")
    OAuthClient providerRealmOAuth;

    @InjectEvents(realmRef = "user")
    Events userRealmEvents;

    @InjectWebDriver
    WebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    ConsentPage consentPage;

    private static final Logger LOGGER = Logger.getLogger(ConsentsTest.class);

    private static final String REALM_PROV_NAME = "provider";
    private static final String REALM_CONS_NAME = "consumer";

    private static final String IDP_OIDC_ALIAS = "kc-oidc-idp";
    private static final String IDP_OIDC_PROVIDER_ID = "keycloak-oidc";

    private static final String CLIENT_ID = "brokerapp";
    private static final String CLIENT_SECRET = "secret";

    @Test
    public void testConsents() {
        consumerRealmOAuth.openLoginForm();

        LOGGER.debug("Clicking social " + IDP_OIDC_ALIAS);
        loginPage.clickSocial(IDP_OIDC_ALIAS);

        if (!driver.getCurrentUrl().contains("/realms/" + providerRealm.getName() + "/")) {
            LOGGER.debug("Not on provider realm page, url: " + driver.getCurrentUrl());
        }

        Assertions.assertTrue(driver.getCurrentUrl().contains("/realms/" + providerRealm.getName() + "/"), "Driver should be on the provider realm page right now");

        LOGGER.debug("Logging in");
        loginPage.fillLogin(userFromProviderRealm.getUsername(), userFromProviderRealm.getPassword());
        loginPage.submit();

        consentPage.waitForPage();
        consentPage.assertCurrent();
        consentPage.confirm();

        assertTrue(driver.getPageSource().contains("Happy days"), "Test user should be successfully logged in.");

        UsersResource consumerUsers = consumerRealm.admin().users();
        Assertions.assertTrue(consumerUsers.count() > 0, "There must be at least one user");

        List<UserRepresentation> users = consumerUsers.search("", 0, 5);

        UserRepresentation foundUser = null;
        for (UserRepresentation userRep : users) {
            if (userRep.getUsername().equals(userFromProviderRealm.getUsername()) && userRep.getEmail().equals(userFromProviderRealm.admin().toRepresentation().getEmail())) {
                foundUser = userRep;
                break;
            }
        }

        Assertions.assertNotNull(foundUser, "There must be user " + userFromProviderRealm.getUsername() + " in realm " + consumerRealm.getName());

        // get user with the same username from provider realm
        users = providerRealm.admin().users().search(null, foundUser.getFirstName(), foundUser.getLastName(), null, 0, 1);
        Assertions.assertEquals(1, users.size(), "Same user should be in provider realm");

        String userId = users.get(0).getId();
        UserResource userResource = providerRealm.admin().users().get(userId);

        // list consents
        List<Map<String, Object>> consents = userResource.getConsents();
        Assertions.assertEquals(1, consents.size(), "There should be one consent");

        Map<String, Object> consent = consents.get(0);
        Assertions.assertEquals(CLIENT_ID, consent.get("clientId"), "Consent should be given to " + CLIENT_ID);

        // list sessions. Single client should be in user session
        List<UserSessionRepresentation> sessions = userResource.getUserSessions();
        Assertions.assertEquals(1, sessions.size(), "There should be one active session");
        Assertions.assertEquals(1, sessions.get(0).getClients().size(), "There should be one client in user session");

        // revoke consent
        userResource.revokeConsent(CLIENT_ID);

        // list consents
        consents = userResource.getConsents();
        Assertions.assertEquals(0, consents.size(), "There should be no consents");

        // list sessions
        sessions = userResource.getUserSessions();
        Assertions.assertEquals(1, sessions.size(), "There should be one active session");
        Assertions.assertEquals(0, sessions.get(0).getClients().size(), "There should be no client in user session");

        AccountHelper.logout(providerRealm.admin(), userFromProviderRealm.getUsername());
    }

    /**
     * KEYCLOAK-18954
     */
    @Test
    public void testRetrieveConsentsForUserWithClientsWithGrantedOfflineAccess() throws Exception {

        RealmRepresentation providerRealmRep = providerRealm.admin().toRepresentation();
        providerRealmRep.setAccountTheme("keycloak");
        providerRealm.admin().update(providerRealmRep);
        providerRealm.admin().clients().create(ClientConfigBuilder.create().clientId("test-app").redirectUris("*").publicClient(true).webOrigins("*").build());

        ClientRepresentation providerAccountRep = providerRealm.admin().clients().findByClientId("test-app").get(0);

        // add offline_scope to default account-console client scope
        ClientScopeRepresentation offlineAccessScope = providerRealm.admin().getDefaultOptionalClientScopes().stream()
                .filter(csr -> csr.getName().equals(OAuth2Constants.OFFLINE_ACCESS)).findFirst().get();
        providerRealm.admin().clients().get(providerAccountRep.getId()).removeOptionalClientScope(offlineAccessScope.getId());
        providerRealm.admin().clients().get(providerAccountRep.getId()).addDefaultClientScope(offlineAccessScope.getId());

        // enable consent required to explicitly grant offline access
        providerAccountRep.setConsentRequired(true);
        providerAccountRep.setDirectAccessGrantsEnabled(true); // for offline token retrieval
        providerRealm.admin().clients().get(providerAccountRep.getId()).update(providerAccountRep);

        // navigate to account console and login
        providerRealmOAuth.openLoginForm();

        loginPage.waitForPage();
        LOGGER.debug("Logging in");
        loginPage.fillLogin(userFromProviderRealm.getUsername(), userFromProviderRealm.getPassword());
        loginPage.submit();

        consentPage.waitForPage();
        LOGGER.debug("Grant consent for offline_access");
        consentPage.assertCurrent();
        consentPage.confirm();

        // disable consent required again to enable direct grant token retrieval.
        providerAccountRep.setConsentRequired(false);
        providerRealm.admin().clients().get(providerAccountRep.getId()).update(providerAccountRep);

        LOGGER.debug("Obtain offline_token");
        AccessTokenResponse response = providerRealmOAuth
                .scope(OAuth2Constants.SCOPE_OPENID +" " + OAuth2Constants.SCOPE_PROFILE + " " + OAuth2Constants.OFFLINE_ACCESS)
                .doPasswordGrantRequest(userFromProviderRealm.getUsername(), userFromProviderRealm.getPassword());
        assertNotNull(response.getRefreshToken());

        LOGGER.debug("Check for Offline Token in consents");
        List<Map<String, Object>> consents = providerRealm.admin().users().get(userFromProviderRealm.getId()).getConsents();
        assertFalse(consents.isEmpty(), "Consents should not be empty");

        assertTrue(consents.toString().contains("Offline Token"));

        AccountHelper.logout(providerRealm.admin(), userFromProviderRealm.getUsername());
    }

    @Test
    public void testConsentCancel() {
        // setup account client to require consent
        ClientResource accountClient = findClientByClientId(providerRealm.admin(), "test-app");

        ClientRepresentation clientRepresentation = accountClient.toRepresentation();
        clientRepresentation.setConsentRequired(true);
        accountClient.update(clientRepresentation);

        // navigate to account console and login
        providerRealmOAuth.openLoginForm();
        loginPage.fillLogin(userFromProviderRealm.getUsername(), userFromProviderRealm.getPassword());
        loginPage.submit();

        consentPage.assertCurrent();
        consentPage.cancel();

        // check an error page after cancelling the consent
        assertTrue(driver.getPageSource().contains("Happy days"));
        assertTrue(driver.getCurrentUrl().contains("error=access_denied"));

        providerRealmOAuth.openLoginForm();
        loginPage.fillLogin(userFromProviderRealm.getUsername(), userFromProviderRealm.getPassword());
        loginPage.submit();
        consentPage.confirm();

        // successful login
        assertFalse(driver.getCurrentUrl().contains("error"));
        assertTrue(driver.getPageSource().contains("Happy days"), "Test user should be successfully logged in.");
    }

    @Test
    public void clientConsentRequiredAfterLogin() {
        AuthorizationEndpointResponse response = userRealmOAuth.doLogin(userFromUserRealm.getUsername(), userFromUserRealm.getPassword());
        AccessTokenResponse accessTokenResponse = userRealmOAuth.doAccessTokenRequest(response.getCode());

        Assertions.assertNotNull(userRealmOAuth.parseLoginResponse().getCode());
        assertTrue(driver.getPageSource().contains("Happy days"), "Test user should be successfully logged in.");

        EventRepresentation loginEvent = userRealmEvents.poll();
        Assertions.assertNotNull(loginEvent);
        Assertions.assertEquals(userFromUserRealm.getId(), loginEvent.getUserId());
        Assertions.assertEquals(EventType.LOGIN.toString(), loginEvent.getType());
        loginEvent.getDetails().forEach((key, value) -> {
            switch (key) {
                case Details.CODE_ID -> MatcherAssert.assertThat(value, EventMatchers.isCodeId());
                case Details.USERNAME -> Assertions.assertEquals(userFromUserRealm.getUsername(), value);
                case Details.CONSENT -> Assertions.assertEquals(Details.CONSENT_VALUE_NO_CONSENT_REQUIRED, value);
                case Details.REDIRECT_URI -> Assertions.assertEquals("http://127.0.0.1:8500/callback/oauth", value);
            }
        });

        userRealm.updateWithCleanup(r -> r.enabledEventTypes("REFRESH_TOKEN_ERROR"));
        String sessionId = loginEvent.getSessionId();

        ClientRepresentation clientRepresentation = userRealm.admin().clients().findByClientId("test-app").get(0);
        try {
            clientRepresentation.setConsentRequired(true);
            userRealm.admin().clients().get(clientRepresentation.getId()).update(clientRepresentation);

            userRealmEvents.clear();

            // try to refresh the token
            // this fails as client no longer has requested consent from user
            AccessTokenResponse refreshTokenResponse = userRealmOAuth.doRefreshTokenRequest(accessTokenResponse.getRefreshToken());
            Assertions.assertEquals(OAuthErrorException.INVALID_SCOPE, refreshTokenResponse.getError());
            Assertions.assertEquals("Client no longer has requested consent from user", refreshTokenResponse.getErrorDescription());

            EventRepresentation refreshEvent = userRealmEvents.poll();
            Assertions.assertNotNull(refreshEvent);
            Assertions.assertNull(refreshEvent.getUserId());
            Assertions.assertEquals(EventType.REFRESH_TOKEN_ERROR.toString(), refreshEvent.getType());
            Assertions.assertNull(refreshTokenResponse.getRefreshToken());
            Assertions.assertEquals(sessionId, refreshEvent.getSessionId());
            Assertions.assertEquals(Errors.INVALID_TOKEN, refreshEvent.getError());
        } finally {
            clientRepresentation.setConsentRequired(false);
            userRealm.admin().clients().get(clientRepresentation.getId()).update(clientRepresentation);
        }

        AccountHelper.logout(userRealm.admin(), userFromUserRealm.getUsername());
    }

    @Test
    public void testConsentWithAdditionalClientAttributes() {
        // setup account client to require consent
        ClientResource accountClient = findClientByClientId(providerRealm.admin(), "test-app");

        ClientRepresentation clientRepresentation = accountClient.toRepresentation();
        clientRepresentation.setConsentRequired(true);
        clientRepresentation.getAttributes().put(ClientModel.LOGO_URI,"https://www.keycloak.org/resources/images/keycloak_logo_480x108.png");
        clientRepresentation.getAttributes().put(ClientModel.POLICY_URI,"https://www.keycloak.org/policy");
        clientRepresentation.getAttributes().put(ClientModel.TOS_URI,"https://www.keycloak.org/tos");
        accountClient.update(clientRepresentation);

        // navigate to account console and login
        providerRealmOAuth.openLoginForm();
        loginPage.fillLogin(userFromProviderRealm.getUsername(), userFromProviderRealm.getPassword());
        loginPage.submit();

        consentPage.assertCurrent();

        assertTrue(driver.findElement(By.xpath("//img[@src='https://www.keycloak.org/resources/images/keycloak_logo_480x108.png']")).isDisplayed(), "logoUri must be presented");
        assertTrue(driver.findElement(By.xpath("//a[@href='https://www.keycloak.org/policy']")).isDisplayed(), "policyUri must be presented");
        assertTrue(driver.findElement(By.xpath("//a[@href='https://www.keycloak.org/tos']")).isDisplayed(), "tosUri must be presented");

        consentPage.confirm();

        // successful login
        assertTrue(driver.getPageSource().contains("Happy days"), "Test user should be successfully logged in.");
        AccountHelper.logout(providerRealm.admin(), userFromProviderRealm.getUsername());
    }

    private static IdentityProviderRepresentation setUpIdentityProvider() {
        IdentityProviderRepresentation idp = createIdentityProvider(IDP_OIDC_ALIAS, IDP_OIDC_PROVIDER_ID);

        Map<String, String> config = idp.getConfig();

        config.put("clientId", CLIENT_ID);
        config.put("clientSecret", CLIENT_SECRET);
        config.put("prompt", "login");
        config.put("authorizationUrl", "http://localhost:8080/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/auth");
        config.put("tokenUrl", "http://localhost:8080/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/token");
        config.put("logoutUrl", "http://localhost:8080/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/logout");
        config.put("userInfoUrl", "http://localhost:8080/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/userinfo");
        config.put("defaultScope", "email profile");
        config.put("backchannelSupported", "true");

        return idp;
    }

    private static IdentityProviderRepresentation createIdentityProvider(String alias, String providerId) {
        IdentityProviderRepresentation identityProviderRepresentation = new IdentityProviderRepresentation();

        identityProviderRepresentation.setAlias(alias);
        identityProviderRepresentation.setDisplayName(providerId);
        identityProviderRepresentation.setProviderId(providerId);
        identityProviderRepresentation.setEnabled(true);

        return identityProviderRepresentation;
    }

    private boolean isUUID(String uuid) {
        return 36 == uuid.length()
                && uuid.charAt(8) == '-'
                && uuid.charAt(13) == '-'
                && uuid.charAt(18) == '-'
                && uuid.charAt(23) == '-';
    }

    private static class UserRealmUserConf implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder builder) {
            builder.username("user");
            builder.password("password");
            builder.email("user@local");
            builder.emailVerified(true);
            builder.name("Local", "User");

            return builder;
        }
    }

    private static class ProviderRealmUserConf implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder builder) {
            builder.username("provider");
            builder.password("password");
            builder.email("provider@local");
            builder.emailVerified(true);
            builder.name("Provider", "User");

            return builder;
        }
    }

    private static class ProviderRealmClientConf implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder builder) {
            builder.clientId(CLIENT_ID);
            builder.name(CLIENT_ID);
            builder.secret(CLIENT_SECRET);
            builder.consentRequired(true);
            builder.redirectUris( "http://localhost:8080/realms/" + REALM_CONS_NAME + "/broker/" + IDP_OIDC_ALIAS + "/endpoint/*");
            builder.adminUrl("http://localhost:8080/realms/" + REALM_CONS_NAME + "/broker/" + IDP_OIDC_ALIAS + "/endpoint");

            return builder;
        }
    }

    private static class ConsumerRealmConf implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder builder) {
            builder.identityProvider(setUpIdentityProvider());

            return builder;
        }
    }
}
