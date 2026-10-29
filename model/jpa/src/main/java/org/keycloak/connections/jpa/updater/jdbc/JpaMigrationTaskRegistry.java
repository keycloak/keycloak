/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.connections.jpa.updater.jdbc;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.keycloak.connections.jpa.updater.jdbc.task.AddRealmCodeSecretTask;
import org.keycloak.connections.jpa.updater.jdbc.task.AuthzResourceUseMoreURIsTask;
import org.keycloak.connections.jpa.updater.jdbc.task.ExtractRealmKeysFromRealmTableTask;
import org.keycloak.connections.jpa.updater.jdbc.task.FederatedUserAttributeTextColumnMigrationTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate13_0_0_MigrateDefaultRolesTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate14_0_0_MigrateSamlArtifactAttributeTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate19_0_0_DefaultPostLogoutRedirectUriTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate1_2_0_Beta1Task;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate1_2_0_CR1Task;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate1_4_0_FinalTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate1_9_0_FinalTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate21_0_2_TermsAndConditionsRequiredActionTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate22_0_0_RemoveRhssoThemesTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate22_0_5_UpdateAccountThemeTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate24_0_2_FederatedTermsAndConditionsRequiredActionTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate25_0_0_ConsentConstraintsTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate25_0_0_MySQL_ConsentConstraintsTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate26_0_0_IdentityProviderAttributesMigrationTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate26_0_0_OrganizationGroupTypeTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate26_2_6_RemoveDuplicateMigrationModelTimeTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate26_2_6_RemoveDuplicateMigrationModelVersionTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate26_4_0_SamlEncryptionAttributesTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate26_5_0_OrgTablesSetDefaultCharsetAndCollationTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate26_6_0_OfflineClientSessionRealmTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate26_7_0_RealmDisplayNameTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate26_8_0_BackfillGroupOrgIdTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate26_8_0_DomainIdpRoutingMigrationTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate26_8_0_IdpMapperOrganizationIdTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate26_8_0_PopulateOrgDomainRealmIdTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate4_0_0_DefaultClientScopesTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate4_7_0_OfflineSessionsTimestampsTask;
import org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdateAuthz_3_4_0_CR1Task;
import org.keycloak.connections.jpa.updater.jdbc.task.MigrateUserFedToComponentTask;
import org.keycloak.connections.jpa.updater.jdbc.task.PortLdapUserFedToComponentModelTask;
import org.keycloak.connections.jpa.updater.jdbc.task.RemoveDuplicateOfflineSessionsTask;

/**
 * Registry that maps the {@code className} strings stored in the migration manifest to
 * {@link JpaMigrationTask} instances.
 *
 * <p>The manifest is generated from the Liquibase changelogs, which reference the tasks by their historic
 * {@code org.keycloak.connections.jpa.updater.liquibase.custom.*} class names. Those classes no longer exist;
 * the registry maps the historic names to the current implementations.
 *
 * <p>Tasks are created fresh for each lookup (they are stateless beyond the scope of one
 * {@code generateStatements} call).
 */
public final class JpaMigrationTaskRegistry {

    private static final Map<String, Supplier<JpaMigrationTask>> REGISTRY = new HashMap<>();

