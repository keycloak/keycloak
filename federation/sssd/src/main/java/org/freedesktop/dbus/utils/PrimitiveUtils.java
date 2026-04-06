package org.freedesktop.dbus.utils;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class containing methods dealing with object and primitive class types.
 * @since 5.1.1 - 2024-09-15
 * @author hypfvieh
 */
public final class PrimitiveUtils {
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = Collections.unmodifiableMap(
        new ConcurrentHashMap<>(
            Map.of(
                Boolean.TYPE, Boolean.class,
                Byte.TYPE, Byte.class,
                Short.TYPE, Short.class,
                Character.TYPE, Character.class,
                Integer.TYPE, Integer.class,
                Long.TYPE, Long.class,
                Float.TYPE, Float.class,
                Double.TYPE, Double.class)
            )
        );

        private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE = Collections.unmodifiableMap(
            new ConcurrentHashMap<>(
                Map.of(
                    Boolean.class, Boolean.TYPE,
                    Byte.class, Byte.TYPE,
                    Short.class, Short.TYPE,
                    Character.class, Character.TYPE,
                    Integer.class, Integer.TYPE,
                    Long.class, Long.TYPE,
                    Float.class, Float.TYPE,
                    Double.class, Double.TYPE)
                )
            );

    private PrimitiveUtils() {

    }

    /**
     * Map with all primitives and the corresponding wrapper type.
     * @return unmodifiable map
     */
    public static Map<Class<?>, Class<?>> getPrimitiveToWrapperTypes() {
        return Collections.unmodifiableMap(PRIMITIVE_TO_WRAPPER);
    }

    /**
     * Map with all wrapper types and the corresponding primitive.
     * @return unmodifiable map
     */
    public static Map<Class<?>, Class<?>> getWrapperToPrimitiveTypes() {
        return Collections.unmodifiableMap(WRAPPER_TO_PRIMITIVE);
    }

    /**
     * Check if the given classes are equal or are equal wrapper types (e.g. byte == Byte).
     *
     * @param _clz1
     * @param _clz2
     * @return true if classes equal or both of compatible (wrapper) types,
     *  false otherwise or if any parameter is null
     */
    public static boolean isCompatiblePrimitiveOrWrapper(Class<?> _clz1, Class<?> _clz2) {
        if (_clz1 == null || _clz2 == null) {
            return false;
        }

        if (_clz1 == _clz2) {
            return true;
        } else if (_clz1.isPrimitive()) {
            Class<?> wrappedType = PRIMITIVE_TO_WRAPPER.get(_clz1);
            return wrappedType == _clz2;
        } else if (_clz2.isPrimitive()) {
            Class<?> wrappedType = PRIMITIVE_TO_WRAPPER.get(_clz2);
            return wrappedType == _clz1;
        }

        return false;
    }

}
