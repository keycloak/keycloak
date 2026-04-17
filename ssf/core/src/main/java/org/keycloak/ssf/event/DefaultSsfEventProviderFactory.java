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
import org.keycloak.ssf.event.caep.CaepAssuranceLevelChange;
import org.keycloak.ssf.event.caep.CaepCredentialChange;
import org.keycloak.ssf.event.caep.CaepDeviceComplianceChange;
import org.keycloak.ssf.event.caep.CaepSessionEstablished;
import org.keycloak.ssf.event.caep.CaepSessionPresented;
import org.keycloak.ssf.event.caep.CaepSessionRevoked;
import org.keycloak.ssf.event.caep.CaepTokenClaimsChanged;
import org.keycloak.ssf.event.risc.RiscAccountCredentialChangeRequired;
import org.keycloak.ssf.event.risc.RiscAccountDisabled;
import org.keycloak.ssf.event.risc.RiscAccountEnabled;
import org.keycloak.ssf.event.risc.RiscAccountPurged;
import org.keycloak.ssf.event.risc.RiscCredentialCompromise;
import org.keycloak.ssf.event.risc.RiscIdentifierChanged;
import org.keycloak.ssf.event.risc.RiscIdentifierRecycled;
import org.keycloak.ssf.event.risc.RiscOptIn;
import org.keycloak.ssf.event.risc.RiscOptOutCancelled;
import org.keycloak.ssf.event.risc.RiscOptOutEffective;
import org.keycloak.ssf.event.risc.RiscOptOutInitiated;
import org.keycloak.ssf.event.risc.RiscRecoveryActivated;
import org.keycloak.ssf.event.risc.RiscRecoveryInformationChanged;
import org.keycloak.ssf.event.stream.SsfStreamUpdatedEvent;
import org.keycloak.ssf.event.stream.SsfStreamVerificationEvent;

/**
 * Default {@link SsfEventProviderFactory} that contributes the built-in set
 * of standard SSF / CAEP / RISC events.
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
        events.put(CaepAssuranceLevelChange.TYPE, CaepAssuranceLevelChange::new);
        events.put(CaepCredentialChange.TYPE, CaepCredentialChange::new);
        events.put(CaepDeviceComplianceChange.TYPE, CaepDeviceComplianceChange::new);
        events.put(CaepSessionEstablished.TYPE, CaepSessionEstablished::new);
        events.put(CaepSessionPresented.TYPE, CaepSessionPresented::new);
        events.put(CaepSessionRevoked.TYPE, CaepSessionRevoked::new);
        events.put(CaepTokenClaimsChanged.TYPE, CaepTokenClaimsChanged::new);
        // RISC events
        events.put(RiscAccountCredentialChangeRequired.TYPE, RiscAccountCredentialChangeRequired::new);
        events.put(RiscAccountDisabled.TYPE, RiscAccountDisabled::new);
        events.put(RiscAccountEnabled.TYPE, RiscAccountEnabled::new);
        events.put(RiscAccountPurged.TYPE, RiscAccountPurged::new);
        events.put(RiscCredentialCompromise.TYPE, RiscCredentialCompromise::new);
        events.put(RiscIdentifierChanged.TYPE, RiscIdentifierChanged::new);
        events.put(RiscIdentifierRecycled.TYPE, RiscIdentifierRecycled::new);
        events.put(RiscOptIn.TYPE, RiscOptIn::new);
        events.put(RiscOptOutInitiated.TYPE, RiscOptOutInitiated::new);
        events.put(RiscOptOutCancelled.TYPE, RiscOptOutCancelled::new);
        events.put(RiscOptOutEffective.TYPE, RiscOptOutEffective::new);
        events.put(RiscRecoveryActivated.TYPE, RiscRecoveryActivated::new);
        events.put(RiscRecoveryInformationChanged.TYPE, RiscRecoveryInformationChanged::new);
        STANDARD_EVENT_FACTORIES = Map.copyOf(events);
    }

    /**
     * Subset of {@link #STANDARD_EVENT_FACTORIES} that {@code SecurityEventTokenMapper}
     * actually produces from Keycloak events. Every other built-in event in
     * the map is contributed to the registry only so that the receiver-side
     * parser can decode incoming SETs of that type.
     */
    public static final Set<String> EMITTABLE_EVENT_TYPES = Set.of(
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
    public SsfEventProvider create(KeycloakSession session) {
        return new DefaultSsfEventProvider(registry);
    }

    @Override
    public void init(Config.Scope config) {
        // no-op
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        this.registry = SsfEventProviderFactory.buildRegistry(factory);
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.SSF);
    }
}
