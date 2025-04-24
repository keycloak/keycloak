package org.keycloak.quarkus.runtime.configuration.test;

import io.smallrye.config.Expressions;
import io.smallrye.config.SmallRyeConfig;
import org.h2.jdbcx.JdbcDataSource;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.junit.Test;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.mariadb.jdbc.MariaDbDataSource;
import org.postgresql.xa.PGXADataSource;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DatasourcesConfigurationTest extends AbstractConfigurationTest {

    @Test
    public void propertyMapping() {
        ConfigArgsConfigSource.setCliArgs("--db-kind-user-store=mariadb", "--db-url-full-user-store=jdbc:mariadb://localhost/keycloak");

        initConfig();

        assertConfig("db-dialect-user-store", MariaDBDialect.class.getName());
        assertExternalConfig("quarkus.datasource.\"user-store\".jdbc.url", "jdbc:mariadb://localhost/keycloak");
    }

    @Test
    public void driverSetExplicitly() {
        ConfigArgsConfigSource.setCliArgs("--db-kind-user-store=mssql", "--db-url-full-user-store=jdbc:sqlserver://localhost/keycloak");
        System.setProperty("kc.db-driver-user-store", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        System.setProperty("kc.transaction-xa-enabled-user-store", "false");
        assertTrue(ConfigArgsConfigSource.getAllCliArgs().contains("--db-kind-user-store=mssql"));

        initConfig();

        assertConfig("db-kind-user-store", "mssql");
        assertExternalConfig(Map.of(
                "quarkus.datasource.\"user-store\".jdbc.url", "jdbc:sqlserver://localhost/keycloak",
                "quarkus.datasource.\"user-store\".db-kind", "mssql",
                "quarkus.datasource.\"user-store\".jdbc.driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver",
                "quarkus.datasource.\"user-store\".jdbc.transactions", "enabled")
        );
    }

    @Test
    public void urlProperties() {
        ConfigArgsConfigSource.setCliArgs("--db-kind-customstore=mariadb", "--db-url-full-customstore=jdbc:mariadb:aurora://foo/bar?a=1&b=2");

        initConfig();

        assertConfig("db-dialect-customstore", MariaDBDialect.class.getName());
        assertExternalConfig("quarkus.datasource.\"customstore\".jdbc.url", "jdbc:mariadb:aurora://foo/bar?a=1&b=2");
    }

    @Test
    public void expansionDisabled() {
        ConfigArgsConfigSource.setCliArgs("--db-kind-store=mysql");
        SmallRyeConfig config = createConfig();
        String value = Expressions.withoutExpansion(() -> config.getConfigValue("quarkus.datasource.\"store\".jdbc.url").getValue());
        assertEquals("jdbc:mysql://${kc.db-url-host-store:localhost}:${kc.db-url-port-store:3306}/${kc.db-url-database-store:keycloak}${kc.db-url-properties-store:}", value);

        assertExternalConfig("quarkus.datasource.\"store\".jdbc.url", "jdbc:mysql://localhost:3306/keycloak");
    }

    @Test
    public void defaults() {
        ConfigArgsConfigSource.setCliArgs("--db-kind-store=dev-file");
        initConfig();
        assertConfig("db-dialect-store", H2Dialect.class.getName());
        // XA datasource is the default
        assertExternalConfig("quarkus.datasource.\"store\".jdbc.driver", JdbcDataSource.class.getName());
        assertExternalConfig("quarkus.datasource.\"store\".jdbc.url", "jdbc:h2:file:" + Environment.getHomeDir() + "/data/h2/keycloakdb-store;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=0");
        onAfter();

        ConfigArgsConfigSource.setCliArgs("--db-kind-store=dev-mem");
        initConfig();
        assertConfig("db-dialect-store", H2Dialect.class.getName());
        assertExternalConfig("quarkus.datasource.\"store\".jdbc.url", "jdbc:h2:mem:keycloakdb-store;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=0");
        assertExternalConfig("quarkus.datasource.\"store\".db-kind", "h2");
        onAfter();

        ConfigArgsConfigSource.setCliArgs("--db-kind-clients=dev-mem", "--db-username-clients=other");
        initConfig();
        assertExternalConfig("quarkus.datasource.\"clients\".username", "other");
        assertConfig("db-username-clients", "other");
        onAfter();

        ConfigArgsConfigSource.setCliArgs("--db-kind-tigers=dev-mem");
        SmallRyeConfig config = createConfig();
        assertNull(config.getConfigValue("quarkus.datasource.\"tigers\".username").getValue());
        assertNull(config.getConfigValue("quarkus.datasource.\"tigers\".password").getValue());
        assertNull(config.getConfigValue("kc.db-username-tigers").getValue());
        assertNull(config.getConfigValue("kc.db-password-tigers").getValue());
        onAfter();

        ConfigArgsConfigSource.setCliArgs("--db-kind-users=postgres", "--db-username-users=other");
        initConfig();
        assertExternalConfig("quarkus.datasource.\"users\".username", "other");
        onAfter();

        ConfigArgsConfigSource.setCliArgs("--db-kind-some-store=postgres");
        config = createConfig();
        // username  or password should not be set, either as the quarkus or kc property
        assertNull(config.getConfigValue("quarkus.datasource.\"some-store\".username").getValue());
        assertNull(config.getConfigValue("quarkus.datasource.\"some-store\".password").getValue());
        assertNull(config.getConfigValue("kc.db-username-some-store").getValue());
        assertNull(config.getConfigValue("kc.db-password-some-store").getValue());
    }

    @Test
    public void datasourceEnabled() {
        ConfigArgsConfigSource.setCliArgs("");
        initConfig();
        assertConfig("db-enabled-store", "true");
        assertExternalConfig("quarkus.datasource.\"store\".active", "true");
        onAfter();

        ConfigArgsConfigSource.setCliArgs("--db-enabled-store=false");
        initConfig();
        assertConfig("db-enabled-store", "false");
        assertExternalConfig("quarkus.datasource.\"store\".active", "false");
    }

    @Test
    public void datasourceKindProperties() {
        ConfigArgsConfigSource.setCliArgs("--db-kind-my-store=postgres", "--db-url-full-my-store=jdbc:postgresql://localhost/keycloak", "--db-username-my-store=postgres");
        initConfig();

        assertConfig("db-dialect-my-store", "org.hibernate.dialect.PostgreSQLDialect");
        assertExternalConfig(Map.of(
                "quarkus.datasource.\"my-store\".jdbc.url", "jdbc:postgresql://localhost/keycloak",
                "quarkus.datasource.\"my-store\".db-kind", "postgresql",
                "quarkus.datasource.\"my-store\".username", "postgres"
        ));
    }

    @Test
    public void propertiesGetApplied() {
        ConfigArgsConfigSource.setCliArgs("--db-kind-asdf=postgres");
        initConfig();
        assertConfig("db-dialect-asdf", "org.hibernate.dialect.PostgreSQLDialect");
        assertExternalConfig(Map.of(
                "quarkus.datasource.\"asdf\".jdbc.url", "jdbc:postgresql://localhost:5432/keycloak",
                "quarkus.datasource.\"asdf\".db-kind", "postgresql"
        ));
        onAfter();

        ConfigArgsConfigSource.setCliArgs("--db-kind-asdf=postgres", "--db-url-host-asdf=myhost", "--db-url-port-asdf=5432", "--db-url-database-asdf=kcdb", "--db-url-properties-asdf=?foo=bar");
        initConfig();
        assertConfig("db-dialect-asdf", "org.hibernate.dialect.PostgreSQLDialect");
        assertExternalConfig(Map.of(
                "quarkus.datasource.\"asdf\".jdbc.url", "jdbc:postgresql://myhost:5432/kcdb?foo=bar",
                "quarkus.datasource.\"asdf\".db-kind", "postgresql"
        ));
        onAfter();
    }


    @Test
    public void removeSpaceFromValue() {
        ConfigArgsConfigSource.setCliArgs("--db-kind-store=postgres      ");
        initConfig();

        assertConfig("db-dialect-store", "org.hibernate.dialect.PostgreSQLDialect");
        assertExternalConfig("quarkus.datasource.\"store\".db-kind", "postgresql");
        assertThat(Configuration.getConfigValue("quarkus.datasource.\"store\".db-kind").getRawValue(), is("postgres"));
    }

    @Test
    public void defaultDbPortGetApplied() {
        ConfigArgsConfigSource.setCliArgs("--db-kind-realms=mssql", "--db-url-host-realms=myhost", "--db-url-database-realms=kcdb", "--db-url-port-realms=1234", "--db-url-properties-realms=?foo=bar");
        initConfig();

        assertConfig("db-dialect-realms", "org.hibernate.dialect.SQLServerDialect");
        assertExternalConfig(Map.of(
                "quarkus.datasource.\"realms\".jdbc.url", "jdbc:sqlserver://myhost:1234;databaseName=kcdb?foo=bar",
                "quarkus.datasource.\"realms\".db-kind", "mssql"
        ));
    }

    @Test
    public void setDbUrlOverridesDefaultDataSource() {
        ConfigArgsConfigSource.setCliArgs("--db-kind-my-super-duper-store=mariadb", "--db-url-host-my-super-duper-store=myhost", "--db-url-full-my-super-duper-store=jdbc:mariadb://localhost/keycloak");
        initConfig();

        assertConfig("db-dialect-my-super-duper-store", "org.hibernate.dialect.MariaDBDialect");
        assertExternalConfig(Map.of(
                "quarkus.datasource.\"my-super-duper-store\".jdbc.url", "jdbc:mariadb://localhost/keycloak",
                "quarkus.datasource.\"my-super-duper-store\".db-kind", "mariadb"
        ));
    }

    @Test
    public void datasourceProperties() {
        System.setProperty("kc.db-url-properties-clients", ";;test=test;test1=test1");
        System.setProperty("kc.db-url-path", "test-dir");
        System.setProperty("kc.transaction-xa-enabled-clients", "true");
        ConfigArgsConfigSource.setCliArgs("--db-kind-clients=dev-file");

        initConfig();

        assertConfig("db-dialect-clients", H2Dialect.class.getName());
        assertExternalConfig(Map.of(
                "quarkus.datasource.\"clients\".jdbc.url", "jdbc:h2:file:test-dir/data/h2/keycloakdb-clients;;test=test;test1=test1;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=0",
                "quarkus.datasource.\"clients\".jdbc.transactions", "xa"
        ));

        ConfigArgsConfigSource.setCliArgs("");
        initConfig();
        assertConfig("db-dialect-clients", H2Dialect.class.getName());
        assertExternalConfig("quarkus.datasource.\"clients\".jdbc.url", "jdbc:h2:file:test-dir/data/h2/keycloakdb-clients;;test=test;test1=test1;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=0");
        onAfter();

        System.setProperty("kc.db-url-properties-users", "?test=test&test1=test1");
        System.setProperty("kc.transaction-xa-enabled-users", "true");
        ConfigArgsConfigSource.setCliArgs("--db-kind-users=mariadb");
        initConfig();

        assertConfig("db-dialect-users", MariaDBDialect.class.getName());
        assertExternalConfig(Map.of(
                "quarkus.datasource.\"users\".jdbc.url", "jdbc:mariadb://localhost:3306/keycloak?test=test&test1=test1",
                "quarkus.datasource.\"users\".jdbc.driver", MariaDbDataSource.class.getName()
        ));
        onAfter();

        System.setProperty("kc.db-url-properties-elephants", "?test=test&test1=test1");
        System.setProperty("kc.transaction-xa-enabled-elephants", "true");
        ConfigArgsConfigSource.setCliArgs("--db-kind-elephants=postgres");

        initConfig();
        assertConfig("db-dialect-elephants", PostgreSQLDialect.class.getName());
        assertExternalConfig(Map.of(
                "quarkus.datasource.\"elephants\".jdbc.url", "jdbc:postgresql://localhost:5432/keycloak?test=test&test1=test1",
                "quarkus.datasource.\"elephants\".jdbc.driver", PGXADataSource.class.getName()
        ));
        onAfter();

        ConfigArgsConfigSource.setCliArgs("--db-schema-lions=test-schema");
        initConfig();
        assertConfig("db-schema-lions", "test-schema");
    }

    // KEYCLOAK-15632
    @Test
    public void nestedDatasourceProperties() {
        initConfig();
        assertExternalConfig("quarkus.datasource.foo", "jdbc:h2:file:" + Environment.getHomeDir() + "/data/keycloakdb");
        assertExternalConfig("quarkus.datasource.bar", "foo-def-suffix");

        System.setProperty("kc.prop5", "val5");
        initConfig();
        assertExternalConfig("quarkus.datasource.bar", "foo-val5-suffix");

        System.setProperty("kc.prop4", "val4");
        initConfig();
        assertExternalConfig("quarkus.datasource.bar", "foo-val4");

        System.setProperty("kc.prop3", "val3");
        initConfig();
        assertExternalConfig("quarkus.datasource.bar", "foo-val3");
    }
}
