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
package org.keycloak.testsuite.model.infinispan;

import java.util.List;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleProvider;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RequireProvider(RealmProvider.class)
@RequireProvider(ClientProvider.class)
@RequireProvider(RoleProvider.class)
public class ClientScopeMappingCacheTest extends KeycloakModelTest {

    private String realmId;
    private String clientDbId;
    private String liveRoleId;
    private String staleRoleId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = createRealm(s, "client-scope-cache-test");
        realmId = realm.getId();

        ClientModel client = s.clients().addClient(realm, "test-client");
        clientDbId = client.getId();

        RoleModel liveRole = client.addRole("live-role");
        RoleModel staleRole = client.addRole("stale-role");
        liveRoleId = liveRole.getId();
        staleRoleId = staleRole.getId();

        client.addScopeMapping(liveRole);
        client.addScopeMapping(staleRole);
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.realms().removeRealm(realm.getId());
    }

    @Test
    public void getScopeMappingsStreamFiltersUnresolvableRoleIds() {
        // Warm the cache with both currently resolvable role IDs.
        withRealm(realmId, (session, realm) -> {
            ClientModel client = session.clients().getClientById(realm, clientDbId);
            List<RoleModel> warm = client.getScopeMappingsStream().toList();
            assertEquals(2, warm.size());
            return null;
        });

        // Remove the role through the role store so the cached scope mapping
        // retains a stale role ID that can no longer be resolved.
        withRealm(realmId, (session, realm) -> {
            RoleModel stale = session.roles().getRoleById(realm, staleRoleId);
            session.roles().removeRole(stale);
            return null;
        });

        // The cache layer must drop the unresolved mapping rather than return
        // a null RoleModel or throw an NPE.
        withRealm(realmId, (session, realm) -> {
            ClientModel client = session.clients().getClientById(realm, clientDbId);
            List<RoleModel> result = client.getScopeMappingsStream().toList();

            assertFalse("scope mappings must not contain null entries",
                    result.stream().anyMatch(r -> r == null));
            assertEquals(1, result.size());
            assertTrue(result.stream().anyMatch(r -> liveRoleId.equals(r.getId())));
            return null;
        });
    }
}
