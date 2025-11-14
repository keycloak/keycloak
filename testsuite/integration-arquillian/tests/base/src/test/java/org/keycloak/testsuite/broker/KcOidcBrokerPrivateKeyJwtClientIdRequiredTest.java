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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation.KeyMetadataRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.broker.oidc.ClientIdRequiredJWTClientAuthenticator;
import org.keycloak.testsuite.util.ExecutionBuilder;
import org.keycloak.testsuite.util.FlowBuilder;
import org.keycloak.testsuite.util.KeyUtils;

import org.junit.Before;

import static org.keycloak.testsuite.AbstractAuthenticationTest.findFlowByAlias;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_PROVIDER_ID;
import static org.keycloak.testsuite.broker.BrokerTestTools.createIdentityProvider;

/**
 * Test that the broker will send the client_id parameter.
 *
 * @author Justin Tay
 */
public class KcOidcBrokerPrivateKeyJwtClientIdRequiredTest extends AbstractBrokerTest {

    @Override
    @Before
    public void beforeBrokerTest() {
        super.beforeBrokerTest();
        RealmResource realmResource = adminClient.realm(bc.providerRealmName());

        AuthenticationFlowRepresentation clientFlow = FlowBuilder.create()
                .alias("new-client-flow")
                .description("Base authentication for clients")
                .providerId(AuthenticationFlow.CLIENT_FLOW)
                .topLevel(true)
                .builtIn(false)
                .build();

        realmResource.flows().createFlow(clientFlow);

        RealmRepresentation realm = realmResource.toRepresentation();
        realm.setClientAuthenticationFlow(clientFlow.getAlias());
        realmResource.update(realm);

        // refresh flow to find its id
        clientFlow = findFlowByAlias(clientFlow.getAlias(), realmResource.flows().getFlows());

        AuthenticationExecutionRepresentation execution = ExecutionBuilder.create()
                .parentFlow(clientFlow.getId())
                .requirement(AuthenticationExecutionModel.Requirement.REQUIRED.toString())
                .authenticator(ClientIdRequiredJWTClientAuthenticator.PROVIDER_ID)
                .priority(10)
                .authenticatorFlow(false)
                .build();
        realmResource.flows().addExecution(execution);
    }

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfigurationWithJWTAuthentication();
    }

    private class KcOidcBrokerConfigurationWithJWTAuthentication extends KcOidcBrokerConfiguration {
        @Override
        public List<ClientRepresentation> createProviderClients() {
            List<ClientRepresentation> clientsRepList = super.createProviderClients();
            log.info("Update provider clients to accept JWT authentication");
            KeyMetadataRepresentation keyRep = KeyUtils.findActiveSigningKey(adminClient.realm(consumerRealmName()), Algorithm.RS256);
            for (ClientRepresentation client: clientsRepList) {
                client.setClientAuthenticatorType(ClientIdRequiredJWTClientAuthenticator.PROVIDER_ID);
                if (client.getAttributes() == null) {
                    client.setAttributes(new HashMap<String, String>());
                }
                client.getAttributes().put(JWTClientAuthenticator.CERTIFICATE_ATTR, keyRep.getCertificate());
            }
            return clientsRepList;
        }

        @Override
        public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
            IdentityProviderRepresentation idp = createIdentityProvider(IDP_OIDC_ALIAS, IDP_OIDC_PROVIDER_ID);
            Map<String, String> config = idp.getConfig();
            applyDefaultConfiguration(config, syncMode);
            config.put("clientSecret", null);
            config.put("clientAuthMethod", OIDCLoginProtocol.PRIVATE_KEY_JWT);
            return idp;
        }

    }

}
