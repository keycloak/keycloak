package org.freedesktop.dbus.connections.transports;

import java.io.IOException;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.SASL;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.connections.config.TransportConfigBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;
import org.freedesktop.dbus.exceptions.TransportRegistrationException;
import org.freedesktop.dbus.spi.transport.ITransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder to create transports of different types.
 *
 * @author hypfvieh
 * @since v4.0.0 - 2021-09-17
 */
public final class TransportBuilder {

    private static final Logger                          LOGGER      = LoggerFactory.getLogger(TransportBuilder.class);
    private static final Map<String, ITransportProvider> PROVIDERS   = getTransportProvider();

    private TransportConfigBuilder<TransportConfigBuilder<?, TransportBuilder>, TransportBuilder> transportConfigBuilder;

    private TransportBuilder(TransportConfig _config) throws DBusException {
        transportConfigBuilder = new TransportConfigBuilder<>(() -> this);
        if (_config != null) {
            transportConfigBuilder.withConfig(_config);
        }
    }

    static Map<String, ITransportProvider> getTransportProvider() {
        Map<String, ITransportProvider> providers = new ConcurrentHashMap<>();
        try {
            ServiceLoader<ITransportProvider> spiLoader = ServiceLoader.load(ITransportProvider.class);
            for (ITransportProvider provider : spiLoader) {
                String providerBusType = provider.getSupportedBusType();
                if (providerBusType == null) { // invalid transport, ignore
                    LOGGER.warn("Transport {} is invalid: No bustype configured", provider.getClass());
                    continue;
                }
                providerBusType = providerBusType.toUpperCase(Locale.US);

                LOGGER.debug("Found provider '{}' named '{}' providing bustype '{}'", provider.getClass().getSimpleName(), provider.getTransportName(), providerBusType);

                if (providers.containsKey(providerBusType)) {
                    throw new TransportRegistrationException("Found transport "
                            + providers.get(providerBusType).getClass().getName()
                            + " and "
                            + provider.getClass().getName() + " both providing transport for socket type "
                            + providerBusType + ", please only add one of them to classpath.");
                }
                providers.put(providerBusType, provider);
            }
            if (providers.isEmpty()) {
                throw new TransportRegistrationException("No dbus-java-transport found in classpath, please add a transport module");
            }
        } catch (ServiceConfigurationError _ex) {
            LOGGER.error("Could not initialize service provider.", _ex);
        }
        return providers;
    }

    /**
     * Creates a new {@link TransportBuilder} instance with the given address.
     *
     * @param _address address, never null
     *
     * @return new {@link TransportBuilder}
     * @throws DBusException if invalid address provided
     *
     */
    public static TransportBuilder create(String _address) throws DBusException {
        TransportConfig cfg = new TransportConfig();
        cfg.setBusAddress(BusAddress.of(_address));
        return new TransportBuilder(cfg);
    }

    /**
     * Creates a new {@link TransportBuilder} instance using the given configuration.
     *
     * @param _config config, never null
     *
     * @return new {@link TransportBuilder}
     * @throws DBusException if invalid address provided
     */
    public static TransportBuilder create(TransportConfig _config) throws DBusException {
        return new TransportBuilder(_config);
    }

    /**
     * Creates a new {@link TransportBuilder} instance using a empty transport configuration.
     *
     * @return new {@link TransportBuilder}
     * @throws DBusException if invalid address provided
     */
    public static TransportBuilder create() throws DBusException {
        return new TransportBuilder(null);
    }

    /**
     * Creates a new {@link TransportBuilder} instance with the given address.
     *
     * @param _address address, never null
     *
     * @return new {@link TransportBuilder}
     * @throws DBusException if invalid address provided
     */
    public static TransportBuilder create(BusAddress _address) throws DBusException {
        Objects.requireNonNull(_address, "BusAddress required");
        return new TransportBuilder(new TransportConfig(_address));
    }

