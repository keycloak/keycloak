package org.freedesktop.dbus;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.LoggerFactory;

public final class ArrayFrob {
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE = new ConcurrentHashMap<>();
    static {
        PRIMITIVE_TO_WRAPPER.put(Boolean.TYPE, Boolean.class);
        PRIMITIVE_TO_WRAPPER.put(Byte.TYPE, Byte.class);
        PRIMITIVE_TO_WRAPPER.put(Short.TYPE, Short.class);
        PRIMITIVE_TO_WRAPPER.put(Character.TYPE, Character.class);
        PRIMITIVE_TO_WRAPPER.put(Integer.TYPE, Integer.class);
        PRIMITIVE_TO_WRAPPER.put(Long.TYPE, Long.class);
        PRIMITIVE_TO_WRAPPER.put(Float.TYPE, Float.class);
        PRIMITIVE_TO_WRAPPER.put(Double.TYPE, Double.class);
        WRAPPER_TO_PRIMITIVE.put(Boolean.class, Boolean.TYPE);
        WRAPPER_TO_PRIMITIVE.put(Byte.class, Byte.TYPE);
        WRAPPER_TO_PRIMITIVE.put(Short.class, Short.TYPE);
        WRAPPER_TO_PRIMITIVE.put(Character.class, Character.TYPE);
        WRAPPER_TO_PRIMITIVE.put(Integer.class, Integer.TYPE);
        WRAPPER_TO_PRIMITIVE.put(Long.class, Long.TYPE);
        WRAPPER_TO_PRIMITIVE.put(Float.class, Float.TYPE);
        WRAPPER_TO_PRIMITIVE.put(Double.class, Double.TYPE);
    }

    private ArrayFrob() {
    }

    public static Map<Class<?>, Class<?>> getPrimitiveToWrapperTypes() {
        return Collections.unmodifiableMap(PRIMITIVE_TO_WRAPPER);
    }

    public static Map<Class<?>, Class<?>> getWrapperToPrimitiveTypes() {
        return Collections.unmodifiableMap(WRAPPER_TO_PRIMITIVE);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] wrap(Object _o) throws IllegalArgumentException {
        Class<? extends Object> ac = _o.getClass();
        if (!ac.isArray()) {
            throw new IllegalArgumentException("Not an array");
        }
        Class<? extends Object> cc = ac.getComponentType();
        Class<? extends Object> ncc = PRIMITIVE_TO_WRAPPER.get(cc);
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
        Class<? extends Object> ncc = WRAPPER_TO_PRIMITIVE.get(cc);
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

        throw new IllegalArgumentException(String.format("Not An Expected Conversion type from %s to %s", _o.getClass(), _c));
    }

    public static Object[] type(Object[] _old, Class<Object> _c) {
        Object[] ns = (Object[]) Array.newInstance(_c, _old.length);
        System.arraycopy(_old, 0, ns, 0, ns.length);
        return ns;
    }
}
