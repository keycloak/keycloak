package org.keycloak.services.listeners;

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.util.KeycloakRegistry;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class KeycloakSessionDestroyListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        KeycloakRegistry registry = (KeycloakRegistry)sce.getServletContext().getAttribute(KeycloakRegistry.class.getName());
        KeycloakSessionFactory factory = registry.getService(KeycloakSessionFactory.class);
        if (factory != null) {
            factory.close();
        }
    }

}
