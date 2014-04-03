package org.keycloak.services;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

import java.util.HashMap;
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
                provider = providerFactory.create();
                providers.put(hash, provider);
            }
        }
        return provider;
    }

    public <T extends Provider> Set<String> listProviderIds(Class<T> clazz) {
        return factory.providerIds(clazz);
    }

    public void close() {
        for (Provider p : providers.values()) {
            p.close();
        }
    }

}
