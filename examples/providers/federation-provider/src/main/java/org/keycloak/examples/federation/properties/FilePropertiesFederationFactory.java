package org.keycloak.examples.federation.properties;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserFederationProviderModel;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class FilePropertiesFederationFactory extends BasePropertiesFederationFactory {

    public static final String PROVIDER_NAME = "file-properties";

    @Override
    protected BasePropertiesFederationProvider createProvider(KeycloakSession session, UserFederationProviderModel model, Properties props) {
        return new FilePropertiesFederationProvider(session, props, model);
    }
    protected InputStream getPropertiesFileStream(String path) {
        try {
            return new FileInputStream(path);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Name of the provider.  This will show up under the "Add Provider" select box on the Federation page in the
     * admin console
     *
     * @return
     */
    @Override
    public String getId() {
        return PROVIDER_NAME;
    }
}
