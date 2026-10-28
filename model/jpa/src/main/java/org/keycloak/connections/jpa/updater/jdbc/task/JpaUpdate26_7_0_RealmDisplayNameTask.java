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
package org.keycloak.connections.jpa.updater.jdbc.task;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.connections.jpa.updater.jdbc.NativeSqlHelper;
import org.keycloak.models.KeycloakSession;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate26_7_0_RealmDisplayName}.
 * Copies the displayName realm attribute into the new REALM.DISPLAY_NAME column.
 */
public class JpaUpdate26_7_0_RealmDisplayNameTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Migrate displayName from REALM_ATTRIBUTE to REALM column";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema)
            throws SQLException {
        String realmTable = NativeSqlHelper.qualified("REALM", defaultSchema);
        String realmAttributeTable = NativeSqlHelper.qualified("REALM_ATTRIBUTE", defaultSchema);
        return List.of(
                "UPDATE " + realmTable + " SET DISPLAY_NAME = ("
                        + "SELECT VALUE FROM " + realmAttributeTable
                        + " WHERE REALM_ID = " + realmTable + ".ID AND NAME = 'displayName'"
                        + ") WHERE EXISTS ("
                        + "SELECT 1 FROM " + realmAttributeTable
                        + " WHERE REALM_ID = " + realmTable + ".ID AND NAME = 'displayName'"
                        + ")"
        );
    }
}
