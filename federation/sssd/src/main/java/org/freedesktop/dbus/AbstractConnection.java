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
import org.freedesktop.DBus;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.exceptions.FatalDBusException;
import org.freedesktop.dbus.exceptions.FatalException;
import org.freedesktop.dbus.exceptions.NotConnected;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;

import static org.freedesktop.dbus.Gettext.getString;


/**
 * Handles a connection to DBus.
 */
public abstract class AbstractConnection {
    protected class FallbackContainer {
        private Map<String[], ExportedObject> fallbacks = new HashMap<String[], ExportedObject>();

        public synchronized void add(String path, ExportedObject eo) {
            if (Debug.debug) Debug.print(Debug.DEBUG, "Adding fallback on " + path + " of " + eo);
            fallbacks.put(path.split("/"), eo);
        }

        public synchronized void remove(String path) {
            if (Debug.debug) Debug.print(Debug.DEBUG, "Removing fallback on " + path);
            fallbacks.remove(path.split("/"));
        }

        public synchronized ExportedObject get(String path) {
            int best = 0;
            int i = 0;
            ExportedObject bestobject = null;
            String[] pathel = path.split("/");
            for (String[] fbpath : fallbacks.keySet()) {
                if (Debug.debug)
                    Debug.print(Debug.VERBOSE, "Trying fallback path " + Arrays.deepToString(fbpath) + " to match " + Arrays.deepToString(pathel));
                for (i = 0; i < pathel.length && i < fbpath.length; i++)
                    if (!pathel[i].equals(fbpath[i])) break;
                if (i > 0 && i == fbpath.length && i > best)
                    bestobject = fallbacks.get(fbpath);
                if (Debug.debug) Debug.print(Debug.VERBOSE, "Matches " + i + " bestobject now " + bestobject);
            }
            if (Debug.debug) Debug.print(Debug.DEBUG, "Found fallback for " + path + " of " + bestobject);
            return bestobject;
        }
    }

    protected class _thread extends Thread {
        public _thread() {
            setName("DBusConnection");
        }

