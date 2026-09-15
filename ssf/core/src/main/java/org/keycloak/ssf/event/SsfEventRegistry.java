package org.keycloak.ssf.event;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.keycloak.models.KeycloakSession;
import org.keycloak.ssf.event.stream.SsfStreamUpdatedEvent;
import org.keycloak.ssf.event.stream.SsfStreamVerificationEvent;

/**
 * Immutable registry of all known SSF event types and their aliases.
 *
 * <p>The registry is built once at server startup by aggregating the events
 * contributed by every registered {@link SsfEventProviderFactory} and is then
 * exposed through {@link SsfEventProvider#getRegistry()}.
 */
public final class SsfEventRegistry {

    private final Map<String, Class<? extends SsfEvent>> classByEventType;

    private final Map<String, Class<? extends SsfEvent>> classByAlias;

    private final Map<String, String> aliasByEventType;

    private final Map<String, String> eventTypeByAlias;

    private final Map<String, Supplier<? extends SsfEvent>> factoryByEventType;

    private final Set<String> emittableEventTypes;

    private final Set<String> nativelyEmittedEventTypes;

    SsfEventRegistry(
            Map<String, Class<? extends SsfEvent>> classByEventType,
            Map<String, Class<? extends SsfEvent>> classByAlias,
            Map<String, String> aliasByEventType,
            Map<String, String> eventTypeByAlias,
            Map<String, Supplier<? extends SsfEvent>> factoryByEventType,
            Set<String> emittableEventTypes,
            Set<String> nativelyEmittedEventTypes) {
        this.classByEventType = Collections.unmodifiableMap(classByEventType);
        this.classByAlias = Collections.unmodifiableMap(classByAlias);
        this.aliasByEventType = Collections.unmodifiableMap(aliasByEventType);
        this.eventTypeByAlias = Collections.unmodifiableMap(eventTypeByAlias);
        this.factoryByEventType = Collections.unmodifiableMap(factoryByEventType);
        this.emittableEventTypes = Collections.unmodifiableSet(emittableEventTypes);
        this.nativelyEmittedEventTypes = Collections.unmodifiableSet(nativelyEmittedEventTypes);
    }

    /**
     * Builds an aggregated registry from the contributions of the given
     * factories. Used by {@link SsfEventProviderFactory#buildRegistry}.
     */
    static SsfEventRegistry from(Collection<? extends SsfEventProviderFactory> factories) {

        Map<String, Class<? extends SsfEvent>> classByEventType = new HashMap<>();
        Map<String, Class<? extends SsfEvent>> classByAlias = new HashMap<>();
        Map<String, String> aliasByEventType = new HashMap<>();
        Map<String, String> eventTypeByAlias = new HashMap<>();
        Map<String, Supplier<? extends SsfEvent>> factoryByEventType = new HashMap<>();
        Set<String> emittableEventTypes = new LinkedHashSet<>();
        Set<String> nativelyEmittedEventTypes = new LinkedHashSet<>();

        for (SsfEventProviderFactory factory : factories) {
            for (Map.Entry<String, Supplier<? extends SsfEvent>> entry
                    : factory.getContributedEventFactories().entrySet()) {
                String eventType = entry.getKey();
                Supplier<? extends SsfEvent> eventFactory = entry.getValue();

                // Instantiate once at registry-build time to derive the
                // event class (used by Jackson as the deserialization
                // target) and the alias (explicit override or fallback
                // to the class' simple name). The factory is stored
                // alongside so callers that need fresh instances at
                // runtime (e.g. the synthetic event emitter) can invoke
                // eventFactory.get() directly without reflection.
                SsfEvent sample = eventFactory.get();
                Class<? extends SsfEvent> eventClass = sample.getClass();
                String alias = sample.getAlias() != null ? sample.getAlias() : eventClass.getSimpleName();

                classByEventType.put(eventType, eventClass);
                classByAlias.put(alias, eventClass);
                aliasByEventType.put(eventType, alias);
                eventTypeByAlias.put(alias, eventType);
                factoryByEventType.put(eventType, eventFactory);
            }
            emittableEventTypes.addAll(factory.getEmittableEventTypes());
            nativelyEmittedEventTypes.addAll(factory.getNativelyEmittedEventTypes());
        }

        return new SsfEventRegistry(classByEventType, classByAlias, aliasByEventType,
                eventTypeByAlias, factoryByEventType, emittableEventTypes,
                nativelyEmittedEventTypes);
    }

    /**
     * Returns the {@link SsfEvent} class for the given full event type URI,
     * or an empty {@link Optional} if the event type is not registered.
     */
    public Optional<Class<? extends SsfEvent>> getEventClassByType(String eventType) {
        return Optional.ofNullable(classByEventType.get(eventType));
    }

