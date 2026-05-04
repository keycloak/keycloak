package org.keycloak.ssf.transmitter;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.events.outbox.OutboxBackoff;
import org.keycloak.events.outbox.OutboxCleanupTask;
import org.keycloak.events.outbox.OutboxConfig;
import org.keycloak.events.outbox.OutboxDrainerTask;
import org.keycloak.events.outbox.OutboxStore;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.services.scheduled.ScheduledTaskRunner;
import org.keycloak.ssf.event.SsfEventProviderFactory;
import org.keycloak.ssf.event.SsfEventRegistry;
import org.keycloak.ssf.transmitter.delivery.SecurityEventTokenDispatcher;
import org.keycloak.ssf.transmitter.delivery.poll.PollDeliveryService;
import org.keycloak.ssf.transmitter.delivery.push.PushDeliveryService;
import org.keycloak.ssf.transmitter.event.SecurityEventTokenEncoder;
import org.keycloak.ssf.transmitter.event.SecurityEventTokenMapper;
import org.keycloak.ssf.transmitter.metadata.TransmitterMetadataService;
import org.keycloak.ssf.transmitter.metrics.SsfMetricsBinder;
import org.keycloak.ssf.transmitter.outbox.SsfOutboxKinds;
import org.keycloak.ssf.transmitter.outbox.SsfPushDeliveryHandler;
import org.keycloak.ssf.transmitter.stream.StreamVerificationService;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.ssf.transmitter.subject.DefaultSsfSubjectInclusionResolver;
import org.keycloak.ssf.transmitter.subject.SsfSubjectInclusionResolver;
import org.keycloak.ssf.transmitter.subject.SubjectManagementService;
import org.keycloak.ssf.transmitter.support.SsfUtil;
import org.keycloak.timer.TimerProvider;

import org.jboss.logging.Logger;

public class DefaultSsfTransmitterProviderFactory implements SsfTransmitterProviderFactory, SsfTransmitterServiceBuilder {

    private static final Logger log = Logger.getLogger(DefaultSsfTransmitterProviderFactory.class);

    public static final String CONFIG_SUPPORTED_EVENTS = "supported-events";

    /**
     * Name passed to {@link ExecutorsProvider#getExecutor(String)} for
     * the background outbox cleanup executor the client-/realm-removed
     * listeners submit to. Kept as a constant so ops teams can surface
     * the pool in their Quarkus thread-pool configuration if they want
     * a non-default concurrency / queue-size profile.
     */
    public static final String OUTBOX_CLEANUP_EXECUTOR = "ssf-outbox-cleanup";

    public static final String CONFIG_OUTBOX_DRAINER_INTERVAL = "outbox-drainer-interval";

    public static final String CONFIG_OUTBOX_DRAINER_BATCH_SIZE = "outbox-drainer-batch-size";

    public static final String CONFIG_OUTBOX_DRAINER_MAX_ATTEMPTS = "outbox-drainer-max-attempts";

    public static final String CONFIG_OUTBOX_DEAD_LETTER_RETENTION = "outbox-dead-letter-retention";

    public static final String CONFIG_OUTBOX_DELIVERED_RETENTION = "outbox-delivered-retention";

    public static final String CONFIG_OUTBOX_PENDING_MAX_AGE = "outbox-pending-max-age";

    public static final long DEFAULT_OUTBOX_DRAINER_INTERVAL_MILLIS = Duration.ofSeconds(30).toMillis();

    public static final int DEFAULT_OUTBOX_DRAINER_BATCH_SIZE = 50;

    /**
     * Default retention for {@code DEAD_LETTER} outbox rows — 30 days. Set
     * to {@code 0} to disable the purge and retain dead-letters
     * indefinitely (e.g. for audit/forensic use cases).
     */
    public static final long DEFAULT_OUTBOX_DEAD_LETTER_RETENTION_MILLIS = Duration.ofDays(30).toMillis();

    /**
     * Default retention for {@code DELIVERED} outbox rows — 24 hours.
     * Delivered rows are kept post-delivery to preserve jti-dedup
     * coverage for at-least-once enqueue paths that might retry shortly
     * after a successful delivery; the value must be well beyond the
     * maximum backoff window so no in-flight retry can outlive it. Set
     * to {@code 0} to disable the purge and retain delivered rows
     * indefinitely.
     */
    public static final long DEFAULT_OUTBOX_DELIVERED_RETENTION_MILLIS = Duration.ofHours(24).toMillis();

