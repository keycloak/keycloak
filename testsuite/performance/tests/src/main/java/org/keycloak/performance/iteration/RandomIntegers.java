package org.keycloak.performance.iteration;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.commons.collections.map.LRUMap;
import org.keycloak.performance.util.ValidateNumber;

/**
 *
 * @author tkyjovsk
 */
public class RandomIntegers extends AbstractList<Integer> {

    protected final List<Integer> randoms;
    protected final int seed;
    protected final int bound;

    private final Random random;

    public RandomIntegers(int seed, int bound) {
        this.randoms = new ArrayList<>();
        this.seed = seed;
        ValidateNumber.minValue(bound, 1);
        this.bound = bound;
        this.random = new Random(seed);
    }

    protected int nextInt() {
        return bound == 0 ? random.nextInt() : random.nextInt(bound);
    }

    private void generateRandomsUpTo(int index) {
        int mIndex = randoms.size() - 1;
        for (int i = mIndex; i < index; i++) {
            randoms.add(nextInt());
        }
    }

    @Override
    public Integer get(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException();
        }
        generateRandomsUpTo(index);
        return randoms.get(index);
    }

    @Override
    public int size() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int hashCode() {
        return hashCode(seed, bound);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RandomIntegers other = (RandomIntegers) obj;
        if (this.seed != other.seed) {
            return false;
        }
        return this.bound == other.bound;
    }

    public static int hashCode(int seed, int bound) {
        int hash = 5;
        hash = 41 * hash + seed;
        hash = 41 * hash + bound;
        return hash;
    }

    public static final int RANDOMS_CACHE_SIZE = Integer.parseInt(System.getProperty("randoms.cache.size", "10000"));

    private static final Map<Integer, RandomIntegers> RANDOM_INTS_CACHE
            = Collections.synchronizedMap(new LRUMap(RANDOMS_CACHE_SIZE));

    public static synchronized RandomIntegers getRandomIntegers(int seed, int bound) {
        return RANDOM_INTS_CACHE.computeIfAbsent(hashCode(seed, bound), r -> new RandomIntegers(seed, bound));
    }

}
