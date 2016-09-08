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
import org.freedesktop.dbus.exceptions.DBusExecutionException;

import java.util.HashMap;

import static org.freedesktop.dbus.Gettext.getString;

public class DBusMatchRule {
    /* signal, error, method_call, method_reply */
    private String type;
    private String iface;
    private String member;
    private String object;
    private String source;
    private static HashMap<String, Class<? extends DBusSignal>> signalTypeMap =
            new HashMap<String, Class<? extends DBusSignal>>();

    static Class<? extends DBusSignal> getCachedSignalType(String type) {
        return signalTypeMap.get(type);
    }

    public DBusMatchRule(String type, String iface, String member) {
        this.type = type;
        this.iface = iface;
        this.member = member;
    }

    public DBusMatchRule(DBusExecutionException e) throws DBusException {
        this(e.getClass());
        member = null;
        type = "error";
    }

    public DBusMatchRule(Message m) {
        iface = m.getInterface();
        member = m.getName();
        if (m instanceof DBusSignal)
            type = "signal";
        else if (m instanceof Error) {
            type = "error";
            member = null;
        } else if (m instanceof MethodCall)
            type = "method_call";
        else if (m instanceof MethodReturn)
            type = "method_reply";
    }

    public DBusMatchRule(Class<? extends DBusInterface> c, String method) throws DBusException {
        this(c);
        member = method;
        type = "method_call";
    }

    public DBusMatchRule(Class<? extends Object> c, String source, String object) throws DBusException {
        this(c);
        this.source = source;
        this.object = object;
    }

    @SuppressWarnings("unchecked")
    public DBusMatchRule(Class<? extends Object> c) throws DBusException {
        if (DBusInterface.class.isAssignableFrom(c)) {
            if (null != c.getAnnotation(DBusInterfaceName.class))
                iface = c.getAnnotation(DBusInterfaceName.class).value();
            else
                iface = AbstractConnection.dollar_pattern.matcher(c.getName()).replaceAll(".");
            if (!iface.matches(".*\\..*"))
                throw new DBusException(getString("interfaceMustBeDefinedPackage"));
            member = null;
            type = null;
        } else if (DBusSignal.class.isAssignableFrom(c)) {
            if (null == c.getEnclosingClass())
                throw new DBusException(getString("signalsMustBeMemberOfClass"));
            else if (null != c.getEnclosingClass().getAnnotation(DBusInterfaceName.class))
                iface = c.getEnclosingClass().getAnnotation(DBusInterfaceName.class).value();
            else
                iface = AbstractConnection.dollar_pattern.matcher(c.getEnclosingClass().getName()).replaceAll(".");
            // Don't export things which are invalid D-Bus interfaces
            if (!iface.matches(".*\\..*"))
                throw new DBusException(getString("interfaceMustBeDefinedPackage"));
            if (c.isAnnotationPresent(DBusMemberName.class))
                member = c.getAnnotation(DBusMemberName.class).value();
            else
                member = c.getSimpleName();
            signalTypeMap.put(iface + '$' + member, (Class<? extends DBusSignal>) c);
            type = "signal";
        } else if (Error.class.isAssignableFrom(c)) {
            if (null != c.getAnnotation(DBusInterfaceName.class))
                iface = c.getAnnotation(DBusInterfaceName.class).value();
            else
                iface = AbstractConnection.dollar_pattern.matcher(c.getName()).replaceAll(".");
            if (!iface.matches(".*\\..*"))
                throw new DBusException(getString("interfaceMustBeDefinedPackage"));
            member = null;
            type = "error";
        } else if (DBusExecutionException.class.isAssignableFrom(c)) {
            if (null != c.getClass().getAnnotation(DBusInterfaceName.class))
                iface = c.getClass().getAnnotation(DBusInterfaceName.class).value();
            else
                iface = AbstractConnection.dollar_pattern.matcher(c.getClass().getName()).replaceAll(".");
            if (!iface.matches(".*\\..*"))
                throw new DBusException(getString("interfaceMustBeDefinedPackage"));
            member = null;
            type = "error";
        } else
            throw new DBusException(getString("invalidTypeMatchRule") + c);
    }

    public String toString() {
        String s = null;
        if (null != type) s = null == s ? "type='" + type + "'" : s + ",type='" + type + "'";
        if (null != member) s = null == s ? "member='" + member + "'" : s + ",member='" + member + "'";
        if (null != iface) s = null == s ? "interface='" + iface + "'" : s + ",interface='" + iface + "'";
        if (null != source) s = null == s ? "sender='" + source + "'" : s + ",sender='" + source + "'";
        if (null != object) s = null == s ? "path='" + object + "'" : s + ",path='" + object + "'";
        return s;
    }

    public String getType() {
        return type;
    }

    public String getInterface() {
        return iface;
    }

    public String getMember() {
        return member;
    }

    public String getSource() {
        return source;
    }

    public String getObject() {
        return object;
    }

}