    /**
     * Default backstop for {@code PENDING} outbox rows - 2 days. Any
     * row older than this is bulk-promoted to {@code DEAD_LETTER} so
     * the dead-letter retention purge can eventually delete it. Bounds
     * the worst case where rows would otherwise sit forever (e.g. realm
     * with SSF transmitter switched off, no per-receiver
     * {@code ssf.maxEventAgeSeconds}, no realm/client removal). Chosen
     * shorter than the dead-letter retention window so promoted rows
     * still get a meaningful forensic window before the dead-letter
     * purge deletes them. Set to {@code 0} to disable the backstop.
     */
    public static final long DEFAULT_OUTBOX_PENDING_MAX_AGE_MILLIS = Duration.ofDays(2).toMillis();

    /**
     * Aliases (or full URIs) of the events the transmitter advertises as
     * "default supported events" for a receiver client that does not set
     * its own {@code ssf.supportedEvents} attribute. Sourced from the
     * {@code supported-events} SPI property. When {@code null} (i.e. the
     * property is unset), the provider falls back to every event type
     * known to the
     * {@link SsfEventRegistry}, which
     * includes events contributed by custom
     * {@link SsfEventProviderFactory}
     * implementations.
     */
    protected Set<String> configuredDefaultSupportedEventAliases;

    protected SsfTransmitterConfig transmitterConfig = SsfTransmitterConfig.defaults();

    protected long outboxDrainerIntervalMillis = DEFAULT_OUTBOX_DRAINER_INTERVAL_MILLIS;

    protected int outboxDrainerBatchSize = DEFAULT_OUTBOX_DRAINER_BATCH_SIZE;

    protected int outboxDrainerMaxAttempts = OutboxBackoff.DEFAULT_MAX_ATTEMPTS;

    protected long outboxDeadLetterRetentionMillis = DEFAULT_OUTBOX_DEAD_LETTER_RETENTION_MILLIS;

    protected long outboxDeliveredRetentionMillis = DEFAULT_OUTBOX_DELIVERED_RETENTION_MILLIS;

    protected long outboxPendingMaxAgeMillis = DEFAULT_OUTBOX_PENDING_MAX_AGE_MILLIS;

    /**
     * Shared metrics binder — constructed once at factory init time and
     * reused across every session-scoped dispatcher, the long-lived
     * drainer task, and any on-demand lookups (poll endpoint).
     * Defaults to {@link SsfMetricsBinder#NOOP} until
     * {@link #init(Config.Scope)} decides whether metrics are enabled.
     */
    protected SsfMetricsBinder metricsBinder = SsfMetricsBinder.NOOP;

    /**
     * Factory-scoped context bundle. Built in {@link #init} from the
     * resolved {@link #transmitterConfig}, {@link #metricsBinder},
     * and method references on this factory. Re-used across every
     * per-session {@link DefaultSsfTransmitterProvider} instance — the
     * provider's two-arg constructor takes only {@code (session, ctx)}.
     */
    protected SsfTransmitterContext context;

    /**
     * Handles {@code RealmRemovedEvent} / {@code ClientRemovedEvent}
     * by submitting a background {@link OutboxCleanupTask} that
     * drains the orphaned outbox rows in a bounded transaction
     * (REALM_ID / OWNER_ID are not foreign keys on OUTBOX_ENTRY
     * — the table is shared infrastructure, so it can't declare FKs
     * into core Keycloak tables — and an inline
     * bulk DELETE would drag the admin's removal transaction into a
     * timeout for receivers with a large backlog). Also handles
     * {@code ClientUpdatedEvent} to reject saves that would leave two
     * clients in the same realm sharing an {@code ssf.streamId}
     * ({@link ModelDuplicateException} propagates out of the event so
     * the admin fixes the duplicate instead of having SSF state
     * silently mutated).
     */
    protected ProviderEventListener ssfProviderEventListener;


    @Override
    public String getId() {
        return "default";
    }

    @Override
    public SsfTransmitterProvider create(KeycloakSession session) {
        return new DefaultSsfTransmitterProvider(session, context);
    }

    // -- SsfTransmitterServiceBuilder ------------------------------------
    //
    // Leaf services: built directly from (session, ctx). Subclasses
    // override one method instead of touching the rest of the wiring.

    @Override
    public SecurityEventTokenEncoder createEncoder(KeycloakSession session, SsfTransmitterContext ctx) {
        return new SecurityEventTokenEncoder(session);
    }

