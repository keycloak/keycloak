package org.keycloak.example;

import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.enums.SslRequired;
import org.keycloak.representations.adapters.config.AdapterConfig;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BootstrapListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        AdapterDeploymentContext deploymentContext = (AdapterDeploymentContext)sce.getServletContext().getAttribute(AdapterDeploymentContext.class.getName());
        AdapterConfig config = new AdapterConfig();
        config.setRealm("aerogear");
        config.setResource("unified-push-server");
        config.setAuthServerUrl("/auth");
        config.setSslRequired(SslRequired.EXTERNAL.name());
        config.setPublicClient(true);
        config.setDisableTrustManager(true);
        deploymentContext.updateDeployment(config);

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
