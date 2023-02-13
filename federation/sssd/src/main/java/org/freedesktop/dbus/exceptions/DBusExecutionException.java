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
 * An exception while running a remote method within DBus.
 */
@SuppressWarnings("serial")
public class DBusExecutionException extends RuntimeException {
    private String type;

    /**
     * Create an exception with the specified message
     */
    public DBusExecutionException(String message) {
        super(message);
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get the DBus type of this exception. Use if this
     * was an exception we don't have a class file for.
     */
    public String getType() {
        if (null == type) return getClass().getName();
        else return type;
    }
}
