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

public class test_low_level {
    public static void main(String[] args) throws Exception {
        Debug.setHexDump(true);
        String addr = System.getenv("DBUS_SESSION_BUS_ADDRESS");
        Debug.print(addr);
        BusAddress address = new BusAddress(addr);
        Debug.print(address);

        Transport conn = new Transport(address);

        Message m = new MethodCall("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "Hello", (byte) 0, null);
        conn.mout.writeMessage(m);
        m = conn.min.readMessage();
        Debug.print(m.getClass());
        Debug.print(m);
        m = conn.min.readMessage();
        Debug.print(m.getClass());
        Debug.print(m);
        m = conn.min.readMessage();
        Debug.print("" + m);
        m = new MethodCall("org.freedesktop.DBus", "/", null, "Hello", (byte) 0, null);
        conn.mout.writeMessage(m);
        m = conn.min.readMessage();
        Debug.print(m);

        m = new MethodCall("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "RequestName", (byte) 0, "su", "org.testname", 0);
        conn.mout.writeMessage(m);
        m = conn.min.readMessage();
        Debug.print(m);
        m = new DBusSignal(null, "/foo", "org.foo", "Foo", null);
        conn.mout.writeMessage(m);
        m = conn.min.readMessage();
        Debug.print(m);
        conn.disconnect();
    }
}
