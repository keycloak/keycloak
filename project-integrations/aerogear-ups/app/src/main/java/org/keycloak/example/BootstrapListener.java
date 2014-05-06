package org.keycloak.example;

import org.keycloak.adapters.AdapterDeploymentContext;
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
        config.setRealm("demo");
        config.setResource("customer-portal");
        config.setAuthServerUrl("/auth");
        config.setSslNotRequired(true);
        config.setPublicClient(true);
        config.setDisableTrustManager(true);
        deploymentContext.updateDeployment(config);

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
