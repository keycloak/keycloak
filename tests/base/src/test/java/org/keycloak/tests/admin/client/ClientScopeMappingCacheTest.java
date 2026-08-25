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
package org.keycloak.tests.admin.client;

import java.util.List;
import java.util.Objects;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.infinispan.ClientAdapter;
import org.keycloak.models.cache.infinispan.ClientScopeAdapter;
import org.keycloak.models.cache.infinispan.RealmCacheSession;
import org.keycloak.models.cache.infinispan.entities.CachedClient;
import org.keycloak.models.cache.infinispan.entities.CachedClientScope;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Regression test for gh-51589: stale cached role IDs in client scope mappings
 * must not surface as null RoleModel entries.
 */
@KeycloakIntegrationTest
public class ClientScopeMappingCacheTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void getScopeMappingsStreamFiltersStaleRoleIds() {
        String realmName = managedRealm.getName();

        ClientRepresentation clientRep = ClientConfigBuilder.create()
                .clientId("scope-cache-test-client")
                .fullScopeEnabled(false)
                .build();
        String clientUuid;
        try (Response resp = managedRealm.admin().clients().create(clientRep)) {
            clientUuid = ApiUtil.getCreatedId(resp);
        }
        managedRealm.cleanup().add(r -> r.clients().get(clientUuid).remove());

        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setName("scope-cache-live-role");
        managedRealm.admin().roles().create(roleRep);
        managedRealm.cleanup().add(r -> r.roles().deleteRole("scope-cache-live-role"));
        roleRep = managedRealm.admin().roles().get("scope-cache-live-role").toRepresentation();

        managedRealm.admin().clients().get(clientUuid).getScopeMappings()
                .realmLevel().add(List.of(roleRep));

        // Warm the Infinispan cache by loading the client through the model layer.
        String cu = clientUuid;
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            ClientModel client = session.clients().getClientById(realm, cu);
            assertInstanceOf(ClientAdapter.class, client);
            assertEquals(1, client.getScopeMappingsStream().count());
        });

        // Inject a nonexistent role ID into the CachedClient's scope set,
        // simulating a stale entry from a concurrent role deletion whose
        // cache invalidation has not yet reached this node.
        runOnServer.run(session -> {
            RealmCacheSession cacheSession = (RealmCacheSession) session.getProvider(CacheRealmProvider.class);
            CachedClient cached = cacheSession.getCache().get(cu, CachedClient.class);
            assertNotNull(cached);
            cached.getScope().add("nonexistent-stale-role-id");
        });

        // Without the fix, the stale ID resolves to null and leaks into the
        // stream, causing an NPE in downstream composite-role expansion.
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            ClientModel client = session.clients().getClientById(realm, cu);
            assertInstanceOf(ClientAdapter.class, client);

            List<RoleModel> result = client.getScopeMappingsStream().toList();
            assertFalse(result.stream().anyMatch(Objects::isNull),
                    "scope mappings must not contain null entries from stale cached role IDs");
            assertEquals(1, result.size());
        });
    }

    @Test
    public void clientScopeGetScopeMappingsStreamFiltersStaleRoleIds() {
        String realmName = managedRealm.getName();

        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("scope-cache-test-scope");
        scopeRep.setProtocol("openid-connect");
        String scopeId;
        try (Response resp = managedRealm.admin().clientScopes().create(scopeRep)) {
            scopeId = ApiUtil.getCreatedId(resp);
        }
        managedRealm.cleanup().add(r -> r.clientScopes().get(scopeId).remove());

        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setName("scope-cache-cs-live-role");
        managedRealm.admin().roles().create(roleRep);
        managedRealm.cleanup().add(r -> r.roles().deleteRole("scope-cache-cs-live-role"));
        roleRep = managedRealm.admin().roles().get("scope-cache-cs-live-role").toRepresentation();

        managedRealm.admin().clientScopes().get(scopeId).getScopeMappings()
                .realmLevel().add(List.of(roleRep));

        String si = scopeId;
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            ClientScopeModel clientScope = session.clientScopes().getClientScopeById(realm, si);
            assertInstanceOf(ClientScopeAdapter.class, clientScope);
            assertEquals(1, clientScope.getScopeMappingsStream().count());
        });

        runOnServer.run(session -> {
            RealmCacheSession cacheSession = (RealmCacheSession) session.getProvider(CacheRealmProvider.class);
            CachedClientScope cached = cacheSession.getCache().get(si, CachedClientScope.class);
            assertNotNull(cached);
            cached.getScope().add("nonexistent-stale-role-id");
        });

        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            ClientScopeModel clientScope = session.clientScopes().getClientScopeById(realm, si);
            assertInstanceOf(ClientScopeAdapter.class, clientScope);

            List<RoleModel> result = clientScope.getScopeMappingsStream().toList();
            assertFalse(result.stream().anyMatch(Objects::isNull),
                    "client scope mappings must not contain null entries from stale cached role IDs");
            assertEquals(1, result.size());
        });
    }
}
