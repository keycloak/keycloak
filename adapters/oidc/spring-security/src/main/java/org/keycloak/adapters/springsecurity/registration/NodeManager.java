package org.keycloak.adapters.springsecurity.registration;

import org.keycloak.adapters.KeycloakDeployment;

/**
 * Manages registration of application nodes.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
public interface NodeManager {

    /**
     * Registers the given deployment with the Keycloak server.
     *
     * @param deployment the deployment to register (required)
     */
    void register(KeycloakDeployment deployment);

    /**
     * Unregisters the give deployment from the Keycloak server
     * .
     * @param deployment the deployment to unregister (required)
     */
    void unregister(KeycloakDeployment deployment);

}
