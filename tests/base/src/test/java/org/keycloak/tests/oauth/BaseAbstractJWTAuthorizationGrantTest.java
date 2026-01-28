/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.tests.oauth;

import java.util.UUID;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.OAuthIdentityProvider;
import org.keycloak.testframework.oauth.OAuthIdentityProviderConfig;
import org.keycloak.testframework.oauth.OAuthIdentityProviderConfigBuilder;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthIdentityProvider;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.client.authentication.external.ClientAuthIdpServerConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.tests.oauth.AbstractJWTAuthorizationGrantTest.IDP_ISSUER;

/**
 *
 * @author rmartinc
 */
public class BaseAbstractJWTAuthorizationGrantTest {

    public static String IDP_ALIAS = "authorization-grant-idp-alias";
    public static final String IDP_ISSUER = "https://authorization-grant-issuer";

    @InjectOAuthIdentityProvider(config = AbstractJWTAuthorizationGrantTest.AGIdpConfig.class)
    OAuthIdentityProvider identityProvider;

    @InjectRealm(config = AbstractJWTAuthorizationGrantTest.JWTAuthorizationGrantRealmConfig.class)
    protected ManagedRealm realm;

    @InjectUser(config = AbstractJWTAuthorizationGrantTest.FederatedUserConfiguration.class)
    protected ManagedUser user;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectEvents
    Events events;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    protected JsonWebToken createDefaultAuthorizationGrantToken() {
        return createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER, Time.currentTime() + 300L);
    }

    protected JsonWebToken createAuthorizationGrantToken(long expiration) {
        return createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER, Time.currentTime() + expiration);
    }

    protected JsonWebToken createAuthorizationGrantToken(String subject, String audience, String issuer) {
        return createAuthorizationGrantToken(subject, audience, issuer, Time.currentTime() + 300L, (long) Time.currentTime(), null);
    }

    protected JsonWebToken createAuthorizationGrantToken(String subject, String audience, String issuer, Long exp) {
        return createAuthorizationGrantToken(subject, audience, issuer, exp, null, null);
    }

    protected AccessToken createDefaultAuthorizationGrantToken(String scope) {
        return createAuthorizationGrantToken("basic-user-id", oAuthClient.getEndpoints().getIssuer(), IDP_ISSUER, Time.currentTime() + 300L, null, scope);
    }

    protected AccessToken createAuthorizationGrantToken(String subject, String audience, String issuer, Long exp, Long iat) {
        return createAuthorizationGrantToken(subject, audience, issuer, exp, iat, null);
    }

    protected AccessToken createAuthorizationGrantToken(String subject, String audience, String issuer, Long exp, Long iat, String scope) {
        AccessToken token = new AccessToken();
        token.id(UUID.randomUUID().toString());
        token.subject(subject);
        token.audience(audience);
        token.issuer(issuer);
        token.exp(exp);
        token.iat(iat);
        token.setScope(scope);
        return token;
    }

    public OAuthIdentityProvider getIdentityProvider() {
        return identityProvider;
    }

    public static class AGIdpConfig implements OAuthIdentityProviderConfig {

        @Override
        public OAuthIdentityProviderConfigBuilder configure(OAuthIdentityProviderConfigBuilder config) {
            return config;
        }
    }

    public static class JWTAuthorizationGrantServerConfig extends ClientAuthIdpServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return super.configure(config).features(Profile.Feature.JWT_AUTHORIZATION_GRANT);
        }
    }

    public static class JWTAuthorizationGrantRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addClient("test-public").publicClient(true);
            realm.addClient("authorization-grant-disabled-client").publicClient(false).secret("test-secret");
            realm.addClient("authorization-grant-not-allowed-idp-client").publicClient(false).attribute(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_ENABLED, "true").secret("test-secret");
            return realm;
        }
    }

    public static class FederatedUserConfiguration implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder user) {
            return user
                    .username("basic-user")
                    .password("password")
                    .email("basic@localhost")
                    .name("First", "Last")
                    .federatedLink(IDP_ALIAS, "basic-user-id", "basic-user");
        }
    }

    protected AccessToken assertSuccess(String expectedClientId, AccessTokenResponse response) {
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertNull(response.getRefreshToken());
        AccessToken accessToken = oAuthClient.parseToken(response.getAccessToken(), AccessToken.class);
        Assertions.assertNull(accessToken.getSessionId());
        MatcherAssert.assertThat(accessToken.getId(), Matchers.startsWith("trrtag:"));
        Assertions.assertEquals(expectedClientId, accessToken.getIssuedFor());
        Assertions.assertEquals(user.getUsername(), accessToken.getPreferredUsername());
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .clientId(expectedClientId)
                .sessionId(null)
                .userId(user.getId())
                .details(Details.GRANT_TYPE, OAuth2Constants.JWT_AUTHORIZATION_GRANT)
                .details(Details.IDENTITY_PROVIDER, IDP_ALIAS)
                .details(Details.IDENTITY_PROVIDER_ISSUER, IDP_ISSUER)
                .details(Details.IDENTITY_PROVIDER_USER_ID, "basic-user-id")
                .details(Details.USERNAME, user.getUsername());
        return accessToken;
    }

    protected EventAssertion assertFailure(String expectedErrorDescription, AccessTokenResponse response, EventRepresentation event) {
        return assertFailure(OAuthErrorException.INVALID_GRANT, expectedErrorDescription, response, event);
    }

    protected EventAssertion assertFailure(String expectedError, String expectedErrorDescription, AccessTokenResponse response, EventRepresentation event) {
        Assertions.assertFalse(response.isSuccess());
        Assertions.assertEquals(expectedError, response.getError());
        Assertions.assertEquals(expectedErrorDescription, response.getErrorDescription());
        return EventAssertion.assertError(event)
                .type(EventType.LOGIN_ERROR)
                .sessionId(null)
                .error(OAuthErrorException.INVALID_REQUEST)
                .details(Details.GRANT_TYPE, OAuth2Constants.JWT_AUTHORIZATION_GRANT)
                .details(Details.REASON, expectedErrorDescription);
    }

    protected EventAssertion assertFailurePolicy(String expectedError, String expectedErrorDescription, AccessTokenResponse response, EventRepresentation event) {
        Assertions.assertFalse(response.isSuccess());
        Assertions.assertEquals(expectedError, response.getError());
        Assertions.assertEquals(expectedErrorDescription, response.getErrorDescription());
        return EventAssertion.assertError(event)
                .type(EventType.LOGIN_ERROR)
                .sessionId(null)
                .userId(user.getId())
                .error(expectedError)
                .details(Details.GRANT_TYPE, OAuth2Constants.JWT_AUTHORIZATION_GRANT)
                .details(Details.IDENTITY_PROVIDER_ISSUER, IDP_ISSUER)
                .details(Details.IDENTITY_PROVIDER_USER_ID, "basic-user-id")
                .details(Details.REASON, Details.CLIENT_POLICY_ERROR)
                .details(Details.CLIENT_POLICY_ERROR, expectedError)
                .details(Details.CLIENT_POLICY_ERROR_DETAIL, expectedErrorDescription)
                .details(Details.USERNAME, user.getUsername());
    }
}
