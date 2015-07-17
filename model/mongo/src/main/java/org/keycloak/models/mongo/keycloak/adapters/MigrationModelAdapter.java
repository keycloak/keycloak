package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.migration.MigrationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.entities.ProtocolMapperEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoClientEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoMigrationModelEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRoleEntity;
import org.keycloak.models.mongo.utils.MongoModelUtils;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
