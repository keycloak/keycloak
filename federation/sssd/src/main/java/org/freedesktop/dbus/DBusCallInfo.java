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
 * Holds information on a method call
 */
public class DBusCallInfo {
    /**
     * Indicates the caller won't wait for a reply (and we won't send one).
     */
    public static final int NO_REPLY = Message.Flags.NO_REPLY_EXPECTED;
    public static final int ASYNC = 0x100;
    private String source;
    private String destination;
    private String objectpath;
    private String iface;
    private String method;
    private int flags;

    DBusCallInfo(Message m) {
        this.source = m.getSource();
        this.destination = m.getDestination();
        this.objectpath = m.getPath();
        this.iface = m.getInterface();
        this.method = m.getName();
        this.flags = m.getFlags();
    }

    /**
     * Returns the BusID which called the method
     */
    public String getSource() {
        return source;
    }

    /**
     * Returns the name with which we were addressed on the Bus
     */
    public String getDestination() {
        return destination;
    }

    /**
     * Returns the object path used to call this method
     */
    public String getObjectPath() {
        return objectpath;
    }

    /**
     * Returns the interface this method was called with
     */
    public String getInterface() {
        return iface;
    }

    /**
     * Returns the method name used to call this method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns any flags set on this method call
     */
    public int getFlags() {
        return flags;
    }
}
