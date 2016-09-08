/* Copyright (C) 1991-2015 Free Software Foundation, Inc.
   This file is part of the GNU C Library.

   The GNU C Library is free software; you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public
   License as published by the Free Software Foundation; either
   version 2.1 of the License, or (at your option) any later version.

   The GNU C Library is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   Lesser General Public License for more details.

   You should have received a copy of the GNU Lesser General Public
   License along with the GNU C Library; if not, see
   <http://www.gnu.org/licenses/>.  */
/* This header is separate from features.h so that the compiler can
   include it implicitly at the start of every compilation.  It must
   not itself include <features.h> or any other header that includes
   <features.h> because the implicit include comes before any feature
   test macros that may be defined in a source file before it first
   explicitly includes a system header.  GCC knows the name of this
   header in order to preinclude it.  */
/* glibc's intent is to support the IEC 559 math functionality, real
   and complex.  If the GCC (4.9 and later) predefined macros
   specifying compiler intent are available, use them to determine
   whether the overall intent is to support these features; otherwise,
   presume an older compiler has intent to support these features and
   define these macros by default.  */
/* wchar_t uses Unicode 7.0.0.  Version 7.0 of the Unicode Standard is
   synchronized with ISO/IEC 10646:2012, plus Amendments 1 (published
   on April, 2013) and 2 (not yet published as of February, 2015).
   Additionally, it includes the accelerated publication of U+20BD
   RUBLE SIGN.  Therefore Unicode 7.0.0 is between 10646:2012 and
   10646:2014, and so we use the date ISO/IEC 10646:2012 Amd.1 was
   published.  */
/* We do not support C11 <threads.h>.  */
/*
 * Java Debug Library
 *
 * Copyright (c) Matthew Johnson 2005
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * To Contact the author, please email src@matthew.ath.cx
 *
 */
package cx.ath.matthew.debug;

import cx.ath.matthew.utils.Hexdump;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Add debugging to your program, has support for large projects with multiple
 * classes and debug levels per class. Supports optional enabling of debug
 * per-level per-class and debug targets of files, Streams or stderr.
 * Also supports timing between debug outputs, printing of stack traces for Throwables
 * and files/line numbers on each message.
 * <p>
 * Debug now automatically figures out which class it was called from, so all
 * methods passing in the calling class are deprecated.
 * </p>
 * <p>
 * The defaults are to print all messages to stderr with class and method name.
 * </p>
 * <p>
 * Should be called like this:
 * </p>
 * <pre>
 * if (Debug.debug) Debug.print(Debug.INFO, "Debug Message");
 * </pre>
 */
public class Debug {
    /**
     * This interface can be used to provide custom printing filters
     * for certain classes.
     */
    public static interface FilterCommand {
        /**
         * Called to print debug messages with a custom filter.
         *
         * @param output   The PrintStream to output to.
         * @param level    The debug level of this message.
         * @param location The textual location of the message.
         * @param extra    Extra information such as timing details.
         * @param message  The debug message.
         * @param lines    Other lines of a multiple-line debug message.
         */
        public void filter(PrintStream output, int level, String location, String extra, String message, String[] lines);
    }

    /**
     * Highest priority messages
     */
    public static final int CRIT = 1;
    /**
     * Error messages
     */
    public static final int ERR = 2;
    /**
     * Warnings
     */
    public static final int WARN = 3;
    /**
     * Information
     */
    public static final int INFO = 4;
    /**
     * Debug messages
     */
    public static final int DEBUG = 5;
    /**
     * Verbose debug messages
     */
    public static final int VERBOSE = 6;
    /**
     * Set this to false to disable compilation of Debug statements
     */
    public static final boolean debug = false;
    /**
     * The current output stream (defaults to System.err)
     */
    public static PrintStream debugout = System.err;
    private static Properties prop = null;
    private static boolean timing = false;
    private static boolean ttrace = false;
    private static boolean lines = false;
    private static boolean hexdump = false;
    private static long last = 0;
    private static int balen = 36;
    private static int bawidth = 80;
    private static Class saveclass = null;
    //TODO: 1.5 private static Map<Class<? extends Object>, FilterCommand> filterMap = new HashMap<Class<? extends Object>, FilterCommand>();
    private static Map filterMap = new HashMap();

