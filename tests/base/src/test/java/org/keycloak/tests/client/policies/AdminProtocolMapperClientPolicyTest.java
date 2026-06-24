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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.executor.RejectRequestExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class AdminProtocolMapperClientPolicyTest extends AbstractClientPoliciesTest {

    @InjectRealm
    ManagedRealm realm;

    @Test
    public void rejectProtocolMapperCreateUpdateAndDelete() throws Exception {
        ProtocolMappersResource mappers = createClient("policy-mapper-client").getProtocolMappers();
        ProtocolMapperRepresentation existingMapper = mapper("existing-mapper", "email");
        String existingMapperId = createMapper(mappers, existingMapper);

        setupRejectingPolicy();

        try (Response response = mappers.createMapper(mapper("created-mapper", "firstName"))) {
            Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
        assertMapperAbsent(mappers, "created-mapper");

        ProtocolMapperRepresentation updatedMapper = mapper("updated-mapper", "lastName");
        updatedMapper.setId(existingMapperId);
        Assertions.assertThrows(BadRequestException.class, () -> mappers.update(existingMapperId, updatedMapper));
        Assertions.assertEquals("existing-mapper", mappers.getMapperById(existingMapperId).getName());

        Assertions.assertThrows(BadRequestException.class, () -> mappers.delete(existingMapperId));
        Assertions.assertNotNull(mappers.getMapperById(existingMapperId));
    }

    @Test
    public void rejectProtocolMapperBulkCreate() throws Exception {
        ProtocolMappersResource mappers = createClient("policy-mapper-bulk-client").getProtocolMappers();
        setupRejectingPolicy();

        List<ProtocolMapperRepresentation> proposedMappers = List.of(
                mapper("bulk-mapper-one", "firstName"),
                mapper("bulk-mapper-two", "lastName"));

        Assertions.assertThrows(BadRequestException.class, () -> mappers.createMapper(proposedMappers));
        assertMapperAbsent(mappers, "bulk-mapper-one");
        assertMapperAbsent(mappers, "bulk-mapper-two");
    }

    private ClientResource createClient(String clientId) {
        ClientRepresentation client = ClientBuilder.create(generateSuffixedName(clientId))
                .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                .publicClient()
                .build();

        try (Response response = realm.admin().clients().create(client)) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            String id = ApiUtil.getCreatedId(response);
            realm.cleanup().add(r -> r.clients().delete(id));
            return realm.admin().clients().get(id);
        }
    }

    private String createMapper(ProtocolMappersResource mappers, ProtocolMapperRepresentation mapper) {
        try (Response response = mappers.createMapper(mapper)) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            return ApiUtil.getCreatedId(response);
        }
    }

    private ProtocolMapperRepresentation mapper(String name, String userAttribute) {
        Map<String, String> config = new HashMap<>();
        config.put("user.attribute", userAttribute);
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, name);
        config.put(OIDCAttributeMapperHelper.JSON_TYPE, "String");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");

        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName(name);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        mapper.setProtocolMapper(UserAttributeMapper.PROVIDER_ID);
        mapper.setConfig(config);
        return mapper;
    }

    private void assertMapperAbsent(ProtocolMappersResource mappers, String name) {
        Assertions.assertTrue(mappers.getMappers().stream().noneMatch(mapper -> name.equals(mapper.getName())));
    }

    private void setupRejectingPolicy() throws Exception {
        setupPolicy(realm, RejectRequestExecutorFactory.PROVIDER_ID, null,
                AnyClientConditionFactory.PROVIDER_ID, new ClientPolicyConditionConfigurationRepresentation());
    }
}
