/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.model.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.policy.DeleteUserActionProviderFactory;
import org.keycloak.models.policy.ResourcePolicyManager;
import org.keycloak.models.policy.UserActionBuilder;
import org.keycloak.models.policy.UserSessionRefreshTimeResourcePolicyProviderFactory;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
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
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ConsentPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.openqa.selenium.WebDriver;

/**
 */
@KeycloakIntegrationTest(config = RLMServerConfig.class)
public class BrokeredUserSessionRefreshTimePolicyTest {

    private static final String REALM_NAME = "consumer";

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @InjectRealm(ref = "consumer", config = ConsumerRealmConf.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm consumerRealm;

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD)
    ManagedRealm providerRealm;

    @InjectUser(ref = "alice", realmRef = "provider", config = ProviderRealmUserConf.class)
    ManagedUser aliceFromProviderRealm;

    @InjectUser(ref = "bob", realmRef = "consumer", config = ConsumerRealmUserConf.class)
    ManagedUser bobFromConsumerRealm;

    @InjectOAuthClient(ref = "consumer", realmRef = "consumer")
    OAuthClient consumerRealmOAuth;

    @InjectClient(realmRef = "provider", config = ProviderRealmClientConf.class)
    ManagedClient providerRealmClient;

    @InjectWebDriver
    WebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    ConsentPage consentPage;

    private static final String REALM_PROV_NAME = "provider";
    private static final String REALM_CONS_NAME = "consumer";

    private static final String IDP_OIDC_ALIAS = "kc-oidc-idp";
    private static final String IDP_OIDC_PROVIDER_ID = "keycloak-oidc";

    private static final String CLIENT_ID = "brokerapp";
    private static final String CLIENT_SECRET = "secret";

    @Test
    public void tesRunActionOnFederatedUser() {
        runOnServer.run((session -> {
                    configureSessionContext(session);
                    PolicyBuilder.create()
                            .of(UserSessionRefreshTimeResourcePolicyProviderFactory.ID)
                            .withConfig("broker-aliases", IDP_OIDC_ALIAS)
                            .withActions(
                                    UserActionBuilder.builder(DeleteUserActionProviderFactory.ID)
                                            .after(Duration.ofDays(1))
                                            .build()
                            ).build(session);
                }));

        consumerRealmOAuth.openLoginForm();
        loginPage.clickSocial(IDP_OIDC_ALIAS);

        Assertions.assertTrue(driver.getCurrentUrl().contains("/realms/" + providerRealm.getName() + "/"), "Driver should be on the provider realm page right now");
        loginPage.fillLogin(aliceFromProviderRealm.getUsername(), aliceFromProviderRealm.getPassword());
        loginPage.submit();
        consentPage.waitForPage();
        consentPage.assertCurrent();
        consentPage.confirm();
        assertTrue(driver.getPageSource().contains("Happy days"), "Test user should be successfully logged in.");

        UsersResource users = consumerRealm.admin().users();
        String username = aliceFromProviderRealm.getUsername();
        UserRepresentation federatedUser = users.search(username).get(0);
        List<FederatedIdentityRepresentation> federatedIdentities = users.get(federatedUser.getId()).getFederatedIdentity();
        assertFalse(federatedIdentities.isEmpty());

        runOnServer.run((session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);

            manager.runScheduledTasks();
            UserModel user = session.users().getUserByUsername(realm, username);
            assertNotNull(user);
            assertTrue(user.isEnabled());

            try {
                Time.setOffset(Math.toIntExact(Duration.ofDays(2).toSeconds()));
                manager.runScheduledTasks();
                user = session.users().getUserByUsername(realm, username);
                assertNull(user);
            } finally {
                Time.setOffset(0);
            }
        }));

        // now authenticate with bob directly in the consumer realm - he is not associated with the IDP and thus not influenced
        // by the idp-exclusive lifecycle policy.
        consumerRealmOAuth.openLoginForm();
        loginPage.fillLogin(bobFromConsumerRealm.getUsername(), bobFromConsumerRealm.getPassword());
        loginPage.submit();
        assertTrue(driver.getPageSource().contains("Happy days"), "Test user should be successfully logged in.");

        runOnServer.run((session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);

            // run the scheduled tasks - bob should not be affected.
            manager.runScheduledTasks();
            UserModel user = session.users().getUserByUsername(realm, "bob");
            assertNotNull(user);
            assertTrue(user.isEnabled());

            try {
                // run with a time offset - bob should still not be affected.
                Time.setOffset(Math.toIntExact(Duration.ofDays(2).toSeconds()));
                manager.runScheduledTasks();
                user = session.users().getUserByUsername(realm, "bob");
                assertNotNull(user);
            } finally {
                Time.setOffset(0);
            }
        }));
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

    private static class ProviderRealmUserConf implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder builder) {
            builder.username("alice");
            builder.password("password");
            builder.email("alice@wonderland.org");
            builder.emailVerified(true);
            builder.name("Alice", "Wonderland");
            return builder;
        }
    }

    private static class ConsumerRealmUserConf implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder builder) {
            builder.username("bob");
            builder.password("password");
            builder.email("bob@wonderland.org");
            builder.emailVerified(true);
            builder.name("Bob", "Madhatter");
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

    private static RealmModel configureSessionContext(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(REALM_NAME);
        session.getContext().setRealm(realm);
        return realm;
    }
}
