package org.keycloak.tests.db;

import java.util.List;
import java.util.Map;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.tests.suites.DatabaseTest;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.HibernateHints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
@DatabaseTest
public class DefaultPuPropertiesTest {

    @TestOnServer
    public void hibernatePropertiesSurvival(KeycloakSession session) {
        var sf = session.getProvider(JpaConnectionProvider.class)
                .getEntityManager().getEntityManagerFactory()
                .unwrap(SessionFactoryImplementor.class);
        Map<String, Object> props = sf.getProperties();

        assertEquals("32", String.valueOf(props.get("hibernate.jdbc.batch_size")));
        assertEquals("true", String.valueOf(props.get("hibernate.order_inserts")));
        assertEquals("true", String.valueOf(props.get("hibernate.order_updates")));
        assertEquals("8", String.valueOf(props.get("hibernate.default_batch_fetch_size")));
        assertEquals("true", String.valueOf(props.get("hibernate.query.in_clause_parameter_padding")));
        assertEquals("64", String.valueOf(props.get("hibernate.jdbc.fetch_size")));

        assertEquals("false", String.valueOf(props.get("hibernate.query.startup_check")));
        assertEquals("false", String.valueOf(props.get("hibernate.jdbc.log.errors")));
    }

    @TestOnServer
    public void defaultUnitInjectRuntimeConfigDefaults(KeycloakSession session) {
        var sf = session.getProvider(JpaConnectionProvider.class)
                .getEntityManager().getEntityManagerFactory()
                .unwrap(SessionFactoryImplementor.class);
        Map<String, Object> settings = sf.getServiceRegistry()
                .requireService(ConfigurationService.class)
                .getSettings();

        assertEquals("none", String.valueOf(settings.get(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION)));
        assertEquals("none", String.valueOf(settings.get(AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION)));
        assertEquals("false", String.valueOf(settings.get(AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS)));
        assertEquals("AUTO", String.valueOf(settings.get(HibernateHints.HINT_FLUSH_MODE)));
    }

    @TestOnServer
    public void coreEntitiesRegistered(KeycloakSession session) {
        var sf = session.getProvider(JpaConnectionProvider.class)
                .getEntityManager().getEntityManagerFactory()
                .unwrap(SessionFactoryImplementor.class);
        List<String> entityNames = sf.getMetamodel().getEntities().stream()
                .map(e -> e.getJavaType().getName())
                .sorted()
                .toList();

        assertNotNull(entityNames);
        assertTrue(entityNames.size() >= 70,
                "Expected at least 70 entities but found " + entityNames.size());
        assertTrue(entityNames.contains("org.keycloak.models.jpa.entities.UserEntity"));
        assertTrue(entityNames.contains("org.keycloak.models.jpa.entities.RealmEntity"));
        assertTrue(entityNames.contains("org.keycloak.models.jpa.entities.ClientEntity"));
        assertTrue(entityNames.contains("org.keycloak.models.jpa.entities.RoleEntity"));
        assertTrue(entityNames.contains("org.keycloak.models.jpa.entities.GroupEntity"));
    }
}