    /**
     * Set properties to configure debugging.
     * Format of properties is class =&gt; level, e.g.
     * <pre>
     * cx.ath.matthew.io.TeeOutputStream = INFO
     * cx.ath.matthew.io.DOMPrinter = DEBUG
     * </pre>
     * The debug level can be one of CRIT, ERR, WARN, INFO, DEBUG or VERBOSE which
     * correspond to all messages up to that level. The special words YES, ALL and TRUE
     * cause all messages to be printed regardless of level. All other terms disable
     * messages for that class. CRIT and ERR messages are always printed if debugging is enabled
     * unless explicitly disabled.
     * The special class name ALL can be used to set the default level for all classes.
     *
     * @param prop Properties object to use.
     */
    public static void setProperties(Properties prop) {
        Debug.prop = prop;
    }

    /**
     * Read which class to debug on at which level from the given File.
     * Syntax the same as Java Properties files:
     * <pre>
     * &lt;class&gt; = &lt;debuglevel&gt;
     * </pre>
     * E.G.
     * <pre>
     * cx.ath.matthew.io.TeeOutputStream = INFO
     * cx.ath.matthew.io.DOMPrinter = DEBUG
     * </pre>
     * The debug level can be one of CRIT, ERR, WARN, INFO, DEBUG or VERBOSE which
     * correspond to all messages up to that level. The special words YES, ALL and TRUE
     * cause all messages to be printed regardless of level. All other terms disable
     * messages for that class. CRIT and ERR messages are always printed if debugging is enabled
     * unless explicitly disabled.
     * The special class name ALL can be used to set the default level for all classes.
     *
     * @param f File to read from.
     */
    public static void loadConfig(File f) throws IOException {
        prop = new Properties();
        prop.load(new FileInputStream(f));
    }

    /**
     * @deprecated In Java 1.5 calling class is automatically identified, no need to pass it in.
     */
    //TODO: 1.5 @Deprecated()
    public static boolean debugging(Class c, int loglevel) {
        if (debug) {
            if (null == c) return true;
            return debugging(c.getName(), loglevel);
        }
        return false;
    }