    @Override
    public SecurityEventTokenMapper createMapper(KeycloakSession session, SsfTransmitterContext ctx) {
        return new SecurityEventTokenMapper(session, ctx.config(), ctx.issuerUrlFactory());
    }

    @Override
    public PushDeliveryService createPushDelivery(KeycloakSession session, SsfTransmitterContext ctx) {
        return new PushDeliveryService(session, ctx.config());
    }

    @Override
    public ClientStreamStore createStreamStore(KeycloakSession session, SsfTransmitterContext ctx) {
        return new ClientStreamStore(session);
    }

    @Override
    public TransmitterMetadataService createMetadataService(KeycloakSession session, SsfTransmitterContext ctx) {
        return new TransmitterMetadataService(session, ctx.issuerUrlFactory(), ctx.config());
    }

    @Override
    public SubjectManagementService createSubjectManagement(KeycloakSession session, SsfTransmitterContext ctx) {
        return new SubjectManagementService(session);
    }

    @Override
    public SsfSubjectInclusionResolver createSubjectInclusionResolver(KeycloakSession session, SsfTransmitterContext ctx) {
        return new DefaultSsfSubjectInclusionResolver();
    }

    // Composite services: pull cached deps off the provider so we
    // share the same encoder / push delivery / mapper instances the
    // rest of the request uses.

    @Override
    public SecurityEventTokenDispatcher createDispatcher(SsfTransmitterProvider provider) {
        SsfTransmitterContext ctx = provider.context();
        return new SecurityEventTokenDispatcher(provider.session(),
                provider.securityEventTokenEncoder(),
                provider.pushDeliveryService(),
                ctx.config(),
                this::createOutboxStore,
                ctx.metrics(),
                provider.subjectInclusionResolver());
    }

    @Override
    public StreamVerificationService createVerification(SsfTransmitterProvider provider) {
        return new StreamVerificationService(provider.session(),
                provider.streamStore(),
                provider.securityEventTokenMapper(),
                provider.securityEventTokenDispatcher(),
                provider.metrics());
    }

    @Override
    public PollDeliveryService createPollDelivery(SsfTransmitterProvider provider) {
        KeycloakSession session = provider.session();
        return new PollDeliveryService(session,
                createOutboxStore(session),
                provider.metrics());
    }

    @Override
    public void init(Config.Scope config) {

        this.configuredDefaultSupportedEventAliases = extractSupportedEvents(config);
        this.transmitterConfig = createTransmitterConfig(config);

        String intervalStr = config.get(CONFIG_OUTBOX_DRAINER_INTERVAL);
        if (intervalStr != null) {
            this.outboxDrainerIntervalMillis = SsfUtil.parseDurationMillis(intervalStr, DEFAULT_OUTBOX_DRAINER_INTERVAL_MILLIS);
        }
        Integer batch = config.getInt(CONFIG_OUTBOX_DRAINER_BATCH_SIZE);
        if (batch != null && batch > 0) {
            this.outboxDrainerBatchSize = batch;
        }
        Integer attempts = config.getInt(CONFIG_OUTBOX_DRAINER_MAX_ATTEMPTS);
        if (attempts != null && attempts > 0) {
            this.outboxDrainerMaxAttempts = attempts;
        }
        String retentionStr = config.get(CONFIG_OUTBOX_DEAD_LETTER_RETENTION);
        if (retentionStr != null) {
            this.outboxDeadLetterRetentionMillis = SsfUtil.parseDurationMillis(retentionStr, DEFAULT_OUTBOX_DEAD_LETTER_RETENTION_MILLIS);
        }
        String deliveredRetentionStr = config.get(CONFIG_OUTBOX_DELIVERED_RETENTION);
        if (deliveredRetentionStr != null) {
            this.outboxDeliveredRetentionMillis = SsfUtil.parseDurationMillis(deliveredRetentionStr, DEFAULT_OUTBOX_DELIVERED_RETENTION_MILLIS);
        }
        String pendingMaxAgeStr = config.get(CONFIG_OUTBOX_PENDING_MAX_AGE);
        if (pendingMaxAgeStr != null) {
            this.outboxPendingMaxAgeMillis = SsfUtil.parseDurationMillis(pendingMaxAgeStr, DEFAULT_OUTBOX_PENDING_MAX_AGE_MILLIS);
        }

        // Metrics binder lifecycle. Two gates combine:
        //   1. SSF-level metrics-enabled SPI knob — lets an operator
        //      opt out of SSF meters specifically (e.g. to avoid label
        //      cardinality they don't want in their Prometheus).
        //   2. Micrometer classpath availability — keeps SSF functional
        //      on custom distributions that strip Micrometer.
        //
        // Note: we deliberately do NOT pre-check
        // Metrics.globalRegistry.getRegistries().isEmpty() here —
        // the SPI init() runs early in Quarkus boot, before the
        // Micrometer extension wires the Prometheus / OTLP registries
        // into the global composite. A pre-flight check at this point
        // would always see an empty composite and permanently install
        // NOOP even when --metrics-enabled=true. The Keycloak-global
        // gate is the /metrics endpoint itself: if the operator
        // disabled metrics, our in-memory increments land in an empty
        // composite and harmlessly do nothing.
        this.metricsBinder = resolveMetricsBinder(transmitterConfig);

        this.ssfProviderEventListener = createProviderEventListener();

        // Build the factory-scoped context bundle. From here on
        // every per-session DefaultSsfTransmitterProvider takes only
        // (session, ctx) as constructor args.
        this.context = createTransmitterContext();
    }

