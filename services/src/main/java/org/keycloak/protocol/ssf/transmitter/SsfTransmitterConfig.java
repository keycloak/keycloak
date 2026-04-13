package org.keycloak.protocol.ssf.transmitter;

import org.keycloak.Config;

/**
 * Immutable snapshot of the transmitter-wide SSF configuration that is sourced
 * from the {@link SsfTransmitterProviderFactory} SPI configuration. Exposed via
 * {@link SsfTransmitterProvider#getConfig()} so that consumers can
 * access the effective defaults without having to go through the factory
 * directly.
 */
public class SsfTransmitterConfig {

    public static final String CONFIG_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS = "push-endpoint-connect-timeout-millis";

    public static final String CONFIG_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS = "push-endpoint-socket-timeout-millis";

    public static final String CONFIG_TRANSMITTER_INITIATED_VERIFICATION_DELAY_MILLIS = "transmitter-initiated-verification-delay-millis";

    /**
     * Default connect timeout (in milliseconds) for delivering SSF events via
     * HTTP push to a receiver's push endpoint.
     */
    public static final int DEFAULT_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS = 1000;

    /**
     * Default socket (read) timeout (in milliseconds) for delivering SSF
     * events via HTTP push to a receiver's push endpoint.
     */
    public static final int DEFAULT_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS = 1000;

    /**
     * Default delay (in milliseconds) before the transmitter dispatches a
     * verification event after a stream has been created or updated.
     */
    public static final int DEFAULT_TRANSMITTER_INITIATED_VERIFICATION_DELAY_MILLIS = 1500;

    private final int pushEndpointConnectTimeoutMillis;

    private final int pushEndpointSocketTimeoutMillis;

    private final int transmitterInitiatedVerificationDelayMillis;

    public SsfTransmitterConfig(int pushEndpointConnectTimeoutMillis,
                                int pushEndpointSocketTimeoutMillis,
                                int transmitterInitiatedVerificationDelayMillis) {
        this.pushEndpointConnectTimeoutMillis = pushEndpointConnectTimeoutMillis;
        this.pushEndpointSocketTimeoutMillis = pushEndpointSocketTimeoutMillis;
        this.transmitterInitiatedVerificationDelayMillis = transmitterInitiatedVerificationDelayMillis;
    }

    /**
     * Builds a {@link SsfTransmitterConfig} from the given SPI configuration
     * scope. Missing properties fall back to the {@code DEFAULT_*} constants
     * declared on this class.
     */
    public SsfTransmitterConfig(Config.Scope config) {
        this(
                config.getInt(CONFIG_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS,
                        DEFAULT_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS),
                config.getInt(CONFIG_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS,
                        DEFAULT_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS),
                config.getInt(CONFIG_TRANSMITTER_INITIATED_VERIFICATION_DELAY_MILLIS,
                        DEFAULT_TRANSMITTER_INITIATED_VERIFICATION_DELAY_MILLIS));
    }

    /**
     * Returns a {@link SsfTransmitterConfig} populated with the {@code DEFAULT_*}
     * constants. Used as the initial value before the factory has been
     * initialized with an SPI configuration scope.
     */
    public static SsfTransmitterConfig defaults() {
        return new SsfTransmitterConfig(
                DEFAULT_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS,
                DEFAULT_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS,
                DEFAULT_TRANSMITTER_INITIATED_VERIFICATION_DELAY_MILLIS);
    }

    /**
     * Connect timeout (in milliseconds) used when delivering SSF
     * events to a receiver's push endpoint if the stream does not define its
     * own timeout.
     */
    public int getPushEndpointConnectTimeoutMillis() {
        return pushEndpointConnectTimeoutMillis;
    }

    /**
     * Socket (read) timeout (in milliseconds) used when delivering SSF
     * events to a receiver's push endpoint if the stream does not define its
     * own timeout.
     */
    public int getPushEndpointSocketTimeoutMillis() {
        return pushEndpointSocketTimeoutMillis;
    }

    /**
     * Delay (in milliseconds) the transmitter waits before dispatching a
     * verification event after a stream has been created or updated.
     */
    public int getTransmitterInitiatedVerificationDelayMillis() {
        return transmitterInitiatedVerificationDelayMillis;
    }
}
