package org.keycloak.quarkus.runtime.logging;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KeycloakLogFilterTest {

    private static final String FASTBOOT = "io.quarkus.hibernate.orm.runtime.FastBootHibernatePersistenceProvider";

    private static final String GENERIC_WARN =
            "Persistence-unit [%s] sets unsupported properties. These properties may not work correctly, and even if"
                    + " they do, that may change when upgrading to a newer version of Quarkus (even just a micro/patch"
                    + " version). Consider using a supported configuration property before falling back to unsupported"
                    + " ones. If there is no supported equivalent, make sure to file a feature request so that a"
                    + " supported configuration property can be added to Quarkus, and more importantly so that the"
                    + " configuration property is tested regularly. Unsupported properties being set: %s";
    private static final String OVERRIDE_WARN =
            "Persistence-unit [%s] sets unsupported properties that override Quarkus' own settings. These properties may"
                    + " break assumptions in Quarkus code and cause malfunctions. If this override is absolutely"
                    + " necessary, make sure to file a feature request or bug report so that a solution can be"
                    + " implemented in Quarkus. Unsupported properties that override Quarkus' own settings: %s";

    private static LogRecord record(Level level, String loggerName, String message, Object... parameters) {
        LogRecord record = new LogRecord(level, message);
        record.setLoggerName(loggerName);
        record.setParameters(parameters);
        return record;
    }

    @Test
    public void suppressesGenericWarningForDefaultUnit() {
        assertTrue(KeycloakLogFilter.isDefaultPersistenceUnitUnsupportedPropertiesWarning(
                record(Level.WARNING, FASTBOOT, GENERIC_WARN, "<default>", Set.of("hibernate.order_inserts"))));
    }

    @Test
    public void suppressesOverrideWarningForDefaultUnit() {
        assertTrue(KeycloakLogFilter.isDefaultPersistenceUnitUnsupportedPropertiesWarning(
                record(Level.WARNING, FASTBOOT, OVERRIDE_WARN, "<default>", Set.of("hibernate.order_inserts"))));
    }

    @Test
    public void keepsWarningForUserDefinedUnit() {
        assertFalse(KeycloakLogFilter.isDefaultPersistenceUnitUnsupportedPropertiesWarning(
                record(Level.WARNING, FASTBOOT, GENERIC_WARN, "user-store", Set.of("hibernate.order_inserts"))));
    }

    @Test
    public void keepsWarningWhenUserAddsOwnUnsupportedProperty() {
        assertFalse(KeycloakLogFilter.isDefaultPersistenceUnitUnsupportedPropertiesWarning(
                record(Level.WARNING, FASTBOOT, GENERIC_WARN, "<default>",
                        Set.of("hibernate.order_inserts", "hibernate.jdbc.fetch_size"))));
    }

    @Test
    public void suppressesWarningForKeycloakNamedQueryProperty() {
        assertTrue(KeycloakLogFilter.isDefaultPersistenceUnitUnsupportedPropertiesWarning(
                record(Level.WARNING, FASTBOOT, GENERIC_WARN, "<default>",
                        Set.of("hibernate.use_sql_comments", "kc.query.deleteExpiredClientSessions[native]"))));
    }

    @Test
    public void keepsUnrelatedWarningFromSameLogger() {
        assertFalse(KeycloakLogFilter.isDefaultPersistenceUnitUnsupportedPropertiesWarning(
                record(Level.WARNING, FASTBOOT,
                        "Persistence-unit [%s]: enabling best-effort backwards compatibility with '%2$s=%3$s'.",
                        "<default>")));
    }

    @Test
    public void keepsNonWarningLevel() {
        assertFalse(KeycloakLogFilter.isDefaultPersistenceUnitUnsupportedPropertiesWarning(
                record(Level.INFO, FASTBOOT, GENERIC_WARN, "<default>", Set.of("hibernate.order_inserts"))));
    }

    @Test
    public void keepsWarningFromDifferentLogger() {
        assertFalse(KeycloakLogFilter.isDefaultPersistenceUnitUnsupportedPropertiesWarning(
                record(Level.WARNING, "org.hibernate.orm.deprecation", GENERIC_WARN, "<default>", Set.of("x"))));
    }

    @Test
    public void keepsWarningWithNoParameters() {
        assertFalse(KeycloakLogFilter.isDefaultPersistenceUnitUnsupportedPropertiesWarning(
                record(Level.WARNING, FASTBOOT, GENERIC_WARN)));
    }
}
