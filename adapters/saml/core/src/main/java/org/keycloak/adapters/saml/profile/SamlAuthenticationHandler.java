package org.keycloak.adapters.saml.profile;

import org.keycloak.adapters.saml.OnSessionCreated;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface SamlAuthenticationHandler {
    AuthOutcome handle(OnSessionCreated onCreateSession);
    AuthChallenge getChallenge();
}
