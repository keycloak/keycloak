package org.keycloak.services;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderSession;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultProviderSession implements ProviderSession {
    private DefaultProviderSessionFactory factory;
    private Map<Integer, Provider> providers = new HashMap<Integer, Provider>();

    public DefaultProviderSession(DefaultProviderSessionFactory factory) {
        this.factory = factory;
    }

    public <T extends Provider> T getProvider(Class<T> clazz) {
        String id = factory.getDefaultProvider(clazz);
        return id != null ? getProvider(clazz, id) : null;
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
        return factory.providerIds(clazz);
    }

    @Override
    public <T extends Provider> Set<T> getAllProviders(Class<T> clazz) {
        Set<String> providerIds = listProviderIds(clazz);
        Set<T> providers = new HashSet<T>();
        for (String providerId : providerIds) {
            providers.add(getProvider(clazz, providerId));
        }
        return providers;
    }

    public void close() {
        for (Provider p : providers.values()) {
            p.close();
        }
    }

}
