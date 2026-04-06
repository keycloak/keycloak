package org.freedesktop.dbus;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Arrays;

import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.MethodNoReply;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.errors.NoReply;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.exceptions.NotConnected;
import org.freedesktop.dbus.interfaces.CallbackHandler;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.Error;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.messages.constants.Flags;
import org.freedesktop.dbus.propertyref.PropRefRemoteHandler;
import org.freedesktop.dbus.utils.DBusNamingUtil;
import org.freedesktop.dbus.utils.LoggingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteInvocationHandler implements InvocationHandler {
    public static final int CALL_TYPE_SYNC     = 0;
    public static final int CALL_TYPE_ASYNC    = 1;
    public static final int CALL_TYPE_CALLBACK = 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteInvocationHandler.class);

    // CHECKSTYLE:OFF
    AbstractConnection conn;
    RemoteObject       remote;
    // CHECKSTYLE:ON

    public RemoteInvocationHandler(AbstractConnection _conn, RemoteObject _remote) {
        this.remote = _remote;
        this.conn = _conn;
    }

    public RemoteObject getRemote() {
        return remote;
    }

    @Override
    public Object invoke(Object _proxy, Method _method, Object[] _args) throws Throwable {
        switch (_method.getName()) {
            case "isRemote":
                return true;
            case "getObjectPath":
                return remote.getObjectPath();
            case "clone":
                return null;
            case "equals":
                try {
                    if (1 == _args.length) {
                        return _args[0] != null && remote.equals(((RemoteInvocationHandler) Proxy.getInvocationHandler(_args[0])).remote);
                    }
                } catch (IllegalArgumentException _exIa) {
                    return Boolean.FALSE;
                }
            case "finalize":
                return null;
            case "getClass":
                return DBusInterface.class;
            case "hashCode":
                return remote.hashCode();
            case "notify":
                synchronized (remote) {
                    remote.notify();
                }
                return null;
            case "notifyAll":
                synchronized (remote) {
                    remote.notifyAll();
                }
                return null;
            case "wait":
                synchronized (remote) {
                    if (_args.length == 0) {
                        remote.wait();
                    } else if (_args.length == 1 && _args[0] instanceof Long l) {
                        remote.wait(l);
                    } else if (_args.length == 2 && _args[0] instanceof Long l && _args[1] instanceof Integer i) {
                        remote.wait(l, i);
                    }
                    if (_args.length <= 2) {
                        return null;
                    }
                }
            case "toString":
                return remote.toString();
            default:
                if (_method.isAnnotationPresent(DBusBoundProperty.class)) {
                    return PropRefRemoteHandler.handleDBusBoundProperty(conn, remote, _method, _args);
                }

                return executeRemoteMethod(remote, _method, conn, CALL_TYPE_SYNC, null, _args);
        }
    }

    public static Object convertRV(Object[] _rp, Method _m, AbstractConnection _conn) throws DBusException {
        return convertRV(_rp, new Type[] {_m.getGenericReturnType()}, _m, _conn);
    }

    public static Object convertRV(Object[] _rp, Type[] _types, Method _m, AbstractConnection _conn) throws DBusException {
        Class<? extends Object> c = _m.getReturnType();
        Object[] rp = _rp;
        if (rp == null) {
            if (null == c || Void.TYPE.equals(c)) {
                return null;
            } else {
                throw new DBusException("Wrong return type (got void, expected a value)");
            }
        } else {
            try {
                LoggingHelper.logIf(LOGGER.isTraceEnabled(), () -> LOGGER.trace("Converting return parameters from {} to type {}",
                        Arrays.deepToString(_rp), _m.getGenericReturnType()));

                rp = Marshalling.deSerializeParameters(rp, _types, _conn);
            } catch (Exception _ex) {
                LOGGER.debug("Wrong return type.", _ex);
                throw new DBusException(String.format("Wrong return type (failed to de-serialize correct types: %s )", _ex.getMessage()), _ex);
            }
        }

        switch (rp.length) {
            case 0:
                if (null == c || Void.TYPE.equals(c)) {
                    return null;
                } else {
                    throw new DBusException("Wrong return type (got void, expected a value)");
                }
            case 1:
                return rp[0];
            default:

                // check we are meant to return multiple values
                if (!Tuple.class.isAssignableFrom(c)) {
                    throw new DBusException("Wrong return type (not expecting Tuple)");
                }

                Constructor<? extends Object> cons = c.getConstructors()[0];
                try {
                    return cons.newInstance(rp);
                } catch (Exception _ex) {
                    LOGGER.debug("Error creating tuple instance using reflection", _ex);
                    throw new DBusException(_ex.getMessage());
                }
        }
    }

    public static Object executeRemoteMethod(final RemoteObject _ro, final Method _m,
                                             final AbstractConnection _conn, final int _syncmethod, final CallbackHandler<?> _callback, Object... _args) throws DBusException {
        return executeRemoteMethod(_ro, _m, new Type[] {_m.getGenericReturnType()}, _conn, _syncmethod, _callback, _args);
    }

    /**
     * Executes a remote method.
     *
     * @param _ro remote object
     * @param _m method to call
     * @param _customSignatures array of custom signatures which will be used for variants
     * @param _types array of types
     * @param _conn connection
     * @param _syncmethod true if the method is executed synchronously
     * @param _callback callback used when async method call
     * @param _args arguments to pass to method
     *
     * @return Object, maybe null
     *
     * @throws DBusException when call fails
     */
    public static Object executeRemoteMethod(final RemoteObject _ro, final Method _m, String[] _customSignatures,
        final Type[] _types, final AbstractConnection _conn, final int _syncmethod, final CallbackHandler<?> _callback, Object... _args) throws DBusException {

        Type[] ts = _m.getGenericParameterTypes();
        String sig = null;
        Object[] args = _args;
        if (ts.length > 0) {
            try {
                sig = Marshalling.getDBusType(ts);
                args = Marshalling.convertParameters(args, ts, _customSignatures, _conn);
            } catch (DBusException _ex) {
                throw new DBusExecutionException("Failed to construct D-Bus type: " + _ex.getMessage(), _ex);
            }
        }
        MethodCall call;
        byte flags = 0;
        if (!_ro.isAutostart()) {
            flags |= Flags.NO_AUTO_START;
        }
        if (_syncmethod == CALL_TYPE_ASYNC) {
            flags |= Flags.ASYNC;
        }
        if (_m.isAnnotationPresent(MethodNoReply.class)) {
            flags |= Flags.NO_REPLY_EXPECTED;
        }
        try {
            String name = DBusNamingUtil.getMethodName(_m);
            if (null == _ro.getInterface()) {
                call = _conn.getMessageFactory().createMethodCall(null, _ro.getBusName(), _ro.getObjectPath(), null, name, flags, sig, args);
            } else {
                String iface = DBusNamingUtil.getInterfaceName(_ro.getInterface());
                call = _conn.getMessageFactory().createMethodCall(null, _ro.getBusName(), _ro.getObjectPath(), iface, name, flags, sig, args);
            }
        } catch (DBusException _ex) {
            LOGGER.debug("Failed to construct outgoing method call.", _ex);
            throw new DBusExecutionException("Failed to construct outgoing method call: " + _ex.getMessage(), _ex);
        }
        if (!_conn.isConnected()) {
            throw new NotConnected("Not Connected");
        }

        switch (_syncmethod) {
            case CALL_TYPE_ASYNC -> {
                _conn.sendMessage(call);
                return new DBusAsyncReply<>(call, _m, _conn);
            }
            case CALL_TYPE_CALLBACK -> {
                _conn.queueCallback(call, _m, _callback);
                _conn.sendMessage(call);
                return null;
            }
             case CALL_TYPE_SYNC -> _conn.sendMessage(call);
             default -> throw new UnsupportedOperationException("Unsupported Method call type: " + _syncmethod);
        }

        // get reply
        if (_m.isAnnotationPresent(MethodNoReply.class)) {
            return null;
        }

        Message reply = call.getReply();
        if (null == reply) {
            throw new NoReply("No reply within specified time");
        }

        if (reply instanceof Error err) {
            err.throwException();
        }

        try {
            return convertRV(reply.getParameters(), _types, _m, _conn);
        } catch (DBusException _ex) {
            LOGGER.debug("", _ex);
            throw new DBusExecutionException(_ex.getMessage(), _ex);
        }
    }

    public static Object executeRemoteMethod(final RemoteObject _ro, final Method _m,
                                             final Type[] _types, final AbstractConnection _conn, final int _syncmethod, final CallbackHandler<?> _callback, Object... _args) throws DBusException {
        return executeRemoteMethod(_ro, _m, null, _types, _conn, _syncmethod, _callback, _args);
    }

}
