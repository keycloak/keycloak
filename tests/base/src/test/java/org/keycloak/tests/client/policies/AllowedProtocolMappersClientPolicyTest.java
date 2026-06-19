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

package org.keycloak.tests.client.policies;

import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.FullNameMapper;
import org.keycloak.protocol.oidc.mappers.HardcodedRole;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.executor.AllowedProtocolMappersExecutorFactory;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies allowed-protocol-mappers client policy enforcement on Admin REST API operations.
 */
@KeycloakIntegrationTest
public class AllowedProtocolMappersClientPolicyTest extends AbstractClientPoliciesTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectClient(config = TestClient.class)
    ManagedClient client;

    @BeforeEach
    public void setupPolicy() throws Exception {
        ClientPolicyExecutorConfigurationRepresentation executorConfig = new ClientPolicyExecutorConfigurationRepresentation();
        executorConfig.setConfigAsMap(AllowedProtocolMappersExecutorFactory.ALLOWED_PROTOCOL_MAPPER_TYPES,
                List.of(FullNameMapper.PROVIDER_ID));

        setupPolicy(realm,
                AllowedProtocolMappersExecutorFactory.PROVIDER_ID,
                executorConfig,
                AnyClientConditionFactory.PROVIDER_ID,
                new ClientPolicyConditionConfigurationRepresentation());
    }

    @Test
    public void testAdminProtocolMapperCreateRejected() {
        ProtocolMapperRepresentation mapper = createHardcodedMapperRep();

        try (Response response = client.admin().getProtocolMappers().createMapper(mapper)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testAdminProtocolMapperCreateAllowed() {
        ProtocolMapperRepresentation mapper = createFullNameMapperRep();

        try (Response response = client.admin().getProtocolMappers().createMapper(mapper)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testAdminClientUpdateWithDisallowedMapperRejected() {
        ClientRepresentation clientRep = client.admin().toRepresentation();
        clientRep.setProtocolMappers(Collections.singletonList(createHardcodedMapperRep()));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> client.admin().update(clientRep));
        try (Response response = ex.getResponse()) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    private ProtocolMapperRepresentation createHardcodedMapperRep() {
        ProtocolMapperRepresentation protocolMapper = new ProtocolMapperRepresentation();
        protocolMapper.setName("Hardcoded foo role");
        protocolMapper.setProtocolMapper(HardcodedRole.PROVIDER_ID);
        protocolMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        protocolMapper.getConfig().put(HardcodedRole.ROLE_CONFIG, "foo-role");
        return protocolMapper;
    }

    private ProtocolMapperRepresentation createFullNameMapperRep() {
        ProtocolMapperRepresentation protocolMapper = new ProtocolMapperRepresentation();
        protocolMapper.setName("Full name");
        protocolMapper.setProtocolMapper(FullNameMapper.PROVIDER_ID);
        protocolMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        return protocolMapper;
    }

    public static class TestClient implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("allowed-protocol-mappers-test-client")
                    .name("allowed-protocol-mappers-test-client")
                    .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                    .publicClient(true)
                    .redirectUris("http://localhost:8080/*");
        }
    }
}
