package org.keycloak.example.oauth;

import org.keycloak.servlet.ServletOAuthClient;
import org.keycloak.servlet.ServletOAuthClientBuilder;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Init code to load up the truststore so we can make appropriate SSL connections
 * You really should use a better way of initializing this stuff.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Bootstrap implements ServletContextListener {

    private ServletOAuthClient client;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        client = new ServletOAuthClient();
        ServletContext context = sce.getServletContext();

        configureClient(context);

        client.start();
        context.setAttribute(ServletOAuthClient.class.getName(), client);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        client.stop();
    }

    private void configureClient(ServletContext context) {
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

        client = ServletOAuthClientBuilder.build(is);
    }
}
