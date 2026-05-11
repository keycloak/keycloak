package org.keycloak.ssf.transmitter;

import java.util.Set;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;
import org.keycloak.ssf.event.SsfEventProviderFactory;
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
import org.keycloak.ssf.transmitter.stream.storage.SsfStreamStore;
import org.keycloak.ssf.transmitter.subject.SsfSubjectInclusionResolver;
import org.keycloak.ssf.transmitter.subject.SubjectManagementService;
import org.keycloak.ssf.transmitter.support.SsfPushUrlValidator;

/**
 * Provider for the SSF (Shared Signals Framework) Transmitter.
 *
 * <p>The transmitter is responsible for generating and delivering Security Event Tokens (SETs)
 * to registered SSF receivers via configured streams. It exposes services for stream management,
 * stream verification, event mapping, and event dispatching, as well as the JAX-RS sub-resources
 * that implement the SSF Transmitter REST API.
 *
 * @see <a href="https://openid.github.io/sharedsignals/openid-sharedsignals-framework-1_0.html">OpenID Shared Signals Framework 1.0</a>
 */
public interface SsfTransmitterProvider extends Provider {

    default void close() {
    }

    /**
     * Returns the service for handling stream verification requests.
     *
     * @return the stream verification service
     */
    StreamVerificationService verificationService();

    /**
     * Returns the service for managing transmitter metadata,
     * such as supported events, delivery methods, and the transmitter configuration endpoint.
     *
     * @return the transmitter metadata service
     */
    TransmitterMetadataService metadataService();

    /**
     * Returns the mapper that converts Keycloak events (user events, admin events)
     * into SSF Security Event Tokens (SETs).
     *
     * @return the security event token mapper
     */
    SecurityEventTokenMapper securityEventTokenMapper();

    /**
     * Returns the dispatcher responsible for delivering Security Event Tokens
     * to all applicable streams based on their delivery configuration.
     *
     * @return the security event token dispatcher
     */
    SecurityEventTokenDispatcher securityEventTokenDispatcher();

    /**
     * Returns the JWS encoder used to sign outgoing Security Event Tokens.
     * Cached per session — composite service builders pull this via the
     * provider so they share one encoder instance with the dispatcher.
     */
    SecurityEventTokenEncoder securityEventTokenEncoder();

    /**
     * Returns the HTTP push delivery service. Cached per session for
     * the same reason as {@link #securityEventTokenEncoder}.
     */
    PushDeliveryService pushDeliveryService();

    /**
     * Returns the service responsible for managing subjects (users, clients, etc.)
     * within the SSF framework.
     *
     * @return the subject management service
     */
    SubjectManagementService subjectManagementService();

    /**
     * Returns the read-side gate that decides whether a user / org
     * counts as a subscribed subject for a given receiver. Drives the
     * dispatcher's subject-selection filter and the synthetic-emit
     * dispatchability check. Default implementation reads the
     * {@code ssf.notify.<receiverClientId>} attribute via
     * {@link org.keycloak.ssf.transmitter.subject.SsfNotifyAttributes};
     * extensions plug additional inclusion sources (group attributes,
     * roles, external policy services) by overriding either this
     * accessor on a custom {@link SsfTransmitterProvider} subclass or
     * {@link SsfTransmitterServiceBuilder#createSubjectInclusionResolver}
     * on a custom service builder.
     */
    SsfSubjectInclusionResolver subjectInclusionResolver();

    /**
     * Returns the service that pushes synthetic SSF events injected by
     * a trusted IAM management client through the normal dispatch
     * pipeline. Backs the {@code /admin/realms/{realm}/ssf/clients/{id}/events/emit}
     * admin endpoint.
     */
    EventEmitterService eventEmitterService();

    /**
     * Returns the JAX-RS sub-resource for stream CRUD operations (create, read, update, delete).
     *
     * @return the stream management endpoint
     */
    SsfStreamManagementResource streamManagementResource();

    /**
     * Returns the service responsible for storing and retrieving SSF stream configurations.
     *
     * @return the SSF stream store service
     */
    SsfStreamStore streamStore();

    /**
     * Returns the service for managing SSF streams (create, update, delete, lookup).
     *
     * @return the stream service
     */
    StreamService streamService();

