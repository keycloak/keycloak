/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.lang.ref.WeakReference;

/**
 * An alternative to a WeakReference when you don't want
 * that behaviour.
 */
public class StrongReference<T> extends WeakReference<T> {
    T referant;

    public StrongReference(T referant) {
        super(referant);
        this.referant = referant;
    }

    public void clear() {
        referant = null;
    }

    public boolean enqueue() {
        return false;
    }

    public T get() {
        return referant;
    }

    public boolean isEnqueued() {
        return false;
    }
}
