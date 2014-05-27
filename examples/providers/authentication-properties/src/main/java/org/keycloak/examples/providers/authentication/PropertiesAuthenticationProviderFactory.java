package org.keycloak.examples.providers.authentication;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationProvider;
import org.keycloak.authentication.AuthenticationProviderFactory;
import org.keycloak.provider.ProviderSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PropertiesAuthenticationProviderFactory implements AuthenticationProviderFactory {

    private static final Logger log = Logger.getLogger(PropertiesAuthenticationProviderFactory.class);

    private Properties properties;

    @Override
    public AuthenticationProvider create(ProviderSession providerSession) {
        return new PropertiesAuthenticationProvider(properties);
    }

    @Override
    public void init(Config.Scope config) {
        String propsFileLocation = config.get("propertiesFileLocation");
        if (propsFileLocation == null) {
            throw new IllegalStateException("Properties file location is not configured in PropertiesAuthenticationProviderFactory");
        } else {
            log.info("Using properties file: " + propsFileLocation);
        }

        this.properties = new Properties();
        InputStream propertiesStream = null;
        try {
            propertiesStream = getClass().getClassLoader().getResourceAsStream(propsFileLocation);
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
    }

    @Override
    public String getId() {
        return "properties";
    }
}
