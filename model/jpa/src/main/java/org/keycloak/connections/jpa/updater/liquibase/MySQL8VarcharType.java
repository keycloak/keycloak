/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
import liquibase.database.core.MySQLDatabase;
import liquibase.datatype.DatabaseDataType;
import liquibase.datatype.core.VarcharType;
import liquibase.exception.DatabaseException;

/**
 * Changes VARCHAR type with size greater than 255 to text type for MySQL 8 and newer.
 * 
 * Resolves Limits on Table Column Count and Row Size for MySQL 8
 */
public class MySQL8VarcharType extends VarcharType {

    @Override
    public int getPriority() {
        return super.getPriority() + 1; // Always take precedence over VarcharType
    }

    @Override
    public DatabaseDataType toDatabaseDataType(Database database) {
        if (database instanceof MySQLDatabase) {
            try {
                if (database.getDatabaseMajorVersion() >= 8 && getSize() > 255) {
                    return new DatabaseDataType(database.escapeDataTypeName("TEXT"), getSize());
                }
            } catch (DatabaseException e) {
                throw new RuntimeException(e);
            }
        }
        return super.toDatabaseDataType(database);
    }
}
