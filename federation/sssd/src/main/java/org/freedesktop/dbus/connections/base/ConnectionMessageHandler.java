package org.freedesktop.dbus.connections.base;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;

import org.freedesktop.dbus.DBusAsyncReply;
import org.freedesktop.dbus.DBusCallInfo;
import org.freedesktop.dbus.MethodTuple;
import org.freedesktop.dbus.RemoteInvocationHandler;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfig;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.connections.impl.ConnectionConfig;
import org.freedesktop.dbus.errors.UnknownMethod;
import org.freedesktop.dbus.errors.UnknownObject;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.CallbackHandler;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.matchrules.DBusMatchRule;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.Error;
import org.freedesktop.dbus.messages.ExportedObject;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.messages.MethodReturn;

/**
 * Abstract class containing most methods to handle/react to a message received on a connection. <br>
 * Part of the {@link AbstractConnectionBase} &rarr;  {@link ConnectionMethodInvocation}
 * &rarr; {@link DBusBoundPropertyHandler} &rarr; {@link ConnectionMessageHandler} &rarr; {@link AbstractConnection} hierarchy.
 *
 * @author hypfvieh
 * @since 5.0.0 - 2023-10-23
 */
public abstract class ConnectionMessageHandler extends DBusBoundPropertyHandler {

    protected ConnectionMessageHandler(ConnectionConfig _conCfg, TransportConfig _transportConfig, ReceivingServiceConfig _rsCfg) throws DBusException {
        super(_conCfg, _transportConfig, _rsCfg);
    }

    @Override
    protected void handleException(Message _methodOrSignal, DBusExecutionException _exception) {
        try {
            sendMessage(getMessageFactory().createError(_methodOrSignal, _exception));
        } catch (DBusException _ex) {
            getLogger().warn("Exception caught while processing previous error.", _ex);
        }
    }

    /**
     * Handle a signal received on DBus.
     *
     * @param _signal signal to handle
     * @param _useThreadPool whether to handle this signal in another thread or handle it byself
     */
    @SuppressWarnings({
            "unchecked"
    })
    private void handleMessage(final DBusSignal _signal, boolean _useThreadPool) {
        getLogger().debug("Handling incoming signal: {}", _signal);

        List<DBusSigHandler<? extends DBusSignal>> handlers = new ArrayList<>();
        List<DBusSigHandler<DBusSignal>> genericHandlers = new ArrayList<>();

        for (Entry<DBusMatchRule, Queue<DBusSigHandler<? extends DBusSignal>>> e : getHandledSignals().entrySet()) {
            if (e.getKey().matches(_signal)) {
                handlers.addAll(e.getValue());
            }
        }

        for (Entry<DBusMatchRule, Queue<DBusSigHandler<DBusSignal>>> e : getGenericHandledSignals().entrySet()) {
            if (e.getKey().matches(_signal)) {
                genericHandlers.addAll(e.getValue());
            }
        }

        if (handlers.isEmpty() && genericHandlers.isEmpty()) {
            return;
        }

        final AbstractConnectionBase conn = this;
        for (final DBusSigHandler<? extends DBusSignal> h : handlers) {
            getLogger().trace("Adding Runnable for signal {} with handler {}",  _signal, h);
            Runnable command = () -> {
                try {
                    DBusSignal rs;
                    if (_signal.getClass().equals(DBusSignal.class)) {
                        rs = _signal.createReal(conn);
                    } else {
                        rs = _signal;
                    }
                    if (rs == null) {
                        if (getConnectionConfig().getUnknownSignalHandler() != null) {
                            getConnectionConfig().getUnknownSignalHandler().accept(_signal);
                        }
                        return;
                    }
                    ((DBusSigHandler<DBusSignal>) h).handle(rs);
                } catch (DBusException _ex) {
                    getLogger().warn("Exception while running signal handler '{}' for signal '{}':", h, _signal, _ex);
                    handleException(_signal, new DBusExecutionException("Error handling signal " + _signal.getInterface()
                            + "." + _signal.getName() + ": " + _ex.getMessage(), _ex));
                }
            };
            if (_useThreadPool) {
                getReceivingService().execSignalHandler(command);
            } else {
                command.run();
            }
        }

        for (final DBusSigHandler<DBusSignal> h : genericHandlers) {
            getLogger().trace("Adding Runnable for signal {} with handler {}",  _signal, h);
            Runnable command = () -> h.handle(_signal);
            if (_useThreadPool) {
                getReceivingService().execSignalHandler(command);
            } else {
                command.run();
            }
        }
    }

