package org.keycloak.provider;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ProviderFactoryLoader<T extends Provider> implements Iterable<ProviderFactory<T>> {

    private Map<String, ProviderFactory<T>> factories = new HashMap<String, ProviderFactory<T>>();

    private ProviderFactoryLoader(ServiceLoader<? extends ProviderFactory> serviceLoader) {
        for (ProviderFactory p : serviceLoader) {
            if (!System.getProperties().containsKey(p.getClass().getName() + ".disabled")) {
                if (p.lazyLoad()) {
                    p = new LazyProviderFactory(p);
                }
                factories.put(p.getId(), p);
            }
        }
    }

    public static ProviderFactoryLoader create(Class<? extends ProviderFactory> service) {
        return new ProviderFactoryLoader(ServiceLoader.load(service));
    }

    public static ProviderFactoryLoader create(Class<? extends ProviderFactory> service, ClassLoader loader) {
        return new ProviderFactoryLoader(ServiceLoader.load(service, loader));
    }

    public ProviderFactory find(String id) {
        return factories.get(id);
    }

    @Override
    public Iterator<ProviderFactory<T>> iterator() {
        return factories.values().iterator();
    }

    public Set<String> providerIds() {
        return factories.keySet();
    }

    public void init() {
        for (ProviderFactory p : factories.values()) {
            p.init();
        }
    }

    public void close() {
        for (ProviderFactory p : factories.values()) {
            p.close();
        }
    }

    private class LazyProviderFactory<T extends Provider> implements ProviderFactory<T> {

        private volatile boolean initialized = false;

        private ProviderFactory<T> factory;

        private LazyProviderFactory(ProviderFactory<T> factory) {
            this.factory = factory;
        }

        @Override
        public synchronized T create() {
            if (!initialized) {
                factory.init();
                initialized = true;
            }
            return factory.create();
        }

        @Override
        public void init() {
            // do nothing
        }

        @Override
        public void close() {
            factory.close();
        }

        @Override
        public String getId() {
            return factory.getId();
        }

        @Override
        public boolean lazyLoad() {
            return false;
        }
    }

}
