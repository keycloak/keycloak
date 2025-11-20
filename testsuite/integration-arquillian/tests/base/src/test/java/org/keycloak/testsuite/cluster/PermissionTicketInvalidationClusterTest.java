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

import java.util.concurrent.ThreadLocalRandom;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.AuthorizationProviderFactory;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.PermissionTicketRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.ContainerInfo;

import org.apache.commons.lang.RandomStringUtils;

import static org.junit.Assert.assertEquals;

public class PermissionTicketInvalidationClusterTest extends AbstractInvalidationClusterTestWithTestRealm<PermissionTicketRepresentation, Object> {

    private String clientId;
    private String userId;
    private String resourceId;
    private String scopeId;
    private final String resourceName = RandomStringUtils.randomAlphabetic(5);
    private final String scopeName = RandomStringUtils.randomAlphabetic(5);

    @Override
    protected void createTestRealm(ContainerInfo node) {
        super.createTestRealm(node);
        createClient(node);
        createUser(node);
        createResource(node);
        createScope(node);
    }

    private void createClient(ContainerInfo node) {
        var client = new ClientRepresentation();
        String s = RandomStringUtils.randomAlphabetic(5);
        client.setClientId("client_" + s);
        client.setName("name_" + s);
        try (var rsp = getAdminClientFor(node)
                .realm(testRealmName)
                .clients()
                .create(client)) {
            clientId = ApiUtil.getCreatedId(rsp);
        }
    }

    private void createUser(ContainerInfo node) {
        var user = createUserRepresentation("user1", "password");
        try (var rsp = getAdminClientFor(node)
                .realm(testRealmName)
                .users()
                .create(user)) {
            userId = ApiUtil.getCreatedId(rsp);
        }
    }

