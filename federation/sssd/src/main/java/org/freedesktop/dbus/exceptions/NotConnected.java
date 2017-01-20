/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.exceptions;

/**
 * Thrown if a DBus action is called when not connected to the Bus.
 */
@SuppressWarnings("serial")
public class NotConnected extends DBusExecutionException implements FatalException {
    public NotConnected(String message) {
        super(message);
    }
}