    static {
        register("AddRealmCodeSecret",                               AddRealmCodeSecretTask::new);
        register("AuthzResourceUseMoreURIs",                         AuthzResourceUseMoreURIsTask::new);
        register("ExtractRealmKeysFromRealmTable",                   ExtractRealmKeysFromRealmTableTask::new);
        register("FederatedUserAttributeTextColumnMigration",        FederatedUserAttributeTextColumnMigrationTask::new);
        register("JpaUpdate1_2_0_Beta1",                             JpaUpdate1_2_0_Beta1Task::new);
        register("JpaUpdate1_2_0_CR1",                               JpaUpdate1_2_0_CR1Task::new);
        register("JpaUpdate13_0_0_MigrateDefaultRoles",              JpaUpdate13_0_0_MigrateDefaultRolesTask::new);
        register("JpaUpdate14_0_0_MigrateSamlArtifactAttribute",     JpaUpdate14_0_0_MigrateSamlArtifactAttributeTask::new);
        register("JpaUpdate1_4_0_Final",                             JpaUpdate1_4_0_FinalTask::new);
        register("JpaUpdate19_0_0_DefaultPostLogoutRedirectUri",     JpaUpdate19_0_0_DefaultPostLogoutRedirectUriTask::new);
        register("JpaUpdate1_9_0_Final",                             JpaUpdate1_9_0_FinalTask::new);
        register("JpaUpdate21_0_2_TermsAndConditionsRequiredAction", JpaUpdate21_0_2_TermsAndConditionsRequiredActionTask::new);
        register("JpaUpdate22_0_0_RemoveRhssoThemes",                JpaUpdate22_0_0_RemoveRhssoThemesTask::new);
        register("JpaUpdate22_0_5_UpdateAccountTheme",               JpaUpdate22_0_5_UpdateAccountThemeTask::new);
        register("JpaUpdate24_0_2_FederatedTermsAndConditionsRequiredAction",
                 JpaUpdate24_0_2_FederatedTermsAndConditionsRequiredActionTask::new);
        register("JpaUpdate25_0_0_ConsentConstraints",               JpaUpdate25_0_0_ConsentConstraintsTask::new);
        register("JpaUpdate25_0_0_MySQL_ConsentConstraints",         JpaUpdate25_0_0_MySQL_ConsentConstraintsTask::new);
        register("JpaUpdate26_0_0_IdentityProviderAttributesMigration",
                 JpaUpdate26_0_0_IdentityProviderAttributesMigrationTask::new);
        register("JpaUpdate26_0_0_OrganizationGroupType",            JpaUpdate26_0_0_OrganizationGroupTypeTask::new);
        register("JpaUpdate26_2_6_RemoveDuplicateMigrationModelTime",
                 JpaUpdate26_2_6_RemoveDuplicateMigrationModelTimeTask::new);
        register("JpaUpdate26_2_6_RemoveDuplicateMigrationModelVersion",
                 JpaUpdate26_2_6_RemoveDuplicateMigrationModelVersionTask::new);
        register("JpaUpdate26_4_0_SamlEncryptionAttributes",         JpaUpdate26_4_0_SamlEncryptionAttributesTask::new);
        register("JpaUpdate26_5_0_OrgTablesSetDefaultCharsetAndCollation",
                 JpaUpdate26_5_0_OrgTablesSetDefaultCharsetAndCollationTask::new);
        register("JpaUpdate26_6_0_OfflineClientSessionRealm",        JpaUpdate26_6_0_OfflineClientSessionRealmTask::new);
        register("JpaUpdate26_7_0_RealmDisplayName",                 JpaUpdate26_7_0_RealmDisplayNameTask::new);
        register("JpaUpdate26_8_0_BackfillGroupOrgId",               JpaUpdate26_8_0_BackfillGroupOrgIdTask::new);
        register("JpaUpdate26_8_0_DomainIdpRoutingMigration",        JpaUpdate26_8_0_DomainIdpRoutingMigrationTask::new);
        register("JpaUpdate26_8_0_IdpMapperOrganizationId",          JpaUpdate26_8_0_IdpMapperOrganizationIdTask::new);
        register("JpaUpdate26_8_0_PopulateOrgDomainRealmId",         JpaUpdate26_8_0_PopulateOrgDomainRealmIdTask::new);
        register("JpaUpdate4_0_0_DefaultClientScopes",               JpaUpdate4_0_0_DefaultClientScopesTask::new);
        register("JpaUpdate4_7_0_OfflineSessionsTimestamps",         JpaUpdate4_7_0_OfflineSessionsTimestampsTask::new);
        register("JpaUpdateAuthz_3_4_0_CR1",                         JpaUpdateAuthz_3_4_0_CR1Task::new);
        register("MigrateUserFedToComponent",                        MigrateUserFedToComponentTask::new);
        register("PortLdapUserFedToComponentModel",                  PortLdapUserFedToComponentModelTask::new);
        register("RemoveDuplicateOfflineSessions",                   RemoveDuplicateOfflineSessionsTask::new);
    }

    private static void register(String simpleClassName, Supplier<JpaMigrationTask> factory) {
        String fqn = "org.keycloak.connections.jpa.updater.liquibase.custom." + simpleClassName;
        REGISTRY.put(fqn, factory);
    }

    /**
     * Returns a fresh {@link JpaMigrationTask} instance for the given class name.
     * Looks up built-in historic tasks first, and falls back to dynamic class loading
     * for custom tasks. Returns {@code null} if no task is found or if the class
     * does not implement {@link JpaMigrationTask}.
     *
     * @param className fully-qualified class name from the manifest
     */
    public static JpaMigrationTask get(String className) {
        Supplier<JpaMigrationTask> factory = REGISTRY.get(className);
        if (factory != null) {
            return factory.get();
        }
        try {
            ClassLoader cl = getClassLoader();
            Class<?> clazz = Class.forName(className, true, cl);
            if (JpaMigrationTask.class.isAssignableFrom(clazz)) {
                return (JpaMigrationTask) clazz.getDeclaredConstructor().newInstance();
            }
        } catch (ClassNotFoundException e) {
            // Not a class or not found
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate JpaMigrationTask '" + className + "': " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Returns {@code true} if a {@link JpaMigrationTask} is registered or loadable for the given class.
     */
    public static boolean isRegistered(String className) {
        if (REGISTRY.containsKey(className)) {
            return true;
        }
        try {
            ClassLoader cl = getClassLoader();
            Class<?> clazz = Class.forName(className, false, cl);
            return JpaMigrationTask.class.isAssignableFrom(clazz);
        } catch (Throwable t) {
            return false;
        }
    }

    private static ClassLoader getClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl != null ? cl : JpaMigrationTaskRegistry.class.getClassLoader();
    }
}
