package org.freedesktop.dbus.connections.config;

import java.nio.file.attribute.PosixFilePermission;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.messages.constants.Endian;
import org.freedesktop.dbus.spi.transport.ITransportProvider;

public class TransportConfigBuilder<X extends TransportConfigBuilder<?, R>, R> {
    private final Supplier<R>          connectionBuilder;

    private TransportConfig   config = new TransportConfig();

    private final SaslConfigBuilder<R> saslConfigBuilder;

    public TransportConfigBuilder(Supplier<R> _sup) {
        connectionBuilder = _sup;
        saslConfigBuilder = new SaslConfigBuilder<>(this);
    }

    /**
     * Return ourselves.
     * @return concrete version of this
     */
    @SuppressWarnings("unchecked")
    X self() {
        return (X) this;
    }

    /**
     * Use the predefined TransportConfig.
     * <p>
     * Using this will override any previous configuration and replaces
     * the internal configuration object with the given instance.
     * </p>
     * @param _config configuration, never null
     *
     * @return this
     */
    public X withConfig(TransportConfig _config) {
        config = Objects.requireNonNull(_config, "TransportConfig required");
        saslConfigBuilder.withConfig(_config.getSaslConfig());
        return self();
    }

    /**
     * Set the {@link BusAddress} which should be used for the connection.
     *
     * @param _address address to use
     *
     * @return this
     */
    public X withBusAddress(BusAddress _address) {
        config.setBusAddress(Objects.requireNonNull(_address, "BusAddress required"));
        return self();
    }

    /**
     * Returns the currently configured BusAddress.
     *
     * @return BusAddress, maybe null
     */
    public BusAddress getBusAddress() {
        return config.getBusAddress();
    }

    /**
     * Set a callback which will be called right before the connection to the transport is established.
     * <p>
     * The given consumer will receive the created {@link AbstractTransport} object which is not yet
     * connected. A callback should <b>NEVER</b> connect the transport, but is allowed to do further
     * configuration if needed.
     * </p>
     *
     * @param _callback consumer to call, null to remove any callback
     *
     * @return this
     */
    public X withPreConnectCallback(Consumer<AbstractTransport> _callback) {
        config.setPreConnectCallback(_callback);
        return self();
    }

    /**
     * Set a callback which will be called after {@code bindImpl()} on a server connection was called.<br>
     * This method is only called if the transport is configured as server connection.
     * <p>
     * The given consumer will receive the created {@link AbstractTransport} object which is not yet
     * accepting connections. A callback should <b>NEVER</b> call accept on the transport, but is allowed to do further
     * configuration if needed.
     * </p>
     *
     * @param _callback consumer to call, null to remove any callback
     *
     * @return this
     * @since 5.0.0 - 2023-10-20
     */
    public X withAfterBindCallback(Consumer<AbstractTransport> _callback) {
        config.setAfterBindCallback(_callback);
        return self();
    }

    /**
     * Instantly connect to DBus when {@link #build()} is called.
     * <p>
     * This option will be ignored when this is a listening (server) socket.
     * </p>
     * <p>
     * default: true
     * </p>
     *
     * @param _connect boolean
     *
     * @return this
     */
    public X withAutoConnect(boolean _connect) {
        config.setAutoConnect(_connect);
        return self();
    }

    /**
     * Register the new connection on DBus using 'hello' message. Default is true.
     *
     * @param _register boolean
     * @return this
     *
     * @since 5.0.0 - 2023-10-11
     */
    public X withRegisterSelf(boolean _register) {
        config.setRegisterSelf(_register);
        return self();
    }

    /**
     * Switch to the {@link SaslConfigBuilder} to configure the SASL authentication mechanism.<br>
     * Use {@link SaslConfigBuilder#back()} to return to this builder when finished.
     *
     * @return SaslConfigBuilder
     */
    public SaslConfigBuilder<R> configureSasl() {
        return saslConfigBuilder;
    }

    /**
     * Use true to use the transport as listener (server).
     *
     * @param _listen true to be a listening connection
     *
     * @return this
     */
    public X withListening(boolean _listen) {
        config.setListening(_listen);
        return self();
    }

