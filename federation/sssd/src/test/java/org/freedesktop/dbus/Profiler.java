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

import java.util.List;
import java.util.Map;

public interface Profiler extends DBusInterface {
    public class ProfileSignal extends DBusSignal {
        public ProfileSignal(String path) throws DBusException {
            super(path);
        }
    }

    public void array(int[] v);

    public void stringarray(String[] v);

    public void map(Map<String, String> m);

    public void list(List<String> l);

    public void bytes(byte[] b);

    public void struct(ProfileStruct ps);

    public void string(String s);

    public void NoReply();

    public void Pong();
}


