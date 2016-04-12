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

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.mongo.keycloak.entities.MongoClientEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRealmEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Wrapper around RoleData object, which will persist wrapped object after each set operation (compatibility with picketlink based idm)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RoleAdapter extends AbstractMongoAdapter<MongoRoleEntity> implements RoleModel {

    private final MongoRoleEntity role;
    private RoleContainerModel roleContainer;
    private RealmModel realm;
    private KeycloakSession session;

    public RoleAdapter(KeycloakSession session, RealmModel realm, MongoRoleEntity roleEntity, MongoStoreInvocationContext invContext) {
        this(session, realm, roleEntity, null, invContext);
    }

    public RoleAdapter(KeycloakSession session, RealmModel realm, MongoRoleEntity roleEntity, RoleContainerModel roleContainer, MongoStoreInvocationContext invContext) {
        super(invContext);
        this.role = roleEntity;
        this.roleContainer = roleContainer;
        this.realm = realm;
        this.session = session;
    }

    @Override
    public String getId() {
        return role.getId();
    }

    @Override
    public String getName() {
        return role.getName();
    }

    @Override
    public void setName(String name) {
        role.setName(name);
        updateRole();
    }

    @Override
    public String getDescription() {
        return role.getDescription();
    }

    @Override
    public void setDescription(String description) {
        role.setDescription(description);
        updateRole();
    }

    @Override
    public boolean isScopeParamRequired() {
        return role.isScopeParamRequired();
    }

    @Override
    public void setScopeParamRequired(boolean scopeParamRequired) {
        role.setScopeParamRequired(scopeParamRequired);
        updateRole();
    }

    @Override
    public boolean isComposite() {
        return role.getCompositeRoleIds() != null && role.getCompositeRoleIds().size() > 0;
    }

    protected void updateRole() {
        super.updateMongoEntity();
    }

    @Override
    public void addCompositeRole(RoleModel childRole) {
        getMongoStore().pushItemToList(role, "compositeRoleIds", childRole.getId(), true, invocationContext);
    }

    @Override
    public void removeCompositeRole(RoleModel childRole) {
        getMongoStore().pullItemFromList(role, "compositeRoleIds", childRole.getId(), invocationContext);
    }

    @Override
    public Set<RoleModel> getComposites() {
        if (role.getCompositeRoleIds() == null || role.getCompositeRoleIds().isEmpty()) {
            return Collections.EMPTY_SET;
        }

        DBObject query = new QueryBuilder()
                .and("_id").in(role.getCompositeRoleIds())
                .get();
        List<MongoRoleEntity> childRoles = getMongoStore().loadEntities(MongoRoleEntity.class, query, invocationContext);

        Set<RoleModel> set = new HashSet<RoleModel>();
        for (MongoRoleEntity childRole : childRoles) {
            set.add(new RoleAdapter(session, realm, childRole, invocationContext));
        }
        return set;
    }

    @Override
    public boolean isClientRole() {
        return role.getClientId() != null;
    }



    @Override
    public String getContainerId() {
        if (isClientRole()) return role.getClientId();
        else return role.getRealmId();
    }


    @Override
    public RoleContainerModel getContainer() {
        if (roleContainer == null) {
            // Compute it
            if (role.getRealmId() != null) {
                MongoRealmEntity realm = getMongoStore().loadEntity(MongoRealmEntity.class, role.getRealmId(), invocationContext);
                if (realm == null) {
                    throw new IllegalStateException("Realm with id: " + role.getRealmId() + " doesn't exists");
                }
                roleContainer = new RealmAdapter(session, realm, invocationContext);
            } else if (role.getClientId() != null) {
                MongoClientEntity appEntity = getMongoStore().loadEntity(MongoClientEntity.class, role.getClientId(), invocationContext);
                if (appEntity == null) {
                    throw new IllegalStateException("Application with id: " + role.getClientId() + " doesn't exists");
                }
                roleContainer = new ClientAdapter(session, realm, appEntity, invocationContext);
            } else {
                throw new IllegalStateException("Both realmId and clientId are null for role: " + this);
            }
        }
        return roleContainer;
    }

    @Override
    public boolean hasRole(RoleModel role) {
        if (this.equals(role)) return true;
        if (!isComposite()) return false;

        Set<RoleModel> visited = new HashSet<RoleModel>();
        return KeycloakModelUtils.searchFor(role, this, visited);
    }

    public MongoRoleEntity getRole() {
        return role;
    }

    @Override
    public MongoRoleEntity getMongoEntity() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof RoleModel)) return false;

        RoleModel that = (RoleModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
