/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.jpa.liquibase.extension;

import liquibase.change.core.LoadDataChange;
import liquibase.database.Database;
import liquibase.database.core.CockroachDatabase;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.DatabaseDataType;
import liquibase.datatype.LiquibaseDataType;

/**
 * A {@link LiquibaseDataType} to handle hashed value of other column. 
 */
@DataTypeInfo(name="kc_hash", minParameters = 0, maxParameters = 0, priority = LiquibaseDataType.PRIORITY_DEFAULT)
public class KeycloakHashDataType extends LiquibaseDataType {

    @Override
    public DatabaseDataType toDatabaseDataType(Database database) {
        if (database instanceof CockroachDatabase) {
            // currently cockroachdb returns string
            // there is open issue to make it compatible with postgres: https://github.com/cockroachdb/cockroach/issues/73896
            return new DatabaseDataType("STRING", super.getParameters());
        }
        return new DatabaseDataType("BYTEA", super.getParameters());
    }

    @Override
    public LoadDataChange.LOAD_DATA_TYPE getLoadTypeName() {
        return LoadDataChange.LOAD_DATA_TYPE.BLOB;
    }
}
