package org.freedesktop.dbus.connections.impl;

import java.nio.ByteOrder;
import java.util.function.Consumer;

import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.IDisconnectCallback;
import org.freedesktop.dbus.connections.base.ReceivingService;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfig;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfigBuilder;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.connections.config.TransportConfigBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.constants.Endian;

/**
 * Base class for connection builders containing commonly used options.
 *
 * @author hypfvieh
 * @since 4.2.0 - 2022-07-13
 *
 * @param <R> concrete type of connection builder
 */
public abstract class BaseConnectionBuilder<R extends BaseConnectionBuilder<R, C>, C extends AbstractConnection> {

    private final Class<R>                         returnType;

    private final ReceivingServiceConfigBuilder<R> rsConfigBuilder;

    private final TransportConfigBuilder<?, R>     transportConfigBuilder;

    private final ConnectionConfig                 connectionConfig;

    protected BaseConnectionBuilder(Class<R> _returnType, BusAddress _address) {
        returnType = _returnType;
        connectionConfig = new ConnectionConfig();
        rsConfigBuilder = new ReceivingServiceConfigBuilder<>(this::self);
        transportConfigBuilder = new TransportConfigBuilder<>(this::self);
        transportConfigBuilder.withBusAddress(_address);
    }

    /**
     * Return ourselves.
     * @return concrete version of this
     */
    R self() {
        return returnType.cast(this);
    }

    /**
     * Creates the configuration to use for {@link ReceivingService}.
     *
     * @return config
     */
    protected ReceivingServiceConfig buildThreadConfig() {
        return rsConfigBuilder.build();
    }

    /**
     * Creates the configuration to use for {@link TransportBuilder}.
     *
     * @return config
     */
    protected TransportConfig buildTransportConfig() {
        return transportConfigBuilder.build();
    }

    /**
     * Returns the currently configured connection configuration.
     * @return config
     */
    protected ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    /**
     * Returns the builder to configure the receiving thread pools.
     * @return builder
     */
    public ReceivingServiceConfigBuilder<R> receivingThreadConfig() {
        return rsConfigBuilder;
    }

    /**
     * Returns the builder to configure the used transport.
     * @return builder
     */
    public TransportConfigBuilder<?, R> transportConfig() {
        return transportConfigBuilder;
    }

    /**
     * Enable/Disable weak references on connection.
     * Default is false.
     *
     * @param _weakRef true to enable
     * @return this
     * @deprecated use {@link #withExportWeakReferences(boolean)} instead
     */
    @Deprecated(forRemoval = true, since = "5.1.0 - 2024-07-12")
    public R withWeakReferences(boolean _weakRef) {
        return withExportWeakReferences(_weakRef);
    }

    /**
     * Enable/Disable usage of weak references for exported objects.
     * <p>
     * Exported objects are objects provided by the application to
     * DBus and are used through DBus by other applications.
     * </p>
     * <p>
     * Using weak references may allow the Garbage Collector to remove exported objects
     * when they are no longer reachable.
     * Enabling this feature may cause dbus-java to be forced to re-create exported objects
     * because the GC already cleaned up the old references.
     * <br>
     * <b>Use with caution!</b>
     * </p>
     *
     * Default is false.
     *
     * @param _weakRef true to enable
     * @return this
     */
    public R withExportWeakReferences(boolean _weakRef) {
        connectionConfig.setExportWeakReferences(_weakRef);
        return self();
    }

    /**
     * Enable/Disable usage of weak references for imported objects.
     * <p>
     * Imported objects are all objects which are created when calling interfaces which belong to any object provided by DBus.<br>
     * E.g. when you want to use Bluetooth, you will query the bluez interfaces on the bus which will create a proxy object in dbus-java.<br>
     * These objects are stored in an internal Map so re-querying the same object will return the cached object instead of creating a new proxy.
     * </p>
     * <p>
     * Usually all imported objects are dropped when the connection gets closed (by either side).<br>
     * If the application will not close the connection and run for a long time the default behavior may cause high memory usage.<br>
     * Enabling weak references may allow the Garbage Collector to remove imported objects when they are no longer reachable.<br>
     * This will free up memory when no other references are kept on the remote imported object.
     * </p>
     * <p>
     * The current default is false.<br>
     * Anyway it is considered to enable weak references for imported objects as default in the future.
     * </p>
     *
     * @param _weakRef true to enable
     * @return this
     */
    public R withImportWeakReferences(boolean _weakRef) {
        connectionConfig.setImportWeakReferences(_weakRef);
        return self();
    }

    /**
     * Set the given disconnect callback to the created connection.
     *
     * @param _disconnectCallback callback
     * @return this
     */
    public R withDisconnectCallback(IDisconnectCallback _disconnectCallback) {
        connectionConfig.setDisconnectCallback(_disconnectCallback);
        return self();
    }

    /**
     * Configures a consumer which will receive any signal which could not be handled.
     * <p>
     * By default, no handler is configured, so unknown/unhandled signals will be
     * ignored and only be logged as warn message.
     * </p>
     * @param _handler callback which receives all unknown signals
     * @return this
     * @since 5.1.1 - 2024-09-11
     */
    public R withUnknownSignalHandler(Consumer<DBusSignal> _handler) {
        connectionConfig.setUnknownSignalHandler(_handler);
        return self();
    }

    public abstract C build() throws DBusException;

    /**
     * Get the default system endianness.
     *
     * @return LITTLE or BIG
     */
    public static byte getSystemEndianness() {
       return ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)
                ? Endian.BIG
                : Endian.LITTLE;
    }
}
