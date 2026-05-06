package org.keycloak.ssf.event;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.ssf.event.caep.CaepCredentialChange;
import org.keycloak.ssf.event.caep.CaepSessionRevoked;
import org.keycloak.ssf.event.stream.SsfStreamUpdatedEvent;
import org.keycloak.ssf.event.stream.SsfStreamVerificationEvent;

/**
 * Default {@link SsfEventProviderFactory} that contributes the built-in set
 * of standard SSF / CAEP events.
 *
 * <p>This factory is also responsible for assembling the global
 * {@link SsfEventRegistry} from the contributions of every registered
 * factory in {@link #postInit(KeycloakSessionFactory)}.
 */
public class DefaultSsfEventProviderFactory implements SsfEventProviderFactory, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "default";

    /**
     * Standard event contributions keyed by event type URI. The value is a
     * {@code ::new} method reference so the registry can instantiate fresh
     * instances at runtime (e.g. for the synthetic event emitter) without
     * reflection.
     *
     * <p>{@link LinkedHashMap} to preserve insertion order for predictable
     * iteration when introspecting the registry (e.g. in the admin UI's
     * supported-events list).
     */
    private static final Map<String, Supplier<? extends SsfEvent>> STANDARD_EVENT_FACTORIES;

    static {
        Map<String, Supplier<? extends SsfEvent>> events = new LinkedHashMap<>();

        // SSF Stream events
        events.put(SsfStreamVerificationEvent.TYPE, SsfStreamVerificationEvent::new);
        events.put(SsfStreamUpdatedEvent.TYPE, SsfStreamUpdatedEvent::new);

        // CAEP events
        events.put(CaepCredentialChange.TYPE, CaepCredentialChange::new);
        events.put(CaepSessionRevoked.TYPE, CaepSessionRevoked::new);

        STANDARD_EVENT_FACTORIES = Map.copyOf(events);
    }

    /**
     * Subset of {@link #STANDARD_EVENT_FACTORIES} the transmitter can ship out.
     * Either {@code SecurityEventTokenMapper} produces them from Keycloak
     * events, or operators can raise them on demand via the admin emit API.
     * Every other built-in event in the map is contributed to the registry
     * only so the receiver-side parser can decode incoming SETs of that type.
     *
     * <p>The two types here are use-cases enumerated by the OpenID CAEP
     * Interoperability Profile 1.0: {@code session-revoked} and
     * {@code credential-change}. The profile is opt-in per use-case
     * ("Implementations MAY choose to support one or more …"), so supporting
     * any one of them is enough to count as interoperable.
     *
     * @see <a href="https://openid.github.io/sharedsignals/openid-caep-interoperability-profile-1_0.html">OpenID CAEP Interoperability Profile 1.0</a>
     */
    public static final Set<String> EMITTABLE_EVENT_TYPES = Set.of(
            CaepCredentialChange.TYPE,
            CaepSessionRevoked.TYPE);

    /**
     * Subset of {@link #EMITTABLE_EVENT_TYPES} that {@code SecurityEventTokenMapper}
     * actually produces natively from Keycloak listener events. Drives the
     * "natively emitted" badge in the admin UI.
     */
    public static final Set<String> NATIVELY_EMITTED_EVENT_TYPES = Set.of(
            CaepCredentialChange.TYPE,
            CaepSessionRevoked.TYPE);

    private volatile SsfEventRegistry registry;

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Map<String, Supplier<? extends SsfEvent>> getContributedEventFactories() {
        return STANDARD_EVENT_FACTORIES;
    }

    @Override
    public Set<String> getEmittableEventTypes() {
        return EMITTABLE_EVENT_TYPES;
    }

    @Override
    public Set<String> getNativelyEmittedEventTypes() {
        return NATIVELY_EMITTED_EVENT_TYPES;
    }

    @Override
    public SsfEventProvider create(KeycloakSession session) {
        return new DefaultSsfEventProvider(registry);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        this.registry = SsfEventProviderFactory.buildRegistry(factory);
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.SSF);
    }
}
