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

package org.keycloak.connections.jpa.dialect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.query.insert.ConflictClause;
import org.hibernate.sql.ast.spi.query.insert.InsertSelectStatement;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcOperations;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * Preserves concurrent-insert safety for HQL {@code insert ... on conflict (...) do nothing} on Oracle.
 * <p>
 * It renders an INSERT with the {@code IGNORE_ROW_ON_DUPKEY_INDEX} hint, so that a duplicate key is skipped and
 * counted as 0 rows. Unlike a MERGE, this also holds when a concurrent transaction inserts the same key. The conflict
 * columns must match a unique index. {@code do update} clauses are still rendered as MERGE, which fails with a unique
 * constraint violation in that case.
 */
public class KeycloakOracleDialect extends OracleDialect {

    public KeycloakOracleDialect() {
        super();
    }

    public KeycloakOracleDialect(DialectResolutionInfo info) {
        super(info);
    }

    // Remove this override once Keycloak upgrades to a Hibernate version that fixes HHH-21039.
    // https://hibernate.atlassian.net/browse/HHH-21039
    @Override
    public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
        SqlAstTranslatorFactory delegateFactory = super.getSqlAstTranslatorFactory();
        return new SqlAstTranslatorFactory() {
            @Override
            @SuppressWarnings("unchecked")
            public <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
                    SqlAstTranslationRequest<S, T> request) {
                if (request instanceof SqlAstTranslationRequest.QueryMutation mutation
                        && mutation.statement() instanceof InsertSelectStatement insert) {
                    ConflictClause conflict = insert.getConflictClause();
                    if (conflict != null && conflict.isDoNothing() && !conflict.getConstraintColumnNames().isEmpty()) {
                        if (conflict.getConstraintName() != null || conflict.getPredicate() != null) {
                            throw new IllegalQueryOperationException("Oracle duplicate-index hints require a column-only conflict target");
                        }

                        // Translate a copy as an ordinary insert; the database hint implements do-nothing.
                        // Never mutate the original AST, which may be retained by the query plan.
                        InsertSelectStatement copy = new InsertSelectStatement(insert, insert.getTargetTable(),
                                insert.getMutationTarget(), insert.getReturningColumns());
                        copy.addTargetColumnReferences(insert.getTargetColumns());
                        copy.setValuesList(List.copyOf(insert.getValuesList()));
                        copy.setSourceSelectStatement(insert.getSourceSelectStatement());
                        SqlAstTranslationRequest.QueryMutation translatedRequest =
                                new SqlAstTranslationRequest.QueryMutation(request.sessionFactory(), copy);
                        String table = unqualifiedTableName(insert.getTargetTable().getTableExpression());
                        String hint = "IGNORE_ROW_ON_DUPKEY_INDEX(" + table + "("
                                + String.join(",", conflict.getConstraintColumnNames()) + "))";
                        return (SqlAstTranslator<T>) hintedTranslator(mutation,
                                delegateFactory.buildTranslator(translatedRequest), hint);
                    }
                }
                return delegateFactory.buildTranslator(request);
            }
        };
    }

    private static String unqualifiedTableName(String table) {
        // The expression is already SQL-rendered; dots inside quoted identifiers are not qualifiers.
        int start = 0;
        boolean quoted = false;
        for (int i = 0; i < table.length(); i++) {
            char character = table.charAt(i);
            if (character == '"') {
                if (quoted && i + 1 < table.length() && table.charAt(i + 1) == '"') {
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == '.' && !quoted) {
                start = i + 1;
            }
        }
        return table.substring(start);
    }

    @SuppressWarnings("unchecked")
    private SqlAstTranslator<JdbcOperationQueryMutation> hintedTranslator(SqlAstTranslationRequest.QueryMutation request,
            SqlAstTranslator<JdbcOperationQueryMutation> delegate, String hint) {
        // Delegate the complete public translator/walker contract, including future visitor methods.
        return (SqlAstTranslator<JdbcOperationQueryMutation>) Proxy.newProxyInstance(
                SqlAstTranslator.class.getClassLoader(), new Class<?>[] { SqlAstTranslator.class }, (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "equals" -> proxy == args[0];
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "toString" -> "OracleConflictTranslator[" + delegate + "]";
                            default -> throw new IllegalStateException("Unexpected Object method: " + method);
                        };
                    }
                    if (method.getName().equals("getSqlAst")) {
                        return request.statement();
                    }
                    if (method.getName().equals("translate")) {
                        JdbcOperationQueryMutation operation = delegate.translate((JdbcParameterBindings) args[0], (QueryOptions) args[1]);
                        return applyHint(request, operation, hint);
                    }
                    try {
                        return method.invoke(delegate, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    JdbcOperationQueryMutation applyHint(SqlAstTranslationRequest.QueryMutation request,
            JdbcOperationQueryMutation operation, String hint) {
        String sql;
        try {
            sql = getQueryHintString(operation.getSqlString(), hint);
        } catch (IllegalArgumentException e) {
            throw new IllegalQueryOperationException("Cannot apply Oracle duplicate-index hint to this insert shape",
                    operation.getSqlString(), e);
        }
        return JdbcOperations.queryMutation(request)
                .command(sql)
                .parameterBinders(operation.getParameterBinders())
                .affectedQuerySpaces(operation.getAffectedTableNames())
                .appliedParameterBindings(operation.getAppliedParameters())
                .build();
    }
}
