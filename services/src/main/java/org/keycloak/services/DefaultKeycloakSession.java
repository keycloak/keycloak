package org.keycloak.services;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.ModelProvider;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.cache.CacheModelProvider;
import org.keycloak.models.cache.CacheUserProvider;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultKeycloakSession implements KeycloakSession {

    private final DefaultKeycloakSessionFactory factory;
    private final Map<Integer, Provider> providers = new HashMap<Integer, Provider>();
    private final DefaultKeycloakTransactionManager transactionManager;
    private ModelProvider model;
    private UserProvider userModel;
    private UserSessionProvider sessionProvider;

    public DefaultKeycloakSession(DefaultKeycloakSessionFactory factory) {
        this.factory = factory;
        this.transactionManager = new DefaultKeycloakTransactionManager();
    }

    private ModelProvider getModelProvider() {
        if (factory.getDefaultProvider(CacheModelProvider.class) != null) {
            return getProvider(CacheModelProvider.class);
        } else {
            return getProvider(ModelProvider.class);
        }
    }

    private UserProvider getUserProvider() {
        if (factory.getDefaultProvider(CacheUserProvider.class) != null) {
            return getProvider(CacheUserProvider.class);
        } else {
            return getProvider(UserProvider.class);
        }
    }

    @Override
    public KeycloakTransactionManager getTransaction() {
        return transactionManager;
    }

    public <T extends Provider> T getProvider(Class<T> clazz) {
        Integer hash = clazz.hashCode();
        T provider = (T) providers.get(hash);
        if (provider == null) {
            ProviderFactory<T> providerFactory = factory.getProviderFactory(clazz);
            if (providerFactory != null) {
                provider = providerFactory.create(this);
                providers.put(hash, provider);
            }
        }
        return provider;
    }

    public <T extends Provider> T getProvider(Class<T> clazz, String id) {
        Integer hash = clazz.hashCode() + id.hashCode();
        T provider = (T) providers.get(hash);
        if (provider == null) {
            ProviderFactory<T> providerFactory = factory.getProviderFactory(clazz, id);
            if (providerFactory != null) {
                provider = providerFactory.create(this);
                providers.put(hash, provider);
            }
        }
        return provider;
    }

    public <T extends Provider> Set<String> listProviderIds(Class<T> clazz) {
        return factory.getAllProviderIds(clazz);
    }

    @Override
    public <T extends Provider> Set<T> getAllProviders(Class<T> clazz) {
        Set<T> providers = new HashSet<T>();
        for (String id : listProviderIds(clazz)) {
            providers.add(getProvider(clazz, id));
        }
        return providers;
    }

    @Override
    public ModelProvider model() {
        if (model == null) {
            model = getModelProvider();
        }
        return model;
    }

    @Override
    public UserProvider users() {
        if (userModel == null) {
            userModel = getUserProvider();
        }
        return userModel;
    }

    @Override
    public UserSessionProvider sessions() {
        if (sessionProvider == null) {
            sessionProvider = getProvider(UserSessionProvider.class);
        }
        return sessionProvider;
    }

    public void close() {
        for (Provider p : providers.values()) {
            p.close();
        }
    }

}
