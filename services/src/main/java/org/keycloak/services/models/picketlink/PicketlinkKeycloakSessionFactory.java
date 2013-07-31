package org.keycloak.services.models.picketlink;

import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.KeycloakSessionFactory;
import org.picketlink.idm.IdentitySessionFactory;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PicketlinkKeycloakSessionFactory implements KeycloakSessionFactory {
    protected IdentitySessionFactory factory;

    public PicketlinkKeycloakSessionFactory(IdentitySessionFactory factory) {
        this.factory = factory;
    }

    @Override
    public KeycloakSession createSession() {
        return new PicketlinkKeycloakSession(factory.createIdentitySession());
    }

    @Override
    public void close() {
        factory.close();
    }
}
