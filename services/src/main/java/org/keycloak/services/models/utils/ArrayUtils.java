package org.keycloak.services.models.utils;

import java.lang.reflect.Array;
import java.util.Arrays;

public class ArrayUtils {

    public static <T> T[] add(T[] src, T o) {
        T[] dst = Arrays.copyOf(src, src.length + 1);
        dst[src.length] = o;
        return dst;
    }

    public static <T> T[] remove(T[] src, T o) {
        int l = Arrays.binarySearch(src, o);
        if (l < 0) {
            return src;
        }

        T[] dst = newInstance(o, src.length - 1);
        System.arraycopy(src, 0, dst, 0, l);
        System.arraycopy(src, l + 1, dst, l, dst.length - l);
        return dst;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Object> T[] newInstance(T type, int length) {
        return (T[]) Array.newInstance(type.getClass(), length);
    }

}
