package org.freedesktop.dbus.connections;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.ByteOrder;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.freedesktop.dbus.DBusAsyncReply;
import org.freedesktop.dbus.DBusCallInfo;
import org.freedesktop.dbus.DBusMatchRule;
import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.MethodTuple;
import org.freedesktop.dbus.RemoteInvocationHandler;
import org.freedesktop.dbus.RemoteObject;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfig;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.errors.Error;
import org.freedesktop.dbus.errors.UnknownMethod;
import org.freedesktop.dbus.errors.UnknownObject;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.exceptions.FatalDBusException;
import org.freedesktop.dbus.exceptions.NotConnected;
import org.freedesktop.dbus.interfaces.CallbackHandler;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.ExportedObject;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.messages.MethodReturn;
import org.freedesktop.dbus.messages.ObjectTree;
import org.freedesktop.dbus.utils.LoggingHelper;
import org.freedesktop.dbus.utils.NameableThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles a connection to DBus.
 */
public abstract class AbstractConnection implements Closeable {

    public static final boolean      FLOAT_SUPPORT          = null != System.getenv("DBUS_JAVA_FLOATS");
    public static final Pattern      BUSNAME_REGEX          = Pattern.compile("^[-_a-zA-Z][-_a-zA-Z0-9]*(\\.[-_a-zA-Z][-_a-zA-Z0-9]*)*$");
    public static final Pattern      CONNID_REGEX           = Pattern.compile("^:[0-9]*\\.[0-9]*$");
    public static final Pattern      OBJECT_REGEX_PATTERN   = Pattern.compile("^/([-_a-zA-Z0-9]+(/[-_a-zA-Z0-9]+)*)?$");
    public static final Pattern      DOLLAR_PATTERN         = Pattern.compile("[$]");

    public static final int          MAX_ARRAY_LENGTH       = 67108864;
    public static final int          MAX_NAME_LENGTH        = 255;

    /**
     * Connect timeout, used for TCP only.
     * @deprecated no longer used
     */
    @Deprecated(forRemoval = true, since = "4.2.2 - 2022-12-23")
    public static final int TCP_CONNECT_TIMEOUT     = 100000;

    /**
     * System property name containing the DBUS TCP SESSION address used by dbus-java DBusDaemon in TCP mode.
     * @deprecated is no longer in use
     */
    @Deprecated(since = "4.2.0 - 2022-08-04", forRemoval = true)
    public static final String TCP_ADDRESS_PROPERTY = "DBUS_TCP_SESSION";

    private static final Map<Thread, DBusCallInfo> INFOMAP = new ConcurrentHashMap<>();

    /** Lame method to setup endianness used on DBus messages */
    private static byte              endianness             = getSystemEndianness();

    private final Logger                                                          logger;

    private final ObjectTree                                                      objectTree;

    private final Map<String, ExportedObject>                                     exportedObjects;
    private final Map<DBusInterface, RemoteObject>                                importedObjects;

    private final PendingCallbackManager                                          callbackManager;

    private final FallbackContainer                                               fallbackContainer;

    private final Queue<Error>                                                    pendingErrorQueue;

    private final Map<DBusMatchRule, Queue<DBusSigHandler<? extends DBusSignal>>> handledSignals;
    private final Map<DBusMatchRule, Queue<DBusSigHandler<DBusSignal>>>           genericHandledSignals;
    private final Map<Long, MethodCall>                                           pendingCalls;

    private final IncomingMessageThread                                           readerThread;
    private final ExecutorService                                                 senderService;
    private final ReceivingService                                                receivingService;
    private final TransportBuilder                                                transportBuilder;

    private boolean                                                               weakreferences       = false;
    private volatile boolean                                                      disconnecting        = false;

    private AbstractTransport                                                     transport;

    private Optional<IDisconnectCallback>                                         disconnectCallback   =
            Optional.ofNullable(null);

    protected AbstractConnection(TransportConfig _transportConfig, ReceivingServiceConfig _rsCfg) throws DBusException {
        logger = LoggerFactory.getLogger(getClass());
        exportedObjects = Collections.synchronizedMap(new HashMap<>());
        importedObjects = new ConcurrentHashMap<>();

        exportedObjects.put(null, new ExportedObject(new GlobalHandler(this), weakreferences));

        handledSignals = new ConcurrentHashMap<>();
        genericHandledSignals = new ConcurrentHashMap<>();

        pendingCalls = Collections.synchronizedMap(new LinkedHashMap<>());
        callbackManager = new PendingCallbackManager();

        pendingErrorQueue = new ConcurrentLinkedQueue<>();

        receivingService = new ReceivingService(_rsCfg);
        senderService =
                Executors.newFixedThreadPool(1, new NameableThreadFactory("DBus Sender Thread-", false));

        objectTree = new ObjectTree();
        fallbackContainer = new FallbackContainer();

        transportBuilder = TransportBuilder.create(_transportConfig);
        readerThread = new IncomingMessageThread(this, transportBuilder.getAddress());

        try {
            transport = transportBuilder.build();
        } catch (IOException | DBusException _ex) {
            logger.debug("Error creating transport", _ex);
            if (_ex instanceof IOException) {
                internalDisconnect((IOException) _ex);
            }
            throw new DBusException("Failed to connect to bus: " + _ex.getMessage(), _ex);
        }
    }

