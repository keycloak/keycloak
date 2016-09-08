/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import org.freedesktop.DBus.Introspectable;
import org.freedesktop.DBus.Peer;

import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

class ProfileHandler implements DBusSigHandler<Profiler.ProfileSignal> {
    public int c = 0;

    public void handle(Profiler.ProfileSignal s) {
        if (0 == (c++ % profile.SIGNAL_INNER)) System.out.print("-");
    }
}

/**
 * Profiling tests.
 */
public class profile {
    public static final int SIGNAL_INNER = 100;
    public static final int SIGNAL_OUTER = 100;
    public static final int PING_INNER = 100;
    public static final int PING_OUTER = 100;
    public static final int BYTES = 2000000;
    public static final int INTROSPECTION_OUTER = 100;
    public static final int INTROSPECTION_INNER = 10;
    public static final int STRUCT_OUTER = 100;
    public static final int STRUCT_INNER = 10;
    public static final int LIST_OUTER = 100;
    public static final int LIST_INNER = 10;
    public static final int LIST_LENGTH = 100;
    public static final int MAP_OUTER = 100;
    public static final int MAP_INNER = 10;
    public static final int MAP_LENGTH = 100;
    public static final int ARRAY_OUTER = 100;
    public static final int ARRAY_INNER = 10;
    public static final int ARRAY_LENGTH = 1000;
    public static final int STRING_ARRAY_OUTER = 10;
    public static final int STRING_ARRAY_INNER = 1;
    public static final int STRING_ARRAY_LENGTH = 20000;

    public static class Log {
        private long last;
        private int[] deltas;
        private int current = 0;

        public Log(int size) {
            deltas = new int[size];
        }

        public void start() {
            last = System.currentTimeMillis();
        }

        public void stop() {
            deltas[current] = (int) (System.currentTimeMillis() - last);
            current++;
        }

        public double mean() {
            if (0 == current) return 0;
            long sum = 0;
            for (int i = 0; i < current; i++)
                sum += deltas[i];
            return sum /= current;
        }

        public long min() {
            int m = Integer.MAX_VALUE;
            for (int i = 0; i < current; i++)
                if (deltas[i] < m) m = deltas[i];
            return m;
        }

        public long max() {
            int m = 0;
            for (int i = 0; i < current; i++)
                if (deltas[i] > m) m = deltas[i];
            return m;
        }

        public double stddev() {
            double mean = mean();
            double sum = 0;
            for (int i = 0; i < current; i++)
                sum += (deltas[i] - mean) * (deltas[i] - mean);
            return Math.sqrt(sum / (current - 1));
        }
    }