    private void createResource(ContainerInfo node) {
        var realmFinal = testRealmName;
        var clientFinal = clientId;
        var resourceFinal = resourceName;
        resourceId = getTestingClientFor(node).server().fetchString(session -> {
            var realm = session.realms().getRealmByName(realmFinal);
            session.getContext().setRealm(realm);
            var factory = (AuthorizationProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(AuthorizationProvider.class);
            var storeFactory = factory.create(session, realm).getStoreFactory();
            var client = session.clients().getClientById(realm, clientFinal);

            var resourceServer = storeFactory.getResourceServerStore().findByClient(client);
            if (resourceServer == null) {
                resourceServer = storeFactory.getResourceServerStore().create(client);
            }
            return storeFactory.getResourceStore().create(resourceServer, resourceFinal, clientFinal).getId();
        }).replaceAll("\"", "");
    }

    private void createScope(ContainerInfo node) {
        var realmFinal = testRealmName;
        var clientFinal = clientId;
        var scopeFinal = scopeName;
        scopeId = getTestingClientFor(node).server().fetchString(session -> {
            var realm = session.realms().getRealmByName(realmFinal);
            session.getContext().setRealm(realm);
            var storeFactory = session.getProvider(AuthorizationProvider.class).getStoreFactory();
            var client = session.clients().getClientById(realm, clientFinal);

            var resourceServer = storeFactory.getResourceServerStore().findByClient(client);
            if (resourceServer == null) {
                resourceServer = storeFactory.getResourceServerStore().create(client);
            }
            return storeFactory.getScopeStore().create(resourceServer, scopeFinal).getId();
        }).replaceAll("\"", "");
    }

    @Override
    protected PermissionTicketRepresentation createTestEntityRepresentation() {
        var ticket = new PermissionTicketRepresentation();
        ticket.setGranted(true);
        ticket.setOwner(clientId); // client is the owner
        ticket.setRequester(userId); // userid is the requester
        ticket.setScope(scopeId);
        ticket.setResource(resourceId);
        return ticket;
    }

    @Override
    protected Object entityResource(PermissionTicketRepresentation testEntity, ContainerInfo node) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object entityResource(String idOrName, ContainerInfo node) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected PermissionTicketRepresentation createEntity(PermissionTicketRepresentation testEntity, ContainerInfo node) {
        var realmFinal = testRealmName;
        var clientFinal = clientId;
        var scopeFinal = scopeId;
        var resourceFinal = resourceId;
        var userFinal = userId;
        return getTestingClientFor(node).server().fetch(session -> {
            var realm = session.realms().getRealmByName(realmFinal);
            session.getContext().setRealm(realm);
            var factory = (AuthorizationProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(AuthorizationProvider.class);
            var provider = factory.create(session, realm);
            var storeFactory = provider.getStoreFactory();
            var client = session.clients().getClientById(realm, clientFinal);

            var resourceServer = storeFactory.getResourceServerStore().findByClient(client);
            var resource = storeFactory.getResourceStore().findById(resourceServer, resourceFinal);
            var scope = storeFactory.getScopeStore().findById(resourceServer, scopeFinal);
            var ticket = storeFactory.getPermissionTicketStore().create(resourceServer, resource, scope, userFinal);
            return ModelToRepresentation.toRepresentation(ticket, provider, false);
        }, PermissionTicketRepresentation.class);
    }

    @Override
    protected PermissionTicketRepresentation readEntity(PermissionTicketRepresentation entity, ContainerInfo node) {
        var realmFinal = testRealmName;
        var clientFinal = clientId;
        var idFinal = entity.getId();
        return getTestingClientFor(node).server().fetch(session -> {
            var realm = session.realms().getRealmByName(realmFinal);
            session.getContext().setRealm(realm);
            var factory = (AuthorizationProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(AuthorizationProvider.class);
            var provider = factory.create(session, realm);
            var storeFactory = provider.getStoreFactory();
            var client = session.clients().getClientById(realm, clientFinal);

            var resourceServer = storeFactory.getResourceServerStore().findByClient(client);
            var ticket = storeFactory.getPermissionTicketStore().findById(resourceServer, idFinal);
            return ticket == null ? null : ModelToRepresentation.toRepresentation(ticket, provider, false);
        }, PermissionTicketRepresentation.class);
    }

    @Override
    protected PermissionTicketRepresentation updateEntity(PermissionTicketRepresentation entity, ContainerInfo node) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void deleteEntity(PermissionTicketRepresentation entity, ContainerInfo node) {
        var realmFinal = testRealmName;
        var idFinal = entity.getId();
        getTestingClientFor(node).server().run(session -> {
            var realm = session.realms().getRealmByName(realmFinal);
            session.getContext().setRealm(realm);
            var factory = (AuthorizationProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(AuthorizationProvider.class);
            var storeFactory = factory.create(session, realm).getStoreFactory();
            storeFactory.getPermissionTicketStore().delete(idFinal);
        });
    }

    @Override
    protected PermissionTicketRepresentation testEntityUpdates(PermissionTicketRepresentation entity, boolean backendFailover) {
        final long timestamp = ThreadLocalRandom.current().nextLong(100000);
        var realmFinal = testRealmName;
        var clientFinal = clientId;
        var idFinal = entity.getId();

        getTestingClientFor(getCurrentFailNode()).server().run(session -> {
            var realm = session.realms().getRealmByName(realmFinal);
            session.getContext().setRealm(realm);
            var factory = (AuthorizationProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(AuthorizationProvider.class);
            var storeFactory = factory.create(session, realm).getStoreFactory();
            var client = session.clients().getClientById(realm, clientFinal);

            var resourceServer = storeFactory.getResourceServerStore().findByClient(client);
            var ticket = storeFactory.getPermissionTicketStore().findById(resourceServer, idFinal);
            ticket.setGrantedTimestamp(timestamp);
        });

        if (backendFailover) {
            failure();
        }

        for (var node : getCurrentSurvivorNodes()) {
            var rsp = getTestingClientFor(node).server().fetchString(session -> {
                var realm = session.realms().getRealmByName(realmFinal);
                session.getContext().setRealm(realm);
                var factory = (AuthorizationProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(AuthorizationProvider.class);
                var storeFactory = factory.create(session, realm).getStoreFactory();
                var client = session.clients().getClientById(realm, clientFinal);

                var resourceServer = storeFactory.getResourceServerStore().findByClient(client);
                var ticket = storeFactory.getPermissionTicketStore().findById(resourceServer, idFinal);
                return Long.toString(ticket.getGrantedTimestamp());
            });
            assertEquals(timestamp, Long.parseLong(rsp.replaceAll("\"", "")));
        }

        failback();
        iterateCurrentFailNode();

        return entity;
    }
}
