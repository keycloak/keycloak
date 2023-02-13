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
import org.freedesktop.dbus.exceptions.DBusException;

import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.Vector;

import static org.freedesktop.dbus.Gettext.getString;

/**
 * A Wrapper class for Variant values.
 * A method on DBus can send or receive a Variant.
 * This will wrap another value whose type is determined at runtime.
 * The Variant may be parameterized to restrict the types it may accept.
 */
public class Variant<T> {
    private final T o;
    private final Type type;
    private final String sig;

    /**
     * Create a Variant from a basic type object.
     *
     * @param o The wrapped value.
     * @throws IllegalArugmentException If you try and wrap Null or an object of a non-basic type.
     */
    public Variant(T o) throws IllegalArgumentException {
        if (null == o) throw new IllegalArgumentException(getString("cannotWrapNullInVariant"));
        type = o.getClass();
        try {
            String[] ss = Marshalling.getDBusType(o.getClass(), true);
            if (ss.length != 1)
                throw new IllegalArgumentException(getString("cannotWrapMultiValuedInVariant") + type);
            this.sig = ss[0];
        } catch (DBusException DBe) {
            if (AbstractConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, DBe);
            throw new IllegalArgumentException(MessageFormat.format(getString("cannotWrapUnqualifiedVariant"), new Object[]{o.getClass(), DBe.getMessage()}));
        }
        this.o = o;
    }

    /**
     * Create a Variant.
     *
     * @param o    The wrapped value.
     * @param type The explicit type of the value.
     * @throws IllegalArugmentException If you try and wrap Null or an object which cannot be sent over DBus.
     */
    public Variant(T o, Type type) throws IllegalArgumentException {
        if (null == o) throw new IllegalArgumentException(getString("cannotWrapNullInVariant"));
        this.type = type;
        try {
            String[] ss = Marshalling.getDBusType(type);
            if (ss.length != 1)
                throw new IllegalArgumentException(getString("cannotWrapMultiValuedInVariant") + type);
            this.sig = ss[0];
        } catch (DBusException DBe) {
            if (AbstractConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, DBe);
            throw new IllegalArgumentException(MessageFormat.format(getString("cannotWrapUnqualifiedVariant"), new Object[]{type, DBe.getMessage()}));
        }
        this.o = o;
    }

    /**
     * Create a Variant.
     *
     * @param o   The wrapped value.
     * @param sig The explicit type of the value, as a dbus type string.
     * @throws IllegalArugmentException If you try and wrap Null or an object which cannot be sent over DBus.
     */
    public Variant(T o, String sig) throws IllegalArgumentException {
        if (null == o) throw new IllegalArgumentException(getString("cannotWrapNullInVariant"));
        this.sig = sig;
        try {
            Vector<Type> ts = new Vector<Type>();
            Marshalling.getJavaType(sig, ts, 1);
            if (ts.size() != 1)
                throw new IllegalArgumentException(getString("cannotWrapNoTypesInVariant") + sig);
            this.type = ts.get(0);
        } catch (DBusException DBe) {
            if (AbstractConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, DBe);
            throw new IllegalArgumentException(MessageFormat.format(getString("cannotWrapUnqualifiedVariant"), new Object[]{sig, DBe.getMessage()}));
        }
        this.o = o;
    }

    /**
     * Return the wrapped value.
     */
    public T getValue() {
        return o;
    }

    /**
     * Return the type of the wrapped value.
     */
    public Type getType() {
        return type;
    }

    /**
     * Return the dbus signature of the wrapped value.
     */
    public String getSig() {
        return sig;
    }

    /**
     * Format the Variant as a string.
     */
    public String toString() {
        return "[" + o + "]";
    }

    /**
     * Compare this Variant with another by comparing contents
     */
    @SuppressWarnings("unchecked")
    public boolean equals(Object other) {
        if (null == other) return false;
        if (!(other instanceof Variant)) return false;
        return this.o.equals(((Variant<? extends Object>) other).o);
    }
}
