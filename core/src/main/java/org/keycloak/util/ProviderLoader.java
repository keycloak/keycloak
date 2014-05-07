package org.keycloak.util;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ProviderLoader<T> implements Iterable<T> {

    private final ServiceLoader<T> serviceLoader;

    public static <T> Iterable<T> load(Class<T> service) {
        ServiceLoader<T> providers = ServiceLoader.load(service);
        return new ProviderLoader(providers);
    }

    private ProviderLoader(ServiceLoader<T> serviceLoader) {
        this.serviceLoader = serviceLoader;
    }

    @Override
    public Iterator iterator() {
        return new ProviderIterator(serviceLoader.iterator());
    }

    private static class ProviderIterator<T> implements Iterator<T> {

        private final Iterator<T> itr;

        private T next;

        private ProviderIterator(Iterator<T> itr) {
            this.itr = itr;
            setNext();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public T next() {
            T n = next;
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
                    T n = itr.next();
                    if (!System.getProperties().containsKey(n.getClass().getName() + ".disabled")) {
                        next = n;
                        return;
                    }
                }
            }
        }

    }

}
