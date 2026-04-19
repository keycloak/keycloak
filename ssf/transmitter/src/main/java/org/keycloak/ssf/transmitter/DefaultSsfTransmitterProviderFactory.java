package org.keycloak.ssf.transmitter;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
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
import org.keycloak.ssf.transmitter.outbox.SsfPendingEventStore;
import org.keycloak.ssf.transmitter.outbox.SsfPushOutboxBackoff;
import org.keycloak.ssf.transmitter.outbox.SsfPushOutboxDrainerTask;
import org.keycloak.ssf.transmitter.stream.StreamVerificationService;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.ssf.transmitter.subject.SubjectManagementService;
import org.keycloak.ssf.transmitter.support.SsfUtil;
import org.keycloak.timer.TimerProvider;

import org.jboss.logging.Logger;

public class DefaultSsfTransmitterProviderFactory implements SsfTransmitterProviderFactory {

    private static final Logger log = Logger.getLogger(DefaultSsfTransmitterProviderFactory.class);

    public static final String CONFIG_SUPPORTED_EVENTS = "supported-events";

    public static final String CONFIG_OUTBOX_DRAINER_INTERVAL = "outbox-drainer-interval";

    public static final String CONFIG_OUTBOX_DRAINER_BATCH_SIZE = "outbox-drainer-batch-size";

    public static final String CONFIG_OUTBOX_DRAINER_MAX_ATTEMPTS = "outbox-drainer-max-attempts";

    public static final String CONFIG_OUTBOX_DEAD_LETTER_RETENTION = "outbox-dead-letter-retention";

    public static final long DEFAULT_OUTBOX_DRAINER_INTERVAL_MILLIS = Duration.ofSeconds(30).toMillis();

    public static final int DEFAULT_OUTBOX_DRAINER_BATCH_SIZE = 50;

    /**
     * Default retention for {@code DEAD_LETTER} outbox rows — 30 days. Set
     * to {@code 0} to disable the purge and retain dead-letters
     * indefinitely (e.g. for audit/forensic use cases).
     */
    public static final long DEFAULT_OUTBOX_DEAD_LETTER_RETENTION_MILLIS = Duration.ofDays(30).toMillis();

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

    protected int outboxDrainerMaxAttempts = SsfPushOutboxBackoff.DEFAULT_MAX_ATTEMPTS;

    protected long outboxDeadLetterRetentionMillis = DEFAULT_OUTBOX_DEAD_LETTER_RETENTION_MILLIS;

    /**
     * Shared metrics binder — constructed once at factory init time and
     * reused across every session-scoped dispatcher, the long-lived
     * drainer task, and any on-demand lookups (poll endpoint).
     * Defaults to {@link SsfMetricsBinder#NOOP} until
     * {@link #init(Config.Scope)} decides whether metrics are enabled.
     */
    protected SsfMetricsBinder metricsBinder = SsfMetricsBinder.NOOP;

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public SsfTransmitterProvider create(KeycloakSession session) {
        var transmitterConfig = getTransmitterConfig();
        var mapper = createSecurityEventTokenMapper(session, transmitterConfig);
        var encoder = createSecurityEventTokenEncoder(session);
        var pushDelivery = createPushDeliveryService(session, transmitterConfig);
        var dispatcher = createSecurityEventTokenDispatcher(session, encoder, pushDelivery, transmitterConfig);
        var streamStore = createClientStreamStore(session);
        var verificationService = createVerificationService(session, streamStore, mapper, dispatcher);
        var transmitterMetadataService = createTransmitterMetadataService(session, transmitterConfig);
        var subjectManagementService = createSubjectManagementService(session);
        var pollDeliveryService = createPollDeliveryService(session, transmitterConfig);
        return createTransmitter(session, transmitterMetadataService, verificationService, subjectManagementService,
                mapper, dispatcher, transmitterConfig, streamStore, this::createSsfPendingEventStore, pollDeliveryService);
    }

    protected SsfTransmitterProvider createTransmitter(KeycloakSession session,
                                                       TransmitterMetadataService transmitterMetadataService,
                                                       StreamVerificationService verificationService,
                                                       SubjectManagementService subjectManagementService,
                                                       SecurityEventTokenMapper mapper,
                                                       SecurityEventTokenDispatcher dispatcher,
                                                       SsfTransmitterConfig transmitterConfig,
                                                       ClientStreamStore streamStore,
                                                       Function<KeycloakSession, SsfPendingEventStore> pendingSsfEventStoreFactory,
                                                       PollDeliveryService pollDeliveryService) {

        return new DefaultSsfTransmitterProvider(session, transmitterMetadataService, verificationService, mapper,
                dispatcher, transmitterConfig, configuredDefaultSupportedEventAliases, streamStore,
                subjectManagementService, pendingSsfEventStoreFactory, metricsBinder, pollDeliveryService);
    }

    protected SubjectManagementService createSubjectManagementService(KeycloakSession session) {
        return new SubjectManagementService(session);
    }

    protected TransmitterMetadataService createTransmitterMetadataService(KeycloakSession session, SsfTransmitterConfig transmitterConfig) {
        return new TransmitterMetadataService(session, this::createSsfIssuerUrl, transmitterConfig);
    }

    protected StreamVerificationService createVerificationService(KeycloakSession session, ClientStreamStore streamStore, SecurityEventTokenMapper mapper, SecurityEventTokenDispatcher dispatcher) {
        return new StreamVerificationService(session, streamStore, mapper, dispatcher, metricsBinder);
    }

