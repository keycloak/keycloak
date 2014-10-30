package org.keycloak.adapters.wildfly;

import io.undertow.servlet.api.DeploymentInfo;
import org.jboss.logging.Logger;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.undertow.KeycloakServletExtension;
import org.keycloak.adapters.undertow.ServletKeycloakAuthMech;
import org.keycloak.adapters.undertow.UndertowUserSessionManagement;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class WildflyKeycloakServletExtension extends KeycloakServletExtension {
    protected static Logger log = Logger.getLogger(WildflyKeycloakServletExtension.class);

    @Override
    protected ServletKeycloakAuthMech createAuthenticationMechanism(DeploymentInfo deploymentInfo, AdapterDeploymentContext deploymentContext,
                                                                    UndertowUserSessionManagement userSessionManagement, NodesRegistrationManagement nodesRegistrationManagement) {
        log.debug("creating WildflyAuthenticationMechanism");
        return new WildflyAuthenticationMechanism(deploymentContext, userSessionManagement, nodesRegistrationManagement, deploymentInfo.getConfidentialPortManager());

    }
}
