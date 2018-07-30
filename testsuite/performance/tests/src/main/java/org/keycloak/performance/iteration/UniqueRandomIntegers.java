package org.keycloak.performance.iteration;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author tkyjovsk
 */
public class UniqueRandomIntegers extends RandomIntegers {

    private static final Map<Integer, Map<Integer, UniqueRandomIntegers>> UNIQUE_RANDOM_INTS_CACHE = new HashMap<>();

    public UniqueRandomIntegers(int seed, int bound) {
        super(seed, bound);
    }

    @Override
    protected int nextInt() {
        int n = super.nextInt();
        return randoms.contains(n) ? nextInt() : n;
    }

    @Override
    public Integer get(int index) {
        if (index >= bound) {
            throw new IndexOutOfBoundsException(String.format(
                    "Sequence of unique random integers from interval [0,%s) only contains %s items. Requested index: %s, is out of bounds.",
                    bound, bound, index
            ));
        }
        return super.get(index);
    }

    public static synchronized UniqueRandomIntegers getUniqueRandomIntegers(int seed, int bound) {
        return UNIQUE_RANDOM_INTS_CACHE
                .computeIfAbsent(seed, (s) -> new HashMap<>())
                .computeIfAbsent(bound, (b) -> new UniqueRandomIntegers(seed, b));
    }
    
}
