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

package org.keycloak.authorization.fgap.evaluation.partial;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.GROUPS;

import static org.junit.Assert.assertEquals;

public class PartialEvaluatorTest {

    private static final int POSTGRESQL_PARAMETER_LIMIT = 65_535;
    private static StandardServiceRegistry registry;
    private static SessionFactory sessionFactory;
    private static SqlRecorder sqlRecorder;

    @BeforeClass
    public static void createSessionFactory() {
        sqlRecorder = new SqlRecorder();
        registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.driver_class", "org.h2.Driver")
                .applySetting("hibernate.connection.url", "jdbc:h2:mem:partial-evaluator;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.hbm2ddl.auto", "create-drop")
                .applySetting("hibernate.session_factory.statement_inspector", sqlRecorder)
                .build();
        sessionFactory = new MetadataSources(registry)
                .addAnnotatedClass(TestEntity.class)
                .buildMetadata()
                .buildSessionFactory();
    }

    @AfterClass
    public static void closeSessionFactory() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @Test
    public void deniedResourcesUseBindParametersForOrdinaryCollections() {
        assertEquals(2, countParameters(executeQuery(Set.of("denied"))));
    }

    @Test
    public void deniedResourcesDoNotUseBindParametersForOversizedCollections() {
        assertEquals(1, countParameters(executeQuery(createIds(POSTGRESQL_PARAMETER_LIMIT + 1))));
    }

    private static String executeQuery(Set<String> deniedIds) {
        sqlRecorder.clear();

        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<TestEntity> query = builder.createQuery(TestEntity.class);
            Root<TestEntity> root = query.from(TestEntity.class);
            PartialEvaluationContext context = new PartialEvaluationContext(
                    GROUPS,
                    Set.of("allowed"),
                    deniedIds,
                    null,
                    builder,
                    query,
                    root);
            List<Predicate> predicates = new PartialEvaluator().buildPredicates(context);
            query.select(root).where(predicates.toArray(Predicate[]::new));

            session.createQuery(query).getResultList();

            return sqlRecorder.lastSelect();
        }
    }

    private static Set<String> createIds(int size) {
        Set<String> ids = new LinkedHashSet<>(size);
        ids.add("group-'quoted");

        for (int i = 1; i < size; i++) {
            ids.add("group-" + i);
        }

        return ids;
    }

    private static int countParameters(String sql) {
        return (int) sql.chars().filter(character -> character == '?').count();
    }

    @Entity(name = "PartialEvaluatorTestEntity")
    @Table(name = "partial_evaluator_test")
    public static class TestEntity {
        @Id
        private String id;
    }

    private static final class SqlRecorder implements StatementInspector {
        private String lastSelect;

        @Override
        public String inspect(String sql) {
            if (sql.startsWith("select ")) {
                lastSelect = sql;
            }
            return sql;
        }

        void clear() {
            lastSelect = null;
        }

        String lastSelect() {
            if (lastSelect == null) {
                throw new AssertionError("No select statement was captured");
            }
            return lastSelect;
        }
    }
}