    public static void main(String[] args) {
        try {
            if (0 == args.length) {
                System.out.println("You must specify a profile type.");
                System.out.println("Syntax: profile <pings|arrays|introspect|maps|bytes|lists|structs|signals|rate|strings>");
                System.exit(1);
            }
            DBusConnection conn = DBusConnection.getConnection(DBusConnection.SESSION);
            conn.requestBusName("org.freedesktop.DBus.java.profiler");
            if ("pings".equals(args[0])) {
                int count = PING_INNER * PING_OUTER;
                System.out.print("Sending " + count + " pings...");
                Peer p = conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Peer.class);
                Log l = new Log(count);
                long t = System.currentTimeMillis();
                for (int i = 0; i < PING_OUTER; i++) {
                    for (int j = 0; j < PING_INNER; j++) {
                        l.start();
                        p.Ping();
                        l.stop();
                    }
                    System.out.print("");
                }
                t = System.currentTimeMillis() - t;
                System.out.println(" done.");
                System.out.println("min/max/avg (ms): " + l.min() + "/" + l.max() + "/" + l.mean());
                System.out.println("deviation: " + l.stddev());
                System.out.println("Total time: " + t + "ms");
            } else if ("strings".equals(args[0])) {
                int count = STRING_ARRAY_INNER * STRING_ARRAY_OUTER;
                System.out.print("Sending array of " + STRING_ARRAY_LENGTH + " strings " + count + " times.");
                ProfilerInstance pi = new ProfilerInstance();
                conn.exportObject("/Profiler", pi);
                Profiler p = conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Profiler.class);
                String[] v = new String[STRING_ARRAY_LENGTH];
                Random r = new Random();
                for (int i = 0; i < STRING_ARRAY_LENGTH; i++) v[i] = "" + r.nextInt();
                Log l = new Log(count);
                long t = System.currentTimeMillis();
                for (int i = 0; i < STRING_ARRAY_OUTER; i++) {
                    for (int j = 0; j < STRING_ARRAY_INNER; j++) {
                        l.start();
                        p.stringarray(v);
                        l.stop();
                    }
                    System.out.print("");
                }
                t = System.currentTimeMillis() - t;
                System.out.println(" done.");
                System.out.println("min/max/avg (ms): " + l.min() + "/" + l.max() + "/" + l.mean());
                System.out.println("deviation: " + l.stddev());
                System.out.println("Total time: " + t + "ms");
            } else if ("arrays".equals(args[0])) {
                int count = ARRAY_INNER * ARRAY_OUTER;
                System.out.print("Sending array of " + ARRAY_LENGTH + " ints " + count + " times.");
                ProfilerInstance pi = new ProfilerInstance();
                conn.exportObject("/Profiler", pi);
                Profiler p = conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Profiler.class);
                int[] v = new int[ARRAY_LENGTH];
                Random r = new Random();
                for (int i = 0; i < ARRAY_LENGTH; i++) v[i] = r.nextInt();
                Log l = new Log(count);
                long t = System.currentTimeMillis();
                for (int i = 0; i < ARRAY_OUTER; i++) {
                    for (int j = 0; j < ARRAY_INNER; j++) {
                        l.start();
                        p.array(v);
                        l.stop();
                    }
                    System.out.print("");
                }
                t = System.currentTimeMillis() - t;
                System.out.println(" done.");
                System.out.println("min/max/avg (ms): " + l.min() + "/" + l.max() + "/" + l.mean());
                System.out.println("deviation: " + l.stddev());
                System.out.println("Total time: " + t + "ms");
            } else if ("maps".equals(args[0])) {
                int count = MAP_INNER * MAP_OUTER;
                System.out.print("Sending map of " + MAP_LENGTH + " string=>strings " + count + " times.");
                ProfilerInstance pi = new ProfilerInstance();
                conn.exportObject("/Profiler", pi);
                Profiler p = conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Profiler.class);
                HashMap<String, String> m = new HashMap<String, String>();
                for (int i = 0; i < MAP_LENGTH; i++)
                    m.put("" + i, "hello");
                Log l = new Log(count);
                long t = System.currentTimeMillis();
                for (int i = 0; i < MAP_OUTER; i++) {
                    for (int j = 0; j < MAP_INNER; j++) {
                        l.start();
                        p.map(m);
                        l.stop();
                    }
                    System.out.print("");
                }
                t = System.currentTimeMillis() - t;
                System.out.println(" done.");
                System.out.println("min/max/avg (ms): " + l.min() + "/" + l.max() + "/" + l.mean());
                System.out.println("deviation: " + l.stddev());
                System.out.println("Total time: " + t + "ms");
            } else if ("lists".equals(args[0])) {
                int count = LIST_OUTER * LIST_INNER;
                System.out.print("Sending list of " + LIST_LENGTH + " strings " + count + " times.");
                ProfilerInstance pi = new ProfilerInstance();
                conn.exportObject("/Profiler", pi);
                Profiler p = conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Profiler.class);
                Vector<String> v = new Vector<String>();
                for (int i = 0; i < LIST_LENGTH; i++)
                    v.add("hello " + i);
                Log l = new Log(count);
                long t = System.currentTimeMillis();
                for (int i = 0; i < LIST_OUTER; i++) {
                    for (int j = 0; j < LIST_INNER; j++) {
                        l.start();
                        p.list(v);
                        l.stop();
                    }
                    System.out.print("");
                }
                t = System.currentTimeMillis() - t;
                System.out.println(" done.");
                System.out.println("min/max/avg (ms): " + l.min() + "/" + l.max() + "/" + l.mean());
                System.out.println("deviation: " + l.stddev());
                System.out.println("Total time: " + t + "ms");
            } else if ("structs".equals(args[0])) {
                int count = STRUCT_OUTER * STRUCT_INNER;
                System.out.print("Sending a struct " + count + " times.");
                ProfilerInstance pi = new ProfilerInstance();
                conn.exportObject("/Profiler", pi);
                Profiler p = conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Profiler.class);
                ProfileStruct ps = new ProfileStruct("hello", new UInt32(18), 500L);
                Log l = new Log(count);
                long t = System.currentTimeMillis();
                for (int i = 0; i < STRUCT_OUTER; i++) {
                    for (int j = 0; j < STRUCT_INNER; j++) {
                        l.start();
                        p.struct(ps);
                        l.stop();
                    }
                    System.out.print("");
                }
                t = System.currentTimeMillis() - t;
                System.out.println(" done.");
                System.out.println("min/max/avg (ms): " + l.min() + "/" + l.max() + "/" + l.mean());
                System.out.println("deviation: " + l.stddev());
                System.out.println("Total time: " + t + "ms");
            } else if ("introspect".equals(args[0])) {
                int count = INTROSPECTION_OUTER * INTROSPECTION_INNER;
                System.out.print("Recieving introspection data " + count + " times.");
                ProfilerInstance pi = new ProfilerInstance();
                conn.exportObject("/Profiler", pi);
                Introspectable is = conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Introspectable.class);
                Log l = new Log(count);
                long t = System.currentTimeMillis();
                String s = null;
                for (int i = 0; i < INTROSPECTION_OUTER; i++) {
                    for (int j = 0; j < INTROSPECTION_INNER; j++) {
                        l.start();
                        s = is.Introspect();
                        l.stop();
                    }
                    System.out.print("");
                }
                t = System.currentTimeMillis() - t;
                System.out.println(" done.");
                System.out.println("min/max/avg (ms): " + l.min() + "/" + l.max() + "/" + l.mean());
                System.out.println("deviation: " + l.stddev());
                System.out.println("Total time: " + t + "ms");
                System.out.println("Introspect data: " + s);
            } else if ("bytes".equals(args[0])) {
                System.out.print("Sending " + BYTES + " bytes");
                ProfilerInstance pi = new ProfilerInstance();
                conn.exportObject("/Profiler", pi);
                Profiler p = conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Profiler.class);
                byte[] bs = new byte[BYTES];
                for (int i = 0; i < BYTES; i++)
                    bs[i] = (byte) i;
                long t = System.currentTimeMillis();
                p.bytes(bs);
                System.out.println(" done in " + (System.currentTimeMillis() - t) + "ms.");
            } else if ("rate".equals(args[0])) {
                ProfilerInstance pi = new ProfilerInstance();
                conn.exportObject("/Profiler", pi);
                Profiler p = conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Profiler.class);
                Peer peer = conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Peer.class);
                conn.changeThreadCount((byte) 1);

                long start = System.currentTimeMillis();
                int count = 0;
                do {
                    p.Pong();
                    count++;
                } while (count < 10000);
                long end = System.currentTimeMillis();
                System.out.println("No payload: " + ((count * 1000) / (end - start)) + " RT/second");
                start = System.currentTimeMillis();
                count = 0;
                do {
                    p.Pong();
                    count++;
                } while (count < 10000);
                peer.Ping();
                end = System.currentTimeMillis();
                System.out.println("No payload, One way: " + ((count * 1000) / (end - start)) + " /second");
                int len = 256;
                while (len <= 32768) {
                    byte[] bs = new byte[len];
                    count = 0;
                    start = System.currentTimeMillis();
                    do {
                        p.bytes(bs);
                        count++;
                    } while (count < 1000);
                    end = System.currentTimeMillis();
                    long ms = end - start;
                    double cps = (count * 1000) / ms;
                    double rate = (len * cps) / (1024.0 * 1024.0);
                    System.out.println(len + " byte array) " + (count * len) + " bytes in " + ms + "ms (in " + count + " calls / " + (int) cps + " CPS): " + rate + "MB/s");
                    len <<= 1;
                }
                len = 256;
                while (len <= 32768) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < len; i++) sb.append('a');
                    String s = sb.toString();
                    end = System.currentTimeMillis() + 500;
                    count = 0;
                    do {
                        p.string(s);
                        count++;
                    } while (count < 1000);
                    long ms = end - start;
                    double cps = (count * 1000) / ms;
                    double rate = (len * cps) / (1024.0 * 1024.0);
                    System.out.println(len + " string) " + (count * len) + " bytes in " + ms + "ms (in " + count + " calls / " + (int) cps + " CPS): " + rate + "MB/s");
                    len <<= 1;
                }
            } else if ("signals".equals(args[0])) {
                int count = SIGNAL_OUTER * SIGNAL_INNER;
                System.out.print("Sending " + count + " signals");
                ProfileHandler ph = new ProfileHandler();
                conn.addSigHandler(Profiler.ProfileSignal.class, ph);
                Log l = new Log(count);
                Profiler.ProfileSignal ps = new Profiler.ProfileSignal("/");
                long t = System.currentTimeMillis();
                for (int i = 0; i < SIGNAL_OUTER; i++) {
                    for (int j = 0; j < SIGNAL_INNER; j++) {
                        l.start();
                        conn.sendSignal(ps);
                        l.stop();
                    }
                    System.out.print("");
                }
                t = System.currentTimeMillis() - t;
                System.out.println(" done.");
                System.out.println("min/max/avg (ms): " + l.min() + "/" + l.max() + "/" + l.mean());
                System.out.println("deviation: " + l.stddev());
                System.out.println("Total time: " + t + "ms");
                while (ph.c < count) try {
                    Thread.sleep(100);
                } catch (InterruptedException Ie) {
                }
                ;
            } else {
                conn.disconnect();
                System.out.println("Invalid profile ``" + args[0] + "''.");
                System.out.println("Syntax: profile <pings|arrays|introspect|maps|bytes|lists|structs|signals>");
                System.exit(1);
            }
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
