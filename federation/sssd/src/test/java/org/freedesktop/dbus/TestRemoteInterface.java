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
import org.freedesktop.DBus.Method;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * A sample remote interface which exports one method.
 */
public interface TestRemoteInterface extends DBusInterface {
    /**
     * A simple method with no parameters which returns a String
     */
    @Description("Simple test method")
    public String getName();

    public String getNameAndThrow();

    @Description("Test of nested maps")
    public <T> int frobnicate(List<Long> n, Map<String, Map<UInt16, Short>> m, T v);

    @Description("Throws a TestException when called")
    public void throwme() throws TestException;

    @Description("Waits then doesn't return")
    @Method.NoReply()
    public void waitawhile();

    @Description("Interface-overloaded method")
    public int overload();

    @Description("Testing Type Signatures")
    public void sig(Type[] s);

    @Description("Testing object paths as Path objects")
    public void newpathtest(Path p);

    @Description("Testing the float type")
    public float testfloat(float[] f);

    @Description("Testing structs of structs")
    public int[][] teststructstruct(TestStruct3 in);

    @Description("Regression test for #13291")
    public void reg13291(byte[] as, byte[] bs);

    public Map<String, Variant> svm();

    /* test lots of things involving Path */
    public Path pathrv(Path a);

    public List<Path> pathlistrv(List<Path> a);

    public Map<Path, Path> pathmaprv(Map<Path, Path> a);
}
