/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.quarkus.runtime.configuration;

import io.agroal.api.AgroalPoolInterceptor;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Interceptor to update socket timeout on connection creation to prevent hanging connections.
 * This ensures that database connections have appropriate timeouts set.
 */
public class SocketTimeoutInterceptor implements AgroalPoolInterceptor {

    private static final Logger logger = Logger.getLogger(SocketTimeoutInterceptor.class);

    protected boolean isSupported = true;
    protected String dbUrlSocketTimeout;
    protected String dbUrlPropertiesSocketTimeout;

    @Override
    public void onConnectionCreate(Connection connection) {
        if (isSupported) {
            String effectiveTimeout = dbUrlSocketTimeout != null ? dbUrlSocketTimeout : dbUrlPropertiesSocketTimeout;
            
            if (effectiveTimeout != null) {
                try {
                    Executor executor = Executors.newSingleThreadExecutor();
                    int timeout = Integer.parseInt(effectiveTimeout);
                    connection.setNetworkTimeout(executor, timeout);
                } catch (SQLException e) {
                    logger.warn("Failed to set network timeout on connection", e);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid timeout value: " + effectiveTimeout);
                }
            }
        }
    }
}
