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
import org.freedesktop.DBus.Error.NoReply;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static org.freedesktop.dbus.Gettext.getString;

/**
 * A handle to an asynchronous method call.
 */
public class DBusAsyncReply<ReturnType> {
    /**
     * Check if any of a set of asynchronous calls have had a reply.
     *
     * @param replies A Collection of handles to replies to check.
     * @return A Collection only containing those calls which have had replies.
     */
    public static Collection<DBusAsyncReply<? extends Object>> hasReply(Collection<DBusAsyncReply<? extends Object>> replies) {
        Collection<DBusAsyncReply<? extends Object>> c = new ArrayList<DBusAsyncReply<? extends Object>>(replies);
        Iterator<DBusAsyncReply<? extends Object>> i = c.iterator();
        while (i.hasNext())
            if (!i.next().hasReply()) i.remove();
        return c;
    }

    private ReturnType rval = null;
    private DBusExecutionException error = null;
    private MethodCall mc;
    private Method me;
    private AbstractConnection conn;

    DBusAsyncReply(MethodCall mc, Method me, AbstractConnection conn) {
        this.mc = mc;
        this.me = me;
        this.conn = conn;
    }

    @SuppressWarnings("unchecked")
    private synchronized void checkReply() {
        if (mc.hasReply()) {
            Message m = mc.getReply();
            if (m instanceof Error)
                error = ((Error) m).getException();
            else if (m instanceof MethodReturn) {
                try {
                    rval = (ReturnType) RemoteInvocationHandler.convertRV(m.getSig(), m.getParameters(), me, conn);
                } catch (DBusExecutionException DBEe) {
                    error = DBEe;
                } catch (DBusException DBe) {
                    if (AbstractConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, DBe);
                    error = new DBusExecutionException(DBe.getMessage());
                }
            }
        }
    }

    /**
     * Check if we've had a reply.
     *
     * @return True if we have a reply
     */
    public boolean hasReply() {
        if (null != rval || null != error) return true;
        checkReply();
        return null != rval || null != error;
    }

    /**
     * Get the reply.
     *
     * @return The return value from the method.
     * @throws DBusExecutionException if the reply to the method was an error.
     * @throws NoReply                if the method hasn't had a reply yet
     */
    public ReturnType getReply() throws DBusExecutionException {
        if (null != rval) return rval;
        else if (null != error) throw error;
        checkReply();
        if (null != rval) return rval;
        else if (null != error) throw error;
        else throw new NoReply(getString("asyncCallNoReply"));
    }

    public String toString() {
        return getString("waitingFor") + mc;
    }

    Method getMethod() {
        return me;
    }

    AbstractConnection getConnection() {
        return conn;
    }

    MethodCall getCall() {
        return mc;
    }
}