    /**
     * Creates a new {@link TransportBuilder} with a dynamically created address.
     *
     * @param _transportType type of session (e.g. UNIX or TCP)
     *
     * @return {@link TransportBuilder}
     *
     * @throws DBusException when invalid/unknown/unsupported transport type given
     */
    public static TransportBuilder createWithDynamicSession(String _transportType) throws DBusException {
        String dynSession = createDynamicSession(_transportType, false);
        if (dynSession == null) {
            throw new DBusException("Could not create dynamic session for transport type '" + _transportType + "'");
        }
        return create(dynSession);
    }

    /**
     * Set the connection timeout (usually only used for TCP based transports).
     * <p>
     * default: {@link AbstractConnection#TCP_CONNECT_TIMEOUT}
     *
     * @param _timeout timeout, if &lt; 0 default timeout of {@link AbstractConnection#TCP_CONNECT_TIMEOUT} will be used
     *
     * @deprecated please use {@link #configure()}
     */
    @Deprecated(since = "4.2.0 - 2022-07-21", forRemoval = true)
    public TransportBuilder withTimeout(int _timeout) {
        configure().withTimeout(_timeout);
        return this;
    }

    /**
     * Toggle the created transport to be a listening (server) or initiating (client) connection.
     * <p>
     * Default is a client connection.
     *
     * @param _listen true to create a listening transport (e.g. for server usage)
     *
     * @return this
     *
     * @deprecated please use {@link #configure()}
     */
    @Deprecated(forRemoval = true, since = "4.2.0 - 2022-05-23")
    public TransportBuilder isListening(boolean _listen) { //NOPMD
        return listening(_listen);
    }

    /**
     * Toggle the created transport to be a listening (server) or initiating (client) connection.
     * <p>
     * Default is a client connection.
     *
     * @param _listen true to create a listening transport (e.g. for server usage)
     *
     * @deprecated please use {@link #configure()}
     */
    @Deprecated(since = "4.2.0 - 2022-07-21", forRemoval = true)
    public TransportBuilder listening(boolean _listen) {
        configure().withListening(_listen);
        return this;
    }

