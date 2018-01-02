/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
import liquibase.structure.core.Table;

import liquibase.database.core.MSSQLDatabase;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.RawSqlStatement;

public class JpaUpdateAuthz_3_4_0_CR1 extends CustomKeycloakTask {

    private SqlStatement generateUpdateStatement(String resourceServerDetailTable) {
        String resourceServerTableName = database.correctObjectName(getTableName("RESOURCE_SERVER"), Table.class);
        String resourceServerDetailTableName = database.correctObjectName(getTableName(resourceServerDetailTable), Table.class);

        if (database instanceof MSSQLDatabase) {
//            UPDATE RESOURCE_SERVER_POLICY   SET RESOURCE_SERVER_CLIENT_ID = s.CLIENT_ID FROM (SELECT ID, CLIENT_ID FROM RESOURCE_SERVER) s WHERE s.ID = RESOURCE_SERVER_POLICY.RESOURCE_SERVER_ID;
//            UPDATE RESOURCE_SERVER_RESOURCE SET RESOURCE_SERVER_CLIENT_ID = s.CLIENT_ID FROM (SELECT ID, CLIENT_ID FROM RESOURCE_SERVER) s WHERE s.ID = RESOURCE_SERVER_RESOURCE.RESOURCE_SERVER_ID;
//            UPDATE RESOURCE_SERVER_SCOPE    SET RESOURCE_SERVER_CLIENT_ID = s.CLIENT_ID FROM (SELECT ID, CLIENT_ID FROM RESOURCE_SERVER) s WHERE s.ID = RESOURCE_SERVER_SCOPE.RESOURCE_SERVER_ID;
            return new RawSqlStatement(
              "UPDATE "
              + resourceServerDetailTableName
                + " SET RESOURCE_SERVER_CLIENT_ID = s.CLIENT_ID FROM "
                  + " (SELECT ID, CLIENT_ID FROM "
                  + resourceServerTableName
                  + ") s "
                + " WHERE s.ID = "
                + resourceServerDetailTableName
                + ".RESOURCE_SERVER_ID"
            );
        } else {
//          UPDATE RESOURCE_SERVER_POLICY p   SET RESOURCE_SERVER_CLIENT_ID = (SELECT CLIENT_ID FROM RESOURCE_SERVER s WHERE s.ID = p.RESOURCE_SERVER_ID);
//          UPDATE RESOURCE_SERVER_RESOURCE p SET RESOURCE_SERVER_CLIENT_ID = (SELECT CLIENT_ID FROM RESOURCE_SERVER s WHERE s.ID = p.RESOURCE_SERVER_ID);
//          UPDATE RESOURCE_SERVER_SCOPE p    SET RESOURCE_SERVER_CLIENT_ID = (SELECT CLIENT_ID FROM RESOURCE_SERVER s WHERE s.ID = p.RESOURCE_SERVER_ID);
            return new RawSqlStatement(
              "UPDATE "
              + resourceServerDetailTableName
              + " p SET RESOURCE_SERVER_CLIENT_ID = "
                + "(SELECT CLIENT_ID FROM "
                + resourceServerTableName
                + " s WHERE s.ID = p.RESOURCE_SERVER_ID)"
            );
        }

    }

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        statements.add(generateUpdateStatement("RESOURCE_SERVER_POLICY"));
        statements.add(generateUpdateStatement("RESOURCE_SERVER_RESOURCE"));
        statements.add(generateUpdateStatement("RESOURCE_SERVER_SCOPE"));
    }

    @Override
    protected String getTaskId() {
        return "Update authz-3.4.0.CR1-resource-server-pk-change-part2";
    }
}
