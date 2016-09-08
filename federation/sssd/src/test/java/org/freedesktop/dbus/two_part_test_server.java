/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

public class two_part_test_server implements TwoPartInterface, DBusSigHandler<TwoPartInterface.TwoPartSignal> {
    public class two_part_test_object implements TwoPartObject {
        public boolean isRemote() {
            return false;
        }

        public String getName() {
            System.out.println("give name");
            return toString();
        }
    }

    private DBusConnection conn;

    public two_part_test_server(DBusConnection conn) {
        this.conn = conn;
    }

    public boolean isRemote() {
        return false;
    }

    public TwoPartObject getNew() {
        TwoPartObject o = new two_part_test_object();
        System.out.println("export new");
        try {
            conn.exportObject("/12345", o);
        } catch (Exception e) {
        }
        System.out.println("give new");
        return o;
    }

    public void handle(TwoPartInterface.TwoPartSignal s) {
        System.out.println("Got: " + s.o);
    }

    public static void main(String[] args) throws Exception {
        DBusConnection conn = DBusConnection.getConnection(DBusConnection.SESSION);
        conn.requestBusName("org.freedesktop.dbus.test.two_part_server");
        two_part_test_server server = new two_part_test_server(conn);
        conn.exportObject("/", server);
        conn.addSigHandler(TwoPartInterface.TwoPartSignal.class, server);
        while (true) try {
            Thread.sleep(10000);
        } catch (InterruptedException Ie) {
        }
    }
}

