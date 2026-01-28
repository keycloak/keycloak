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
package org.keycloak.connections.jpa.updater.liquibase.custom;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.RawSqlStatement;

public class JpaUpdate25_0_0_ConsentConstraints  extends CustomKeycloakTask {

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        final String userConsentClientScopeTable = getTableName("USER_CONSENT_CLIENT_SCOPE");
        final String userConsentTable = getTableName("USER_CONSENT");
        statements.add(new RawSqlStatement(
                "DELETE FROM "+ userConsentClientScopeTable + " WHERE USER_CONSENT_ID IN (" +
                        " SELECT uc.ID FROM "+userConsentTable+" uc INNER JOIN (" +
                        " SELECT CLIENT_ID, USER_ID, MAX(LAST_UPDATED_DATE) AS MAX_UPDATED_DATE FROM " + userConsentTable +
                        " GROUP BY CLIENT_ID, USER_ID HAVING COUNT(*) > 1 ) max_dates ON uc.CLIENT_ID = max_dates.CLIENT_ID" +
                        " AND uc.USER_ID = max_dates.USER_ID AND uc.LAST_UPDATED_DATE = max_dates.MAX_UPDATED_DATE)"
        ));
        statements.add(new RawSqlStatement(
                "  DELETE FROM "+userConsentTable+" WHERE ID IN (" +
                        " SELECT uc.ID FROM "+userConsentTable+" uc INNER JOIN (" +
                        " SELECT CLIENT_ID, USER_ID, MAX(LAST_UPDATED_DATE) AS MAX_UPDATED_DATE" +
                        " FROM "+userConsentTable+" GROUP BY CLIENT_ID, USER_ID HAVING COUNT(*) > 1 )" +
                        " max_dates ON uc.CLIENT_ID = max_dates.CLIENT_ID" +
                        " AND uc.USER_ID = max_dates.USER_ID AND uc.LAST_UPDATED_DATE = max_dates.MAX_UPDATED_DATE )"
        ));
        statements.add(new RawSqlStatement(
                " DELETE FROM "+ userConsentClientScopeTable + " WHERE USER_CONSENT_ID IN (" +
                        " SELECT uc.ID FROM "+userConsentTable+" uc INNER JOIN (" +
                        " SELECT CLIENT_STORAGE_PROVIDER, EXTERNAL_CLIENT_ID, USER_ID, MAX(LAST_UPDATED_DATE) AS MAX_UPDATED_DATE" +
                        " FROM "+userConsentTable+" GROUP BY CLIENT_STORAGE_PROVIDER, EXTERNAL_CLIENT_ID, USER_ID HAVING COUNT(*) > 1 )" +
                        " max_dates ON uc.CLIENT_STORAGE_PROVIDER = max_dates.CLIENT_STORAGE_PROVIDER" +
                        " AND uc.EXTERNAL_CLIENT_ID = max_dates.EXTERNAL_CLIENT_ID AND uc.USER_ID = max_dates.USER_ID AND uc.LAST_UPDATED_DATE = max_dates.MAX_UPDATED_DATE )"
        ));
        statements.add(new RawSqlStatement(
                " DELETE FROM "+userConsentTable+" WHERE ID IN (" +
                        " SELECT uc.ID FROM "+userConsentTable+" uc INNER JOIN (" +
                        " SELECT CLIENT_STORAGE_PROVIDER, EXTERNAL_CLIENT_ID, USER_ID, MAX(LAST_UPDATED_DATE) AS MAX_UPDATED_DATE" +
                        " FROM "+userConsentTable+" GROUP BY CLIENT_STORAGE_PROVIDER, EXTERNAL_CLIENT_ID, USER_ID HAVING COUNT(*) > 1 )" +
                        " max_dates ON uc.CLIENT_STORAGE_PROVIDER = max_dates.CLIENT_STORAGE_PROVIDER" +
                        " AND uc.EXTERNAL_CLIENT_ID = max_dates.EXTERNAL_CLIENT_ID AND uc.USER_ID = max_dates.USER_ID AND uc.LAST_UPDATED_DATE = max_dates.MAX_UPDATED_DATE )"
        ));
    }

    @Override
    protected String getTaskId() {
        return "Correct User Consent Unique Constraints for PostgreSQL and MariaDB";
    }

}
