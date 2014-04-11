package org.keycloak.adapters;

import org.keycloak.representations.adapters.config.AdapterConfig;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AdapterDeploymentContext {
    protected KeycloakDeployment deployment;

    public AdapterDeploymentContext() {
    }

    public AdapterDeploymentContext(KeycloakDeployment deployment) {
        this.deployment = deployment;
    }

    public KeycloakDeployment getDeployment() {
        return deployment;
    }

    public void updateDeployment(AdapterConfig config) {
        deployment = KeycloakDeploymentBuilder.build(config);
    }
}
