package org.keycloak.test.tools;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DestroyListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (KeycloakTestApplication.mail != null) {
            KeycloakTestApplication.mail.stop();
        }
    }
}
