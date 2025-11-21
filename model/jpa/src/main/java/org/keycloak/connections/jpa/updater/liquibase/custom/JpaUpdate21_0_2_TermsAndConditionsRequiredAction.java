/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.models.UserModel;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.UpdateStatement;
import liquibase.structure.core.Table;

public class JpaUpdate21_0_2_TermsAndConditionsRequiredAction extends CustomKeycloakTask {

    private static final String TERMS_AND_CONDITION_LEGACY_ALIAS = "terms_and_conditions";

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        statements.add(
            new UpdateStatement(null, null,  database.correctObjectName("REQUIRED_ACTION_PROVIDER", Table.class))
                .addNewColumnValue("ALIAS", UserModel.RequiredAction.TERMS_AND_CONDITIONS.name())
                .addNewColumnValue("PROVIDER_ID", UserModel.RequiredAction.TERMS_AND_CONDITIONS.name())
                .setWhereClause("ALIAS=?")
                .addWhereParameter(TERMS_AND_CONDITION_LEGACY_ALIAS)
        );

        statements.add(
            new UpdateStatement(null, null, database.correctObjectName("USER_REQUIRED_ACTION", Table.class))
                .addNewColumnValue("REQUIRED_ACTION", UserModel.RequiredAction.TERMS_AND_CONDITIONS.name())
                .setWhereClause("REQUIRED_ACTION=?")
                .addWhereParameter(TERMS_AND_CONDITION_LEGACY_ALIAS)
        );
    }

    @Override
    protected String getTaskId() {
        return "TermsAndConditions required action alias and providerId change (21.0.2)";
    }

}
