/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.broker.oidc;

import java.net.URI;
import java.util.Set;

import org.keycloak.OAuth2Constants;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.common.util.UriUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Brokers with a signed request object passed by value to another realm that requires it.
 */
@KeycloakIntegrationTest
public class KcOidcBrokerRequestObjectTest extends AbstractKcOidcBrokerTest {

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD, config = RequestObjectProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD, config = RequestObjectConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @BeforeEach
    void configureJwksUrl() {
        configureProviderClientJwksUrl(CLIENT_ID);
    }

    @Test
    public void testRequestObjectIsPassedByValue() {
        oauth.openLoginForm();
        logInWithBroker();

        // the provider renders its login page at the authorization URL
        webDriver.waiting().until(d -> webDriver.getCurrentUrl().startsWith(providerRealm.getBaseUrl()));
        loginPage.assertCurrent();
        // https://openid.net/specs/openid-connect-core-1_0.html#RequestObject
        Assertions.assertEquals(Set.of(OAuth2Constants.CLIENT_ID, OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.SCOPE, OIDCLoginProtocol.REQUEST_PARAM),
                UriUtils.decodeQueryString(URI.create(webDriver.getCurrentUrl()).getRawQuery()).keySet());
    }

    static class RequestObjectProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureProviderRealm(realm,
                    createDefaultProviderClient()
                            .attribute(OIDCConfigAttributes.USE_JWKS_URL, "true")
                            .attribute(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED, OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST));
        }
    }

    static class RequestObjectConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            // the issuer is set before every test
            return configureConsumerRealm(realm,
                    createOidcIdentityProvider()
                            .attribute(OAuth2IdentityProviderConfig.ISSUER, "http://localhost:8080/realms/provider")
                            .attribute(OAuth2IdentityProviderConfig.REQUEST_OBJECT_ENABLED, "true")
                            .attribute(OAuth2IdentityProviderConfig.REQUEST_OBJECT_SIGNING_ALG, Algorithm.RS256));
        }
    }
}
