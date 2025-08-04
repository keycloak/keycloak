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
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * This overwrites the get connection method again to avoid the JPA style connection handling
 */
public class KEYCLOAK_JDBC_PING2_FOR_TESTING extends KEYCLOAK_JDBC_PING2 {
    @Override
    protected Connection getConnection() {
        try {
            return dataSource != null? dataSource.getConnection() :
                    DriverManager.getConnection(connection_url, connection_username, connection_password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