    /**
     * Retrieves an remote object using source and path.
     * Will try to find suitable exported DBusInterface automatically.
     *
     * @param _source source
     * @param _path path
     *
     * @return {@link DBusInterface} compatible object
     */
    public abstract DBusInterface getExportedObject(String _source, String _path) throws DBusException;

    /**
     * Retrieves an remote object using source and path.
     * Will use the given type as object class.
     *
     * @param _source source
     * @param _path path
     * @param _type class of remote object
     *
     * @return {@link DBusInterface} compatible object
     */
    public abstract <T extends DBusInterface> T getExportedObject(String _source, String _path, Class<T> _type) throws DBusException;

    /**
     * Remove a match rule with the given {@link DBusSigHandler}.
     * The rule will only be removed from DBus if no other additional handlers are registered to the same rule.
     *
     * @param _rule rule to remove
     * @param _handler handler to remove
     *
     * @param <T> signal type
     *
     * @throws DBusException on error
     */
    protected abstract <T extends DBusSignal> void removeSigHandler(DBusMatchRule _rule, DBusSigHandler<T> _handler) throws DBusException;

    /**
     * Add a signal handler with the given {@link DBusMatchRule} to DBus.
     * The rule will be added to DBus if it was not added before.
     * If the rule was already added, the signal handler is added to the internal map receiving
     * the same signal as the first (and additional) handlers for this rule.
     *
     * @param _rule rule to add
     * @param _handler handler to use
     *
     * @param <T> signal type
     * @return closeable that removes signal handler
     *
     * @throws DBusException on error
     */
    protected abstract <T extends DBusSignal> AutoCloseable addSigHandler(DBusMatchRule _rule, DBusSigHandler<T> _handler) throws DBusException;

    /**
     * Remove a generic signal handler with the given {@link DBusMatchRule}.
     * The rule will only be removed from DBus if no other additional handlers are registered to the same rule.
     *
     * @param _rule rule to remove
     * @param _handler handler to remove
     * @throws DBusException on error
     */
    protected abstract void removeGenericSigHandler(DBusMatchRule _rule, DBusSigHandler<DBusSignal> _handler) throws DBusException;

    /**
     * Adds a {@link DBusMatchRule} to with a generic signal handler.
     * Generic signal handlers allow receiving different signals with the same handler.
     * If the rule was already added, the signal handler is added to the internal map receiving
     * the same signal as the first (and additional) handlers for this rule.
     *
     * @param _rule rule to add
     * @param _handler handler to use
     * @return closeable that removes signal handler
     * @throws DBusException on error
     */
    protected abstract AutoCloseable addGenericSigHandler(DBusMatchRule _rule, DBusSigHandler<DBusSignal> _handler) throws DBusException;

    /**
     * The generated UUID of this machine.
     * @return String
     */
    public abstract String getMachineId();

    /**
     * If given type is null, will try to find suitable types by examining the given ifaces.
     * If a non-null type is given, returns the given type.
     *
     * @param <T> any DBusInterface compatible object
     * @param _type type or null
     * @param _ifaces interfaces to examining when type is null
     *
     * @return List
     */
    protected <T extends DBusInterface> List<Class<?>> findMatchingTypes(Class<T> _type, List<String> _ifaces) {
        List<Class<?>> ifcs = new ArrayList<>();
        if (_type == null) {
            for (String iface : _ifaces) {

                logger.debug("Trying interface {}", iface);
                int j = 0;
                while (j >= 0) {
                    try {
                        Class<?> ifclass = Class.forName(iface);
                        if (!ifcs.contains(ifclass)) {
                            ifcs.add(ifclass);
                        }
                        break;
                    } catch (Exception _ex) {
                        logger.trace("No class found for {}", iface, _ex);
                    }
                    j = iface.lastIndexOf('.');
                    char[] cs = iface.toCharArray();
                    if (j >= 0) {
                        cs[j] = '$';
                        iface = String.valueOf(cs);
                    }
                }
            }
        } else {
            ifcs.add(_type);
        }
        return ifcs;
    }

    /**
     * Start reading and sending messages.
     */
    protected void listen() {
        readerThread.start();
    }

    public String getExportedObject(DBusInterface _interface) throws DBusException {

        Optional<Entry<String, ExportedObject>> foundInterface =
                getExportedObjects().entrySet().stream()
                    .filter(e -> _interface.equals(e.getValue().getObject().get()))
                    .findFirst();
        if (foundInterface.isPresent()) {
            return foundInterface.get().getKey();
        } else {
            RemoteObject rObj = getImportedObjects().get(_interface);
            if (rObj != null) {
                String s = rObj.getObjectPath();
                if (s != null) {
                    return s;
                }
            }

            throw new DBusException("Not an object exported or imported by this connection");
        }

    }

    /**
     * Change the number of worker threads to receive method calls and handle signals. Default is 4 threads
     *
     * @param _newPoolSize
     *            The new number of worker Threads to use.
     * @deprecated does nothing as threading has been changed significantly
     */
    @Deprecated(forRemoval = true, since = "4.1.0")
    public void changeThreadCount(byte _newPoolSize) {

    }

