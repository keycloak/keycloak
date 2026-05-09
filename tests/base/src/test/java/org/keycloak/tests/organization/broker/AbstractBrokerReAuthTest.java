/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.organization.broker;

import java.time.Instant;

import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class AbstractBrokerReAuthTest {

    protected static final String CONSUMER_REALM_NAME = "consumer";
    protected static final String PROVIDER_REALM_NAME = "provider";
    protected static final String IDP_ALIAS = "test-identity-provider";
    protected static final String IDP_CLIENT_ID = "test-idp-client";
    protected static final String IDP_CLIENT_SECRET = "test-idp-secret";
    protected static final String USER_LOGIN = "testuser";
    // Domain must match the org domain used in OrganizationBrokerReAuthTest
    protected static final String USER_EMAIL = "testuser@neworg.org";
    protected static final String USER_PASSWORD = "password";
    protected static final String BROKER_APP_CLIENT_ID = "broker-app";
    protected static final String BASE_URL = "http://localhost:8080";

    @InjectRealm(ref = PROVIDER_REALM_NAME, config = ProviderRealmConfig.class, lifecycle = LifeCycle.METHOD)
    protected ManagedRealm providerRealm;

    @InjectOAuthClient(realmRef = CONSUMER_REALM_NAME, config = BrokerAppClientConfig.class, lifecycle = LifeCycle.METHOD)
    protected OAuthClient oauth;

    @InjectPage
    protected LoginPage loginPage;

    @InjectWebDriver
    protected ManagedWebDriver driver;

    /**
     * Sets up the IdP in the consumer realm and completes the first broker login on the provider realm.
     * The browser must be navigating away from the provider realm when this returns (i.e. credentials
     * have been submitted). The base class handles the optional "Update Account Information" page and
     * the "Happy days" assertion that follow.
     *
     * @param hideOnLogin subclass-specific: controls whether/when the IdP is hidden on the login page
     * @return the {@link Instant} captured right after credentials were submitted to the provider realm
     */
    protected abstract Instant performFirstBrokerLogin(boolean hideOnLogin) throws InterruptedException;

    protected abstract ManagedRealm getConsumerRealm();

    @ParameterizedTest(name = "IdP hidden: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("Brokered user redirected to IdP for re-authentication when app-initiated action requires re-auth")
    public void testBrokeredUserRedirectedToIdpForReAuthenticationWhenAppInitiatedActionRequiresReAuth(boolean hideOnLogin) throws InterruptedException {
        Instant instantAfterLogin = performFirstBrokerLogin(hideOnLogin);

        assertTrue(driver.page().getPageSource().contains("Happy days"));

        ManagedRealm consumerRealm = getConsumerRealm();
        RequiredActionProviderRepresentation updateEmailAction = consumerRealm.admin().flows().getRequiredActions()
                .stream()
                .filter(a -> UserModel.RequiredAction.UPDATE_EMAIL.name().equals(a.getProviderId()))
                .findFirst()
                .orElseThrow();
        final var originalUpdateEmailEnabled = updateEmailAction.isEnabled();
        updateEmailAction.getConfig().put(Constants.MAX_AUTH_AGE_KEY, "0");
        updateEmailAction.setEnabled(true);
        consumerRealm.admin().flows().updateRequiredAction(UserModel.RequiredAction.UPDATE_EMAIL.name(), updateEmailAction);
        consumerRealm.cleanup().add(r -> {
            updateEmailAction.getConfig().remove(Constants.MAX_AUTH_AGE_KEY);
            updateEmailAction.setEnabled(originalUpdateEmailEnabled);
            r.flows().updateRequiredAction(UserModel.RequiredAction.UPDATE_EMAIL.name(), updateEmailAction);
        });

        while (Instant.now().isBefore(instantAfterLogin.plusSeconds(1))) {
            Thread.sleep(100);
        }
        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_EMAIL.name()).open();

        loginPage.assertCurrent();
        Assertions.assertTrue(driver.page().getPageSource().contains("Please re-authenticate to continue"));

        loginPage.findSocialButton(IDP_ALIAS).click();

        Assertions.assertTrue(driver.getCurrentUrl().contains("/realms/" + CONSUMER_REALM_NAME + "/"),
                "After re-authentication, should be redirected back to the consumer realm to update the email address");
        Assertions.assertTrue(driver.getCurrentUrl().contains("execution=UPDATE_EMAIL"),
                "After re-authentication, should land on the UPDATE_EMAIL execution");
    }

    static class ProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .users(UserBuilder.create(USER_LOGIN)
                            .name("Test", "User")
                            .email(USER_EMAIL)
                            .emailVerified(true)
                            .password(USER_PASSWORD))
                    .clients(ClientBuilder.create()
                            .clientId(IDP_CLIENT_ID)
                            .secret(IDP_CLIENT_SECRET)
                            .redirectUris(BASE_URL + "/realms/" + CONSUMER_REALM_NAME + "/broker/" + IDP_ALIAS + "/endpoint*")
                            .build());
        }
    }

    static class BrokerAppClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client
                    .clientId(BROKER_APP_CLIENT_ID)
                    .publicClient()
                    .redirectUris(BASE_URL + "/*");
        }
    }
}
