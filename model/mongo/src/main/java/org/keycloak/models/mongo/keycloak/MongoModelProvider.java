package org.keycloak.models.mongo.keycloak;

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelProvider;

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
            String host = PropertiesManager.getMongoHost();
            int port = PropertiesManager.getMongoPort();
            String dbName = PropertiesManager.getMongoDbName();
            boolean dropDatabaseOnStartup = PropertiesManager.dropDatabaseOnStartup();

            // Create MongoDBSessionFactory via reflection now
            try {
                return new MongoDBSessionFactory(host, port, dbName, dropDatabaseOnStartup);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    }
}
