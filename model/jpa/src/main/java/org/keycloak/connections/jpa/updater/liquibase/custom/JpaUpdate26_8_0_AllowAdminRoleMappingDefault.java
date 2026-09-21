/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.models.IdentityProviderModel;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.RawParameterizedSqlStatement;

/**
 * Custom SQL change that preserves backward compatibility for identity providers created before the
 * {@link IdentityProviderModel#ALLOW_ADMIN_ROLE_MAPPING} setting was introduced. For those identity providers the
 * setting is absent, which the model defaults to {@code false} (the secure default for new identity providers). To avoid
 * silently breaking existing admin-role mappings on upgrade, this migration explicitly stores
 * {@code allowAdminRoleMapping=true} for every existing identity provider that does not already have the setting.
 */
public class JpaUpdate26_8_0_AllowAdminRoleMappingDefault extends CustomKeycloakTask {

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        String idpTable = getTableName("IDENTITY_PROVIDER");
        String configTable = getTableName("IDENTITY_PROVIDER_CONFIG");

        statements.add(new RawParameterizedSqlStatement(
                "INSERT INTO " + configTable + " (IDENTITY_PROVIDER_ID, NAME, VALUE) " +
                "SELECT ip.INTERNAL_ID, ?, ? FROM " + idpTable + " ip " +
                "WHERE NOT EXISTS (SELECT 1 FROM " + configTable + " c " +
                "WHERE c.IDENTITY_PROVIDER_ID = ip.INTERNAL_ID AND c.NAME = ?)",
                IdentityProviderModel.ALLOW_ADMIN_ROLE_MAPPING,
                Boolean.TRUE.toString(),
                IdentityProviderModel.ALLOW_ADMIN_ROLE_MAPPING
        ));
    }

    @Override
    protected String getTaskId() {
        return "Default allowAdminRoleMapping to true for existing identity providers";
    }
}
