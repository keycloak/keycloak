package org.keycloak.ssf.transmitter;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.keycloak.models.KeycloakSession;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.event.SsfEventRegistry;
import org.keycloak.ssf.transmitter.delivery.SecurityEventTokenDispatcher;
import org.keycloak.ssf.transmitter.delivery.poll.PollDeliveryService;
import org.keycloak.ssf.transmitter.delivery.push.PushDeliveryService;
import org.keycloak.ssf.transmitter.emit.EventEmitterService;
import org.keycloak.ssf.transmitter.event.SecurityEventTokenEncoder;
import org.keycloak.ssf.transmitter.event.SecurityEventTokenMapper;
import org.keycloak.ssf.transmitter.metadata.TransmitterMetadataService;
import org.keycloak.ssf.transmitter.metrics.SsfMetricsBinder;
import org.keycloak.ssf.transmitter.resources.SsfStreamManagementResource;
import org.keycloak.ssf.transmitter.resources.SsfStreamStatusResource;
import org.keycloak.ssf.transmitter.resources.SsfStreamVerificationResource;
import org.keycloak.ssf.transmitter.resources.SsfSubjectManagementResource;
import org.keycloak.ssf.transmitter.stream.StreamService;
import org.keycloak.ssf.transmitter.stream.StreamVerificationService;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.ssf.transmitter.subject.SsfSubjectInclusionResolver;
import org.keycloak.ssf.transmitter.subject.SubjectManagementService;

/**
 * Default per-session SSF transmitter provider. Constructed once per
 * {@link KeycloakSession} by {@link DefaultSsfTransmitterProviderFactory#create};
 * holds lazy-initialized references to the per-session services so a
 * request that touches only the dispatcher (for example) doesn't pay
 * the cost of building the verification service or the stream store.
 *
 * <p>All long-lived state lives on the factory-scoped
 * {@link SsfTransmitterContext}, which this provider takes by
 * reference. The two-arg constructor lets the factory's {@code create}
 * method stay a one-liner — and lets future shared collaborators get
 * added via the context without changing the provider's constructor
 * shape.
 *
 * <p><b>Threading:</b> assumes session-scoped, single-threaded use
 * (the standard Keycloak SPI lifetime). Lazy-init fields are
 * intentionally not synchronized; if a future caller shares a provider
 * across threads, behavior is undefined.
 */
public class DefaultSsfTransmitterProvider implements SsfTransmitterProvider {

    protected final KeycloakSession session;

    protected final SsfTransmitterContext context;

    // -- lazy-built per-session services ----------------------------------
    private SecurityEventTokenEncoder encoder;
    private SecurityEventTokenMapper mapper;
    private PushDeliveryService pushDelivery;
    private SecurityEventTokenDispatcher dispatcher;
    private ClientStreamStore streamStore;
    private TransmitterMetadataService metadata;
    private StreamVerificationService verification;
    private SubjectManagementService subjectMgmt;
    private SsfSubjectInclusionResolver subjectInclusionResolver;
    private PollDeliveryService pollDelivery;

    public DefaultSsfTransmitterProvider(KeycloakSession session, SsfTransmitterContext context) {
        this.session = session;
        this.context = context;
    }

    @Override
    public KeycloakSession session() {
        return session;
    }

    @Override
    public SsfTransmitterContext context() {
        return context;
    }

    // -- leaf services (built directly from session + context) ------------

    @Override
    public SecurityEventTokenMapper securityEventTokenMapper() {
        if (mapper == null) {
            mapper = context.services().createMapper(session, context);
        }
        return mapper;
    }

    public SecurityEventTokenEncoder securityEventTokenEncoder() {
        if (encoder == null) {
            encoder = context.services().createEncoder(session, context);
        }
        return encoder;
    }

    public org.keycloak.ssf.transmitter.delivery.push.PushDeliveryService pushDeliveryService() {
        if (pushDelivery == null) {
            pushDelivery = context.services().createPushDelivery(session, context);
        }
        return pushDelivery;
    }

    @Override
    public ClientStreamStore streamStore() {
        if (streamStore == null) {
            streamStore = context.services().createStreamStore(session, context);
        }
        return streamStore;
    }

    @Override
    public TransmitterMetadataService metadataService() {
        if (metadata == null) {
            metadata = context.services().createMetadataService(session, context);
        }
        return metadata;
    }

    @Override
    public SubjectManagementService subjectManagementService() {
        if (subjectMgmt == null) {
            subjectMgmt = context.services().createSubjectManagement(session, context);
        }
        return subjectMgmt;
    }

