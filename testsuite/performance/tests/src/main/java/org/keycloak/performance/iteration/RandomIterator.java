package org.keycloak.performance.iteration;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author tkyjovsk
 */
public class RandomIterator<T> implements Iterator<T> {

    List<T> list;

    public RandomIterator(List<T> iteratedList) {
        this.list = iteratedList;
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public T next() {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

}
