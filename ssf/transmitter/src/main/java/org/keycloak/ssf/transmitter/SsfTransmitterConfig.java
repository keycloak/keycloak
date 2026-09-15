package org.keycloak.ssf.transmitter;

import java.util.LinkedHashSet;
import java.util.Set;

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

    public static final String CONFIG_SUBJECT_MANAGEMENT_ENABLED = "subject-management-enabled";

    public static final String CONFIG_SSE_CAEP_ENABLED = "sse-caep-enabled";

    public static final String CONFIG_CRITICAL_SUBJECT_MEMBERS = "critical-subject-members";

    /**
     * Toggles the SSF Prometheus metrics binder. When {@code false}
     * the factory installs a no-op binder and the dispatcher / drainer /
     * poll endpoint skip every meter call — useful for operators who
     * don't want SSF label series in their metrics store, or for
     * debugging a suspected Micrometer issue.
     */
    public static final String CONFIG_METRICS_ENABLED = "metrics-enabled";

    /**
     * Grace period (seconds) during which the dispatcher continues to
     * deliver events for a subject after a <em>receiver-driven</em>
     * {@code POST /streams/subjects/remove} fired. Defends against the
     * SSF 1.0 §9.3 "Malicious Subject Removal" scenario where a
     * compromised receiver bearer token silences events for a target
     * subject during an attack window. Admin-driven removals
     * deliberately skip the tombstone — operator actions are trusted
     * and take effect immediately. Default {@code 0} disables the
     * grace window entirely (current behavior preserved); set to a
     * positive value (e.g. 3600 for one hour) to enable the
     * spec-recommended protection.
     */
    public static final String CONFIG_SUBJECT_REMOVAL_GRACE_SECONDS = "subject-removal-grace-seconds";

    /**
     * Whether the receiver-supplied {@code delivery.endpoint_url} on a PUSH
     * stream is allowed to use the {@code http} scheme or resolve to a
     * loopback / link-local / site-local / unique-local / multicast /
     * any-local address. Default {@code false} — production-safe.
     *
     * <p>Set to {@code true} on closed-network deployments where the
     * transmitter pushes to internal receivers over plaintext or RFC 1918
     * targets, and on integration test setups that push to a local mock
     * server. The {@code ssf.validPushUrls} per-client allowlist is still
     * the primary SSRF gate; this flag only relaxes the
     * scheme/host class check that runs on the URL after it matches the
     * allowlist.
     */
    public static final String CONFIG_ALLOW_INSECURE_PUSH_TARGETS = "allow-insecure-push-targets";

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
    public static final DefaultSubjects DEFAULT_DEFAULT_SUBJECTS = DefaultSubjects.NONE;

    /**
     * Default for the subject-management-enabled flag. {@code true}
     * exposes the {@code /subjects:add} and {@code /subjects:remove}
     * endpoints and advertises them in the transmitter metadata.
     * Set to {@code false} to hide the endpoints entirely — useful for
     * deployments that rely exclusively on admin-curated
     * {@code ssf.notify.<clientId>} attributes and don't want receivers
     * to be able to manage subjects at all.
     */
    public static final boolean DEFAULT_SUBJECT_MANAGEMENT_ENABLED = true;

    /**
     * Default for the SSE CAEP (legacy Apple Business Manager / Apple
     * School Manager) profile flag. {@code true} advertises the legacy
     * RISC PUSH and RISC POLL URIs ({@code https://schemas.openid.net/secevent/risc/delivery-method/{push,poll}})
     * in the transmitter metadata and accepts them on stream-create.
     * Set to {@code false} to expose only the standard SSF 1.0 RFC 8935
     * push and RFC 8936 poll delivery methods — useful for deployments
     * that don't integrate with Apple-style receivers and want to keep
     * the advertised surface to the spec-standard URIs only.
     */
    public static final boolean DEFAULT_SSE_CAEP_ENABLED = true;

    /**
     * Default value for the transmitter's {@code critical_subject_members}
     * metadata field. Names the complex-subject member keys a receiver must
     * understand to interpret SETs from this transmitter — single member
     * {@code "user"} reflects that every CAEP event the transmitter emits
     * carries the user identifier under that key (the session and tenant
     * keys are optional siblings).
     *
     * <p>Override via the {@code critical-subject-members} SPI property
     * with a comma-separated list (e.g. {@code "user,tenant"}). An empty
     * value omits the field from the metadata document entirely.
     */
    public static final Set<String> DEFAULT_CRITICAL_SUBJECT_MEMBERS = Set.of("user");

    /**
     * Default for {@link #isMetricsEnabled()}. Metrics are on by default
     * so operators get useful defaults without extra SPI configuration;
     * impact is bounded (in-memory counters on hot paths, one grouped
     * aggregate per drainer tick for outbox depth).
     */
    public static final boolean DEFAULT_METRICS_ENABLED = true;

    /**
     * Default for {@link #getSubjectRemovalGraceSeconds()}.
     * {@code 0} keeps existing behavior — receiver-driven removes take
     * effect immediately. Operators who want the SSF §9.3 protection
     * set this to a positive value explicitly.
     */
    public static final int DEFAULT_SUBJECT_REMOVAL_GRACE_SECONDS = 0;

    /**
     * Default for {@link #isAllowInsecurePushTargets()}. Always {@code false} —
     * production-safe; receiver-supplied push URLs must be https and must
     * not resolve to private/loopback addresses. We deliberately do NOT
     * relax this in dev mode: a developer who tests PUSH integration in
     * {@code start-dev} against {@code http://localhost} would otherwise
     * never see the production-mode rejection and discover the
     * misconfiguration only at deploy time. Test fixtures and closed-
     * network deployments opt in via the SPI option explicitly.
     */
    public static final boolean DEFAULT_ALLOW_INSECURE_PUSH_TARGETS = false;

    private final int pushEndpointConnectTimeoutMillis;

    private final int pushEndpointSocketTimeoutMillis;

    private final int transmitterInitiatedVerificationDelayMillis;

    private final int minVerificationIntervalSeconds;

    private final String signatureAlgorithm;

    private final String userSubjectFormat;

    private final DefaultSubjects defaultSubjects;

    private final boolean subjectManagementEnabled;

    private final boolean sseCaepEnabled;

    private final Set<String> criticalSubjectMembers;

    private final boolean metricsEnabled;

    private final int subjectRemovalGraceSeconds;

    private final boolean allowInsecurePushTargets;

    public SsfTransmitterConfig(int pushEndpointConnectTimeoutMillis,
                                int pushEndpointSocketTimeoutMillis,
                                int transmitterInitiatedVerificationDelayMillis,
                                int minVerificationIntervalSeconds,
                                String signatureAlgorithm,
                                String userSubjectFormat,
                                DefaultSubjects defaultSubjects,
                                boolean subjectManagementEnabled,
                                boolean sseCaepEnabled,
                                Set<String> criticalSubjectMembers,
                                boolean metricsEnabled,
                                int subjectRemovalGraceSeconds,
                                boolean allowInsecurePushTargets) {
        this.pushEndpointConnectTimeoutMillis = pushEndpointConnectTimeoutMillis;
        this.pushEndpointSocketTimeoutMillis = pushEndpointSocketTimeoutMillis;
        this.transmitterInitiatedVerificationDelayMillis = transmitterInitiatedVerificationDelayMillis;
        this.minVerificationIntervalSeconds = minVerificationIntervalSeconds;
        this.signatureAlgorithm = signatureAlgorithm;
        this.userSubjectFormat = userSubjectFormat;
        this.defaultSubjects = defaultSubjects;
        this.subjectManagementEnabled = subjectManagementEnabled;
        this.sseCaepEnabled = sseCaepEnabled;
        this.criticalSubjectMembers = criticalSubjectMembers;
        this.metricsEnabled = metricsEnabled;
        this.subjectRemovalGraceSeconds = Math.max(0, subjectRemovalGraceSeconds);
        this.allowInsecurePushTargets = allowInsecurePushTargets;
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
                        DEFAULT_DEFAULT_SUBJECTS),
                config.getBoolean(CONFIG_SUBJECT_MANAGEMENT_ENABLED,
                        DEFAULT_SUBJECT_MANAGEMENT_ENABLED),
                config.getBoolean(CONFIG_SSE_CAEP_ENABLED,
                        DEFAULT_SSE_CAEP_ENABLED),
                parseCriticalSubjectMembers(config.get(CONFIG_CRITICAL_SUBJECT_MEMBERS)),
                config.getBoolean(CONFIG_METRICS_ENABLED,
                        DEFAULT_METRICS_ENABLED),
                config.getInt(CONFIG_SUBJECT_REMOVAL_GRACE_SECONDS,
                        DEFAULT_SUBJECT_REMOVAL_GRACE_SECONDS),
                config.getBoolean(CONFIG_ALLOW_INSECURE_PUSH_TARGETS,
                        DEFAULT_ALLOW_INSECURE_PUSH_TARGETS));
    }

    /**
     * Parses the comma-separated {@code critical-subject-members} SPI value.
     * {@code null} (unset) returns {@link #DEFAULT_CRITICAL_SUBJECT_MEMBERS};
     * an explicit empty value returns an empty set, which signals
     * {@link org.keycloak.ssf.transmitter.metadata.TransmitterMetadataService}
     * to omit the metadata field entirely.
     */
    protected static Set<String> parseCriticalSubjectMembers(String raw) {
        if (raw == null) {
            return DEFAULT_CRITICAL_SUBJECT_MEMBERS;
        }
        Set<String> members = new LinkedHashSet<>();
        for (String token : raw.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                members.add(trimmed);
            }
        }
        return members;
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
                DEFAULT_DEFAULT_SUBJECTS,
                DEFAULT_SUBJECT_MANAGEMENT_ENABLED,
                DEFAULT_SSE_CAEP_ENABLED,
                DEFAULT_CRITICAL_SUBJECT_MEMBERS,
                DEFAULT_METRICS_ENABLED,
                DEFAULT_SUBJECT_REMOVAL_GRACE_SECONDS,
                DEFAULT_ALLOW_INSECURE_PUSH_TARGETS);
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

    /**
     * Whether the {@code /subjects:add} and {@code /subjects:remove}
     * endpoints are exposed. When {@code false}, the endpoints are not
     * registered and the transmitter metadata omits them. Subject
     * subscriptions can still be managed via admin-curated
     * {@code ssf.notify.<clientId>} attributes on users and
     * organizations.
     */
    public boolean isSubjectManagementEnabled() {
        return subjectManagementEnabled;
    }

    /**
     * Whether the legacy SSE CAEP profile (Apple Business Manager /
     * Apple School Manager interop) is exposed. When {@code true}
     * (default), the transmitter advertises the RISC PUSH and RISC POLL
     * URIs in {@code delivery_methods_supported} and accepts them on
     * {@code POST /streams}. When {@code false}, only the spec-standard
     * SSF 1.0 RFC 8935 push and RFC 8936 poll URIs are advertised /
     * accepted — useful for deployments that don't integrate with
     * Apple-style receivers.
     */
    public boolean isSseCaepEnabled() {
        return sseCaepEnabled;
    }

    /**
     * Names the complex-subject member keys a receiver MUST understand to
     * interpret SETs from this transmitter. Echoed back as
     * {@code critical_subject_members} on the SSF metadata document so a
     * receiver can fail-fast on stream-create if it can't process the
     * required keys. Empty set ⇒ omit the field entirely.
     */
    public Set<String> getCriticalSubjectMembers() {
        return criticalSubjectMembers;
    }

    /**
     * Whether the SSF Prometheus metrics binder is installed. When
     * {@code false} every meter call is a no-op — see
     * {@link org.keycloak.ssf.transmitter.metrics.SsfMetricsBinder}.
     */
    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    /**
     * Grace window (seconds) during which the dispatcher continues to
     * deliver events for a subject after a receiver-driven
     * {@code POST /streams/subjects/remove}. {@code 0} disables the
     * grace and current-behavior takes effect — receiver removes are
     * applied immediately. See
     * {@link #CONFIG_SUBJECT_REMOVAL_GRACE_SECONDS} for the SSF §9.3
     * rationale.
     */
    public int getSubjectRemovalGraceSeconds() {
        return subjectRemovalGraceSeconds;
    }

    /**
     * Whether the receiver-supplied push {@code delivery.endpoint_url} is
     * permitted to use the {@code http} scheme or resolve to a loopback /
     * link-local / site-local / unique-local / multicast / any-local
     * address. {@code false} (the default) means the URL must be {@code https}
     * and must resolve to a publicly routable address; {@code true} relaxes
     * both checks for closed-network deployments and integration tests.
     * The {@code ssf.validPushUrls} per-client allowlist remains the
     * primary SSRF gate regardless of this flag — see
     * {@link #CONFIG_ALLOW_INSECURE_PUSH_TARGETS} for context.
     */
    public boolean isAllowInsecurePushTargets() {
        return allowInsecurePushTargets;
    }
}
