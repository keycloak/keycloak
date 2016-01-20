package org.keycloak.adapters;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.spi.KeycloakAccount;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface OidcKeycloakAccount extends KeycloakAccount {
    KeycloakSecurityContext getKeycloakSecurityContext();
}
