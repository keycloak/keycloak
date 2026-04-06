package org.freedesktop.dbus.connections.base;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;

import org.freedesktop.dbus.DBusCallInfo;
import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfig;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.connections.impl.ConnectionConfig;
import org.freedesktop.dbus.errors.UnknownMethod;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.messages.MethodReturn;
import org.freedesktop.dbus.messages.constants.Flags;
import org.freedesktop.dbus.utils.LoggingHelper;

/**
 * Abstract class containing most methods to invoke methods on a connection. <br>
 * Part of the {@link AbstractConnectionBase} &rarr;  {@link ConnectionMethodInvocation}
 * &rarr; {@link DBusBoundPropertyHandler} &rarr; {@link ConnectionMessageHandler} &rarr; {@link AbstractConnection} hierarchy.
 *
 * @author hypfvieh
 * @since 5.0.0 - 2023-10-23
 */
public abstract class ConnectionMethodInvocation extends AbstractConnectionBase {

    protected ConnectionMethodInvocation(ConnectionConfig _conCfg, TransportConfig _transportConfig, ReceivingServiceConfig _rsCfg) throws DBusException {
        super(_conCfg, _transportConfig, _rsCfg);
    }

    protected abstract void handleException(Message _methodOrSignal, DBusExecutionException _exception);

    protected void queueInvokeMethod(final MethodCall _methodCall, Method _meth, final Object _ob) {
        getLogger().trace("Adding Runnable for method {}", _meth);
        boolean noReply = 1 == (_methodCall.getFlags() & Flags.NO_REPLY_EXPECTED);
        getReceivingService().execMethodCallHandler(() -> setupAndInvoke(_methodCall, _meth, _ob, noReply));
    }

    protected Object setupAndInvoke(final MethodCall _methodCall, Method _meth, final Object _ob, final boolean _noReply) {
        getLogger().debug("Running method {} for remote call", _meth);
        try {
            Type[] ts = _meth.getGenericParameterTypes();
            Object[] params2 = _methodCall.getParameters();
            _methodCall.setArgs(Marshalling.deSerializeParameters(params2, ts, this));
            LoggingHelper.logIf(getLogger().isTraceEnabled(), () -> {
                try {
                    Object[] params3 = _methodCall.getParameters();
                    getLogger().trace("Deserialised {} to types {}", Arrays.deepToString(params3), Arrays.deepToString(ts));
                } catch (Exception _ex) {
                    getLogger().trace("Error getting method call parameters", _ex);
                }
            });
        } catch (Exception _ex) {
            getLogger().debug("", _ex);
            handleException(_methodCall, new UnknownMethod("Failure in de-serializing message: " + _ex));
            return null;
        }

        return invokeMethodAndReply(_methodCall, _meth, _ob, _noReply);
    }

    protected Object invokeMethodAndReply(final MethodCall _methodCall, final Method _me, final Object _ob, final boolean _noreply) {
        try {
            Object result = invokeMethod(_methodCall, _me, _ob);
            if (_me.getDeclaringClass() == Properties.class && _me.getName().equals("Get") && result == null) {
                rejectUnknownProperty(_methodCall, _methodCall.getParameters());
                return null;
            }

            if (!_noreply) {
                invokedMethodReply(_methodCall, _me, result);
            }
            return result;
        } catch (DBusExecutionException _ex) {
            getLogger().debug("Failed to invoke method call", _ex);
            handleException(_methodCall, _ex);
        } catch (Throwable _ex) {
            getLogger().debug("Error invoking method call {}", _methodCall, _ex);
            handleException(_methodCall,
                    new DBusExecutionException(String.format("Error Executing Method %s.%s: %s",
                            _methodCall.getInterface(), _methodCall.getName(), _ex.getMessage()), _ex));
        }
        return null;
    }

    protected void invokedMethodReply(final MethodCall _methodCall, final Method _me, Object _result)
        throws DBusException {
        MethodReturn reply;
        if (Void.TYPE.equals(_me.getReturnType())) {
            reply = getMessageFactory().createMethodReturn(_methodCall, null);
        } else {
            StringBuilder sb = new StringBuilder();
            for (String s : Marshalling.getDBusType(_me.getGenericReturnType())) {
                sb.append(s);
            }
            Object[] nr = Marshalling.convertParameters(new Object[] {
                    _result
            }, new Type[] {
                    _me.getGenericReturnType()
            }, this);

            reply = getMessageFactory().createMethodReturn(_methodCall, sb.toString(), nr);
        }
        sendMessage(reply);
    }

    protected Object invokeMethod(final MethodCall _methodCall, final Method _me, final Object _ob)
            throws Throwable {
        DBusCallInfo info = new DBusCallInfo(_methodCall);
        getInfoMap().put(Thread.currentThread(), info);
        try {
            LoggingHelper.logIf(getLogger().isTraceEnabled(), () -> {
                try {
                    Object[] params4 = _methodCall.getParameters();
                    getLogger().trace("Invoking Method: {} on {} with parameters {}", _me, _ob, Arrays.deepToString(params4));
                } catch (DBusException _ex) {
                    getLogger().trace("Error getting parameters from method call", _ex);
                }
            });

            Object[] params5 = _methodCall.getParameters();
            return _me.invoke(_ob, params5);
        } catch (InvocationTargetException _ex) {
            getLogger().debug("Unable to execute {}: {}", _methodCall, _ex.getMessage(), _ex);
            throw _ex.getCause();
        } finally {
            getInfoMap().remove(Thread.currentThread());
        }
    }

}
