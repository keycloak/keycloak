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
import org.openqa.selenium.NoSuchElementException;

@KeycloakIntegrationTest
public class HiddenProviderTest {

    @InjectRealm(config = HiddenProviderRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @Test
    public void testVisibleProviderButton() {
        oauth.openLoginForm();
        Assertions.assertNotNull(loginPage.findSocialButton("visible-oidc"));
    }

    @Test
    public void testHiddenProviderButton() {
        oauth.openLoginForm();
        Assertions.assertThrows(NoSuchElementException.class, () -> {
            loginPage.findSocialButton("hidden-oidc");
        });
    }

    public static class HiddenProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("realm-with-broker");

            realm.identityProviders(
                    IdentityProviderBuilder.create()
                            .providerId("oidc")
                            .alias("visible-oidc")
                            .displayName("VisibleOIDC")
                            .build(),
                    IdentityProviderBuilder.create()
                            .providerId("oidc")
                            .alias("hidden-oidc")
                            .displayName("HiddenOIDC")
                            .hideOnLoginPage()
                            .build()
            );

            return realm;
        }
    }
}
