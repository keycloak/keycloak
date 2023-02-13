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

class MethodTuple {
    String name;
    String sig;

    public MethodTuple(String name, String sig) {
        this.name = name;
        if (null != sig)
            this.sig = sig;
        else
            this.sig = "";
        if (Debug.debug) Debug.print(Debug.VERBOSE, "new MethodTuple(" + this.name + ", " + this.sig + ")");
    }

    public boolean equals(Object o) {
        return o.getClass().equals(MethodTuple.class)
                && ((MethodTuple) o).name.equals(this.name)
                && ((MethodTuple) o).sig.equals(this.sig);
    }

    public int hashCode() {
        return name.hashCode() + sig.hashCode();
    }
}
