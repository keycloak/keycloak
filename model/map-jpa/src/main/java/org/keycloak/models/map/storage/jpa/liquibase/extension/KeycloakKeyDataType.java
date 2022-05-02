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
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.DatabaseDataType;
import liquibase.datatype.LiquibaseDataType;
import liquibase.datatype.core.VarcharType;

/**
 * A {@link LiquibaseDataType} used in columns that reference an entity that can be external to the JPA storage. In other
 * words, they reference objects that might be stored in different storages, with different id formats than those used
 * by the JPA storage.
 * <p/>
 * Its usage is encouraged to bring consistency to all columns that might reference external entities.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@DataTypeInfo(name="kc_key", minParameters = 0, maxParameters = 0, priority = LiquibaseDataType.PRIORITY_DEFAULT)
public class KeycloakKeyDataType extends LiquibaseDataType {

    @Override
    public DatabaseDataType toDatabaseDataType(Database database) {
        // convert the type into a varchar of size 4000.
        VarcharType varcharType = new VarcharType();
        varcharType.addParameter("4000");
        return varcharType.toDatabaseDataType(database);
    }

    @Override
    public LoadDataChange.LOAD_DATA_TYPE getLoadTypeName() {
        return LoadDataChange.LOAD_DATA_TYPE.STRING;
    }
}