    /**
     * If set to true the bus will not hold a strong reference to exported objects. If they go out of scope they will
     * automatically be unexported from the bus. The default is to hold a strong reference, which means objects must be
     * explicitly unexported before they will be garbage collected.
     *
     * @param _weakreferences
     *            reference
     */
    public void setWeakReferences(boolean _weakreferences) {
        this.weakreferences = _weakreferences;
    }

    /**
     * Export an object so that its methods can be called on DBus.
     *
     * @param _objectPath
     *            The path to the object we are exposing. MUST be in slash-notation, like "/org/freedesktop/Local", and
     *            SHOULD end with a capitalised term. Only one object may be exposed on each path at any one time, but
     *            an object may be exposed on several paths at once.
     * @param _object
     *            The object to export.
     * @throws DBusException
     *             If the objectpath is already exporting an object. or if objectpath is incorrectly formatted,
     */
    public void exportObject(String _objectPath, DBusInterface _object) throws DBusException {
        if (null == _objectPath || _objectPath.isEmpty()) {
            throw new DBusException("Must Specify an Object Path");
        }
        if (_objectPath.length() > MAX_NAME_LENGTH || !(OBJECT_REGEX_PATTERN.matcher(_objectPath).matches())) {
            throw new DBusException("Invalid object path: " + _objectPath);
        }
        synchronized (getExportedObjects()) {
            if (null != getExportedObjects().get(_objectPath)) {
                throw new DBusException("Object already exported");
            }
            ExportedObject eo = new ExportedObject(_object, weakreferences);
            getExportedObjects().put(_objectPath, eo);
            synchronized (getObjectTree()) {
                getObjectTree().add(_objectPath, eo, eo.getIntrospectiondata());
            }
        }
    }

    /**
     * Export an object so that its methods can be called on DBus. The path to the object will be taken from the
     * {@link DBusInterface#getObjectPath()} method, make sure it is implemented and returns immutable value.
     * If you want export object with multiple paths, please use {@link AbstractConnection#exportObject(String, DBusInterface)}.
     *
     * @param _object
     *            The object to export.
     * @throws DBusException
     *             If the object path is already exporting an object or if object path is incorrectly formatted.
     */
    public void exportObject(DBusInterface _object) throws DBusException {
        Objects.requireNonNull(_object, "object must not be null");
        exportObject(_object.getObjectPath(), _object);
    }

    /**
     * Export an object as a fallback object. This object will have it's methods invoked for all paths starting with
     * this object path.
     *
     * @param _objectPrefix
     *            The path below which the fallback handles calls. MUST be in slash-notation, like
     *            "/org/freedesktop/Local",
     * @param _object
     *            The object to export.
     * @throws DBusException
     *             If the objectpath is incorrectly formatted,
     */
    public void addFallback(String _objectPrefix, DBusInterface _object) throws DBusException {
        if (null == _objectPrefix || _objectPrefix.isEmpty()) {
            throw new DBusException("Must Specify an Object Path");
        }
        if (_objectPrefix.length() > MAX_NAME_LENGTH || !OBJECT_REGEX_PATTERN.matcher(_objectPrefix).matches()) {
            throw new DBusException("Invalid object path: " + _objectPrefix);
        }
        ExportedObject eo = new ExportedObject(_object, weakreferences);
        fallbackContainer.add(_objectPrefix, eo);
    }

    /**
     * Remove a fallback
     *
     * @param _objectprefix
     *            The prefix to remove the fallback for.
     */
    public void removeFallback(String _objectprefix) {
        fallbackContainer.remove(_objectprefix);
    }

    /**
     * Stop Exporting an object
     *
     * @param _objectpath
     *            The objectpath to stop exporting.
     */
    public void unExportObject(String _objectpath) {
        synchronized (getExportedObjects()) {
            getExportedObjects().remove(_objectpath);
            getObjectTree().remove(_objectpath);
        }
    }

    /**
     * Send a message or signal to the DBus daemon.
     * @param _message message to send
     */
    public void sendMessage(Message _message) {
        if (!isConnected()) {
            throw new NotConnected("Cannot send message: Not connected");
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                sendMessageInternally(_message);
            }
        };

