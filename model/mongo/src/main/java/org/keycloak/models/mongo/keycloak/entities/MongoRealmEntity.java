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
import org.keycloak.connections.mongo.api.MongoCollection;
import org.keycloak.connections.mongo.api.MongoIdentifiableEntity;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.entities.RealmEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "realms")
public class MongoRealmEntity extends RealmEntity implements MongoIdentifiableEntity {

    @Override
    public void afterRemove(MongoStoreInvocationContext context) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(getId())
                .get();

        // Remove all roles of this realm
        context.getMongoStore().removeEntities(MongoGroupEntity.class, query, true, context);


        // Remove all roles of this realm
        context.getMongoStore().removeEntities(MongoRoleEntity.class, query, true, context);

        // Remove all client templates of this realm
        context.getMongoStore().removeEntities(MongoClientTemplateEntity.class, query, true, context);

        // Remove all client templates of this realm
        context.getMongoStore().removeEntities(MongoGroupEntity.class, query, true, context);

        // Remove all clients of this realm
        context.getMongoStore().removeEntities(MongoClientEntity.class, query, true, context);
    }
}
