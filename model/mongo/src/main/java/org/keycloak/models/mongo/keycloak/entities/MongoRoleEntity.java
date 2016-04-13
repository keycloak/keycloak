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

package org.keycloak.models.mongo.keycloak.entities;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.jboss.logging.Logger;
import org.keycloak.connections.mongo.api.MongoCollection;
import org.keycloak.connections.mongo.api.MongoField;
import org.keycloak.connections.mongo.api.MongoIdentifiableEntity;
import org.keycloak.connections.mongo.api.MongoStore;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.entities.RoleEntity;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "roles")
public class MongoRoleEntity extends RoleEntity implements MongoIdentifiableEntity {

    private static final Logger logger = Logger.getLogger(MongoRoleEntity.class);

    @MongoField
    // TODO This is required as Mongo doesn't support sparse indexes with compound keys (see https://jira.mongodb.org/browse/SERVER-2193)
    public String getNameIndex() {
        String realmId = getRealmId();
        String clientId = getClientId();
        String name = getName();

        if (realmId != null) {
            return realmId + "//" + name;
        } else {
            return clientId + "//" + name;
        }
    }

    public void setNameIndex(String ignored) {
    }

    @Override
    public void afterRemove(MongoStoreInvocationContext invContext) {
        MongoStore mongoStore = invContext.getMongoStore();

        // Remove from groups
        DBObject query = new QueryBuilder()
                .and("roleIds").is(getId())
                .get();

        List<MongoGroupEntity> groups = mongoStore.loadEntities(MongoGroupEntity.class, query, invContext);
        for (MongoGroupEntity group : groups) {
            mongoStore.pullItemFromList(group, "roleIds", getId(), invContext);
        }


        // Remove this scope from all clients, which has it
        query = new QueryBuilder()
                .and("scopeIds").is(getId())
                .get();

        List<MongoClientEntity> clients = mongoStore.loadEntities(MongoClientEntity.class, query, invContext);
        for (MongoClientEntity client : clients) {
            //logger.info("Removing scope " + getName() + " from user " + user.getUsername());
            mongoStore.pullItemFromList(client, "scopeIds", getId(), invContext);
        }

        // Remove this scope from all clientTemplates, which has it
        List<MongoClientTemplateEntity> clientTemplates = mongoStore.loadEntities(MongoClientTemplateEntity.class, query, invContext);
        for (MongoClientTemplateEntity clientTemplate : clientTemplates) {
            //logger.info("Removing scope " + getName() + " from user " + user.getUsername());
            mongoStore.pullItemFromList(clientTemplate, "scopeIds", getId(), invContext);
        }

        // Remove this role from others who has it as composite
        query = new QueryBuilder()
                .and("compositeRoleIds").is(getId())
                .get();
        List<MongoRoleEntity> parentRoles = mongoStore.loadEntities(MongoRoleEntity.class, query, invContext);
        for (MongoRoleEntity role : parentRoles) {
            mongoStore.pullItemFromList(role, "compositeRoleIds", getId(), invContext);
        }
    }
}
