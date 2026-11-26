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
package org.keycloak.tests.broker;

import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.oauth.OAuth2IdentityProviderFactory;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.EventType;
import org.keycloak.models.ParConfig;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Brokers to another realm with an OAuth 2.0 identity provider that uses PAR, DPoP and the {@code iss} parameter.
 */
@KeycloakIntegrationTest
public class KcOAuth2BrokerDPoPTest extends AbstractKcOidcBrokerTest {

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD, config = DPoPProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD, config = OAuth2ConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @BeforeEach
    void configurePushedAuthorizationRequests() {
        IdentityProviderResource idp = consumerRealm.admin().identityProviders().get(getIdpAlias());
        IdentityProviderRepresentation rep = idp.toRepresentation();
        rep.getConfig().put(OAuth2IdentityProviderConfig.PAR_URL,
                providerRealm.getBaseUrl() + "/protocol/openid-connect/ext/par/request");
        // changing the URL requires the client secret to be entered again
        rep.getConfig().put("clientSecret", CLIENT_SECRET);
        idp.update(rep);
    }

    @Test
    public void testLoginWithDPoPBoundToken() {
        RealmEventsConfigRepresentation eventsConfig = providerRealm.admin().getRealmEventsConfig();
        eventsConfig.setEventsEnabled(true);
        eventsConfig.setEnabledEventTypes(List.of(EventType.USER_INFO_REQUEST.name(), EventType.USER_INFO_REQUEST_ERROR.name()));
        providerRealm.admin().updateRealmEventsConfig(eventsConfig);

        loginUser();

        Assertions.assertEquals(1, userInfoEvents(EventType.USER_INFO_REQUEST));
        Assertions.assertEquals(0, userInfoEvents(EventType.USER_INFO_REQUEST_ERROR));
    }

    @Test
    public void testImportOfTheMetadataFillsInTheSettings() {
        Map<String, String> config = consumerRealm.admin().identityProviders().importFrom(
                Map.of("providerId", OAuth2IdentityProviderFactory.PROVIDER_ID,
                        "fromUrl", providerRealm.getBaseUrl() + "/.well-known/openid-configuration"));

        Assertions.assertEquals(providerRealm.getBaseUrl() + "/protocol/openid-connect/ext/par/request",
                config.get(OAuth2IdentityProviderConfig.PAR_URL));
        Assertions.assertEquals("true", config.get(OAuth2IdentityProviderConfig.AUTHORIZATION_RESPONSE_ISSUER_REQUIRED));
        Assertions.assertEquals(Algorithm.ES256, config.get(OAuth2IdentityProviderConfig.DPOP_SIGNING_ALG));
    }

    private static IdentityProviderBuilder createOAuth2IdentityProvider() {
        return createOidcIdentityProvider()
                .providerId(OAuth2IdentityProviderFactory.PROVIDER_ID)
                // set the openid scope to be able to access the userinfo endpoint
                .attribute("defaultScope", "openid");
    }

    private long userInfoEvents(EventType type) {
        return providerRealm.admin().getEvents(List.of(type.name()), null, null, null, null, null, null, null).size();
    }

    static class DPoPProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureProviderRealm(realm,
                    createDefaultProviderClient()
                            .attribute(ParConfig.REQUIRE_PUSHED_AUTHORIZATION_REQUESTS, "true")
                            .attribute(OIDCConfigAttributes.DPOP_BOUND_ACCESS_TOKENS, "true"));
        }
    }

    static class OAuth2ConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            // the endpoints and the issuer are set before every test
            return configureConsumerRealm(realm,
                    createOAuth2IdentityProvider()
                            .attribute(OAuth2IdentityProviderConfig.ISSUER, "http://localhost:8080/realms/provider")
                            .attribute("userInfoUrl", "http://localhost:8080/realms/provider/protocol/openid-connect/userinfo")
                            .attribute(OAuth2IdentityProviderConfig.PAR_ENABLED, "true")
                            .attribute(OAuth2IdentityProviderConfig.PAR_URL, "http://localhost:8080/realms/provider/par")
                            .attribute(OAuth2IdentityProviderConfig.DPOP_ENABLED, "true")
                            .attribute(OAuth2IdentityProviderConfig.DPOP_SIGNING_ALG, Algorithm.RS256)
                            .attribute(OAuth2IdentityProviderConfig.AUTHORIZATION_RESPONSE_ISSUER_REQUIRED, "true"));
        }
    }
}
