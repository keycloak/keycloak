package org.keycloak.quarkus.runtime.configuration.test;

import io.smallrye.config.SmallRyeConfig;
import org.junit.Test;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;

public class DatasourcesConfigurationTest extends AbstractConfigurationTest {

    @Test
    public void testPropertyMapping() {
        ConfigArgsConfigSource.setCliArgs("--db-user.store=mariadb", "--db-user.store-url=jdbc:mariadb://localhost/keycloak");
        SmallRyeConfig config = createConfig();
        System.err.println(config.getConfigValue("kc.db-user.store-dialect"));
        //assertEquals(MariaDBDialect.class.getName(), config.getConfigValue("kc.db-user.store-dialect").getValue());
        //assertEquals("jdbc:mariadb://localhost/keycloak", config.getConfigValue("quarkus.datasource.\"user-store\".jdbc.url").getValue());
    }
}