    protected SsfTransmitterContext createTransmitterContext() {
        return new SsfTransmitterContext(
                this.transmitterConfig,
                this.configuredDefaultSupportedEventAliases,
                this.metricsBinder,
                this::createOutboxStore,
                this::createSsfIssuerUrl,
                this);
    }

    /**
     * Picks the real {@link SsfMetricsBinder} or {@link SsfMetricsBinder#NOOP}
     * based on the SSF-level toggle + Micrometer classpath availability.
     * Runs at {@code init()} time, so the chosen binder lives for the
     * duration of the factory.
     */
    protected SsfMetricsBinder resolveMetricsBinder(SsfTransmitterConfig transmitterConfig) {
        if (!transmitterConfig.isMetricsEnabled()) {
            log.infof("SSF metrics disabled by SPI config (%s=false) — installing NOOP binder",
                    SsfTransmitterConfig.CONFIG_METRICS_ENABLED);
            return SsfMetricsBinder.NOOP;
        }
        try {
            SsfMetricsBinder binder = new SsfMetricsBinder();
            log.infof("SSF metrics binder installed; meters under prefix %s",
                    SsfMetricsBinder.METER_OUTBOX_DEPTH.substring(0,
                            SsfMetricsBinder.METER_OUTBOX_DEPTH.indexOf("outbox")));
            return binder;
        } catch (LinkageError micrometerUnavailable) {
            // Covers NoClassDefFoundError + all other linkage errors.
            log.warnf(micrometerUnavailable,
                    "Micrometer not available on classpath — SSF metrics disabled");
            return SsfMetricsBinder.NOOP;
        }
    }

    protected SsfTransmitterConfig createTransmitterConfig(Config.Scope config) {
        return new SsfTransmitterConfig(config);
    }


    /**
     * Session-scoped factory for the generic outbox DAO. Extension
     * point for deployments that want to plug in a custom
     * {@link OutboxStore} subclass (e.g. for instrumentation or
     * schema overrides) — the default is {@code OutboxStore::new}.
     */
    protected OutboxStore createOutboxStore(KeycloakSession session) {
        return new OutboxStore(session);
    }

