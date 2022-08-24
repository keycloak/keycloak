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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.keycloak.models.map.storage.jpa.authSession.entity.JpaAuthenticationSessionMetadata;
import org.keycloak.models.map.storage.jpa.authSession.entity.JpaRootAuthenticationSessionMetadata;
import org.keycloak.models.map.storage.jpa.authorization.permission.entity.JpaPermissionMetadata;
import org.keycloak.models.map.storage.jpa.authorization.policy.entity.JpaPolicyMetadata;
import org.keycloak.models.map.storage.jpa.authorization.resource.entity.JpaResourceMetadata;
import org.keycloak.models.map.storage.jpa.authorization.scope.entity.JpaScopeMetadata;
import org.keycloak.models.map.storage.jpa.authorization.resourceServer.entity.JpaResourceServerMetadata;
import org.keycloak.models.map.storage.jpa.client.entity.JpaClientMetadata;
import org.keycloak.models.map.storage.jpa.clientscope.entity.JpaClientScopeMetadata;
import org.keycloak.models.map.storage.jpa.event.admin.entity.JpaAdminEventMetadata;
import org.keycloak.models.map.storage.jpa.event.auth.entity.JpaAuthEventMetadata;
import org.keycloak.models.map.storage.jpa.group.entity.JpaGroupMetadata;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaAdminEventMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaAuthEventMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaAuthenticationSessionMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaClientMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaClientSessionMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaClientScopeMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaComponentMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaGroupMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaPermissionMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaPolicyMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaRealmMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaResourceMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaResourceServerMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaRoleMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaRootAuthenticationSessionMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaScopeMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaSingleUseObjectMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaUserConsentMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaUserFederatedIdentityMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaUserLoginFailureMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaUserMigration;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.migration.JpaUserSessionMigration;
import org.keycloak.models.map.storage.jpa.loginFailure.entity.JpaUserLoginFailureMetadata;
import org.keycloak.models.map.storage.jpa.realm.entity.JpaComponentMetadata;
import org.keycloak.models.map.storage.jpa.realm.entity.JpaRealmMetadata;
import org.keycloak.models.map.storage.jpa.role.entity.JpaRoleMetadata;
import org.keycloak.models.map.storage.jpa.singleUseObject.entity.JpaSingleUseObjectMetadata;
import org.keycloak.models.map.storage.jpa.user.entity.JpaUserConsentMetadata;
import org.keycloak.models.map.storage.jpa.user.entity.JpaUserFederatedIdentityMetadata;
import org.keycloak.models.map.storage.jpa.user.entity.JpaUserMetadata;
import org.keycloak.models.map.storage.jpa.userSession.entity.JpaClientSessionMetadata;
import org.keycloak.models.map.storage.jpa.userSession.entity.JpaUserSessionMetadata;

import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_ADMIN_EVENT;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_AUTHZ_PERMISSION;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_AUTHZ_POLICY;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_AUTHZ_RESOURCE;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_AUTHZ_RESOURCE_SERVER;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_AUTHZ_SCOPE;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_AUTH_EVENT;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_AUTH_SESSION;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_CLIENT;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_CLIENT_SCOPE;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_CLIENT_SESSION;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_GROUP;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_REALM;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_ROLE;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_SINGLE_USE_OBJECT;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_USER;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_USER_CONSENT;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_USER_FEDERATED_IDENTITY;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_USER_LOGIN_FAILURE;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_USER_SESSION;

public class JpaEntityMigration {

    static final Map<Class<?>, BiFunction<ObjectNode, Integer, ObjectNode>> MIGRATIONS = new HashMap<>();
    static {
        //auth-sessions
        MIGRATIONS.put(JpaAuthenticationSessionMetadata.class,      (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_AUTH_SESSION,      tree, JpaAuthenticationSessionMigration.MIGRATORS));
        MIGRATIONS.put(JpaRootAuthenticationSessionMetadata.class,  (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_AUTH_SESSION,      tree, JpaRootAuthenticationSessionMigration.MIGRATORS));
        //authorization
        MIGRATIONS.put(JpaPermissionMetadata.class,                 (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_AUTHZ_PERMISSION,  tree, JpaPermissionMigration.MIGRATORS));
        MIGRATIONS.put(JpaPolicyMetadata.class,                     (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_AUTHZ_POLICY,      tree, JpaPolicyMigration.MIGRATORS));
        MIGRATIONS.put(JpaResourceMetadata.class,                   (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_AUTHZ_RESOURCE,    tree, JpaResourceMigration.MIGRATORS));
        MIGRATIONS.put(JpaResourceServerMetadata.class,             (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_AUTHZ_RESOURCE_SERVER, tree, JpaResourceServerMigration.MIGRATORS));
        MIGRATIONS.put(JpaScopeMetadata.class,                      (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_AUTHZ_SCOPE,       tree, JpaScopeMigration.MIGRATORS));
        //clients
        MIGRATIONS.put(JpaClientMetadata.class,                     (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_CLIENT,            tree, JpaClientMigration.MIGRATORS));
        //client-scopes
        MIGRATIONS.put(JpaClientScopeMetadata.class,                (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_CLIENT_SCOPE,      tree, JpaClientScopeMigration.MIGRATORS));
        //events
        MIGRATIONS.put(JpaAdminEventMetadata.class,                 (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_ADMIN_EVENT,       tree, JpaAdminEventMigration.MIGRATORS));
        MIGRATIONS.put(JpaAuthEventMetadata.class,                  (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_AUTH_EVENT,        tree, JpaAuthEventMigration.MIGRATORS));
        //groups
        MIGRATIONS.put(JpaGroupMetadata.class,                      (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_GROUP,             tree, JpaGroupMigration.MIGRATORS));
        //realms
        MIGRATIONS.put(JpaComponentMetadata.class,                  (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_REALM,             tree, JpaComponentMigration.MIGRATORS));
        MIGRATIONS.put(JpaRealmMetadata.class,                      (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_REALM,             tree, JpaRealmMigration.MIGRATORS));
        //roles
        MIGRATIONS.put(JpaRoleMetadata.class,                       (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_ROLE,              tree, JpaRoleMigration.MIGRATORS));
        //sessions
        MIGRATIONS.put(JpaClientSessionMetadata.class,              (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_CLIENT_SESSION,    tree, JpaClientSessionMigration.MIGRATORS));
        MIGRATIONS.put(JpaUserSessionMetadata.class,                (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_USER_SESSION,      tree, JpaUserSessionMigration.MIGRATORS));
        //single-use-objects
        MIGRATIONS.put(JpaSingleUseObjectMetadata.class,            (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_SINGLE_USE_OBJECT, tree, JpaSingleUseObjectMigration.MIGRATORS));
        //user-login-failures
        MIGRATIONS.put(JpaUserLoginFailureMetadata.class,           (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_USER_LOGIN_FAILURE, tree, JpaUserLoginFailureMigration.MIGRATORS));
        //users
        MIGRATIONS.put(JpaUserMetadata.class,                       (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_USER,              tree, JpaUserMigration.MIGRATORS));
        MIGRATIONS.put(JpaUserConsentMetadata.class,                (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_USER_CONSENT,      tree, JpaUserConsentMigration.MIGRATORS));
        MIGRATIONS.put(JpaUserFederatedIdentityMetadata.class,      (tree, entityVersion) -> migrateTreeTo(entityVersion, CURRENT_SCHEMA_VERSION_USER_FEDERATED_IDENTITY, tree, JpaUserFederatedIdentityMigration.MIGRATORS));

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
