package org.keycloak.performance.iteration;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.Validate;
import static org.keycloak.performance.iteration.RandomIntegers.getRandomIntegers;
import static org.keycloak.performance.iteration.UniqueRandomIntegers.getUniqueRandomIntegers;
import org.keycloak.performance.util.ValidateNumber;

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
        Validate.notNull(originalList);
        this.originalList = originalList;
        ValidateNumber.isInRange(sublistSize, 0, originalList.size());
        this.size = sublistSize;
        this.randomIndexesOfOriginalList = originalList.isEmpty()
                ? Collections.<Integer>emptyList()
                : (unique
                        ? getUniqueRandomIntegers(seed, originalList.size())
                        : getRandomIntegers(seed, originalList.size()));
    }

    public RandomSublist(List<T> originalList, int seed, int sublistSize) {
        this(originalList, seed, sublistSize, false);
    }

    @Override
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }
        return originalList.get(randomIndexesOfOriginalList.get(index));
    }

    @Override
    public int size() {
        return size;
    }

}