    public static boolean debugging(String s, int loglevel) {
        if (debug) {
            try {
                if (null == s) return true;
                if (null == prop) return loglevel <= DEBUG;
                String d = prop.getProperty(s);
                if (null == d || "".equals(d)) d = prop.getProperty("ALL");
                if (null == d) return loglevel <= ERR;
                if ("".equals(d)) return loglevel <= ERR;
                d = d.toLowerCase();
                if ("true".equals(d)) return true;
                if ("yes".equals(d)) return true;
                if ("all".equals(d)) return true;
                if ("verbose".equals(d)) return loglevel <= VERBOSE;
                if ("debug".equals(d)) return loglevel <= DEBUG;
                if ("info".equals(d)) return loglevel <= INFO;
                if ("warn".equals(d)) return loglevel <= WARN;
                if ("err".equals(d)) return loglevel <= ERR;
                if ("crit".equals(d)) return loglevel <= CRIT;
                int i = Integer.parseInt(d);
                return i >= loglevel;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Output to the given Stream
     */
    public static void setOutput(PrintStream p) throws IOException {
        debugout = p;
    }

    /**
     * Output to the given file
     */
    public static void setOutput(String filename) throws IOException {
        debugout = new PrintStream(new FileOutputStream(filename, true));
    }

    /**
     * Output to the default debug.log
     */
    public static void setOutput() throws IOException {
        setOutput("./debug.log");
    }

    /**
     * Log at DEBUG
     *
     * @param d The object to log
     */
    public static void print(Object d) {
        if (debug) {
            if (d instanceof String)
                print(DEBUG, (String) d);
            else if (d instanceof Throwable)
                print(DEBUG, (Throwable) d);
            else if (d instanceof byte[])
                print(DEBUG, (byte[]) d);
            else if (d instanceof Map)
                printMap(DEBUG, (Map) d);
            else print(DEBUG, d);
        }
    }

    /**
     * Log at DEBUG
     *
     * @param o The object doing the logging
     * @param d The object to log
     * @deprecated In Java 1.5 calling class is automatically identified, no need to pass it in.
     */
    //TODO: 1.5 @Deprecated()
    public static void print(Object o, Object d) {
        if (debug) {
            if (o instanceof Class)
                saveclass = (Class) o;
            else
                saveclass = o.getClass();
            print(d);
        }
    }

    /**
     * Log an Object
     *
     * @param o        The object doing the logging
     * @param loglevel The level to log at (DEBUG, WARN, etc)
     * @param d        The object to log with d.toString()
     * @deprecated In Java 1.5 calling class is automatically identified, no need to pass it in.
     */
    //TODO: 1.5 @Deprecated()
    public static void print(Object o, int loglevel, Object d) {
        if (debug) {
            if (o instanceof Class)
                saveclass = (Class) o;
            else
                saveclass = o.getClass();
            print(loglevel, d);
        }
    }

    /**
     * Log a String
     *
     * @param o        The object doing the logging
     * @param loglevel The level to log at (DEBUG, WARN, etc)
     * @param s        The log message
     * @deprecated In Java 1.5 calling class is automatically identified, no need to pass it in.
     */
    //TODO: 1.5 @Deprecated()
    public static void print(Object o, int loglevel, String s) {
        if (debug) {
            if (o instanceof Class)
                saveclass = (Class) o;
            else
                saveclass = o.getClass();
            print(loglevel, s);
        }
    }

    /**
     * Log a Throwable
     *
     * @param o        The object doing the logging
     * @param loglevel The level to log at (DEBUG, WARN, etc)
     * @param t        The throwable to log with .toString and .printStackTrace
     * @deprecated In Java 1.5 calling class is automatically identified, no need to pass it in.
     */
    //TODO: 1.5 @Deprecated()
    public static void print(Object o, int loglevel, Throwable t) {
        if (debug) {
            if (o instanceof Class)
                saveclass = (Class) o;
            else
                saveclass = o.getClass();
            print(loglevel, t);
        }
    }

    /**
     * Log a Throwable
     *
     * @param c        The class doing the logging
     * @param loglevel The level to log at (DEBUG, WARN, etc)
     * @param t        The throwable to log with .toString and .printStackTrace
     * @deprecated In Java 1.5 calling class is automatically identified, no need to pass it in.
     */
    //TODO: 1.5 @Deprecated()
    public static void print(Class c, int loglevel, Throwable t) {
        if (debug) {
            saveclass = c;
            print(loglevel, t);
        }
    }

    /**
     * Log a Throwable
     *
     * @param loglevel The level to log at (DEBUG, WARN, etc)
     * @param t        The throwable to log with .toString and .printStackTrace
     * @see #setThrowableTraces to turn on stack traces.
     */
    public static void print(int loglevel, Throwable t) {
        if (debug) {
            String timestr = "";
            String[] data = getTraceElements();
            if (debugging(data[0], loglevel)) {
                if (timing) {
                    long now = System.currentTimeMillis();
                    timestr = "{" + (now - last) + "} ";
                    last = now;
                }
                String[] lines = null;
                if (ttrace) {
                    StackTraceElement[] ste = t.getStackTrace();
                    lines = new String[ste.length];
                    for (int i = 0; i < ste.length; i++)
                        lines[i] = "\tat " + ste[i].toString();
                }
                _print(t.getClass(), loglevel, data[0] + "." + data[1] + "()" + data[2], timestr, t.toString(), lines);
            }
        }
    }

    /**
     * Log a byte array
     *
     * @param loglevel The level to log at (DEBUG, WARN, etc)
     * @param b        The byte array to print.
     * @see #setHexDump to enable hex dumping.
     * @see #setByteArrayCount to change how many bytes are printed.
     * @see #setByteArrayWidth to change the formatting width of hex.
     */
    public static void print(int loglevel, byte[] b) {
        if (debug) {
            String timestr = "";
            String[] data = getTraceElements();
            if (debugging(data[0], loglevel)) {
                if (timing) {
                    long now = System.currentTimeMillis();
                    timestr = "{" + (now - last) + "} ";
                    last = now;
                }
                String[] lines = null;
                if (hexdump) {
                    if (balen >= b.length)
                        lines = Hexdump.format(b, bawidth).split("\n");
                    else {
                        byte[] buf = new byte[balen];
                        System.arraycopy(b, 0, buf, 0, balen);
                        lines = Hexdump.format(buf, bawidth).split("\n");
                    }
                }
                _print(b.getClass(), loglevel, data[0] + "." + data[1] + "()" + data[2], timestr, b.length + " bytes", lines);
            }
        }
    }

    /**
     * Log a String
     *
     * @param loglevel The level to log at (DEBUG, WARN, etc)
     * @param s        The string to log with d.toString()
     */
    public static void print(int loglevel, String s) {
        if (debug)
            print(loglevel, (Object) s);
    }

    /**
     * Log an Object
     *
     * @param c        The class doing the logging
     * @param loglevel The level to log at (DEBUG, WARN, etc)
     * @param d        The object to log with d.toString()
     * @deprecated In Java 1.5 calling class is automatically identified, no need to pass it in.
     */
    //TODO: 1.5 @Deprecated()
    public static void print(Class c, int loglevel, Object d) {
        if (debug) {
            saveclass = c;
            print(loglevel, d);
        }
    }

    /**
     * Log a String
     *
     * @param c        The class doing the logging
     * @param loglevel The level to log at (DEBUG, WARN, etc)
     * @param s        The log message
     * @deprecated In Java 1.5 calling class is automatically identified, no need to pass it in.
     */
    //TODO: 1.5 @Deprecated()
    public static void print(Class c, int loglevel, String s) {
        if (debug) {
            saveclass = c;
            print(loglevel, s);
        }
    }

    private static String[] getTraceElements() {
        String[] data = new String[]{"", "", ""};
        try {
            Method m = Thread.class.getMethod("getStackTrace", new Class[0]);
            StackTraceElement[] stes = (StackTraceElement[]) m.invoke(Thread.currentThread(), new Object[0]);
            for (StackTraceElement ste : stes) {
                if (Debug.class.getName().equals(ste.getClassName())) continue;
                if (Thread.class.getName().equals(ste.getClassName())) continue;
                if (Method.class.getName().equals(ste.getClassName())) continue;
                if (ste.getClassName().startsWith("sun.reflect")) continue;
                data[0] = ste.getClassName();
                data[1] = ste.getMethodName();
                if (lines)
                    data[2] = " " + ste.getFileName() + ":" + ste.getLineNumber();
                break;
            }
        } catch (NoSuchMethodException NSMe) {
            if (null != saveclass)
                data[0] = saveclass.getName();
        } catch (IllegalAccessException IAe) {
        } catch (InvocationTargetException ITe) {
        }
        return data;
    }

    /**
     * Log an Object
     *
     * @param loglevel The level to log at (DEBUG, WARN, etc)
     * @param o        The object to log
     */
    public static void print(int loglevel, Object o) {
        if (debug) {
            String timestr = "";
            String[] data = getTraceElements();
            if (debugging(data[0], loglevel)) {
                if (timing) {
                    long now = System.currentTimeMillis();
                    timestr = "{" + (now - last) + "} ";
                    last = now;
                }
                _print(o.getClass(), loglevel, data[0] + "." + data[1] + "()" + data[2], timestr, o.toString(), null);
            }
        }
    }

    /**
     * Log a Map
     *
     * @param o        The object doing the logging
     * @param loglevel The level to log at (DEBUG, WARN, etc)
     * @param m        The Map to print out
     * @deprecated In Java 1.5 calling class is automatically identified, no need to pass it in.
     */
    //TODO: 1.5 @Deprecated()
    public static void printMap(Object o, int loglevel, Map m) {
        if (debug) {
            if (o instanceof Class)
                saveclass = (Class) o;
            else
                saveclass = o.getClass();
            printMap(loglevel, m);
        }
    }

    /**
     * Log a Map
     *
     * @param c        The class doing the logging
     * @param loglevel The level to log at (DEBUG, WARN, etc)
     * @param m        The Map to print out
     * @deprecated In Java 1.5 calling class is automatically identified, no need to pass it in.
     */
    //TODO: 1.5 @Deprecated()
    public static void printMap(Class c, int loglevel, Map m) {
        if (debug) {
            saveclass = c;
            printMap(loglevel, m);
        }
    }

    /**
     * Log a Map at DEBUG log level
     *
     * @param m The Map to print out
     */
    public static void printMap(Map m) {
        printMap(DEBUG, m);
    }

    /**
     * Log a Map
     *
     * @param loglevel The level to log at (DEBUG, WARN, etc)
     * @param m        The Map to print out
     */
    public static void printMap(int loglevel, Map m) {
        if (debug) {
            String timestr = "";
            String[] data = getTraceElements();
            if (debugging(data[0], loglevel)) {
                if (timing) {
                    long now = System.currentTimeMillis();
                    timestr = "{" + (now - last) + "} ";
                    last = now;
                }
                Iterator i = m.keySet().iterator();
                String[] lines = new String[m.size()];
                int j = 0;
                while (i.hasNext()) {
                    Object key = i.next();
                    lines[j++] = "\t\t- " + key + " => " + m.get(key);
                }
                _print(m.getClass(), loglevel, data[0] + "." + data[1] + "()" + data[2], timestr, "Map:", lines);
            }
        }
    }

    /**
     * Enable or disable stack traces in Debuging throwables.
     */
    public static void setThrowableTraces(boolean ttrace) {
        Debug.ttrace = ttrace;
    }

    /**
     * Enable or disable timing in Debug messages.
     */
    public static void setTiming(boolean timing) {
        Debug.timing = timing;
    }

    /**
     * Enable or disable line numbers.
     */
    public static void setLineNos(boolean lines) {
        Debug.lines = lines;
    }

    /**
     * Enable or disable hexdumps.
     */
    public static void setHexDump(boolean hexdump) {
        Debug.hexdump = hexdump;
    }

    /**
     * Set the size of hexdumps.
     * (Default: 36)
     */
    public static void setByteArrayCount(int count) {
        Debug.balen = count;
    }

    /**
     * Set the formatted width of hexdumps.
     * (Default: 80 chars)
     */
    public static void setByteArrayWidth(int width) {
        Debug.bawidth = width;
    }

    /**
     * Add a filter command for a specific type.
     * This command will be called with the output stream
     * and the text to be sent. It should perform any
     * changes necessary to the text and then print the
     * result to the output stream.
     */
    public static void addFilterCommand(Class c, FilterCommand f)
    //TODO 1.5: public static void addFilterCommand(Class<? extends Object> c, FilterCommand f)
    {
        filterMap.put(c, f);
    }

    private static void _print(Class c, int level, String loc, String extra, String message, String[] lines) {
        //TODO 1.5: FilterCommand f = filterMap.get(c);
        FilterCommand f = (FilterCommand) filterMap.get(c);
        if (null == f) {
            debugout.println("[" + loc + "] " + extra + message);
            if (null != lines)
                for (String s : lines)
                    debugout.println(s);
        } else
            f.filter(debugout, level, loc, extra, message, lines);
    }
}
