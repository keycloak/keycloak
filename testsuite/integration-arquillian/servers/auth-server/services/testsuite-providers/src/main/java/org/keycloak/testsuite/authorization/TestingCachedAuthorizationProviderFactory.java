package org.keycloak.testsuite.authorization;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProviderFactory;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.DefaultPolicyEvaluator;
import org.keycloak.authorization.policy.evaluation.PolicyEvaluator;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.infinispan.authorization.CachedAuthorizationProvider;
import org.keycloak.models.cache.infinispan.authorization.PermissionCacheManager;
import org.keycloak.provider.ProviderFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.keycloak.models.cache.infinispan.authorization.InfinispanCacheStoreFactoryProviderFactory.AUTHORIZATION_INVALIDATION_EVENTS;

public class TestingCachedAuthorizationProviderFactory implements AuthorizationProviderFactory {

    private static final Logger LOGGER = Logger.getLogger(TestingCachedAuthorizationProviderFactory.class);
    private Map<String, PolicyProviderFactory> policyProviderFactories;
    private PolicyEvaluator policyEvaluator = new DefaultPolicyEvaluator();
    public static PermissionCacheManager permissionCacheManager = null;
    public static boolean wasCleared = false;

    @Override
    public CachedAuthorizationProvider create(KeycloakSession session) {
        return create(session, session.getContext().getRealm());
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        policyProviderFactories = configurePolicyProviderFactories(factory);
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "testing_cached";
    }

    @Override
    public CachedAuthorizationProvider create(KeycloakSession session, RealmModel realm) {
        lazyInit(session);
        return new TestingCachedAuthorizationProvider(session, realm, policyProviderFactories, policyEvaluator, permissionCacheManager);
    }

    private void lazyInit(KeycloakSession session) {
        if (permissionCacheManager == null) {
            synchronized (this) {
                if (permissionCacheManager == null) {
                    permissionCacheManager = new PermissionCacheManager(session);
                }

                ClusterProvider cluster = session.getProvider(ClusterProvider.class);
                cluster.registerListener(AUTHORIZATION_INVALIDATION_EVENTS, (ClusterEvent event) -> {
                    if (event instanceof Policy.PolicyChangedEvent) {
                        wasCleared = true;
                    }
                });
            }
        }
    }

    private Map<String, PolicyProviderFactory> configurePolicyProviderFactories(KeycloakSessionFactory keycloakSessionFactory) {
        List<ProviderFactory> providerFactories = keycloakSessionFactory.getProviderFactories(PolicyProvider.class);

        if (providerFactories.isEmpty()) {
            throw new RuntimeException("Could not find any policy provider.");
        }

        HashMap<String, PolicyProviderFactory> providers = new HashMap<>();

        providerFactories.forEach(providerFactory -> providers.put(providerFactory.getId(), (PolicyProviderFactory) providerFactory));

        return providers;
    }
}
