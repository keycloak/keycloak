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
package org.keycloak.models.cache.infinispan;

import java.util.List;
import java.util.Set;

import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.infinispan.entities.CachedClient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientAdapterTest {

    @Test
    void getScopeMappingsStreamFiltersUnresolvableRoleIds() {
        RealmModel realm = mock(RealmModel.class);
        RealmCacheSession cacheSession = mock(RealmCacheSession.class);
        CachedClient cached = mock(CachedClient.class);

        RoleModel liveRole = mock(RoleModel.class);
        when(cached.getScope()).thenReturn(Set.of("stale-role-id", "live-role-id"));
        // simulates a role deleted on another node whose cache invalidation
        // hasn't reached this one yet
        when(cacheSession.getRoleById(eq(realm), eq("stale-role-id"))).thenReturn(null);
        when(cacheSession.getRoleById(eq(realm), eq("live-role-id"))).thenReturn(liveRole);

        ClientAdapter adapter = new ClientAdapter(realm, cached, cacheSession);

        List<RoleModel> result = adapter.getScopeMappingsStream().toList();

        assertEquals(List.of(liveRole), result,
                "a scope mapping pointing at an unresolvable role must be silently dropped, not surfaced as null");
    }
}