    protected String createSsfIssuerUrl(KeycloakSession session) {
        return SsfUtil.getIssuerUrl(session);
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(SsfTransmitterConfig.CONFIG_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS)
                .type("int")
                .helpText("Default connect timeout in milliseconds for delivering SSF events via HTTP push to a receiver's push endpoint.")
                .defaultValue(SsfTransmitterConfig.DEFAULT_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS)
                .add()
                .property()
                .name(SsfTransmitterConfig.CONFIG_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS)
                .type("int")
                .helpText("Default socket (read) timeout in milliseconds for delivering SSF events via HTTP push to a receiver's push endpoint.")
                .defaultValue(SsfTransmitterConfig.DEFAULT_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS)
                .add()
                .property()
                .name(SsfTransmitterConfig.CONFIG_TRANSMITTER_INITIATED_VERIFICATION_DELAY_MILLIS)
                .type("int")
                .helpText("Delay in milliseconds before the transmitter dispatches a verification event after a stream is created or updated.")
                .defaultValue(SsfTransmitterConfig.DEFAULT_TRANSMITTER_INITIATED_VERIFICATION_DELAY_MILLIS)
                .add()
                .property()
                .name(SsfTransmitterConfig.CONFIG_MIN_VERIFICATION_INTERVAL_SECONDS)
                .type("int")
                .helpText("Minimum amount of time in seconds that must pass between receiver-initiated verification requests. Requests within this window are rejected with HTTP 429. Set to 0 to disable rate limiting.")
                .defaultValue(SsfTransmitterConfig.DEFAULT_MIN_VERIFICATION_INTERVAL_SECONDS)
                .add()
                .property()
                .name(CONFIG_SUPPORTED_EVENTS)
                .type("string")
                .helpText("Comma-separated list of event aliases or full event type URIs that the transmitter advertises as the default supported event set for receiver clients that do not configure their own ssf.supportedEvents attribute. When unset, every event type registered via SsfEventProviderFactory is advertised.")
                .add()
                .property()
                .name(SsfTransmitterConfig.CONFIG_SIGNATURE_ALGORITHM)
                .type("string")
                .helpText("Default JWS signature algorithm used to sign outgoing SSF Security Event Tokens when a receiver client does not configure its own ssf.signatureAlgorithm attribute. Defaults to RS256 per the CAEP interoperability profile 1.0 §2.6.")
                .defaultValue(SsfTransmitterConfig.DEFAULT_SIGNATURE_ALGORITHM)
                .add()
                .property()
                .name(SsfTransmitterConfig.CONFIG_USER_SUBJECT_FORMAT)
                .type("string")
                .helpText("Default subject identifier format for the user portion of outgoing SSF Security Event Tokens when a receiver client does not configure its own ssf.userSubjectFormat attribute. Defaults to iss_sub (realm issuer + user ID). Allowed values: iss_sub, email.")
                .defaultValue(SsfTransmitterConfig.DEFAULT_USER_SUBJECT_FORMAT)
                .add()
                .property()
                .name(SsfTransmitterConfig.CONFIG_DEFAULT_SUBJECTS)
                .type("string")
                .helpText("Value advertised as default_subjects on the transmitter's SSF metadata document. ALL means the transmitter delivers events for every matching subject unless a stream narrows it explicitly; NONE means events are only delivered for subjects that have been explicitly subscribed (via receiver add-subject calls or admin-curated ssf.notify.<clientId> attributes). Allowed values: ALL, NONE.")
                .defaultValue(SsfTransmitterConfig.DEFAULT_DEFAULT_SUBJECTS.name())
                .add()
                .property()
                .name(CONFIG_OUTBOX_DRAINER_INTERVAL)
                .type("string")
                .helpText("How often the SSF push outbox drainer ticks. Accepts suffixes ms, s, m, h (default 30s).")
                .defaultValue(DEFAULT_OUTBOX_DRAINER_INTERVAL_MILLIS + "ms")
                .add()
                .property()
                .name(CONFIG_OUTBOX_DRAINER_BATCH_SIZE)
                .type("int")
                .helpText("Maximum number of outbox rows processed per drainer tick.")
                .defaultValue(DEFAULT_OUTBOX_DRAINER_BATCH_SIZE)
                .add()
                .property()
                .name(CONFIG_OUTBOX_DRAINER_MAX_ATTEMPTS)
                .type("int")
                .helpText("Maximum number of push attempts before an outbox row is dead-lettered.")
                .defaultValue(OutboxBackoff.DEFAULT_MAX_ATTEMPTS)
                .add()
                .property()
                .name(CONFIG_OUTBOX_DEAD_LETTER_RETENTION)
                .type("string")
                .helpText("How long DEAD_LETTER outbox rows are retained before the drainer purges them. Accepts suffixes ms, s, m, h (default 30d equivalent). Set to 0 to retain dead-letters indefinitely.")
                .defaultValue(DEFAULT_OUTBOX_DEAD_LETTER_RETENTION_MILLIS + "ms")
                .add()
                .property()
                .name(CONFIG_OUTBOX_DELIVERED_RETENTION)
                .type("string")
                .helpText("How long DELIVERED outbox rows are retained before the drainer purges them. Kept post-delivery to preserve jti-dedup coverage for at-least-once enqueue paths that might retry shortly after a successful delivery — the value must be well beyond the maximum backoff window so no in-flight retry can outlive it. Accepts suffixes ms, s, m, h (default 24h equivalent). Set to 0 to retain delivered rows indefinitely.")
                .defaultValue(DEFAULT_OUTBOX_DELIVERED_RETENTION_MILLIS + "ms")
                .add()
                .property()
                .name(CONFIG_OUTBOX_PENDING_MAX_AGE)
                .type("string")
                .helpText("Backstop max age for PENDING outbox rows: any PENDING row older than this gets bulk-promoted to DEAD_LETTER on every drainer tick, so the dead-letter retention purge eventually deletes it. Bounds the worst case where rows would otherwise sit forever (no per-receiver ssf.maxEventAgeSeconds, no realm/client removal, push attempts repeatedly blocked). Should be shorter than outbox-dead-letter-retention so promoted rows retain a meaningful forensic window, and comfortably above the natural retry-curve sum (sum of outbox-drainer-max-attempts entries) so rows in legitimate backoff aren't prematurely promoted. Accepts suffixes ms, s, m, h (default 2d equivalent). Set to 0 to disable the backstop.")
                .defaultValue(DEFAULT_OUTBOX_PENDING_MAX_AGE_MILLIS + "ms")
                .add()
                .property()
                .name(SsfTransmitterConfig.CONFIG_SUBJECT_MANAGEMENT_ENABLED)
                .type("boolean")
                .helpText("Whether the /subjects:add and /subjects:remove endpoints are exposed. When false, the endpoints are not registered and the transmitter metadata omits them. Subject subscriptions can still be managed via admin-curated ssf.notify.<clientId> attributes.")
                .defaultValue(SsfTransmitterConfig.DEFAULT_SUBJECT_MANAGEMENT_ENABLED)
                .add()
                .property()
                .name(SsfTransmitterConfig.CONFIG_SSE_CAEP_ENABLED)
                .type("boolean")
                .helpText("Whether the legacy SSE CAEP (Apple Business Manager / Apple School Manager) profile is exposed. When true (default), the transmitter advertises the RISC PUSH and RISC POLL URIs in delivery_methods_supported and accepts them on stream-create. When false, only the standard SSF 1.0 RFC 8935 push and RFC 8936 poll delivery methods are advertised.")
                .defaultValue(SsfTransmitterConfig.DEFAULT_SSE_CAEP_ENABLED)
                .add()
                .property()
                .name(SsfTransmitterConfig.CONFIG_METRICS_ENABLED)
                .type("boolean")
                .helpText("Whether the SSF Prometheus metrics binder is installed. When true (default), the dispatcher, push drainer and poll endpoint record per-realm / per-receiver counters and timers plus per-(realm, status) outbox depth gauges refreshed once per drainer tick. Set to false to disable all SSF meters — hot-path calls then become branch-predicted no-ops.")
                .defaultValue(SsfTransmitterConfig.DEFAULT_METRICS_ENABLED)
                .add()
                .property()
                .name(SsfTransmitterConfig.CONFIG_SUBJECT_REMOVAL_GRACE_SECONDS)
                .type("int")
                .helpText("Grace window (seconds) during which the dispatcher continues delivering events for a subject after a receiver-driven /streams/subjects/remove call (SSF 1.0 §9.3 'Malicious Subject Removal' defense). Admin-driven removes always take effect immediately. Default 0 disables the grace entirely. Enabling means accepting that legitimate churn-removes (receiver dropping users that left their service) also get the grace tail.")
                .defaultValue(SsfTransmitterConfig.DEFAULT_SUBJECT_REMOVAL_GRACE_SECONDS)
                .add()
                .build();
    }