    protected ClientStreamStore createClientStreamStore(KeycloakSession session) {
        return new ClientStreamStore(session);
    }

    protected SecurityEventTokenDispatcher createSecurityEventTokenDispatcher(KeycloakSession session, SecurityEventTokenEncoder encoder, PushDeliveryService pushDelivery, SsfTransmitterConfig transmitterConfig) {
        return new SecurityEventTokenDispatcher(session, encoder, pushDelivery, transmitterConfig, this::createSsfPendingEventStore, metricsBinder);
    }

    protected PushDeliveryService createPushDeliveryService(KeycloakSession session, SsfTransmitterConfig transmitterConfig) {
        return new PushDeliveryService(session, transmitterConfig);
    }

    protected SecurityEventTokenEncoder createSecurityEventTokenEncoder(KeycloakSession session) {
        return new SecurityEventTokenEncoder(session);
    }

    protected SecurityEventTokenMapper createSecurityEventTokenMapper(KeycloakSession session, SsfTransmitterConfig transmitterConfig) {
        return new SecurityEventTokenMapper(session, transmitterConfig, this::createSsfIssuerUrl);
    }

    protected PollDeliveryService createPollDeliveryService(KeycloakSession session, SsfTransmitterConfig transmitterConfig) {
        return new PollDeliveryService(session, createSsfPendingEventStore(session), metricsBinder);
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

    @Override
    public SsfTransmitterConfig getTransmitterConfig() {
        return transmitterConfig;
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
                .defaultValue(SsfPushOutboxBackoff.DEFAULT_MAX_ATTEMPTS)
                .add()
                .property()
                .name(CONFIG_OUTBOX_DEAD_LETTER_RETENTION)
                .type("string")
                .helpText("How long DEAD_LETTER outbox rows are retained before the drainer purges them. Accepts suffixes ms, s, m, h (default 30d equivalent). Set to 0 to retain dead-letters indefinitely.")
                .defaultValue(DEFAULT_OUTBOX_DEAD_LETTER_RETENTION_MILLIS + "ms")
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
        return SsfUtil.parseEventTypeAliases(supportedEventsString);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        scheduleOutboxDrainer(factory);
        factory.register(outboxRealmRemovedPurgeListener);
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
            SsfPushOutboxDrainerTask task = createDrainerTask(session);
            ScheduledTaskRunner runner = createDrainerScheduledTaskRunner(factory, task);
            timer.schedule(runner, outboxDrainerIntervalMillis, outboxDrainerIntervalMillis, task.getClass().getSimpleName());
            log.infof("SSF push outbox drainer scheduled: interval=%dms, batchSize=%d, maxAttempts=%d, deadLetterRetention=%s",
                    outboxDrainerIntervalMillis, outboxDrainerBatchSize, outboxDrainerMaxAttempts,
                    outboxDeadLetterRetentionMillis > 0 ? outboxDeadLetterRetentionMillis + "ms" : "disabled");
        }
    }

    protected ScheduledTaskRunner createDrainerScheduledTaskRunner(KeycloakSessionFactory factory, SsfPushOutboxDrainerTask task) {
        return new ClusterAwareScheduledTaskRunner(factory, task, outboxDrainerIntervalMillis);
    }

    protected SsfPushOutboxDrainerTask createDrainerTask(KeycloakSession session) {
        SsfPushOutboxBackoff backoff = new SsfPushOutboxBackoff(outboxDrainerMaxAttempts);
        Duration deadLetterRetention = outboxDeadLetterRetentionMillis > 0
                ? Duration.ofMillis(outboxDeadLetterRetentionMillis)
                : null;

        return new SsfPushOutboxDrainerTask(outboxDrainerBatchSize, backoff, deadLetterRetention,
                this::createSsfPendingEventStore, getTransmitterConfig(), this::createPushDeliveryService,
                metricsBinder);
    }

    /**
     * Cascade-purges outbox rows when a realm is removed. REALM_ID is not
     * a foreign key on the SSF_PENDING_EVENT table (the table is
     * plugin-contributed via JpaEntityProvider, so it can't declare FKs
     * into core Keycloak tables), so the cleanup is handled explicitly
     * in-transaction with the realm delete.
     */
    private final ProviderEventListener outboxRealmRemovedPurgeListener = new ProviderEventListener() {
        @Override
        public void onEvent(ProviderEvent event) {
            if (event instanceof RealmModel.RealmRemovedEvent ev) {
                try {
                    createSsfPendingEventStore(ev.getKeycloakSession())
                            .deleteByRealm(ev.getRealm().getId());
                } catch (RuntimeException e) {
                    // Don't block realm removal on outbox cleanup failures —
                    // orphaned rows are harmless (the drainer dead-letters
                    // them once it can't resolve the realm) and we don't
                    // want to leave the realm half-deleted.
                    log.warnf(e, "SSF outbox realm-removed cleanup failed for realm %s",
                            ev.getRealm().getId());
                }
            }
        }
    };

    /**
     * Session-scoped factory for the outbox DAO. Extension point for
     * deployments that want to plug in a custom {@link SsfPendingEventStore}
     * subclass (e.g. for instrumentation or schema overrides) — the
     * default is {@code SsfPendingEventStore::new}.
     */
    protected SsfPendingEventStore createSsfPendingEventStore(KeycloakSession session) {
        return new SsfPendingEventStore(session);
    }

    protected String createSsfIssuerUrl(KeycloakSession session) {
        return SsfUtil.getIssuerUrl(session);
    }

    @Override
    public void close() {
        // NOOP
    }


    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.SSF);
    }

}
