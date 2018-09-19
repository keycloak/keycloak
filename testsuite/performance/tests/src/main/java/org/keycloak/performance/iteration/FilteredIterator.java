package org.keycloak.performance.iteration;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class FilteredIterator<T> implements Iterator<T> {

    private final Iterator<T> delegate;
    private final Predicate<T> filter;
    private T next;

    public FilteredIterator(Iterator<T> delegate, Predicate<T> filter) {
        this.delegate = delegate;
        this.filter = filter;
    }

    @Override
    public synchronized boolean hasNext() {
        // check matching one
        if (next != null) {
            return true;
        }
        if (delegate.hasNext()) {
            T v = delegate.next();
            if (filter.test(v)) {
                next = v;
                return true;
            } else {
                // for infinite iterators it's up to provided 'filter' to make sure looping is not infinite
                // otherwise this may result in StackOverflowError
                return hasNext();
            }
        }
        return false;
    }

    @Override
    public synchronized T next() {
        if (hasNext()) {
            T v = next;
            next = null;
            return v;
        }
        throw new NoSuchElementException();
    }
}
