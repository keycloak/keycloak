package org.keycloak.adapters;

import java.security.Principal;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface KeycloakAccount {
    Principal getPrincipal();

    Set<String> getRoles();
}
