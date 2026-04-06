package org.freedesktop.dbus;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.utils.PrimitiveUtils;
import org.slf4j.LoggerFactory;

public final class ArrayFrob {
    private ArrayFrob() {
    }

    /**
     * @deprecated use {@link PrimitiveUtils#getPrimitiveToWrapperTypes()}
     * @return Map with primitive type and wrapper types
     */
    @Deprecated(since = "5.1.1 - 2024-09-15")
    public static Map<Class<?>, Class<?>> getPrimitiveToWrapperTypes() {
        return PrimitiveUtils.getPrimitiveToWrapperTypes();
    }

    /**
     * @deprecated use {@link PrimitiveUtils#getWrapperToPrimitiveTypes()}
     * @return Map with wrapper type and primitive type
     */
    @Deprecated(since = "5.1.1 - 2024-09-15")
    public static Map<Class<?>, Class<?>> getWrapperToPrimitiveTypes() {
        return PrimitiveUtils.getWrapperToPrimitiveTypes();
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] wrap(Object _o) throws IllegalArgumentException {
        Class<? extends Object> ac = _o.getClass();
        if (!ac.isArray()) {
            throw new IllegalArgumentException("Not an array");
        }
        Class<? extends Object> cc = ac.getComponentType();
        Class<? extends Object> ncc = PrimitiveUtils.getPrimitiveToWrapperTypes().get(cc);
        if (null == ncc) {
            throw new IllegalArgumentException("Not a primitive type");
        }
        T[] ns = (T[]) Array.newInstance(ncc, Array.getLength(_o));
        for (int i = 0; i < ns.length; i++) {
            ns[i] = (T) Array.get(_o, i);
        }
        return ns;
    }

    @SuppressWarnings("unchecked")
    public static <T> Object unwrap(T[] _ns) throws IllegalArgumentException {
        Class<? extends T[]> ac = (Class<? extends T[]>) _ns.getClass();
        Class<T> cc = (Class<T>) ac.getComponentType();
        Class<? extends Object> ncc =  PrimitiveUtils.getWrapperToPrimitiveTypes().get(cc);
        if (null == ncc) {
            throw new IllegalArgumentException("Not a wrapper type");
        }
        Object o = Array.newInstance(ncc, _ns.length);
        for (int i = 0; i < _ns.length; i++) {
            Array.set(o, i, _ns[i]);
        }
        return o;
    }

    public static <T> List<T> listify(T[] _ns) throws IllegalArgumentException {
        return Arrays.asList(_ns);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> listify(Object _o) throws IllegalArgumentException {
        if (_o instanceof Object[]) {
            return listify((T[]) _o);
        }
        if (!_o.getClass().isArray()) {
            throw new IllegalArgumentException("Not an array");
        }
        List<T> l = new ArrayList<>(Array.getLength(_o));
        for (int i = 0; i < Array.getLength(_o); i++) {
            l.add((T) Array.get(_o, i));
        }
        return l;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] delist(List<T> _l, Class<T> _c) throws IllegalArgumentException {
        return _l.toArray((T[]) Array.newInstance(_c, 0));
    }

    public static <T> Object delistprimitive(List<T> _l, Class<T> _c) throws IllegalArgumentException {
        Object o = Array.newInstance(_c, _l.size());
        for (int i = 0; i < _l.size(); i++) {
            Array.set(o, i, _l.get(i));
        }
        return o;
    }

    @SuppressWarnings("unchecked")
    public static Object convert(Object _o, Class<? extends Object> _c) throws IllegalArgumentException {
        /* Possible Conversions:
         *
         ** List<Integer> -> List<Integer>
         ** List<Integer> -> int[]
         ** List<Integer> -> Integer[]
         ** int[] -> int[]
         ** int[] -> List<Integer>
         ** int[] -> Integer[]
         ** Integer[] -> Integer[]
         ** Integer[] -> int[]
         ** Integer[] -> List<Integer>
         */
        try {
            // List<Integer> -> List<Integer>
            if (List.class.equals(_c) && _o instanceof List) {
                return _o;
            }

            // int[] -> List<Integer>
            // Integer[] -> List<Integer>
            if (List.class.equals(_c) && _o.getClass().isArray()) {
                return listify(_o);
            }

            // int[] -> int[]
            // Integer[] -> Integer[]
            if (_o.getClass().isArray() && _c.isArray() && _o.getClass().getComponentType().equals(_c.getComponentType())) {
                return _o;
            }

            // int[] -> Integer[]
            if (_o.getClass().isArray() && _c.isArray() && _o.getClass().getComponentType().isPrimitive()) {
                return wrap(_o);
            }

            // Integer[] -> int[]
            if (_o.getClass().isArray() && _c.isArray() && _c.getComponentType().isPrimitive()) {
                return unwrap((Object[]) _o);
            }

            // List<Integer> -> int[]
            if (_o instanceof List && _c.isArray() && _c.getComponentType().isPrimitive()) {
                return delistprimitive((List<Object>) _o, (Class<Object>) _c.getComponentType());
            }

            // List<Integer> -> Integer[]
            if (_o instanceof List && _c.isArray()) {
                return delist((List<Object>) _o, (Class<Object>) _c.getComponentType());
            }

            if (_o.getClass().isArray() && _c.isArray()) {
                return type((Object[]) _o, (Class<Object>) _c.getComponentType());
            }

        } catch (Exception _ex) {
            LoggerFactory.getLogger(ArrayFrob.class).debug("Cannot convert object.", _ex);
            throw new IllegalArgumentException(_ex);
        }

        throw new IllegalArgumentException(String.format("Not An Expected Convertion type from %s to %s", _o.getClass(), _c));
    }

    public static Object[] type(Object[] _old, Class<Object> _c) {
        Object[] ns = (Object[]) Array.newInstance(_c, _old.length);
        System.arraycopy(_old, 0, ns, 0, ns.length);
        return ns;
    }

}
