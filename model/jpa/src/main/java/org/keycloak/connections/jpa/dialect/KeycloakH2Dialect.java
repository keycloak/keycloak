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

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.sql.ast.H2SqlAstTranslator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * Fixes Hibernate's H2 dialect to correctly handle HQL {@code insert ... on conflict ... do nothing}
 * by emitting a MERGE statement instead of a plain INSERT that silently drops the conflict clause.
 */
public class KeycloakH2Dialect extends H2Dialect {

    @Override
    public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
        return new StandardSqlAstTranslatorFactory() {
            @Override
            protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
                    SessionFactoryImplementor sessionFactory, Statement statement) {
                return new H2SqlAstTranslator<>(sessionFactory, statement) {
                    @Override
                    protected void visitInsertStatementOnly(InsertSelectStatement statement) {
                        if (statement.getConflictClause() == null) {
                            super.visitInsertStatementOnly(statement);
                        } else {
                            visitInsertStatementEmulateMerge(statement);
                        }
                    }
                };
            }
        };
    }
}