    /**
     * Returns the factory for creating fresh {@link SsfEvent} instances of
     * the given event type URI. Used by callers like the synthetic event
     * emitter to build a default event body without reaching for
     * reflection. Returns an empty {@link Optional} if the event type is
     * not registered.
     */
    public Optional<Supplier<? extends SsfEvent>> getEventFactoryByType(String eventType) {
        return Optional.ofNullable(factoryByEventType.get(eventType));
    }

    /**
     * Resolves the {@link SsfEvent} class for the given alias or full event
     * type URI. Returns {@code null} if neither matches a known event.
     */
    public Class<? extends SsfEvent> resolveEventClass(String aliasOrEventType) {
        Class<? extends SsfEvent> eventClass = classByAlias.get(aliasOrEventType);
        if (eventClass != null) {
            return eventClass;
        }
        return classByEventType.get(aliasOrEventType);
    }

    /**
     * Resolves the alias (e.g. {@code CaepCredentialChange}) for the given
     * full event type URI, or {@code null} if the event type is not registered.
     */
    public String resolveAliasForEventType(String eventType) {
        return aliasByEventType.get(eventType);
    }

    /**
     * Resolves the full event type URI for the given alias, or {@code null}
     * if the alias is not registered.
     */
    public String resolveEventTypeForAlias(String alias) {
        return eventTypeByAlias.get(alias);
    }

    /**
     * @return the full set of known event aliases (sorted).
     */
    public Set<String> getKnownAliases() {
        return Collections.unmodifiableSet(new TreeSet<>(eventTypeByAlias.keySet()));
    }

    /**
     * @return the full set of known event type URIs.
     */
    public Set<String> getKnownEventTypes() {
        return Collections.unmodifiableSet(classByEventType.keySet());
    }

    /**
     * Stream-internal lifecycle event types (verification SET, stream-updated SET).
     * The transmitter owns these end-to-end and external callers
     * (synthetic emit, admin UI) are intentionally not allowed to
     * forge them — letting an external caller fire a stream-updated
     * SET would let it spoof transmitter behaviour towards the
     * receiver. Both the synthetic emit gate and the admin UI's
     * "available supported events" list filter these out.
     */
    public static final Set<String> STREAM_LIFECYCLE_EVENT_TYPES = Set.of(
            SsfStreamVerificationEvent.TYPE,
            SsfStreamUpdatedEvent.TYPE);

    /**
     * Event types a receiver can legitimately request via
     * {@code events_requested} on stream-create / -update — i.e. the
     * full registry minus the {@link #STREAM_LIFECYCLE_EVENT_TYPES
     * protocol-internal lifecycle events} that only the transmitter
     * may produce. Drives the admin UI's "available supported events"
     * multi-select so operators can configure receivers to receive
     * <em>any</em> deliverable type — even events Keycloak doesn't
     * fire from native event listeners but that an external system
     * may produce via the synthetic emit endpoint.
     */
    public Set<String> getReceiverRequestableEventTypes() {
        Set<String> known = classByEventType.keySet();
        if (known.isEmpty()) {
            return Set.of();
        }
        java.util.Set<String> result = new java.util.LinkedHashSet<>(known.size());
        for (String type : known) {
            if (!STREAM_LIFECYCLE_EVENT_TYPES.contains(type)) {
                result.add(type);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Returns the subset of {@link #getReceiverRequestableEventTypes()}
     * the transmitter can ship out — either via a Keycloak listener / trigger
     * or via the admin emit API. Aggregates contributions from every
     * registered {@link SsfEventProviderFactory#getEmittableEventTypes()}.
     *
     * <p>Drives the default {@code events_supported} set advertised to receiver
     * clients and the whitelist of types the admin emit API will accept.
     */
    public Set<String> getEmittableEventTypes() {
        return emittableEventTypes;
    }

    /**
     * Returns the subset of {@link #getEmittableEventTypes()} that the
     * transmitter emits natively via Keycloak listener / trigger logic, as
     * declared by every registered
     * {@link SsfEventProviderFactory#getNativelyEmittedEventTypes()}.
     *
     * <p><b>Not an enforcement gate.</b> Purely informational — the admin UI
     * uses it to surface the "built-in" badge on event entries so operators
     * see which event types fire automatically from Keycloak vs which only
     * ship when something explicitly invokes the emit API.
     */
    public Set<String> getNativelyEmittedEventTypes() {
        return nativelyEmittedEventTypes;
    }

    public static Set<String> parseEventTypeAliases(String eventAliases) {
        return Set.copyOf(Stream.of(eventAliases.split(",")).map(String::trim).toList());
    }

    public static SsfEventRegistry of(KeycloakSession session) {
        SsfEventProvider eventsProvider = session.getProvider(SsfEventProvider.class);
        if (eventsProvider == null) {
            return null;
        }
        return eventsProvider.getRegistry();
    }
}
