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

package org.keycloak.connections.jpa;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

public class AsyncCommitIntegratorTest {

    @Test
    public void auroraProbeUsesCatalogLookupInsteadOfCallingAuroraVersion() throws SQLException {
        Probe probe = new Probe(false);

        boolean exists = AsyncCommitIntegrator.auroraVersionFunctionExists(probe.connection);

        Assert.assertFalse(exists);
        Assert.assertFalse("standard PostgreSQL must not execute SELECT aurora_version()",
                probe.calledAuroraVersionFunction());
        Assert.assertTrue("probe should resolve aurora_version via to_regproc",
                probe.lastSql().toLowerCase(Locale.ROOT).contains("to_regproc"));
    }

    @Test
    public void auroraProbeReportsPresentFunction() throws SQLException {
        Probe probe = new Probe(true);

        Assert.assertTrue(AsyncCommitIntegrator.auroraVersionFunctionExists(probe.connection));
        Assert.assertFalse(probe.calledAuroraVersionFunction());
    }

    private static final class Probe {
        private final List<String> sql = new ArrayList<>();
        private final boolean functionExists;
        final Connection connection;

        Probe(boolean functionExists) {
            this.functionExists = functionExists;
            this.connection = proxy(Connection.class, (proxy, method, args) -> {
                if ("createStatement".equals(method.getName())) {
                    return statement();
                }
                if ("close".equals(method.getName())) {
                    return null;
                }
                throw new UnsupportedOperationException(method.getName());
            });
        }

        String lastSql() {
            return sql.isEmpty() ? "" : sql.get(sql.size() - 1);
        }

        boolean calledAuroraVersionFunction() {
            for (String statement : sql) {
                String normalized = statement.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
                if (normalized.contains("aurora_version()") && !normalized.contains("to_regproc")) {
                    return true;
                }
            }
            return false;
        }

        private Statement statement() {
            return proxy(Statement.class, (proxy, method, args) -> {
                if ("executeQuery".equals(method.getName())) {
                    String query = (String) args[0];
                    sql.add(query);
                    return resultSet(query);
                }
                if ("close".equals(method.getName())) {
                    return null;
                }
                throw new UnsupportedOperationException(method.getName());
            });
        }

        private ResultSet resultSet(String query) {
            String normalized = query.toLowerCase(Locale.ROOT);
            boolean catalogLookup = normalized.contains("to_regproc");
            return proxy(ResultSet.class, (proxy, method, args) -> {
                if ("next".equals(method.getName())) {
                    return true;
                }
                if ("getBoolean".equals(method.getName())) {
                    return catalogLookup && functionExists;
                }
                if ("close".equals(method.getName())) {
                    return null;
                }
                throw new UnsupportedOperationException(method.getName());
            });
        }

        @SuppressWarnings("unchecked")
        private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
            return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
        }
    }
}
