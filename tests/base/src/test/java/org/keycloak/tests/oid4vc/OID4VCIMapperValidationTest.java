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
package org.keycloak.tests.oid4vc;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCTargetRoleMapper;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCIMapperValidationTest {

    private static final String INVALID_CLIENT_ID = "<img src=x onerror=alert(1)>";

    @InjectRealm(config = VerifiableCredentialRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @Test
    public void testValidationErrorWithoutMessageKey() {
        String scopeId = ApiUtil.getCreatedId(realm.admin().clientScopes()
                .create(new CredentialScopeRepresentation("target-role-scope")));
        ProtocolMapperRepresentation mapper = targetRoleMapper(
                Map.of(OID4VCTargetRoleMapper.CLIENT_CONFIG_KEY, INVALID_CLIENT_ID));

        // This validator throws an exception without a message key and includes the supplied client ID in its message.
        try (Response response = realm.admin().clientScopes().get(scopeId).getProtocolMappers().createMapper(mapper)) {
            assertMapperError(response);
        }

        assertTrue(realm.admin().clientScopes().get(scopeId).getProtocolMappers().getMappers().isEmpty(),
                "A rejected mapper must not be stored");
    }

    @ParameterizedTest
    @MethodSource("invalidTargetRoleConfigs")
    public void testInvalidTargetRoleMapperRejectedOnScopeCreate(Map<String, String> config) {
        CredentialScopeRepresentation scope = new CredentialScopeRepresentation("invalid-target-role-scope");
        scope.setProtocolMappers(List.of(targetRoleMapper(config)));

        try (Response response = realm.admin().clientScopes().create(scope)) {
            assertMapperError(response);
        }

        assertTrue(realm.admin().clientScopes().findAll().stream().noneMatch(s -> scope.getName().equals(s.getName())),
                "A rejected scope must not be stored");
    }

    static Stream<Map<String, String>> invalidTargetRoleConfigs() {
        return Stream.of(null, Map.of(), Map.of(OID4VCTargetRoleMapper.CLIENT_CONFIG_KEY, INVALID_CLIENT_ID));
    }

    @Test
    public void testValidTargetRoleMapperAcceptedOnScopeCreate() {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("target-role-client");
        ApiUtil.getCreatedId(realm.admin().clients().create(client));

        CredentialScopeRepresentation scope = new CredentialScopeRepresentation("valid-target-role-scope");
        scope.setProtocolMappers(List.of(targetRoleMapper(
                Map.of(OID4VCTargetRoleMapper.CLIENT_CONFIG_KEY, client.getClientId()))));
        String scopeId = ApiUtil.getCreatedId(realm.admin().clientScopes().create(scope));

        List<ProtocolMapperRepresentation> stored = realm.admin().clientScopes().get(scopeId).getProtocolMappers().getMappers();
        assertEquals(1, stored.size());
        assertEquals(client.getClientId(), stored.get(0).getConfig().get(OID4VCTargetRoleMapper.CLIENT_CONFIG_KEY));
    }

    private static ProtocolMapperRepresentation targetRoleMapper(Map<String, String> config) {
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName("target-role-mapper");
        mapper.setProtocol(OID4VCIConstants.OID4VC_PROTOCOL);
        mapper.setProtocolMapper(OID4VCTargetRoleMapper.MAPPER_ID);
        mapper.setConfig(config);
        return mapper;
    }

    private static void assertMapperError(Response response) {
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        OAuth2ErrorRepresentation error = response.readEntity(OAuth2ErrorRepresentation.class);
        assertNotNull(error);
        assertEquals("invalid_input", error.getError());
        assertEquals("Invalid protocol mapper configuration", error.getErrorDescription());
    }

    public static class VerifiableCredentialRealmConfig implements RealmConfig {
        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            return realm.verifiableCredentialsEnabled(true);
        }
    }
}
