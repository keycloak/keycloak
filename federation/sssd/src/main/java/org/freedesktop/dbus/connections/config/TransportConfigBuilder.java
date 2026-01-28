package org.freedesktop.dbus.connections.config;

import java.nio.file.attribute.PosixFilePermission;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.connections.transports.TransportBuilder.SaslAuthMode;

public class TransportConfigBuilder<X extends TransportConfigBuilder<?, R>, R> {
    private final Supplier<R> connectionBuilder;

    private TransportConfig   config = new TransportConfig();

    private final SaslConfigBuilder<R> saslConfigBuilder;

    public TransportConfigBuilder(Supplier<R> _sup) {
        connectionBuilder = _sup;
        saslConfigBuilder = new SaslConfigBuilder<>(config.getSaslConfig(), () -> this);
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
        Objects.requireNonNull(_config, "TransportConfig required");
        config = _config;
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
     * Instantly connect to DBus when {@link #build()} is called.
     * <p>
     * default: true
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
     * Set a different SASL authentication mode.
     * <p>
     * Usually when a unixsocket based transport is used, {@link SaslAuthMode#AUTH_EXTERNAL} will be used.
     * For TCP based transport {@link SaslAuthMode#AUTH_COOKIE} will be used.
     * <p>
     *
     * @param _authMode authmode to use, if null is given, default mode will be used
     *
     * @return this
     * @deprecated use {@link #configureSasl()} instead
     */
    @Deprecated(forRemoval = true, since = "4.2.2 - 2023-02-03")
    public X withSaslAuthMode(SaslAuthMode _authMode) {
        configureSasl().withAuthMode(_authMode);
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
     * Set to UID to present during SASL authentication.
     * <p>
     * Default is the user of the running JVM process on Unix-like operating systems. On Windows, the default is zero.<br><br>
     *
     * @param _saslUid UID to set, if a negative long is given the default is used
     *
     * @return this
     * @deprecated use {@link #configureSasl()} instead
     */
    @Deprecated(forRemoval = true, since = "4.2.2 - 2023-02-03")
    public X withSaslUid(long _saslUid) {
        configureSasl().withSaslUid(_saslUid);
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
     * Return to the previous builder.
     * <p>
     * This allows you to return from the this builder to the builder which
     * started this builder so you can continue using the previous builder.
     * </p>
     *
     * @return previous builder, maybe null
     */
    public R back() {
        return connectionBuilder != null ? connectionBuilder.get() : null;
    }

    /**
     * Returns the transport config.
     * @return config
     */
    public TransportConfig build() {
        return config;
    }

}
