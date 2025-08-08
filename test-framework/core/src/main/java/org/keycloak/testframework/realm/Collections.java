package org.keycloak.testframework.realm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Collections {

    private Collections() {
    }

    static <T> List<T> combine(List<T> l1, List<T> l2) {
        if (l1 == null) {
            return new LinkedList<>(l2);
        } else {
            l1.addAll(l2);
            return l1;
        }
    }

    @SafeVarargs
    static <T> List<T> combine(List<T> l1, T... items) {
        return combine(l1, Arrays.asList(items));
    }

    static <T> List<T> combine(List<T> l1, Stream<T> items) {
        return combine(l1, items.toList());
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

    static <T> Set<T> combine(Set<T> s1, Stream<T> items) {
        return combine(s1, items.collect(Collectors.toSet()));
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

    @SafeVarargs
    static <K, V> Map<K, List<V>> combine(Map<K, List<V>> m1, K key, V... values) {
        return combine(m1, Map.of(key, List.of(values)));
    }

    static <K, V> Map<K, List<V>> combine(Map<K, List<V>> m1, K key, Stream<V> values) {
        return combine(m1, Map.of(key, values.toList()));
    }

}
