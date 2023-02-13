/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is the super class of both Structs and Tuples
 * and holds common methods.
 */
abstract class Container {
    private static Map<Type, Type[]> typecache = new HashMap<Type, Type[]>();

    static void putTypeCache(Type k, Type[] v) {
        typecache.put(k, v);
    }

    static Type[] getTypeCache(Type k) {
        return typecache.get(k);
    }

    private Object[] parameters = null;

    public Container() {
    }

    private void setup() {
        Field[] fs = getClass().getDeclaredFields();
        Object[] args = new Object[fs.length];

        int diff = 0;
        for (Field f : fs) {
            Position p = f.getAnnotation(Position.class);
            if (null == p) {
                diff++;
                continue;
            }
            try {
                args[p.value()] = f.get(this);
            } catch (IllegalAccessException IAe) {
            }
        }

        this.parameters = new Object[args.length - diff];
        System.arraycopy(args, 0, parameters, 0, parameters.length);
    }

    /**
     * Returns the struct contents in order.
     *
     * @throws DBusException If there is  a problem doing this.
     */
    public final Object[] getParameters() {
        if (null != parameters) return parameters;
        setup();
        return parameters;
    }

    /**
     * Returns this struct as a string.
     */
    public final String toString() {
        String s = getClass().getName() + "<";
        if (null == parameters)
            setup();
        if (0 == parameters.length)
            return s + ">";
        for (Object o : parameters)
            s += o + ", ";
        return s.replaceAll(", $", ">");
    }

    public final boolean equals(Object other) {
        if (other instanceof Container) {
            Container that = (Container) other;
            if (this.getClass().equals(that.getClass()))
                return Arrays.equals(this.getParameters(), that.getParameters());
            else return false;
        } else return false;
    }
}