    @Override
    public SsfSubjectInclusionResolver subjectInclusionResolver() {
        if (subjectInclusionResolver == null) {
            subjectInclusionResolver = context.services().createSubjectInclusionResolver(session, context);
        }
        return subjectInclusionResolver;
    }

    // -- composite services (build via SsfTransmitterServiceBuilder) -----

    @Override
    public SecurityEventTokenDispatcher securityEventTokenDispatcher() {
        if (dispatcher == null) {
            dispatcher = context.services().createDispatcher(this);
        }
        return dispatcher;
    }

    @Override
    public StreamVerificationService verificationService() {
        if (verification == null) {
            verification = context.services().createVerification(this);
        }
        return verification;
    }

    @Override
    public PollDeliveryService pollDeliveryService() {
        if (pollDelivery == null) {
            pollDelivery = context.services().createPollDelivery(this);
        }
        return pollDelivery;
    }

    // -- per-request orchestrators (cheap; built fresh on every call) ----

    @Override
    public StreamService streamService() {
        return new StreamService(session, this, streamStore(), metadataService(), verificationService(),
                context.outboxStoreFactory());
    }

    @Override
    public EventEmitterService eventEmitterService() {
        return new EventEmitterService(session, streamStore(), securityEventTokenMapper(),
                securityEventTokenDispatcher(), subjectInclusionResolver());
    }

    @Override
    public SsfStreamManagementResource streamManagementResource() {
        return new SsfStreamManagementResource(session, streamService());
    }

    @Override
    public SsfStreamStatusResource streamStatusResource() {
        return new SsfStreamStatusResource(session, streamService());
    }

    @Override
    public SsfStreamVerificationResource streamVerificationResource() {
        return new SsfStreamVerificationResource(session, verificationService(), context.config(), streamStore(), context.metrics());
    }

    @Override
    public SsfSubjectManagementResource subjectManagementResource() {
        return new SsfSubjectManagementResource(session, subjectManagementService(), false);
    }

    // -- registry-backed accessors ---------------------------------------

    @Override
    public Set<String> getDefaultSupportedEvents() {
        SsfEventRegistry registry = registry();
        Set<String> aliases = context.defaultSupportedEventAliases();
        if (aliases == null) {
            // No explicit SPI configuration — advertise only the events
            // the transmitter can actually emit, as declared by every
            // registered SsfEventProviderFactory#getEmittableEventTypes.
            // This naturally picks up events contributed by custom SPI
            // extensions that also wire up an emission path.
            return registry.getEmittableEventTypes();
        }

        // SPI config is set — resolve each configured entry (which may be
        // either an alias or a full URI) to its canonical event type URI
        // via the registry. Unknown entries are silently dropped.
        Set<String> resolved = new LinkedHashSet<>();
        for (String candidate : aliases) {
            String eventType = registry.resolveEventTypeForAlias(candidate);
            if (eventType == null && registry.getEventClassByType(candidate).isPresent()) {
                eventType = candidate;
            }
            if (eventType != null) {
                resolved.add(eventType);
            }
        }
        return resolved;
    }

    @Override
    public String resolveAliasForEventType(String eventType) {
        return registry().resolveAliasForEventType(eventType);
    }

    @Override
    public Set<String> getAvailableEventAliases() {
        return toAliases(registry(), registry().getReceiverRequestableEventTypes());
    }

    @Override
    public Set<String> getNativelyEmittedEventAliases() {
        return toAliases(registry(), registry().getNativelyEmittedEventTypes());
    }

    /**
     * Converts a set of event-type URIs to their receiver-friendly
     * aliases. Falls back to the URI for any type without a registered
     * alias so unknown / custom types stay visible in the UI rather
     * than being silently dropped.
     *
     * <p>Returns a {@link TreeSet} so the alias order is deterministic
     * (alphabetical) across calls. The underlying registry sources
     * the URI list from a {@link java.util.HashMap}, whose iteration
     * order can shift between processes; without the sort, admin-UI
     * dropdowns would shuffle on every restart.
     */
    protected Set<String> toAliases(SsfEventRegistry registry, Set<String> eventTypes) {
        Set<String> aliases = new TreeSet<>();
        for (String eventType : eventTypes) {
            String alias = registry.resolveAliasForEventType(eventType);
            aliases.add(alias != null ? alias : eventType);
        }
        return aliases;
    }

    protected SsfEventRegistry registry() {
        return Ssf.events().getRegistry();
    }

    @Override
    public SsfTransmitterConfig getConfig() {
        return context.config();
    }

    @Override
    public SsfMetricsBinder metrics() {
        return context.metrics();
    }
}
