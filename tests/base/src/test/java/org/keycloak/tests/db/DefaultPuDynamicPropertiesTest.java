package org.keycloak.tests.db;

import java.util.Map;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.suites.DatabaseTest;

import org.hibernate.engine.spi.SessionFactoryImplementor;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest(config = DefaultPuDynamicPropertiesTest.SqlDebugAndSlowQueryServerConfig.class)
@DatabaseTest
public class DefaultPuDynamicPropertiesTest {

    @TestOnServer
    public void configDerivedHibernatePropertiesReachSessionFactory(KeycloakSession session) {
        var sf = session.getProvider(JpaConnectionProvider.class)
                .getEntityManager().getEntityManagerFactory()
                .unwrap(SessionFactoryImplementor.class);
        Map<String, Object> props = sf.getProperties();

        assertEquals("true", String.valueOf(props.get("hibernate.use_sql_comments")));
        assertEquals("5000", String.valueOf(props.get("hibernate.log_slow_query")));
    }

    public static class SqlDebugAndSlowQueryServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config
                    .option("db-debug-jpql", "true")
                    .option("db-log-slow-queries-threshold", "5000");
        }
    }
}
