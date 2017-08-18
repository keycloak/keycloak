/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

/**
 * Handle a signal on DBus.
 * All Signal handlers are run in their own Thread.
 * Application writers are responsible for managing any concurrency issues.
 */
public interface DBusSigHandler<T extends DBusSignal> {
    /**
     * Handle a signal.
     *
     * @param s The signal to handle. If such a class exists, the
     *          signal will be an instance of the class with the correct type signature.
     *          Otherwise it will be an instance of DBusSignal
     */
    public void handle(T s);
}
