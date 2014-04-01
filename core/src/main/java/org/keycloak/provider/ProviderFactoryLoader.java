package org.keycloak.provider;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ProviderFactoryLoader<P extends ProviderFactory> implements Iterable<P> {

    private ServiceLoader<P> serviceLoader;

    private ProviderFactoryLoader(ServiceLoader<P> serviceLoader) {
        this.serviceLoader = serviceLoader;
    }

    public static <P extends ProviderFactory> ProviderFactoryLoader<P> load(Class<P> service) {
        return new ProviderFactoryLoader(ServiceLoader.load(service));
    }

    public static <P extends ProviderFactory> ProviderFactoryLoader<P> load(Class<P> service, ClassLoader loader) {
        return new ProviderFactoryLoader(ServiceLoader.load(service, loader));
    }

    public P find(String id) {
        Iterator<P> itr = iterator();
        while (itr.hasNext()) {
            P p = itr.next();
            if (p.getId() != null && p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    @Override
    public Iterator<P> iterator() {
        return new ProviderFactoryIterator(serviceLoader.iterator());
    }

    public void close() {

    }

    private static class ProviderFactoryIterator<P> implements Iterator<P> {

        private Iterator<P> itr;

        private P next;

        private ProviderFactoryIterator(Iterator<P> itr) {
            this.itr = itr;
            setNext();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public P next() {
            P n = next;
            setNext();
            return n;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private void setNext() {
            next = null;
            while (itr.hasNext()) {
                if (itr.hasNext()) {
                    P n = itr.next();
                    if (!System.getProperties().containsKey(n.getClass().getName() + ".disabled")) {
                        next = n;
                        return;
                    }
                }
            }
        }

    }

}
