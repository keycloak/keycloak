/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.util.List;
import java.util.Map;

public class ProfilerInstance implements Profiler {
    public boolean isRemote() {
        return false;
    }

    public void array(int[] v) {
        return;
    }

    public void stringarray(String[] v) {
        return;
    }

    public void map(Map<String, String> m) {
        return;
    }

    public void list(List<String> l) {
        return;
    }

    public void bytes(byte[] b) {
        return;
    }

    public void struct(ProfileStruct ps) {
        return;
    }

    public void string(String s) {
        return;
    }

    public void NoReply() {
        return;
    }

    public void Pong() {
        return;
    }
}
