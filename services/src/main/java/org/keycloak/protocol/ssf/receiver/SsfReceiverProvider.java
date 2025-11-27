package org.keycloak.protocol.ssf.receiver;

import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import org.jboss.logging.Logger;

/**
 * SsfReceiverProvider is an adapter that uses the Identity Provider infrastructure to manage SSF Receivers.
 */
public class SsfReceiverProvider implements IdentityProvider<SsfReceiverProviderConfig> {

    protected static final Logger LOG = Logger.getLogger(SsfReceiverProvider.class);

    private final KeycloakSession session;

    private final SsfReceiverProviderConfig model;

    public SsfReceiverProvider(KeycloakSession session, SsfReceiverProviderConfig model) {
        this.session = session;
        this.model = model;
    }

    @Override
    public SsfReceiverProviderConfig getConfig() {
        return new SsfReceiverProviderConfig(model);
    }

    public void requestVerification() {

        // TODO make this callable from the Admin UI via the SSF "Identity Provider" component.

        // store current verification state
        RealmModel realm = session.getContext().getRealm();

        SsfReceiver ssfReceiver = SsfReceiverProviderFactory.getSsfReceiver(session, realm, model.getAlias());
        ssfReceiver.requestVerification();
    }

    @Override
    public void close() {
        // NOOP
    }
}
