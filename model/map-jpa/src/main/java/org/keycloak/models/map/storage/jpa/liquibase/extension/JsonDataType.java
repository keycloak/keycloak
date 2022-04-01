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

package org.keycloak.models.map.storage.jpa.liquibase.extension;

import liquibase.change.core.LoadDataChange;
import liquibase.database.Database;
import liquibase.database.core.PostgresDatabase;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.DatabaseDataType;
import liquibase.datatype.LiquibaseDataType;

/**
 * A {@link LiquibaseDataType} to handle the JSON column type.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@DataTypeInfo(name="json", minParameters = 0, maxParameters = 1, priority = LiquibaseDataType.PRIORITY_DEFAULT + 1)
public class JsonDataType extends LiquibaseDataType {

    @Override
    public DatabaseDataType toDatabaseDataType(Database database) {
        if (database instanceof PostgresDatabase) {
            // on Postgres switch the columns of type JSON to JSONB as JSONB is a more efficient type to handle JSON contents.
            return new DatabaseDataType("JSONB", super.getParameters());
        }
        return super.toDatabaseDataType(database);
    }

    @Override
    public LoadDataChange.LOAD_DATA_TYPE getLoadTypeName() {
        return LoadDataChange.LOAD_DATA_TYPE.UNKNOWN;
    }
}
