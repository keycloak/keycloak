package org.keycloak.adapters.springsecurity;

import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Bean holding the {@link KeycloakDeployment} and {@link AdapterDeploymentContext} for this
 * Spring application context. The Keycloak deployment is loaded from the required
 * <code>keycloak.json</code> resource file.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
public class AdapterDeploymentContextBean implements InitializingBean {

    private final Resource keycloakConfigFileResource;

    private AdapterDeploymentContext deploymentContext;
    private KeycloakDeployment deployment;

    public AdapterDeploymentContextBean(Resource keycloakConfigFileResource) {
        this.keycloakConfigFileResource = keycloakConfigFileResource;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.deployment = loadKeycloakDeployment();
        this.deploymentContext = new AdapterDeploymentContext(deployment);
    }

    private KeycloakDeployment loadKeycloakDeployment() throws IOException {

        if (!keycloakConfigFileResource.isReadable()) {
            throw new FileNotFoundException(String.format("Unable to locate Keycloak configuration file: %s",
                    keycloakConfigFileResource.getFilename()));
        }

        return KeycloakDeploymentBuilder.build(keycloakConfigFileResource.getInputStream());
    }

    /**
     * Returns the Keycloak {@link AdapterDeploymentContext} for this application context.
     *
     * @return the Keycloak {@link AdapterDeploymentContext} for this application context
     */
    public AdapterDeploymentContext getDeploymentContext() {
        return deploymentContext;
    }

    /**
     * Returns the {@link KeycloakDeployment} for this application context.
     *
     * @return the {@link KeycloakDeployment} for this application context
     */
    public KeycloakDeployment getDeployment() {
        return deployment;
    }
}
