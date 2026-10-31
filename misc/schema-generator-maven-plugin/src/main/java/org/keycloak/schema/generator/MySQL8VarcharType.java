package org.keycloak.schema.generator;

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
