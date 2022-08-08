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

package org.keycloak.testsuite.oauth;

import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ImpersonationConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.services.resources.admin.permissions.AdminPermissionManagement;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.util.BasicAuthHelper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_ID;
import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_USERNAME;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.util.AdminClientUtil;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
@EnableFeature(value = Profile.Feature.TOKEN_EXCHANGE, skipRestart = true)
@EnableFeature(value = Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ, skipRestart = true)
public class ClientTokenExchangeTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Test
    @UncaughtServerErrorExpected
    @DisableFeature(value = Profile.Feature.TOKEN_EXCHANGE, skipRestart = true)
    public void checkFeatureDisabled() {
        // Required feature should return Status code 501 - Feature doesn't work
        testingClient.server().run(ClientTokenExchangeTest::addDirectExchanger);
        Assert.assertEquals(501, checkTokenExchange().getStatus());
        testingClient.server().run(ClientTokenExchangeTest::removeDirectExchanger);
    }

    @Test
    public void checkFeatureEnabled() {
        // Test if the required feature really works.
        testingClient.server().run(ClientTokenExchangeTest::addDirectExchanger);
        Assert.assertEquals(200, checkTokenExchange().getStatus());
        testingClient.server().run(ClientTokenExchangeTest::removeDirectExchanger);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setId(TEST);
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        testRealms.add(testRealmRep);
    }

    public static void setupRealm(KeycloakSession session) {
        addDirectExchanger(session);

        RealmModel realm = session.realms().getRealmByName(TEST);
        RoleModel exampleRole = realm.getRole("example");

        AdminPermissionManagement management = AdminPermissions.management(session, realm);
        ClientModel target = realm.getClientByClientId("target");
        assertNotNull(target);

        RoleModel impersonateRole = management.getRealmManagementClient().getRole(ImpersonationConstants.IMPERSONATION_ROLE);

        ClientModel clientExchanger = realm.addClient("client-exchanger");
        clientExchanger.setClientId("client-exchanger");
        clientExchanger.setPublicClient(false);
        clientExchanger.setDirectAccessGrantsEnabled(true);
        clientExchanger.setEnabled(true);
        clientExchanger.setSecret("secret");
        clientExchanger.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        clientExchanger.setFullScopeAllowed(false);
        clientExchanger.addScopeMapping(impersonateRole);
        clientExchanger.addProtocolMapper(UserSessionNoteMapper.createUserSessionNoteMapper(IMPERSONATOR_ID));
        clientExchanger.addProtocolMapper(UserSessionNoteMapper.createUserSessionNoteMapper(IMPERSONATOR_USERNAME));

        ClientModel illegal = realm.addClient("illegal");
        illegal.setClientId("illegal");
        illegal.setPublicClient(false);
        illegal.setDirectAccessGrantsEnabled(true);
        illegal.setEnabled(true);
        illegal.setSecret("secret");
        illegal.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        illegal.setFullScopeAllowed(false);

        ClientModel legal = realm.addClient("legal");
        legal.setClientId("legal");
        legal.setPublicClient(false);
        legal.setDirectAccessGrantsEnabled(true);
        legal.setEnabled(true);
        legal.setSecret("secret");
        legal.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        legal.setFullScopeAllowed(false);

        ClientModel directLegal = realm.addClient("direct-legal");
        directLegal.setClientId("direct-legal");
        directLegal.setPublicClient(false);
        directLegal.setDirectAccessGrantsEnabled(true);
        directLegal.setEnabled(true);
        directLegal.setSecret("secret");
        directLegal.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        directLegal.setFullScopeAllowed(false);

        ClientModel directPublic = realm.addClient("direct-public");
        directPublic.setClientId("direct-public");
        directPublic.setPublicClient(true);
        directPublic.setDirectAccessGrantsEnabled(true);
        directPublic.setEnabled(true);
        directPublic.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        directPublic.setFullScopeAllowed(false);
        directPublic.addRedirectUri("*");
        directPublic.addProtocolMapper(AudienceProtocolMapper.createClaimMapper("client-exchanger-audience", clientExchanger.getClientId(), null, true, false));

        ClientModel directUntrustedPublic = realm.addClient("direct-public-untrusted");
        directUntrustedPublic.setClientId("direct-public-untrusted");
        directUntrustedPublic.setPublicClient(true);
        directUntrustedPublic.setDirectAccessGrantsEnabled(true);
        directUntrustedPublic.setEnabled(true);
        directUntrustedPublic.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        directUntrustedPublic.setFullScopeAllowed(false);
        directUntrustedPublic.addRedirectUri("*");
        directUntrustedPublic.setAttribute(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+");
        directUntrustedPublic.addProtocolMapper(AudienceProtocolMapper.createClaimMapper("client-exchanger-audience", clientExchanger.getClientId(), null, true, false));

        ClientModel directNoSecret = realm.addClient("direct-no-secret");
        directNoSecret.setClientId("direct-no-secret");
        directNoSecret.setPublicClient(false);
        directNoSecret.setDirectAccessGrantsEnabled(true);
        directNoSecret.setEnabled(true);
        directNoSecret.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        directNoSecret.setFullScopeAllowed(false);

        ClientModel noRefreshToken = realm.addClient("no-refresh-token");
        noRefreshToken.setClientId("no-refresh-token");
        noRefreshToken.setPublicClient(false);
        noRefreshToken.setDirectAccessGrantsEnabled(true);
        noRefreshToken.setEnabled(true);
        noRefreshToken.setSecret("secret");
        noRefreshToken.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        noRefreshToken.setFullScopeAllowed(false);
        noRefreshToken.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, "false");

        ClientModel serviceAccount = realm.addClient("my-service-account");
        serviceAccount.setClientId("my-service-account");
        serviceAccount.setPublicClient(false);
        serviceAccount.setServiceAccountsEnabled(true);
        serviceAccount.setEnabled(true);
        serviceAccount.setSecret("secret");
        serviceAccount.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        serviceAccount.setFullScopeAllowed(false);

        // permission for client to client exchange to "target" client
        ClientPolicyRepresentation clientRep = new ClientPolicyRepresentation();
        clientRep.setName("to");
        clientRep.addClient(clientExchanger.getId());
        clientRep.addClient(legal.getId());
        clientRep.addClient(directLegal.getId());
        clientRep.addClient(noRefreshToken.getId());
        clientRep.addClient(serviceAccount.getId());

        ResourceServer server = management.realmResourceServer();
        Policy clientPolicy = management.authz().getStoreFactory().getPolicyStore().create(server, clientRep);
        management.clients().exchangeToPermission(target).addAssociatedPolicy(clientPolicy);

        // permission for user impersonation for a client

        ClientPolicyRepresentation clientImpersonateRep = new ClientPolicyRepresentation();
        clientImpersonateRep.setName("clientImpersonators");
        clientImpersonateRep.addClient(directLegal.getId());
        clientImpersonateRep.addClient(directPublic.getId());
        clientImpersonateRep.addClient(directUntrustedPublic.getId());
        clientImpersonateRep.addClient(directNoSecret.getId());
        server = management.realmResourceServer();
        Policy clientImpersonatePolicy = management.authz().getStoreFactory().getPolicyStore().create(server, clientImpersonateRep);
        management.users().setPermissionsEnabled(true);
        management.users().adminImpersonatingPermission().addAssociatedPolicy(clientImpersonatePolicy);
        management.users().adminImpersonatingPermission().setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);

        UserModel user = session.users().addUser(realm, "user");
        user.setEnabled(true);
        user.credentialManager().updateCredential(UserCredentialModel.password("password"));
        user.grantRole(exampleRole);
        user.grantRole(impersonateRole);

        UserModel bad = session.users().addUser(realm, "bad-impersonator");
        bad.setEnabled(true);
        bad.credentialManager().updateCredential(UserCredentialModel.password("password"));
    }

    public static void setUpUserImpersonatePermissions(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        AdminPermissionManagement management = AdminPermissions.management(session, realm);
        ResourceServer server = management.realmResourceServer();
        Policy userImpersonationPermission = management.users().userImpersonatedPermission();
        ClientPolicyRepresentation clientsAllowedToImpersonateRep = new ClientPolicyRepresentation();
        clientsAllowedToImpersonateRep.setName("clientsAllowedToImpersonateRep");
        clientsAllowedToImpersonateRep.addClient("direct-public");
        Policy clientsAllowedToImpersonate = management.authz().getStoreFactory().getPolicyStore().create(server, clientsAllowedToImpersonateRep);
        userImpersonationPermission.addAssociatedPolicy(clientsAllowedToImpersonate);
    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchange() throws Exception {
        testingClient.server().run(ClientTokenExchangeTest::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("client-exchanger");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        {
            response = oauth.doTokenExchange(TEST, accessToken, "target", "client-exchanger", "secret");
            String exchangedTokenString = response.getAccessToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals("client-exchanger", exchangedToken.getIssuedFor());
            Assert.assertEquals("target", exchangedToken.getAudience()[0]);
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "user");
            Assert.assertTrue(exchangedToken.getRealmAccess().isUserInRole("example"));
        }

        {
            response = oauth.doTokenExchange(TEST, accessToken, "target", "legal", "secret");

            String exchangedTokenString = response.getAccessToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals("legal", exchangedToken.getIssuedFor());
            Assert.assertEquals("target", exchangedToken.getAudience()[0]);
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "user");
            Assert.assertTrue(exchangedToken.getRealmAccess().isUserInRole("example"));
        }
        {
            response = oauth.doTokenExchange(TEST, accessToken, "target", "illegal", "secret");
            Assert.assertEquals(403, response.getStatusCode());
        }
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeUsingServiceAccount() throws Exception {
        testingClient.server().run(ClientTokenExchangeTest::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("my-service-account");
        OAuthClient.AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest("secret");
        String accessToken = response.getAccessToken();

        {
            response = oauth.doTokenExchange(TEST, accessToken, "target", "my-service-account", "secret");
            String exchangedTokenString = response.getAccessToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals("my-service-account", exchangedToken.getIssuedFor());
            Assert.assertEquals("target", exchangedToken.getAudience()[0]);
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "service-account-my-service-account");
        }
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeFromPublicClient() throws Exception {
        testingClient.server().run(ClientTokenExchangeTest::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("direct-public");
        OAuthClient.AuthorizationEndpointResponse authzResponse = oauth.doLogin("user", "password");
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(authzResponse.getCode(), "secret");

        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        response = oauth.doTokenExchange(TEST, accessToken, "target", "client-exchanger", "secret");
        String exchangedTokenString = response.getAccessToken();
        TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
        AccessToken exchangedToken = verifier.parse().getToken();
        Assert.assertEquals("client-exchanger", exchangedToken.getIssuedFor());
        Assert.assertEquals("target", exchangedToken.getAudience()[0]);
        Assert.assertEquals(exchangedToken.getPreferredUsername(), "user");
        Assert.assertTrue(exchangedToken.getRealmAccess().isUserInRole("example"));

        // can exchange to itself because the client is within the audience of the token issued to the public client
        response = oauth.doTokenExchange(TEST, accessToken, null, "client-exchanger", "secret");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        // can not exchange to itself because the client is not within the audience of the token issued to the public client
        response = oauth.doTokenExchange(TEST, accessToken, null, "direct-legal", "secret");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatusCode());

        response = oauth.doTokenExchange(TEST, accessToken, null, "direct-public", null);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
    }

    @Test
    @UncaughtServerErrorExpected
    public void testImpersonation() throws Exception {
        testingClient.server().run(ClientTokenExchangeTest::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("client-exchanger");

        Client httpClient = AdminClientUtil.createResteasyClient();

        WebTarget exchangeUrl = httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(TEST)
                .path("protocol/openid-connect/token");
        System.out.println("Exchange url: " + exchangeUrl.getUri().toString());

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = tokenResponse.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        // client-exchanger can impersonate from token "user" to user "impersonated-user"
        {
            Response response = exchangeUrl.request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("client-exchanger", "secret"))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                    .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                    .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")

                    ));
            org.junit.Assert.assertEquals(200, response.getStatus());
            AccessTokenResponse accessTokenResponse = response.readEntity(AccessTokenResponse.class);
            response.close();

            String exchangedTokenString = accessTokenResponse.getToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals("client-exchanger", exchangedToken.getIssuedFor());
            Assert.assertNull(exchangedToken.getAudience());
            Assert.assertEquals("impersonated-user", exchangedToken.getPreferredUsername());
            Assert.assertNull(exchangedToken.getRealmAccess());

            Object impersonatorRaw = exchangedToken.getOtherClaims().get("impersonator");
            Assert.assertThat(impersonatorRaw, instanceOf(Map.class));
            Map impersonatorClaim = (Map) impersonatorRaw;

            Assert.assertEquals(token.getSubject(), impersonatorClaim.get("id"));
            Assert.assertEquals("user", impersonatorClaim.get("username"));
        }

        // client-exchanger can impersonate from token "user" to user "impersonated-user" and to "target" client
        {
            Response response = exchangeUrl.request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("client-exchanger", "secret"))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                    .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                    .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")
                                    .param(OAuth2Constants.AUDIENCE, "target")

                    ));
            org.junit.Assert.assertEquals(200, response.getStatus());
            AccessTokenResponse accessTokenResponse = response.readEntity(AccessTokenResponse.class);
            response.close();

            String exchangedTokenString = accessTokenResponse.getToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals("client-exchanger", exchangedToken.getIssuedFor());
            Assert.assertEquals("target", exchangedToken.getAudience()[0]);
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "impersonated-user");
            Assert.assertTrue(exchangedToken.getRealmAccess().isUserInRole("example"));
        }

        try (Response response = exchangeUrl.request()
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.CLIENT_ID, "direct-public")
                                .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")

                ))) {
            org.junit.Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
            assertEquals("Client is not the holder of the token",
                    response.readEntity(OAuth2ErrorRepresentation.class).getErrorDescription());
        }

        try (Response response = exchangeUrl.request()
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.CLIENT_ID, "direct-public")
                                .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                .param(OAuth2Constants.AUDIENCE, "direct-public")
                                .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")

                ))) {
            org.junit.Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
            assertEquals("Client is not the holder of the token",
                    response.readEntity(OAuth2ErrorRepresentation.class).getErrorDescription());
        }

        try (Response response = exchangeUrl.request()
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.CLIENT_ID, "direct-public")
                                .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                .param(OAuth2Constants.AUDIENCE, "client-exchanger")
                                .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")

                ))) {
            org.junit.Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
            assertEquals("Client is not the holder of the token",
                    response.readEntity(OAuth2ErrorRepresentation.class).getErrorDescription());
        }
    }

    @UncaughtServerErrorExpected
    @Test
    public void testImpersonationUsingPublicClient() throws Exception {
        testingClient.server().run(ClientTokenExchangeTest::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("direct-public");

        Client httpClient = AdminClientUtil.createResteasyClient();

        OAuthClient.AuthorizationEndpointResponse authzResponse = oauth.doLogin("user", "password");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(authzResponse.getCode(), "secret");
        String accessToken = tokenResponse.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        WebTarget exchangeUrl = httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(TEST)
                .path("protocol/openid-connect/token");
        System.out.println("Exchange url: " + exchangeUrl.getUri().toString());

        Response response = exchangeUrl.request()
                .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("direct-public", null))
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")

                ));
        org.junit.Assert.assertEquals(200, response.getStatus());
        AccessTokenResponse accessTokenResponse = response.readEntity(AccessTokenResponse.class);
        response.close();

        String exchangedTokenString = accessTokenResponse.getToken();
        TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
        AccessToken exchangedToken = verifier.parse().getToken();
        Assert.assertEquals("direct-public", exchangedToken.getIssuedFor());
        Assert.assertEquals("impersonated-user", exchangedToken.getPreferredUsername());
        Assert.assertNull(exchangedToken.getRealmAccess());

        testingClient.server().run(ClientTokenExchangeTest::setUpUserImpersonatePermissions);
    }

    @UncaughtServerErrorExpected
    @Test
    public void testImpersonationUsingTokenIssuedToUntrustedPublicClient() throws Exception {
        testingClient.server().run(ClientTokenExchangeTest::setupRealm);
        testingClient.server().run(ClientTokenExchangeTest::setUpUserImpersonatePermissions);

        oauth.realm(TEST);
        oauth.clientId("direct-public-untrusted");

        Client httpClient = AdminClientUtil.createResteasyClient();

        OAuthClient.AuthorizationEndpointResponse authzResponse = oauth.doLogin("user", "password");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(authzResponse.getCode(), "secret");
        String accessToken = tokenResponse.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        WebTarget exchangeUrl = httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(TEST)
                .path("protocol/openid-connect/token");
        System.out.println("Exchange url: " + exchangeUrl.getUri().toString());

        Response response = exchangeUrl.request()
                .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("direct-public-untrusted", null))
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")

                ));
        org.junit.Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

        oauth.idTokenHint(tokenResponse.getIdToken()).openLogout();
        oauth.clientId("direct-public");
        authzResponse = oauth.doLogin("user", "password");
        tokenResponse = oauth.doAccessTokenRequest(authzResponse.getCode(), "secret");
        accessToken = tokenResponse.getAccessToken();

        response = exchangeUrl.request()
                .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("direct-public", null))
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")

                ));
        org.junit.Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @UncaughtServerErrorExpected
    public void testBadImpersonator() throws Exception {
        testingClient.server().run(ClientTokenExchangeTest::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("client-exchanger");

        Client httpClient = AdminClientUtil.createResteasyClient();

        WebTarget exchangeUrl = httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(TEST)
                .path("protocol/openid-connect/token");
        System.out.println("Exchange url: " + exchangeUrl.getUri().toString());

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doGrantAccessTokenRequest("secret", "bad-impersonator", "password");
        String accessToken = tokenResponse.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "bad-impersonator");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        // test that user does not have impersonator permission
        {
            Response response = exchangeUrl.request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("client-exchanger", "secret"))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.SUBJECT_TOKEN, accessToken)
                                    .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                    .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")

                    ));
            org.junit.Assert.assertEquals(403, response.getStatus());
            response.close();
        }


    }

    @Test
    @UncaughtServerErrorExpected
    public void testDirectImpersonation() throws Exception {
        testingClient.server().run(ClientTokenExchangeTest::setupRealm);
        Client httpClient = AdminClientUtil.createResteasyClient();

        WebTarget exchangeUrl = httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(TEST)
                .path("protocol/openid-connect/token");
        System.out.println("Exchange url: " + exchangeUrl.getUri().toString());

        // direct-exchanger can impersonate from token "user" to user "impersonated-user"
        // see https://issues.redhat.com/browse/KEYCLOAK-5492
        {
            Response response = exchangeUrl.request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("direct-exchanger", "secret"))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")

                    ));
            Assert.assertEquals(200, response.getStatus());
            AccessTokenResponse accessTokenResponse = response.readEntity(AccessTokenResponse.class);
            response.close();

            String exchangedTokenString = accessTokenResponse.getToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals("direct-exchanger", exchangedToken.getIssuedFor());
            Assert.assertNull(exchangedToken.getAudience());
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "impersonated-user");
            Assert.assertNull(exchangedToken.getRealmAccess());
        }

        // direct-legal can impersonate from token "user" to user "impersonated-user" and to "target" client
        {
            Response response = exchangeUrl.request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("direct-legal", "secret"))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")
                                    .param(OAuth2Constants.AUDIENCE, "target")

                    ));
            Assert.assertEquals(200, response.getStatus());
            AccessTokenResponse accessTokenResponse = response.readEntity(AccessTokenResponse.class);
            response.close();

            String exchangedTokenString = accessTokenResponse.getToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
            AccessToken exchangedToken = verifier.parse().getToken();
            Assert.assertEquals("direct-legal", exchangedToken.getIssuedFor());
            Assert.assertEquals("target", exchangedToken.getAudience()[0]);
            Assert.assertEquals(exchangedToken.getPreferredUsername(), "impersonated-user");
            Assert.assertTrue(exchangedToken.getRealmAccess().isUserInRole("example"));
        }

        // direct-public fails impersonation
        {
            Response response = exchangeUrl.request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("direct-public", "secret"))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")
                                    .param(OAuth2Constants.AUDIENCE, "target")

                    ));
            Assert.assertEquals(403, response.getStatus());
            response.close();
        }

        // direct-no-secret fails impersonation
        {
            Response response = exchangeUrl.request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("direct-no-secret", "secret"))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")
                                    .param(OAuth2Constants.AUDIENCE, "target")

                    ));
            Assert.assertTrue(response.getStatus() >= 400);
            response.close();
        }
    }

    @Test
    @UncaughtServerErrorExpected
    public void testExchangeNoRefreshToken() throws Exception {
        testingClient.server().run(ClientTokenExchangeTest::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("client-exchanger");

        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm(TEST), "no-refresh-token");
        ClientRepresentation clientRepresentation = client.toRepresentation();
        clientRepresentation.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, "false");
        client.update(clientRepresentation);

        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();

        {
            response = oauth.doTokenExchange(TEST, accessToken, "target", "client-exchanger", "secret");
            String exchangedTokenString = response.getAccessToken();
            String refreshTokenString = response.getRefreshToken();
            assertNotNull(exchangedTokenString);
            assertNotNull(refreshTokenString);
        }

        {
            response = oauth.doTokenExchange(TEST, accessToken, "target", "no-refresh-token", "secret");
            String exchangedTokenString = response.getAccessToken();
            String refreshTokenString = response.getRefreshToken();
            assertNotNull(exchangedTokenString);
            assertNull(refreshTokenString);
        }
        clientRepresentation.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, "true");
        client.update(clientRepresentation);
    }

    @Test
    public void testClientExchangeToItself() throws Exception {
        testingClient.server().run(ClientTokenExchangeTest::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("client-exchanger");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        response = oauth.doTokenExchange(TEST, accessToken, null, "client-exchanger", "secret");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        response = oauth.doTokenExchange(TEST, accessToken, "client-exchanger", "client-exchanger", "secret");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
    }

    @Test
    public void testClientExchange() throws Exception {
        testingClient.server().run(ClientTokenExchangeTest::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("direct-legal");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        response = oauth.doTokenExchange(TEST, accessToken, "target", "direct-legal", "secret");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
    }

    @Test
    public void testPublicClientNotAllowed() throws Exception {
        testingClient.server().run(ClientTokenExchangeTest::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("direct-legal");
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();
        TokenVerifier<AccessToken> accessTokenVerifier = TokenVerifier.create(accessToken, AccessToken.class);
        AccessToken token = accessTokenVerifier.parse().getToken();
        Assert.assertEquals(token.getPreferredUsername(), "user");
        Assert.assertTrue(token.getRealmAccess() == null || !token.getRealmAccess().isUserInRole("example"));

        // public client has no permission to exchange with the client direct-legal to which the token was issued for
        // if not set, the audience is calculated based on the client to which the token was issued for
        response = oauth.doTokenExchange(TEST, accessToken, null, "direct-public", null);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatusCode());
        assertEquals("Client is not the holder of the token", response.getErrorDescription());

        // public client has no permission to exchange
        response = oauth.doTokenExchange(TEST, accessToken, "target", "direct-public", null);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatusCode());
        assertEquals("Client is not the holder of the token", response.getErrorDescription());

        response = oauth.doTokenExchange(TEST, accessToken, "direct-legal", "direct-public", null);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatusCode());
        assertEquals("Client is not the holder of the token", response.getErrorDescription());

        // public client can not exchange a token to itself if the token was issued to another client
        response = oauth.doTokenExchange(TEST, accessToken, "direct-public", "direct-public", null);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatusCode());
        assertEquals("Client is not the holder of the token", response.getErrorDescription());

        // client with access to exchange
        response = oauth.doTokenExchange(TEST, accessToken, "target", "client-exchanger", "secret");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        // client must pass the audience because the client has no permission to exchange with the calculated audience (direct-legal)
        response = oauth.doTokenExchange(TEST, accessToken, null, "client-exchanger", "secret");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatusCode());
        assertEquals("Client is not within the token audience", response.getErrorDescription());
    }

    private static void addDirectExchanger(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        RoleModel exampleRole = realm.addRole("example");
        AdminPermissionManagement management = AdminPermissions.management(session, realm);

        ClientModel target = realm.addClient("target");
        target.setName("target");
        target.setClientId("target");
        target.setDirectAccessGrantsEnabled(true);
        target.setEnabled(true);
        target.setSecret("secret");
        target.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        target.setFullScopeAllowed(false);
        target.addScopeMapping(exampleRole);

        ClientModel directExchanger = realm.addClient("direct-exchanger");
        directExchanger.setName("direct-exchanger");
        directExchanger.setClientId("direct-exchanger");
        directExchanger.setPublicClient(false);
        directExchanger.setDirectAccessGrantsEnabled(true);
        directExchanger.setEnabled(true);
        directExchanger.setSecret("secret");
        directExchanger.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        directExchanger.setFullScopeAllowed(false);

        // permission for client to client exchange to "target" client
        management.clients().setPermissionsEnabled(target, true);

        ClientPolicyRepresentation clientImpersonateRep = new ClientPolicyRepresentation();
        clientImpersonateRep.setName("clientImpersonatorsDirect");
        clientImpersonateRep.addClient(directExchanger.getId());

        ResourceServer server = management.realmResourceServer();
        Policy clientImpersonatePolicy = management.authz().getStoreFactory().getPolicyStore().create(server, clientImpersonateRep);
        management.users().setPermissionsEnabled(true);
        management.users().adminImpersonatingPermission().addAssociatedPolicy(clientImpersonatePolicy);
        management.users().adminImpersonatingPermission().setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);

        UserModel impersonatedUser = session.users().addUser(realm, "impersonated-user");
        impersonatedUser.setEnabled(true);
        impersonatedUser.credentialManager().updateCredential(UserCredentialModel.password("password"));
        impersonatedUser.grantRole(exampleRole);
    }

    private static void removeDirectExchanger(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        realm.removeClient(realm.getClientByClientId("direct-exchanger").getId());
        realm.removeClient(realm.getClientByClientId("target").getId());
        realm.removeRole(realm.getRole("example"));
        session.users().removeUser(realm, session.users().getUserByUsername(realm, "impersonated-user"));
    }

    private Response checkTokenExchange() {
        Client httpClient = AdminClientUtil.createResteasyClient();
        WebTarget exchangeUrl = httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(TEST)
                .path("protocol/openid-connect/token");

        Response response = exchangeUrl.request()
                .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader("direct-exchanger", "secret"))
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.REQUESTED_SUBJECT, "impersonated-user")

                ));
        return response;
    }
}
