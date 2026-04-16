package org.keycloak.ssf.transmitter;

import org.keycloak.Config;
import org.keycloak.crypto.Algorithm;
import org.keycloak.ssf.metadata.DefaultSubjects;
import org.keycloak.ssf.subject.IssuerSubjectId;

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

    public static final String CONFIG_MIN_VERIFICATION_INTERVAL_SECONDS = "min-verification-interval-seconds";

    public static final String CONFIG_SIGNATURE_ALGORITHM = "signature-algorithm";

    public static final String CONFIG_USER_SUBJECT_FORMAT = "user-subject-format";

    public static final String CONFIG_DEFAULT_SUBJECTS = "default-subjects";

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

    /**
     * Default minimum amount of time (in seconds) that must pass between
     * receiver-initiated verification requests. Subsequent requests within
     * this window are rejected with HTTP 429.
     */
    public static final int DEFAULT_MIN_VERIFICATION_INTERVAL_SECONDS = 60;

    /**
     * Default signature algorithm the transmitter uses to sign SSF Security
     * Event Tokens. Pinned to RS256 per the CAEP interoperability profile 1.0,
     * section 2.6, which mandates RS256 with RSA keys of at least 2048 bits.
     *
     * @see <a href="https://openid.github.io/sharedsignals/openid-caep-interoperability-profile-1_0.html#section-2.6">CAEP Interoperability Profile §2.6</a>
     */
    public static final String DEFAULT_SIGNATURE_ALGORITHM = Algorithm.RS256;

    /**
     * Default subject identifier format the transmitter uses for the user
     * portion of outgoing SSF Security Event Tokens. Defaults to
     * {@link IssuerSubjectId#TYPE iss_sub} (realm issuer + user ID), matching
     * the behavior the transmitter had before the knob was added.
     */
    public static final String DEFAULT_USER_SUBJECT_FORMAT = IssuerSubjectId.TYPE;

    /**
     * Default value for the transmitter's {@code default_subjects} metadata
     * field (SSF 1.0 §7.1). {@link DefaultSubjects#ALL ALL} preserves the
     * transmitter's pre-subject-management behaviour — events are delivered
     * to every matching stream regardless of per-subject subscriptions.
     * Set to {@link DefaultSubjects#NONE NONE} to require explicit opt-in
     * via {@code ssf.notify.<clientId>} attributes before an event is
     * delivered.
     */
    public static final DefaultSubjects DEFAULT_DEFAULT_SUBJECTS = DefaultSubjects.ALL;

    private final int pushEndpointConnectTimeoutMillis;

    private final int pushEndpointSocketTimeoutMillis;

    private final int transmitterInitiatedVerificationDelayMillis;

    private final int minVerificationIntervalSeconds;

    private final String signatureAlgorithm;

    private final String userSubjectFormat;

    private final DefaultSubjects defaultSubjects;

    public SsfTransmitterConfig(int pushEndpointConnectTimeoutMillis,
                                int pushEndpointSocketTimeoutMillis,
                                int transmitterInitiatedVerificationDelayMillis,
                                int minVerificationIntervalSeconds,
                                String signatureAlgorithm,
                                String userSubjectFormat,
                                DefaultSubjects defaultSubjects) {
        this.pushEndpointConnectTimeoutMillis = pushEndpointConnectTimeoutMillis;
        this.pushEndpointSocketTimeoutMillis = pushEndpointSocketTimeoutMillis;
        this.transmitterInitiatedVerificationDelayMillis = transmitterInitiatedVerificationDelayMillis;
        this.minVerificationIntervalSeconds = minVerificationIntervalSeconds;
        this.signatureAlgorithm = signatureAlgorithm;
        this.userSubjectFormat = userSubjectFormat;
        this.defaultSubjects = defaultSubjects;
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
                        DEFAULT_TRANSMITTER_INITIATED_VERIFICATION_DELAY_MILLIS),
                config.getInt(CONFIG_MIN_VERIFICATION_INTERVAL_SECONDS,
                        DEFAULT_MIN_VERIFICATION_INTERVAL_SECONDS),
                config.get(CONFIG_SIGNATURE_ALGORITHM,
                        DEFAULT_SIGNATURE_ALGORITHM),
                config.get(CONFIG_USER_SUBJECT_FORMAT,
                        DEFAULT_USER_SUBJECT_FORMAT),
                DefaultSubjects.parseOrDefault(config.get(CONFIG_DEFAULT_SUBJECTS),
                        DEFAULT_DEFAULT_SUBJECTS));
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
                DEFAULT_TRANSMITTER_INITIATED_VERIFICATION_DELAY_MILLIS,
                DEFAULT_MIN_VERIFICATION_INTERVAL_SECONDS,
                DEFAULT_SIGNATURE_ALGORITHM,
                DEFAULT_USER_SUBJECT_FORMAT,
                DEFAULT_DEFAULT_SUBJECTS);
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

    /**
     * Minimum amount of time (in seconds) that must pass between
     * receiver-initiated verification requests. Subsequent requests within
     * this window are rejected with HTTP 429.
     */
    public int getMinVerificationIntervalSeconds() {
        return minVerificationIntervalSeconds;
    }

    /**
     * Default JWS signature algorithm used to sign outgoing SSF Security
     * Event Tokens when a receiver client does not configure its own
     * {@code ssf.signatureAlgorithm} attribute. Pinned to RS256 per the CAEP
     * interoperability profile 1.0 §2.6 unless explicitly overridden via the
     * {@code signature-algorithm} SPI property.
     */
    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    /**
     * Default subject identifier format for the user portion of outgoing
     * SSF Security Event Tokens when a receiver client does not configure
     * its own {@code ssf.userSubjectFormat} attribute. Defaults to
     * {@link IssuerSubjectId#TYPE iss_sub} unless overridden via the
     * {@code user-subject-format} SPI property.
     */
    public String getUserSubjectFormat() {
        return userSubjectFormat;
    }

    /**
     * Default behaviour the transmitter advertises for subject-scoped
     * event delivery. Echoed back as {@code default_subjects} on the
     * transmitter's SSF metadata document and consulted by the dispatcher
     * when deciding whether to apply per-stream subject subscription
     * filtering. See {@link DefaultSubjects} for semantics.
     */
    public DefaultSubjects getDefaultSubjects() {
        return defaultSubjects;
    }
}
