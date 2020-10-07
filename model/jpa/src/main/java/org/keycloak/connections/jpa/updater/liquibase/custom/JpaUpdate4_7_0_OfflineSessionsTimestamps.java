/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
import liquibase.statement.core.UpdateStatement;
import liquibase.structure.core.Table;
import org.keycloak.common.util.Time;

/**
 * Update CREATED_ON and LAST_SESSION_REFRESH columns to current startup time
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JpaUpdate4_7_0_OfflineSessionsTimestamps extends CustomKeycloakTask {

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        String offlineUserSessionsTableName = database.correctObjectName("OFFLINE_USER_SESSION", Table.class);

        try {
            int currentTime = Time.currentTime();

            UpdateStatement updateStatement = new UpdateStatement(null, null, offlineUserSessionsTableName)
                    .addNewColumnValue("LAST_SESSION_REFRESH", currentTime);

            statements.add(updateStatement);

            confirmationMessage.append("Updated column LAST_SESSION_REFRESH in OFFLINE_USER_SESSION table with time " + currentTime);
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when updating data from previous version", e);
        }
    }

    @Override
    protected String getTaskId() {
        return "Update 4.7.0.Final";
    }
}
