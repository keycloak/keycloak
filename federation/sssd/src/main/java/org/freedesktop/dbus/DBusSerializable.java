/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import org.freedesktop.dbus.exceptions.DBusException;

/**
 * Custom classes may be sent over DBus if they implement this interface.
 * <p>
 * In addition to the serialize method, classes <b>MUST</b> implement
 * a deserialize method which returns null and takes as it's arguments
 * all the DBus types the class will be serialied to <i>in order</i> and
 * <i>with type parameterisation</i>. They <b>MUST</b> also provide a
 * zero-argument constructor.
 * </p>
 * <p>
 * The serialize method should return the class properties you wish to
 * serialize, correctly formatted for the wire
 * (DBusConnection.convertParameters() can help with this), in order in an
 * Object array.
 * </p>
 * <p>
 * The deserialize method will be called once after the zero-argument
 * constructor. This should contain all the code to initialise the object
 * from the types.
 * </p>
 */
public interface DBusSerializable {
    public Object[] serialize() throws DBusException;
}
            
