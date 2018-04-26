package org.keycloak.performance.iteration;

import java.util.AbstractList;
import java.util.List;
import static org.keycloak.performance.iteration.RandomIntegers.getRandomIntegers;
import static org.keycloak.performance.iteration.UniqueRandomIntegers.getUniqueRandomIntegers;

/**
 *
 * @author tkyjovsk
 * @param <T>
 */
public class RandomSublist<T> extends AbstractList<T> {

    private final List<T> originalList;

    private final List<Integer> randomIndexesOfOriginalList;

    private final int size;

    public RandomSublist(List<T> originalList, int seed, int sublistSize, boolean unique) {
        this.originalList = originalList;
        this.randomIndexesOfOriginalList = unique
                ? getUniqueRandomIntegers(seed, originalList.size())
                : getRandomIntegers(seed, originalList.size());
        this.size = sublistSize;
    }

    public RandomSublist(List<T> originalList, int seed, int sublistSize) {
        this(originalList, seed, sublistSize, false);
    }

    @Override
    public T get(int index) {
        return originalList.get(randomIndexesOfOriginalList.get(index));
    }

    @Override
    public int size() {
        return size;
    }

}
