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

package org.keycloak.connections.jpa.updater.liquibase;

import liquibase.database.Database;
import liquibase.database.core.MSSQLDatabase;
import liquibase.datatype.DatabaseDataType;
import liquibase.datatype.core.ClobType;
import liquibase.util.StringUtils;

/**
 * Changes TEXT data type to VARCHAR for a MSSQL database.
 *
 * Example: TEXT[(n)] -> VARCHAR(max)
 */
public class MSSQLTextType extends ClobType {
    @Override
    public int getPriority() {
        return super.getPriority() + 1; // Always take precedence over ClobType
    }

    @Override
    public DatabaseDataType toDatabaseDataType(Database database) {
        if (database instanceof MSSQLDatabase) {
            String originalDefinition = StringUtils.trimToEmpty(this.getRawDefinition());
            if (originalDefinition.matches("^(?i)\\[?text\\]?.*")) {
                DatabaseDataType type = new DatabaseDataType(database.escapeDataTypeName("varchar"));
                type.addAdditionalInformation("(max)");
                return type;
            }
        }
        return super.toDatabaseDataType(database);
    }
}
