package org.freedesktop.dbus;

import java.lang.ref.WeakReference;

/**
 * An alternative to a WeakReference when you don't want
 * that behavior.
 */
public class StrongReference<T> extends WeakReference<T> {
    private T referant;

    public StrongReference(T _referant) {
        super(_referant);
        this.referant = _referant;
    }

    @Override
    public void clear() {
        referant = null;
    }

    @Override
    public boolean enqueue() {
        return false;
    }

    @Override
    public T get() {
        return referant;
    }

}
