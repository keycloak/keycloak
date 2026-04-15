package org.keycloak.ssf.event;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    private static final List<SsfEvent> STANDARD_EVENTS = List.of(
            // SSF Stream events
            new SsfStreamVerificationEvent(),
            new SsfStreamUpdatedEvent(),
            // CAEP events
            new CaepAssuranceLevelChange(),
            new CaepCredentialChange(),
            new CaepDeviceComplianceChange(),
            new CaepSessionEstablished(),
            new CaepSessionPresented(),
            new CaepSessionRevoked(),
            new CaepTokenClaimsChanged(),
            // RISC events
            new RiscAccountCredentialChangeRequired(),
            new RiscAccountDisabled(),
            new RiscAccountEnabled(),
            new RiscAccountPurged(),
            new RiscCredentialCompromise(),
            new RiscIdentifierChanged(),
            new RiscIdentifierRecycled(),
            new RiscOptIn(),
            new RiscOptOutInitiated(),
            new RiscOptOutCancelled(),
            new RiscOptOutEffective(),
            new RiscRecoveryActivated(),
            new RiscRecoveryInformationChanged());

    /**
     * Subset of {@link #STANDARD_EVENTS} that {@code SecurityEventTokenMapper}
     * actually produces from Keycloak events. Every other built-in event in
     * the list is contributed to the registry only so that the receiver-side
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
    public Set<SsfEvent> getContributedEvents() {
        return new LinkedHashSet<>(STANDARD_EVENTS);
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
