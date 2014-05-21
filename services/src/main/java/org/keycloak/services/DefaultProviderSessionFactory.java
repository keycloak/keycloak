package org.keycloak.services;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderSession;
import org.keycloak.provider.ProviderSessionFactory;
import org.keycloak.provider.Spi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

public class DefaultProviderSessionFactory implements ProviderSessionFactory {

    private static final Logger log = Logger.getLogger(DefaultProviderSessionFactory.class);

    private Map<Class<? extends Provider>, String> provider = new HashMap<Class<? extends Provider>, String>();
    private Map<Class<? extends Provider>, Map<String, ProviderFactory>> factoriesMap = new HashMap<Class<? extends Provider>, Map<String, ProviderFactory>>();

    public void init() {
        for (Spi spi : ServiceLoader.load(Spi.class)) {
            Map<String, ProviderFactory> factories = new HashMap<String, ProviderFactory>();
            factoriesMap.put(spi.getProviderClass(), factories);

            String provider = Config.getProvider(spi.getName());
            if (provider != null) {
                this.provider.put(spi.getProviderClass(), provider);

                ProviderFactory factory = loadProviderFactory(spi, provider);
                Config.Scope scope = Config.scope(spi.getName(), provider);
                factory.init(scope);

                factories.put(factory.getId(), factory);

                log.info("Loaded SPI " + spi.getName() + " (provider = " + provider + ")");
            } else {
                for (ProviderFactory factory : ServiceLoader.load(spi.getProviderFactoryClass())) {
                    Config.Scope scope = Config.scope(spi.getName(), factory.getId());
                    factory.init(scope);

                    factories.put(factory.getId(), factory);
                }

                if (factories.size() == 1) {
                    provider = factories.values().iterator().next().getId();
                    this.provider.put(spi.getProviderClass(), provider);

                    log.info("Loaded SPI " + spi.getName() + " (provider = " + provider + ")");
                } else {
                    log.info("Loaded SPI " + spi.getName() + " (providers = " + factories.keySet() + ")");
                }
            }
        }
    }

    private ProviderFactory loadProviderFactory(Spi spi, String id) {
        for (ProviderFactory factory : ServiceLoader.load(spi.getProviderFactoryClass())) {
            if (factory.getId().equals(id)){
                return factory;
            }
        }
        throw new RuntimeException("Failed to find provider " + id + " for " + spi.getName());
    }

    public ProviderSession createSession() {
        return new DefaultProviderSession(this);
    }

    <T extends Provider> ProviderFactory<T> getProviderFactory(Class<T> clazz) {
         return getProviderFactory(clazz, provider.get(clazz));
    }

    <T extends Provider> ProviderFactory<T> getProviderFactory(Class<T> clazz, String id) {
         return factoriesMap.get(clazz).get(id);
    }

    <T extends Provider> Set<String> getAllProviderIds(Class<T> clazz) {
        Set<String> ids = new HashSet<String>();
        for (ProviderFactory f : factoriesMap.get(clazz).values()) {
            ids.add(f.getId());
        }
        return ids;
    }

    public void close() {
        for (Map<String, ProviderFactory> factories : factoriesMap.values()) {
            for (ProviderFactory factory : factories.values()) {
                factory.close();
            }
        }
    }

}
