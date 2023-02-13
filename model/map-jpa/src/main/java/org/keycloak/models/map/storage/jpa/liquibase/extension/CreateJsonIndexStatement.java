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

import liquibase.change.AddColumnConfig;
import liquibase.statement.core.CreateIndexStatement;

/**
 * A {@link liquibase.statement.SqlStatement} that holds the information needed to create JSON indexes. Having a specific
 * subtype allows for easier selection of the respective {@link liquibase.sqlgenerator.SqlGenerator}, since Liquibase
 * selects the generators based on the statement type they are capable of handling.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class CreateJsonIndexStatement extends CreateIndexStatement {

    public CreateJsonIndexStatement(final String tableCatalogName, final String tableSchemaName, final String tableName,
                                    final String indexName, final Boolean isUnique, final String associatedWith,
                                    final String tablespace, final Boolean clustered, final AddColumnConfig... columns) {
        super(indexName, tableCatalogName, tableSchemaName, tableName, isUnique, associatedWith, columns);
        super.setTablespace(tablespace);
        super.setClustered(clustered);
    }
}