    protected void handleMessage(final Error _err) {
        getLogger().debug("Handling incoming error: {}", _err);
        MethodCall m = null;
        if (getPendingCalls() == null) {
            return;
        }
        synchronized (getPendingCalls()) {
            if (getPendingCalls().containsKey(_err.getReplySerial())) {
                m = getPendingCalls().remove(_err.getReplySerial());
            }
        }
        if (m != null) {
            m.setReply(_err);
            CallbackHandler<?> cbh;
            cbh = getCallbackManager().removeCallback(m);
            getLogger().trace("{} = pendingCallbacks.remove({})", cbh, m);

            // queue callback for execution
            if (null != cbh) {
                final CallbackHandler<?> fcbh = cbh;
                getLogger().trace("Adding Error Runnable with callback handler {}", fcbh);
                Runnable command = new Runnable() {

                    @Override
                    public synchronized void run() {
                        try {
                            getLogger().trace("Running Error Callback for {}", _err);
                            DBusCallInfo info = new DBusCallInfo(_err);
                            getInfoMap().put(Thread.currentThread(), info);

                            fcbh.handleError(_err.getException());
                            getInfoMap().remove(Thread.currentThread());

                        } catch (Exception _ex) {
                            getLogger().debug("Exception while running error callback.", _ex);
                        }
                    }
                };
                getReceivingService().execErrorHandler(command);
            }

        } else {
            getPendingErrorQueue().add(_err);
        }
    }

    @SuppressWarnings("unchecked")
    protected void handleMessage(final MethodReturn _mr) {
        getLogger().debug("Handling incoming method return: {}", _mr);
        MethodCall m = null;

        if (null == getPendingCalls()) {
            return;
        }

        synchronized (getPendingCalls()) {
            if (getPendingCalls().containsKey(_mr.getReplySerial())) {
                m = getPendingCalls().remove(_mr.getReplySerial());
            }
        }

        if (null != m) {
            m.setReply(_mr);
            _mr.setCall(m);
            @SuppressWarnings("rawtypes")
            CallbackHandler cbh = getCallbackManager().getCallback(m);
            DBusAsyncReply<?> asr = getCallbackManager().getCallbackReply(m);
            getCallbackManager().removeCallback(m);

            // queue callback for execution
            if (null != cbh) {
                final CallbackHandler<Object> fcbh = cbh;
                final DBusAsyncReply<?> fasr = asr;
                if (fasr == null) {
                    getLogger().debug("Cannot add runnable for method, given method callback was null");
                    return;
                }
                getLogger().trace("Adding Runnable for method {} with callback handler {}", fcbh, fasr.getMethod());
                Runnable r = new Runnable() {

                    @Override
                    public synchronized void run() {
                        try {
                            getLogger().trace("Running Callback for {}", _mr);
                            DBusCallInfo info = new DBusCallInfo(_mr);
                            getInfoMap().put(Thread.currentThread(), info);
                            Object convertRV = RemoteInvocationHandler.convertRV(_mr.getParameters(), fasr.getMethod(),
                                    fasr.getConnection());
                            fcbh.handle(convertRV);
                            getInfoMap().remove(Thread.currentThread());

                        } catch (Exception _ex) {
                            getLogger().debug("Exception while running callback.", _ex);
                        }
                    }
                };
                getReceivingService().execMethodReturnHandler(r);
            }

        } else {
            try {
                sendMessage(getMessageFactory().createError(_mr, new DBusExecutionException(
                        "Spurious reply. No message with the given serial id was awaiting a reply.")));
            } catch (DBusException _exDe) {
                getLogger().trace("Could not send error message", _exDe);
            }
        }
    }

