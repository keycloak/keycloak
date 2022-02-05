/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.jpa.hibernate.jsonb;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_CLIENT;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_CLIENT_SCOPE;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_ROLE;
import org.keycloak.models.map.storage.jpa.client.entity.JpaClientMetadata;
import org.keycloak.models.map.storage.jpa.clientscope.entity.JpaClientScopeMetadata;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaClientMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaClientScopeMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaRoleMigration;
import org.keycloak.models.map.storage.jpa.role.entity.JpaRoleMetadata;



public class JpaEntityMigration {

    static final Map<Class<?>, BiFunction<ObjectNode, Integer, ObjectNode>> MIGRATIONS = new HashMap<>();
    static {
        MIGRATIONS.put(JpaClientMetadata.class,      (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_CLIENT,       tree, JpaClientMigration.MIGRATORS));
        MIGRATIONS.put(JpaClientScopeMetadata.class, (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_CLIENT_SCOPE, tree, JpaClientScopeMigration.MIGRATORS));
        MIGRATIONS.put(JpaRoleMetadata.class,        (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_ROLE,         tree, JpaRoleMigration.MIGRATORS));
    }

    private static ObjectNode migrateTreeTo(int entityVersion, Integer supportedVersion, ObjectNode node, List<Function<ObjectNode, ObjectNode>> migrators) {
        if (entityVersion > supportedVersion + 1) throw new IllegalArgumentException("Incompatible entity version: " + entityVersion + ", supportedVersion: " + supportedVersion);

        if (entityVersion < supportedVersion) {
            while (entityVersion < supportedVersion) {
                Function<ObjectNode, ObjectNode> migrator = migrators.get(entityVersion);
                if (migrator != null) {
                    node = migrator.apply(node);
                }
                entityVersion++;
            }
        }
        return node;
    }

}
