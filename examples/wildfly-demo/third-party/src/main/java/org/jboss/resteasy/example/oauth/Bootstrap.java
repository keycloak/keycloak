package org.jboss.resteasy.example.oauth;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.servlet.ServletOAuthClient;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

/**
 * Stupid init code to load up the truststore so we can make appropriate SSL connections
 * You really should use a better way of initializing this stuff.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Bootstrap implements ServletContextListener {

    private ServletOAuthClient client;

    private static KeyStore loadKeyStore(String filename, String password) throws Exception {
        KeyStore trustStore = KeyStore.getInstance(KeyStore
                .getDefaultType());
        File truststoreFile = new File(filename);
        FileInputStream trustStream = new FileInputStream(truststoreFile);
        trustStore.load(trustStream, password.toCharArray());
        trustStream.close();
        return trustStore;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        client = new ServletOAuthClient();
/*
       // hardcoded, WARNING, you should really have a better way of doing this
      // configuration.  Either use something like Spring or CDI, or even pull
      // config vales from context-params
      String truststorePath = "${jboss.server.config.dir}/client-truststore.ts";
      String truststorePassword = "password";
      truststorePath = EnvUtil.replace(truststorePath);
      KeyStore truststore = null;
      try
      {
         truststore = loadKeyStore(truststorePath, truststorePassword);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
      client.setTruststore(truststore);
      */
        client.setClientId("third-party");
        client.setPassword("password");
        client.setAuthUrl("http://localhost:8080/auth-server/rest/realms/demo/tokens/login");
        client.setCodeUrl("http://localhost:8080/auth-server/rest/realms/demo/tokens/access/codes");
        client.setClient(new ResteasyClientBuilder().build());
        client.start();
        sce.getServletContext().setAttribute(ServletOAuthClient.class.getName(), client);


    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        client.stop();
    }
}
