package org.keycloak.protocol.oid4vc;

import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientregistration.AbstractClientRegistrationContext;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;

/**
 * Empty registration context to fulfill client-registration integration.
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCClientRegistrationContext extends AbstractClientRegistrationContext {

    public OID4VCClientRegistrationContext(KeycloakSession session,
                                           ClientRepresentation client,
                                           ClientRegistrationProvider provider) {
        super(session, client, provider);
    }
}