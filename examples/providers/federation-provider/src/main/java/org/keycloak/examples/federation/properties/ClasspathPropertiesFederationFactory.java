package org.keycloak.examples.federation.properties;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClasspathPropertiesFederationFactory extends BasePropertiesFederationFactory {

    @Override
    protected BasePropertiesFederationProvider createProvider(KeycloakSession session, UserFederationProviderModel model, Properties props) {
        return new ClasspathPropertiesFederationProvider(session, model, props);
    }


    @Override
    public String getId() {
        return "classpath-properties";
    }
}
