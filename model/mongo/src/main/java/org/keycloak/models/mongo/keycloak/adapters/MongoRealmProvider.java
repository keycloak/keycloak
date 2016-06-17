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
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.mongo.keycloak.entities.MongoClientEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoClientTemplateEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoGroupEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoMigrationModelEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRealmEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
            RealmModel realm = session.realms().getRealm(realmEntity.getId());
            if (realm != null) results.add(realm);
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
        return session.realms().getRealm(realm.getId());
    }

    @Override
    public boolean removeRealm(String id) {
        final RealmModel realm = getRealm(id);
        if (realm == null) return false;
        session.users().preRemove(realm);
        boolean removed = getMongoStore().removeEntity(MongoRealmEntity.class, id, invocationContext);

        if (removed) {
            session.getKeycloakSessionFactory().publish(new RealmModel.RealmRemovedEvent() {
                @Override
                public RealmModel getRealm() {
                    return realm;
                }

                @Override
                public KeycloakSession getKeycloakSession() {
                    return session;
                }
            });
        }

        return removed;
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
    public void moveGroup(RealmModel realm, GroupModel group, GroupModel toParent) {
        if (toParent != null && group.getId().equals(toParent.getId())) {
            return;
        }
        if (group.getParentId() != null) {
            group.getParent().removeChild(group);
        }
        group.setParent(toParent);
        if (toParent != null) toParent.addChild(group);
        else session.realms().addTopLevelGroup(realm, group);

    }

    @Override
    public List<GroupModel> getGroups(RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .get();
        List<MongoGroupEntity> groups = getMongoStore().loadEntities(MongoGroupEntity.class, query, invocationContext);
        if (groups == null) return Collections.EMPTY_LIST;

        List<GroupModel> result = new LinkedList<>();

        if (groups == null) return result;
        for (MongoGroupEntity group : groups) {
            result.add(getGroupById(group.getId(), realm));
        }

        return Collections.unmodifiableList(result);
    }

    @Override
    public List<GroupModel> getTopLevelGroups(RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .and("parentId").is(null)
                .get();
        List<MongoGroupEntity> groups = getMongoStore().loadEntities(MongoGroupEntity.class, query, invocationContext);
        if (groups == null) return Collections.EMPTY_LIST;

        List<GroupModel> result = new LinkedList<>();

        if (groups == null) return result;
        for (MongoGroupEntity group : groups) {
            result.add(getGroupById(group.getId(), realm));
        }

        return Collections.unmodifiableList(result);
    }

    @Override
    public boolean removeGroup(RealmModel realm, GroupModel group) {
        session.users().preRemove(realm, group);
        realm.removeDefaultGroup(group);
        for (GroupModel subGroup : group.getSubGroups()) {
            removeGroup(realm, subGroup);
        }
        moveGroup(realm, group, null);
        return getMongoStore().removeEntity(MongoGroupEntity.class, group.getId(), invocationContext);
    }

    @Override
    public GroupModel createGroup(RealmModel realm, String name) {
        String id = KeycloakModelUtils.generateId();
        return createGroup(realm, id, name);
    }

    @Override
    public GroupModel createGroup(RealmModel realm, String id, String name) {
        if (id == null) id = KeycloakModelUtils.generateId();
        MongoGroupEntity group = new MongoGroupEntity();
        group.setId(id);
        group.setName(name);
        group.setRealmId(realm.getId());

        getMongoStore().insertEntity(group, invocationContext);

        return new GroupAdapter(session, realm, group, invocationContext);
    }

    @Override
    public void addTopLevelGroup(RealmModel realm, GroupModel subGroup) {
        subGroup.setParent(null);
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
    public ClientModel addClient(RealmModel realm, String clientId) {
        return addClient(realm, KeycloakModelUtils.generateId(), clientId);
    }

    @Override
    public ClientModel addClient(RealmModel realm, String id, String clientId) {
        MongoClientEntity clientEntity = new MongoClientEntity();
        clientEntity.setId(id);
        clientEntity.setClientId(clientId);
        clientEntity.setRealmId(realm.getId());
        clientEntity.setEnabled(true);
        clientEntity.setStandardFlowEnabled(true);
        getMongoStore().insertEntity(clientEntity, invocationContext);

        if (clientId == null) {
            clientEntity.setClientId(clientEntity.getId());
            getMongoStore().updateEntity(clientEntity, invocationContext);
        }

        final ClientModel model = new ClientAdapter(session, realm, clientEntity, invocationContext);
        session.getKeycloakSessionFactory().publish(new RealmModel.ClientCreationEvent() {
            @Override
            public ClientModel getCreatedClient() {
                return model;
            }
        });
        return model;
    }

    @Override
    public List<ClientModel> getClients(RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .get();
        List<MongoClientEntity> clientEntities = getMongoStore().loadEntities(MongoClientEntity.class, query, invocationContext);

        if (clientEntities.isEmpty()) return Collections.EMPTY_LIST;
        List<ClientModel> result = new ArrayList<ClientModel>();
        for (MongoClientEntity clientEntity : clientEntities) {
            result.add(session.realms().getClientById(clientEntity.getId(), realm));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public RoleModel addRealmRole(RealmModel realm, String name) {
        return addRealmRole(realm, KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RoleModel addRealmRole(RealmModel realm, String id, String name) {
        MongoRoleEntity roleEntity = new MongoRoleEntity();
        roleEntity.setId(id);
        roleEntity.setName(name);
        roleEntity.setRealmId(realm.getId());

        getMongoStore().insertEntity(roleEntity, invocationContext);

        return new RoleAdapter(session, realm, roleEntity, realm, invocationContext);
    }

    @Override
    public Set<RoleModel> getRealmRoles(RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .get();
        List<MongoRoleEntity> roles = getMongoStore().loadEntities(MongoRoleEntity.class, query, invocationContext);


        if (roles == null) return Collections.EMPTY_SET;
        Set<RoleModel> result = new HashSet<RoleModel>();
        for (MongoRoleEntity role : roles) {
            result.add(session.realms().getRoleById(role.getId(), realm));
        }

        return Collections.unmodifiableSet(result);

    }

    @Override
    public Set<RoleModel> getClientRoles(RealmModel realm, ClientModel client) {
        DBObject query = new QueryBuilder()
                .and("clientId").is(client.getId())
                .get();
        List<MongoRoleEntity> roles = getMongoStore().loadEntities(MongoRoleEntity.class, query, invocationContext);

        Set<RoleModel> result = new HashSet<RoleModel>();
        for (MongoRoleEntity role : roles) {
            result.add(session.realms().getRoleById(role.getId(), realm));
        }

        return result;
    }

    @Override
    public RoleModel getRealmRole(RealmModel realm, String name) {
        DBObject query = new QueryBuilder()
                .and("name").is(name)
                .and("realmId").is(realm.getId())
                .get();
        MongoRoleEntity role = getMongoStore().loadSingleEntity(MongoRoleEntity.class, query, invocationContext);
        if (role == null) {
            return null;
        } else {
            return session.realms().getRoleById(role.getId(), realm);
        }
    }

    @Override
    public RoleModel getClientRole(RealmModel realm, ClientModel client, String name) {
        DBObject query = new QueryBuilder()
                .and("name").is(name)
                .and("clientId").is(client.getId())
                .get();
        MongoRoleEntity role = getMongoStore().loadSingleEntity(MongoRoleEntity.class, query, invocationContext);
        if (role == null) {
            return null;
        } else {
            return session.realms().getRoleById(role.getId(), realm);
        }
    }

    @Override
    public RoleModel addClientRole(RealmModel realm, ClientModel client, String name) {
        return addClientRole(realm, client, KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RoleModel addClientRole(RealmModel realm, ClientModel client, String id, String name) {
        MongoRoleEntity roleEntity = new MongoRoleEntity();
        roleEntity.setId(id);
        roleEntity.setName(name);
        roleEntity.setClientId(client.getId());

        getMongoStore().insertEntity(roleEntity, invocationContext);

        return new RoleAdapter(session, realm, roleEntity, client, invocationContext);
    }

    @Override
    public boolean removeRole(RealmModel realm, RoleModel role) {
        session.users().preRemove(realm, role);
        RoleContainerModel container = role.getContainer();
        if (container.getDefaultRoles().contains(role.getName())) {
            container.removeDefaultRoles(role.getName());
        }

        return getMongoStore().removeEntity(MongoRoleEntity.class, role.getId(), invocationContext);
    }

    @Override
    public boolean removeClient(String id, RealmModel realm) {
        if (id == null) return false;
        final ClientModel client = getClientById(id, realm);
        if (client == null) return false;

        session.users().preRemove(realm, client);
        boolean removed = getMongoStore().removeEntity(MongoClientEntity.class, id, invocationContext);

        if (removed) {
            session.getKeycloakSessionFactory().publish(new RealmModel.ClientRemovedEvent() {
                @Override
                public ClientModel getClient() {
                    return client;
                }

                @Override
                public KeycloakSession getKeycloakSession() {
                    return session;
                }
            });
        }

        return removed;
    }

    @Override
    public ClientModel getClientByClientId(String clientId, RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .and("clientId").is(clientId)
                .get();
        MongoClientEntity appEntity = getMongoStore().loadSingleEntity(MongoClientEntity.class, query, invocationContext);
        if (appEntity == null) return null;
        return session.realms().getClientById(appEntity.getId(), realm);

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
