package org.keycloak.examples.federation.properties;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserFederationProviderModel;

import java.util.Properties;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class FilePropertiesFederationFactory extends BasePropertiesFederationFactory {

    @Override
    protected BasePropertiesFederationProvider createProvider(KeycloakSession session, UserFederationProviderModel model, Properties props) {
        return new FilePropertiesFederationProvider(session, props, model);
    }


    @Override
    public String getId() {
        return "file-properties";
    }
}
