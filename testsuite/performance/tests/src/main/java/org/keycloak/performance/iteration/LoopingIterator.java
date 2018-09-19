package org.keycloak.performance.iteration;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class LoopingIterator<T> implements Iterator<T> {

    private Collection<T> collection;
    private Iterator<T> it;

    public LoopingIterator(Collection<T> collection) {
        this.collection = collection;
        it = collection.iterator();
    }

    @Override
    public synchronized boolean hasNext() {
        return true;
    }

    @Override
    public synchronized T next() {
        if (!it.hasNext()) {
            it = collection.iterator();
        }
        return it.next();
    }
}