    /**
     * Setup a timeout for the transport.
     * <p>
     * This option might not be used by every transport
     * (e.g. unix sockets do not support a timeout).
     * Timeout cannot be less than zero.
     * </p>
     *
     * @param _timeout true to be a listening connection
     *
     * @return this
     */
    public X withTimeout(int _timeout) {
        if (_timeout >= 0) {
            config.setTimeout(_timeout);
        }
        return self();
    }

    /**
     * The owner of the socket file if a unix socket is used and this is a server transport.
     * <p>
     * Default is the user of the running JVM process.<br><br>
     * <b>Please note:</b><br>
     * The running process user has to have suitable permissions to change the owner
     * of the file. Otherwise the file owner will not be changed!
     *
     * @param _user user to set, if null is given JVM process user is used
     *
     * @return this
     */
    public X withUnixSocketFileOwner(String _user) {
        config.setFileOwner(_user);
        return self();
    }

    /**
     * The group of the socket file if a unix socket is used and this is a server transport.
     * <p>
     * Default is the group of the running JVM process.<br><br>
     * <b>Please note:</b><br>
     * The running process user has to have suitable permissions to change the group
     * of the file. Otherwise the file group will not be changed!
     *
     * @param _group group to set, if null is given JVM process group is used
     *
     * @return this
     */
    public X withUnixSocketFileGroup(String _group) {
        config.setFileGroup(_group);
        return self();
    }

    /**
     * The permissions which will be set on socket file if a unix socket is used and this is a server transport.
     * <p>
     * This method does nothing when used on windows systems.
     * <b>Please note:</b><br>
     * The running process user has to have suitable permissions to change the permissions
     * of the file. Otherwise the file permissions will not be changed!
     *
     * @param _permissions permissions to set, if null is given default permissions will be used
     *
     * @return this
     */
    public X withUnixSocketFilePermissions(PosixFilePermission... _permissions) {
        config.setFileUnixPermissions(_permissions);
        return self();
    }

    /**
     * Adds an additional config key to the transport config.<br>
     * Will overwrite value if key exists.
     *
     * @param _key key to use
     * @param _value value to use
     *
     * @return this
     */
    public X withAdditionalConfig(String _key, Object _value) {
        config.getAdditionalConfig().put(_key, _value);
        return self();
    }

    /**
     * Configure parent class loader used for {@link ServiceLoader} to find {@link ITransportProvider} implementations.<br>
     * If {@link #withServiceLoaderModuleLayer(ModuleLayer)} is also configured, {@link ModuleLayer} will take precedence.
     * <br><br>
     * Defaults to {@code TransportBuilder.class.getClassLoader()}.
     * @param _ldr class loader
     *
     * @return this
     */
    public X withServiceLoaderClassLoader(ClassLoader _ldr) {
        config.setServiceLoaderClassLoader(_ldr);
        return self();
    }

    /**
     * Configure {@link ModuleLayer} used for {@link ServiceLoader} to find {@link ITransportProvider} implementations.
     *
     * @param _layer module layer
     *
     * @return this
     */
    public X withServiceLoaderModuleLayer(ModuleLayer _layer) {
        config.setServiceLoaderModuleLayer(_layer);
        return self();
    }

    /**
     * Removes an additional config key to of transport config.<br>
     * Will do nothing if key does not exist.
     *
     * @param _key key to remove
     *
     * @return this
     */
    public X withRemoveAdditionalConfig(String _key) {
        config.getAdditionalConfig().remove(_key);
        return self();
    }

    /**
     * Set the endianess for the connection Default is based on system endianess.
     *
     * @param _endianess {@link Endian#BIG} or {@value Endian#LITTLE}
     * @return this
     */
    public X withEndianess(byte _endianess) {
        if (_endianess == Endian.BIG || _endianess == Endian.LITTLE) {
            config.setEndianess(_endianess);
        }
        return self();
    }

    /**
     * Return to the previous builder.
     * <p>
     * This allows you to return from the this builder to the builder which started this builder so you can continue
     * using the previous builder.
     * </p>
     *
     * @return previous builder, maybe null
     */
    public R back() {
        return connectionBuilder != null ? connectionBuilder.get() : null;
    }

    /**
     * Returns the transport config.
     *
     * @return config
     */
    public TransportConfig build() {
        SaslConfig saslCfg = saslConfigBuilder.build();
        config.setSaslConfig(saslCfg);
        return config;
    }

}
