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

package org.keycloak.quarkus.runtime.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;

import org.keycloak.common.Profile;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.quarkus.runtime.integration.QuarkusKeycloakSessionFactory;

import io.agroal.api.AgroalPoolInterceptor;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.jboss.logging.Logger;

/**
 * When a switchover between replicas happens, this bean can detected by watching the timeline ID.
 * If in a setup asynchronously replicated database are used, this can reset the caches automatically.
 *
 * See <a href="https://www.postgresql.org/docs/current/continuous-archiving.html#BACKUP-TIMELINES">PostgreSQL Timelines</a> for more information.
 */
@ApplicationScoped
public class PostgresqlFailoverDetection implements AgroalPoolInterceptor {
    protected static final Logger logger = Logger.getLogger(PostgresqlFailoverDetection.class);

    volatile Long timelineId;
    volatile boolean missingPermissions;

    @Override
    public void onConnectionCreate(Connection connection) {
        try {
            try (Statement statement = connection.createStatement();
                 // https://www.postgresql.org/docs/current/functions-info.html#FUNCTIONS-INFO-CONTROLDATA
                 ResultSet resultSet = statement.executeQuery("select timeline_id FROM pg_control_checkpoint()")) {
                resultSet.next();
                long timelineId = resultSet.getLong(1);
                verifyTimelineChange(timelineId);
            }
        } catch (SQLException e) {
            if (timelineId == null) {
                logger.warn("No permissions to select timeline information from FROM pg_control_checkpoint(), ignoring check until next server restart.", e);
                missingPermissions = true;
            } else {
                throw new RuntimeException("Unable to retrieve timeline information", e);
            }
        }
    }

    private void verifyTimelineChange(long timelineId) {
        if (!Objects.equals(this.timelineId, timelineId)) {
            synchronized (this) {
                if (!Objects.equals(this.timelineId, timelineId)) {
                    if (this.timelineId != null) {
                        QuarkusKeycloakSessionFactory sf = QuarkusKeycloakSessionFactory.getInstance();
                        KeycloakModelUtils.runJobInTransaction(sf, session -> {
                            logger.info("Database failover detected, clearing caches that might be out-of-sync with the database");

                            InfinispanConnectionProvider p = session.getProvider(InfinispanConnectionProvider.class);
                            Arrays.stream(InfinispanConnectionProvider.LOCAL_CACHE_NAMES)
                                    .map(p::getCache)
                                    .filter(cache -> cache.getCacheConfiguration().clustering().cacheMode() == CacheMode.LOCAL)
                                    .forEach(Cache::clear);

                            if (Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS)) {
                                // If persistent user sessions are enabled, the reasoning from above is true for the user and client sessions as well.
                                // As this is executed at different times on different nodes, each one would clear all nodes to avoid having some left-over
                                // data present in the caches in the end.
                                Arrays.stream(InfinispanConnectionProvider.USER_AND_CLIENT_SESSION_CACHES)
                                        .map(p::getCache)
                                        .forEach(Cache::clear);
                            }

                            logger.info("Clearing of caches complete");
                        });
                    }
                    this.timelineId = timelineId;
                }
            }
        }
    }
}
