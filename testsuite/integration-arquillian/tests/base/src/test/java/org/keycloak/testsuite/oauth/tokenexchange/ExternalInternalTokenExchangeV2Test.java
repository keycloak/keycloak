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

package org.keycloak.testsuite.oauth.tokenexchange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.broker.AbstractInitializedBaseBrokerTest;
import org.keycloak.testsuite.broker.BrokerConfiguration;
import org.keycloak.testsuite.broker.BrokerTestConstants;
import org.keycloak.testsuite.broker.KcOidcBrokerConfiguration;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.util.BasicAuthHelper;

import org.junit.Test;

import static org.keycloak.broker.oidc.OAuth2IdentityProviderConfig.TOKEN_ENDPOINT_URL;
import static org.keycloak.broker.oidc.OAuth2IdentityProviderConfig.TOKEN_INTROSPECTION_URL;
import static org.keycloak.testsuite.broker.BrokerTestConstants.CLIENT_ID;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_CONS_NAME;
import static org.keycloak.testsuite.broker.BrokerTestTools.getProviderRoot;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;

/**
 * Test for external-internal token exchange using token_exchange_external_internal:v2
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@EnableFeature(Profile.Feature.TOKEN_EXCHANGE_EXTERNAL_INTERNAL_V2)
public class ExternalInternalTokenExchangeV2Test extends AbstractInitializedBaseBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration() {

            @Override
            protected void applyDefaultConfiguration(Map<String, String> config, IdentityProviderSyncMode syncMode) {
                super.applyDefaultConfiguration(config, syncMode);
                config.put(TOKEN_INTROSPECTION_URL, config.get(TOKEN_ENDPOINT_URL) + "/introspect");
            }

            @Override
            public List<ClientRepresentation> createProviderClients() {
                List<ClientRepresentation> providerClients = super.createProviderClients();
                ClientRepresentation brokerApp = providerClients.stream()
                        .filter(client -> CLIENT_ID.equals(client.getClientId()))
                        .findFirst().get();
                brokerApp.setDirectAccessGrantsEnabled(true);

                ClientRepresentation client2 = createProviderClientWithAudienceMapper("client-with-brokerapp-audience", CLIENT_ID);
                ClientRepresentation client3 = createProviderClientWithAudienceMapper("client-with-consumer-realm-issuer-audience", getProviderRoot() + "/auth/realms/" + REALM_CONS_NAME);
                ClientRepresentation client4 = createProviderClientWithAudienceMapper("client-without-valid-audience", "some-random-audience");

                providerClients = new ArrayList<>(providerClients);
                providerClients.addAll(Arrays.asList(client2, client3, client4));
                return providerClients;
            }

            private ClientRepresentation createProviderClientWithAudienceMapper(String clientId, String hardcodedAudience) {
                ClientRepresentation client = new ClientRepresentation();
                client.setClientId(clientId);
                client.setSecret("secret");
                client.setDirectAccessGrantsEnabled(true);

                ProtocolMapperRepresentation hardcodedAudienceMapper = new ProtocolMapperRepresentation();
                hardcodedAudienceMapper.setName("audience");
                hardcodedAudienceMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
                hardcodedAudienceMapper.setProtocolMapper(AudienceProtocolMapper.PROVIDER_ID);

                Map<String, String> hardcodedAudienceMapperConfig = hardcodedAudienceMapper.getConfig();
                hardcodedAudienceMapperConfig.put(AudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE, hardcodedAudience);
                hardcodedAudienceMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");

                client.setProtocolMappers(Collections.singletonList(hardcodedAudienceMapper));
                return client;
            }

        };
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

        realm = session.realms().getRealmByName(BrokerTestConstants.REALM_PROV_NAME);
        client = realm.getClientByClientId("brokerapp");
        client.addRedirectUri(OAuthClient.APP_ROOT + "/auth");
        client.setAttribute(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, OAuthClient.APP_ROOT + "/admin/backchannelLogout");
    }


    @Test
    public void testSuccess_externalTokenIssuedToBrokerClient() throws Exception {
        ClientRepresentation brokerApp = getBrokerAppClient();

        // Send initial direct-grant request
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.realm(bc.providerRealmName()).client(brokerApp.getClientId(), brokerApp.getSecret()).doPasswordGrantRequest(bc.getUserLogin(), bc.getUserPassword());
        assertThat(tokenResponse.getIdToken(), notNullValue());

        testingClient.server(BrokerTestConstants.REALM_CONS_NAME).run(ExternalInternalTokenExchangeV2Test::setupRealm);

        // Send token-exchange
        testTokenExchange(tokenResponse.getAccessToken(), (tokenExchangeResponse) -> {
            assertThat(tokenExchangeResponse.getStatus(), equalTo(200));
            AccessTokenResponse externalToInternalTokenResponse = tokenExchangeResponse.readEntity(AccessTokenResponse.class);
            assertThat(externalToInternalTokenResponse.getToken(), notNullValue());
        });
    }


    @Test
    public void testSuccess_brokerClientAsAudienceOfExternalToken() throws Exception {
        // Send initial direct-grant request. Token is issued to the "client-with-brokerapp-audience". Client "brokerapp" is available inside token audience
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.realm(bc.providerRealmName()).client("client-with-brokerapp-audience", "secret").doPasswordGrantRequest(bc.getUserLogin(), bc.getUserPassword());
        assertThat(tokenResponse.getIdToken(), notNullValue());

        testingClient.server(BrokerTestConstants.REALM_CONS_NAME).run(ExternalInternalTokenExchangeV2Test::setupRealm);

        testTokenExchange(tokenResponse.getAccessToken(), (tokenExchangeResponse) -> {
            assertThat(tokenExchangeResponse.getStatus(), equalTo(200));
            AccessTokenResponse externalToInternalTokenResponse = tokenExchangeResponse.readEntity(AccessTokenResponse.class);
            assertThat(externalToInternalTokenResponse.getToken(), notNullValue());
        });
    }


    @Test
    public void testSuccess_consumerRealmIssuerAsAudienceOfExternalToken() throws Exception {
        // Send initial direct-grant request. Token is issued to the "client-with-consumer-realm-issuer-audience". Consumer realm is available inside token audience and hence token considered as valid external token for the token exchange
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.realm(bc.providerRealmName()).client("client-with-consumer-realm-issuer-audience", "secret").doPasswordGrantRequest(bc.getUserLogin(), bc.getUserPassword());
        assertThat(tokenResponse.getIdToken(), notNullValue());

        testingClient.server(BrokerTestConstants.REALM_CONS_NAME).run(ExternalInternalTokenExchangeV2Test::setupRealm);

        testTokenExchange(tokenResponse.getAccessToken(), (tokenExchangeResponse) -> {
            assertThat(tokenExchangeResponse.getStatus(), equalTo(200));
            AccessTokenResponse externalToInternalTokenResponse = tokenExchangeResponse.readEntity(AccessTokenResponse.class);
            assertThat(externalToInternalTokenResponse.getToken(), notNullValue());
        });
    }


    @Test
    public void testFailure_externalTokenIssuedToInvalidClient() throws Exception {
        // Send initial direct-grant request. Token is issued to the "client-without-valid-audience". This external token will fail token-exchange as token is not issued to brokerapp and there is not any valid audience available
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.realm(bc.providerRealmName()).client("client-without-valid-audience", "secret").doPasswordGrantRequest(bc.getUserLogin(), bc.getUserPassword());
        assertThat(tokenResponse.getIdToken(), notNullValue());

        testingClient.server(BrokerTestConstants.REALM_CONS_NAME).run(ExternalInternalTokenExchangeV2Test::setupRealm);

        testTokenExchange(tokenResponse.getAccessToken(), (tokenExchangeResponse) -> {
            assertThat(tokenExchangeResponse.getStatus(), equalTo(400));
            AccessTokenResponse externalToInternalTokenResponse = tokenExchangeResponse.readEntity(AccessTokenResponse.class);
            assertThat(externalToInternalTokenResponse.getToken(), nullValue());
            assertEquals("Token not authorized for token exchange", externalToInternalTokenResponse.getErrorDescription());
        });
    }

    @Test
    public void testFailure_externalTokenIntrospectionFailureDueInvalidClientCredentials() throws Exception {
        // Update IDP and set invalid credentials there
        IdentityProviderResource idpResource = adminClient.realm(REALM_CONS_NAME).identityProviders().get(IDP_OIDC_ALIAS);
        IdentityProviderRepresentation idpRep = idpResource.toRepresentation();
        idpRep.getConfig().put("clientSecret", "invalid");
        idpResource.update(idpRep);

        ClientRepresentation brokerApp = getBrokerAppClient();

        try {
            org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.realm(bc.providerRealmName()).client(brokerApp.getClientId(), brokerApp.getSecret()).doPasswordGrantRequest(bc.getUserLogin(), bc.getUserPassword());
            assertThat(tokenResponse.getIdToken(), notNullValue());

            testingClient.server(BrokerTestConstants.REALM_CONS_NAME).run(ExternalInternalTokenExchangeV2Test::setupRealm);

            testTokenExchange(tokenResponse.getAccessToken(), (tokenExchangeResponse) -> {
                assertThat(tokenExchangeResponse.getStatus(), equalTo(400));
                AccessTokenResponse externalToInternalTokenResponse = tokenExchangeResponse.readEntity(AccessTokenResponse.class);
                assertThat(externalToInternalTokenResponse.getToken(), nullValue());
                assertEquals("Introspection endpoint call failure. Introspection response status: 401", externalToInternalTokenResponse.getErrorDescription());
            });
        } finally {
            // Revert IDP config
            idpRep.getConfig().put("clientSecret", brokerApp.getSecret());
            idpResource.update(idpRep);
        }
    }


    @Test
    public void testFailure_inactiveExternalToken() throws Exception {
        ClientRepresentation brokerApp = getBrokerAppClient();
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.realm(bc.providerRealmName()).client(brokerApp.getClientId(), brokerApp.getSecret()).doPasswordGrantRequest(bc.getUserLogin(), bc.getUserPassword());
        assertThat(tokenResponse.getIdToken(), notNullValue());

        testingClient.server(BrokerTestConstants.REALM_CONS_NAME).run(ExternalInternalTokenExchangeV2Test::setupRealm);

        setTimeOffset(3600);

        testTokenExchange(tokenResponse.getAccessToken(), (tokenExchangeResponse) -> {
            assertThat(tokenExchangeResponse.getStatus(), equalTo(400));
            AccessTokenResponse externalToInternalTokenResponse = tokenExchangeResponse.readEntity(AccessTokenResponse.class);
            assertThat(externalToInternalTokenResponse.getToken(), nullValue());
            assertEquals("Token not active", externalToInternalTokenResponse.getErrorDescription());
        });
    }

    private void testTokenExchange(String subjectToken, Consumer<Response> tokenExchangeResponseConsumer) {
        // Send token-exchange
        try (Client httpClient = AdminClientUtil.createResteasyClient()) {
            WebTarget exchangeUrl = getConsumerTokenEndpoint(httpClient);

            String subjectTokenType = OAuth2Constants.ACCESS_TOKEN_TYPE; // hardcoded to access-token just for now. More types might need to be tested...
            try (Response response = sendExternalInternalTokenExchangeRequest(exchangeUrl, subjectToken, subjectTokenType)) {
                tokenExchangeResponseConsumer.accept(response);
            }
        }
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

    private WebTarget getConsumerTokenEndpoint(Client httpClient) {
        return httpClient.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms")
                .path(bc.consumerRealmName())
                .path("protocol/openid-connect/token");
    }

    private ClientRepresentation getBrokerAppClient() {
        RealmResource providerRealm = realmsResouce().realm(bc.providerRealmName());
        ClientsResource clients = providerRealm.clients();
        return clients.findByClientId("brokerapp").get(0);
    }
}
