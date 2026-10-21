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
package org.keycloak.tests.broker.trust;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.keycloak.broker.jwtauthorizationgrant.JWTAuthorizationGrantConfig;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.provider.TrustMaterialIdentityProvider;
import org.keycloak.broker.provider.TrustMaterialRequest;
import org.keycloak.broker.provider.TrustMaterialResolver;
import org.keycloak.broker.trust.DefaultTrustIdentityProvider;
import org.keycloak.broker.trust.DefaultTrustIdentityProviderConfig;
import org.keycloak.broker.trust.DefaultTrustIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.common.util.PemUtils;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.HttpServerUtil;
import org.keycloak.util.JsonSerialization;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = TrustMaterialIdentityProviderTest.TrustMaterialServerConfig.class)
public class TrustMaterialIdentityProviderTest {

    private static final String DEFAULT_INLINE_ALIAS = "trust-material-default-inline";
    private static final String DEFAULT_URL_ALIAS = "trust-material-default-url";
    private static final String DEFAULT_PUBLIC_KEY_WITHOUT_REQUEST_ALIAS = "trust-material-default-public-key-without-request";
    private static final String DEFAULT_PUBLIC_KEY_WITH_REQUEST_ALIAS = "trust-material-default-public-key-with-request";
    private static final String DEFAULT_DISABLED_ALIAS = "trust-material-default-disabled";
    private static final String OIDC_ALIAS = "trust-material-oidc";
    private static final String KEY_ID = "trust-material-key";
    private static final String CONFIGURED_KEY_ID = "trust-material-configured-key";
    private static final String REQUEST_KEY_ID = "trust-material-request-key";
    private static final String ALGORITHM = "PS256";
    private static final String ISSUER = "https://issuer.example.test";

    private static String trustedJwks;
    private static String trustedPublicKey;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectHttpServer
    HttpServer httpServer;

    @TestSetup
    public void setup() throws Exception {
        KeyPair key = createRsaKeyPair();
        JWK jwk = JWKBuilder.create()
                .kid(KEY_ID)
                .algorithm(ALGORITHM)
                .rsa(key.getPublic());
        JSONWebKeySet jwks = new JSONWebKeySet();
        jwks.setKeys(new JWK[] { jwk });
        trustedJwks = JsonSerialization.writeValueAsString(jwks);
        trustedPublicKey = PemUtils.encodeKey(key.getPublic());
    }

