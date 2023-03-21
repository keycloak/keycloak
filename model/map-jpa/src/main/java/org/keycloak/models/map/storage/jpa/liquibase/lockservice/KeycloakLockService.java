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

package org.keycloak.models.map.storage.jpa.liquibase.lockservice;

import liquibase.exception.DatabaseException;
import liquibase.lockservice.StandardLockService;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.snapshot.SnapshotControl;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.structure.core.PrimaryKey;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Table;
import org.jboss.logging.Logger;

/**
 * Extending the Liquibase {@link StandardLockService} for situations where it failed on a H2 database.
 *
 * @author Alexander Schwartz
 */
public class KeycloakLockService extends StandardLockService {

    private static final Logger log = Logger.getLogger(KeycloakLockService.class);

    @Override
    public int getPriority() {
        return super.getPriority() + 1;
    }

    @Override
    protected boolean hasDatabaseChangeLogLockTable() throws DatabaseException {
        boolean originalReturnValue = super.hasDatabaseChangeLogLockTable();
        if (originalReturnValue) {
            /* Liquibase only checks that the table exists. On the H2 database, creation of a table with a primary key is not atomic,
               and the primary key might not be visible yet. The primary key would be needed to prevent inserting the data into the table
               a second time. Inserting it a second time might lead to a failure when creating the primary key, which would then roll back
               the creation of the table. Therefore, at least on the H2 database, checking for the primary key is essential.

               An existing DATABASECHANGELOG might indicate that the insertion of data was completed previously.
               Still, this isn't working with the DBLockTest which deletes only the DATABASECHANGELOGLOCK table.

               See https://github.com/keycloak/keycloak/issues/15487 for more information.
             */
            Table lockTable = (Table) new Table().setName(database.getDatabaseChangeLogLockTableName()).setSchema(
                    new Schema(database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName()));
            SnapshotGeneratorFactory instance = SnapshotGeneratorFactory.getInstance();

            try {
                DatabaseSnapshot snapshot = instance.createSnapshot(lockTable.getSchema().toCatalogAndSchema(), database,
                        new SnapshotControl(database, false, Table.class, PrimaryKey.class).setWarnIfObjectNotFound(false));
                Table lockTableFromSnapshot = snapshot.get(lockTable);
                if (lockTableFromSnapshot == null) {
                    throw new RuntimeException("DATABASECHANGELOGLOCK not found, although Liquibase claims it exists.");
                } else if (lockTableFromSnapshot.getPrimaryKey() == null) {
                    log.warn("Primary key not found - table creation not complete yet.");
                    return false;
                }
            } catch (InvalidExampleException e) {
                throw new RuntimeException(e);
            }
        }
        return originalReturnValue;
    }
}
