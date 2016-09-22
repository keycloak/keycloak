/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import cx.ath.matthew.debug.Debug;

import java.lang.reflect.Array;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import static org.freedesktop.dbus.Gettext.getString;

class ArrayFrob {
    static Hashtable<Class<? extends Object>, Class<? extends Object>> primitiveToWrapper = new Hashtable<Class<? extends Object>, Class<? extends Object>>();
    static Hashtable<Class<? extends Object>, Class<? extends Object>> wrapperToPrimitive = new Hashtable<Class<? extends Object>, Class<? extends Object>>();

    static {
        primitiveToWrapper.put(Boolean.TYPE, Boolean.class);
        primitiveToWrapper.put(Byte.TYPE, Byte.class);
        primitiveToWrapper.put(Short.TYPE, Short.class);
        primitiveToWrapper.put(Character.TYPE, Character.class);
        primitiveToWrapper.put(Integer.TYPE, Integer.class);
        primitiveToWrapper.put(Long.TYPE, Long.class);
        primitiveToWrapper.put(Float.TYPE, Float.class);
        primitiveToWrapper.put(Double.TYPE, Double.class);
        wrapperToPrimitive.put(Boolean.class, Boolean.TYPE);
        wrapperToPrimitive.put(Byte.class, Byte.TYPE);
        wrapperToPrimitive.put(Short.class, Short.TYPE);
        wrapperToPrimitive.put(Character.class, Character.TYPE);
        wrapperToPrimitive.put(Integer.class, Integer.TYPE);
        wrapperToPrimitive.put(Long.class, Long.TYPE);
        wrapperToPrimitive.put(Float.class, Float.TYPE);
        wrapperToPrimitive.put(Double.class, Double.TYPE);

    }

    @SuppressWarnings("unchecked")
    public static <T> T[] wrap(Object o) throws IllegalArgumentException {
        Class<? extends Object> ac = o.getClass();
        if (!ac.isArray()) throw new IllegalArgumentException(getString("invalidArray"));
        Class<? extends Object> cc = ac.getComponentType();
        Class<? extends Object> ncc = primitiveToWrapper.get(cc);
        if (null == ncc) throw new IllegalArgumentException(getString("notPrimitiveType"));
        T[] ns = (T[]) Array.newInstance(ncc, Array.getLength(o));
        for (int i = 0; i < ns.length; i++)
            ns[i] = (T) Array.get(o, i);
        return ns;
    }

    @SuppressWarnings("unchecked")
    public static <T> Object unwrap(T[] ns) throws IllegalArgumentException {
        Class<? extends T[]> ac = (Class<? extends T[]>) ns.getClass();
        Class<T> cc = (Class<T>) ac.getComponentType();
        Class<? extends Object> ncc = wrapperToPrimitive.get(cc);
        if (null == ncc) throw new IllegalArgumentException(getString("invalidWrapperType"));
        Object o = Array.newInstance(ncc, ns.length);
        for (int i = 0; i < ns.length; i++)
            Array.set(o, i, ns[i]);
        return o;
    }

    public static <T> List<T> listify(T[] ns) throws IllegalArgumentException {
        return Arrays.asList(ns);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> listify(Object o) throws IllegalArgumentException {
        if (o instanceof Object[]) return listify((T[]) o);
        if (!o.getClass().isArray()) throw new IllegalArgumentException(getString("invalidArray"));
        List<T> l = new ArrayList<T>(Array.getLength(o));
        for (int i = 0; i < Array.getLength(o); i++)
            l.add((T) Array.get(o, i));
        return l;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] delist(List<T> l, Class<T> c) throws IllegalArgumentException {
        return l.toArray((T[]) Array.newInstance(c, 0));
    }

    public static <T> Object delistprimitive(List<T> l, Class<T> c) throws IllegalArgumentException {
        Object o = Array.newInstance(c, l.size());
        for (int i = 0; i < l.size(); i++)
            Array.set(o, i, l.get(i));
        return o;
    }

    @SuppressWarnings("unchecked")
    public static Object convert(Object o, Class<? extends Object> c) throws IllegalArgumentException {
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
            if (List.class.equals(c)
                    && o instanceof List)
                return o;

            // int[] -> List<Integer>
            // Integer[] -> List<Integer>
            if (List.class.equals(c)
                    && o.getClass().isArray())
                return listify(o);

            // int[] -> int[]
            // Integer[] -> Integer[]
            if (o.getClass().isArray()
                    && c.isArray()
                    && o.getClass().getComponentType().equals(c.getComponentType()))
                return o;

            // int[] -> Integer[]
            if (o.getClass().isArray()
                    && c.isArray()
                    && o.getClass().getComponentType().isPrimitive())
                return wrap(o);

            // Integer[] -> int[]
            if (o.getClass().isArray()
                    && c.isArray()
                    && c.getComponentType().isPrimitive())
                return unwrap((Object[]) o);

            // List<Integer> -> int[]
            if (o instanceof List
                    && c.isArray()
                    && c.getComponentType().isPrimitive())
                return delistprimitive((List<Object>) o, (Class<Object>) c.getComponentType());

            // List<Integer> -> Integer[]
            if (o instanceof List
                    && c.isArray())
                return delist((List<Object>) o, (Class<Object>) c.getComponentType());

            if (o.getClass().isArray()
                    && c.isArray())
                return type((Object[]) o, (Class<Object>) c.getComponentType());

        } catch (Exception e) {
            if (AbstractConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);
            throw new IllegalArgumentException(e);
        }

        throw new IllegalArgumentException(MessageFormat.format(getString("convertionTypeNotExpected"), new Object[]{o.getClass(), c}));
    }

    public static Object[] type(Object[] old, Class<Object> c) {
        Object[] ns = (Object[]) Array.newInstance(c, old.length);
        for (int i = 0; i < ns.length; i++)
            ns[i] = old[i];
        return ns;
    }
}
