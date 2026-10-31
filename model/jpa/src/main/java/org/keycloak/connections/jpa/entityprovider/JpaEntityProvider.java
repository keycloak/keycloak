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

package org.keycloak.connections.jpa.entityprovider;

import java.util.List;

import org.keycloak.provider.Provider;
import org.keycloak.representations.migration.MigrationManifest;

/**
 * @author <a href="mailto:erik.mulder@docdatapayments.com">Erik Mulder</a>
 *
 * A JPA Entity Provider can supply extra JPA entities that the Keycloak system should include in its entity manager.
 */
public interface JpaEntityProvider extends Provider {

    /**
     * Return the entities that should be added to the entity manager.
     *
     * @return list of class objects
     * @deprecated this method is ignored in production and will be removed together with legacy testsuite
     */
    @Deprecated(forRemoval = true)
    List<Class<?>> getEntities();

    /**
     * Return the location of the Liquibase changelog that facilitates the extra JPA entities.
     *
     * @return a changelog location or null if not needed
     * @deprecated use {@link #getMigrationManifest()} instead; this method is no longer consulted by the
     *             JDBC updater and will be removed in a future release.
     */
    @Deprecated(forRemoval = true)
    default String getChangelogLocation() {
        return null;
    }

    /**
     * Return a {@link MigrationManifest} describing the database schema changes needed by this provider,
     * or {@code null} if no schema migration is required.
     *
     * <p>The manifest is executed by the native JDBC updater against a dedicated history table
     * ({@code DATABASECHANGELOG_<factoryId>}) so that provider change-sets are tracked independently
     * from the master Keycloak changelog.
     *
     * <p>Implementations must supply dialect-specific SQL for the dialects they support.  The manifest
     * is evaluated with the same precondition and history-tracking semantics as the master changelog.
     *
     * @return migration manifest, or {@code null} if no migration is needed
     */
    default MigrationManifest getMigrationManifest() {
        return null;
    }

    /**
     * Return the ID of provider factory, which created this provider.  Used to compute the name of the
     * dedicated history table ({@code DATABASECHANGELOG_<factoryId>}).
     *
     * @return ID of provider factory
     */
    String getFactoryId();

}
