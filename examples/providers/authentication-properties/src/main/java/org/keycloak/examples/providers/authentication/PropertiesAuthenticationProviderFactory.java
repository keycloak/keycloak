package org.keycloak.examples.providers.authentication;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationProvider;
import org.keycloak.authentication.AuthenticationProviderFactory;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PropertiesAuthenticationProviderFactory implements AuthenticationProviderFactory {

    private static final Logger log = Logger.getLogger(PropertiesAuthenticationProviderFactory.class);

    private Properties properties;
    private String propsFileLocation;

    @Override
    public AuthenticationProvider create(KeycloakSession session) {
        return new PropertiesAuthenticationProvider(properties);
    }

    @Override
    public void init(Config.Scope config) {
        this.propsFileLocation = config.get("propertiesFileLocation");

        InputStream propertiesStream = null;
        this.properties = new Properties();
        try {
            if (propsFileLocation == null) {
                log.info("propertiesFileLocation not configured. Using default users.properties file from classpath.");
                log.warn("Password updates won't be persisted!");
                propertiesStream = getClass().getClassLoader().getResourceAsStream("users.properties");
            } else {
                log.info("Using properties file from location: " + propsFileLocation);
                propertiesStream = new FileInputStream(propsFileLocation);
            }

            this.properties.load(propertiesStream);
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        } finally {
            if (propertiesStream != null) {
                try {
                    propertiesStream.close();
                } catch (IOException e) {
                    log.error("Error when closing InputStream", e);
                }
            }
        }
    }

    @Override
    public void close() {
        // Update properties file now, just in case that we are using custom location from filesystem
        if (propsFileLocation != null) {
            storePasswords();
        }
    }

    private void storePasswords() {
        log.info("Going to store passwords back to file: " + propsFileLocation);
        OutputStream propertiesStream = null;
        try {
            OutputStream stream = new FileOutputStream(propsFileLocation);
            this.properties.store(stream, "User passwords");
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        } finally {
            if (propertiesStream != null) {
                try {
                    propertiesStream.close();
                } catch (IOException e) {
                    log.error("Error when closing InputStream", e);
                }
            }
        }
    }

    @Override
    public String getId() {
        return "properties";
    }
}
