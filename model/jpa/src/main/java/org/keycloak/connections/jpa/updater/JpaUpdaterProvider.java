/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.jpa.updater;

import org.keycloak.provider.Provider;

import java.io.File;
import java.sql.Connection;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface JpaUpdaterProvider extends Provider {

    /**
     * Status of database up-to-dateness
     */
    enum Status {
        /**
         * Database is valid and up to date
         */
        VALID,
        /**
         * No database exists.
         */
        EMPTY,
        /**
         * Database needs to be updated
         */
        OUTDATED
    }

    /**
     * Updates the Keycloak database
     * @param connection DB connection
     * @param defaultSchema DB connection
     */
    void update(Connection connection, String defaultSchema);

    /**
     * Checks whether Keycloak database is up to date with the most recent changesets
     * @param connection DB connection
     * @param defaultSchema DB schema to use
     * @return
     */
    Status validate(Connection connection, String defaultSchema);

    /**
     * Exports the SQL update script into the given File.
     * @param connection DB connection
     * @param defaultSchema DB schema to use
     * @param file File to write to
     */
    void export(Connection connection, String defaultSchema, File file);

}
