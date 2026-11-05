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
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate24_0_2_FederatedTermsAndConditionsRequiredAction}.
 */
public class JpaUpdate24_0_2_FederatedTermsAndConditionsRequiredActionTask implements JpaMigrationTask {

    private static final String TERMS_AND_CONDITION_LEGACY_ALIAS = "terms_and_conditions";

    @Override
    public String getTaskId() {
        return "Federated Terms And Conditions required action alias change (25.0.0)";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException {
        String table = SqlDialect.qualified("FED_USER_REQUIRED_ACTION", defaultSchema);
        String newAlias = dialect.literal(UserModel.RequiredAction.TERMS_AND_CONDITIONS.name());
        String legacyAlias = dialect.literal(TERMS_AND_CONDITION_LEGACY_ALIAS);
        return List.of(
                "UPDATE " + table
                        + " SET REQUIRED_ACTION = " + newAlias
                        + " WHERE REQUIRED_ACTION = " + legacyAlias
        );
    }
}
