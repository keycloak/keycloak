/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.cluster;

import java.util.Map;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.ClientScopesResource;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.ContainerInfo;

import static org.junit.Assert.assertNull;

public class ClientScopeInvalidationClusterTest extends AbstractInvalidationClusterTestWithTestRealm<ClientScopeRepresentation, ClientScopeResource> {
    @Override
    protected ClientScopeRepresentation createTestEntityRepresentation() {
        var clientScope = new ClientScopeRepresentation();
        clientScope.setName("client-scope-name");
        clientScope.setDescription("client-scope-description");
        clientScope.setProtocol("openid-connect");
        clientScope.setAttributes(Map.of("a", "b", "c", "d"));
        return clientScope;
    }

    @Override
    protected ClientScopeResource entityResource(ClientScopeRepresentation testEntity, ContainerInfo node) {
        return entityResource(testEntity.getId(), node);
    }

    @Override
    protected ClientScopeResource entityResource(String idOrName, ContainerInfo node) {
        return clientScopes(node).get(idOrName);
    }

    @Override
    protected ClientScopeRepresentation createEntity(ClientScopeRepresentation testEntity, ContainerInfo node) {
        try (var rsp = clientScopes(node).create(testEntity)) {
            testEntity.setId(ApiUtil.getCreatedId(rsp));
        }
        return readEntity(testEntity, node);
    }

    @Override
    protected ClientScopeRepresentation readEntity(ClientScopeRepresentation entity, ContainerInfo node) {
        try {
            return entityResource(entity, node).toRepresentation();
        } catch (NotFoundException nfe) {
            // expected when client scope doesn't exist
            return null;
        }
    }

    @Override
    protected ClientScopeRepresentation updateEntity(ClientScopeRepresentation entity, ContainerInfo node) {
        entityResource(entity, node).update(entity);
        return readEntity(entity, node);
    }

    @Override
    protected void deleteEntity(ClientScopeRepresentation testEntity, ContainerInfo node) {
        entityResource(testEntity, node).remove();
        assertNull(readEntity(testEntity, node));
    }

    @Override
    protected ClientScopeRepresentation testEntityUpdates(ClientScopeRepresentation testEntity, boolean backendFailover) {
        // groupname
        testEntity.setName("name-updated");
        testEntity = updateEntityOnCurrentFailNode(testEntity, "name");
        verifyEntityUpdateDuringFailover(testEntity, backendFailover);

        testEntity.setProtocol("protocol-updated");
        testEntity = updateEntityOnCurrentFailNode(testEntity, "protocol");
        verifyEntityUpdateDuringFailover(testEntity, backendFailover);

        testEntity.setDescription("description-updated");
        testEntity = updateEntityOnCurrentFailNode(testEntity, "description");
        verifyEntityUpdateDuringFailover(testEntity, backendFailover);

        testEntity.setAttributes(Map.of("updated", "updated"));
        testEntity = updateEntityOnCurrentFailNode(testEntity, "attributes");
        verifyEntityUpdateDuringFailover(testEntity, backendFailover);

        return testEntity;
    }

    private ClientScopesResource clientScopes(ContainerInfo node) {
        return getAdminClientFor(node).realm(testRealmName).clientScopes();
    }
}
