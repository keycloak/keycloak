/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.core.MSSQLDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.snapshot.jvm.ForeignKeySnapshotGenerator;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.ForeignKeyConstraintType;

/**
 * This class overrides original ForeignKeySnapshotGenerator from liquibase 3.5.5. 
 * It contains fix https://liquibase.jira.com/browse/CORE-3141
 */
public class CustomForeignKeySnapshotGenerator extends ForeignKeySnapshotGenerator {

    public CustomForeignKeySnapshotGenerator() {
        super();
    }

    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        return super.getPriority(objectType, database) + 1;
    }

    @Override
    protected ForeignKeyConstraintType convertToForeignKeyConstraintType(Integer jdbcType, Database database) throws DatabaseException {
        if (jdbcType == null) {
            return ForeignKeyConstraintType.importedKeyRestrict;
        }
        if (driverUsesSpFkeys(database)) {
            switch (jdbcType) {
                case 0:
                    return ForeignKeyConstraintType.importedKeyCascade;
                case 1:
                    return ForeignKeyConstraintType.importedKeyNoAction;
                case 2:
                    return ForeignKeyConstraintType.importedKeySetNull;
                case 3:
                    return ForeignKeyConstraintType.importedKeySetDefault;
                default:
                    throw new DatabaseException("Unknown constraint type: " + jdbcType);
            }
        } else {
            switch (jdbcType) {
                case DatabaseMetaData.importedKeyCascade:
                    return ForeignKeyConstraintType.importedKeyCascade;
                case DatabaseMetaData.importedKeyNoAction:
                    return ForeignKeyConstraintType.importedKeyNoAction;
                case DatabaseMetaData.importedKeyRestrict:
                    if (database instanceof MSSQLDatabase) {
                        //mssql doesn't support restrict. Not sure why it comes back with this type sometimes
                        return ForeignKeyConstraintType.importedKeyNoAction;
                    } else {
                        return ForeignKeyConstraintType.importedKeyRestrict;
                    }
                case DatabaseMetaData.importedKeySetDefault:
                    return ForeignKeyConstraintType.importedKeySetDefault;
                case DatabaseMetaData.importedKeySetNull:
                    return ForeignKeyConstraintType.importedKeySetNull;
                default:
                    throw new DatabaseException("Unknown constraint type: " + jdbcType);
            }
        }
    }

    /*
    * Sql server JDBC drivers prior to 6.3.3 used sp_fkeys to determine the delete/cascade metadata.
    * The sp_fkeys stored procedure spec says that returned integer values of 0, 1, 2, or 4
    * translate to cascade, noAction, SetNull, or SetDefault which are not the values in the JDBC
    * standard.
    *
    * If this method returns true, the sp_fkeys values should be used. Otherwise use the standard jdbc logic
    *
    * The change in logic went in with https://github.com/Microsoft/mssql-jdbc/pull/490
    */
    private boolean driverUsesSpFkeys(Database database) throws DatabaseException {
        if (!(database instanceof MSSQLDatabase)) {
            return false;
        }
        DatabaseConnection connection = database.getConnection();
        if (!(connection instanceof JdbcConnection)) {
            return false;
        }

        try {
            DatabaseMetaData metaData = ((JdbcConnection) connection).getMetaData();
            int driverMajorVersion = metaData.getDriverMajorVersion();
            int driverMinorVersion= metaData.getDriverMinorVersion();
            String driverName = metaData.getDriverName();

            if (!driverName.startsWith("Microsoft")) {
                return false;
            }

            return !(driverMajorVersion > 6 || (driverMajorVersion == 6 && driverMinorVersion >= 3));
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

}