    /**
     * Handle received message from DBus.
     * @param _message
     * @throws DBusException
     */
    void handleMessage(Message _message) throws DBusException {
        if (_message instanceof DBusSignal sig) {
            handleMessage(sig, true);
        } else if (_message instanceof MethodCall mc) {
            handleMessage(mc);
        } else if (_message instanceof MethodReturn mr) {
            handleMessage(mr);
        } else if (_message instanceof Error err) {
            handleMessage(err);
        }
    }

    private void handleMessage(final MethodCall _methodCall) throws DBusException {
        getLogger().debug("Handling incoming method call: {}", _methodCall);

        ExportedObject exportObject;
        Method meth = null;
        Object o = null;

        if (null == _methodCall.getInterface() || _methodCall.getInterface().equals("org.freedesktop.DBus.Peer")
                || _methodCall.getInterface().equals("org.freedesktop.DBus.Introspectable")) {
            exportObject = doWithExportedObjectsAndReturn(DBusException.class, eos -> eos.get(null));
            if (null != exportObject && null == exportObject.getObject().get()) {
                unExportObject(null);
                exportObject = null;
            }
            if (exportObject != null) {
                meth = exportObject.getMethods().get(new MethodTuple(_methodCall.getName(), _methodCall.getSig()));
            }
            if (meth != null) {
                o = new GlobalHandler(this, _methodCall.getPath());
            }
        }
        if (o == null) {
            // now check for specific exported functions

            exportObject = doWithExportedObjectsAndReturn(DBusException.class, eos -> eos.get(_methodCall.getPath()));
            getLogger().debug("Found exported object: {}", exportObject == null ? "<no object found>" : exportObject);

            if (exportObject != null && exportObject.getObject().get() == null) {
                getLogger().info("Unexporting {} implicitly (object present: {}, reference present: {})", _methodCall.getPath(), exportObject != null, exportObject.getObject().get() == null);
                unExportObject(_methodCall.getPath());
                exportObject = null;
            }

            if (exportObject == null) {
                exportObject = getFallbackContainer().get(_methodCall.getPath());
                getLogger().debug("Found {} in fallback container", exportObject == null ? "no" : exportObject);
            }

            if (exportObject == null) {
                getLogger().debug("No object found for method {}", _methodCall.getPath());
                sendMessage(getMessageFactory().createError(_methodCall,
                    new UnknownObject(_methodCall.getPath() + " is not an object provided by this process.")));
                return;
            }
            if (getLogger().isTraceEnabled()) {
                getLogger().trace("Searching for method {}  with signature {}", _methodCall.getName(), _methodCall.getSig());
                getLogger().trace("List of methods on {}: ", exportObject);
                for (MethodTuple mt : exportObject.getMethods().keySet()) {
                    getLogger().trace("   {} => {}", mt, exportObject.getMethods().get(mt));
                }
            }

            Object[] params = _methodCall.getParameters();
            switch (handleDBusBoundProperties(exportObject, _methodCall, params)) {
                case HANDLED:
                    return;
                case NO_PROPERTY:
                    rejectUnknownProperty(_methodCall, params);
                    return;
                case NOT_HANDLED:
                default:
                    break;
            }

            if (meth == null) {
                meth = exportObject.getMethods().get(new MethodTuple(_methodCall.getName(), _methodCall.getSig()));
                if (meth == null) {
                    sendMessage(getMessageFactory().createError(_methodCall, new UnknownMethod(String.format(
                        "The method `%s.%s' does not exist on this object.", _methodCall.getInterface(), _methodCall.getName()))));
                    return;
                }
            }
            o = exportObject.getObject().get();
        }

        if (ExportedObject.isExcluded(meth)) {
            sendMessage(getMessageFactory().createError(_methodCall, new UnknownMethod(String.format(
                    "The method `%s.%s' is not exported.", _methodCall.getInterface(), _methodCall.getName()))));
            return;
        }

        // now execute it
        queueInvokeMethod(_methodCall, meth, o);
    }

}
