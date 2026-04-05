package org.freedesktop.dbus;

import java.lang.reflect.Method;

import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.errors.Error;
import org.freedesktop.dbus.errors.NoReply;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.messages.MethodReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A handle to an asynchronous method call.
 */
public class DBusAsyncReply<T> {

    private final Logger             logger = LoggerFactory.getLogger(getClass());

    private T                        rval   = null;
    private DBusExecutionException   error  = null;
    private final MethodCall         mc;
    private final Method             me;
    private final AbstractConnection conn;

    public DBusAsyncReply(MethodCall _mc, Method _me, AbstractConnection _conn) {
        this.mc = _mc;
        this.me = _me;
        this.conn = _conn;
    }

    @SuppressWarnings("unchecked")
    private synchronized void checkReply() {
        if (mc.hasReply()) {
            Message m = mc.getReply();
            if (m instanceof Error) {
                error = ((Error) m).getException();
            } else if (m instanceof MethodReturn) {
                try {
                    Object obj = RemoteInvocationHandler.convertRV(m.getSig(), m.getParameters(), me, conn);

                    rval = (T) obj;
                } catch (DBusExecutionException _ex) {
                    error = _ex;
                } catch (DBusException _ex) {
                    logger.debug("", _ex);
                    error = new DBusExecutionException(_ex.getMessage());
                }
            }
        }
    }

    /**
    * Check if we've had a reply.
    * @return true if we have a reply
    */
    public boolean hasReply() {
        if (null != rval || null != error) {
            return true;
        }
        checkReply();
        return null != rval || null != error;
    }

    /**
    * Get the reply.
    * @return The return value from the method.
    * @throws DBusException if the reply to the method was an error.
    * @throws NoReply if the method hasn't had a reply yet
    */
    public T getReply() throws DBusException {
        if (null != rval) {
            return rval;
        } else if (null != error) {
            throw error;
        }
        checkReply();
        if (null != rval) {
            return rval;
        } else if (null != error) {
            throw error;
        } else {
            throw new NoReply("Async call has not had a reply");
        }
    }

    @Override
    public String toString() {
        return "Waiting for: " + mc;
    }

    public Method getMethod() {
        return me;
    }

    public AbstractConnection getConnection() {
        return conn;
    }

    public MethodCall getCall() {
        return mc;
    }

    /**
    * Check if any of a set of asynchronous calls have had a reply.
    * @param _replies A Collection of handles to replies to check.
    * @return A Collection only containing those calls which have had replies.
    */
//    public static Collection<DBusAsyncReply<?>> hasReply(Collection<DBusAsyncReply<?>> _replies) {
//        Collection<DBusAsyncReply<?>> c = new ArrayList<>(_replies);
//        Iterator<DBusAsyncReply<?>> i = c.iterator();
//        while (i.hasNext()) {
//            if (!i.next().hasReply()) {
//                i.remove();
//            }
//        }
//        return c;
//    }
}
