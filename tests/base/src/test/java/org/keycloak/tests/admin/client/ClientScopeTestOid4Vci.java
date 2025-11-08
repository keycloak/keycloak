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
 *
 */

package org.keycloak.tests.admin.client;

import java.util.Map;
import java.util.Optional;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.Profile;
import org.keycloak.constants.Oid4VciConstants;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Pascal Knüppel
 */
@KeycloakIntegrationTest(config = ClientScopeTestOid4Vci.DefaultServerConfigWithOid4Vci.class)
public class ClientScopeTestOid4Vci extends AbstractClientScopeTest {

    @DisplayName("Verify default values are correctly set")
    @Test
    public void testDefaultOid4VciClientScopeAttributes() {
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("test-client-scope");
        clientScope.setDescription("test-client-scope-description");
        clientScope.setProtocol(Oid4VciConstants.OID4VC_PROTOCOL);
        clientScope.setAttributes(Map.of("test-attribute", "test-value"));

        String clientScopeId = null;
        try (Response response = clientScopes().create(clientScope)) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            String location = (String) Optional.ofNullable(response.getHeaders().get(HttpHeaders.LOCATION))
                                               .map(list -> list.get(0))
                                               .orElse(null);
            Assertions.assertNotNull(location);
            clientScopeId = location.substring(location.lastIndexOf("/") + 1);

            ClientScopeRepresentation createdClientScope = clientScopes().get(clientScopeId).toRepresentation();
            Assertions.assertNotNull(createdClientScope);
            Assertions.assertEquals(CredentialScopeModel.SD_JWT_VISIBLE_CLAIMS_DEFAULT,
                                    createdClientScope.getAttributes().get(CredentialScopeModel.SD_JWT_VISIBLE_CLAIMS));
            Assertions.assertEquals(String.valueOf(CredentialScopeModel.SD_JWT_DECOYS_DEFAULT),
                                    createdClientScope.getAttributes().get(CredentialScopeModel.SD_JWT_NUMBER_OF_DECOYS));
            Assertions.assertEquals(CredentialScopeModel.FORMAT_DEFAULT,
                                    createdClientScope.getAttributes().get(CredentialScopeModel.FORMAT));
            Assertions.assertEquals(CredentialScopeModel.HASH_ALGORITHM_DEFAULT,
                                    createdClientScope.getAttributes().get(CredentialScopeModel.HASH_ALGORITHM));
            Assertions.assertEquals(CredentialScopeModel.TOKEN_TYPE_DEFAULT,
                                    createdClientScope.getAttributes().get(CredentialScopeModel.TOKEN_JWS_TYPE));
            Assertions.assertEquals(String.valueOf(CredentialScopeModel.EXPIRY_IN_SECONDS_DEFAULT),
                                    createdClientScope.getAttributes().get(CredentialScopeModel.EXPIRY_IN_SECONDS));
            Assertions.assertEquals(CredentialScopeModel.CRYPTOGRAPHIC_BINDING_METHODS_DEFAULT,
                                    createdClientScope.getAttributes().get(CredentialScopeModel.CRYPTOGRAPHIC_BINDING_METHODS));
            Assertions.assertEquals(clientScope.getName(),
                                    createdClientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID));
            Assertions.assertEquals(clientScope.getName(),
                                    createdClientScope.getAttributes().get(CredentialScopeModel.CREDENTIAL_IDENTIFIER));
            Assertions.assertEquals(clientScope.getName(),
                                    createdClientScope.getAttributes().get(CredentialScopeModel.TYPES));
            Assertions.assertEquals(clientScope.getName(),
                                    createdClientScope.getAttributes().get(CredentialScopeModel.CONTEXTS));
            Assertions.assertEquals(clientScope.getName(),
                                    createdClientScope.getAttributes().get(CredentialScopeModel.VCT));
            Assertions.assertEquals(clientScope.getName(),
                                    createdClientScope.getAttributes().get(CredentialScopeModel.ISSUER_DID));

        } finally {
            Assertions.assertNotNull(clientScopeId);
            // cleanup
            clientScopes().get(clientScopeId).remove();
        }
    }

    public static class DefaultServerConfigWithOid4Vci implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.OID4VC_VCI);
        }
    }
}
