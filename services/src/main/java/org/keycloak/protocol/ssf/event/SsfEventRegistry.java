package org.keycloak.protocol.ssf.event;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

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

    private final Set<String> emittableEventTypes;

    SsfEventRegistry(
            Map<String, Class<? extends SsfEvent>> classByEventType,
            Map<String, Class<? extends SsfEvent>> classByAlias,
            Map<String, String> aliasByEventType,
            Map<String, String> eventTypeByAlias,
            Set<String> emittableEventTypes) {
        this.classByEventType = Collections.unmodifiableMap(classByEventType);
        this.classByAlias = Collections.unmodifiableMap(classByAlias);
        this.aliasByEventType = Collections.unmodifiableMap(aliasByEventType);
        this.eventTypeByAlias = Collections.unmodifiableMap(eventTypeByAlias);
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
        Set<String> emittableEventTypes = new LinkedHashSet<>();

        for (SsfEventProviderFactory factory : factories) {
            for (SsfEvent event : factory.getContributedEvents()) {
                Class<? extends SsfEvent> eventClass = event.getClass();
                String eventType = event.getEventType();
                String alias = event.getAlias() != null ? event.getAlias() : eventClass.getSimpleName();

                classByEventType.put(eventType, eventClass);
                classByAlias.put(alias, eventClass);
                aliasByEventType.put(eventType, alias);
                eventTypeByAlias.put(alias, eventType);
            }
            emittableEventTypes.addAll(factory.getEmittableEventTypes());
        }

        return new SsfEventRegistry(classByEventType, classByAlias, aliasByEventType, eventTypeByAlias, emittableEventTypes);
    }

    /**
     * Returns the {@link SsfEvent} class for the given full event type URI,
     * or an empty {@link Optional} if the event type is not registered.
     */
    public Optional<Class<? extends SsfEvent>> getEventClassByType(String eventType) {
        return Optional.ofNullable(classByEventType.get(eventType));
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
