package org.keycloak.models.cache.infinispan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.infinispan.entities.CachedClient;

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
