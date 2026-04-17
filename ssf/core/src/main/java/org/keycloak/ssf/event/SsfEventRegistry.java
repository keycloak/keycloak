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

    SsfEventRegistry(
            Map<String, Class<? extends SsfEvent>> classByEventType,
            Map<String, Class<? extends SsfEvent>> classByAlias,
            Map<String, String> aliasByEventType,
            Map<String, String> eventTypeByAlias,
            Map<String, Supplier<? extends SsfEvent>> factoryByEventType,
            Set<String> emittableEventTypes) {
        this.classByEventType = Collections.unmodifiableMap(classByEventType);
        this.classByAlias = Collections.unmodifiableMap(classByAlias);
        this.aliasByEventType = Collections.unmodifiableMap(aliasByEventType);
        this.eventTypeByAlias = Collections.unmodifiableMap(eventTypeByAlias);
        this.factoryByEventType = Collections.unmodifiableMap(factoryByEventType);
        this.emittableEventTypes = Collections.unmodifiableSet(emittableEventTypes);
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
        }

        return new SsfEventRegistry(classByEventType, classByAlias, aliasByEventType,
                eventTypeByAlias, factoryByEventType, emittableEventTypes);
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
     * Returns the set of event type URIs that the transmitter can
     * actually emit, as declared by every registered
     * {@link SsfEventProviderFactory#getEmittableEventTypes()}. This is
     * the honest "default supported events" set advertised to receivers
     * that do not configure their own {@code ssf.supportedEvents}
     * attribute — it excludes events contributed to the registry purely
     * for inbound parsing on the receiver side.
     */
    public Set<String> getEmittableEventTypes() {
        return emittableEventTypes;
    }
}
