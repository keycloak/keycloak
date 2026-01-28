package org.freedesktop.dbus.connections.impl;

import java.nio.ByteOrder;

import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.IDisconnectCallback;
import org.freedesktop.dbus.connections.ReceivingService;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfig;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfigBuilder;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.connections.config.TransportConfigBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.Message.Endian;

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

    private boolean                                weakReference = false;
    private byte                                   endianess     = getSystemEndianness();

    private IDisconnectCallback                    disconnectCallback;

    private final ReceivingServiceConfigBuilder<R> rsConfigBuilder;

    private final TransportConfigBuilder<?, R>     transportConfigBuilder;

    protected BaseConnectionBuilder(Class<R> _returnType, BusAddress _address) {
        returnType = _returnType;
        rsConfigBuilder = new ReceivingServiceConfigBuilder<>(() -> self());
        transportConfigBuilder = new TransportConfigBuilder<>(() -> self());
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

    protected boolean isWeakReference() {
        return weakReference;
    }

    protected byte getEndianess() {
        return endianess;
    }

    protected IDisconnectCallback getDisconnectCallback() {
        return disconnectCallback;
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
     * Set the size of the thread-pool used to handle signals from the bus.
     * Caution: Using thread-pool size &gt; 1 may cause signals to be handled out-of-order
     * <p>
     * Default: 1
     *
     * @param _threads int &gt;= 1
     * @return this
     * @deprecated use receivingThreadConfig().withSignalThreadCount(_threads)
     */
    @Deprecated(since = "4.2.0", forRemoval = true)
    public R withSignalThreadCount(int _threads) {
        receivingThreadConfig().withSignalThreadCount(_threads);
        return self();
    }

    /**
     * Set the size of the thread-pool used to handle error messages received on the bus.
     * <p>
     * Default: 1
     *
     * @param _threads int &gt;= 1
     * @return this
     * @deprecated use receivingThreadConfig().withErrorHandlerThreadCount(_threads)
     */
    @Deprecated(since = "4.2.0", forRemoval = true)
    public R withErrorHandlerThreadCount(int _threads) {
        receivingThreadConfig().withErrorHandlerThreadCount(_threads);
        return self();
    }

    /**
     * Set the size of the thread-pool used to handle methods calls previously sent to the bus.
     * The thread pool size has to be &gt; 1 to handle recursive calls.
     * <p>
     * Default: 4
     *
     * @param _threads int &gt;= 1
     * @return this
     * @deprecated use receivingThreadConfig().withMethodCallThreadCount(_threads)
     */
    @Deprecated(since = "4.2.0", forRemoval = true)
    public R withMethodCallThreadCount(int _threads) {
        receivingThreadConfig().withMethodCallThreadCount(_threads);
        return self();
    }

    /**
     * Set the size of the thread-pool used to handle method return values received on the bus.
     * <p>
     * Default: 1
     *
     * @param _threads int &gt;= 1
     * @return this
     * @deprecated use receivingThreadConfig().withMethodReturnThreadCount(_threads)
     */
    @Deprecated(since = "4.2.0", forRemoval = true)
    public R withMethodReturnThreadCount(int _threads) {
        receivingThreadConfig().withMethodReturnThreadCount(_threads);
        return self();
    }

    /**
     * Set the endianness for the connection
     * Default is based on system endianness.
     *
     * @param _endianess {@link Endian#BIG} or {@value Endian#LITTLE}
     * @return this
     */
    public R withEndianess(byte _endianess) {
        if (_endianess == Endian.BIG || _endianess == Endian.LITTLE) {
            endianess = _endianess;
        }
        return self();
    }

    /**
     * Enable/Disable weak references on connection.
     * Default is false.
     *
     * @param _weakRef true to enable
     * @return this
     */
    public R withWeakReferences(boolean _weakRef) {
        weakReference = _weakRef;
        return self();
    }

    /**
     * Set the given disconnect callback to the created connection.
     *
     * @param _disconnectCallback callback
     * @return this
     */
    public R withDisconnectCallback(IDisconnectCallback _disconnectCallback) {
        disconnectCallback = _disconnectCallback;
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
                ? Message.Endian.BIG
                : Message.Endian.LITTLE;
    }
}
