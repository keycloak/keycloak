package org.freedesktop.dbus.types;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * The type of a list.
 * Should be used whenever you need a Type variable for a list.
 */
public class DBusListType implements ParameterizedType {
    private final Type v;

    /**
    * Create a List type.
    * @param _v Type of the list contents.
    */
    public DBusListType(Type _v) {
        this.v = _v;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return new Type[] {
                v
        };
    }

    @Override
    public Type getRawType() {
        return List.class;
    }

    @Override
    public Type getOwnerType() {
        return null;
    }
}
