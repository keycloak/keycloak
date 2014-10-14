package org.keycloak.connections.mongo.updater;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultMongoUpdaterProviderFactory implements MongoUpdaterProviderFactory {

    @Override
    public MongoUpdaterProvider create(KeycloakSession session) {
        return new DefaultMongoUpdaterProvider();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "default";
    }

}
