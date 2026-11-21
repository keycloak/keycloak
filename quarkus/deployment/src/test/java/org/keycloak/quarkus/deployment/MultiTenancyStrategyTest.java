package org.keycloak.quarkus.deployment;

import java.util.Properties;

import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;
import io.quarkus.runtime.configuration.ConfigurationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultiTenancyStrategyTest {

    @Test
    public void defaultsToNoneWhenAbsentOrBlank() {
        assertEquals(MultiTenancyStrategy.NONE, KeycloakProcessor.getMultiTenancyStrategy("pu", null));
        assertEquals(MultiTenancyStrategy.NONE, KeycloakProcessor.getMultiTenancyStrategy("pu", new Properties()));
        assertEquals(MultiTenancyStrategy.NONE, KeycloakProcessor.getMultiTenancyStrategy("pu", withMultiTenancy("  ")));
    }

    @Test
    public void parsesDeclaredStrategyCaseInsensitively() {
        assertEquals(MultiTenancyStrategy.DATABASE, KeycloakProcessor.getMultiTenancyStrategy("pu", withMultiTenancy("DATABASE")));
        assertEquals(MultiTenancyStrategy.SCHEMA, KeycloakProcessor.getMultiTenancyStrategy("pu", withMultiTenancy("schema")));
        assertEquals(MultiTenancyStrategy.DISCRIMINATOR, KeycloakProcessor.getMultiTenancyStrategy("pu", withMultiTenancy(" Discriminator ")));
    }

    @Test
    public void rejectsUnknownStrategy() {
        ConfigurationException exception = assertThrows(ConfigurationException.class,
                () -> KeycloakProcessor.getMultiTenancyStrategy("pu", withMultiTenancy("bogus")));
        assertTrue(exception.getMessage().contains("bogus"));
        assertTrue(exception.getMessage().contains("pu"));
    }

    private static Properties withMultiTenancy(String value) {
        Properties properties = new Properties();
        properties.setProperty("hibernate.multiTenancy", value);
        return properties;
    }
}
