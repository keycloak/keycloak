package org.keycloak.adapters.saml;

import org.keycloak.adapters.spi.HttpFacade;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlDeploymentContext {
    private SamlDeployment deployment = null;

    public SamlDeploymentContext(SamlDeployment deployment) {
        this.deployment = deployment;
    }

    public SamlDeployment resolveDeployment(HttpFacade facade) {
        return deployment;
    }
}
