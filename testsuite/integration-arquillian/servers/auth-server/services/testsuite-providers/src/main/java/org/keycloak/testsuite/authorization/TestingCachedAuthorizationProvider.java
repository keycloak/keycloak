package org.keycloak.testsuite.authorization;

import org.keycloak.authorization.policy.evaluation.PolicyEvaluator;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.infinispan.authorization.CachedAuthorizationProvider;
import org.keycloak.models.cache.infinispan.authorization.PermissionCacheManager;

import java.util.Map;

public class TestingCachedAuthorizationProvider extends CachedAuthorizationProvider {

    public static boolean wasCleared = false;

    public TestingCachedAuthorizationProvider(KeycloakSession session, RealmModel realm, Map<String, PolicyProviderFactory> policyProviderFactories, PolicyEvaluator policyEvaluator, PermissionCacheManager permissionCacheManager) {
        super(session, realm, policyProviderFactories, policyEvaluator, permissionCacheManager);
    }

    @Override
    public void clear() {
        wasCleared = true;
        // Empty so that we can test
    }
}
