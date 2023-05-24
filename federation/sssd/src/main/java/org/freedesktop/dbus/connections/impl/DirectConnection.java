package org.freedesktop.dbus.connections.impl;

import static org.freedesktop.dbus.utils.CommonRegexPattern.IFACE_PATTERN;
import static org.freedesktop.dbus.utils.CommonRegexPattern.PROXY_SPLIT_PATTERN;

import org.freedesktop.dbus.DBusMatchRule;
import org.freedesktop.dbus.RemoteInvocationHandler;
import org.freedesktop.dbus.RemoteObject;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfig;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.ExportedObject;
import org.freedesktop.dbus.utils.Hexdump;
import org.freedesktop.dbus.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Handles a peer to peer connection between two applications without a bus daemon.
 * <p>
 * Signal Handlers and method calls from remote objects are run in their own threads, you MUST handle the concurrency issues.
 * </p>
 */
public class DirectConnection extends AbstractConnection {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String machineId;

    /**
     * Create a direct connection to another application.
     * @param _address The address to connect to. This is a standard D-Bus address, except that the additional parameter 'listen=true' should be added in the application which is creating the socket.
     * @throws DBusException on error
     * @deprecated use {@link DirectConnectionBuilder}
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    public DirectConnection(String _address) throws DBusException {
        this(_address, AbstractConnection.TCP_CONNECT_TIMEOUT);
    }

    /**
    * Create a direct connection to another application.
    * @param _address The address to connect to. This is a standard D-Bus address, except that the additional parameter 'listen=true' should be added in the application which is creating the socket.
    * @param _timeout the timeout set for the underlying socket. 0 will block forever on the underlying socket.
    * @throws DBusException on error
    * @deprecated use {@link DirectConnectionBuilder}
    */
    @Deprecated(since = "4.1.0", forRemoval = true)
    public DirectConnection(String _address, int _timeout) throws DBusException {
        this(createTransportConfig(_address, _timeout), null);
    }

    DirectConnection(TransportConfig _transportCfg, ReceivingServiceConfig _rsCfg) throws DBusException {
        super(_transportCfg, _rsCfg);
        machineId = createMachineId();
        if (!getAddress().isServer()) {
            super.listen();
        }
    }

    @Deprecated(since = "4.2.0", forRemoval = true)
    static TransportConfig createTransportConfig(String _address, int _timeout) {
        TransportConfig cfg = new TransportConfig();
        cfg.setBusAddress(BusAddress.of(_address));
        cfg.getAdditionalConfig().put("TIMEOUT", _timeout);
        return cfg;
    }

    /**
     * Use this method when running on server side.
     * Call will block.
     */
    @Override
    public void listen() {
        if (getAddress().isServer()) {
            super.listen();
        }
    }

    private String createMachineId() {
        String ascii;

        try {
            ascii = Hexdump.toAscii(MessageDigest.getInstance("MD5").digest(InetAddress.getLocalHost().getHostName().getBytes()));
            return ascii;
        } catch (NoSuchAlgorithmException _ex) {
            logger.trace("MD5 algorithm not present", _ex);
        } catch (UnknownHostException _ex) {
            logger.trace("Unable to determine this machines hostname", _ex);
        }

        return Util.randomString(32);
    }

