package org.keycloak.protocol.ssf.receiver.registration;

import org.keycloak.broker.provider.IdentityProvider;

import org.jboss.logging.Logger;

/**
 * SsfReceiverRegistrationProvider is an adapter that uses the Identity Provider infrastructure to manage SSF Receivers.
 */
public class SsfReceiverRegistrationProvider implements IdentityProvider<SsfReceiverRegistrationProviderConfig> {

    protected static final Logger LOG = Logger.getLogger(SsfReceiverRegistrationProvider.class);

    private final SsfReceiverRegistrationProviderConfig model;

    public SsfReceiverRegistrationProvider(SsfReceiverRegistrationProviderConfig model) {
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
