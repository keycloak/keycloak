package org.freedesktop.dbus.types;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * The type of a map.
 * Should be used whenever you need a Type variable for a map.
 */
public class DBusMapType implements ParameterizedType {
    private final Type k;
    private final Type v;

    /**
    * Create a map type.
    * @param _k The type of the keys.
    * @param _v The type of the values.
    */
    public DBusMapType(Type _k, Type _v) {
        this.k = _k;
        this.v = _v;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return new Type[] {
                k, v
        };
    }

    @Override
    public Type getRawType() {
        return Map.class;
    }

    @Override
    public Type getOwnerType() {
        return null;
    }
}