    /**
     * Parses the {@code supported-events} SPI property into a set of
     * aliases (or full URIs). Returns {@code null} when the property is
     * unset or blank so the provider can fall back to the full
     * {@link SsfEventRegistry}.
     */
    protected Set<String> extractSupportedEvents(Config.Scope config) {
        String defaultSupportedEventsString = config.get(CONFIG_SUPPORTED_EVENTS);

        if (defaultSupportedEventsString == null || defaultSupportedEventsString.isBlank()) {
            return null;
        }

        return parseSupportedEvents(defaultSupportedEventsString);
    }

    protected Set<String> parseSupportedEvents(String supportedEventsString) {
        return SsfEventRegistry.parseEventTypeAliases(supportedEventsString);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        scheduleOutboxDrainer(factory);
        factory.register(ssfProviderEventListener);
        // NOTE: ssf.notify.<clientId> attributes on users/orgs are NOT
        // cleaned up on client removal. Orphaned attributes are inert —
        // the dispatcher resolves the stream (which requires a live
        // client) before checking the attribute, so no stale attribute
        // can cause a spurious delivery. Cleaning them eagerly would
        // require loading every tagged user/org in one transaction,
        // which is prohibitively expensive for receivers with large
        // subscriber sets. If storage hygiene becomes a concern, add an
        // admin "purge stale SSF notify attributes" endpoint or a
        // bulk-SQL scheduled task.
    }

