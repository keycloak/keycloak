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
package org.keycloak.tests.forms;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Verifies the {@code usernamePasswordFormEnabled} realm attribute, which controls whether the
 * username/password form is rendered on the login page. When disabled, only the configured
 * identity providers are offered.
 */
@KeycloakIntegrationTest
public class UsernamePasswordFormEnabledTest {

    @InjectRealm(config = BrokerRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @Test
    public void formShownByDefault() {
        oauth.openLoginForm();

        Assertions.assertTrue(loginPage.isUsernameInputPresent());
        Assertions.assertTrue(loginPage.isPasswordInputPresent());
        Assertions.assertTrue(loginPage.isSocialButtonPresent("my-oidc"));
    }

    @Test
    public void formHiddenWhenDisabled() {
        realm.updateWithCleanup(r -> r.usernamePasswordFormEnabled(false));

        oauth.openLoginForm();

        Assertions.assertFalse(loginPage.isUsernameInputPresent());
        Assertions.assertFalse(loginPage.isPasswordInputPresent());
        // Identity providers remain available so users can still sign in.
        Assertions.assertTrue(loginPage.isSocialButtonPresent("my-oidc"));
    }

    @Test
    public void formShownWhenExplicitlyEnabled() {
        realm.updateWithCleanup(r -> r.usernamePasswordFormEnabled(true));

        oauth.openLoginForm();

        Assertions.assertTrue(loginPage.isUsernameInputPresent());
        Assertions.assertTrue(loginPage.isPasswordInputPresent());
    }

    public static class BrokerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("realm-hide-password-form");

            realm.identityProviders(
                    IdentityProviderBuilder.create()
                            .providerId("oidc")
                            .alias("my-oidc")
                            .displayName("MyOIDC")
                            .build()
            );

            return realm;
        }
    }
}