    /**
     * Instantly connect to DBus when {@link #build()} is called.
     * <p>
     * default: true
     *
     * @param _connect boolean
     *
     * @return this
     * @deprecated please use {@link #configure()}
     */
    @Deprecated(since = "4.2.0 - 2022-07-21", forRemoval = true)
    public TransportBuilder withAutoConnect(boolean _connect) {
        configure().withAutoConnect(_connect);
        return this;
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
     * @deprecated please use {@link #configure()}
     */
    @Deprecated(since = "4.2.0 - 2022-07-21", forRemoval = true)
    public TransportBuilder withSaslAuthMode(SaslAuthMode _authMode) {
        configure().withSaslAuthMode(_authMode);
        return this;
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
     * @deprecated please use {@link #configure()}
     */
    @Deprecated(since = "4.2.0 - 2022-07-21", forRemoval = true)
    public TransportBuilder withUnixSocketFileOwner(String _user) {
        configure().withUnixSocketFileOwner(_user);
        return this;
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
     * @deprecated please use {@link #configure()}
     */
    @Deprecated(since = "4.2.0 - 2022-07-21", forRemoval = true)
    public TransportBuilder withUnixSocketFileGroup(String _group) {
        configure().withUnixSocketFileGroup(_group);
        return this;
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
     *
     * @deprecated please use {@link #configure()}
     */
    @Deprecated(since = "4.2.0 - 2022-07-21", forRemoval = true)
    public TransportBuilder withUnixSocketFilePermissions(PosixFilePermission... _permissions) {
        configure().withUnixSocketFilePermissions(_permissions);
        return this;
    }

    /**
     * Returns the configuration builder to configure the transport.
     * @return TransportConfigBuilder
     */
    public TransportConfigBuilder<TransportConfigBuilder<?, TransportBuilder>, TransportBuilder> configure() {
        return transportConfigBuilder;
    }

    /**
     * Create the transport with the previously provided configuration.
     *
     * @return {@link AbstractTransport} instance
     *
     * @throws DBusException when creating transport fails
     * @throws IOException when autoconnect is true and connection to DBus failed
     */
    public AbstractTransport build() throws DBusException, IOException {
        BusAddress myBusAddress = getAddress();
        TransportConfig config = transportConfigBuilder.build();
        if (myBusAddress == null) {
            throw new DBusException("Transport requires a BusAddress, use withBusAddress() to configure before building");
        }

        AbstractTransport transport = null;
        ITransportProvider provider = PROVIDERS.get(config.getBusAddress().getBusType());
        if (provider == null) {
            throw new DBusException("No transport provider found for bustype " + config.getBusAddress().getBusType());
        } else {
            LOGGER.debug("Using transport {} for address {}", provider.getTransportName(), config.getBusAddress());
        }

        try {
            transport = provider.createTransport(myBusAddress, config);
            Objects.requireNonNull(transport, "Transport required"); // in case the factory returns null, we cannot continue

            if (config.getSaslConfig().getAuthMode() > 0) {
                transport.getSaslConfig().setAuthMode(config.getSaslConfig().getAuthMode());
            }
        } catch (TransportConfigurationException _ex) {
            LOGGER.error("Could not initialize transport", _ex);
        }

        if (transport == null) {
            throw new DBusException("Unknown address type " + myBusAddress.getType() + " or no transport provider found for bus type " + myBusAddress.getBusType());
        }

        if (myBusAddress.isListeningSocket() && myBusAddress instanceof IFileBasedBusAddress) {
            ((IFileBasedBusAddress) myBusAddress).updatePermissions(config.getFileOwner(), config.getFileGroup(), config.getFileUnixPermissions());
        }

        transport.setPreConnectCallback(config.getPreConnectCallback());

        if (config.isAutoConnect()) {
            if (config.isListening()) {
                transport.listen();
            } else {
                transport.connect();
            }
        }
        return transport;
    }

    /**
     * The currently configured BusAddress.
     *
     * @return {@link BusAddress}
     */
    public BusAddress getAddress() {
        return configure().getBusAddress();
    }

    /**
     * Returns a {@link List} of all bustypes supported in the current runtime.
     *
     * @return {@link List}, maybe empty
     */
    public static List<String> getRegisteredBusTypes() {
        return new ArrayList<>(PROVIDERS.keySet());
    }

    /**
     * Creates a new dynamic bus address for the given bus type.
     *
     * @param _busType bus type (e.g. UNIX or TCP), never null
     * @param _listeningAddress true if a listening (server) address should be created, false otherwise
     *
     * @return String containing BusAddress or null
     */
    public static String createDynamicSession(String _busType, boolean _listeningAddress) {
        Objects.requireNonNull(_busType, "Bustype required");
        ITransportProvider provider = PROVIDERS.get(_busType.toUpperCase(Locale.US));
        if (provider != null) {
            return provider.createDynamicSessionAddress(_listeningAddress);
        }
        return null;
    }

    /**
     * Represents supported SASL authentication modes.
     *
     * @author hypfvieh
     * @since v4.0.0 - 2021-09-17
     */
    public enum SaslAuthMode {
        /** No authentication (allow everyone). */
        AUTH_ANONYMOUS(SASL.AUTH_ANON),
        /** Authentication using SHA Cookie. */
        AUTH_COOKIE(SASL.AUTH_SHA),
        /** External authentication (e.g. by user ID). */
        AUTH_EXTERNAL(SASL.AUTH_EXTERNAL);

        private final int authMode;

        SaslAuthMode(int _authMode) {
            authMode = _authMode;
        }

        public int getAuthMode() {
            return authMode;
        }

    }
}
