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
package org.keycloak.tests.oauth;

import java.util.HashMap;
import java.util.List;

import org.keycloak.broker.jwtauthorizationgrant.JWTAuthorizationGrantConfig;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.tests.oauth.AuthChainingAcrossDomainsTest.DomainbConfig;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest(config = JWTAuthorizationGrantTest.JWTAuthorizationGrantServerConfig.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
public class AuthChainingAcrossDomainsTest {

    @InjectRealm(ref = "domaina", config = DomainaConfig.class)
    ManagedRealm domaina;

    @InjectRealm(ref = "domainb", config = DomainbConfig.class)
    ManagedRealm domainb;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    public static class DomainaConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.name("domaina");

            // client for the IdP in domainb
            realm.addClient("http://localhost:8080/realms/domainb")
                    .redirectUris("http://localhost:8080/realms/domainb/broker/domaina/endpoint/*")
                    .secret("password");

            // mapper to add audience for domainb in clienta
            ProtocolMapperRepresentation domainbAudience = new ProtocolMapperRepresentation();
            domainbAudience.setName("domainb-audience");
            domainbAudience.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            domainbAudience.setProtocolMapper(AudienceProtocolMapper.PROVIDER_ID);
            domainbAudience.setConfig(new HashMap<>());
            domainbAudience.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, Boolean.TRUE.toString());
            domainbAudience.getConfig().put(AudienceProtocolMapper.INCLUDED_CLIENT_AUDIENCE, "http://localhost:8080/realms/domainb");

            // test client to request the Token exchange in domaina
            realm.addClient("clienta")
                    .secret("password")
                    .redirectUris("*")
                    .attribute(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_ENABLED, Boolean.TRUE.toString())
                    .protocolMappers(List.of(domainbAudience));

            // test user in domaina
            realm.addUser("testuser")
                    .name("Test", "User")
                    .email("test@localhost")
                    .emailVerified(Boolean.TRUE)
                    .password("password");

            return realm;
        }
    }

    public static class DomainbConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.name("domainb");

            // idp for domaina
            realm.identityProvider(IdentityProviderBuilder.create()
                    .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                    .alias("domaina")
                    .setAttribute(IdentityProviderModel.ISSUER, "http://localhost:8080/realms/domaina")
                    .setAttribute(OIDCIdentityProviderConfig.USE_JWKS_URL, Boolean.TRUE.toString())
                    .setAttribute("validateSignature", Boolean.TRUE.toString())
                    .setAttribute(OIDCIdentityProviderConfig.JWKS_URL, "http://localhost:8080/realms/domaina/protocol/openid-connect/certs")
                    .setAttribute("authorizationUrl", "http://localhost:8080/realms/domaina/protocol/openid-connect/auth")
                    .setAttribute(OAuth2IdentityProviderConfig.TOKEN_ENDPOINT_URL, "http://localhost:8080/realms/domaina/protocol/openid-connect/token")
                    .setAttribute(OAuth2IdentityProviderConfig.TOKEN_INTROSPECTION_URL, "http://localhost:8080/realms/domaina/protocol/openid-connect/token/introspect")
                    .setAttribute("userInfoUrl", "http://localhost:8080/realms/domaina/protocol/openid-connect/userinfo")
                    .setAttribute("logoutUrl", "http://localhost:8080/realms/domaina/protocol/openid-connect/logout")
                    .setAttribute("backchannelSupported", Boolean.TRUE.toString())
                    .setAttribute("clientId", "http://localhost:8080/realms/domainb")
                    .setAttribute("clientSecret", "password")
                    .setAttribute(JWTAuthorizationGrantConfig.JWT_AUTHORIZATION_GRANT_ENABLED, Boolean.TRUE.toString())
                    .build());

            // test client to request jwt auth grant in domainb
            realm.addClient("clientb")
                    .secret("password")
                    .redirectUris("*")
                    .attribute(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_ENABLED, Boolean.TRUE.toString())
                    .attribute(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP, "domaina");

            return realm;
        }
    }

    @Test
    public void authorizationChainingAcrossDomains() throws Exception {
        // login user in domainb using broker, link to domaina and logout
        oauth.realm("domainb").client("clientb", "password").openLoginForm();
        loginPage.clickSocial("domaina");
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());
        oauth.logoutRequest().idTokenHint(tokenResponse.getIdToken()).send();

        // do a login in domaina to obtain an access token with clienta
        oauth.realm("domaina").client("clienta", "password").openLoginForm();
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();
        tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        // perform token exchange to get the assertion grant for domainb
        tokenResponse = oauth.tokenExchangeRequest(tokenResponse.getAccessToken())
                .audience("http://localhost:8080/realms/domainb")
                .send();
        Assertions.assertTrue(tokenResponse.isSuccess());

        // perform the jwt authorization grant in domainb
        oauth.realm("domainb").client("clientb", "password");
        tokenResponse = oauth.doJWTAuthorizationGrantRequest(tokenResponse.getAccessToken());
        Assertions.assertTrue(tokenResponse.isSuccess());

        // use the token for introspection in domainb
        IntrospectionResponse introspectionResponse = oauth.doIntrospectionAccessTokenRequest(tokenResponse.getAccessToken());
        Assertions.assertTrue(introspectionResponse.isSuccess());
        TokenMetadataRepresentation metadata = introspectionResponse.asTokenMetadata();
        Assertions.assertNull(metadata.getSessionId());
        MatcherAssert.assertThat(metadata.getId(), Matchers.startsWith("trrtag:"));
        Assertions.assertEquals("clientb", metadata.getIssuedFor());
        Assertions.assertEquals("testuser", metadata.getPreferredUsername());
    }
}
