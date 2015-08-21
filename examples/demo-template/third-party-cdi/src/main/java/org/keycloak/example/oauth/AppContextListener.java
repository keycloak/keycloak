package org.keycloak.example.oauth;

import org.jboss.logging.Logger;
import org.keycloak.servlet.ServletOAuthClient;
import org.keycloak.servlet.ServletOAuthClientBuilder;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger logger = Logger.getLogger(AppContextListener.class);

    @Inject
    private ServletOAuthClient oauthClient;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();

        InputStream is = null;
        String path = context.getInitParameter("keycloak.config.file");
        if (path == null) {
            is = context.getResourceAsStream("/WEB-INF/keycloak.json");
        } else {
            try {
                is = new FileInputStream(path);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        ServletOAuthClientBuilder.build(is, oauthClient);
        logger.info("OAuth client configured and started");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        oauthClient.stop();
        logger.info("OAuth client stopped");
    }
}
