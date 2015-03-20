package org.keycloak.connections.mongo.updater.impl;

import org.keycloak.Config;
import org.keycloak.connections.mongo.updater.MongoUpdaterProvider;
import org.keycloak.connections.mongo.updater.MongoUpdaterProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

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
    public void postInit(KeycloakSessionFactory factory) {

    }
    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "default";
    }

}