    /**
     * Registers the SSF push outbox drainer with Keycloak's
     * {@link TimerProvider}, wrapped in a
     * {@link ClusterAwareScheduledTaskRunner} so that in an HA deployment
     * only one node drains per interval even though every node schedules
     * the timer.
     */
    protected void scheduleOutboxDrainer(KeycloakSessionFactory factory) {
        try (KeycloakSession session = factory.create()) {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            OutboxDrainerTask task = createDrainerTask(session);
            ScheduledTaskRunner runner = createDrainerScheduledTaskRunner(factory, task);
            timer.schedule(runner, outboxDrainerIntervalMillis, outboxDrainerIntervalMillis,
                    "SsfPushOutboxDrainerTask");
            log.infof("SSF push outbox drainer scheduled: entryKind=%s, interval=%dms, batchSize=%d, maxAttempts=%d, deadLetterRetention=%s, deliveredRetention=%s, pendingMaxAge=%s",
                    SsfOutboxKinds.PUSH, outboxDrainerIntervalMillis,
                    outboxDrainerBatchSize, outboxDrainerMaxAttempts,
                    outboxDeadLetterRetentionMillis > 0 ? outboxDeadLetterRetentionMillis + "ms" : "disabled",
                    outboxDeliveredRetentionMillis > 0 ? outboxDeliveredRetentionMillis + "ms" : "disabled",
                    outboxPendingMaxAgeMillis > 0 ? outboxPendingMaxAgeMillis + "ms" : "disabled");
        }
    }

    protected ScheduledTaskRunner createDrainerScheduledTaskRunner(KeycloakSessionFactory factory, OutboxDrainerTask task) {
        return new ClusterAwareScheduledTaskRunner(factory, task, outboxDrainerIntervalMillis);
    }

    protected OutboxDrainerTask createDrainerTask(KeycloakSession session) {

        // The drainer is generic; SSF-specific delivery semantics live
        // on the SsfPushDeliveryHandler the drainer invokes per row.
        // PushDeliveryService is constructed lazily inside the handler
        // via the same SsfTransmitterServiceBuilder#createPushDelivery
        // factory method the per-session provider uses, so a custom
        // subclass that overrides it (e.g. for instrumentation or
        // tweaked timeouts) automatically applies to outbox push
        // delivery too.
        return new OutboxDrainerTask(
                createOutboxConfig(),
                createSsfPushDeliveryHandler(),
                this::createOutboxStore);
    }

    protected SsfPushDeliveryHandler createSsfPushDeliveryHandler() {
        return new SsfPushDeliveryHandler(this.context, this::createPushDelivery, this.metricsBinder);
    }

    protected OutboxConfig createOutboxConfig() {
        OutboxBackoff backoff = new OutboxBackoff(outboxDrainerMaxAttempts, OutboxBackoff.DEFAULT_HTTP_PUSH_CURVE);
        Duration deadLetterRetention = outboxDeadLetterRetentionMillis > 0
                ? Duration.ofMillis(outboxDeadLetterRetentionMillis)
                : null;
        Duration deliveredRetention = outboxDeliveredRetentionMillis > 0
                ? Duration.ofMillis(outboxDeliveredRetentionMillis)
                : null;
        Duration pendingMaxAge = outboxPendingMaxAgeMillis > 0
                ? Duration.ofMillis(outboxPendingMaxAgeMillis)
                : null;

        return new OutboxConfig(
                SsfOutboxKinds.PUSH,
                outboxDrainerBatchSize,
                backoff,
                deadLetterRetention,
                deliveredRetention,
                pendingMaxAge);
    }


    protected ProviderEventListener createProviderEventListener() {
        return new ProviderEventListener() {
            @Override
            public void onEvent(ProviderEvent event) {
                if (event instanceof RealmModel.RealmRemovedEvent ev) {
                    submitOutboxCleanup(ev.getKeycloakSession(),
                            OutboxCleanupTask.Scope.REALM, ev.getRealm().getId());
                } else if (event instanceof ClientModel.ClientRemovedEvent ev) {
                    submitOutboxCleanup(ev.getKeycloakSession(),
                            OutboxCleanupTask.Scope.OWNER, ev.getClient().getId());
                } else if (event instanceof ClientModel.ClientUpdatedEvent ev) {
                    // Deliberately does not catch — a ModelDuplicateException
                    // from the validator must propagate so the offending
                    // import / update is rolled back with a clear error.
                    validateImportedStreamId(ev.getKeycloakSession(), ev.getUpdatedClient());
                }
            }
        };
    }

