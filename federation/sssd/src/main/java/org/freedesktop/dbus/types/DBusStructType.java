/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.types;

import org.freedesktop.dbus.Struct;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * The type of a struct.
 * Should be used whenever you need a Type variable for a struct.
 */
public class DBusStructType implements ParameterizedType {
    private Type[] contents;

    /**
     * Create a struct type.
     *
     * @param contents The types contained in this struct.
     */
    public DBusStructType(Type... contents) {
        this.contents = contents;
    }

    public Type[] getActualTypeArguments() {
        return contents;
    }

    public Type getRawType() {
        return Struct.class;
    }

    public Type getOwnerType() {
        return null;
    }
}
