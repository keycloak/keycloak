package org.freedesktop.dbus.connections.impl;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfig;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.Message;

import java.nio.ByteOrder;

/**
 * Builder to create a new DirectConnection.
 *
 * @author hypfvieh
 * @version 4.1.0 - 2022-02-04
 */
public final class DirectConnectionBuilder extends BaseConnectionBuilder<DirectConnectionBuilder, DirectConnection> {

    private DirectConnectionBuilder(BusAddress _address) {
        super(DirectConnectionBuilder.class, _address);
    }

    /**
     * Use the given address to create the connection (e.g. used for remote TCP connected DBus daemons).
     *
     * @param _address address to use
     * @return this
     */
    public static DirectConnectionBuilder forAddress(String _address) {
        DirectConnectionBuilder instance = new DirectConnectionBuilder(BusAddress.of(_address));
        return instance;
    }

    /**
     * Create the new {@link DBusConnection}.
     *
     * @return {@link DBusConnection}
     * @throws DBusException when DBusConnection could not be opened
     */
    @Override
    public DirectConnection build() throws DBusException {
        ReceivingServiceConfig rsCfg = buildThreadConfig();
        TransportConfig transportCfg = buildTransportConfig();

        DirectConnection c = new DirectConnection(transportCfg, rsCfg);
        c.setDisconnectCallback(getDisconnectCallback());
        c.setWeakReferences(isWeakReference());
        DirectConnection.setEndianness(getEndianess());
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
