package org.keycloak.adapters.springsecurity;

import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
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
public class AdapterDeploymentContextBean implements ApplicationContextAware, InitializingBean {

    private static final String KEYCLOAK_CONFIG_FILE = "keycloak.json";
    private static final String KEYCLOAK_CONFIG_WEB_RESOURCE = "WEB-INF/" + KEYCLOAK_CONFIG_FILE;
    private static final String KEYCLOAK_CONFIG_CLASSPATH_RESOURCE = "classpath:" + KEYCLOAK_CONFIG_FILE;

    private ApplicationContext applicationContext;
    private AdapterDeploymentContext deploymentContext;
    private KeycloakDeployment deployment;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.deployment = loadKeycloakDeployment();
        this.deploymentContext = new AdapterDeploymentContext(deployment);
    }

    private KeycloakDeployment loadKeycloakDeployment() throws IOException {

        Resource resource = applicationContext.getResource(KEYCLOAK_CONFIG_WEB_RESOURCE);

        if (!resource.isReadable()) {
            resource=  applicationContext.getResource(KEYCLOAK_CONFIG_CLASSPATH_RESOURCE);
        }

        if (!resource.isReadable()) {
            throw new FileNotFoundException(String.format("Unable to locate Keycloak from %s or %s", KEYCLOAK_CONFIG_WEB_RESOURCE, KEYCLOAK_CONFIG_CLASSPATH_RESOURCE));
        }

        return KeycloakDeploymentBuilder.build(resource.getInputStream());
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
