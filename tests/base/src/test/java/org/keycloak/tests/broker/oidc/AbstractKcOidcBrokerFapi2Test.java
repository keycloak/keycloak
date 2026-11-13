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

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.EventType;
import org.keycloak.keys.GeneratedEcdsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.ParConfig;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.https.CertificatesConfig;
import org.keycloak.testframework.https.CertificatesConfigBuilder;
import org.keycloak.testframework.https.InjectCertificates;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.OAuthGrantPage;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.utils.ClientPoliciesUtil.ClientPoliciesBuilder;
import org.keycloak.tests.utils.ClientPoliciesUtil.ClientPolicyBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

/**
 * Brokers over https to another realm that enforces a built-in FAPI 2.0 client profile on its clients.
 */
public abstract class AbstractKcOidcBrokerFapi2Test extends AbstractKcOidcBrokerTest {

    protected static final String SIGN_ALG = Algorithm.ES256;

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD, config = Fapi2ProviderRealmConfig.class)
    protected ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD, config = Fapi2ConsumerRealmConfig.class)
    protected ManagedRealm consumerRealm;

    // the profiles only accept https URIs
    @InjectCertificates(config = TlsEnabledConfig.class)
    ManagedCertificates certificates;

    @InjectEvents(realmRef = "provider")
    protected Events providerEvents;

    @InjectPage
    OAuthGrantPage grantPage;

    /**
     * The built-in client profile that the provider enforces.
     */
    protected abstract String getClientProfile();

    @BeforeEach
    void configureFapi2() throws Exception {
        addEcdsaKeyProvider(providerRealm);
        addEcdsaKeyProvider(consumerRealm);
        configureProviderClientJwksUrl(CLIENT_ID);
        updateIdentityProvider(config -> {
            config.put(OAuth2IdentityProviderConfig.PAR_URL, providerRealm.getBaseUrl() + "/protocol/openid-connect/ext/par/request");
            config.put("clientAssertionAudience", providerRealm.getBaseUrl());
            config.put(IdentityProviderModel.METADATA_DESCRIPTOR_URL, providerRealm.getBaseUrl() + "/.well-known/openid-configuration");
            config.put(OAuth2IdentityProviderConfig.VALIDATE_DISCOVERY_ISSUER, "true");
        });

        ClientPoliciesRepresentation policies = new ClientPoliciesBuilder()
                .addPolicy(new ClientPolicyBuilder()
                        .createPolicy("fapi2", null, true)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID, null)
                        .addProfile(getClientProfile())
                        .toRepresentation())
                .toRepresentation();
        providerRealm.admin().clientPoliciesPoliciesResource().updatePolicies(policies);

        // the profile configures the client on the update and requires consent
        updateProviderClient(client -> {
            client.setRedirectUris(List.of(consumerRealm.getBaseUrl() + "/broker/" + getIdpAlias() + "/endpoint"));
            client.setWebOrigins(List.of());
        });
    }

    @Override
    protected void logInAsUserInIDPForFirstTime() {
        super.logInAsUserInIDPForFirstTime();
        grantPage.accept();
    }

    protected static void setIssuer(Map<String, String> config, String issuer) {
        config.put(OAuth2IdentityProviderConfig.ISSUER, issuer);
        config.put(IdentityProviderModel.METADATA_DESCRIPTOR_URL, issuer + "/.well-known/openid-configuration");
    }

    protected void startLogin(boolean providerLoginExpected) {
        oauth.openLoginForm();
        logInWithBroker();
        if (providerLoginExpected) {
            logInAsUserInIDPForFirstTime();
        }
    }

    protected void updateProviderClient(Consumer<ClientRepresentation> update) {
        ClientsResource clients = providerRealm.admin().clients();
        ClientRepresentation client = clients.findByClientId(CLIENT_ID).get(0);
        update.accept(client);
        clients.get(client.getId()).update(client);
    }

    protected void updateIdentityProvider(Consumer<Map<String, String>> update) {
        IdentityProviderResource idp = consumerRealm.admin().identityProviders().get(getIdpAlias());
        IdentityProviderRepresentation rep = idp.toRepresentation();
        update.accept(rep.getConfig());
        idp.update(rep);
    }

    private static void addEcdsaKeyProvider(ManagedRealm realm) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName("fapi2-ecdsa");
        rep.setParentId(realm.admin().toRepresentation().getId());
        rep.setProviderId(GeneratedEcdsaKeyProviderFactory.ID);
        rep.setProviderType(KeyProvider.class.getName());
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle("priority", Long.toString(System.currentTimeMillis()));
        rep.getConfig().putSingle("active", "true");
        rep.getConfig().putSingle("enabled", "true");
        rep.getConfig().putSingle(GeneratedEcdsaKeyProviderFactory.ECDSA_ELLIPTIC_CURVE_KEY, "P-256");
        try (Response response = realm.admin().components().add(rep)) {
            Assertions.assertNotNull(CreatedResponseUtil.getCreatedId(response));
        }
    }

    static class Fapi2ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            // the server does not trust its own certificate for requests to itself
            // remove once the test framework adds it to the server truststore
            // https://github.com/keycloak/keycloak/pull/54143
            return new CustomProvidersServerConfig().configure(config)
                    .spiOption("connections-http-client", "default", "disable-trust-manager", "true");
        }
    }

    static class TlsEnabledConfig implements CertificatesConfig {
        @Override
        public CertificatesConfigBuilder configure(CertificatesConfigBuilder config) {
            return config.tlsEnabled(true);
        }
    }

    static class Fapi2ProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureProviderRealm(realm,
                    createDefaultProviderClient()
                            .authenticatorType(JWTClientAuthenticator.PROVIDER_ID)
                            .attribute(OIDCConfigAttributes.USE_JWKS_URL, "true")
                            .attribute(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, SIGN_ALG)
                            .attribute(ParConfig.REQUIRE_PUSHED_AUTHORIZATION_REQUESTS, "true")
                            .attribute(OIDCConfigAttributes.PKCE_CODE_CHALLENGE_METHOD, OAuth2Constants.PKCE_METHOD_S256)
                            .attribute(OIDCConfigAttributes.DPOP_BOUND_ACCESS_TOKENS, "true")
                            .attribute(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG, SIGN_ALG)
                            .attribute(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG, SIGN_ALG)
                            .attribute(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, SIGN_ALG)
                            .attribute(OIDCConfigAttributes.REQUEST_OBJECT_SIGNATURE_ALG, SIGN_ALG))
                    // only the events the tests assert
                    // user info events are not stored by default
                    .enabledEventTypes(EventType.LOGIN_ERROR.name(), EventType.USER_INFO_REQUEST.name(),
                            EventType.USER_INFO_REQUEST_ERROR.name());
        }
    }

    static class Fapi2ConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            // the endpoints and the issuer are set before every test
            return configureConsumerRealm(realm,
                    createOidcIdentityProvider()
                            .attribute("clientSecret", null)
                            .attribute("clientAuthMethod", OIDCLoginProtocol.PRIVATE_KEY_JWT)
                            .attribute("clientAssertionSigningAlg", SIGN_ALG)
                            .attribute(OAuth2IdentityProviderConfig.ISSUER, "http://localhost:8080/realms/provider")
                            .attribute("validateSignature", "true")
                            .attribute("useJwksUrl", "true")
                            .attribute("jwksUrl", "http://localhost:8080/realms/provider/protocol/openid-connect/certs")
                            .attribute(OAuth2IdentityProviderConfig.PKCE_ENABLED, "true")
                            .attribute(OAuth2IdentityProviderConfig.PKCE_METHOD, OAuth2Constants.PKCE_METHOD_S256)
                            .attribute(OAuth2IdentityProviderConfig.PAR_ENABLED, "true")
                            .attribute(OAuth2IdentityProviderConfig.PAR_URL, "http://localhost:8080/realms/provider/par")
                            .attribute(OAuth2IdentityProviderConfig.DPOP_ENABLED, "true")
                            .attribute(OAuth2IdentityProviderConfig.DPOP_SIGNING_ALG, SIGN_ALG)
                            .attribute(OAuth2IdentityProviderConfig.AUTHORIZATION_RESPONSE_ISSUER_REQUIRED, "true")
                            .attribute(OIDCIdentityProviderConfig.ID_TOKEN_SIGNING_ALG, SIGN_ALG)
                            .attribute(OIDCIdentityProviderConfig.REJECT_OTHER_ID_TOKEN_AUDIENCES, "true"));
        }
    }
}
