package org.freedesktop.dbus.connections;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.freedesktop.dbus.DBusAsyncReply;
import org.freedesktop.dbus.interfaces.CallbackHandler;
import org.freedesktop.dbus.messages.MethodCall;

public class PendingCallbackManager {
    private final Map<MethodCall, CallbackHandler<? extends Object>> pendingCallbacks;
    private final Map<MethodCall, DBusAsyncReply<?>>                 pendingCallbackReplys;

    PendingCallbackManager() {
        pendingCallbacks = new ConcurrentHashMap<>();
        pendingCallbackReplys = new ConcurrentHashMap<>();
    }

    public synchronized void queueCallback(MethodCall _call, Method _method, CallbackHandler<?> _callback, AbstractConnection _connection) {
        pendingCallbacks.put(_call, _callback);
        pendingCallbackReplys.put(_call, new DBusAsyncReply<>(_call, _method, _connection));

    }

    public synchronized CallbackHandler<? extends Object> removeCallback(MethodCall _methodCall) {
        pendingCallbackReplys.remove(_methodCall);
        return pendingCallbacks.remove(_methodCall);
    }

    public synchronized CallbackHandler<? extends Object> getCallback(MethodCall _methodCall) {
        return pendingCallbacks.get(_methodCall);
    }

    public synchronized DBusAsyncReply<?> getCallbackReply(MethodCall _methodCall) {
        return pendingCallbackReplys.get(_methodCall);
    }

}
