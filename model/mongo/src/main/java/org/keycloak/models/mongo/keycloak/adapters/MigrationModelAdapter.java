package org.keycloak.models.mongo.keycloak.adapters;

import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.migration.MigrationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.mongo.keycloak.entities.MongoMigrationModelEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MigrationModelAdapter extends AbstractMongoAdapter<MongoMigrationModelEntity> implements MigrationModel {

    protected final MongoMigrationModelEntity entity;

    public MigrationModelAdapter(KeycloakSession session, MongoMigrationModelEntity entity, MongoStoreInvocationContext invContext) {
        super(invContext);
        this.entity = entity;
    }

    @Override
    public MongoMigrationModelEntity getMongoEntity() {
        return entity;
    }

    @Override
    public String getStoredVersion() {
        return getMongoEntity().getVersion();
    }

    @Override
    public void setStoredVersion(String version) {
        getMongoEntity().setVersion(version);
        updateMongoEntity();

    }


}
