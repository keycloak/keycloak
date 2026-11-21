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

import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.sql.ast.internal.SQLServerSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.TableReferenceAliasContext;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * Preserves concurrent-insert safety for Hibernate's MERGE-based conflict handling on SQL Server.
 * <p>
 * The MERGE target gets a {@code HOLDLOCK} hint for all conflict clauses. Without it, two concurrent upserts of the
 * same new key can both take the "not matched" branch, and one fails with a unique constraint violation.
 */
public class KeycloakSQLServerDialect extends SQLServerDialect {

    public KeycloakSQLServerDialect() {
        super();
    }

    public KeycloakSQLServerDialect(DialectResolutionInfo info) {
        super(info);
    }

    // Remove this override once Keycloak upgrades to a Hibernate version that fixes HHH-21039.
    // https://hibernate.atlassian.net/browse/HHH-21039
    @Override
    public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
        return new StandardSqlAstTranslatorFactory() {
            @Override
            protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
                    SqlAstTranslationRequest<S, T> request) {
                return new SQLServerSqlAstTranslator<>(request) {
                    @Override
                    protected void renderTableReferenceAlias(String alias, TableReferenceAliasContext context) {
                        if (context == TableReferenceAliasContext.INSERT_TARGET
                                && getCurrentClauseStack().getCurrent() == Clause.MERGE) {
                            // In a MERGE the table hint goes between the table name and the alias
                            appendSql(" with (holdlock)");
                        }
                        super.renderTableReferenceAlias(alias, context);
                    }
                };
            }
        };
    }
}