    @SuppressWarnings("unchecked")
    <T extends DBusInterface> T dynamicProxy(String _path, Class<T> _type) throws DBusException {
        try {
            Introspectable intro = getRemoteObject(_path, Introspectable.class);
            String data = intro.Introspect();

            String[] tags = PROXY_SPLIT_PATTERN.split(data);

            List<String> ifaces = Arrays.stream(tags).filter(t -> t.startsWith("interface"))
                .map(t -> IFACE_PATTERN.matcher(t).replaceAll("$1"))
                .collect(Collectors.toList());

            List<Class<?>> ifcs = findMatchingTypes(_type, ifaces);

            if (ifcs.isEmpty()) {
                throw new DBusException("Could not find an interface to cast to");
            }

            RemoteObject ro = new RemoteObject(null, _path, _type, false);
            DBusInterface newi = (DBusInterface) Proxy.newProxyInstance(ifcs.get(0).getClassLoader(), ifcs.toArray(new Class[0]), new RemoteInvocationHandler(this, ro));
            getImportedObjects().put(newi, ro);
            return (T) newi;
        } catch (Exception _ex) {
            logger.debug("", _ex);
            throw new DBusException(String.format("Failed to create proxy object for %s; reason: %s.", _path, _ex.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    <T extends DBusInterface> T getExportedObject(String _path, Class<T> _type) throws DBusException {
        ExportedObject o = null;
        synchronized (getExportedObjects()) {
            o = getExportedObjects().get(_path);
        }
        if (null != o && null == o.getObject().get()) {
            unExportObject(_path);
            o = null;
        }
        if (null != o) {
            return (T) o.getObject().get();
        }
        return dynamicProxy(_path, _type);
    }

    /**
       * Return a reference to a remote object.
       * This method will always refer to the well known name (if given) rather than resolving it to a unique bus name.
       * In particular this means that if a process providing the well known name disappears and is taken over by another process
       * proxy objects gained by this method will make calls on the new proccess.
       *
       * This method will use bus introspection to determine the interfaces on a remote object and so
       * <b>may block</b> and <b>may fail</b>. The resulting proxy object will, however, be castable
       * to any interface it implements. It will also autostart the process if applicable. Also note
       * that the resulting proxy may fail to execute the correct method with overloaded methods
       * and that complex types may fail in interesting ways. Basically, if something odd happens,
       * try specifying the interface explicitly.
       *
       * @param _objectPath The path on which the process is exporting the object.
       * @return A reference to a remote object.
       * @throws ClassCastException If type is not a sub-type of DBusInterface
       * @throws DBusException If busname or objectpath are incorrectly formatted.
    */
    public DBusInterface getRemoteObject(String _objectPath) throws DBusException {
        if (null == _objectPath) {
            throw new DBusException("Invalid object path: null");
        }

        if (_objectPath.length() > MAX_NAME_LENGTH || !OBJECT_REGEX_PATTERN.matcher(_objectPath).matches()) {
            throw new DBusException("Invalid object path: " + _objectPath);
        }

        return dynamicProxy(_objectPath, null);
    }

    /**
       * Return a reference to a remote object.
       * This method will always refer to the well known name (if given) rather than resolving it to a unique bus name.
       * In particular this means that if a process providing the well known name disappears and is taken over by another process
       * proxy objects gained by this method will make calls on the new proccess.
       * @param _objectPath The path on which the process is exporting the object.
       * @param _type The interface they are exporting it on. This type must have the same full class name and exposed method signatures
       * as the interface the remote object is exporting.
       * @param <T> class which extends DBusInterface
       * @return A reference to a remote object.
       * @throws ClassCastException If type is not a sub-type of DBusInterface
       * @throws DBusException If busname or objectpath are incorrectly formatted or type is not in a package.
    */
    public <T extends DBusInterface> T getRemoteObject(String _objectPath, Class<T> _type) throws DBusException {
        if (null == _objectPath) {
            throw new DBusException("Invalid object path: null");
        }
        if (null == _type) {
            throw new ClassCastException("Not A DBus Interface");
        }

        if (_objectPath.length() > MAX_NAME_LENGTH || !OBJECT_REGEX_PATTERN.matcher(_objectPath).matches()) {
            throw new DBusException("Invalid object path: " + _objectPath);
        }

        if (!DBusInterface.class.isAssignableFrom(_type)) {
            throw new ClassCastException("Not A DBus Interface");
        }

        // don't let people import things which don't have a
        // valid D-Bus interface name
        if (_type.getName().equals(_type.getSimpleName())) {
            throw new DBusException("DBusInterfaces cannot be declared outside a package");
        }

        RemoteObject ro = new RemoteObject(null, _objectPath, _type, false);

        @SuppressWarnings("unchecked")
        T i = (T) Proxy.newProxyInstance(_type.getClassLoader(),
                new Class[] {_type}, new RemoteInvocationHandler(this, ro));

        getImportedObjects().put(i, ro);

        return i;
    }

    @Override
    protected <T extends DBusSignal> void removeSigHandler(DBusMatchRule _rule, DBusSigHandler<T> _handler) throws DBusException {
        Queue<DBusSigHandler<? extends DBusSignal>> v = getHandledSignals().get(_rule);
        if (null != v) {
            v.remove(_handler);
            if (0 == v.size()) {
                getHandledSignals().remove(_rule);
            }
        }
    }

    @Override
    protected <T extends DBusSignal> AutoCloseable addSigHandler(DBusMatchRule _rule, DBusSigHandler<T> _handler) throws DBusException {
        Queue<DBusSigHandler<? extends DBusSignal>> v =
                getHandledSignals().computeIfAbsent(_rule, val -> {
                    Queue<DBusSigHandler<? extends DBusSignal>> l = new ConcurrentLinkedQueue<>();
                    return l;
                });

        v.add(_handler);
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                removeSigHandler(_rule, _handler);
            }
        };
    }

    @Override
    protected void removeGenericSigHandler(DBusMatchRule _rule, DBusSigHandler<DBusSignal> _handler) throws DBusException {
        Queue<DBusSigHandler<DBusSignal>> v = getGenericHandledSignals().get(_rule);
        if (null != v) {
            v.remove(_handler);
            if (0 == v.size()) {
                getGenericHandledSignals().remove(_rule);
            }
        }
    }

    @Override
    protected AutoCloseable addGenericSigHandler(DBusMatchRule _rule, DBusSigHandler<DBusSignal> _handler) throws DBusException {
        Queue<DBusSigHandler<DBusSignal>> v =
                getGenericHandledSignals().computeIfAbsent(_rule, val -> {
                    Queue<DBusSigHandler<DBusSignal>> l = new ConcurrentLinkedQueue<>();
                    return l;
                });

        v.add(_handler);
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                removeGenericSigHandler(_rule, _handler);
            }
        };
    }

    @Override
    public <T extends DBusInterface> T getExportedObject(String _source, String _path, Class<T> _type) throws DBusException {
        return getExportedObject(_path, _type);
    }

    @Override
    public String getMachineId() {
       return machineId;
    }

    @Override
    public DBusInterface getExportedObject(String _source, String _path) throws DBusException {
        return getExportedObject(_path, (Class<DBusInterface>) null);
    }
}
