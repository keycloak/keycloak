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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
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
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.tests.utils.admin.ApiUtil.findClientByClientId;

import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@KeycloakIntegrationTest
public class ConsentsTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectRealm(ref = "consumer")
    ManagedRealm consumerRealm;

    @InjectRealm(ref = "provider", config = ConsentsRealmConf.class)
    ManagedRealm providerRealm;

    @InjectUser(realmRef = "provider")
    ManagedUser user;

    @InjectClient(realmRef = "consumer", config = ConsentsClient1Conf.class)
    ManagedClient client1;

    @InjectClient(realmRef = "provider", config = ConsentsClient2Conf.class)
    ManagedClient client2;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectEvents
    Events events;

    @InjectWebDriver
    WebDriver driver;

    @InjectPage
    LoginPage accountLoginPage;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    ConsentPage consentPage;

    AppPage appPage;

    private static final String REALM_PROV_NAME = "provider";
    private static final String REALM_CONS_NAME = "consumer";

    private static final String IDP_OIDC_ALIAS = "kc-oidc-idp";
    private static final String IDP_OIDC_PROVIDER_ID = "keycloak-oidc";

    private static final String CLIENT_ID = "brokerapp";
    private static final String CLIENT_SECRET = "secret";

    @Test
    public void testConsents() {
        oauth.realm(consumerRealmName());
        oauth.redirectUri(oauth.SERVER_ROOT + "/auth/realms/" + consumerRealmName() + "/app/auth");
        oauth.openLoginForm();

        log.debug("Clicking social " + getIDPAlias());
        accountLoginPage.clickSocial(getIDPAlias());

        if (!driver.getCurrentUrl().contains("/auth/realms/" + providerRealmName() + "/")) {
            log.debug("Not on provider realm page, url: " + driver.getCurrentUrl());
        }

        Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + providerRealmName() + "/"), "Driver should be on the provider realm page right now");

        log.debug("Logging in");
        accountLoginPage.fillLogin(user.getUsername(), user.getUsername());
        accountLoginPage.submit();

        waitForPage("grant access");

        Assertions.assertTrue(consentPage.isCurrent());
        consentPage.confirm();

        Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + consumerRealmName() + "/"), "We must be on correct realm right now");

        UsersResource consumerUsers = consumerRealm.admin().users();
        Assertions.assertTrue(consumerUsers.count() > 0, "There must be at least one user");

        List<UserRepresentation> users = consumerUsers.search("", 0, 5);

        UserRepresentation foundUser = null;
        for (UserRepresentation user : users) {
            if (user.getUsername().equals(USER_LOGIN) && user.getEmail().equals(getUserEmail())) {
                foundUser = user;
                break;
            }
        }

        Assertions.assertNotNull(foundUser, "There must be user " + USER_LOGIN + " in realm " + consumerRealmName());

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

        // oauth clean up
        oauth.realm("test");
        oauth.redirectUri(oauth.SERVER_ROOT + "/auth/realms/master/app/auth");
    }

    /**
     * KEYCLOAK-18954
     */
    @Test
    public void testRetrieveConsentsForUserWithClientsWithGrantedOfflineAccess() throws Exception {

        RealmRepresentation providerRealmRep = providerRealm.admin().toRepresentation();
        providerRealmRep.setAccountTheme("keycloak");
        providerRealm.admin().update(providerRealmRep);
        providerRealm.admin().clients().create(ClientConfigBuilder.create().clientId("test-app").redirectUris("*").publicClient(true).addWebOrigin("*").build());

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

        List<UserRepresentation> searchResult = providerRealm.admin().users().search(USER_LOGIN);
        UserRepresentation user = searchResult.get(0);

        accountLoginPage..open(providerRealmName());

        waitForPage("Sign in to provider");
        log.debug("Logging in");
        accountLoginPage.fillLogin(USER_LOGIN, USER_PASSWORD);
        accountLoginPage.submit();

        waitForPage("grant access");
        log.debug("Grant consent for offline_access");
        Assertions.assertTrue(consentPage.isCurrent());
        consentPage.confirm();

        // disable consent required again to enable direct grant token retrieval.
        providerAccountRep.setConsentRequired(false);
        providerRealm.admin().clients().get(providerAccountRep.getId()).update(providerAccountRep);

        log.debug("Obtain offline_token");
        AccessTokenResponse response = oauth.realm(providerRealmRep.getRealm())
                .client(providerAccountRep.getClientId())
                .scope(OAuth2Constants.SCOPE_OPENID +" " + OAuth2Constants.SCOPE_PROFILE + " " + OAuth2Constants.OFFLINE_ACCESS)
                .doPasswordGrantRequest(USER_LOGIN, USER_PASSWORD);
        assertNotNull(response.getRefreshToken());

        log.debug("Check for Offline Token in consents");
        List<Map<String, Object>> consents = providerRealm.users().get(user.getId()).getConsents();
        assertFalse(consents.isEmpty(), "Consents should not be empty");

        assertTrue(consents.toString().contains("Offline Token"));
    }

    @Test
    public void testConsentCancel() {
        // setup account client to require consent
        ClientResource accountClient = findClientByClientId(providerRealm.admin(), "test-app");

        ClientRepresentation clientRepresentation = accountClient.toRepresentation();
        clientRepresentation.setConsentRequired(true);
        accountClient.update(clientRepresentation);

        // setup correct realm
        oauth.realm(providerRealm.getName());

        // navigate to account console and login
        oauth.openLoginForm();
        loginPage.form().login(user.getUsername(), user.getUsername());

        consentPage.assertCurrent();
        consentPage.cancel();

        // check an error page after cancelling the consent
        assertTrue(driver.getTitle().contains("AUTH_RESPONSE"));
        assertTrue(driver.getCurrentUrl().contains("error=access_denied"));

        oauth.openLoginForm();
        accountLoginPage.fillLogin(user.getUsername(), user.getUsername());
        accountLoginPage.submit();
        consentPage.confirm();

        // successful login
        assertFalse(driver.getCurrentUrl().contains("error"));
        assertTrue(driver.getTitle().contains("AUTH_RESPONSE"), "Test user should be successfully logged in.");
    }

    @Test
    public void clientConsentRequiredAfterLogin() {
        oauth.realm(TEST_REALM_NAME).client("test-app", "password");
        AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(response.getCode());

        Assertions.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        EventRepresentation loginEvent = events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
        String sessionId = loginEvent.getSessionId();

        ClientRepresentation clientRepresentation = managedRealm.admin().clients().findByClientId("test-app").get(0);
        try {
            clientRepresentation.setConsentRequired(true);
            managedRealm.admin().clients().get(clientRepresentation.getId()).update(clientRepresentation);

            events.clear();

            // try to refresh the token
            // this fails as client no longer has requested consent from user
            AccessTokenResponse refreshTokenResponse = oauth.doRefreshTokenRequest(accessTokenResponse.getRefreshToken());
            Assertions.assertEquals(OAuthErrorException.INVALID_SCOPE, refreshTokenResponse.getError());
            Assertions.assertEquals("Client no longer has requested consent from user", refreshTokenResponse.getErrorDescription());

            events.expectRefresh(accessTokenResponse.getRefreshToken(), sessionId).user((String) null).clearDetails().error(Errors.INVALID_TOKEN).assertEvent();
        } finally {
            clientRepresentation.setConsentRequired(false);
            managedRealm.admin().clients().get(clientRepresentation.getId()).update(clientRepresentation);
        }
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

        // setup correct realm
        oauth.realm(providerRealm.getName());

        // navigate to account console and login
        oauth.openLoginForm();
        accountLoginPage.fillLogin(user.getUsername(), user.getUsername());
        accountLoginPage.submit();

        consentPage.assertCurrent();

        assertTrue(driver.findElement(By.xpath("//img[@src='https://www.keycloak.org/resources/images/keycloak_logo_480x108.png']")).isDisplayed(), "logoUri must be presented");
        assertTrue(driver.findElement(By.xpath("//a[@href='https://www.keycloak.org/policy']")).isDisplayed(), "policyUri must be presented");
        assertTrue(driver.findElement(By.xpath("//a[@href='https://www.keycloak.org/tos']")).isDisplayed(), "tosUri must be presented");

        consentPage.confirm();

        // successful login
        assertTrue(driver.getTitle().contains("AUTH_RESPONSE"), "Test user should be successfully logged in.");
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

    private static class ConsentsClient1Conf implements ClientConfig {

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

    private static class ConsentsClient2Conf implements ClientConfig {

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

    private static class ConsentsRealmConf implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder builder) {
            builder.identityProvider(setUpIdentityProvider());

            return builder;
        }
    }
}
