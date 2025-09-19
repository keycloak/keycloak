/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jgroups.protocol;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import org.jgroups.protocols.JDBC_PING2;
import org.jgroups.protocols.PingData;
import org.jgroups.util.UUID;
import org.keycloak.connections.jpa.JpaConnectionProviderFactory;

public class KEYCLOAK_JDBC_PING2 extends JDBC_PING2 {

    private JpaConnectionProviderFactory factory;

    @Override
    protected void loadDriver() {
        //no-op, using JpaConnectionProviderFactory
    }

    @Override
    protected Connection getConnection() {
        return factory.getConnection();
    }

    public void setJpaConnectionProviderFactory(JpaConnectionProviderFactory factory) {
        this.factory = Objects.requireNonNull(factory);
    }

    /**
     * Detects a network partition and decides if the node belongs to the winning partition.
     * <p>
     * The algorithm performs the following steps
     *
     * <ul>
     *     <li>Reads the data from the database</li>
     *     <li>If an error occurs fetching the data, it returns {@link HealthStatus#ERROR}</li>
     *     <li>Filters out non coordinator members</li>
     *     <li>If no coordinator is found, it return {@link HealthStatus#NO_COORDINATOR}</li>
     *     <li>If multiple coordinators are found, it compares them and uses the coordinator with the lowest {@link UUID}</li>
     *     <li>Finally, it compares if the coordinator is the same as the current view coordinator. If so, it returns {@link HealthStatus#HEALTHY}, otherwise {@link HealthStatus#UNHEALTHY}</li>
     * </ul>
     *
     * @return The {@link HealthStatus}.
     * @see HealthStatus
     */
    public HealthStatus healthStatus() {
        try {
            // maybe create an index, and a query to return coordinators only?
            return readFromDB(cluster_name)
                    .stream()
                    .filter(PingData::isCoord)
                    .map(PingData::getAddress)
                    .sorted()
                    .findFirst()
                    .map(view.getCoord()::equals)
                    .map(isCoordinatorInView -> isCoordinatorInView ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY)
                    .orElse(HealthStatus.NO_COORDINATOR);
        } catch (Exception e) {
            // database failed?
            log.warn("Failed to fetch the cluster members from the database.", e);
            return HealthStatus.ERROR;
        }
    }

    public enum HealthStatus {
        /**
         * No partition detected or this instance is in the right partition.
         */
        HEALTHY,
        /**
         * Partition detected and this instance is not in the right partition. It should stop handling requests.
         */
        UNHEALTHY,
        /**
         * No coordinator present in the database table.
         */
        NO_COORDINATOR,
        /**
         * If an error occurs when reading from the database.
         */
        ERROR
    }
}
