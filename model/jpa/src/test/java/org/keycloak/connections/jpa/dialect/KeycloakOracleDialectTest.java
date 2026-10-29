package org.keycloak.connections.jpa.dialect;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.QueryLiteral;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.insert.ConflictClause;
import org.hibernate.sql.ast.spi.query.insert.InsertSelectStatement;
import org.hibernate.sql.ast.spi.query.insert.Values;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcOperationQueryInsert;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcOperations;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.type.BasicType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class KeycloakOracleDialectTest {

    private static final String INSERT = "insert into OracleConflictTestEntity (id, payload) values (:id, :payload)";
    private StandardServiceRegistry registry;
    private SessionFactory sessionFactory;
    private KeycloakOracleDialect dialect;

    @Before
    public void createSessionFactory() {
        registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.driver_class", "org.h2.Driver")
                .applySetting("hibernate.connection.url", "jdbc:h2:mem:" + UUID.randomUUID())
                .applySetting("hibernate.dialect", KeycloakOracleDialect.class.getName())
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting("hibernate.hbm2ddl.auto", "none")
                .applySetting("hibernate.session_factory.statement_inspector", (StatementInspector) sql -> {
                    throw new CapturedSql(sql);
                })
                .build();
        sessionFactory = new MetadataSources(registry).addAnnotatedClass(ConflictEntity.class)
                .buildMetadata().buildSessionFactory();
        dialect = (KeycloakOracleDialect) factory().getJdbcServices().getDialect();
    }

    @After
    public void closeSessionFactory() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @Test
    public void doNothingUsesDuplicateIndexHint() {
        String sql = sql(INSERT + " on conflict (id) do nothing");
        assertTrue(sql, sql.startsWith("insert /*+ ignore_row_on_dupkey_index("));
        assertTrue(sql, sql.contains("oracleconflicttestentity(id)) */ into "));
        assertFalse(sql.contains("merge into"));
    }

    @Test
    public void doUpdateRemainsMerge() {
        String sql = sql(INSERT + " on conflict (id) do update set payload = excluded.payload");
        assertTrue(sql, sql.startsWith("merge into "));
        assertFalse(sql.contains("ignore_row_on_dupkey_index"));
    }

    @Test
    public void plainInsertHasNoHint() {
        String sql = sql(INSERT);
        assertTrue(sql, sql.startsWith("insert into "));
        assertFalse(sql.contains("ignore_row_on_dupkey_index"));
    }

    @Test
    public void translationPreservesOriginalAstAndBindings() {
        InsertSelectStatement insert = insert("app.target_table");
        ConflictClause conflict = insert.getConflictClause();
        SqlAstTranslationRequest.QueryMutation request = new SqlAstTranslationRequest.QueryMutation(factory(), insert);
        SqlAstTranslator<JdbcOperationQueryMutation> translator = dialect.getSqlAstTranslatorFactory().buildTranslator(request);
        assertSame(insert, translator.getSqlAst());
        JdbcOperationQueryMutation operation = translator.translate(JdbcParameterBindings.NO_BINDINGS, QueryOptions.NONE);
        assertSame(conflict, insert.getConflictClause());
        assertEquals(1, insert.getValuesList().size());
        assertTrue(operation.getSqlString(), operation.getSqlString().contains("IGNORE_ROW_ON_DUPKEY_INDEX(target_table(id))"));
        assertTrue(operation.getAffectedTableNames().contains("app.target_table"));
        assertTrue(operation instanceof JdbcOperationQueryInsert);
        assertNull(((JdbcOperationQueryInsert) operation).getUniqueConstraintNameThatMayFail());
    }

    @Test
    public void hintPreservesOperationCacheMetadata() {
        InsertSelectStatement insert = insert("target_table");
        SqlAstTranslationRequest.QueryMutation request = new SqlAstTranslationRequest.QueryMutation(factory(), insert);
        BasicType<Integer> type = factory().getTypeConfiguration().getBasicTypeForJavaType(Integer.class);
        JdbcParameterImpl parameter = new JdbcParameterImpl(type);
        JdbcParameterBindingImpl binding = new JdbcParameterBindingImpl(type, 7);
        List<JdbcParameterBinder> binders = List.of(parameter);
        JdbcOperationQueryMutation original = JdbcOperations.queryMutation(request).command("insert into target_table(id) values (?)")
                .parameterBinders(binders).affectedQuerySpaces(Set.of("target_table"))
                .appliedParameterBindings(Map.of(parameter, binding)).build();
        JdbcOperationQueryMutation hinted = dialect.applyHint(request, original, "IGNORE_ROW_ON_DUPKEY_INDEX(target_table(id))");
        assertEquals(original.getAffectedTableNames(), hinted.getAffectedTableNames());
        assertEquals(original.getParameterBinders(), hinted.getParameterBinders());
        assertTrue(hinted.dependsOnParameterBindings());
        JdbcParameterBindingsImpl bindings = new JdbcParameterBindingsImpl(1);
        bindings.addBinding(parameter, binding);
        assertTrue(hinted.isCompatibleWith(bindings, QueryOptions.NONE));
        bindings.addBinding(parameter, new JdbcParameterBindingImpl(type, 8));
        assertFalse(hinted.isCompatibleWith(bindings, QueryOptions.NONE));
        assertNull(((JdbcOperationQueryInsert) hinted).getUniqueConstraintNameThatMayFail());
    }

    @Test
    public void hintPreservesQuotedTableNamesAndDottedSchemas() {
        Map<String, String> tables = Map.of(
                "\"tenant.eu.prod\".target_table", "target_table",
                "\"tenant.eu.prod\".\"target.table\"", "\"target.table\"",
                "\"tenant\"\".eu.prod\".\"target\"\".table\"", "\"target\"\".table\"");
        tables.forEach((table, expected) -> {
            SqlAstTranslationRequest.QueryMutation request = new SqlAstTranslationRequest.QueryMutation(factory(), insert(table));
            JdbcOperationQueryMutation operation = dialect.getSqlAstTranslatorFactory().buildTranslator(request)
                    .translate(JdbcParameterBindings.NO_BINDINGS, QueryOptions.NONE);
            assertTrue(operation.getSqlString(), operation.getSqlString().contains("IGNORE_ROW_ON_DUPKEY_INDEX(" + expected + "(id))"));
        });
    }

    @Test
    public void unsupportedHintPlacementFailsExplicitly() {
        SqlAstTranslationRequest.QueryMutation request = new SqlAstTranslationRequest.QueryMutation(factory(), insert("target_table"));
        JdbcOperationQueryMutation original = JdbcOperations.queryMutation(request)
                .command("with source as (select 1 from dual) insert into target_table values (1)").build();
        assertThrows(IllegalQueryOperationException.class,
                () -> dialect.applyHint(request, original, "IGNORE_ROW_ON_DUPKEY_INDEX(target_table(id))"));
    }

    private InsertSelectStatement insert(String table) {
        BasicType<Integer> type = factory().getTypeConfiguration().getBasicTypeForJavaType(Integer.class);
        NamedTableReference target = new NamedTableReference(table, "t");
        InsertSelectStatement insert = new InsertSelectStatement(target, List.of());
        insert.addTargetColumnReference(new ColumnReference(target, "id", type));
        insert.setValuesList(List.of(new Values(List.of(new QueryLiteral<>(7, type)))));
        insert.setConflictClause(new ConflictClause(null, List.of("id"), List.of(), null));
        return insert;
    }

    private SessionFactoryImplementor factory() {
        return sessionFactory.unwrap(SessionFactoryImplementor.class);
    }

    private String sql(String hql) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            try {
                CapturedSql captured = assertThrows(CapturedSql.class, () -> session.createMutationQuery(hql)
                        .setParameter("id", 1).setParameter("payload", "first").executeUpdate());
                assertNotNull(captured.sql);
                return captured.sql.toLowerCase(Locale.ROOT);
            } finally {
                session.getTransaction().rollback();
            }
        }
    }

    private static class CapturedSql extends RuntimeException {
        private final String sql;

        private CapturedSql(String sql) {
            this.sql = sql;
        }
    }

    @Entity(name = "OracleConflictTestEntity")
    public static class ConflictEntity {
        @Id
        private int id;
        private String payload;
    }
}
