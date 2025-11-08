/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.broker;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.broker.oidc.mappers.UserAttributeMapper;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.jpa.JpaRealmProviderFactory;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.services.resources.admin.fgap.AdminPermissionManagement;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeatures;
import org.keycloak.testsuite.updaters.IdentityProviderAttributeUpdater;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.testsuite.util.broker.OIDCIdentityProviderConfigRep;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.util.BasicAuthHelper;

import org.junit.Test;

import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_ALIAS;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createHardcodedClaim;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Test for identity-provider token exchange scenarios. Base for tests of token-exchange V1
 */
@EnableFeatures({@EnableFeature(Profile.Feature.TOKEN_EXCHANGE), @EnableFeature(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)})
public class KcOidcBrokerTokenExchangeTest extends AbstractInitializedBaseBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }

    @Test
    public void testExternalInternalTokenExchange() throws Exception {
        assertExternalToInternalExchange(bc.getIDPAlias(), true, false);
    }

    // Tests token-exchange works when user is already linked to the IDP
    @Test
    public void testExternalInternalTokenExchangeWithExistingUser() throws Exception {
        assertExternalToInternalExchange(bc.getIDPAlias(), true, false);

        ClientRepresentation brokerApp = realmsResouce().realm(bc.providerRealmName())
                .clients()
                .findByClientId("brokerapp").get(0);
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.realm(bc.providerRealmName()).client(brokerApp.getClientId(), brokerApp.getSecret()).doPasswordGrantRequest(bc.getUserLogin(), bc.getUserPassword());
        assertThat(tokenResponse.getIdToken(), notNullValue());

        try (Client httpClient = AdminClientUtil.createResteasyClient()) {
            WebTarget exchangeUrl = getConsumerTokenEndpoint(httpClient);
            try (Response response = sendExternalInternalTokenExchangeRequest(exchangeUrl, tokenResponse.getIdToken(), OAuth2Constants.ID_TOKEN_TYPE)) {
                assertThat(response.getStatus(), equalTo(200));
            }
        }
    }

    @Test
    public void testExternalInternalTokenExchangeUsingIssuer() throws Exception {
        RealmResource consumerRealm = realmsResouce().realm(bc.consumerRealmName());
        IdentityProviderRepresentation broker = consumerRealm.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        assertExternalToInternalExchange(broker.getConfig().get(OIDCIdentityProviderConfigRep.ISSUER), true, false);
    }

    @Test
    public void testExternalInternalTokenExchangeWithJwtAccessTokenAndUserInfoEndpoint() throws Exception {
        assertExternalToInternalExchange(bc.getIDPAlias(), false, true);
    }

    private void assertExternalToInternalExchange(String subjectIssuer, boolean idToken, boolean userInfo) throws Exception {
        RealmResource providerRealm = realmsResouce().realm(bc.providerRealmName());
        ClientsResource clients = providerRealm.clients();
        ClientRepresentation brokerApp = clients.findByClientId("brokerapp").get(0);
        brokerApp.setDirectAccessGrantsEnabled(true);
        ClientResource brokerAppResource = providerRealm.clients().get(brokerApp.getId());
        brokerAppResource.update(brokerApp);
        brokerAppResource.getProtocolMappers().createMapper(createHardcodedClaim("hard-coded", "hard-coded", "hard-coded", "String", true, idToken, true)).close();

        IdentityProviderMapperRepresentation hardCodedSessionNoteMapper = new IdentityProviderMapperRepresentation();
        hardCodedSessionNoteMapper.setName("hard-coded");
        hardCodedSessionNoteMapper.setIdentityProviderAlias(bc.getIDPAlias());
        hardCodedSessionNoteMapper.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
        hardCodedSessionNoteMapper.setConfig(Map.of(
                IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.toString(),
                UserAttributeMapper.USER_ATTRIBUTE, "mapped-from-claim",
                UserAttributeMapper.CLAIM, "hard-coded"));

        RealmResource consumerRealm = realmsResouce().realm(bc.consumerRealmName());
        IdentityProviderResource identityProviderResource = consumerRealm.identityProviders().get(bc.getIDPAlias());

        IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();
        representation.getConfig().put("isAccessTokenJWT", Boolean.toString(!idToken));
        representation.getConfig().put("validateSignature", Boolean.toString(!userInfo));
        // if auth.server.host != auth.server.host2 we need to update the issuer in the IDP config
        if (!representation.getConfig().get("issuer").startsWith(ServerURLs.getAuthServerContextRoot())) {
            representation.getConfig().put("issuer", ServerURLs.getAuthServerContextRoot() + "/auth/realms/provider");
        }
        if (userInfo) {
            representation.getConfig().put("userInfoUrl", ServerURLs.getAuthServerContextRoot() + "/auth/realms/provider/protocol/openid-connect/userinfo");
        }
        identityProviderResource.update(representation);

        identityProviderResource.addMapper(hardCodedSessionNoteMapper).close();

        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.realm(bc.providerRealmName()).client(brokerApp.getClientId(), brokerApp.getSecret()).doPasswordGrantRequest(bc.getUserLogin(), bc.getUserPassword());
        assertThat(tokenResponse.getIdToken(), notNullValue());

        testingClient.server(BrokerTestConstants.REALM_CONS_NAME).run(KcOidcBrokerTokenExchangeTest::setupRealm);

        ClientRepresentation client = consumerRealm.clients().findByClientId("test-app").get(0);

        try (Client httpClient = AdminClientUtil.createResteasyClient()) {
            WebTarget exchangeUrl = getConsumerTokenEndpoint(httpClient);
            // test user info validation.
            AccessTokenResponse externalToInternalTokenResponse;
            String subjectToken = idToken ? tokenResponse.getIdToken() : tokenResponse.getAccessToken();
            String subjectTokenType = idToken ? OAuth2Constants.ID_TOKEN_TYPE : OAuth2Constants.ACCESS_TOKEN_TYPE;
            try (Response response = sendExternalInternalTokenExchangeRequest(exchangeUrl, subjectToken, subjectTokenType)) {
                assertThat(response.getStatus(), equalTo(200));
                externalToInternalTokenResponse = response.readEntity(AccessTokenResponse.class);
                UserRepresentation user = consumerRealm.users().search(bc.getUserLogin()).get(0);
                getCleanup(bc.consumerRealmName()).addUserId(user.getId());
                assertThat(user.getAttributes().get("mapped-from-claim").get(0), equalTo("hard-coded"));
            }

            try (Response response = exchangeUrl.request()
                    .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader(
                            client.getClientId(), client.getSecret()))
                    .post(Entity.form(
                            new Form()
                                    .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                    .param(OAuth2Constants.SUBJECT_TOKEN, externalToInternalTokenResponse.getToken())
                                    .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                                    .param(OAuth2Constants.REQUESTED_ISSUER, bc.getIDPAlias())
                                    .param(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID)

                    ))) {
                assertThat(response.getStatus(), equalTo(200));
                AccessTokenResponse internalToExternalTokenResponse = response.readEntity(AccessTokenResponse.class);
            }
        }
    }

    @Test
    public void testWithLinkedFederationProviderTokenExchangeInternalToExternalWithStore() throws IOException {
        testingClient.server(bc.consumerRealmName()).run(KcOidcBrokerTokenExchangeTest::setupRealm);
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(
                realmsResouce().realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias()))
                .setStoreToken(true)
                .update()) {
            testWithLinkedFederationProviderTokenExchangeInternalToExternal(true);
        }
    }

    @Test
    public void testWithLinkedFederationProviderTokenExchangeInternalToExternalWithoutStore() throws IOException {
        testingClient.server(bc.consumerRealmName()).run(KcOidcBrokerTokenExchangeTest::setupRealm);
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(
                realmsResouce().realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias()))
                .setStoreToken(false)
                .update()) {
            testWithLinkedFederationProviderTokenExchangeInternalToExternal(false);
        }
    }

    public void testWithLinkedFederationProviderTokenExchangeInternalToExternal(boolean storeToken) {

        final RealmResource consumerRealm = realmsResouce().realm(bc.consumerRealmName());
        final int expires = realmsResouce().realm(bc.providerRealmName()).toRepresentation().getAccessTokenLifespan();
        final ClientRepresentation brokerApp = ApiUtil.findClientByClientId(consumerRealm, "broker-app").toRepresentation();

        logInAsUserInIDPForFirstTimeAndAssertSuccess();
        final String code = oauth.parseLoginResponse().getCode();
        oauth.client(brokerApp.getClientId(), brokerApp.getSecret());
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        assertThat(tokenResponse.getError(), nullValue());
        assertThat(tokenResponse.getAccessToken(), notNullValue());

        String realmName = bc.consumerRealmName();
        String userName = bc.getUserLogin();
        String idpAlias = bc.getIDPAlias();

        String oldTokenFromDatabase = testingClient.server().fetch(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserByUsername(realm, userName);
            return session.getProvider(UserProvider.class, JpaRealmProviderFactory.PROVIDER_ID).getFederatedIdentity(realm, user, idpAlias).getToken();
        }, String.class);

        setTimeOffset(expires + 10);

        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertThat(tokenResponse.getError(), nullValue());
        assertThat(tokenResponse.getAccessToken(), notNullValue());

        exchangeToIdP(brokerApp, tokenResponse.getAccessToken(), expires);

        if (storeToken) {
            /* Ensure that the tokens are persisted to the database as they would otherwise not be present on other nodes or on server restart */
            String newTokenFromDatabase = testingClient.server().fetch(session -> {
                RealmModel realm = session.realms().getRealmByName(realmName);
                UserModel user = session.users().getUserByUsername(realm, userName);
                return session.getProvider(UserProvider.class, JpaRealmProviderFactory.PROVIDER_ID).getFederatedIdentity(realm, user, idpAlias).getToken();
            }, String.class);

            assertThat(newTokenFromDatabase, not(equalTo(oldTokenFromDatabase)));
        }

    }


    @Test
    public void testSupportedTokenTypesWhenValidatingSubjectToken() throws Exception {
        testingClient.server(BrokerTestConstants.REALM_CONS_NAME).run(KcOidcBrokerTokenExchangeTest::setupRealm);
        RealmResource providerRealm = realmsResouce().realm(bc.providerRealmName());
        ClientsResource clients = providerRealm.clients();
        ClientRepresentation brokerApp = clients.findByClientId("brokerapp").get(0);
        brokerApp.setDirectAccessGrantsEnabled(true);
        ClientResource brokerAppResource = providerRealm.clients().get(brokerApp.getId());
        brokerAppResource.update(brokerApp);
        RealmResource consumerRealm = realmsResouce().realm(bc.consumerRealmName());
        IdentityProviderResource identityProviderResource = consumerRealm.identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation idpRep = identityProviderResource.toRepresentation();
        idpRep.getConfig().put("disableUserInfo", "true");
        identityProviderResource.update(idpRep);
        getCleanup().addCleanup(() -> {
            idpRep.getConfig().put("disableUserInfo", "false");
            identityProviderResource.update(idpRep);
        });

        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.realm(bc.providerRealmName()).client(brokerApp.getClientId(), brokerApp.getSecret()).doPasswordGrantRequest(bc.getUserLogin(), bc.getUserPassword());
        assertThat(tokenResponse.getIdToken(), notNullValue());
        String idTokenString = tokenResponse.getIdToken();
        oauth.realm(bc.providerRealmName());
        oauth.logoutForm().idTokenHint(idTokenString)
                .postLogoutRedirectUri(oauth.APP_AUTH_ROOT).open();
        String logoutToken = testingClient.testApp().getBackChannelRawLogoutToken();
        Assert.assertNotNull(logoutToken);

        Client httpClient = AdminClientUtil.createResteasyClient();
        try {
            WebTarget exchangeUrl = getConsumerTokenEndpoint(httpClient);
            // test user info validation.
            try (Response response = sendExternalInternalTokenExchangeRequest(exchangeUrl, logoutToken, OAuth2Constants.JWT_TOKEN_TYPE)) {
                assertThat(response.getStatus(), equalTo(Status.BAD_REQUEST.getStatusCode()));
            }
        } finally {
            httpClient.close();
        }
    }

    @Test
    public void testIgnoredTokenTypesValidationWhenExplicitlyConfigured() throws Exception {
        testingClient.server(BrokerTestConstants.REALM_CONS_NAME).run(KcOidcBrokerTokenExchangeTest::setupRealm);
        RealmResource providerRealm = realmsResouce().realm(bc.providerRealmName());
        ClientsResource clients = providerRealm.clients();
        ClientRepresentation brokerApp = clients.findByClientId("brokerapp").get(0);
        brokerApp.setDirectAccessGrantsEnabled(true);
        ClientResource brokerAppResource = providerRealm.clients().get(brokerApp.getId());
        brokerAppResource.update(brokerApp);
        RealmResource consumerRealm = realmsResouce().realm(bc.consumerRealmName());
        IdentityProviderResource identityProviderResource = consumerRealm.identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation idpRep = identityProviderResource.toRepresentation();
        idpRep.getConfig().put("disableUserInfo", "true");
        idpRep.getConfig().put("disableTypeClaimCheck", "true");
        // if auth.server.host != auth.server.host2 we need to update the issuer in the IDP config
        if (!idpRep.getConfig().get("issuer").startsWith(ServerURLs.getAuthServerContextRoot())) {
            idpRep.getConfig().put("issuer", ServerURLs.getAuthServerContextRoot() + "/auth/realms/provider");
        }
        identityProviderResource.update(idpRep);
        getCleanup().addCleanup(() -> {
            idpRep.getConfig().put("disableUserInfo", "false");
            idpRep.getConfig().put("disableTypeClaimCheck", "false");
            identityProviderResource.update(idpRep);
        });

        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.realm(bc.providerRealmName()).client(brokerApp.getClientId(), brokerApp.getSecret()).doPasswordGrantRequest(bc.getUserLogin(), bc.getUserPassword());
        assertThat(tokenResponse.getIdToken(), notNullValue());
        String idTokenString = tokenResponse.getIdToken();
        oauth.realm(bc.providerRealmName());
        oauth.logoutForm().idTokenHint(idTokenString)
                .postLogoutRedirectUri(oauth.APP_AUTH_ROOT).open();
        String logoutToken = testingClient.testApp().getBackChannelRawLogoutToken();
        Assert.assertNotNull(logoutToken);

        Client httpClient = AdminClientUtil.createResteasyClient();
        try {
            WebTarget exchangeUrl = getConsumerTokenEndpoint(httpClient);
            // test user info validation.
            try (Response response = sendExternalInternalTokenExchangeRequest(exchangeUrl, logoutToken, OAuth2Constants.JWT_TOKEN_TYPE)) {
                assertThat(response.getStatus(), equalTo(Status.OK.getStatusCode()));
            }
        } finally {
            httpClient.close();
        }
    }

    @Test
    public void testInternalExternalTokenExchangeSessionToken() throws Exception {
        testingClient.server(bc.consumerRealmName()).run(KcOidcBrokerTokenExchangeTest::setupRealm);

        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(
                        realmsResouce().realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias()))
                .setStoreToken(false)
                .update()) {
            testInternalExternalTokenExchange();
        }
    }

    @Test
    public void testInternalExternalTokenExchangeStoredToken() throws Exception {
        testingClient.server(bc.consumerRealmName()).run(KcOidcBrokerTokenExchangeTest::setupRealm);

        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(
                        realmsResouce().realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias()))
                .setStoreToken(true)
                .update()) {
            testInternalExternalTokenExchange();
        }
    }

    private static void setupRealm(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        IdentityProviderModel idp = session.identityProviders().getByAlias(IDP_OIDC_ALIAS);
        org.junit.Assert.assertNotNull(idp);

        ClientModel client = realm.addClient("test-app");
        client.setClientId("test-app");
        client.setPublicClient(false);
        client.setDirectAccessGrantsEnabled(true);
        client.setEnabled(true);
        client.setSecret("secret");
        client.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        client.setFullScopeAllowed(false);
        client.setRedirectUris(Set.of(OAuthClient.AUTH_SERVER_ROOT + "/*"));

        ClientModel brokerApp = realm.getClientByClientId("broker-app");

        AdminPermissionManagement management = AdminPermissions.management(session, realm);
        management.idps().setPermissionsEnabled(idp, true);
        ClientPolicyRepresentation clientRep = new ClientPolicyRepresentation();
        clientRep.setName("toIdp");
        clientRep.addClient(client.getId(), brokerApp.getId());
        ResourceServer server = management.realmResourceServer();
        Policy clientPolicy = management.authz().getStoreFactory().getPolicyStore().create(server, clientRep);
        management.idps().exchangeToPermission(idp).addAssociatedPolicy(clientPolicy);

        realm = session.realms().getRealmByName(BrokerTestConstants.REALM_PROV_NAME);
        client = realm.getClientByClientId("brokerapp");
        client.addRedirectUri(OAuthClient.APP_ROOT + "/auth");
        client.setAttribute(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, OAuthClient.APP_ROOT + "/admin/backchannelLogout");
    }

    private void testInternalExternalTokenExchange() throws Exception {
        final RealmResource consumerRealm = realmsResouce().realm(bc.consumerRealmName());
        final int expires = realmsResouce().realm(bc.providerRealmName()).toRepresentation().getAccessTokenLifespan();
        final ClientRepresentation brokerApp = ApiUtil.findClientByClientId(consumerRealm, "broker-app").toRepresentation();

        logInAsUserInIDPForFirstTimeAndAssertSuccess();
        final String code = oauth.parseLoginResponse().getCode();
        oauth.client(brokerApp.getClientId(), brokerApp.getSecret());
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        assertThat(tokenResponse.getError(), nullValue());
        assertThat(tokenResponse.getAccessToken(), notNullValue());

        exchangeToIdP(brokerApp, tokenResponse.getAccessToken(), expires);

        setTimeOffset(expires - IdentityProviderModel.DEFAULT_MIN_VALIDITY_TOKEN);

        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertThat(tokenResponse.getError(), nullValue());
        assertThat(tokenResponse.getAccessToken(), notNullValue());

        exchangeToIdP(brokerApp, tokenResponse.getAccessToken(), expires);
    }

    private WebTarget getConsumerTokenEndpoint(Client httpClient) {
        return httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(bc.consumerRealmName())
                .path("protocol/openid-connect/token");
    }

    private Response sendExternalInternalTokenExchangeRequest(WebTarget exchangeUrl, String subjectToken, String subjectTokenType) {
        return exchangeUrl.request()
                .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader(
                        "test-app", "secret"))
                .post(Entity.form(
                        new Form()
                                .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                                .param(OAuth2Constants.SUBJECT_TOKEN, subjectToken)
                                .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, subjectTokenType)
                                .param(OAuth2Constants.SUBJECT_ISSUER, bc.getIDPAlias())
                                .param(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID)

                ));
    }

    private void exchangeToIdP(ClientRepresentation brokerApp, String subjectToken, long expires) {
        try (Client httpClient = AdminClientUtil.createResteasyClient();
             Response response = httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms").path(bc.consumerRealmName()).path("protocol/openid-connect/token")
                .request()
                .header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader(brokerApp.getClientId(), brokerApp.getSecret()))
                .post(Entity.form(new Form()
                        .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)
                        .param(OAuth2Constants.SUBJECT_TOKEN, subjectToken)
                        .param(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE)
                        .param(OAuth2Constants.REQUESTED_ISSUER, bc.getIDPAlias()))
                )) {
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            AccessTokenResponse exchangeResponse = response.readEntity(AccessTokenResponse.class);
            assertThat(exchangeResponse.getError(), nullValue());
            assertThat(exchangeResponse.getToken(), notNullValue());
            assertThat(exchangeResponse.getExpiresIn(), greaterThan((long) IdentityProviderModel.DEFAULT_MIN_VALIDITY_TOKEN));
            assertThat(exchangeResponse.getExpiresIn(), lessThanOrEqualTo(expires));
        }
    }
}
