package org.keycloak.protocol.ssf.receiver.registration;

import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;

import org.jboss.logging.Logger;

/**
 * SsfReceiverRegistrationProvider is an adapter that uses the Identity Provider infrastructure to manage SSF Receivers.
 */
public class SsfReceiverRegistrationProvider implements IdentityProvider<SsfReceiverRegistrationProviderConfig> {

    protected static final Logger LOG = Logger.getLogger(SsfReceiverRegistrationProvider.class);

    private final KeycloakSession session;

    private final SsfReceiverRegistrationProviderConfig model;

    public SsfReceiverRegistrationProvider(KeycloakSession session, SsfReceiverRegistrationProviderConfig model) {
        this.session = session;
        this.model = model;
    }

    @Override
    public SsfReceiverRegistrationProviderConfig getConfig() {
        return new SsfReceiverRegistrationProviderConfig(model);
    }

    @Override
    public void close() {
        // NOOP
    }
}
