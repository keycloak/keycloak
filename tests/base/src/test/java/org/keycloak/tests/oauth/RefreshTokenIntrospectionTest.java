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
package org.keycloak.tests.oauth;

import java.io.IOException;

import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.ProtocolMapperBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest
public class RefreshTokenIntrospectionTest {

    private static final String CLIENT_A = "client-a";
    private static final String CLIENT_A_SECRET = "secret-a";
    private static final String CLIENT_B = "client-b";
    private static final String CLIENT_B_SECRET = "secret-b";

    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "password";

    @InjectRealm(config = RefreshTokenIntrospectionRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient(config = ClientAConfig.class)
    OAuthClient oauth;

    @InjectUser(config = TestUserConfig.class)
    ManagedUser user;

    @BeforeEach
    public void resetOAuthClient() {
        oauth.client(CLIENT_A, CLIENT_A_SECRET);
        oauth.scope(null);
    }

    @Test
    public void issuingClientCanIntrospectRefreshTokenWithHint() throws IOException {
        String refreshToken = refreshTokenForClientA();

        IntrospectionResponse response = oauth.introspectionRequest(refreshToken)
                .tokenTypeHint(OAuth2Constants.REFRESH_TOKEN)
                .send();

        assertThat(response.asTokenMetadata().isActive(), is(true));
        assertThat(response.asTokenMetadata().getClientId(), is(CLIENT_A));
    }

    @Test
    public void issuingClientCanIntrospectRefreshTokenWithoutHint() throws IOException {
        String refreshToken = refreshTokenForClientA();

        IntrospectionResponse response = oauth.introspectionRequest(refreshToken).send();

        assertThat(response.asTokenMetadata().isActive(), is(true));
        assertThat(response.asTokenMetadata().getClientId(), is(CLIENT_A));
    }

    @Test
    public void issuingClientCanIntrospectRefreshTokenWithAccessTokenHint() throws IOException {
        String refreshToken = refreshTokenForClientA();

        IntrospectionResponse response = oauth.introspectionRequest(refreshToken)
                .tokenTypeHint(OAuth2Constants.ACCESS_TOKEN)
                .send();

        assertThat(response.asTokenMetadata().isActive(), is(true));
        assertThat(response.asTokenMetadata().getClientId(), is(CLIENT_A));
    }

    @Test
    public void issuingClientCanIntrospectOfflineTokenWithoutHint() throws IOException {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(USERNAME, PASSWORD);
        assertThat(tokenResponse.getStatusCode(), is(200));

        IntrospectionResponse response = oauth.introspectionRequest(tokenResponse.getRefreshToken()).send();

        assertThat(response.asTokenMetadata().isActive(), is(true));
        assertThat(response.asTokenMetadata().getClientId(), is(CLIENT_A));
    }

    @Test
    public void otherClientCannotIntrospectRefreshTokenWithHint() throws IOException {
        String refreshToken = refreshTokenForClientA();

        IntrospectionResponse response = oauth.client(CLIENT_B, CLIENT_B_SECRET)
                .introspectionRequest(refreshToken)
                .tokenTypeHint(OAuth2Constants.REFRESH_TOKEN)
                .send();

        assertThat(response.asTokenMetadata().isActive(), is(false));
    }

    @Test
    public void otherClientCannotIntrospectRefreshTokenWithoutHint() throws IOException {
        String refreshToken = refreshTokenForClientA();

        IntrospectionResponse response = oauth.client(CLIENT_B, CLIENT_B_SECRET)
                .introspectionRequest(refreshToken)
                .send();

        assertThat(response.asTokenMetadata().isActive(), is(false));
    }

    @Test
    public void accessTokenIntrospectionIsUnaffected() throws IOException {
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(USERNAME, PASSWORD);
        assertThat(tokenResponse.getStatusCode(), is(200));

        IntrospectionResponse response = oauth.introspectionRequest(tokenResponse.getAccessToken()).send();

        assertThat(response.asTokenMetadata().isActive(), is(true));
        assertThat(response.asTokenMetadata().getClientId(), is(CLIENT_A));
    }

    private String refreshTokenForClientA() {
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(USERNAME, PASSWORD);
        assertThat(tokenResponse.getStatusCode(), is(200));
        return tokenResponse.getRefreshToken();
    }

    public static class RefreshTokenIntrospectionRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {

            return realm.clients(ClientBuilder.create()
                    .clientId(CLIENT_B)
                    .secret(CLIENT_B_SECRET)
                    .directAccessGrantsEnabled(true));
        }
    }

    public static class ClientAConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            // The audience mapper adds client-a to the aud claim of its own access token, as described in #51552.
            // It has no effect on refresh tokens, which is precisely why those are validated against azp.
            return client.clientId(CLIENT_A)
                    .secret(CLIENT_A_SECRET)
                    .directAccessGrantsEnabled(true)
                    .protocolMappers(ProtocolMapperBuilder.create()
                            .name("audience-" + CLIENT_A)
                            .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                            .protocolMapper(AudienceProtocolMapper.PROVIDER_ID)
                            .config(AudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE, CLIENT_A)
                            .config(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true")
                            .build());
        }
    }

    public static class TestUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            // A complete profile keeps VERIFY_PROFILE from blocking the direct grant
            return user.username(USERNAME)
                    .password(PASSWORD)
                    .name("Test", "User")
                    .email("testuser@local")
                    .emailVerified(true);
        }
    }
}
