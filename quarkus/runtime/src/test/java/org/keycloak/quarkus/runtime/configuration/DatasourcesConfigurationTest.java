package org.keycloak.quarkus.runtime.configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.keycloak.quarkus.runtime.Environment;

import io.smallrye.config.Expressions;
import io.smallrye.config.SmallRyeConfig;
import org.h2.jdbcx.JdbcDataSource;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.junit.Test;
import org.mariadb.jdbc.MariaDbDataSource;
import org.postgresql.xa.PGXADataSource;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DatasourcesConfigurationTest extends AbstractConfigurationTest {

    @Test
    public void defaultDatasource() {
        ConfigArgsConfigSource.setCliArgs("--db-kind-default=mariadb", "--db=postgres");
        initConfig();

        assertConfig("db-kind-default", "mariadb");
        assertConfig("db", "postgres");
        assertExternalConfig("quarkus.datasource.\"default\".db-kind", "mariadb");
        assertExternalConfig("quarkus.datasource.db-kind", "postgresql");

        onAfter();

        ConfigArgsConfigSource.setCliArgs("--db-kind-some<other>datasource=mssql");
        initConfig();

        // KC value is present as CLI is available data source
        assertConfig("db-kind-some<other>datasource", "mssql");
        assertExternalConfigNull("quarkus.datasource.\"some<other>datasource\".db-kind");
    }

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
        assertEquals("mysql", value);

        assertExternalConfig("quarkus.datasource.\"store\".jdbc.url", "jdbc:mysql://localhost:3306/keycloak");
    }

    @Test
    public void defaults() {
        ConfigArgsConfigSource.setCliArgs("--db-kind-store=dev-file");
        initConfig();
        assertConfig("db-dialect-store", H2Dialect.class.getName());
        // XA datasource is the default
        assertExternalConfig("quarkus.datasource.\"store\".jdbc.driver", JdbcDataSource.class.getName());
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
        initConfig();
        assertExternalConfigNull("quarkus.datasource.\"tigers\".username");
        assertExternalConfigNull("quarkus.datasource.\"tigers\".password");
        assertConfigNull("db-username-tigers");
        assertConfigNull("db-password-tigers");
        onAfter();

        ConfigArgsConfigSource.setCliArgs("--db-kind-users=postgres", "--db-username-users=other");
        initConfig();
        assertExternalConfig("quarkus.datasource.\"users\".username", "other");
        onAfter();

        ConfigArgsConfigSource.setCliArgs("--db-kind-some-store=postgres");
        initConfig();
        // username  or password should not be set, either as the quarkus or kc property
        assertExternalConfigNull("quarkus.datasource.\"some-store\".username");
        assertExternalConfigNull("quarkus.datasource.\"some-store\".password");
        assertConfigNull("db-username-some-store");
        assertConfigNull("db-password-some-store");
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

        ConfigArgsConfigSource.setCliArgs("--db-kind-asdf=dev-file", "--db-url-properties-asdf=;DB_CLOSE_ON_EXIT=true");
        initConfig();
        assertExternalConfig(Map.of(
                "quarkus.datasource.\"asdf\".jdbc.url", "jdbc:h2:file:" + Environment.getHomeDir().orElseThrow() + "/data/h2-asdf/keycloakdb-asdf;DB_CLOSE_ON_EXIT=true;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=0",
                "quarkus.datasource.\"asdf\".db-kind", "h2"
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
                "quarkus.datasource.\"clients\".jdbc.url", "jdbc:h2:file:test-dir/data/h2-clients/keycloakdb-clients;;test=test;test1=test1;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=0",
                "quarkus.datasource.\"clients\".jdbc.transactions", "xa"
        ));

        ConfigArgsConfigSource.setCliArgs("");
        initConfig();
        assertConfigNull("db-dialect-clients");
        assertConfigNull("quarkus.datasource.\"clients\".jdbc.url", true);
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
        assertExternalConfig("quarkus.datasource.foo", "jdbc:h2:file:" + Environment.getHomeDir().orElseThrow() + "/data/keycloakdb");
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

    @Test
    public void poolSizeDefault() {
        ConfigArgsConfigSource.setCliArgs("");
        initConfig();

        assertConfigNull("db-pool-initial-size-clients");
        assertConfigNull("db-pool-min-size-clients");
        assertConfig("db-pool-max-size-clients", "100");

        ConfigArgsConfigSource.setCliArgs("--db-kind-clients=dev-mem");
        initConfig();

        assertConfigNull("db-pool-initial-size-clients");
        assertConfig(Map.of(
                "db-pool-min-size-clients", "1",
                "db-pool-max-size-clients", "100"
        ));

        assertExternalConfigNull("quarkus.datasource.\"clients\".jdbc.initial-size");
        assertExternalConfig(Map.of(
                "quarkus.datasource.\"clients\".jdbc.min-size", "1",
                "quarkus.datasource.\"clients\".jdbc.max-size", "100"
        ));
    }

    @Test
    public void poolSizeH2() {
        ConfigArgsConfigSource.setCliArgs("--db-pool-min-size-clients=5", "--db-pool-initial-size-clients=10", "--db-pool-max-size-clients=15");
        initConfig();

        assertConfig(Map.of(
                "db-pool-min-size-clients", "5",
                "db-pool-initial-size-clients", "10",
                "db-pool-max-size-clients", "15"
        ));

        assertExternalConfig(Map.of(
                "quarkus.datasource.\"clients\".jdbc.min-size", "5",
                "quarkus.datasource.\"clients\".jdbc.initial-size", "10",
                "quarkus.datasource.\"clients\".jdbc.max-size", "15"
        ));
        onAfter();

        ConfigArgsConfigSource.setCliArgs("--db-pool-initial-size-clients=10");
        initConfig();
        assertConfigNull("db-pool-min-size-clients");
        assertConfig("db-pool-initial-size-clients", "10");
        onAfter();

        ConfigArgsConfigSource.setCliArgs("--db-pool-initial-size-clients=10", "--db-kind-clients=dev-file");
        initConfig();
        assertConfig(Map.of(
                "db-pool-min-size-clients", "1", // set 1 for H2
                "db-pool-initial-size-clients", "10"
        ));
        onAfter();

        ConfigArgsConfigSource.setCliArgs("--db-kind-clients=mysql", "--db-pool-initial-size-clients=16");
        initConfig();
        assertConfig(Map.of(
                "db-pool-min-size-clients", "1", // set default value (1) for H2 default datasource
                "db-pool-initial-size-clients", "16"
        ));
        onAfter();

        ConfigArgsConfigSource.setCliArgs("--db=postgres", "--db-kind-clients=mysql", "--db-pool-initial-size-clients=10");
        initConfig();
        assertConfig("db-pool-initial-size-clients", "10");
        assertConfigNull("db-pool-min-size-clients"); // set null for non-H2 default datasource
    }

    @Test
    public void poolSizeNonDefaultDbKind() {
        ConfigArgsConfigSource.setCliArgs("-db-kind-store=postgres", "--db-pool-min-size-store=5", "--db-pool-initial-size-store=10", "--db-pool-max-size=15");
        initConfig();

        assertConfig(Map.of(
                "db-pool-min-size-store", "5",
                "db-pool-initial-size-store", "10",
                "db-pool-max-size-store", "15"
        ));

        assertExternalConfig(Map.of(
                "quarkus.datasource.\"store\".jdbc.min-size", "5",
                "quarkus.datasource.\"store\".jdbc.initial-size", "10",
                "quarkus.datasource.\"store\".jdbc.max-size", "15"
        ));
    }

    @Test
    public void poolSizeInherit() {
        ConfigArgsConfigSource.setCliArgs("--db-pool-min-size=25", "--db-pool-initial-size=50", "--db-pool-max-size=115", "--db-kind-users=mssql");
        initConfig();

        assertConfig(Map.of(
                "db-pool-min-size", "25",
                "db-pool-min-size-users", "25",
                "db-pool-initial-size", "50",
                "db-pool-initial-size-users", "50",
                "db-pool-max-size", "115",
                "db-pool-max-size-users", "115"
        ));

        assertExternalConfig(Map.of(
                "quarkus.datasource.jdbc.min-size", "25",
                "quarkus.datasource.\"users\".jdbc.min-size", "25",
                "quarkus.datasource.jdbc.initial-size", "50",
                "quarkus.datasource.\"users\".jdbc.initial-size", "50",
                "quarkus.datasource.jdbc.max-size", "115",
                "quarkus.datasource.\"users\".jdbc.max-size", "115"
        ));
    }

    @Test
    public void envVarsHandling() {
        putEnvVars(Map.of(
                "KC_DB_KIND_USER_STORE", "postgres",
                "KC_DB_URL_FULL_USER_STORE", "jdbc:postgresql://localhost/KEYCLOAK",
                "KC_DB_USERNAME_USER_STORE", "my-username",
                "KC_DB_KIND_MY_STORE", "mariadb"
        ));
        initConfig();

        assertConfig(Map.of(
                "db-kind-user-store", "postgres",
                "db-url-full-user-store", "jdbc:postgresql://localhost/KEYCLOAK",
                "db-username-user-store", "my-username",
                "db-kind-my-store", "mariadb"
        ));

        assertExternalConfig(Map.of(
                "quarkus.datasource.\"user-store\".db-kind", "postgresql",
                "quarkus.datasource.\"user-store\".jdbc.url", "jdbc:postgresql://localhost/KEYCLOAK",
                "quarkus.datasource.\"user-store\".username", "my-username",
                "quarkus.datasource.\"my-store\".db-kind", "mariadb"
        ));

        assertThat(Configuration.getPropertyNames(), hasItem("quarkus.datasource.\"my-store\".db-kind"));
        assertThat(Configuration.getPropertyNames(), not(hasItem("quarkus.datasource.\"my.store\".db-kind")));
    }

    @Test
    public void envVarsSpecialChars() {
        putEnvVars(Map.of(
                "KC_USER_STORE_DB_KIND", "mariadb",
                "KCKEY_USER_STORE_DB_KIND", "db-kind-user_store$something",
                "KC_CLIENT_STORE_PW", "password",
                "KCKEY_CLIENT_STORE_PW", "db-password-client.store_123"
        ));
        initConfig();

        assertConfig(Map.of(
                "db-kind-user_store$something", "mariadb",
                "db-password-client.store_123", "password"
        ));

        assertExternalConfig(Map.of(
                "quarkus.datasource.\"user_store$something\".db-kind", "mariadb",
                "quarkus.datasource.\"client.store_123\".password", "password"
        ));
    }

    @Test
    public void sqlParameters() {
        ConfigArgsConfigSource.setCliArgs("--db-kind-my-store=dev-mem");
        initConfig();

        assertConfig(Map.of(
                "db-kind-my-store", "dev-mem",
                "db-debug-jpql-my-store", "false",
                "db-log-slow-queries-threshold-my-store", "10000"
        ));
        onAfter();

        ConfigArgsConfigSource.setCliArgs("--db-kind-my-store=dev-mem", "--db-debug-jpql-my-store=true", "--db-log-slow-queries-threshold-my-store=5000");
        initConfig();

        assertConfig(Map.of(
                "db-kind-my-store", "dev-mem",
                "db-debug-jpql-my-store", "true",
                "db-log-slow-queries-threshold-my-store","5000"
        ));
    }

    @Test
    public void propagatedPropertyNames() {
        ConfigArgsConfigSource.setCliArgs("--db-kind-user-store=mysql");

        var config = createConfig();

        List<String> propertyNames = StreamSupport
                .stream(config.getPropertyNames().spliterator(), false)
                .collect(Collectors.toList());

        assertThat(propertyNames, hasItems(
                "kc.db-kind-user-store",
                "quarkus.datasource.\"user-store\".db-kind",
                "quarkus.datasource.\"user-store\".jdbc.url",
                "quarkus.datasource.\"user-store\".jdbc.transactions"
        ));

        // verify the db-kind is there only once
        long quarkusDbKindCount = propertyNames.stream()
                .filter("quarkus.datasource.\"user-store\".jdbc.url"::equals)
                .count();
        assertThat(quarkusDbKindCount, is(1L));

        assertThat(propertyNames, not(hasItems(
                "kc.db-dialect-user-store",
                "quarkus.datasource.\"user-store\".username",
                "quarkus.datasource.\"user-store\".password",
                "quarkus.datasource.\"user-store\".jdbc.driver"
        )));
    }
}
