package org.keycloak.models.sessions.infinispan.remote;

import org.infinispan.client.hotrod.RemoteCache;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.sessions.infinispan.InfinispanAuthenticationSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.entities.RootAuthenticationSessionEntity;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.sessions.AuthenticationSessionProviderFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.getRemoteCache;
import static org.keycloak.models.sessions.infinispan.InfinispanAuthenticationSessionProviderFactory.DEFAULT_AUTH_SESSIONS_LIMIT;

public class RemoteInfinispanAuthenticationSessionProviderFactory implements AuthenticationSessionProviderFactory<RemoteInfinispanAuthenticationSessionProvider> {

    private final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());
    public static final String PROVIDER_ID = "remote-infinispan";

    private int authSessionsLimit;
    private RemoteCache<String, RootAuthenticationSessionEntity> cache;

    @SuppressWarnings("deprecation")
    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.MULTI_SITE) && Profile.isFeatureEnabled(Profile.Feature.REMOTE_CACHE);
    }

    @Override
    public RemoteInfinispanAuthenticationSessionProvider create(KeycloakSession session) {
        return new RemoteInfinispanAuthenticationSessionProvider(session, this);
    }

    @Override
    public void init(Config.Scope config) {
        authSessionsLimit = InfinispanAuthenticationSessionProviderFactory.getAuthSessionsLimit(config);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        cache = getRemoteCache(factory, AUTHENTICATION_SESSIONS_CACHE_NAME);
        logger.debugf("Provided initialized. session limit=%s", authSessionsLimit);
    }

    @Override
    public void close() {
        cache = null;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("authSessionsLimit")
                .type("int")
                .helpText("The maximum number of concurrent authentication sessions per RootAuthenticationSession.")
                .defaultValue(DEFAULT_AUTH_SESSIONS_LIMIT)
                .add()
                .build();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public int order() {
        // use the same priority as the embedded based one
        return InfinispanAuthenticationSessionProviderFactory.PROVIDER_PRIORITY;
    }

    public int getAuthSessionsLimit() {
        return authSessionsLimit;
    }

    public RemoteCache<String, RootAuthenticationSessionEntity> getCache() {
        return cache;
    }
}
