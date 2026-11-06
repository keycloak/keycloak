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

package org.keycloak.connections.jpa.updater.liquibase;

import liquibase.database.core.PostgresDatabase;

/**
 * PostgreSQL-compatible Liquibase database for YugabyteDB.
 * <p>
 * YugabyteDB doesn't make DDL transactional by default: every DDL statement commits on its own, even inside a transaction block.
 * A statement following a DDL statement in the same transaction block can then fail with
 * {@code schema version mismatch for table ...} as it still uses the previous schema version.
 * Declaring that DDL isn't supported in transactions makes Liquibase run with auto-commit enabled,
 * so each statement is its own transaction and sees the current schema version.
 * See <a href="https://github.com/yugabyte/yugabyte-db/issues/23882">yugabyte/yugabyte-db#23882</a>.
 */
public class YugabyteDatabase extends PostgresDatabase {

    @Override
    public boolean supportsDDLInTransaction() {
        return false;
    }
}
