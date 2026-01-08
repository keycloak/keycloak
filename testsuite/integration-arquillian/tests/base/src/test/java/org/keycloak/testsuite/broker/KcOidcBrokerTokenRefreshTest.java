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

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.mappers.UserAttributeMapper;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
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
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeatures;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.util.BasicAuthHelper;

import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_ALIAS;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createHardcodedClaim;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Test for identity-provider token exchange scenarios verifying session notes update.
 */
@EnableFeatures({@EnableFeature(Profile.Feature.TOKEN_EXCHANGE), @EnableFeature(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)})
public class KcOidcBrokerTokenRefreshTest extends AbstractInitializedBaseBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }

    @Test
    public void testExternalInternalTokenExchangeSessionNotes() throws Exception {
        assertExternalToInternalExchangeSessionNotes(bc.getIDPAlias(), true, false);
    }

    private void assertExternalToInternalExchangeSessionNotes(String subjectIssuer, boolean idToken, boolean userInfo) throws Exception {
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

        testingClient.server(BrokerTestConstants.REALM_CONS_NAME).run(KcOidcBrokerTokenRefreshTest::setupRealm);

        try (Client httpClient = AdminClientUtil.createResteasyClient()) {
            WebTarget exchangeUrl = getConsumerTokenEndpoint(httpClient);
            String subjectToken = idToken ? tokenResponse.getIdToken() : tokenResponse.getAccessToken();
            String subjectTokenType = idToken ? OAuth2Constants.ID_TOKEN_TYPE : OAuth2Constants.ACCESS_TOKEN_TYPE;
            
            final String userLogin = bc.getUserLogin();
            final String realmName = bc.consumerRealmName();
            
            try (Response response = sendExternalInternalTokenExchangeRequest(exchangeUrl, subjectToken, subjectTokenType)) {
                assertThat(response.getStatus(), equalTo(200));
                
                // Verify the session notes
                testingClient.server(realmName).run(session -> {
                    RealmModel realm = session.realms().getRealmByName(realmName);
                    if (realm == null) throw new RuntimeException("Realm is null");
                    
                    if (session.users() == null) throw new RuntimeException("Users provider is null");
                    UserModel user = session.users().getUserByUsername(realm, userLogin);
                    
                    if (user == null) {
                        throw new RuntimeException("User not found: " + userLogin);
                    }
                    
                    if (session.sessions() == null) throw new RuntimeException("Sessions provider is null");
                    
                    // Search for any session of this user
                    UserSessionModel userSession = session.sessions().getUserSessionsStream(realm, user).findFirst().orElse(null);
                    
                    if (userSession == null) {
                        throw new RuntimeException("User session not found for user: " + userLogin);
                    }
                    
                    // Use string literals to avoid class loading issues
                    String fedAccessToken = userSession.getNote("FEDERATED_ACCESS_TOKEN");
                    String fedIdToken = userSession.getNote("FEDERATED_ID_TOKEN");
                    String exchangeProvider = userSession.getNote("EXCHANGE_PROVIDER");

                    // Assertions are run on server side
                    if (fedAccessToken == null || !fedAccessToken.equals(subjectToken)) {
                        throw new AssertionError("FEDERATED_ACCESS_TOKEN not set or incorrect. Expected: " + subjectToken + ", Actual: " + fedAccessToken);
                    }
                    if (idToken && (fedIdToken == null || !fedIdToken.equals(subjectToken))) {
                        throw new AssertionError("FEDERATED_ID_TOKEN not set or incorrect. Expected: " + subjectToken + ", Actual: " + fedIdToken);
                    }
                     if (exchangeProvider == null || !exchangeProvider.equals(IDP_OIDC_ALIAS)) {
                        throw new AssertionError("EXCHANGE_PROVIDER not set or incorrect. Expected: " + IDP_OIDC_ALIAS + ", Actual: " + exchangeProvider);
                    }

                });
            }
        }
    }

    private static void setupRealm(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        IdentityProviderModel idp = session.identityProviders().getByAlias(IDP_OIDC_ALIAS);
        
        if (idp == null) {
            throw new RuntimeException("Identity provider not found: " + IDP_OIDC_ALIAS);
        }

        ClientModel client = realm.addClient("test-app");
        client.setClientId("test-app");
        client.setPublicClient(false);
        client.setDirectAccessGrantsEnabled(true);
        client.setEnabled(true);
        client.setSecret("secret");
        client.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        client.setFullScopeAllowed(false);
        String authServerRoot = ServerURLs.getAuthServerContextRoot();
        client.setRedirectUris(Set.of(authServerRoot + "/*"));

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
        String appRoot = authServerRoot + "/realms/master/app";
        client.addRedirectUri(appRoot + "/auth");
        client.setAttribute(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, appRoot + "/admin/backchannelLogout");
    }

    private WebTarget getConsumerTokenEndpoint(Client httpClient) {
        return httpClient.target(ServerURLs.getAuthServerContextRoot() + "/auth")
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
}
