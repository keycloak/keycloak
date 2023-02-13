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
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.exceptions.MessageFormatException;
import org.freedesktop.dbus.exceptions.NotConnected;

import java.lang.reflect.Constructor;
import java.util.Vector;

import static org.freedesktop.dbus.Gettext.getString;

/**
 * Error messages which can be sent over the bus.
 */
public class Error extends Message {
    Error() {
    }

    public Error(String dest, String errorName, long replyserial, String sig, Object... args) throws DBusException {
        this(null, dest, errorName, replyserial, sig, args);
    }

    public Error(String source, String dest, String errorName, long replyserial, String sig, Object... args) throws DBusException {
        super(Message.Endian.BIG, Message.MessageType.ERROR, (byte) 0);

        if (null == errorName)
            throw new MessageFormatException(getString("missingErrorName"));
        headers.put(Message.HeaderField.REPLY_SERIAL, replyserial);
        headers.put(Message.HeaderField.ERROR_NAME, errorName);

        Vector<Object> hargs = new Vector<Object>();
        hargs.add(new Object[]{Message.HeaderField.ERROR_NAME, new Object[]{ArgumentType.STRING_STRING, errorName}});
        hargs.add(new Object[]{Message.HeaderField.REPLY_SERIAL, new Object[]{ArgumentType.UINT32_STRING, replyserial}});

        if (null != source) {
            headers.put(Message.HeaderField.SENDER, source);
            hargs.add(new Object[]{Message.HeaderField.SENDER, new Object[]{ArgumentType.STRING_STRING, source}});
        }

        if (null != dest) {
            headers.put(Message.HeaderField.DESTINATION, dest);
            hargs.add(new Object[]{Message.HeaderField.DESTINATION, new Object[]{ArgumentType.STRING_STRING, dest}});
        }

        if (null != sig) {
            hargs.add(new Object[]{Message.HeaderField.SIGNATURE, new Object[]{ArgumentType.SIGNATURE_STRING, sig}});
            headers.put(Message.HeaderField.SIGNATURE, sig);
            setArgs(args);
        }

        byte[] blen = new byte[4];
        appendBytes(blen);
        append("ua(yv)", serial, hargs.toArray());
        pad((byte) 8);

        long c = bytecounter;
        if (null != sig) append(sig, args);
        marshallint(bytecounter - c, blen, 0, 4);
    }

    public Error(String source, Message m, Throwable e) throws DBusException {
        this(source, m.getSource(), AbstractConnection.dollar_pattern.matcher(e.getClass().getName()).replaceAll("."), m.getSerial(), "s", e.getMessage());
    }

    public Error(Message m, Throwable e) throws DBusException {
        this(m.getSource(), AbstractConnection.dollar_pattern.matcher(e.getClass().getName()).replaceAll("."), m.getSerial(), "s", e.getMessage());
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends DBusExecutionException> createExceptionClass(String name) {
        if (name == "org.freedesktop.DBus.Local.disconnected") return NotConnected.class;
        Class<? extends DBusExecutionException> c = null;
        do {
            try {
                c = (Class<? extends org.freedesktop.dbus.exceptions.DBusExecutionException>) Class.forName(name);
            } catch (ClassNotFoundException CNFe) {
            }
            name = name.replaceAll("\\.([^\\.]*)$", "\\$$1");
        } while (null == c && name.matches(".*\\..*"));
        return c;
    }

    /**
     * Turns this into an exception of the correct type
     */
    public DBusExecutionException getException() {
        try {
            Class<? extends DBusExecutionException> c = createExceptionClass(getName());
            if (null == c || !DBusExecutionException.class.isAssignableFrom(c)) c = DBusExecutionException.class;
            Constructor<? extends DBusExecutionException> con = c.getConstructor(String.class);
            DBusExecutionException ex;
            Object[] args = getParameters();
            if (null == args || 0 == args.length)
                ex = con.newInstance("");
            else {
                String s = "";
                for (Object o : args)
                    s += o + " ";
                ex = con.newInstance(s.trim());
            }
            ex.setType(getName());
            return ex;
        } catch (Exception e) {
            if (AbstractConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);
            if (AbstractConnection.EXCEPTION_DEBUG && Debug.debug && null != e.getCause())
                Debug.print(Debug.ERR, e.getCause());
            DBusExecutionException ex;
            Object[] args = null;
            try {
                args = getParameters();
            } catch (Exception ee) {
            }
            if (null == args || 0 == args.length)
                ex = new DBusExecutionException("");
            else {
                String s = "";
                for (Object o : args)
                    s += o + " ";
                ex = new DBusExecutionException(s.trim());
            }
            ex.setType(getName());
            return ex;
        }
    }

    /**
     * Throw this as an exception of the correct type
     */
    public void throwException() throws DBusExecutionException {
        throw getException();
    }
}
