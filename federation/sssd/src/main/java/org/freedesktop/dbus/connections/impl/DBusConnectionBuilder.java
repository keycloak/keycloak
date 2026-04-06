package org.freedesktop.dbus.connections.impl;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfig;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.AddressResolvingException;
import org.freedesktop.dbus.exceptions.DBusException;
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
        BusAddress address = validateTransportAddress(AddressBuilder.getSessionConnection(_machineIdFileLocation));
        return new DBusConnectionBuilder(address, getDbusMachineId(_machineIdFileLocation));
    }

    /**
     * Create new default connection to the DBus system bus.
     *
     * @return {@link DBusConnectionBuilder}
     */
    public static DBusConnectionBuilder forSystemBus() {
        BusAddress address = validateTransportAddress(AddressBuilder.getSystemConnection());
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
        return new DBusConnectionBuilder(BusAddress.of(_address), getDbusMachineId(null));
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
        return new DBusConnectionBuilder(_address, getDbusMachineId(null));
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
        ReceivingServiceConfig rcvSvcCfg = buildThreadConfig();
        TransportConfig transportCfg = buildTransportConfig();
        ConnectionConfig connectionConfig = getConnectionConfig();
        DBusConnection c;
        if (shared) {
            synchronized (DBusConnection.CONNECTIONS) {
                String busAddressStr = transportCfg.getBusAddress().toString();
                c = getSharedConnection(busAddressStr);
                if (c != null) {
                    c.concurrentConnections.incrementAndGet();
                    return c; // this connection already exists, do not change anything
                } else {
                    c = new DBusConnection(shared, machineId, connectionConfig, transportCfg, rcvSvcCfg);
                    DBusConnection.CONNECTIONS.put(busAddressStr, c);
                }
            }
        } else {
            c = new DBusConnection(shared, machineId, connectionConfig, transportCfg, rcvSvcCfg);
        }

        c.connectImpl();
        return c;
    }

    /**
     * Retrieve a existing shared connection.
     * Will remove existing shared connections when underlying transport is disconnected.
     * @param _busAddr bus address
     * @return connection if a valid shared connection found or
     *      null if no connection found or found connection was invalid
     */
    private DBusConnection getSharedConnection(String _busAddr) {
        synchronized (DBusConnection.CONNECTIONS) {
            DBusConnection c = DBusConnection.CONNECTIONS.get(_busAddr);
            if (c != null) {
                if (!c.isConnected()) {
                    DBusConnection.CONNECTIONS.remove(_busAddr);
                    return null;
                } else {
                    return c;
                }
            }
        }
        return null;
    }
}
