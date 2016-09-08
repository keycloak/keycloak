/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import org.freedesktop.DBus;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

public class cross_test_server implements DBus.Binding.Tests, DBus.Binding.SingleTests, DBusSigHandler<DBus.Binding.TestClient.Trigger> {
    private DBusConnection conn;
    boolean run = true;
    private Set<String> done = new TreeSet<String>();
    private Set<String> notdone = new TreeSet<String>();

    {
        notdone.add("org.freedesktop.DBus.Binding.Tests.Identity");
        notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityByte");
        notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityBool");
        notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityInt16");
        notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt16");
        notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityInt32");
        notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt32");
        notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityInt64");
        notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt64");
        notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityDouble");
        notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityString");
        notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityArray");
        notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityByteArray");
        notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityBoolArray");
        notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityInt16Array");
        notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt16Array");
        notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityInt32Array");
        notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt32Array");
        notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityInt64Array");
        notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt64Array");
        notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityDoubleArray");
        notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityStringArray");
        notdone.add("org.freedesktop.DBus.Binding.Tests.Sum");
        notdone.add("org.freedesktop.DBus.Binding.SingleTests.Sum");
        notdone.add("org.freedesktop.DBus.Binding.Tests.InvertMapping");
        notdone.add("org.freedesktop.DBus.Binding.Tests.DeStruct");
        notdone.add("org.freedesktop.DBus.Binding.Tests.Primitize");
        notdone.add("org.freedesktop.DBus.Binding.Tests.Invert");
        notdone.add("org.freedesktop.DBus.Binding.Tests.Trigger");
        notdone.add("org.freedesktop.DBus.Binding.Tests.Exit");
        notdone.add("org.freedesktop.DBus.Binding.TestClient.Trigger");
    }

    public cross_test_server(DBusConnection conn) {
        this.conn = conn;
    }

    public boolean isRemote() {
        return false;
    }

    @SuppressWarnings("unchecked")
    @DBus.Description("Returns whatever it is passed")
    public <T> Variant<T> Identity(Variant<T> input) {
        done.add("org.freedesktop.DBus.Binding.Tests.Identity");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.Identity");
        return new Variant(input.getValue());
    }

    @DBus.Description("Returns whatever it is passed")
    public byte IdentityByte(byte input) {
        done.add("org.freedesktop.DBus.Binding.Tests.IdentityByte");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityByte");
        return input;
    }

    @DBus.Description("Returns whatever it is passed")
    public boolean IdentityBool(boolean input) {
        done.add("org.freedesktop.DBus.Binding.Tests.IdentityBool");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityBool");
        return input;
    }

    @DBus.Description("Returns whatever it is passed")
    public short IdentityInt16(short input) {
        done.add("org.freedesktop.DBus.Binding.Tests.IdentityInt16");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityInt16");
        return input;
    }

