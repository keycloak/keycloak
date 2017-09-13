package org.keycloak.adapters.tomcat;

import org.apache.catalina.Container;
import org.apache.catalina.Valve;
import org.keycloak.adapters.AdapterDeploymentContext;

public class AuthenticatedActionsValve extends AbstractAuthenticatedActionsValve {

    public AuthenticatedActionsValve(AdapterDeploymentContext deploymentContext, Valve next, Container container) {
        super(deploymentContext, next, container);
    }

    @Override
    public boolean isAsyncSupported() {
        return true;
    }
}
