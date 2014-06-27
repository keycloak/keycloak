package org.keycloak.services.listeners;

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.managers.BruteForceProtector;

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
        BruteForceProtector protector = (BruteForceProtector) sce.getServletContext().getAttribute(BruteForceProtector.class.getName());
        if (protector != null) {
            protector.shutdown();
        }
        KeycloakSessionFactory sessionFactory = (KeycloakSessionFactory) sce.getServletContext().getAttribute(KeycloakSessionFactory.class.getName());
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

}
