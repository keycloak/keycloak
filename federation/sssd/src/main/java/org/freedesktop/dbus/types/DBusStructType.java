package org.freedesktop.dbus.types;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.freedesktop.dbus.Struct;

/**
 * The type of a struct.
 * Should be used whenever you need a Type variable for a struct.
 */
public class DBusStructType implements ParameterizedType {
    private final Type[] contents;

    /**
    * Create a struct type.
    * @param _contents The types contained in this struct.
    */
    public DBusStructType(Type... _contents) {
        this.contents = _contents;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return contents;
    }

    @Override
    public Type getRawType() {
        return Struct.class;
    }

    @Override
    public Type getOwnerType() {
        return null;
    }
}
