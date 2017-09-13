package org.keycloak.adapters.jbossweb;

import org.apache.catalina.Container;
import org.apache.catalina.Valve;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.tomcat.AbstractAuthenticatedActionsValve;

public class AuthenticatedActionsValve extends AbstractAuthenticatedActionsValve {

    public AuthenticatedActionsValve(AdapterDeploymentContext deploymentContext, Valve next, Container container) {
        super(deploymentContext, next, container);
    }
}
