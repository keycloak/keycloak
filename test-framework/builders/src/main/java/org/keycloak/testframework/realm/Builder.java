package org.keycloak.testframework.realm;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Builder<T> {

    T rep;

    public Builder(T rep) {
        this.rep = rep;
    }

    public T build() {
        return rep;
    }

    protected static <T> T createIfNull(T value, Supplier<T> supplier) {
        return value != null ? value : supplier.get();
    }

    static <T> List<T> combine(List<T> l1, List<T> l2) {
        if (l1 == null) {
            return new LinkedList<>(l2);
        } else {
            l1.addAll(l2);
            return l1;
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T[] combine(T[] l1, T[] l2) {
        if (l1 == null) {
            return l2;
        }
        T[] combined = (T[]) Array.newInstance(l1[0].getClass(), l1.length + l2.length);
        System.arraycopy(l1, 0, combined, 0, l1.length);
        System.arraycopy(l2, 0, combined, l1.length, l2.length);
        return combined;
    }

    @SafeVarargs
    static <T> List<T> combine(List<T> l1, T... items) {
        return combine(l1, Arrays.asList(items));
    }

    @SafeVarargs
    static <T> List<T> combine(List<T> l1, Builder<T>... items) {
        return combine(l1, Arrays.stream(items).map(Builder::build).toList());
    }

    @SafeVarargs
    static <T, P> List<T> combine(Function<P, Builder<T>> mapper, List<T> l1, P... l2) {
        return combine(l1, Arrays.stream(l2).map(mapper).map(Builder::build).toList());
    }

    @SafeVarargs
    static <V, P, K> Map<K, List<V>> combine(Function<P, Builder<V>> mapper, Map<K, List<V>> m1, K key, P... values) {
        return combine(m1, key, Arrays.stream(values).map(mapper).map(Builder::build).toList());
    }

    static <K, V> Map<K, List<V>> combine(Map<K, List<V>> m1, Map<K, List<V>> m2) {
        if (m1 == null) {
            m1 = new HashMap<>();
        }
        for (Map.Entry<K, List<V>> entry : m2.entrySet()) {
            K k = entry.getKey();
            List<V> v = entry.getValue();
            m1.put(k, combine(m1.get(k), v));
        }
        return m1;
    }

    static <K, V> Map<K, V> combineMap(Map<K, V> m1, Map<K, V> m2) {
        if (m1 == null) {
            return m2;
        }
        m1.putAll(m2);
        return m1;
    }

    static <T> Set<T> combine(Set<T> s1, Set<T> s2) {
        if (s1 == null) {
            return new HashSet<>(s2);
        } else {
            s1.addAll(s2);
            return s1;
        }
    }

    @SafeVarargs
    static <T> Set<T> combine(Set<T> s1, T... items) {
        return combine(s1, Set.of(items));
    }

    @SafeVarargs
    static <K, V> Map<K, List<V>> combine(Map<K, List<V>> m1, K key, V... values) {
        return combine(m1, Map.of(key, List.of(values)));
    }

    static <K, V> Map<K, List<V>> combine(Map<K, List<V>> m1, K key, List<V> values) {
        return combine(m1, Map.of(key, values));
    }

    @SafeVarargs
    static <K, V> Map<K, List<V>> combine(Map<K, List<V>> m1, K key, Builder<V>... values) {
        return combine(m1, Map.of(key, Arrays.stream(values).map(Builder::build).toList()));
    }

    @SafeVarargs
    static <K, V> Map<K, V> removeKeys(Map<K, V> map, K... keys) {
        if (map != null) {
            for (K key : keys) {
                map.remove(key);
            }
        }
        return map;
    }

    @SafeVarargs
    static <V> List<V> removeValues(List<V> list, V... values) {
        if (list != null) {
            list.removeAll(Arrays.stream(values).toList());
        }
        return list;
    }

}
