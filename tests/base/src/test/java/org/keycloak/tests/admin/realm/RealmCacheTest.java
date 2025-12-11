package org.keycloak.tests.admin.realm;

import jakarta.ws.rs.core.Response;

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.infinispan.Cache;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class RealmCacheTest extends AbstractRealmTest {

    @InjectRealm(ref = "master", attachTo = "master")
    ManagedRealm masterRealm;

    @InjectRunOnServer(ref = "master", realmRef = "master")
    RunOnServerClient masterRunOnServer;

    @Test
    public void clearRealmCache() {
        String realmId = managedRealm.getId();
        assertTrue(runOnServer.fetch(s -> {
            InfinispanConnectionProvider provider = s.getProvider(InfinispanConnectionProvider.class);
            Cache<Object, Object> cache = provider.getCache("realms");
            return cache.containsKey(realmId);
        }, Boolean.class));

        managedRealm.admin().clearRealmCache();

        // Using master realm to verify that managedRealm cache is empty.
        assertFalse(masterRunOnServer.fetch(s -> {
            InfinispanConnectionProvider provider = s.getProvider(InfinispanConnectionProvider.class);
            Cache<Object, Object> cache = provider.getCache("realms");
            return cache.containsKey(realmId);
        }, Boolean.class));

        // The Admin event must be checked after the verification, because the event poll recreates the cache!
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, "clear-realm-cache", ResourceType.REALM);
    }

    @Test
    public void clearUserCache() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("clearcacheuser");
        Response response = managedRealm.admin().users().create(user);
        String userId = ApiUtil.getCreatedId(response);
        response.close();
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.userResourcePath(userId), user, ResourceType.USER);

        managedRealm.admin().users().get(userId).toRepresentation();

        assertTrue(runOnServer.fetch(s -> {
            InfinispanConnectionProvider provider = s.getProvider(InfinispanConnectionProvider.class);
            Cache<Object, Object> cache = provider.getCache("users");
            return cache.containsKey(userId);
        }, Boolean.class));

        managedRealm.admin().clearUserCache();
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, "clear-user-cache", ResourceType.REALM);

        assertFalse(runOnServer.fetch(s -> {
            InfinispanConnectionProvider provider = s.getProvider(InfinispanConnectionProvider.class);
            Cache<Object, Object> cache = provider.getCache("users");
            return cache.containsKey(userId);
        }, Boolean.class));
    }

    // NOTE: clearKeysCache tested in KcOIDCBrokerWithSignatureTest
}
