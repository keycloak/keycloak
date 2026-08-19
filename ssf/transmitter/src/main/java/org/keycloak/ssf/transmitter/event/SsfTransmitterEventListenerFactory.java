package org.keycloak.ssf.transmitter.event;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.transmitter.subject.PurgedUserSnapshot;

import org.jboss.logging.Logger;

public class SsfTransmitterEventListenerFactory implements EventListenerProviderFactory, EnvironmentDependentProviderFactory {

    protected static final Logger log = Logger.getLogger(SsfTransmitterEventListenerFactory.class);

    private static final String ID = "ssf-events";

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        // Create and return the event mapper
        return new SsfTransmitterEventListener(session);
    }

    @Override
    public boolean isGlobal() {
        return true;
    }

    @Override
    public void init(Config.Scope config) {
        // No initialization needed
    }

    /**
     * Subscribes to {@link UserModel.UserPreRemovedEvent} so a purged user can still
     * be described after its row is gone.
     *
     * <p>Keycloak deletes the user before firing the admin / user event that drives SSF
     * emission, so the transmitter would otherwise have nothing to build a subject from.
     * This is the last hook that runs while the user still exists. It only captures —
     * emission stays entirely on the event-listener path, because a pre-remove hook
     * cannot know whether the removal will actually succeed.
     *
     * <p>Gated on the realm's transmitter flag so realms not using SSF pay nothing
     * beyond one attribute read per user deletion.
     */
    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(event -> {
            if (!(event instanceof UserModel.UserPreRemovedEvent preRemoved)) {
                return;
            }
            RealmModel realm = preRemoved.getRealm();
            if (realm == null || !Ssf.isTransmitterEnabled(realm)) {
                return;
            }
            try {
                PurgedUserSnapshot.capture(preRemoved.getKeycloakSession(), realm, preRemoved.getUser());
            } catch (RuntimeException e) {
                // Never let a snapshot failure break the deletion itself — the user
                // removal is the operator's actual intent. Losing the snapshot costs
                // one undelivered purge SET, which the debug log below makes traceable.
                log.debugf(e, "SSF: could not snapshot user %s before removal; no purge event will be emitted",
                        preRemoved.getUser() != null ? preRemoved.getUser().getId() : null);
            }
        });
    }

    @Override
    public void close() {
        // No resources to close
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.SSF);
    }
}
