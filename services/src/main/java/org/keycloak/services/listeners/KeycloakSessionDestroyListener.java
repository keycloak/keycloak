package org.keycloak.services.listeners;

import org.keycloak.models.KeycloakSessionFactory;

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
        KeycloakSessionFactory factory = (KeycloakSessionFactory) sce.getServletContext().getAttribute(KeycloakSessionFactory.class.getName());
        if (factory != null) {
            factory.close();
        }
    }

}
