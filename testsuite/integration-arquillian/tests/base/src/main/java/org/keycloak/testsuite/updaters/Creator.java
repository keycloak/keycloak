/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.updaters;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.ComponentResource;
import org.keycloak.admin.client.resource.ComponentsResource;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.IdentityProvidersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.ws.rs.core.Response;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.Assert;
import static org.keycloak.testsuite.admin.ApiUtil.getCreatedId;

/**
 *  Creates a temporary realm object and makes sure it is removed when used within try-with-resources.
 */
public class Creator<T> implements AutoCloseable {

    private final static Logger LOG = Logger.getLogger(Creator.class);

    public static Creator<RealmResource> create(Keycloak adminClient, RealmRepresentation rep) {
        adminClient.realms().create(rep);
        final RealmResource r = adminClient.realm(rep.getRealm());
        LOG.debugf("Created realm %s", rep.getRealm());
        return new Creator(rep.getRealm(), r, r::remove);
    }

    public static Creator<GroupResource> create(RealmResource realmResource, GroupRepresentation rep) {
        final GroupsResource groups = realmResource.groups();
        try (Response response = groups.add(rep)) {
            String createdId = getCreatedId(response);
            final GroupResource r = groups.group(createdId);
            LOG.debugf("Created group ID %s", createdId);
            return new Creator(createdId, r, r::remove);
        }
    }

    public static Creator<ClientResource> create(RealmResource realmResource, ClientRepresentation rep) {
        final ClientsResource clients = realmResource.clients();
        try (Response response = clients.create(rep)) {
            String createdId = getCreatedId(response);
            final ClientResource r = clients.get(createdId);
            LOG.debugf("Created client ID %s", createdId);
            return new Creator(createdId, r, r::remove);
        }
    }

    public static Creator<UserResource> create(RealmResource realmResource, UserRepresentation rep) {
        final UsersResource users = realmResource.users();
        try (Response response = users.create(rep)) {
            String createdId = getCreatedId(response);
            final UserResource r = users.get(createdId);
            LOG.debugf("Created user ID %s", createdId);
            return new Creator(createdId, r, r::remove);
        }
    }

    public static Creator<ComponentResource> create(RealmResource realmResource, ComponentRepresentation rep) {
        final ComponentsResource components = realmResource.components();
        try (Response response = components.add(rep)) {
            String createdId = getCreatedId(response);
            final ComponentResource r = components.component(createdId);
            LOG.debugf("Created component ID %s", createdId);
            return new Creator(createdId, r, r::remove);
        }
    }

    public static Creator.Flow create(RealmResource realmResource, AuthenticationFlowRepresentation rep) {
        final AuthenticationManagementResource authMgmgRes = realmResource.flows();
        try (Response response = authMgmgRes.createFlow(rep)) {
            String createdId = getCreatedId(response);
            LOG.debugf("Created flow ID %s", createdId);
            return new Flow(createdId, rep.getAlias(), authMgmgRes, () -> authMgmgRes.deleteFlow(createdId));
        }
    }

    public static Creator<IdentityProviderResource> create(RealmResource realmResource, IdentityProviderRepresentation rep) {
        final IdentityProvidersResource res = realmResource.identityProviders();
        Assert.assertThat("Identity provider alias must be specified", rep.getAlias(), Matchers.notNullValue());
        try (Response response = res.create(rep)) {
            String createdId = getCreatedId(response);
            final IdentityProviderResource r = res.get(rep.getAlias());
            LOG.debugf("Created identity provider ID %s", createdId);
            return new Creator(createdId, r, r::remove);
        }
    }

    private final String id;
    private final T resource;
    private final Runnable closer;
    private final AtomicBoolean closerRan = new AtomicBoolean(false);

    private Creator(String id, T resource, Runnable closer) {
        this.id = id;
        this.resource = resource;
        this.closer = closer;
    }

    public String id() {
        return this.id;
    }

    public T resource() {
        return this.resource;
    }

    @Override
    public void close() {
        if (this.closerRan.compareAndSet(false, true)) {
            LOG.debugf("Removing resource ID %s", id);
            try {
                closer.run();
            } catch (javax.ws.rs.NotFoundException ex) {
                LOG.debugf("Resource with ID %s perhaps removed in meantime.", id);
            }
        } else {
            LOG.debugf("Already removed resource ID %s", id);
        }
    }

    public static class Flow extends Creator<AuthenticationManagementResource> {

        private final String alias;

        public Flow(String id, String alias, AuthenticationManagementResource resource, Runnable closer) {
            super(id, resource, closer);
            this.alias = alias;
        }

        public AuthenticationExecutionInfoRepresentation addExecution(String providerId) {
            Map<String, String> c = new HashMap<>();
            c.put("provider", providerId);
            resource().addExecution(alias, c);  // addExecution only handles "provider" in data
            return resource().getExecutions(alias).stream()
              .filter(aer -> Objects.equals(providerId, aer.getProviderId()))
              .findFirst()
              .orElse(null);
        }

    }
}