    @DBus.Description("Returns whatever it is passed")
    public UInt16 IdentityUInt16(UInt16 input) {
        done.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt16");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityUInt16");
        return input;
    }

    @DBus.Description("Returns whatever it is passed")
    public int IdentityInt32(int input) {
        done.add("org.freedesktop.DBus.Binding.Tests.IdentityInt32");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityInt32");
        return input;
    }

    @DBus.Description("Returns whatever it is passed")
    public UInt32 IdentityUInt32(UInt32 input) {
        done.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt32");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityUInt32");
        return input;
    }

    @DBus.Description("Returns whatever it is passed")
    public long IdentityInt64(long input) {
        done.add("org.freedesktop.DBus.Binding.Tests.IdentityInt64");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityInt64");
        return input;
    }

    @DBus.Description("Returns whatever it is passed")
    public UInt64 IdentityUInt64(UInt64 input) {
        done.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt64");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityUInt64");
        return input;
    }

    @DBus.Description("Returns whatever it is passed")
    public double IdentityDouble(double input) {
        done.add("org.freedesktop.DBus.Binding.Tests.IdentityDouble");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityDouble");
        return input;
    }

    @DBus.Description("Returns whatever it is passed")
    public String IdentityString(String input) {
        done.add("org.freedesktop.DBus.Binding.Tests.IdentityString");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityString");
        return input;
    }

    @DBus.Description("Returns whatever it is passed")
    public <T> Variant<T>[] IdentityArray(Variant<T>[] input) {
        done.add("org.freedesktop.DBus.Binding.Tests.IdentityArray");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityArray");
        return input;
    }

    @DBus.Description("Returns whatever it is passed")
    public byte[] IdentityByteArray(byte[] input) {
        done.add("org.freedesktop.DBus.Binding.Tests.IdentityByteArray");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityByteArray");
        return input;
    }

    @DBus.Description("Returns whatever it is passed")
    public boolean[] IdentityBoolArray(boolean[] input) {
        done.add("org.freedesktop.DBus.Binding.Tests.IdentityBoolArray");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityBoolArray");
        return input;
    }

    @DBus.Description("Returns whatever it is passed")
    public short[] IdentityInt16Array(short[] input) {
        done.add("org.freedesktop.DBus.Binding.Tests.IdentityInt16Array");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityInt16Array");
        return input;
    }

    @DBus.Description("Returns whatever it is passed")
    public UInt16[] IdentityUInt16Array(UInt16[] input) {
        done.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt16Array");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityUInt16Array");
        return input;
    }

    @DBus.Description("Returns whatever it is passed")
    public int[] IdentityInt32Array(int[] input) {
        done.add("org.freedesktop.DBus.Binding.Tests.IdentityInt32Array");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityInt32Array");
        return input;
    }

    @DBus.Description("Returns whatever it is passed")
    public UInt32[] IdentityUInt32Array(UInt32[] input) {
        done.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt32Array");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityUInt32Array");
        return input;
    }

    @DBus.Description("Returns whatever it is passed")
    public long[] IdentityInt64Array(long[] input) {
        done.add("org.freedesktop.DBus.Binding.Tests.IdentityInt64Array");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityInt64Array");
        return input;
    }

    @DBus.Description("Returns whatever it is passed")
    public UInt64[] IdentityUInt64Array(UInt64[] input) {
        done.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt64Array");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityUInt64Array");
        return input;
    }

    @DBus.Description("Returns whatever it is passed")
    public double[] IdentityDoubleArray(double[] input) {
        done.add("org.freedesktop.DBus.Binding.Tests.IdentityDoubleArray");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityDoubleArray");
        return input;
    }

    @DBus.Description("Returns whatever it is passed")
    public String[] IdentityStringArray(String[] input) {
        done.add("org.freedesktop.DBus.Binding.Tests.IdentityStringArray");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityStringArray");
        return input;
    }

    @DBus.Description("Returns the sum of the values in the input list")
    public long Sum(int[] a) {
        done.add("org.freedesktop.DBus.Binding.Tests.Sum");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.Sum");
        long sum = 0;
        for (int b : a) sum += b;
        return sum;
    }

    @DBus.Description("Returns the sum of the values in the input list")
    public UInt32 Sum(byte[] a) {
        done.add("org.freedesktop.DBus.Binding.SingleTests.Sum");
        notdone.remove("org.freedesktop.DBus.Binding.SingleTests.Sum");
        int sum = 0;
        for (byte b : a) sum += (b < 0 ? b + 256 : b);
        return new UInt32(sum % (UInt32.MAX_VALUE + 1));
    }

    @DBus.Description("Given a map of A => B, should return a map of B => a list of all the As which mapped to B")
    public Map<String, List<String>> InvertMapping(Map<String, String> a) {
        done.add("org.freedesktop.DBus.Binding.Tests.InvertMapping");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.InvertMapping");
        HashMap<String, List<String>> m = new HashMap<String, List<String>>();
        for (String s : a.keySet()) {
            String b = a.get(s);
            List<String> l = m.get(b);
            if (null == l) {
                l = new Vector<String>();
                m.put(b, l);
            }
            l.add(s);
        }
        return m;
    }

    @DBus.Description("This method returns the contents of a struct as separate values")
    public DBus.Binding.Triplet<String, UInt32, Short> DeStruct(DBus.Binding.TestStruct a) {
        done.add("org.freedesktop.DBus.Binding.Tests.DeStruct");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.DeStruct");
        return new DBus.Binding.Triplet<String, UInt32, Short>(a.a, a.b, a.c);
    }

    @DBus.Description("Given any compound type as a variant, return all the primitive types recursively contained within as an array of variants")
    @SuppressWarnings("unchecked")
    public List<Variant<Object>> Primitize(Variant<Object> a) {
        done.add("org.freedesktop.DBus.Binding.Tests.Primitize");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.Primitize");
        return cross_test_client.PrimitizeRecurse(a.getValue(), a.getType());
    }

    @DBus.Description("inverts it's input")
    public boolean Invert(boolean a) {
        done.add("org.freedesktop.DBus.Binding.Tests.Invert");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.Invert");
        return !a;
    }

    @DBus.Description("triggers sending of a signal from the supplied object with the given parameter")
    public void Trigger(String a, UInt64 b) {
        done.add("org.freedesktop.DBus.Binding.Tests.Trigger");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.Trigger");
        try {
            conn.sendSignal(new DBus.Binding.TestSignals.Triggered(a, b));
        } catch (DBusException DBe) {
            throw new DBusExecutionException(DBe.getMessage());
        }
    }

    public void Exit() {
        done.add("org.freedesktop.DBus.Binding.Tests.Exit");
        notdone.remove("org.freedesktop.DBus.Binding.Tests.Exit");
        run = false;
        synchronized (this) {
            notifyAll();
        }
    }

    public void handle(DBus.Binding.TestClient.Trigger t) {
        done.add("org.freedesktop.DBus.Binding.TestClient.Trigger");
        notdone.remove("org.freedesktop.DBus.Binding.TestClient.Trigger");
        try {
            DBus.Binding.TestClient cb = conn.getRemoteObject(t.getSource(), "/Test", DBus.Binding.TestClient.class);
            cb.Response(t.a, t.b);
        } catch (DBusException DBe) {
            throw new DBusExecutionException(DBe.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            DBusConnection conn = DBusConnection.getConnection(DBusConnection.SESSION);
            conn.requestBusName("org.freedesktop.DBus.Binding.TestServer");
            cross_test_server cts = new cross_test_server(conn);
            conn.addSigHandler(DBus.Binding.TestClient.Trigger.class, cts);
            conn.exportObject("/Test", cts);
            synchronized (cts) {
                while (cts.run) {
                    try {
                        cts.wait();
                    } catch (InterruptedException Ie) {
                    }
                }
            }
            for (String s : cts.done)
                System.out.println(s + " ok");
            for (String s : cts.notdone)
                System.out.println(s + " untested");
            conn.disconnect();
            System.exit(0);
        } catch (DBusException DBe) {
            DBe.printStackTrace();
            System.exit(1);
        }
    }
}

