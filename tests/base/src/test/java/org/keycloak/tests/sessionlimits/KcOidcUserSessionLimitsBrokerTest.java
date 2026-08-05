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
package org.keycloak.tests.sessionlimits;

import org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginUpdateProfilePage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.broker.oidc.OAuth2IdentityProviderConfig.TOKEN_ENDPOINT_URL;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.ERROR_TO_DISPLAY;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.assertSessionCount;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.configurePostBrokerFlow;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.removePostBrokerFlow;

@KeycloakIntegrationTest
public class KcOidcUserSessionLimitsBrokerTest {

    private static final String PROVIDER_REALM = "provider";
    private static final String CONSUMER_REALM = "consumer";
    private static final String IDP_ALIAS = "kc-oidc-idp";
    private static final String USER_LOGIN = "testuser";
    private static final String USER_PASSWORD = "password";
    private static final String USER_EMAIL = "user@localhost.com";
    private static final String BROKER_CLIENT_ID = "brokerapp";
    private static final String BROKER_CLIENT_SECRET = "secret";
    private static final String CONSUMER_CLIENT_ID = "broker-app";
    private static final String CONSUMER_CLIENT_SECRET = "broker-app-secret";

    @InjectRealm(ref = "provider", config = ProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", config = ConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @InjectRunOnServer(realmRef = "consumer")
    RunOnServerClient runOnServer;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    ErrorPage errorPage;

    @InjectPage
    LoginUpdateProfilePage loginUpdateProfilePage;

    @BeforeEach
    public void setup() {
        deleteAllCookies(consumerRealm);
        deleteAllCookies(providerRealm);

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            session.sessions().removeUserSessions(realm);
            UserModel user = session.users().getUserByUsername(realm, USER_LOGIN);
            if (user != null) {
                session.users().removeUser(realm, user);
            }
        });

        runOnServer.run(removePostBrokerFlow(CONSUMER_REALM));
    }

    @Test
    public void testSessionCountExceededAndNewSessionDeniedFirstBrokerLoginFlow() {
        runOnServer.run(configurePostBrokerFlow(CONSUMER_REALM, IDP_ALIAS,
                UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "0", "1"));

        logInAsUserInIDPForFirstTime();
        deleteAllCookies(consumerRealm);
        deleteAllCookies(providerRealm);
        logInAsUserInIDP();

        errorPage.assertCurrent();
        Assertions.assertEquals(ERROR_TO_DISPLAY, errorPage.getError());
    }

    @Test
    public void testSessionCountExceededAndOldestSessionRemovedFirstBrokerLoginFlow() {
        runOnServer.run(configurePostBrokerFlow(CONSUMER_REALM, IDP_ALIAS,
                UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION, "0", "1"));

        logInAsUserInIDPForFirstTime();
        deleteAllCookies(consumerRealm);
        deleteAllCookies(providerRealm);
        logInAsUserInIDP();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        runOnServer.run(assertSessionCount(CONSUMER_REALM, USER_LOGIN, 1));
    }

    @Test
    public void testRealmSessionCountExceededAndNewSessionDeniedFirstBrokerLoginFlow() {
        runOnServer.run(configurePostBrokerFlow(CONSUMER_REALM, IDP_ALIAS,
                UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "1", "0"));

        logInAsUserInIDPForFirstTime();
        deleteAllCookies(consumerRealm);
        deleteAllCookies(providerRealm);
        logInAsUserInIDP();

        errorPage.assertCurrent();
        Assertions.assertEquals(ERROR_TO_DISPLAY, errorPage.getError());
    }

    @Test
    public void testRealmSessionCountExceededAndOldestSessionRemovedFirstBrokerLoginFlow() {
        runOnServer.run(configurePostBrokerFlow(CONSUMER_REALM, IDP_ALIAS,
                UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION, "1", "0"));

        logInAsUserInIDPForFirstTime();
        deleteAllCookies(consumerRealm);
        deleteAllCookies(providerRealm);
        logInAsUserInIDP();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        runOnServer.run(assertSessionCount(CONSUMER_REALM, USER_LOGIN, 1));
    }

    private void logInAsUserInIDPForFirstTime() {
        logInAsUserInIDP();
        loginUpdateProfilePage.assertCurrent();
        loginUpdateProfilePage.update("Firstname", "Lastname");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    private void logInAsUserInIDP() {
        oauth.realm(CONSUMER_REALM).client(CONSUMER_CLIENT_ID, CONSUMER_CLIENT_SECRET).openLoginForm();
        loginPage.assertCurrent();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.assertCurrent();
        loginPage.fillLogin(USER_LOGIN, USER_PASSWORD);
        loginPage.submit();
    }

    private void deleteAllCookies(ManagedRealm realm) {
        driver.driver().navigate().to(realm.getBaseUrl());
        driver.cookies().deleteAll();
    }

    public static class ProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name(PROVIDER_REALM);
            realm.users(UserBuilder.create(USER_LOGIN)
                    .email(USER_EMAIL)
                    .emailVerified(true)
                    .password(USER_PASSWORD)
                    .enabled(true));
            realm.clients(ClientBuilder.create(BROKER_CLIENT_ID)
                    .secret(BROKER_CLIENT_SECRET)
                    .redirectUris("http://localhost:8080/realms/" + CONSUMER_REALM + "/broker/" + IDP_ALIAS + "/endpoint/*"));
            return realm;
        }
    }

    public static class ConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name(CONSUMER_REALM);
            realm.identityProviders(IdentityProviderBuilder.create()
                    .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                    .alias(IDP_ALIAS)
                    .attribute(IdentityProviderModel.SYNC_MODE, "IMPORT")
                    .attribute("clientId", BROKER_CLIENT_ID)
                    .attribute("clientSecret", BROKER_CLIENT_SECRET)
                    .attribute(OIDCIdentityProviderConfig.ISSUER, "http://localhost:8080/realms/" + PROVIDER_REALM)
                    .attribute("authorizationUrl", "http://localhost:8080/realms/" + PROVIDER_REALM + "/protocol/openid-connect/auth")
                    .attribute(TOKEN_ENDPOINT_URL, "http://localhost:8080/realms/" + PROVIDER_REALM + "/protocol/openid-connect/token")
                    .attribute("logoutUrl", "http://localhost:8080/realms/" + PROVIDER_REALM + "/protocol/openid-connect/logout")
                    .attribute("userInfoUrl", "http://localhost:8080/realms/" + PROVIDER_REALM + "/protocol/openid-connect/userinfo")
                    .attribute(OIDCIdentityProviderConfig.JWKS_URL, "http://localhost:8080/realms/" + PROVIDER_REALM + "/protocol/openid-connect/certs")
                    .attribute(OIDCIdentityProviderConfig.USE_JWKS_URL, "true")
                    .attribute(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE, "true")
                    .attribute("backchannelSupported", "true")
                    .attribute("defaultScope", "email profile")
                    .build());
            realm.clients(ClientBuilder.create(CONSUMER_CLIENT_ID)
                    .secret(CONSUMER_CLIENT_SECRET)
                    .directAccessGrantsEnabled(true)
                    .redirectUris("*"));
            return realm;
        }
    }
}
