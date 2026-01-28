package org.freedesktop.dbus.connections.impl;

import java.nio.ByteOrder;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfig;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.AddressResolvingException;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.utils.AddressBuilder;

import static org.freedesktop.dbus.utils.AddressBuilder.getDbusMachineId;

/**
 * Builder to create a new DBusConnection.
 *
 * @author hypfvieh
 * @version 4.1.0 - 2022-02-04
 */
public final class DBusConnectionBuilder extends BaseConnectionBuilder<DBusConnectionBuilder, DBusConnection> {

    private final String        machineId;
    private boolean             registerSelf            = true;
    private boolean             shared                  = true;

    private DBusConnectionBuilder(BusAddress _address, String _machineId) {
        super(DBusConnectionBuilder.class, _address);
        machineId = _machineId;
    }

    /**
     * Create a new default connection connecting to DBus session bus but use an alternative input for the machineID.
     *
     * @param _machineIdFileLocation file with machine ID
     *
     * @return {@link DBusConnectionBuilder}
     */
    public static DBusConnectionBuilder forSessionBus(String _machineIdFileLocation) {
        BusAddress address = AddressBuilder.getSessionConnection(_machineIdFileLocation);
        address = validateTransportAddress(address);
        DBusConnectionBuilder instance = new DBusConnectionBuilder(address, getDbusMachineId(_machineIdFileLocation));
        return instance;
    }

    /**
     * Create new default connection to the DBus system bus.
     *
     * @return {@link DBusConnectionBuilder}
     */
    public static DBusConnectionBuilder forSystemBus() {
        BusAddress address = AddressBuilder.getSystemConnection();
        address = validateTransportAddress(address);
        return new DBusConnectionBuilder(address, getDbusMachineId(null));
    }

    /**
     * Create a new default connection connecting to the DBus session bus.
     *
     * @return {@link DBusConnectionBuilder}
     */
    public static DBusConnectionBuilder forSessionBus() {
        return forSessionBus(null);
    }

    /**
     * Create a default connection to DBus using the given bus type.
     *
     * @param _type bus type
     *
     * @return this
     */
    public static DBusConnectionBuilder forType(DBusBusType _type) {
        return forType(_type, null);
    }

    /**
     * Create a default connection to DBus using the given bus type and machineIdFile.
     *
     * @param _type bus type
     * @param _machineIdFile machineId file
     *
     * @return this
     */
    public static DBusConnectionBuilder forType(DBusBusType _type, String _machineIdFile) {
        if (_type == DBusBusType.SESSION) {
            return forSessionBus(_machineIdFile);
        } else if (_type == DBusBusType.SYSTEM) {
            return forSystemBus();
        }

        throw new IllegalArgumentException("Unknown bus type: " + _type);
    }

    /**
     * Use the given address to create the connection (e.g. used for remote TCP connected DBus daemons).
     *
     * @param _address address to use
     * @return this
     */
    public static DBusConnectionBuilder forAddress(String _address) {
        DBusConnectionBuilder instance = new DBusConnectionBuilder(BusAddress.of(_address), getDbusMachineId(null));
        return instance;
    }

    /**
     * Use the given address to create the connection (e.g. used for remote TCP connected DBus daemons).
     *
     * @param _address address to use
     * @return this
     *
     * @since 4.2.0 - 2022-07-18
     */
    public static DBusConnectionBuilder forAddress(BusAddress _address) {
        DBusConnectionBuilder instance = new DBusConnectionBuilder(_address, getDbusMachineId(null));
        return instance;
    }

    /**
     * Checks if the given address can be used with the available transports.
     * Will fallback to TCP if no address given and TCP transport is available.
     *
     * @param _address address to check
     * @return address, maybe fallback address
     */
    private static BusAddress validateTransportAddress(BusAddress _address) {
        if (TransportBuilder.getRegisteredBusTypes().isEmpty()) {
            throw new IllegalArgumentException("No transports found to connect to DBus. Please add at least one transport provider to your classpath");
        }

        BusAddress address = _address;

        // no unix transport but address wants to use a unix socket
        if (!TransportBuilder.getRegisteredBusTypes().contains("UNIX")
                && address != null
                && address.isBusType("UNIX")) {
            throw new AddressResolvingException("No transports found to handle UNIX socket connections. Please add a unix-socket transport provider to your classpath");
        }

        // no tcp transport but TCP address given
        if (!TransportBuilder.getRegisteredBusTypes().contains("TCP")
                && address != null
                && address.isBusType("TCP")) {
            throw new AddressResolvingException("No transports found to handle TCP connections. Please add a TCP transport provider to your classpath");
        }

        return address;

    }
    /**
     * Register the new connection on DBus using 'hello' message. Default is true.
     *
     * @param _register boolean
     * @return this
     */
    public DBusConnectionBuilder withRegisterSelf(boolean _register) {
        registerSelf = _register;
        return this;
    }

    /**
     * Use this connection as shared connection. Shared connection means that the same connection is used multiple times
     * if the connection parameter did not change. Default is true.
     *
     * @param _shared boolean
     * @return this
     */
    public DBusConnectionBuilder withShared(boolean _shared) {
        shared = _shared;
        return this;
    }

    /**
     * Create the new {@link DBusConnection}.
     *
     * @return {@link DBusConnection}
     * @throws DBusException when DBusConnection could not be opened
     */
    @Override
    public DBusConnection build() throws DBusException {
        ReceivingServiceConfig cfg = buildThreadConfig();
        TransportConfig transportCfg = buildTransportConfig();

        DBusConnection c;
        if (shared) {
            synchronized (DBusConnection.CONNECTIONS) {
                String busAddressStr = transportCfg.getBusAddress().toString();
                c = DBusConnection.CONNECTIONS.get(busAddressStr);
                if (c != null) {
                    c.concurrentConnections.incrementAndGet();
                    return c; // this connection already exists, do not change anything
                } else {
                    c = new DBusConnection(shared, machineId, transportCfg, cfg);
                    DBusConnection.CONNECTIONS.put(busAddressStr, c);
                }
            }
        } else {
            c = new DBusConnection(shared, machineId, transportCfg, cfg);
        }

        c.setDisconnectCallback(getDisconnectCallback());
        c.setWeakReferences(isWeakReference());
        DBusConnection.setEndianness(getEndianess());
        c.connect(registerSelf);
        return c;
    }

    /**
     * Get the default system endianness.
     *
     * @return LITTLE or BIG
     * @deprecated if required, use {@link BaseConnectionBuilder#getSystemEndianness()}
     */
    @Deprecated(forRemoval = true, since = "4.2.0")
    public static byte getSystemEndianness() {
       return ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)
                ? Message.Endian.BIG
                : Message.Endian.LITTLE;
    }
}