        senderService.execute(runnable);
    }

    /**
     * Remove a Signal Handler. Stops listening for this signal.
     *
     * @param <T>
     *            class extending {@link DBusSignal}
     * @param _type
     *            The signal to watch for.
     * @param _handler
     *            the handler
     * @throws DBusException
     *             If listening for the signal on the bus failed.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusSignal.
     */
    public <T extends DBusSignal> void removeSigHandler(Class<T> _type, DBusSigHandler<T> _handler) throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(_type)) {
            throw new ClassCastException("Not A DBus Signal");
        }

        removeSigHandler(new DBusMatchRule(_type), _handler);
    }

    /**
     * Remove a Signal Handler. Stops listening for this signal.
     *
     * @param <T>
     *            class extending {@link DBusSignal}
     * @param _type
     *            The signal to watch for.
     * @param _object
     *            The object emitting the signal.
     * @param _handler
     *            the handler
     * @throws DBusException
     *             If listening for the signal on the bus failed.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusSignal.
     */
    public <T extends DBusSignal> void removeSigHandler(Class<T> _type, DBusInterface _object, DBusSigHandler<T> _handler)
            throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(_type)) {
            throw new ClassCastException("Not A DBus Signal");
        }
        String objectpath = getImportedObjects().get(_object).getObjectPath();
        if (objectpath.length() > MAX_NAME_LENGTH || !OBJECT_REGEX_PATTERN.matcher(objectpath).matches()) {
            throw new DBusException("Invalid object path: " + objectpath);
        }
        removeSigHandler(new DBusMatchRule(_type, null, objectpath), _handler);
    }

    /**
     * Add a Signal Handler. Adds a signal handler to call when a signal is received which matches the specified type
     * and name.
     *
     * @param <T>
     *            class extending {@link DBusSignal}
     * @param _type
     *            The signal to watch for.
     * @param _handler
     *            The handler to call when a signal is received.
     * @return closeable that removes signal handler
     * @throws DBusException
     *             If listening for the signal on the bus failed.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusSignal.
     */
    public <T extends DBusSignal> AutoCloseable addSigHandler(Class<T> _type, DBusSigHandler<T> _handler) throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(_type)) {
            throw new ClassCastException("Not A DBus Signal");
        }
        return addSigHandler(new DBusMatchRule(_type), _handler);
    }

    /**
     * Add a Signal Handler. Adds a signal handler to call when a signal is received which matches the specified type,
     * name and object.
     *
     * @param <T>
     *            class extending {@link DBusSignal}
     * @param _type
     *            The signal to watch for.
     * @param _object
     *            The object from which the signal will be emitted
     * @param _handler
     *            The handler to call when a signal is received.
     * @return closeable that removes signal handler
     * @throws DBusException
     *             If listening for the signal on the bus failed.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusSignal.
     */
    public <T extends DBusSignal> AutoCloseable addSigHandler(Class<T> _type, DBusInterface _object, DBusSigHandler<T> _handler)
            throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(_type)) {
            throw new ClassCastException("Not A DBus Signal");
        }
        RemoteObject rObj = getImportedObjects().get(_object);
        if (rObj == null) {
            throw new DBusException("Not an object exported or imported by this connection");
        }
        String objectpath = rObj.getObjectPath();
        if (objectpath.length() > MAX_NAME_LENGTH || !OBJECT_REGEX_PATTERN.matcher(objectpath).matches()) {
            throw new DBusException("Invalid object path: " + objectpath);
        }
        return addSigHandler(new DBusMatchRule(_type, null, objectpath), _handler);
    }

    protected <T extends DBusSignal> void addSigHandlerWithoutMatch(Class<? extends DBusSignal> _signal, DBusSigHandler<T> _handler) throws DBusException {
        DBusMatchRule rule = new DBusMatchRule(_signal);
        synchronized (getHandledSignals()) {
            Queue<DBusSigHandler<? extends DBusSignal>> v = getHandledSignals().get(rule);
            if (null == v) {
                v = new ConcurrentLinkedQueue<>();
                v.add(_handler);
                getHandledSignals().put(rule, v);
            } else {
                v.add(_handler);
            }
        }
    }

    /**
     * Special disconnect method which may be used whenever some cleanup before or after
     * disconnection to DBus is required.
     * @param _before action execute before actual disconnect, null if not needed
     * @param _after action execute after disconnect, null if not needed
     */
    protected synchronized void disconnect(IDisconnectAction _before, IDisconnectAction _after) {
        if (_before != null) {
            _before.perform();
        }
        internalDisconnect(null);
        if (_after != null) {
            _after.perform();
        }
    }

    /**
     * Disconnects the DBus session.
     * This method is private as it should never be overwritten by subclasses,
     * otherwise we have an endless recursion when using {@link #disconnect(IDisconnectAction, IDisconnectAction)}
     * which then will cause a StackOverflowError.
     *
     * @param _connectionError exception caused the disconnection (null if intended disconnect)
     */
    protected final synchronized void internalDisconnect(IOException _connectionError) {

        if (!isConnected()) { // already disconnected
            logger.debug("Ignoring disconnect, already disconnected");
            return;
        }
        disconnecting = true;

        logger.debug("Disconnecting Abstract Connection");

        disconnectCallback.ifPresent(cb -> {
            Optional.ofNullable(_connectionError)
                .ifPresentOrElse(ex -> cb.disconnectOnError(ex), () -> cb.requestedDisconnect(null));
        });

        // stop reading new messages
        readerThread.terminate();

        // terminate the signal handling pool
        receivingService.shutdown(10, TimeUnit.SECONDS);

        // stop potentially waiting method-calls
        logger.debug("Notifying {} method call(s) to stop waiting for replies", getPendingCalls().size());
        Exception interrupt = _connectionError == null ? new IOException("Disconnecting") : _connectionError;
        for (MethodCall mthCall : getPendingCalls().values()) {
            try {
                mthCall.setReply(new Error(mthCall, interrupt));
            } catch (DBusException _ex) {
                logger.debug("Cannot set method reply to error", _ex);
            }
        }

        // shutdown sender executor service, send all remaining messages in main thread when no exception caused disconnection
        logger.debug("Shutting down SenderService");
        List<Runnable> remainingMsgsToSend = senderService.shutdownNow();
        // only try to send remaining messages when disconnection was not
        // caused by an IOException, otherwise we may block for method calls waiting for
        // reply which will never be received (due to disconnection by IOException)
        if (_connectionError == null) {
            for (Runnable runnable : remainingMsgsToSend) {
                runnable.run();
            }
        } else if (!remainingMsgsToSend.isEmpty()) {
            logger.debug("Will not send {} messages due to connection closed by IOException", remainingMsgsToSend.size());
        }

        // disconnect from the transport layer
        try {
            if (transport != null) {
                transport.close();
                transport = null;
            }
        } catch (IOException _ex) {
            logger.debug("Exception while disconnecting transport.", _ex);
        }

        // stop all the workers
        receivingService.shutdownNow();
        disconnecting = false;
    }

    /**
     * Disconnect from the Bus.
     */
    public synchronized void disconnect() {
        logger.debug("Disconnect called");
        internalDisconnect(null);
    }

    /**
     * Disconnect this session (for use in try-with-resources).
     */
    @Override
    public void close() throws IOException {
        disconnect();
    }

    /**
     * Call a method asynchronously and set a callback. This handler will be called in a separate thread.
     *
     * @param <A>
     *            whatever
     * @param _object
     *            The remote object on which to call the method.
     * @param _m
     *            The name of the method on the interface to call.
     * @param _callback
     *            The callback handler.
     * @param _parameters
     *            The parameters to call the method with.
     */
    public <A> void callWithCallback(DBusInterface _object, String _m, CallbackHandler<A> _callback,
            Object... _parameters) {
        logger.trace("callWithCallback({}, {}, {})", _object, _m, _callback);
        Class<?>[] types = createTypesArray(_parameters);
        RemoteObject ro = getImportedObjects().get(_object);

        try {
            Method me;
            if (null == ro.getInterface()) {
                me = _object.getClass().getMethod(_m, types);
            } else {
                me = ro.getInterface().getMethod(_m, types);
            }
            RemoteInvocationHandler.executeRemoteMethod(ro, me, this, RemoteInvocationHandler.CALL_TYPE_CALLBACK,
                    _callback, _parameters);
        } catch (DBusExecutionException _ex) {
            logger.debug("", _ex);
            throw _ex;
        } catch (Exception _ex) {
            logger.debug("", _ex);
            throw new DBusExecutionException(_ex.getMessage());
        }
    }

    /**
     * Call a method asynchronously and get a handle with which to get the reply.
     *
     * @param _object
     *            The remote object on which to call the method.
     * @param _method
     *            The name of the method on the interface to call.
     * @param _parameters
     *            The parameters to call the method with.
     * @return A handle to the call.
     */
    public DBusAsyncReply<?> callMethodAsync(DBusInterface _object, String _method, Object... _parameters) {
        Class<?>[] types = createTypesArray(_parameters);
        RemoteObject ro = getImportedObjects().get(_object);

        try {
            Method me;
            if (null == ro.getInterface()) {
                me = _object.getClass().getMethod(_method, types);
            } else {
                me = ro.getInterface().getMethod(_method, types);
            }
            return (DBusAsyncReply<?>) RemoteInvocationHandler.executeRemoteMethod(ro, me, this,
                    RemoteInvocationHandler.CALL_TYPE_ASYNC, null, _parameters);
        } catch (DBusExecutionException _ex) {
            logger.debug("", _ex);
            throw _ex;
        } catch (Exception _ex) {
            logger.debug("", _ex);
            throw new DBusExecutionException(_ex.getMessage());
        }
    }

    private static Class<?>[] createTypesArray(Object... _parameters) {
        if (_parameters == null) {
            return null;
        }
        return Arrays.stream(_parameters)
                .filter(p -> p != null) // do no try to convert null values to concrete class
                .map(p -> {
                    if (List.class.isAssignableFrom(p.getClass())) { // turn possible List subclasses (e.g. ArrayList) to interface class List
                        return List.class;
                    } else if (Map.class.isAssignableFrom(p.getClass())) { // do the same for Map subclasses
                        return Map.class;
                    } else if (Set.class.isAssignableFrom(p.getClass())) { // and also for Set subclasses
                        return Set.class;
                    } else {
                        return p.getClass();
                    }
                })
                .toArray(Class[]::new);
    }

    protected void handleException(Message _methodOrSignal, DBusExecutionException _exception) {
        try {
            sendMessage(new Error(_methodOrSignal, _exception));
        } catch (DBusException _ex) {
            logger.warn("Exception caught while processing previous error.", _ex);
        }
    }

    /**
     * Handle received message from DBus.
     * @param _message
     * @throws DBusException
     */
    void handleMessage(Message _message) throws DBusException {
        if (_message instanceof DBusSignal) {
            handleMessage((DBusSignal) _message, true);
        } else if (_message instanceof MethodCall) {
            handleMessage((MethodCall) _message);
        } else if (_message instanceof MethodReturn) {
            handleMessage((MethodReturn) _message);
        } else if (_message instanceof Error) {
            handleMessage((Error) _message);
        }
    }

    private void handleMessage(final MethodCall _methodCall) throws DBusException {
        logger.debug("Handling incoming method call: {}", _methodCall);

        ExportedObject exportObject;
        Method meth = null;
        Object o = null;

        if (null == _methodCall.getInterface() || _methodCall.getInterface().equals("org.freedesktop.DBus.Peer")
                || _methodCall.getInterface().equals("org.freedesktop.DBus.Introspectable")) {
            exportObject = getExportedObjects().get(null);
            if (null != exportObject && null == exportObject.getObject().get()) {
                unExportObject(null);
                exportObject = null;
            }
            if (null != exportObject) {
                meth = exportObject.getMethods().get(new MethodTuple(_methodCall.getName(), _methodCall.getSig()));
            }
            if (null != meth) {
                o = new GlobalHandler(this, _methodCall.getPath());
            }
        }
        if (null == o) {
            // now check for specific exported functions

            exportObject = getExportedObjects().get(_methodCall.getPath());
            if (exportObject != null && exportObject.getObject().get() == null) {
                logger.info("Unexporting {} implicitly", _methodCall.getPath());
                unExportObject(_methodCall.getPath());
                exportObject = null;
            }

            if (null == exportObject) {
                exportObject = fallbackContainer.get(_methodCall.getPath());
            }

            if (null == exportObject) {
                sendMessage(new Error(_methodCall,
                        new UnknownObject(_methodCall.getPath() + " is not an object provided by this process.")));
                return;
            }
            if (logger.isTraceEnabled()) {
                logger.trace("Searching for method {}  with signature {}", _methodCall.getName(), _methodCall.getSig());
                logger.trace("List of methods on {}: ", exportObject);
                for (MethodTuple mt : exportObject.getMethods().keySet()) {
                    logger.trace("   {} => {}", mt, exportObject.getMethods().get(mt));
                }
            }
            meth = exportObject.getMethods().get(new MethodTuple(_methodCall.getName(), _methodCall.getSig()));
            if (null == meth) {
                sendMessage(new Error(_methodCall, new UnknownMethod(String.format(
                        "The method `%s.%s' does not exist on this object.", _methodCall.getInterface(), _methodCall.getName()))));
                return;
            }
            o = exportObject.getObject().get();
        }

        if (ExportedObject.isExcluded(meth)) {
            sendMessage(new Error(_methodCall, new UnknownMethod(String.format(
                    "The method `%s.%s' is not exported.", _methodCall.getInterface(), _methodCall.getName()))));
            return;
        }

        // now execute it
        final Method me = meth;
        final Object ob = o;
        final boolean noreply = 1 == (_methodCall.getFlags() & Message.Flags.NO_REPLY_EXPECTED);
        final DBusCallInfo info = new DBusCallInfo(_methodCall);
        final AbstractConnection conn = this;

        logger.trace("Adding Runnable for method {}", meth);
        Runnable r = new Runnable() {

            @Override
            public void run() {
                logger.debug("Running method {} for remote call", me);

                try {
                    Type[] ts = me.getGenericParameterTypes();
                    _methodCall.setArgs(Marshalling.deSerializeParameters(_methodCall.getParameters(), ts, conn));
                    LoggingHelper.logIf(logger.isTraceEnabled(), () -> {
                        try {
                            logger.trace("Deserialised {} to types {}", Arrays.deepToString(_methodCall.getParameters()), Arrays.deepToString(ts));
                        } catch (Exception _ex) {
                            logger.trace("Error getting method call parameters", _ex);
                        }
                    });
                } catch (Exception _ex) {
                    logger.debug("", _ex);
                    handleException(_methodCall, new UnknownMethod("Failure in de-serializing message: " + _ex));
                    return;
                }

                try {
                    INFOMAP.put(Thread.currentThread(), info);
                    Object result;
                    try {
                        LoggingHelper.logIf(logger.isTraceEnabled(), () -> {
                            try {
                                logger.trace("Invoking Method: {} on {} with parameters {}", me, ob, Arrays.deepToString(_methodCall.getParameters()));
                            } catch (DBusException _ex) {
                                logger.trace("Error getting parameters from method call", _ex);
                            }
                        });

                        result = me.invoke(ob, _methodCall.getParameters());
                    } catch (InvocationTargetException _ex) {
                        logger.debug(_ex.getMessage(), _ex);
                        throw _ex.getCause();
                    }
                    INFOMAP.remove(Thread.currentThread());
                    if (!noreply) {
                        MethodReturn reply;
                        if (Void.TYPE.equals(me.getReturnType())) {
                            reply = new MethodReturn(_methodCall, null);
                        } else {
                            StringBuffer sb = new StringBuffer();
                            for (String s : Marshalling.getDBusType(me.getGenericReturnType())) {
                                sb.append(s);
                            }
                            Object[] nr = Marshalling.convertParameters(new Object[] {
                                    result
                            }, new Type[] {
                                    me.getGenericReturnType()
                            }, conn);

                            reply = new MethodReturn(_methodCall, sb.toString(), nr);
                        }
                        conn.sendMessage(reply);
                    }
                } catch (DBusExecutionException _ex) {
                    logger.debug("", _ex);
                    handleException(_methodCall, _ex);
                } catch (Throwable _ex) {
                    logger.debug("", _ex);
                    handleException(_methodCall,
                            new DBusExecutionException(String.format("Error Executing Method %s.%s: %s",
                                    _methodCall.getInterface(), _methodCall.getName(), _ex.getMessage())));
                }
            }
        };
        receivingService.execMethodCallHandler(r);
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
        logger.debug("Handling incoming signal: {}", _signal);

        List<DBusSigHandler<? extends DBusSignal>> handlers = new ArrayList<>();
        List<DBusSigHandler<DBusSignal>> genericHandlers = new ArrayList<>();

        for (Entry<DBusMatchRule, Queue<DBusSigHandler<? extends DBusSignal>>> e : getHandledSignals().entrySet()) {
            if (e.getKey().matches(_signal, false)) {
                handlers.addAll(e.getValue());
            }
        }

        for (Entry<DBusMatchRule, Queue<DBusSigHandler<DBusSignal>>> e : getGenericHandledSignals().entrySet()) {
            if (e.getKey().matches(_signal, false)) {
                genericHandlers.addAll(e.getValue());
            }
        }

        if (handlers.isEmpty() && genericHandlers.isEmpty()) {
            return;
        }

        final AbstractConnection conn = this;
        for (final DBusSigHandler<? extends DBusSignal> h : handlers) {
            logger.trace("Adding Runnable for signal {} with handler {}",  _signal, h);
            Runnable command = () -> {
                try {
                    DBusSignal rs;
                    if (_signal.getClass().equals(DBusSignal.class)) {
                        rs = _signal.createReal(conn);
                    } else {
                        rs = _signal;
                    }
                    if (rs == null) {
                        return;
                    }
                    ((DBusSigHandler<DBusSignal>) h).handle(rs);
                } catch (DBusException _ex) {
                    logger.warn("Exception while running signal handler '{}' for signal '{}':", h, _signal, _ex);
                    handleException(_signal, new DBusExecutionException("Error handling signal " + _signal.getInterface()
                            + "." + _signal.getName() + ": " + _ex.getMessage()));
                }
            };
            if (_useThreadPool) {
                receivingService.execSignalHandler(command);
            } else {
                command.run();
            }
        }

        for (final DBusSigHandler<DBusSignal> h : genericHandlers) {
            logger.trace("Adding Runnable for signal {} with handler {}",  _signal, h);
            Runnable command = () -> h.handle(_signal);
            if (_useThreadPool) {
                receivingService.execSignalHandler(command);
            } else {
                command.run();
            }
        }
    }

    private void handleMessage(final Error _err) {
        logger.debug("Handling incoming error: {}", _err);
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
            cbh = callbackManager.removeCallback(m);
            logger.trace("{} = pendingCallbacks.remove({})", cbh, m);

            // queue callback for execution
            if (null != cbh) {
                final CallbackHandler<?> fcbh = cbh;
                logger.trace("Adding Error Runnable with callback handler {}", fcbh);
                Runnable command = new Runnable() {

                    @Override
                    public synchronized void run() {
                        try {
                            logger.trace("Running Error Callback for {}", _err);
                            DBusCallInfo info = new DBusCallInfo(_err);
                            INFOMAP.put(Thread.currentThread(), info);

                            fcbh.handleError(_err.getException());
                            INFOMAP.remove(Thread.currentThread());

                        } catch (Exception _ex) {
                            logger.debug("Exception while running error callback.", _ex);
                        }
                    }
                };
                receivingService.execErrorHandler(command);
            }

        } else {
            getPendingErrorQueue().add(_err);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleMessage(final MethodReturn _mr) {
        logger.debug("Handling incoming method return: {}", _mr);
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
            CallbackHandler cbh = callbackManager.getCallback(m);
            DBusAsyncReply<?> asr = callbackManager.getCallbackReply(m);
            callbackManager.removeCallback(m);

            // queue callback for execution
            if (null != cbh) {
                final CallbackHandler<Object> fcbh = cbh;
                final DBusAsyncReply<?> fasr = asr;
                if (fasr == null) {
                    logger.debug("Cannot add runnable for method, given method callback was null");
                    return;
                }
                logger.trace("Adding Runnable for method {} with callback handler {}", fcbh, fasr.getMethod());
                Runnable r = new Runnable() {

                    @Override
                    public synchronized void run() {
                        try {
                            logger.trace("Running Callback for {}", _mr);
                            DBusCallInfo info = new DBusCallInfo(_mr);
                            INFOMAP.put(Thread.currentThread(), info);
                            Object convertRV = RemoteInvocationHandler.convertRV(_mr.getSig(), _mr.getParameters(),
                                    fasr.getMethod(), fasr.getConnection());
                            fcbh.handle(convertRV);
                            INFOMAP.remove(Thread.currentThread());

                        } catch (Exception _ex) {
                            logger.debug("Exception while running callback.", _ex);
                        }
                    }
                };
                receivingService.execMethodReturnHandler(r);
            }

        } else {
            try {
                sendMessage(new Error(_mr, new DBusExecutionException(
                        "Spurious reply. No message with the given serial id was awaiting a reply.")));
            } catch (DBusException _exDe) {
                logger.trace("Could not send error message", _exDe);
            }
        }
    }

    public void queueCallback(MethodCall _call, Method _method, CallbackHandler<?> _callback) {
        callbackManager.queueCallback(_call, _method, _callback, this);
    }

    /**
     * Send a message to DBus.
     * @param _message message to send
     */
    private void sendMessageInternally(Message _message) {
        try {
            if (!isConnected()) {
                throw new NotConnected("Disconnected");
            }
            if (_message instanceof DBusSignal) {
                ((DBusSignal) _message).appendbody(this);
            }

            if (_message instanceof MethodCall && 0 == (_message.getFlags() & Message.Flags.NO_REPLY_EXPECTED) && null != getPendingCalls()) {
                synchronized (getPendingCalls()) {
                    getPendingCalls().put(_message.getSerial(), (MethodCall) _message);
                }
            }

            transport.writeMessage(_message);

        } catch (Exception _ex) {
            logger.trace("Exception while sending message.", _ex);

            if (_message instanceof MethodCall && _ex instanceof DBusExecutionException) {
                try {
                    ((MethodCall) _message).setReply(new Error(_message, _ex));
                } catch (DBusException _exDe) {
                    logger.trace("Could not set message reply", _exDe);
                }
            } else if (_message instanceof MethodCall) {
                try {
                    logger.info("Setting reply to {} as an error", _message);
                    ((MethodCall) _message).setReply(
                            new Error(_message, new DBusExecutionException("Message Failed to Send: " + _ex.getMessage())));
                } catch (DBusException _exDe) {
                    logger.trace("Could not set message reply", _exDe);
                }
            } else if (_message instanceof MethodReturn) {
                try {
                    transport.writeMessage(new Error(_message, _ex));
                } catch (IOException | DBusException _exIo) {
                    logger.debug("Error writing method return to transport", _exIo);
                }
            }
            if (_ex instanceof IOException) {
                logger.debug("Fatal IOException while sending message, disconnecting", _ex);
                internalDisconnect((IOException) _ex);
            }
        }
    }

    Message readIncoming() throws DBusException {
        if (!isConnected()) {
            return null;
        }
        Message m = null;
        try {
            m = transport.readMessage();
        } catch (IOException _exIo) {
            if (_exIo instanceof EOFException || _exIo instanceof ClosedByInterruptException) {
                disconnectCallback.ifPresent(cb -> cb.clientDisconnect());
                if (disconnecting // when we are already disconnecting, ignore further errors
                        || transportBuilder.getAddress().isListeningSocket()) { // when we are listener, a client may disconnect any time which is no error
                    return null;
                }
            }

            if (isConnected()) {
                throw new FatalDBusException(_exIo);
            } // if run is false, suppress all exceptions - the connection either is already disconnected or should be disconnected right now
        }
        return m;
    }

    protected synchronized Map<String, ExportedObject> getExportedObjects() {
        return exportedObjects;
    }

    FallbackContainer getFallbackContainer() {
        return fallbackContainer;
    }

    /**
     * Returns a structure with information on the current method call.
     *
     * @return the DBusCallInfo for this method call, or null if we are not in a method call.
     */
    public static DBusCallInfo getCallInfo() {
        return INFOMAP.get(Thread.currentThread());
    }

    /**
     * Return any DBus error which has been received.
     *
     * @return A DBusExecutionException, or null if no error is pending.
     */
    public DBusExecutionException getError() {
        Error poll = getPendingErrorQueue().poll();
        if (poll != null) {
            return poll.getException();
        }
        return null;
    }

    /**
     * Returns the address this connection is connected to.
     *
     * @return new {@link BusAddress} object
     */
    public BusAddress getAddress() {
        return transportBuilder.getAddress();
    }

    public boolean isConnected() {
        return transport != null && transport.isConnected();
    }

    protected Queue<Error> getPendingErrorQueue() {
        return pendingErrorQueue;
    }

    protected Map<DBusMatchRule, Queue<DBusSigHandler<? extends DBusSignal>>> getHandledSignals() {
        return handledSignals;
    }

    protected Map<DBusMatchRule, Queue<DBusSigHandler<DBusSignal>>> getGenericHandledSignals() {
        return genericHandledSignals;
    }

    protected Map<Long, MethodCall> getPendingCalls() {
        return pendingCalls;
    }

    protected Map<DBusInterface, RemoteObject> getImportedObjects() {
        return importedObjects;
    }

    protected ObjectTree getObjectTree() {
        return objectTree;
    }

    /**
     * Set the endianness to use for all connections.
     * Defaults to the system architectures endianness.
     *
     * @param _b Message.Endian.BIG or Message.Endian.LITTLE
     */
    public static void setEndianness(byte _b) {
        if (_b == Message.Endian.BIG || _b == Message.Endian.LITTLE) {
            endianness = _b;
        }
    }

    /**
     * Get current endianness to use.
     * @return Message.Endian.BIG or Message.Endian.LITTLE
     */
    public static byte getEndianness() {
        return endianness; // TODO would be nice to have this non-static!
    }

    /**
     * Get the default system endianness.
     *
     * @return LITTLE or BIG
     */
    public static byte getSystemEndianness() {
       return ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)
                ? Message.Endian.BIG
                : Message.Endian.LITTLE;
    }

    /**
     * Returns the currently configured disconnect callback.
     *
     * @return callback or null if no callback registered
     */
    public IDisconnectCallback getDisconnectCallback() {
        return disconnectCallback.orElse(null);
    }

    /**
     * Set the callback which will be notified when a disconnection happens.
     * Use null to remove.
     *
     * @param _disconnectCallback callback to execute or null to remove
     */
    public void setDisconnectCallback(IDisconnectCallback _disconnectCallback) {
        disconnectCallback = Optional.ofNullable(_disconnectCallback);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[address=" + transportBuilder.getAddress() + "]";
    }

}