    @BeforeEach
    public void configureIdentityProviders() {
        String jwks = trustedJwks;
        String publicKey = trustedPublicKey;
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            configureTrustIdentityProvider(realm, DEFAULT_INLINE_ALIAS, DefaultTrustIdentityProviderFactory.PROVIDER_ID, true,
                    Map.of(DefaultTrustIdentityProviderConfig.TRUSTED_JWKS, jwks));
            configureTrustIdentityProvider(realm, DEFAULT_DISABLED_ALIAS, DefaultTrustIdentityProviderFactory.PROVIDER_ID, false,
                    Map.of(DefaultTrustIdentityProviderConfig.TRUSTED_JWKS, jwks));
            configureTrustIdentityProvider(realm, OIDC_ALIAS, OIDCIdentityProviderFactory.PROVIDER_ID, true,
                    Map.of(
                            OIDCIdentityProviderConfig.USE_JWKS_URL, Boolean.FALSE.toString(),
                            JWTAuthorizationGrantConfig.PUBLIC_KEY_SIGNATURE_VERIFIER, publicKey,
                            JWTAuthorizationGrantConfig.PUBLIC_KEY_SIGNATURE_VERIFIER_KEY_ID, KEY_ID,
                            IdentityProviderModel.ISSUER, ISSUER));
        });
    }

    @Test
    public void defaultTrustIdentityProviderResolvesInlineTrustedJwks() {
        runOnServer.run(session -> {
            TrustMaterialIdentityProvider<?> provider = getTrustMaterialProvider(session.getContext().getRealm(), session, DEFAULT_INLINE_ALIAS);
            assertInstanceOf(DefaultTrustIdentityProvider.class, provider);

            JWK jwk = provider.resolveKeys(matchingRequest()).findFirst().orElseThrow();

            assertEquals(KEY_ID, jwk.getKeyId());
            assertEquals(ALGORITHM, jwk.getAlgorithm());
        });
    }

    @Test
    public void defaultTrustIdentityProviderResolvesTrustedJwksUrl() {
        String path = "/trust-material-jwks";
        httpServer.createContext(path, exchange -> HttpServerUtil.sendResponse(exchange, 200,
                Map.of("Content-Type", List.of("application/json")), trustedJwks));

        try {
            String jwksUrl = "http://" + httpServer.getAddress().getHostString() + ":" + httpServer.getAddress().getPort() + path;
            runOnServer.run(session -> configureTrustIdentityProvider(session.getContext().getRealm(), DEFAULT_URL_ALIAS,
                    DefaultTrustIdentityProviderFactory.PROVIDER_ID, true,
                    Map.of(DefaultTrustIdentityProviderConfig.TRUSTED_JWKS_URL, jwksUrl)));

            runOnServer.run(session -> {
                TrustMaterialIdentityProvider<?> provider = getTrustMaterialProvider(session.getContext().getRealm(), session, DEFAULT_URL_ALIAS);
                assertInstanceOf(DefaultTrustIdentityProvider.class, provider);

                JWK jwk = provider.resolveKeys(matchingRequest()).findFirst().orElseThrow();

                assertEquals(KEY_ID, jwk.getKeyId());
                assertEquals(ALGORITHM, jwk.getAlgorithm());
            });
        } finally {
            httpServer.removeContext(path);
        }
    }

    @Test
    public void defaultTrustIdentityProviderUsesConfiguredPublicKeyIdWithoutRequestKid() {
        String publicKey = trustedPublicKey;
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            configureTrustIdentityProvider(realm, DEFAULT_PUBLIC_KEY_WITHOUT_REQUEST_ALIAS,
                    DefaultTrustIdentityProviderFactory.PROVIDER_ID, true,
                    Map.of(
                            OIDCIdentityProviderConfig.USE_JWKS_URL, Boolean.FALSE.toString(),
                            DefaultTrustIdentityProviderConfig.TRUSTED_JWKS, publicKey,
                            DefaultTrustIdentityProviderConfig.TRUSTED_JWKS_KEY_ID, CONFIGURED_KEY_ID));

            TrustMaterialIdentityProvider<?> provider = getTrustMaterialProvider(realm, session, DEFAULT_PUBLIC_KEY_WITHOUT_REQUEST_ALIAS);
            assertInstanceOf(DefaultTrustIdentityProvider.class, provider);

            JWK jwk = provider.resolveKeys(TrustMaterialRequest.builder()
                    .algorithm(ALGORITHM)
                    .build()).findFirst().orElseThrow();

            assertEquals(CONFIGURED_KEY_ID, jwk.getKeyId());
            assertEquals(ALGORITHM, jwk.getAlgorithm());
        });
    }

    @Test
    public void defaultTrustIdentityProviderPrefersRequestKidOverConfiguredPublicKeyId() {
        String publicKey = trustedPublicKey;
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            configureTrustIdentityProvider(realm, DEFAULT_PUBLIC_KEY_WITH_REQUEST_ALIAS,
                    DefaultTrustIdentityProviderFactory.PROVIDER_ID, true,
                    Map.of(
                            OIDCIdentityProviderConfig.USE_JWKS_URL, Boolean.FALSE.toString(),
                            DefaultTrustIdentityProviderConfig.TRUSTED_JWKS, publicKey,
                            DefaultTrustIdentityProviderConfig.TRUSTED_JWKS_KEY_ID, CONFIGURED_KEY_ID));

            TrustMaterialIdentityProvider<?> provider = getTrustMaterialProvider(realm, session, DEFAULT_PUBLIC_KEY_WITH_REQUEST_ALIAS);
            assertInstanceOf(DefaultTrustIdentityProvider.class, provider);

            JWK jwk = provider.resolveKeys(TrustMaterialRequest.builder()
                    .kid(REQUEST_KEY_ID)
                    .algorithm(ALGORITHM)
                    .build()).findFirst().orElseThrow();

            assertEquals(REQUEST_KEY_ID, jwk.getKeyId());
            assertEquals(ALGORITHM, jwk.getAlgorithm());
        });
    }

    @Test
    public void trustMaterialResolverUsesEnabledProviderFromAliasList() {
        runOnServer.run(session -> {
            Optional<JWK> jwk = new TrustMaterialResolver().resolveKey(session,
                    "missing-alias, " + DEFAULT_DISABLED_ALIAS + ", " + DEFAULT_INLINE_ALIAS, matchingRequest());

            assertTrue(jwk.isPresent());
            assertEquals(KEY_ID, jwk.get().getKeyId());
            assertEquals(ALGORITHM, jwk.get().getAlgorithm());
        });
    }

    @Test
    public void trustMaterialResolverReturnsEmptyForDisabledProvider() {
        runOnServer.run(session -> assertTrue(new TrustMaterialResolver()
                .resolveKey(session, DEFAULT_DISABLED_ALIAS, matchingRequest()).isEmpty()));
    }

    @Test
    public void oidcIdentityProviderResolvesConfiguredPublicKey() {
        runOnServer.run(session -> {
            TrustMaterialIdentityProvider<?> provider = getTrustMaterialProvider(session.getContext().getRealm(), session, OIDC_ALIAS);
            assertInstanceOf(OIDCIdentityProvider.class, provider);

            JWK jwk = provider.resolveKeys(TrustMaterialRequest.builder()
                    .kid(KEY_ID)
                    .algorithm(ALGORITHM)
                    .issuer(ISSUER)
                    .build()).findFirst().orElseThrow();

            assertEquals(KEY_ID, jwk.getKeyId());
            assertEquals(ALGORITHM, jwk.getAlgorithm());
        });
    }

    @Test
    public void oidcIdentityProviderResolvesConfiguredPublicKeyWithoutKid() {
        runOnServer.run(session -> {
            TrustMaterialIdentityProvider<?> provider = getTrustMaterialProvider(session.getContext().getRealm(), session, OIDC_ALIAS);

            JWK jwk = provider.resolveKeys(TrustMaterialRequest.builder()
                    .algorithm(ALGORITHM)
                    .issuer(ISSUER)
                    .build()).findFirst().orElseThrow();

            assertEquals(KEY_ID, jwk.getKeyId());
            assertEquals(ALGORITHM, jwk.getAlgorithm());
        });
    }

    @Test
    public void oidcIdentityProviderRejectsIssuerMismatch() {
        runOnServer.run(session -> {
            TrustMaterialIdentityProvider<?> provider = getTrustMaterialProvider(session.getContext().getRealm(), session, OIDC_ALIAS);

            assertTrue(provider.resolveKeys(TrustMaterialRequest.builder()
                    .kid(KEY_ID)
                    .algorithm(ALGORITHM)
                    .issuer("https://issuer.invalid")
                    .build()).findAny().isEmpty());
        });
    }

    @Test
    public void oidcIdentityProviderDoesNotSplitConfiguredIssuer() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            IdentityProviderModel model = realm.getIdentityProviderByAlias(OIDC_ALIAS);
            Map<String, String> config = new HashMap<>(model.getConfig());
            config.put(IdentityProviderModel.ISSUER, ISSUER + ", https://issuer2.example.test");
            model.setConfig(config);
            realm.updateIdentityProvider(model);

            TrustMaterialIdentityProvider<?> provider = getTrustMaterialProvider(realm, session, OIDC_ALIAS);

            assertTrue(provider.resolveKeys(TrustMaterialRequest.builder()
                    .kid(KEY_ID)
                    .algorithm(ALGORITHM)
                    .issuer(ISSUER)
                    .build()).findAny().isEmpty());
        });
    }

    private static void configureTrustIdentityProvider(RealmModel realm, String alias, String providerId, boolean enabled,
            Map<String, String> config) {
        IdentityProviderModel trustIdp = realm.getIdentityProviderByAlias(alias);
        if (trustIdp == null) {
            trustIdp = new IdentityProviderModel();
            trustIdp.setAlias(alias);
            trustIdp.setProviderId(providerId);
            trustIdp.setEnabled(enabled);
            trustIdp.setConfig(config);
            realm.addIdentityProvider(trustIdp);
        } else {
            trustIdp.setProviderId(providerId);
            trustIdp.setEnabled(enabled);
            trustIdp.setConfig(config);
            realm.updateIdentityProvider(trustIdp);
        }
    }

    private static TrustMaterialIdentityProvider<?> getTrustMaterialProvider(RealmModel realm, KeycloakSession session, String alias) {
        IdentityProviderModel model = realm.getIdentityProviderByAlias(alias);
        return IdentityBrokerService.getIdentityProvider(session, model, TrustMaterialIdentityProvider.class);
    }

    private static TrustMaterialRequest matchingRequest() {
        return TrustMaterialRequest.builder()
                .kid(KEY_ID)
                .algorithm(ALGORITHM)
                .build();
    }

    private static KeyPair createRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    public static class TrustMaterialServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_AUTH_ABCA);
        }
    }
}
