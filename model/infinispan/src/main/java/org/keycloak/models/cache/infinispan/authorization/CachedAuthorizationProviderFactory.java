package org.keycloak.models.cache.infinispan.authorization;

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
import org.keycloak.provider.ProviderFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.keycloak.models.cache.infinispan.InfinispanCacheRealmProviderFactory.REALM_CLEAR_CACHE_EVENTS;
import static org.keycloak.models.cache.infinispan.authorization.InfinispanCacheStoreFactoryProviderFactory.AUTHORIZATION_CLEAR_CACHE_EVENTS;
import static org.keycloak.models.cache.infinispan.authorization.InfinispanCacheStoreFactoryProviderFactory.AUTHORIZATION_INVALIDATION_EVENTS;

public class CachedAuthorizationProviderFactory implements AuthorizationProviderFactory {

    private static final Logger LOGGER = Logger.getLogger(CachedAuthorizationProviderFactory.class);
    private Map<String, PolicyProviderFactory> policyProviderFactories;
    private PolicyEvaluator policyEvaluator = new DefaultPolicyEvaluator();
    private PermissionCacheManager permissionCacheManager = null;

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
        return "cached";
    }

    @Override
    public CachedAuthorizationProvider create(KeycloakSession session, RealmModel realm) {
        lazyInit(session);
        return new CachedAuthorizationProvider(session, realm, policyProviderFactories, policyEvaluator, permissionCacheManager);
    }

    private void lazyInit(KeycloakSession session) {
        if (permissionCacheManager == null) {
            synchronized (this) {
                if (permissionCacheManager == null) {
                    permissionCacheManager = new PermissionCacheManager(session);
                    ClusterProvider cluster = session.getProvider(ClusterProvider.class);
                    cluster.registerListener(AUTHORIZATION_INVALIDATION_EVENTS, (ClusterEvent event) -> {
                        if (event instanceof Policy.PolicyChangedEvent) {
                            permissionCacheManager.clear();
                        }
                    });

                    cluster.registerListener(AUTHORIZATION_CLEAR_CACHE_EVENTS, (ClusterEvent event) -> permissionCacheManager.clear());
                    cluster.registerListener(REALM_CLEAR_CACHE_EVENTS, (ClusterEvent event) -> permissionCacheManager.clear());

                    LOGGER.debug("Registered cluster listeners");
                }
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
