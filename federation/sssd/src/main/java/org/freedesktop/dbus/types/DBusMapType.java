/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.types;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * The type of a map.
 * Should be used whenever you need a Type variable for a map.
 */
public class DBusMapType implements ParameterizedType {
    private Type k;
    private Type v;

    /**
     * Create a map type.
     *
     * @param k The type of the keys.
     * @param v The type of the values.
     */
    public DBusMapType(Type k, Type v) {
        this.k = k;
        this.v = v;
    }

    public Type[] getActualTypeArguments() {
        return new Type[]{k, v};
    }

    public Type getRawType() {
        return Map.class;
    }

    public Type getOwnerType() {
        return null;
    }
}
