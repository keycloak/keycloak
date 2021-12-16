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

package org.keycloak.models.map.storage.jpa.updater;

import org.keycloak.provider.Provider;

import java.io.File;
import java.sql.Connection;

public interface MapJpaUpdaterProvider extends Provider {

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
     * Updates the Keycloak database for the given model type
     * @param modelType Model type
     * @param connection DB connection
     * @param defaultSchema DB connection
     */
    void update(Class modelType, Connection connection, String defaultSchema);

    /**
     * Checks whether Keycloak database for the given model type is up to date with the most recent changesets
     * @param modelType Model type
     * @param connection DB connection
     * @param defaultSchema DB schema to use
     * @return
     */
    Status validate(Class modelType, Connection connection, String defaultSchema);

    /**
     * Exports the SQL update script for the given model type into the given File.
     * @param modelType Model type
     * @param connection DB connection
     * @param defaultSchema DB schema to use
     * @param file File to write to
     */
    void export(Class modelType, Connection connection, String defaultSchema, File file);

}
