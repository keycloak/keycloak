package org.keycloak.adapters;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.spi.HttpFacade;

/**
 * Bridge between core adapter and HTTP Engine
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface OIDCHttpFacade extends HttpFacade {

    KeycloakSecurityContext getSecurityContext();
}
