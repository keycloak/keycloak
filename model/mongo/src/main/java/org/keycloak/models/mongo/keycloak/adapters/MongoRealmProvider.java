/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.connections.mongo.api.MongoStore;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.migration.MigrationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.mongo.keycloak.entities.MongoClientEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoClientTemplateEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoGroupEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoMigrationModelEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRealmEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoRealmProvider implements RealmProvider {

    private final MongoStoreInvocationContext invocationContext;
    private final KeycloakSession session;

    public MongoRealmProvider(KeycloakSession session, MongoStoreInvocationContext invocationContext) {
        this.session = session;
        this.invocationContext = invocationContext;
    }

    @Override
    public void close() {
        // TODO
    }

    @Override
    public MigrationModel getMigrationModel() {
        MongoMigrationModelEntity entity = getMongoStore().loadEntity(MongoMigrationModelEntity.class, MongoMigrationModelEntity.MIGRATION_MODEL_ID, invocationContext);
        if (entity == null) {
            entity = new MongoMigrationModelEntity();
            getMongoStore().insertEntity(entity, invocationContext);
        }
        return new MigrationModelAdapter(session, entity, invocationContext);
    }

    @Override
    public RealmModel createRealm(String name) {
        return createRealm(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        MongoRealmEntity newRealm = new MongoRealmEntity();
        newRealm.setId(id);
        newRealm.setName(name);

        getMongoStore().insertEntity(newRealm, invocationContext);

        final RealmModel model = new RealmAdapter(session, newRealm, invocationContext);
        session.getKeycloakSessionFactory().publish(new RealmModel.RealmCreationEvent() {
            @Override
            public RealmModel getCreatedRealm() {
                return model;
            }
        });
        return model;
    }

    @Override
    public RealmModel getRealm(String id) {
        MongoRealmEntity realmEntity = getMongoStore().loadEntity(MongoRealmEntity.class, id, invocationContext);
        return realmEntity != null ? new RealmAdapter(session, realmEntity, invocationContext) : null;
    }

    @Override
    public List<RealmModel> getRealms() {
        DBObject query = new BasicDBObject();
        List<MongoRealmEntity> realms = getMongoStore().loadEntities(MongoRealmEntity.class, query, invocationContext);

        List<RealmModel> results = new ArrayList<RealmModel>();
        for (MongoRealmEntity realmEntity : realms) {
            results.add(new RealmAdapter(session, realmEntity, invocationContext));
        }
        return results;
    }

    @Override
    public RealmModel getRealmByName(String name) {
        DBObject query = new QueryBuilder()
                .and("name").is(name)
                .get();
        MongoRealmEntity realm = getMongoStore().loadSingleEntity(MongoRealmEntity.class, query, invocationContext);

        if (realm == null) return null;
        return new RealmAdapter(session, realm, invocationContext);
    }

    @Override
    public boolean removeRealm(String id) {
        RealmModel realm = getRealm(id);
        if (realm == null) return false;
        session.users().preRemove(realm);
        return getMongoStore().removeEntity(MongoRealmEntity.class, id, invocationContext);
    }

    protected MongoStore getMongoStore() {
        return invocationContext.getMongoStore();
    }

    @Override
    public RoleModel getRoleById(String id, RealmModel realm) {
        MongoRoleEntity role = getMongoStore().loadEntity(MongoRoleEntity.class, id, invocationContext);
        if (role == null) return null;
        if (role.getRealmId() != null && !role.getRealmId().equals(realm.getId())) return null;
        if (role.getClientId() != null && realm.getClientById(role.getClientId()) == null) return null;
        return new RoleAdapter(session, realm, role, null, invocationContext);
    }

    @Override
    public GroupModel getGroupById(String id, RealmModel realm) {
        MongoGroupEntity group = getMongoStore().loadEntity(MongoGroupEntity.class, id, invocationContext);
        if (group == null) return null;
        if (group.getRealmId() != null && !group.getRealmId().equals(realm.getId())) return null;
        return new GroupAdapter(session, realm, group, invocationContext);
    }

    @Override
    public ClientModel getClientById(String id, RealmModel realm) {
        MongoClientEntity appData = getMongoStore().loadEntity(MongoClientEntity.class, id, invocationContext);

        // Check if application belongs to this realm
        if (appData == null || !realm.getId().equals(appData.getRealmId())) {
            return null;
        }

        return new ClientAdapter(session, realm, appData, invocationContext);
    }

    @Override
    public boolean removeClient(String id, RealmModel realm) {
        if (id == null) return false;
        ClientModel client = getClientById(id, realm);
        if (client == null) return false;

        session.users().preRemove(realm, client);

        return getMongoStore().removeEntity(MongoClientEntity.class, id, invocationContext);
    }

    @Override
    public ClientModel getClientByClientId(String clientId, RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .and("clientId").is(clientId)
                .get();
        MongoClientEntity appEntity = getMongoStore().loadSingleEntity(MongoClientEntity.class, query, invocationContext);
        return appEntity == null ? null : new ClientAdapter(session, realm, appEntity, invocationContext);

    }

    @Override
    public ClientTemplateModel getClientTemplateById(String id, RealmModel realm) {
        MongoClientTemplateEntity appData = getMongoStore().loadEntity(MongoClientTemplateEntity.class, id, invocationContext);

        // Check if application belongs to this realm
        if (appData == null || !realm.getId().equals(appData.getRealmId())) {
            return null;
        }

        return new ClientTemplateAdapter(session, realm, appData, invocationContext);
    }
}
