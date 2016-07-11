/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import org.freedesktop.DBus.Description;

import java.util.List;

@Description("An example remote interface")
@DBusInterfaceName("org.freedesktop.dbus.test.AlternateTestInterface")
public interface TestRemoteInterface2 extends DBusInterface {
    @Description("Test multiple return values and implicit variant parameters.")
    public <A> TestTuple<String, List<Integer>, Boolean> show(A in);

    @Description("Test passing structs and explicit variants, returning implicit variants")
    public <T> T dostuff(TestStruct foo);

    @Description("Test arrays, boxed arrays and lists.")
    public List<Integer> sampleArray(List<String> l, Integer[] is, long[] ls);

    @Description("Test passing objects as object paths.")
    public DBusInterface getThis(DBusInterface t);

    @Description("Test bools work")
    @DBusMemberName("checkbool")
    public boolean check();

    @Description("Test Serializable Object")
    public TestSerializable<String> testSerializable(byte b, TestSerializable<String> s, int i);

    @Description("Call another method on itself from within a call")
    public String recursionTest();

    @Description("Parameter-overloaded method (string)")
    public int overload(String s);

    @Description("Parameter-overloaded method (byte)")
    public int overload(byte b);

    @Description("Parameter-overloaded method (void)")
    public int overload();

    @Description("Nested List Check")
    public List<List<Integer>> checklist(List<List<Integer>> lli);

    @Description("Get new objects as object paths.")
    public TestNewInterface getNew();

    @Description("Test Complex Variants")
    public void complexv(Variant<? extends Object> v);

    @Description("Test Introspect on a different interface")
    public String Introspect();
}
