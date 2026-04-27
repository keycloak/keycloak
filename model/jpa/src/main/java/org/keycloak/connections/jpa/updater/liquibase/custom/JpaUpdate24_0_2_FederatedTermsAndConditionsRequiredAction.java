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

import org.keycloak.models.UserModel;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.UpdateStatement;
import liquibase.structure.core.Table;

/**
 * Custom liquibase change to migrate legacy {@code terms and conditions} required action for federated users (table
 * {@code FED_USER_REQUIRED_ACTION}, in line with what {@link JpaUpdate21_0_2_TermsAndConditionsRequiredAction}
 * did to migrate the same action for regular users.
 * </p>
 * The legacy value was in lowercase, and it was changed to upper case to match the other required actions in Keycloak.
 * This class ensures that the legacy action set for federated users is properly migrated to upper case when upgrading
 * the server.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JpaUpdate24_0_2_FederatedTermsAndConditionsRequiredAction extends CustomKeycloakTask {

    private static final String TERMS_AND_CONDITION_LEGACY_ALIAS = "terms_and_conditions";

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        statements.add(
            new UpdateStatement(null, null, database.correctObjectName("FED_USER_REQUIRED_ACTION", Table.class))
                .addNewColumnValue("REQUIRED_ACTION", UserModel.RequiredAction.TERMS_AND_CONDITIONS.name())
                .setWhereClause("REQUIRED_ACTION=?")
                .addWhereParameter(TERMS_AND_CONDITION_LEGACY_ALIAS)
        );
    }

    @Override
    protected String getTaskId() {
        return "Federated Terms And Conditions required action alias change (25.0.0)";
    }

}
