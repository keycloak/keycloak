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
import java.util.List;

/**
 * The type of a list.
 * Should be used whenever you need a Type variable for a list.
 */
public class DBusListType implements ParameterizedType {
    private Type v;

    /**
     * Create a List type.
     *
     * @param v Type of the list contents.
     */
    public DBusListType(Type v) {
        this.v = v;
    }

    public Type[] getActualTypeArguments() {
        return new Type[]{v};
    }

    public Type getRawType() {
        return List.class;
    }

    public Type getOwnerType() {
        return null;
    }
}
