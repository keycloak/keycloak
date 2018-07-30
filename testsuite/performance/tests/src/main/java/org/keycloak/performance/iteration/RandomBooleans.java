package org.keycloak.performance.iteration;

import java.util.AbstractList;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.collections.map.LRUMap;
import static org.keycloak.performance.iteration.RandomIntegers.RANDOMS_CACHE_SIZE;
import static org.keycloak.performance.iteration.RandomIntegers.getRandomIntegers;
import org.keycloak.performance.util.ValidateNumber;

/**
 *
 * @author tkyjovsk
 */
public class RandomBooleans extends AbstractList<Boolean> {

    private final RandomIntegers randomIntegers;
    private final int truesPercentage;

    /**
     *
     * @param seed Random sequence seed.
     * @param truesPercentage Percentage of the sequence values which should be
     * true. Valid range is 0-100.
     */
    public RandomBooleans(int seed, int truesPercentage) {
        randomIntegers = getRandomIntegers(seed, 100);
        ValidateNumber.isInRange(truesPercentage, 0, 100);
        this.truesPercentage = truesPercentage;
    }

    public RandomBooleans(int seed) {
        this(seed, 50);
    }

    @Override
    public Boolean get(int index) {
        return randomIntegers.get(index) < truesPercentage;
    }

    @Override
    public int size() {
        return Integer.MAX_VALUE;
    }

    private static final Map<Integer, RandomBooleans> RANDOM_BOOLS_CACHE
            = Collections.synchronizedMap(new LRUMap(RANDOMS_CACHE_SIZE));

    public static synchronized RandomBooleans getRandomBooleans(int seed, int percent) {
        return RANDOM_BOOLS_CACHE
                .computeIfAbsent(RandomIntegers.hashCode(seed, percent), (p) -> new RandomBooleans(seed, percent));
    }

}
