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

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.EventType;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.realm.ProtocolMapperBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.tests.providers.broker.oidc.WrongNonceProtocolMapper;
import org.keycloak.tests.utils.ClientPoliciesUtil.ClientPoliciesBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Brokers to another realm that enforces the FAPI 2.0 Security Profile with DPoP on its clients.
 */
@KeycloakIntegrationTest(config = AbstractKcOidcBrokerFapi2Test.Fapi2ServerConfig.class)
public class KcOidcBrokerFapi2DPoPTest extends AbstractKcOidcBrokerFapi2Test {

    @InjectPage
    ErrorPage errorPage;

    @Override
    protected String getClientProfile() {
        return "fapi-2-dpop-security-profile";
    }

    @Test
    public void testUserInfoIsRequestedWithDPoPBoundToken() {
        loginUser();

        EventAssertion.assertSuccess(providerEvents.poll()).type(EventType.USER_INFO_REQUEST);
        Assertions.assertNull(providerEvents.poll());
    }

    @Test
    public void testIdTokenSignedWithAnotherAlgorithmIsRejected() {
        // the provider would not let the client use the algorithm
        providerRealm.admin().clientPoliciesPoliciesResource().updatePolicies(new ClientPoliciesBuilder().toRepresentation());
        updateProviderClient(client -> client.getAttributes().put(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG, Algorithm.RS256));

        startLogin(true);

        errorPage.assertCurrent();
    }

    @Test
    public void testIdTokenWithAnAudienceThatIsNotTrustedIsRejected() {
        ProtocolMapperRepresentation audience = new ProtocolMapperRepresentation();
        audience.setName("another-audience");
        audience.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        audience.setProtocolMapper("oidc-audience-mapper");
        audience.setConfig(Map.of("included.custom.audience", "another-audience",
                OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true"));
        updateProviderClient(client -> client.setProtocolMappers(List.of(audience)));

        startLogin(true);

        errorPage.assertCurrent();
    }

    @Test
    public void testNonceIsVerifiedBeforeUserInfoIsRequested() {
        ProtocolMapperRepresentation wrongNonce = ProtocolMapperBuilder.create()
                .name("wrong-nonce")
                .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                .protocolMapper(WrongNonceProtocolMapper.PROVIDER_ID)
                .config(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true")
                .build();
        updateProviderClient(client -> client.setProtocolMappers(List.of(wrongNonce)));

        startLogin(true);

        // the consumer answers with an error response instead of a page
        Assertions.assertTrue(webDriver.driver().getPageSource().contains("invalid nonce"));
        // the access token is not presented to the user info endpoint
        Assertions.assertNull(providerEvents.poll());
    }

    @Test
    public void testAuthorizationResponseFromUnexpectedIssuerIsRejected() {
        updateIdentityProvider(config -> setIssuer(config, providerRealm.getBaseUrl() + "-another-issuer"));

        startLogin(true);

        errorPage.assertCurrent();
    }

    @Test
    public void testImportOfTheMetadataFillsInTheSettings() {
        Map<String, String> config = consumerRealm.admin().identityProviders().importFrom(
                Map.of("providerId", "oidc", "fromUrl", providerRealm.getBaseUrl() + "/.well-known/openid-configuration"));

        Assertions.assertEquals(providerRealm.getBaseUrl() + "/protocol/openid-connect/ext/par/request",
                config.get(OAuth2IdentityProviderConfig.PAR_URL));
        Assertions.assertEquals("true", config.get(OAuth2IdentityProviderConfig.AUTHORIZATION_RESPONSE_ISSUER_REQUIRED));
        Assertions.assertEquals(Algorithm.ES256, config.get(OAuth2IdentityProviderConfig.DPOP_SIGNING_ALG));
        Assertions.assertEquals(providerRealm.getBaseUrl() + "/.well-known/openid-configuration",
                config.get(IdentityProviderModel.METADATA_DESCRIPTOR_URL));
        // the provider does not require pushed authorization requests
        Assertions.assertNull(config.get(OAuth2IdentityProviderConfig.PAR_ENABLED));
    }
}
