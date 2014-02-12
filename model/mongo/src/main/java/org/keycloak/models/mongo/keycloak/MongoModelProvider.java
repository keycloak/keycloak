package org.keycloak.models.mongo.keycloak;

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelProvider;
import org.keycloak.models.mongo.keycloak.adapters.MongoKeycloakSessionFactory;
import org.keycloak.models.mongo.utils.MongoConfiguration;
import org.keycloak.models.mongo.utils.SystemPropertiesConfigurationProvider;

import java.lang.Override;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MongoModelProvider implements ModelProvider {

    @Override
    public String getId() {
        return "mongo";
    }

    @Override
    public KeycloakSessionFactory createFactory() {
        MongoConfiguration config = SystemPropertiesConfigurationProvider.createConfiguration();
        return new MongoKeycloakSessionFactory(config);
    }
}