    /**
     * Returns the service for handling poll requests.
     * @return
     */
    PollDeliveryService pollDeliveryService();

    /**
     * Returns the JAX-RS sub-resource for querying and updating stream status.
     *
     * @return the stream status endpoint
     */
    SsfStreamStatusResource streamStatusResource();

    /**
     * Returns the JAX-RS sub-resource for triggering stream verification.
     *
     * @return the stream verification endpoint
     */
    SsfStreamVerificationResource streamVerificationResource();

    /**
     * Returns the JAX-RS sub-resource for subject management
     * (add/remove subject).
     *
     * @return the subject management endpoint
     */
    SsfSubjectManagementResource subjectManagementResource();

    /**
     * The default set of supported events.
     * @return
     */
    Set<String> getDefaultSupportedEvents();

    /**
     * Resolves the event alias (e.g. {@code CaepCredentialChange}) for the given
     * full event type URI. Returns {@code null} if the transmitter does not know
     * an alias for the given event type — callers can then fall back to the
     * original URI.
     *
     * <p>The default implementation delegates to the global
     * {@link SsfEventRegistry}, which is
     * populated by every registered
     * {@link SsfEventProviderFactory}; extensions
     * can therefore add custom event types and aliases without subclassing the
     * transmitter.
     *
     * @param eventType the long event type URI
     * @return the matching event alias, or {@code null} if unknown
     */
    String resolveAliasForEventType(String eventType);

    /**
     * Aliases of every event type a receiver can legitimately
     * <em>request</em> via {@code events_requested} on stream-create —
     * the full registry minus the protocol-internal lifecycle events
     * (verification SET, stream-updated SET) that only the transmitter
     * may produce. Drives the admin UI's "available supported events"
     * multi-select. Includes event types Keycloak doesn't fire from
     * native event listeners but that an external system may emit
     * via the synthetic emit endpoint or a custom mapper.
     */
    Set<String> getAvailableEventAliases();

    /**
     * Aliases of the subset of {@link #getAvailableEventAliases()}
     * that some registered {@link SsfEventProviderFactory} declares
     * as <em>natively emitted</em> (i.e. the events Keycloak fires
     * automatically from native event listeners — credential change,
     * session revoked, etc.). Used by the admin UI as a "natively
     * emitted" badge, not as a delivery gate. Synthetic emit can fire
     * any event in {@link #getAvailableEventAliases()}, including
     * ones outside this set.
     */
    Set<String> getNativelyEmittedEventAliases();

    /**
     * Returns the immutable transmitter-wide configuration snapshot that is
     * sourced from the {@link SsfTransmitterProviderFactory} SPI configuration.
     * Consumers should use this to access the effective default push endpoint
     * timeouts and the transmitter-initiated verification delay.
     */
    SsfTransmitterConfig getConfig();

    /**
     * Returns the shared Prometheus metrics binder for the SSF
     * transmitter. Hot paths that weren't constructed with a direct
     * binder reference (in particular the poll endpoint, which is
     * constructed per request) resolve it through this accessor.
     * Always non-null — returns
     * {@link SsfMetricsBinder#NOOP} when metrics are disabled.
     */
    SsfMetricsBinder metrics();

    /**
     * Returns the {@link KeycloakSession} this provider instance is
     * scoped to. Exposed on the interface so composite service
     * builders (e.g.
     * {@link SsfTransmitterServiceBuilder#createDispatcher}) can wire
     * the session into the services they construct without
     * downcasting to the default provider implementation.
     */
    KeycloakSession session();

    /**
     * Returns the factory-scoped {@link SsfTransmitterContext}
     * shared across every per-session provider instance. Carries the
     * long-lived configuration, the metrics binder, the pending-event
     * store factory, and the issuer-URL factory.
     */
    SsfTransmitterContext context();

    /**
     * Returns the shared SSRF gate for receiver-supplied push URLs.
     * Owned by the {@link SsfTransmitterContext} so a single instance
     * is reused across every per-session provider; custom SPI
     * implementations can plug in a different validator by overriding
     * {@link SsfTransmitterServiceBuilder#createPushUrlValidator}.
     */
    default SsfPushUrlValidator pushUrlValidator() {
        return context().pushUrlValidator();
    }
}
