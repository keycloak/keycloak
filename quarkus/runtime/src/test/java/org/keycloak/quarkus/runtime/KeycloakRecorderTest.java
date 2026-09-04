package org.keycloak.quarkus.runtime;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;
import org.hibernate.tool.schema.Action;
import org.junit.Test;

import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION;
import static org.junit.Assert.assertEquals;

public class KeycloakRecorderTest {

    private static Map<String, Object> reappliedRuntimeProperties(Map<String, String> persistenceXmlProperties) {
        HibernateOrmIntegrationRuntimeInitListener listener =
                new KeycloakRecorder().createUserDefinedUnitRuntimeListener(persistenceXmlProperties);
        Map<String, Object> captured = new HashMap<>();
        listener.contributeRuntimeProperties(captured::put);
        return captured;
    }

    @Test
    public void reappliesHbm2ddlAutoUnchangedForNonCreateValues() {
        assertEquals("update",
                reappliedRuntimeProperties(Map.of("hibernate.hbm2ddl.auto", "update"))
                        .get(JAKARTA_HBM2DDL_DATABASE_ACTION));
    }

    @Test
    public void mapsLegacyHbm2ddlCreateToDropAndCreate() {
        assertEquals("drop-and-create",
                reappliedRuntimeProperties(Map.of("hibernate.hbm2ddl.auto", "create"))
                        .get(JAKARTA_HBM2DDL_DATABASE_ACTION));
    }

    @Test
    public void reappliesSchemaActionFromJpaStandardKey() {
        assertEquals("drop-and-create",
                reappliedRuntimeProperties(Map.of("jakarta.persistence.schema-generation.database.action", "drop-and-create"))
                        .get(JAKARTA_HBM2DDL_DATABASE_ACTION));
    }

    @Test
    public void reappliesSchemaActionFromLegacyJavaxKey() {
        assertEquals("drop-and-create",
                reappliedRuntimeProperties(Map.of("javax.persistence.schema-generation.database.action", "drop-and-create"))
                        .get(JAKARTA_HBM2DDL_DATABASE_ACTION));
    }

    @Test
    public void jpaStandardActionTakesPrecedenceOverHbm2ddlAuto() {
        assertEquals("none",
                reappliedRuntimeProperties(Map.of(
                        "hibernate.hbm2ddl.auto", "update",
                        "jakarta.persistence.schema-generation.database.action", "none"))
                        .get(JAKARTA_HBM2DDL_DATABASE_ACTION));
    }

    @Test
    public void reappliedSchemaActionIsAcceptedByHibernateJpaParser() {
        assertEquals(Action.UPDATE, resolvedJpaAction("hibernate.hbm2ddl.auto", "update"));
        assertEquals(Action.VALIDATE, resolvedJpaAction("hibernate.hbm2ddl.auto", "validate"));
        assertEquals(Action.CREATE_DROP, resolvedJpaAction("hibernate.hbm2ddl.auto", "create-drop"));
        assertEquals(Action.NONE, resolvedJpaAction("hibernate.hbm2ddl.auto", "none"));
        assertEquals(Action.DROP, resolvedJpaAction("hibernate.hbm2ddl.auto", "drop"));
        // Action.CREATE is Hibernate's drop-and-create; legacy hbm2ddl "create" must map to it, not to create-only.
        assertEquals(Action.CREATE, resolvedJpaAction("hibernate.hbm2ddl.auto", "create"));
        assertEquals(Action.CREATE, resolvedJpaAction("jakarta.persistence.schema-generation.database.action", "drop-and-create"));
    }

    @Test
    public void reappliesSettingsQuarkusResetsAtRuntime() {
        Map<String, Object> reapplied = reappliedRuntimeProperties(Map.of(
                "jakarta.persistence.create-database-schemas", "true",
                "jakarta.persistence.schema-generation.scripts.action", "create",
                "org.hibernate.flushMode", "COMMIT"));
        assertEquals("true", reapplied.get("jakarta.persistence.create-database-schemas"));
        assertEquals("create", reapplied.get("jakarta.persistence.schema-generation.scripts.action"));
        assertEquals("COMMIT", reapplied.get("org.hibernate.flushMode"));
    }

    @Test
    public void reappliesLegacyJavaxScriptsAction() {
        assertEquals("create",
                reappliedRuntimeProperties(Map.of("javax.persistence.schema-generation.scripts.action", "create"))
                        .get("jakarta.persistence.schema-generation.scripts.action"));
    }

    @Test
    public void reappliesLegacyJavaxCreateSchemas() {
        assertEquals("true",
                reappliedRuntimeProperties(Map.of("javax.persistence.create-database-schemas", "true"))
                        .get("jakarta.persistence.create-database-schemas"));
    }

    private static Action resolvedJpaAction(String persistenceXmlKey, String persistenceXmlValue) {
        Object action = reappliedRuntimeProperties(Map.of(persistenceXmlKey, persistenceXmlValue))
                .get(JAKARTA_HBM2DDL_DATABASE_ACTION);
        return Action.interpretJpaSetting(action);
    }
}
