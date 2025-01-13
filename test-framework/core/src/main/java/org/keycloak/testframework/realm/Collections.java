package org.keycloak.testframework.realm;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
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

}
