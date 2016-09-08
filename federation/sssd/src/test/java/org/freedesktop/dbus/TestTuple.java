/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

public final class TestTuple<A, B, C> extends Tuple {
    @Position(0)
    public final A a;
    @Position(1)
    public final B b;
    @Position(2)
    public final C c;

    public TestTuple(A a, B b, C c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }
}