    /**
     * Submits a background {@link OutboxCleanupTask} that drains the
     * outbox rows owned by the removed client or realm in a bounded
     * transaction. Runs on the {@link ExecutorsProvider#getExecutor(String)
     * ssf-cleanup} executor so the admin's removal transaction can
     * commit immediately — a receiver with a large queued backlog
     * would otherwise serialize its entire delete into the synchronous
     * client-/realm-removal transaction and risk a transaction timeout.
     *
     * <p>Fire-and-forget: if the originating node crashes before the
     * task finishes, the drainer's missing-resolve / pendingMaxAge
     * fast-paths sweep orphan rows on subsequent ticks. The event is
     * only published on the node that handled the admin request, so
     * there's no cross-node contention to coordinate.
     *
     * <p>Submits one cleanup task per SSF entryKind ({@code ssf-push}
     * and {@code ssf-poll}) so removing a realm or receiver client
     * drops both PUSH and POLL outbox rows.
     */
    protected void submitOutboxCleanup(KeycloakSession session,
                                       OutboxCleanupTask.Scope scope,
                                       String scopeId) {
        try {
            ExecutorService executor = session.getProvider(ExecutorsProvider.class)
                    .getExecutor(OUTBOX_CLEANUP_EXECUTOR);
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            executor.execute(createOutboxCleanUpTask(SsfOutboxKinds.PUSH, scope, scopeId, sessionFactory));
            executor.execute(createOutboxCleanUpTask(SsfOutboxKinds.POLL, scope, scopeId, sessionFactory));
        } catch (RuntimeException e) {
            // Never block the originating admin transaction on
            // scheduling failures. Orphan rows will be handled by the
            // drainer's pendingMaxAge backstop and retention purges.
            log.warnf(e, "SSF outbox cleanup scheduling failed. scope=%s id=%s", scope, scopeId);
        }
    }

    protected OutboxCleanupTask createOutboxCleanUpTask(String entryKind,
                                                        OutboxCleanupTask.Scope scope,
                                                        String key,
                                                        KeycloakSessionFactory sessionFactory) {
        return new OutboxCleanupTask(sessionFactory, this::createOutboxStore, entryKind, scope, key);
    }

    /**
     * Rejects any client save that would leave two clients in the same
     * realm holding identical {@code ssf.streamId} attributes. Fires
     * from the {@code ClientUpdatedEvent} hook that
     * {@link org.keycloak.models.utils.RepresentationToModel#createClient
     * createClient} and the representation-based update path both
     * publish after all attributes have been set — which is where a
     * cloned JSON export would surface the duplicate.
     *
     * <p>Throws {@link ModelDuplicateException} on collision so the
     * offending import/update is rolled back with a clear error
     * instead of having SSF state silently rewritten. Delete-then-
     * reimport of the same receiver keeps working because the original
     * is gone by the time the new one is saved.
     *
     * <p>No-op when the attribute is absent or unique in the realm.
     * Serves as the primary defence against the JSON import/export
     * collision; the secondary check in
     * {@link ClientStreamStore#findClientByStreamId} catches anything
     * that gets past this (e.g. direct DB writes).
     */
    protected void validateImportedStreamId(KeycloakSession session, ClientModel client) {
        if (client == null) {
            return;
        }
        String streamId = client.getAttribute(ClientStreamStore.SSF_STREAM_ID_KEY);
        if (streamId == null || streamId.isBlank()) {
            return;
        }
        RealmModel realm = client.getRealm();
        if (realm == null) {
            return;
        }
        // Raise the page size above 1 so a collision is actually
        // visible — we only need to know whether at least one *other*
        // client shares the id.
        boolean duplicate = session.clients()
                .searchClientsByAttributes(realm, Map.of(ClientStreamStore.SSF_STREAM_ID_KEY, streamId), 0, 2)
                .map(ClientModel::getId)
                .anyMatch(id -> !id.equals(client.getId()));
        if (!duplicate) {
            return;
        }
        throw new ModelDuplicateException(String.format(
                "SSF stream id '%s' is already in use by another client in realm '%s'. "
                        + "Two clients cannot share the same ssf.streamId — revise the client "
                        + "configuration (or delete the colliding receiver before re-importing).",
                streamId, realm.getName()));
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.SSF);
    }

}
