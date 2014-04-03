package org.keycloak.services;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderFactoryLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DefaultProviderSessionFactory implements ProviderSessionFactory {

    private Map<Class<? extends Provider>, ProviderFactoryLoader> loaders = new HashMap<Class<? extends Provider>, ProviderFactoryLoader>();
    private Map<Class<? extends Provider>, String> defaultFactories = new HashMap<Class<? extends Provider>, String>();

    public ProviderSession createSession() {
        return new DefaultProviderSession(this);
    }

    public void close() {
        for (ProviderFactoryLoader loader : loaders.values()) {
            loader.close();
        }
    }

    public <T extends Provider> ProviderFactory<T> getProviderFactory(Class<T> clazz) {
        String id = defaultFactories.get(clazz);
        if (id == null) {
            return null;
        }
        return getProviderFactory(clazz, id);
    }

    public <T extends Provider> ProviderFactory<T> getProviderFactory(Class<T> clazz, String id) {
        ProviderFactoryLoader loader = getLoader(clazz);
        return loader != null ? loader.find(id) : null;
    }

    public Set<String> providerIds(Class<? extends Provider> clazz) {
        ProviderFactoryLoader loader = getLoader(clazz);
        return loader != null ? loader.providerIds() : null;
    }

    public void registerLoader(Class<? extends Provider> clazz, ProviderFactoryLoader loader) {
        loaders.put(clazz, loader);

    }

    public void registerLoader(Class<? extends Provider> clazz, ProviderFactoryLoader loader, String defaultProvider) {
        loaders.put(clazz, loader);
        defaultFactories.put(clazz, defaultProvider);

    }

    public void init() {
        for (ProviderFactoryLoader l : loaders.values()) {
            l.init();
        }
    }

    private <T extends Provider> ProviderFactoryLoader getLoader(Class<T> clazz) {
        return loaders.get(clazz);
    }

}
