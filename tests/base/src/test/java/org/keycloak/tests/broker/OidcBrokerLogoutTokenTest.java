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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.UUID;

import org.keycloak.broker.jwtauthorizationgrant.JWTAuthorizationGrantConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.oidc.LogoutTokenValidationCode;
import org.keycloak.representations.LogoutToken;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testsuite.util.oauth.BackchannelLogoutResponse;
import org.keycloak.util.TokenUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Sends logout tokens signed with a key configured on an OIDC identity provider.
 */
@KeycloakIntegrationTest
public class OidcBrokerLogoutTokenTest {

    private static final String IDP_ALIAS = "logout-idp";
    private static final String ISSUER = "http://127.0.0.1:8500/logout-idp";
    private static final String CLIENT_ID = "logout-client";
    private static final String KEY_ID = "logout-key";
    private static final KeyPair KEY_PAIR = createRsaKeyPair();

    @InjectRealm(config = ConsumerRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @Test
    public void testValidToken() {
        Assertions.assertTrue(oauth.doBackchannelLogout(sign(createLogoutToken())).isSuccess());
    }

    @Test
    public void testMissingExpirationIsRejected() {
        LogoutToken token = createLogoutToken();
        token.exp(null);

        assertRejected(sign(token));
    }

    @Test
    public void testZeroExpirationIsRejected() {
        LogoutToken token = createLogoutToken();
        token.exp(0L);

        assertRejected(sign(token));
    }

    @Test
    public void testUnexpectedAlgorithmIsRejected() {
        assertRejected(new JWSBuilder().kid(KEY_ID).jsonContent(createLogoutToken()).rsa512(KEY_PAIR.getPrivate()));
    }

    @Test
    public void testOtherAudienceIsRejected() {
        LogoutToken token = createLogoutToken();
        token.audience(CLIENT_ID, "other-client");

        assertRejected(sign(token));
    }

    private void assertRejected(String encodedToken) {
        BackchannelLogoutResponse response = oauth.doBackchannelLogout(encodedToken);

        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals(LogoutTokenValidationCode.TOKEN_VERIFICATION_WITH_IDP_FAILED.getErrorMessage(), response.getErrorDescription());
    }

    private static LogoutToken createLogoutToken() {
        LogoutToken token = new LogoutToken();
        token.putEvents(TokenUtil.TOKEN_BACKCHANNEL_LOGOUT_EVENT, new HashMap<>());
        token.setSubject(UUID.randomUUID().toString());
        token.issuer(ISSUER);
        token.id(UUID.randomUUID().toString());
        token.issuedNow();
        token.exp(Time.currentTime() + 120L);
        token.audience(CLIENT_ID);
        return token;
    }

    private static String sign(LogoutToken token) {
        return new JWSBuilder().kid(KEY_ID).jsonContent(token).rsa256(KEY_PAIR.getPrivate());
    }

    private static KeyPair createRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static class ConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.identityProviders(IdentityProviderBuilder.create()
                    .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                    .alias(IDP_ALIAS)
                    .attribute("clientId", CLIENT_ID)
                    .attribute("clientSecret", "logout-secret")
                    .attribute("validateSignature", "true")
                    .attribute(OIDCIdentityProviderConfig.USE_JWKS_URL, "false")
                    .attribute(JWTAuthorizationGrantConfig.PUBLIC_KEY_SIGNATURE_VERIFIER, PemUtils.encodeKey(KEY_PAIR.getPublic()))
                    .attribute(JWTAuthorizationGrantConfig.PUBLIC_KEY_SIGNATURE_VERIFIER_KEY_ID, KEY_ID)
                    .attribute(IdentityProviderModel.ISSUER, ISSUER)
                    .attribute(OIDCIdentityProviderConfig.ID_TOKEN_SIGNING_ALG, Algorithm.RS256)
                    .attribute(OIDCIdentityProviderConfig.REJECT_OTHER_ID_TOKEN_AUDIENCES, "true")
                    .build());
        }
    }
}
