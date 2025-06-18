/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

public class JpaUpdate19_0_0_DefaultPostLogoutRedirectUri extends CustomKeycloakTask {

    private static final String POST_LOGOUT_REDIRECT_URIS = "post.logout.redirect.uris";

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        final String clientAttributesTable = getTableName("CLIENT_ATTRIBUTES");
        final String redirectUrisTable = getTableName("REDIRECT_URIS");
        statements.add(new RawSqlStatement(
                "INSERT INTO " + clientAttributesTable + " (CLIENT_ID,NAME,VALUE) " +
                "SELECT DISTINCT CLIENT_ID, '" + POST_LOGOUT_REDIRECT_URIS + "', '+' FROM " + redirectUrisTable + " WHERE CLIENT_ID NOT IN " +
                "(SELECT CLIENT_ID FROM " + clientAttributesTable + " WHERE NAME = '" + POST_LOGOUT_REDIRECT_URIS + "')"
        ));
    }

    @Override
    protected String getTaskId() {
        return "Default post_logout_redirect_uris (19.0.0)";
    }

}
