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
import java.util.ArrayList;
import java.util.List;

import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.connections.jpa.updater.jdbc.NativeSqlHelper;
import org.keycloak.models.KeycloakSession;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate25_0_0_MySQL_ConsentConstraints}.
 * MySQL/MariaDB workaround: uses temp tables because MySQL does not allow DELETE with subquery
 * targeting the same table.
 */
public class JpaUpdate25_0_0_MySQL_ConsentConstraintsTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Correct User Consent Unique Constraints for MySQL";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema)
            throws SQLException {
        String scopeTable = NativeSqlHelper.qualified("USER_CONSENT_CLIENT_SCOPE", defaultSchema);
        String consentTable = NativeSqlHelper.qualified("USER_CONSENT", defaultSchema);
        List<String> stmts = new ArrayList<>();

        // Pass 1: CLIENT_ID / USER_ID duplicates
        stmts.add("DELETE FROM " + scopeTable + " WHERE USER_CONSENT_ID IN ("
                + " SELECT uc.ID FROM " + consentTable + " uc INNER JOIN ("
                + " SELECT CLIENT_ID, USER_ID, MAX(LAST_UPDATED_DATE) AS MAX_UPDATED_DATE FROM " + consentTable
                + " GROUP BY CLIENT_ID, USER_ID HAVING COUNT(*) > 1 ) max_dates ON uc.CLIENT_ID = max_dates.CLIENT_ID"
                + " AND uc.USER_ID = max_dates.USER_ID AND uc.LAST_UPDATED_DATE = max_dates.MAX_UPDATED_DATE)");

        stmts.add("CREATE TABLE TEMP_USER_CONSENT_IDS (ID VARCHAR(36) NOT NULL, PRIMARY KEY (ID))");
        stmts.add("INSERT INTO TEMP_USER_CONSENT_IDS SELECT uc.ID FROM " + consentTable + " uc INNER JOIN ("
                + " SELECT CLIENT_ID, USER_ID, MAX(LAST_UPDATED_DATE) AS MAX_UPDATED_DATE"
                + " FROM " + consentTable + " GROUP BY CLIENT_ID, USER_ID HAVING COUNT(*) > 1 )"
                + " max_dates ON uc.CLIENT_ID = max_dates.CLIENT_ID"
                + " AND uc.USER_ID = max_dates.USER_ID AND uc.LAST_UPDATED_DATE = max_dates.MAX_UPDATED_DATE");
        stmts.add("DELETE FROM " + consentTable + " WHERE ID IN (SELECT ID FROM TEMP_USER_CONSENT_IDS)");
        stmts.add("DROP TABLE IF EXISTS TEMP_USER_CONSENT_IDS");

        // Pass 2: CLIENT_STORAGE_PROVIDER / EXTERNAL_CLIENT_ID / USER_ID duplicates
        stmts.add("DELETE FROM " + scopeTable + " WHERE USER_CONSENT_ID IN ("
                + " SELECT uc.ID FROM " + consentTable + " uc INNER JOIN ("
                + " SELECT CLIENT_STORAGE_PROVIDER, EXTERNAL_CLIENT_ID, USER_ID, MAX(LAST_UPDATED_DATE) AS MAX_UPDATED_DATE"
                + " FROM " + consentTable + " GROUP BY CLIENT_STORAGE_PROVIDER, EXTERNAL_CLIENT_ID, USER_ID HAVING COUNT(*) > 1 )"
                + " max_dates ON uc.CLIENT_STORAGE_PROVIDER = max_dates.CLIENT_STORAGE_PROVIDER"
                + " AND uc.EXTERNAL_CLIENT_ID = max_dates.EXTERNAL_CLIENT_ID AND uc.USER_ID = max_dates.USER_ID"
                + " AND uc.LAST_UPDATED_DATE = max_dates.MAX_UPDATED_DATE )");

        stmts.add("CREATE TABLE TEMP_USER_CONSENT_IDS2 (ID VARCHAR(36) NOT NULL, PRIMARY KEY (ID))");
        stmts.add("INSERT INTO TEMP_USER_CONSENT_IDS2 SELECT uc.ID FROM " + consentTable + " uc INNER JOIN ("
                + " SELECT CLIENT_STORAGE_PROVIDER, EXTERNAL_CLIENT_ID, USER_ID, MAX(LAST_UPDATED_DATE) AS MAX_UPDATED_DATE"
                + " FROM " + consentTable + " GROUP BY CLIENT_STORAGE_PROVIDER, EXTERNAL_CLIENT_ID, USER_ID HAVING COUNT(*) > 1 )"
                + " max_dates ON uc.CLIENT_STORAGE_PROVIDER = max_dates.CLIENT_STORAGE_PROVIDER"
                + " AND uc.EXTERNAL_CLIENT_ID = max_dates.EXTERNAL_CLIENT_ID AND uc.USER_ID = max_dates.USER_ID"
                + " AND uc.LAST_UPDATED_DATE = max_dates.MAX_UPDATED_DATE");
        stmts.add("DELETE FROM " + consentTable + " WHERE ID IN (SELECT ID FROM TEMP_USER_CONSENT_IDS2)");
        stmts.add("DROP TABLE IF EXISTS TEMP_USER_CONSENT_IDS2");

        return stmts;
    }
}