        public void run() {
            try {
                Message m = null;
                while (_run) {
                    m = null;

                    // read from the wire
                    try {
                        // this blocks on outgoing being non-empty or a message being available.
                        m = readIncoming();
                        if (m != null) {
                            if (Debug.debug) Debug.print(Debug.VERBOSE, "Got Incoming Message: " + m);
                            synchronized (this) {
                                notifyAll();
                            }

                            if (m instanceof DBusSignal)
                                handleMessage((DBusSignal) m);
                            else if (m instanceof MethodCall)
                                handleMessage((MethodCall) m);
                            else if (m instanceof MethodReturn)
                                handleMessage((MethodReturn) m);
                            else if (m instanceof Error)
                                handleMessage((Error) m);

                            m = null;
                        }
                    } catch (Exception e) {
                        if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);
                        if (e instanceof FatalException) {
                            disconnect();
                        }
                    }

                }
                synchronized (this) {
                    notifyAll();
                }
            } catch (Exception e) {
                if (Debug.debug && EXCEPTION_DEBUG) Debug.print(Debug.ERR, e);
            }
        }
    }

    private class _globalhandler implements org.freedesktop.DBus.Peer, org.freedesktop.DBus.Introspectable {
        private String objectpath;

        public _globalhandler() {
            this.objectpath = null;
        }

        public _globalhandler(String objectpath) {
            this.objectpath = objectpath;
        }

        public boolean isRemote() {
            return false;
        }

        public void Ping() {
            return;
        }

        public String Introspect() {
            String intro = objectTree.Introspect(objectpath);
            if (null == intro) {
                ExportedObject eo = fallbackcontainer.get(objectpath);
                if (null != eo) intro = eo.introspectiondata;
            }
            if (null == intro)
                throw new DBus.Error.UnknownObject("Introspecting on non-existant object");
            else return
                    "<!DOCTYPE node PUBLIC \"-//freedesktop//DTD D-BUS Object Introspection 1.0//EN\" " +
                            "\"http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd\">\n" + intro;
        }
    }

    protected class _workerthread extends Thread {
        private boolean _run = true;

        public void halt() {
            _run = false;
        }

        public void run() {
            while (_run) {
                Runnable r = null;
                synchronized (runnables) {
                    while (runnables.size() == 0 && _run)
                        try {
                            runnables.wait();
                        } catch (InterruptedException Ie) {
                        }
                    if (runnables.size() > 0)
                        r = runnables.removeFirst();
                }
                if (null != r) r.run();
            }
        }
    }

    private class _sender extends Thread {
        public _sender() {
            setName("Sender");
        }

        public void run() {
            Message m = null;

            if (Debug.debug) Debug.print(Debug.INFO, "Monitoring outbound queue");
            // block on the outbound queue and send from it
            while (_run) {
                if (null != outgoing) synchronized (outgoing) {
                    if (Debug.debug) Debug.print(Debug.VERBOSE, "Blocking");
                    while (outgoing.size() == 0 && _run)
                        try {
                            outgoing.wait();
                        } catch (InterruptedException Ie) {
                        }
                    if (Debug.debug) Debug.print(Debug.VERBOSE, "Notified");
                    if (outgoing.size() > 0)
                        m = outgoing.remove();
                    if (Debug.debug) Debug.print(Debug.DEBUG, "Got message: " + m);
                }
                if (null != m)
                    sendMessage(m);
                m = null;
            }

            if (Debug.debug) Debug.print(Debug.INFO, "Flushing outbound queue and quitting");
            // flush the outbound queue before disconnect.
            if (null != outgoing) do {
                EfficientQueue ogq = outgoing;
                synchronized (ogq) {
                    outgoing = null;
                }
                if (!ogq.isEmpty())
                    m = ogq.remove();
                else m = null;
                sendMessage(m);
            } while (null != m);

            // close the underlying streams
        }
    }

    /**
     * Timeout in us on checking the BUS for incoming messages and sending outgoing messages
     */
    protected static final int TIMEOUT = 100000;
    /**
     * Initial size of the pending calls map
     */
    private static final int PENDING_MAP_INITIAL_SIZE = 10;
    static final String BUSNAME_REGEX = "^[-_a-zA-Z][-_a-zA-Z0-9]*(\\.[-_a-zA-Z][-_a-zA-Z0-9]*)*$";
    static final String CONNID_REGEX = "^:[0-9]*\\.[0-9]*$";
    static final String OBJECT_REGEX = "^/([-_a-zA-Z0-9]+(/[-_a-zA-Z0-9]+)*)?$";
    static final byte THREADCOUNT = 4;
    static final int MAX_ARRAY_LENGTH = 67108864;
    static final int MAX_NAME_LENGTH = 255;
    protected Map<String, ExportedObject> exportedObjects;
    private ObjectTree objectTree;
    private _globalhandler _globalhandlerreference;
    protected Map<DBusInterface, RemoteObject> importedObjects;
    protected Map<SignalTuple, Vector<DBusSigHandler<? extends DBusSignal>>> handledSignals;
    protected EfficientMap pendingCalls;
    protected Map<MethodCall, CallbackHandler<? extends Object>> pendingCallbacks;
    protected Map<MethodCall, DBusAsyncReply<? extends Object>> pendingCallbackReplys;
    protected LinkedList<Runnable> runnables;
    protected LinkedList<_workerthread> workers;
    protected FallbackContainer fallbackcontainer;
    protected boolean _run;
    EfficientQueue outgoing;
    LinkedList<Error> pendingErrors;
    private static final Map<Thread, DBusCallInfo> infomap = new HashMap<Thread, DBusCallInfo>();
    protected _thread thread;
    protected _sender sender;
    protected Transport transport;
    protected String addr;
    protected boolean weakreferences = false;
    static final Pattern dollar_pattern = Pattern.compile("[$]");
    public static final boolean EXCEPTION_DEBUG;
    static final boolean FLOAT_SUPPORT;
    protected boolean connected = false;

    static {
        FLOAT_SUPPORT = (null != System.getenv("DBUS_JAVA_FLOATS"));
        EXCEPTION_DEBUG = (null != System.getenv("DBUS_JAVA_EXCEPTION_DEBUG"));
        if (EXCEPTION_DEBUG) {
            Debug.print("Debugging of internal exceptions enabled");
            Debug.setThrowableTraces(true);
        }
        if (Debug.debug) {
            File f = new File("debug.conf");
            if (f.exists()) {
                Debug.print("Loading debug config file: " + f);
                try {
                    Debug.loadConfig(f);
                } catch (IOException IOe) {
                }
            } else {
                Properties p = new Properties();
                p.setProperty("ALL", "INFO");
                Debug.print("debug config file " + f + " does not exist, not loading.");
            }
            Debug.setHexDump(true);
        }
    }

    protected AbstractConnection(String address) throws DBusException {
        exportedObjects = new HashMap<String, ExportedObject>();
        importedObjects = new HashMap<DBusInterface, RemoteObject>();
        _globalhandlerreference = new _globalhandler();
        synchronized (exportedObjects) {
            exportedObjects.put(null, new ExportedObject(_globalhandlerreference, weakreferences));
        }
        handledSignals = new HashMap<SignalTuple, Vector<DBusSigHandler<? extends DBusSignal>>>();
        pendingCalls = new EfficientMap(PENDING_MAP_INITIAL_SIZE);
        outgoing = new EfficientQueue(PENDING_MAP_INITIAL_SIZE);
        pendingCallbacks = new HashMap<MethodCall, CallbackHandler<? extends Object>>();
        pendingCallbackReplys = new HashMap<MethodCall, DBusAsyncReply<? extends Object>>();
        pendingErrors = new LinkedList<Error>();
        runnables = new LinkedList<Runnable>();
        workers = new LinkedList<_workerthread>();
        objectTree = new ObjectTree();
        fallbackcontainer = new FallbackContainer();
        synchronized (workers) {
            for (int i = 0; i < THREADCOUNT; i++) {
                _workerthread t = new _workerthread();
                t.start();
                workers.add(t);
            }
        }
        _run = true;
        addr = address;
    }

    protected void listen() {
        // start listening
        thread = new _thread();
        thread.start();
        sender = new _sender();
        sender.start();
    }

    /**
     * Change the number of worker threads to receive method calls and handle signals.
     * Default is 4 threads
     *
     * @param newcount The new number of worker Threads to use.
     */
    public void changeThreadCount(byte newcount) {
        synchronized (workers) {
            if (workers.size() > newcount) {
                int n = workers.size() - newcount;
                for (int i = 0; i < n; i++) {
                    _workerthread t = workers.removeFirst();
                    t.halt();
                }
            } else if (workers.size() < newcount) {
                int n = newcount - workers.size();
                for (int i = 0; i < n; i++) {
                    _workerthread t = new _workerthread();
                    t.start();
                    workers.add(t);
                }
            }
        }
    }

    private void addRunnable(Runnable r) {
        synchronized (runnables) {
            runnables.add(r);
            runnables.notifyAll();
        }
    }

    String getExportedObject(DBusInterface i) throws DBusException {
        synchronized (exportedObjects) {
            for (String s : exportedObjects.keySet())
                if (i.equals(exportedObjects.get(s).object.get()))
                    return s;
        }

        String s = importedObjects.get(i).objectpath;
        if (null != s) return s;

        throw new DBusException("Not an object exported or imported by this connection");
    }

    abstract DBusInterface getExportedObject(String source, String path) throws DBusException;

    /**
     * Returns a structure with information on the current method call.
     *
     * @return the DBusCallInfo for this method call, or null if we are not in a method call.
     */
    public static DBusCallInfo getCallInfo() {
        DBusCallInfo info;
        synchronized (infomap) {
            info = infomap.get(Thread.currentThread());
        }
        return info;
    }

    /**
     * If set to true the bus will not hold a strong reference to exported objects.
     * If they go out of scope they will automatically be unexported from the bus.
     * The default is to hold a strong reference, which means objects must be
     * explicitly unexported before they will be garbage collected.
     */
    public void setWeakReferences(boolean weakreferences) {
        this.weakreferences = weakreferences;
    }

    /**
     * Export an object so that its methods can be called on DBus.
     *
     * @param objectpath The path to the object we are exposing. MUST be in slash-notation, like "/org/freedesktop/Local",
     *                   and SHOULD end with a capitalised term. Only one object may be exposed on each path at any one time, but an object
     *                   may be exposed on several paths at once.
     * @param object     The object to export.
     * @throws DBusException If the objectpath is already exporting an object.
     *                       or if objectpath is incorrectly formatted,
     */
    public void exportObject(String objectpath, DBusInterface object) throws DBusException {
        if (null == objectpath || "".equals(objectpath))
            throw new DBusException(getString("missingObjectPath"));
        if (!objectpath.matches(OBJECT_REGEX) || objectpath.length() > MAX_NAME_LENGTH)
            throw new DBusException(getString("invalidObjectPath") + objectpath);
        synchronized (exportedObjects) {
            if (null != exportedObjects.get(objectpath))
                throw new DBusException(getString("objectAlreadyExported"));
            ExportedObject eo = new ExportedObject(object, weakreferences);
            exportedObjects.put(objectpath, eo);
            objectTree.add(objectpath, eo, eo.introspectiondata);
        }
    }

    /**
     * Export an object as a fallback object.
     * This object will have it's methods invoked for all paths starting
     * with this object path.
     *
     * @param objectprefix The path below which the fallback handles calls.
     *                     MUST be in slash-notation, like "/org/freedesktop/Local",
     * @param object       The object to export.
     * @throws DBusException If the objectpath is incorrectly formatted,
     */
    public void addFallback(String objectprefix, DBusInterface object) throws DBusException {
        if (null == objectprefix || "".equals(objectprefix))
            throw new DBusException(getString("missingObjectPath"));
        if (!objectprefix.matches(OBJECT_REGEX) || objectprefix.length() > MAX_NAME_LENGTH)
            throw new DBusException(getString("invalidObjectPath") + objectprefix);
        ExportedObject eo = new ExportedObject(object, weakreferences);
        fallbackcontainer.add(objectprefix, eo);
    }

    /**
     * Remove a fallback
     *
     * @param objectprefix The prefix to remove the fallback for.
     */
    public void removeFallback(String objectprefix) {
        fallbackcontainer.remove(objectprefix);
    }

    /**
     * Stop Exporting an object
     *
     * @param objectpath The objectpath to stop exporting.
     */
    public void unExportObject(String objectpath) {
        synchronized (exportedObjects) {
            exportedObjects.remove(objectpath);
            objectTree.remove(objectpath);
        }
    }
    /**
     * Return a reference to a remote object.
     * This method will resolve the well known name (if given) to a unique bus name when you call it.
     * This means that if a well known name is released by one process and acquired by another calls to
     * objects gained from this method will continue to operate on the original process.
     * @param busname The bus name to connect to. Usually a well known bus name in dot-notation (such as "org.freedesktop.local")
     * or may be a DBus address such as ":1-16".
     * @param objectpath The path on which the process is exporting the object.$
     * @param type The interface they are exporting it on. This type must have the same full class name and exposed method signatures
     * as the interface the remote object is exporting.
     * @return A reference to a remote object.
     * @throws ClassCastException If type is not a sub-type of DBusInterface
     * @throws DBusException If busname or objectpath are incorrectly formatted or type is not in a package.
     */
    /**
     * Send a signal.
     *
     * @param signal The signal to send.
     */
    public void sendSignal(DBusSignal signal) {
        queueOutgoing(signal);
    }

    void queueOutgoing(Message m) {
        synchronized (outgoing) {
            if (null == outgoing) return;
            outgoing.add(m);
            if (Debug.debug) Debug.print(Debug.DEBUG, "Notifying outgoing thread");
            outgoing.notifyAll();
        }
    }

    /**
     * Remove a Signal Handler.
     * Stops listening for this signal.
     *
     * @param type The signal to watch for.
     * @throws DBusException      If listening for the signal on the bus failed.
     * @throws ClassCastException If type is not a sub-type of DBusSignal.
     */
    public <T extends DBusSignal> void removeSigHandler(Class<T> type, DBusSigHandler<T> handler) throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(type)) throw new ClassCastException(getString("notDBusSignal"));
        removeSigHandler(new DBusMatchRule(type), handler);
    }

    /**
     * Remove a Signal Handler.
     * Stops listening for this signal.
     *
     * @param type   The signal to watch for.
     * @param object The object emitting the signal.
     * @throws DBusException      If listening for the signal on the bus failed.
     * @throws ClassCastException If type is not a sub-type of DBusSignal.
     */
    public <T extends DBusSignal> void removeSigHandler(Class<T> type, DBusInterface object, DBusSigHandler<T> handler) throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(type)) throw new ClassCastException(getString("notDBusSignal"));
        String objectpath = importedObjects.get(object).objectpath;
        if (!objectpath.matches(OBJECT_REGEX) || objectpath.length() > MAX_NAME_LENGTH)
            throw new DBusException(getString("invalidObjectPath") + objectpath);
        removeSigHandler(new DBusMatchRule(type, null, objectpath), handler);
    }

    protected abstract <T extends DBusSignal> void removeSigHandler(DBusMatchRule rule, DBusSigHandler<T> handler) throws DBusException;

    /**
     * Add a Signal Handler.
     * Adds a signal handler to call when a signal is received which matches the specified type and name.
     *
     * @param type    The signal to watch for.
     * @param handler The handler to call when a signal is received.
     * @throws DBusException      If listening for the signal on the bus failed.
     * @throws ClassCastException If type is not a sub-type of DBusSignal.
     */
    @SuppressWarnings("unchecked")
    public <T extends DBusSignal> void addSigHandler(Class<T> type, DBusSigHandler<T> handler) throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(type)) throw new ClassCastException(getString("notDBusSignal"));
        addSigHandler(new DBusMatchRule(type), (DBusSigHandler<? extends DBusSignal>) handler);
    }

    /**
     * Add a Signal Handler.
     * Adds a signal handler to call when a signal is received which matches the specified type, name and object.
     *
     * @param type    The signal to watch for.
     * @param object  The object from which the signal will be emitted
     * @param handler The handler to call when a signal is received.
     * @throws DBusException      If listening for the signal on the bus failed.
     * @throws ClassCastException If type is not a sub-type of DBusSignal.
     */
    @SuppressWarnings("unchecked")
    public <T extends DBusSignal> void addSigHandler(Class<T> type, DBusInterface object, DBusSigHandler<T> handler) throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(type)) throw new ClassCastException(getString("notDBusSignal"));
        String objectpath = importedObjects.get(object).objectpath;
        if (!objectpath.matches(OBJECT_REGEX) || objectpath.length() > MAX_NAME_LENGTH)
            throw new DBusException(getString("invalidObjectPath") + objectpath);
        addSigHandler(new DBusMatchRule(type, null, objectpath), (DBusSigHandler<? extends DBusSignal>) handler);
    }

    protected abstract <T extends DBusSignal> void addSigHandler(DBusMatchRule rule, DBusSigHandler<T> handler) throws DBusException;

    protected <T extends DBusSignal> void addSigHandlerWithoutMatch(Class<? extends DBusSignal> signal, DBusSigHandler<T> handler) throws DBusException {
        DBusMatchRule rule = new DBusMatchRule(signal);
        SignalTuple key = new SignalTuple(rule.getInterface(), rule.getMember(), rule.getObject(), rule.getSource());
        synchronized (handledSignals) {
            Vector<DBusSigHandler<? extends DBusSignal>> v = handledSignals.get(key);
            if (null == v) {
                v = new Vector<DBusSigHandler<? extends DBusSignal>>();
                v.add(handler);
                handledSignals.put(key, v);
            } else
                v.add(handler);
        }
    }

    /**
     * Disconnect from the Bus.
     */
    public void disconnect() {
        connected = false;
        if (Debug.debug) Debug.print(Debug.INFO, "Sending disconnected signal");
        try {
            handleMessage(new org.freedesktop.DBus.Local.Disconnected("/"));
        } catch (Exception ee) {
            if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, ee);
        }

        if (Debug.debug) Debug.print(Debug.INFO, "Disconnecting Abstract Connection");
        // run all pending tasks.
        while (runnables.size() > 0)
            synchronized (runnables) {
                runnables.notifyAll();
            }

        // stop the main thread
        _run = false;

        // unblock the sending thread.
        synchronized (outgoing) {
            outgoing.notifyAll();
        }

        // disconnect from the trasport layer
        try {
            if (null != transport) {
                transport.disconnect();
                transport = null;
            }
        } catch (IOException IOe) {
            if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, IOe);
        }

        // stop all the workers
        synchronized (workers) {
            for (_workerthread t : workers)
                t.halt();
        }

        // make sure none are blocking on the runnables queue still
        synchronized (runnables) {
            runnables.notifyAll();
        }
    }

    public void finalize() {
        disconnect();
    }

    /**
     * Return any DBus error which has been received.
     *
     * @return A DBusExecutionException, or null if no error is pending.
     */
    public DBusExecutionException getError() {
        synchronized (pendingErrors) {
            if (pendingErrors.size() == 0) return null;
            else
                return pendingErrors.removeFirst().getException();
        }
    }

    /**
     * Call a method asynchronously and set a callback.
     * This handler will be called in a separate thread.
     *
     * @param object     The remote object on which to call the method.
     * @param m          The name of the method on the interface to call.
     * @param callback   The callback handler.
     * @param parameters The parameters to call the method with.
     */
    @SuppressWarnings("unchecked")
    public <A> void callWithCallback(DBusInterface object, String m, CallbackHandler<A> callback, Object... parameters) {
        if (Debug.debug) Debug.print(Debug.VERBOSE, "callWithCallback(" + object + "," + m + ", " + callback);
        Class[] types = new Class[parameters.length];
        for (int i = 0; i < parameters.length; i++)
            types[i] = parameters[i].getClass();
        RemoteObject ro = importedObjects.get(object);

        try {
            Method me;
            if (null == ro.iface)
                me = object.getClass().getMethod(m, types);
            else
                me = ro.iface.getMethod(m, types);
            RemoteInvocationHandler.executeRemoteMethod(ro, me, this, RemoteInvocationHandler.CALL_TYPE_CALLBACK, callback, parameters);
        } catch (DBusExecutionException DBEe) {
            if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, DBEe);
            throw DBEe;
        } catch (Exception e) {
            if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);
            throw new DBusExecutionException(e.getMessage());
        }
    }

    /**
     * Call a method asynchronously and get a handle with which to get the reply.
     *
     * @param object     The remote object on which to call the method.
     * @param m          The name of the method on the interface to call.
     * @param parameters The parameters to call the method with.
     * @return A handle to the call.
     */
    @SuppressWarnings("unchecked")
    public DBusAsyncReply callMethodAsync(DBusInterface object, String m, Object... parameters) {
        Class<?>[] types = new Class[parameters.length];
        for (int i = 0; i < parameters.length; i++)
            types[i] = parameters[i].getClass();
        RemoteObject ro = importedObjects.get(object);

        try {
            Method me;
            if (null == ro.iface)
                me = object.getClass().getMethod(m, types);
            else
                me = ro.iface.getMethod(m, types);
            return (DBusAsyncReply) RemoteInvocationHandler.executeRemoteMethod(ro, me, this, RemoteInvocationHandler.CALL_TYPE_ASYNC, null, parameters);
        } catch (DBusExecutionException DBEe) {
            if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, DBEe);
            throw DBEe;
        } catch (Exception e) {
            if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);
            throw new DBusExecutionException(e.getMessage());
        }
    }

    private void handleMessage(final MethodCall m) throws DBusException {
        if (Debug.debug) Debug.print(Debug.DEBUG, "Handling incoming method call: " + m);

        ExportedObject eo = null;
        Method meth = null;
        Object o = null;

        if (null == m.getInterface() ||
                m.getInterface().equals("org.freedesktop.DBus.Peer") ||
                m.getInterface().equals("org.freedesktop.DBus.Introspectable")) {
            synchronized (exportedObjects) {
                eo = exportedObjects.get(null);
            }
            if (null != eo && null == eo.object.get()) {
                unExportObject(null);
                eo = null;
            }
            if (null != eo) {
                meth = eo.methods.get(new MethodTuple(m.getName(), m.getSig()));
            }
            if (null != meth)
                o = new _globalhandler(m.getPath());
            else
                eo = null;
        }
        if (null == o) {
            // now check for specific exported functions

            synchronized (exportedObjects) {
                eo = exportedObjects.get(m.getPath());
            }
            if (null != eo && null == eo.object.get()) {
                if (Debug.debug) Debug.print(Debug.INFO, "Unexporting " + m.getPath() + " implicitly");
                unExportObject(m.getPath());
                eo = null;
            }

            if (null == eo) {
                eo = fallbackcontainer.get(m.getPath());
            }

            if (null == eo) {
                try {
                    queueOutgoing(new Error(m, new DBus.Error.UnknownObject(m.getPath() + getString("notObjectProvidedByProcess"))));
                } catch (DBusException DBe) {
                }
                return;
            }
            if (Debug.debug) {
                Debug.print(Debug.VERBOSE, "Searching for method " + m.getName() + " with signature " + m.getSig());
                Debug.print(Debug.VERBOSE, "List of methods on " + eo + ":");
                for (MethodTuple mt : eo.methods.keySet())
                    Debug.print(Debug.VERBOSE, "   " + mt + " => " + eo.methods.get(mt));
            }
            meth = eo.methods.get(new MethodTuple(m.getName(), m.getSig()));
            if (null == meth) {
                try {
                    queueOutgoing(new Error(m, new DBus.Error.UnknownMethod(MessageFormat.format(getString("methodDoesNotExist"), new Object[]{m.getInterface(), m.getName()}))));
                } catch (DBusException DBe) {
                }
                return;
            }
            o = eo.object.get();
        }

        // now execute it
        final Method me = meth;
        final Object ob = o;
        final boolean noreply = (1 == (m.getFlags() & Message.Flags.NO_REPLY_EXPECTED));
        final DBusCallInfo info = new DBusCallInfo(m);
        final AbstractConnection conn = this;
        if (Debug.debug) Debug.print(Debug.VERBOSE, "Adding Runnable for method " + meth);
        addRunnable(new Runnable() {
            private boolean run = false;

            public synchronized void run() {
                if (run) return;
                run = true;
                if (Debug.debug) Debug.print(Debug.DEBUG, "Running method " + me + " for remote call");
                try {
                    Type[] ts = me.getGenericParameterTypes();
                    m.setArgs(Marshalling.deSerializeParameters(m.getParameters(), ts, conn));
                    if (Debug.debug)
                        Debug.print(Debug.VERBOSE, "Deserialised " + Arrays.deepToString(m.getParameters()) + " to types " + Arrays.deepToString(ts));
                } catch (Exception e) {
                    if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);
                    try {
                        conn.queueOutgoing(new Error(m, new DBus.Error.UnknownMethod(getString("deSerializationFailure") + e)));
                    } catch (DBusException DBe) {
                    }
                    return;
                }

                try {
                    synchronized (infomap) {
                        infomap.put(Thread.currentThread(), info);
                    }
                    Object result;
                    try {
                        if (Debug.debug)
                            Debug.print(Debug.VERBOSE, "Invoking Method: " + me + " on " + ob + " with parameters " + Arrays.deepToString(m.getParameters()));
                        result = me.invoke(ob, m.getParameters());
                    } catch (InvocationTargetException ITe) {
                        if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, ITe.getCause());
                        throw ITe.getCause();
                    }
                    synchronized (infomap) {
                        infomap.remove(Thread.currentThread());
                    }
                    if (!noreply) {
                        MethodReturn reply;
                        if (Void.TYPE.equals(me.getReturnType()))
                            reply = new MethodReturn(m, null);
                        else {
                            StringBuffer sb = new StringBuffer();
                            for (String s : Marshalling.getDBusType(me.getGenericReturnType()))
                                sb.append(s);
                            Object[] nr = Marshalling.convertParameters(new Object[]{result}, new Type[]{me.getGenericReturnType()}, conn);

                            reply = new MethodReturn(m, sb.toString(), nr);
                        }
                        conn.queueOutgoing(reply);
                    }
                } catch (DBusExecutionException DBEe) {
                    if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, DBEe);
                    try {
                        conn.queueOutgoing(new Error(m, DBEe));
                    } catch (DBusException DBe) {
                    }
                } catch (Throwable e) {
                    if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);
                    try {
                        conn.queueOutgoing(new Error(m, new DBusExecutionException(MessageFormat.format(getString("errorExecutingMethod"), new Object[]{m.getInterface(), m.getName(), e.getMessage()}))));
                    } catch (DBusException DBe) {
                    }
                }
            }
        });
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    private void handleMessage(final DBusSignal s) {
        if (Debug.debug) Debug.print(Debug.DEBUG, "Handling incoming signal: " + s);
        Vector<DBusSigHandler<? extends DBusSignal>> v = new Vector<DBusSigHandler<? extends DBusSignal>>();
        synchronized (handledSignals) {
            Vector<DBusSigHandler<? extends DBusSignal>> t;
            t = handledSignals.get(new SignalTuple(s.getInterface(), s.getName(), null, null));
            if (null != t) v.addAll(t);
            t = handledSignals.get(new SignalTuple(s.getInterface(), s.getName(), s.getPath(), null));
            if (null != t) v.addAll(t);
            t = handledSignals.get(new SignalTuple(s.getInterface(), s.getName(), null, s.getSource()));
            if (null != t) v.addAll(t);
            t = handledSignals.get(new SignalTuple(s.getInterface(), s.getName(), s.getPath(), s.getSource()));
            if (null != t) v.addAll(t);
        }
        if (0 == v.size()) return;
        final AbstractConnection conn = this;
        for (final DBusSigHandler<? extends DBusSignal> h : v) {
            if (Debug.debug) Debug.print(Debug.VERBOSE, "Adding Runnable for signal " + s + " with handler " + h);
            addRunnable(new Runnable() {
                private boolean run = false;

                public synchronized void run() {
                    if (run) return;
                    run = true;
                    try {
                        DBusSignal rs;
                        if (s instanceof DBusSignal.internalsig || s.getClass().equals(DBusSignal.class))
                            rs = s.createReal(conn);
                        else
                            rs = s;
                        ((DBusSigHandler<DBusSignal>) h).handle(rs);
                    } catch (DBusException DBe) {
                        if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, DBe);
                        try {
                            conn.queueOutgoing(new Error(s, new DBusExecutionException("Error handling signal " + s.getInterface() + "." + s.getName() + ": " + DBe.getMessage())));
                        } catch (DBusException DBe2) {
                        }
                    }
                }
            });
        }
    }

    private void handleMessage(final Error err) {
        if (Debug.debug) Debug.print(Debug.DEBUG, "Handling incoming error: " + err);
        MethodCall m = null;
        if (null == pendingCalls) return;
        synchronized (pendingCalls) {
            if (pendingCalls.contains(err.getReplySerial()))
                m = pendingCalls.remove(err.getReplySerial());
        }
        if (null != m) {
            m.setReply(err);
            CallbackHandler cbh = null;
            DBusAsyncReply asr = null;
            synchronized (pendingCallbacks) {
                cbh = pendingCallbacks.remove(m);
                if (Debug.debug) Debug.print(Debug.VERBOSE, cbh + " = pendingCallbacks.remove(" + m + ")");
                asr = pendingCallbackReplys.remove(m);
            }
            // queue callback for execution
            if (null != cbh) {
                final CallbackHandler fcbh = cbh;
                if (Debug.debug) Debug.print(Debug.VERBOSE, "Adding Error Runnable with callback handler " + fcbh);
                addRunnable(new Runnable() {
                    private boolean run = false;

                    public synchronized void run() {
                        if (run) return;
                        run = true;
                        try {
                            if (Debug.debug) Debug.print(Debug.VERBOSE, "Running Error Callback for " + err);
                            DBusCallInfo info = new DBusCallInfo(err);
                            synchronized (infomap) {
                                infomap.put(Thread.currentThread(), info);
                            }

                            fcbh.handleError(err.getException());
                            synchronized (infomap) {
                                infomap.remove(Thread.currentThread());
                            }

                        } catch (Exception e) {
                            if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);
                        }
                    }
                });
            }

        } else
            synchronized (pendingErrors) {
                pendingErrors.addLast(err);
            }
    }

    @SuppressWarnings("unchecked")
    private void handleMessage(final MethodReturn mr) {
        if (Debug.debug) Debug.print(Debug.DEBUG, "Handling incoming method return: " + mr);
        MethodCall m = null;
        if (null == pendingCalls) return;
        synchronized (pendingCalls) {
            if (pendingCalls.contains(mr.getReplySerial()))
                m = pendingCalls.remove(mr.getReplySerial());
        }
        if (null != m) {
            m.setReply(mr);
            mr.setCall(m);
            CallbackHandler cbh = null;
            DBusAsyncReply asr = null;
            synchronized (pendingCallbacks) {
                cbh = pendingCallbacks.remove(m);
                if (Debug.debug) Debug.print(Debug.VERBOSE, cbh + " = pendingCallbacks.remove(" + m + ")");
                asr = pendingCallbackReplys.remove(m);
            }
            // queue callback for execution
            if (null != cbh) {
                final CallbackHandler fcbh = cbh;
                final DBusAsyncReply fasr = asr;
                if (Debug.debug)
                    Debug.print(Debug.VERBOSE, "Adding Runnable for method " + fasr.getMethod() + " with callback handler " + fcbh);
                addRunnable(new Runnable() {
                    private boolean run = false;

                    public synchronized void run() {
                        if (run) return;
                        run = true;
                        try {
                            if (Debug.debug) Debug.print(Debug.VERBOSE, "Running Callback for " + mr);
                            DBusCallInfo info = new DBusCallInfo(mr);
                            synchronized (infomap) {
                                infomap.put(Thread.currentThread(), info);
                            }

                            fcbh.handle(RemoteInvocationHandler.convertRV(mr.getSig(), mr.getParameters(), fasr.getMethod(), fasr.getConnection()));
                            synchronized (infomap) {
                                infomap.remove(Thread.currentThread());
                            }

                        } catch (Exception e) {
                            if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);
                        }
                    }
                });
            }

        } else
            try {
                queueOutgoing(new Error(mr, new DBusExecutionException(getString("spuriousReply"))));
            } catch (DBusException DBe) {
            }
    }

    protected void sendMessage(Message m) {
        try {
            if (!connected) throw new NotConnected(getString("disconnected"));
            if (m instanceof DBusSignal)
                ((DBusSignal) m).appendbody(this);

            if (m instanceof MethodCall) {
                if (0 == (m.getFlags() & Message.Flags.NO_REPLY_EXPECTED))
                    if (null == pendingCalls)
                        ((MethodCall) m).setReply(new Error("org.freedesktop.DBus.Local", "org.freedesktop.DBus.Local.disconnected", 0, "s", new Object[]{getString("disconnected")}));
                    else synchronized (pendingCalls) {
                        pendingCalls.put(m.getSerial(), (MethodCall) m);
                    }
            }

            transport.mout.writeMessage(m);

        } catch (Exception e) {
            if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);
            if (m instanceof MethodCall && e instanceof NotConnected)
                try {
                    ((MethodCall) m).setReply(new Error("org.freedesktop.DBus.Local", "org.freedesktop.DBus.Local.disconnected", 0, "s", new Object[]{getString("disconnected")}));
                } catch (DBusException DBe) {
                }
            if (m instanceof MethodCall && e instanceof DBusExecutionException)
                try {
                    ((MethodCall) m).setReply(new Error(m, e));
                } catch (DBusException DBe) {
                }
            else if (m instanceof MethodCall)
                try {
                    if (Debug.debug) Debug.print(Debug.INFO, "Setting reply to " + m + " as an error");
                    ((MethodCall) m).setReply(new Error(m, new DBusExecutionException(getString("messageFailedSend") + e.getMessage())));
                } catch (DBusException DBe) {
                }
            else if (m instanceof MethodReturn)
                try {
                    transport.mout.writeMessage(new Error(m, e));
                } catch (IOException IOe) {
                    if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, IOe);
                } catch (DBusException IOe) {
                    if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);
                }
            if (e instanceof IOException) disconnect();
        }
    }

    private Message readIncoming() throws DBusException {
        if (!connected) throw new NotConnected(getString("missingTransport"));
        Message m = null;
        try {
            m = transport.min.readMessage();
        } catch (IOException IOe) {
            throw new FatalDBusException(IOe.getMessage());
        }
        return m;
    }

    /**
     * Returns the address this connection is connected to.
     */
    public BusAddress getAddress() throws ParseException {
        return new BusAddress(addr);
    }
}
