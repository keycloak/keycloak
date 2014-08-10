package org.keycloak.examples.federation.properties;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserFederationProviderModel;

import java.io.InputStream;
import java.util.Properties;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClasspathPropertiesFederationFactory extends BasePropertiesFederationFactory {

    public static final String PROVIDER_NAME = "classpath-properties";

    @Override
    protected BasePropertiesFederationProvider createProvider(KeycloakSession session, UserFederationProviderModel model, Properties props) {
        return new ClasspathPropertiesFederationProvider(session, model, props);
    }

    protected InputStream getPropertiesFileStream(String path) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (is == null) {
            throw new IllegalStateException("Path not found for properties file");

        }
        return is;
    }



    @Override
    public String getId() {
        return PROVIDER_NAME;
    }
}
