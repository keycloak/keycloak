package org.keycloak.quarkus.runtime.configuration;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.keycloak.config.DatabaseOptions;
import org.keycloak.config.OptionCategory;
import org.keycloak.config.database.Database;
import org.keycloak.connections.jpa.util.JpaUtils;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.mappers.DatabasePropertyMappers;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import io.smallrye.config.Expressions;
import io.smallrye.config.SmallRyeConfig;
import org.h2.jdbcx.JdbcDataSource;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.junit.Test;
import org.mariadb.jdbc.MariaDbDataSource;
import org.postgresql.ssl.DefaultJavaSSLFactory;
import org.postgresql.xa.PGXADataSource;

import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
        assertConfig("db-dialect-store", "org.keycloak.connections.jpa.dialect.KeycloakH2Dialect");
        // XA datasource is the default
        assertExternalConfig("quarkus.datasource.\"store\".jdbc.driver", JdbcDataSource.class.getName());
        onAfter();

        ConfigArgsConfigSource.setCliArgs("--db-kind-store=dev-mem");
        initConfig();
        assertConfig("db-dialect-store", "org.keycloak.connections.jpa.dialect.KeycloakH2Dialect");
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

        assertConfig("db-dialect-realms", "org.keycloak.connections.jpa.dialect.KeycloakSQLServerDialect");
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

        assertConfig("db-dialect-clients", "org.keycloak.connections.jpa.dialect.KeycloakH2Dialect");
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
                "db-log-slow-queries-threshold-my-store", "5000"
        ));
    }

    @Test
    public void propagatedPropertyNames() {
        ConfigArgsConfigSource.setCliArgs("--db-kind-user-store=mysql");

        var config = createConfig();

        List<String> propertyNames = StreamSupport
                .stream(config.getPropertyNames().spliterator(), false)
                .collect(Collectors.toList());

        List<String> expectedNames = List.of("kc.db-kind-user-store",
                "quarkus.datasource.\"user-store\".db-kind",
                "quarkus.datasource.\"user-store\".jdbc.url",
                "quarkus.datasource.\"user-store\".jdbc.transactions",
                "quarkus.datasource.\"user-store\".jdbc.max-size",
                "quarkus.datasource.\"user-store\".jdbc.driver",
                "quarkus.datasource.jdbc.min-size",
                "quarkus.datasource.jdbc.driver",
                "quarkus.datasource.jdbc.url");

        expectedNames.forEach(n -> assertThat(propertyNames, hasItem(n)));

        Stream.of("quarkus.datasource.\"user-store\".username"
                        , "quarkus.datasource.\"user-store\".password")
                .forEach(n -> assertThat(propertyNames, not(hasItem(n))));
    }

    @Test
    public void testPostgresTLSOptions() {
        doDatabaseTlsOptionTest("postgres",
                "jdbc:postgresql://myhost:5432/keycloak",
                Map.of("sslmode", "verify-full"),
                Map.of("sslfactory", DefaultJavaSSLFactory.class.getName()),
                "sslrootcert",
                null,
                null);
    }

    @Test
    public void testMysqlTLSOptions() {
        doDatabaseTlsOptionTest("mysql",
                "jdbc:mysql://myhost:3306/keycloak",
                Map.of("sslMode", "VERIFY_IDENTITY"),
                Map.of(),
                "trustCertificateKeyStoreUrl",
                "trustCertificateKeyStorePassword",
                null);
    }

    @Test
    public void testMssqlTLSOptions() {
        doDatabaseTlsOptionTest("mssql",
                "jdbc:sqlserver://myhost:1433;databaseName=keycloak",
                Map.of("encrypt", "true", "trustServerCertificate", "false"),
                Map.of(),
                "trustStore",
                "trustStorePassword",
                null);
    }

    @Test
    public void testMariadbTLSOptions() {
        doDatabaseTlsOptionTest("mariadb",
                "jdbc:mariadb://myhost:3306/keycloak",
                Map.of("sslMode", "verify-full"),
                Map.of(),
                "serverSslCert",
                null,
                null);
    }

    @Test
    public void testOracleTLSOptions() {
        doDatabaseTlsOptionTest("oracle",
                "jdbc:oracle:thin:@//myhost:1521/keycloak",
                Map.of("ssl_server_dn_match", "true"),
                Map.of(),
                "javax.net.ssl.trustStore",
                "javax.net.ssl.trustStorePassword",
                "javax.net.ssl.trustStoreType");
    }

    @Test public void testDbConnectTimeout() {
        var config = createConfigFromCliArguments("--db=mysql", "--db-kind-users=postgresql", "--db-connect-timeout=15");

        assertEquals("15000", config.getConfigValue(DatabasePropertyMappers.CONNECT_TIMEOUT).getValue());

        // uses the default value, rather than the primary timeout
        assertEquals("10", config.getConfigValue("quarkus.datasource.\"users\".jdbc.additional-jdbc-properties.connectTimeout").getValue());

        config = createConfigFromCliArguments("--db=mysql", "--db-kind-users=postgresql", "--db-connect-timeout-users=15");

        assertEquals("10000", config.getConfigValue(DatabasePropertyMappers.CONNECT_TIMEOUT).getValue());
        assertEquals("15", config.getConfigValue("quarkus.datasource.\"users\".jdbc.additional-jdbc-properties.connectTimeout").getValue());
        // the to mapping for other source types should not be set
        assertNull(config.getConfigValue("quarkus.datasource.\"users\".jdbc.additional-jdbc-properties.loginTimeout").getValue());

        // Oracle named datasource in XA mode
        config = createConfigFromCliArguments("--db=postgres", "--db-kind-users=oracle", "--transaction-xa-enabled-users=true");
        assertEquals("oracle.net.CONNECT_TIMEOUT=10000", config.getConfigValue("quarkus.datasource.\"users\".jdbc.additional-jdbc-properties.ConnectionProperties").getValue());
        assertNull(config.getConfigValue("quarkus.datasource.\"users\".jdbc.additional-jdbc-properties.oracle.net.CONNECT_TIMEOUT").getValue());

        // Oracle named datasource in XA mode — user sets ConnectionProperties directly
        setSystemProperty("quarkus.datasource.\"users\".jdbc.additional-jdbc-properties.ConnectionProperties", "oracle.net.keepAlive=true", () -> {
            SmallRyeConfig xaConfig = createConfigFromCliArguments("--db=postgres", "--db-kind-users=oracle", "--transaction-xa-enabled-users=true");
            assertEquals("oracle.net.keepAlive=true", xaConfig.getConfigValue("quarkus.datasource.\"users\".jdbc.additional-jdbc-properties.ConnectionProperties").getValue());
        });
        setSystemProperty("quarkus.datasource.\"users\".jdbc.additional-jdbc-properties.ConnectionProperties", "oracle.net.keepAlive=true", () -> {
            SmallRyeConfig xaConfig = createConfigFromCliArguments("--db=postgres", "--db-kind-users=oracle", "--transaction-xa-enabled-users=true", "--db-connect-timeout-users=30s");
            assertEquals("oracle.net.keepAlive=true", xaConfig.getConfigValue("quarkus.datasource.\"users\".jdbc.additional-jdbc-properties.ConnectionProperties").getValue());
        });
    }

    private static void doDatabaseTlsOptionTest(String dbKind, String dbUrl,
                                                // common property with or without --db-tls-truststore-file
                                                Map<String, String> tlsJdbcProperties,
                                                // other properties available when --db-tls-truststore-file is not set
                                                Map<String, String> withJavaTrustStoreJdbcProperties,
                                                String truststoreFileProperty,
                                                String trustStorePasswordProperty,
                                                String trustStoreTypeProperty) {
        // default DB configured to H2 memory file
        var h2Url = "jdbc:h2:mem:keycloakdb;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=0";

        var config = createConfigFromCliArguments("--db=dev-mem", "--db-kind-users=" + dbKind, "--db-url-host-users=myhost", "--db-tls-mode-users=disabled");
        assertEquals(h2Url, config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
        assertEquals(dbUrl, config.getConfigValue("quarkus.datasource.\"users\".jdbc.url").getValue());

        assertNullAllAdditionalJdbcProperty(config, null, tlsJdbcProperties.keySet());
        assertNullAllAdditionalJdbcProperty(config, null, withJavaTrustStoreJdbcProperties.keySet());
        assertNullAdditionalJdbcProperty(config, null, truststoreFileProperty);
        assertNullAdditionalJdbcProperty(config, null, trustStorePasswordProperty);
        assertNullAdditionalJdbcProperty(config, null, trustStoreTypeProperty);


        assertNullAllAdditionalJdbcProperty(config, "users", tlsJdbcProperties.keySet());
        assertNullAllAdditionalJdbcProperty(config, "users", withJavaTrustStoreJdbcProperties.keySet());
        assertNullAdditionalJdbcProperty(config, "users", truststoreFileProperty);
        assertNullAdditionalJdbcProperty(config, "users", trustStorePasswordProperty);
        assertNullAdditionalJdbcProperty(config, "users", trustStoreTypeProperty);

        // MSSQL-specific property should not leak into non-MSSQL named datasources
        if (!"mssql".equals(dbKind)) {
            assertNullAdditionalJdbcProperty(config, "users", "sendStringParametersAsUnicode");
        }

        // oracle has a different protocol for TLS
        if ("oracle".equals(dbKind)) {
            dbUrl = dbUrl.replace("jdbc:oracle:thin:@//", "jdbc:oracle:thin:@tcps://");
        }

        // check defaults
        config = createConfigFromCliArguments("--db=dev-mem", "--db-kind-users=" + dbKind, "--db-url-host-users=myhost", "--db-tls-mode-users=verify-server");
        assertEquals(h2Url, config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
        assertEquals(dbUrl, config.getConfigValue("quarkus.datasource.\"users\".jdbc.url").getValue());

        assertNullAllAdditionalJdbcProperty(config, null, tlsJdbcProperties.keySet());
        assertNullAllAdditionalJdbcProperty(config, null, withJavaTrustStoreJdbcProperties.keySet());
        assertNullAdditionalJdbcProperty(config, null, truststoreFileProperty);
        assertNullAdditionalJdbcProperty(config, null, trustStorePasswordProperty);
        assertNullAdditionalJdbcProperty(config, null, trustStoreTypeProperty);

        assertAllAdditionalJdbcProperty(config, "users", tlsJdbcProperties);
        assertAllAdditionalJdbcProperty(config, "users", withJavaTrustStoreJdbcProperties);
        assertNullAdditionalJdbcProperty(config, "users", truststoreFileProperty);
        assertNullAdditionalJdbcProperty(config, "users", trustStorePasswordProperty);
        assertNullAdditionalJdbcProperty(config, "users", trustStoreTypeProperty);

        // make sure we don't overwrite anything from the user input
        var property = tlsJdbcProperties.keySet().iterator().next();
        var urlProperty = "?%s=bar".formatted(property);
        var arg = "--db-url-properties-users=%s".formatted(urlProperty);

        config = createConfigFromCliArguments("--db=dev-mem", "--db-kind-users=" + dbKind, "--db-url-host-users=myhost", "--db-tls-mode-users=verify-server", arg);
        assertEquals(h2Url, config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
        assertEquals(dbUrl + urlProperty, config.getConfigValue("quarkus.datasource.\"users\".jdbc.url").getValue());

        assertNullAllAdditionalJdbcProperty(config, null, tlsJdbcProperties.keySet());
        assertNullAllAdditionalJdbcProperty(config, null, withJavaTrustStoreJdbcProperties.keySet());
        assertNullAdditionalJdbcProperty(config, null, truststoreFileProperty);
        assertNullAdditionalJdbcProperty(config, null, trustStorePasswordProperty);
        assertNullAdditionalJdbcProperty(config, null, trustStoreTypeProperty);

        for (var entry : tlsJdbcProperties.entrySet()) {
            if (entry.getKey().equals(property)) {
                assertNullAdditionalJdbcProperty(config, "users", entry.getKey());
            } else {
                assertAdditionalJdbcProperty(config, "users", entry.getKey(), entry.getValue());
            }
        }
        assertAllAdditionalJdbcProperty(config, "users", withJavaTrustStoreJdbcProperties);
        assertNullAdditionalJdbcProperty(config, "users", truststoreFileProperty);
        assertNullAdditionalJdbcProperty(config, "users", trustStorePasswordProperty);
        assertNullAdditionalJdbcProperty(config, "users", trustStoreTypeProperty);

        config = createConfigFromCliArguments("--db=dev-mem", "--db-kind-users=" + dbKind, "--db-url-host-users=myhost", "--db-tls-mode-users=verify-server", "--db-tls-trust-store-file-users=cert.pem", "--db-tls-trust-store-password-users=no-secret", "--db-tls-trust-store-type-users=pem");

        assertEquals(h2Url, config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
        assertEquals(dbUrl, config.getConfigValue("quarkus.datasource.\"users\".jdbc.url").getValue());

        assertNullAllAdditionalJdbcProperty(config, null, tlsJdbcProperties.keySet());
        assertNullAllAdditionalJdbcProperty(config, null, withJavaTrustStoreJdbcProperties.keySet());
        assertNullAdditionalJdbcProperty(config, null, truststoreFileProperty);
        assertNullAdditionalJdbcProperty(config, null, trustStorePasswordProperty);
        assertNullAdditionalJdbcProperty(config, null, trustStoreTypeProperty);

        assertAllAdditionalJdbcProperty(config, "users", tlsJdbcProperties);
        assertNullAllAdditionalJdbcProperty(config, "users", withJavaTrustStoreJdbcProperties.keySet());
        assertAdditionalJdbcProperty(config, "users", truststoreFileProperty, "cert.pem");
        assertAdditionalJdbcProperty(config, "users", trustStorePasswordProperty, "no-secret");
        assertAdditionalJdbcProperty(config, "users", trustStoreTypeProperty, "pem");
    }

    @Test
    public void testPostgresMTLSOptions() {
        doDatabaseMtlsOptionTest("postgres",
                "sslkey",
                "sslpassword",
                null);
    }

    @Test
    public void testMysqlMTLSOptions() {
        doDatabaseMtlsOptionTest("mysql",
                "clientCertificateKeyStoreUrl",
                "clientCertificateKeyStorePassword",
                "clientCertificateKeyStoreType");
    }

    @Test
    public void testMariadbMTLSOptions() {
        doDatabaseMtlsOptionTest("mariadb",
                "keyStore",
                "keyStorePassword",
                null);
    }

    @Test
    public void testOracleMTLSOptions() {
        var dbKind = "oracle";
        doDatabaseMtlsOptionTest(dbKind,
                "javax.net.ssl.keyStore",
                "javax.net.ssl.keyStorePassword",
                "javax.net.ssl.keyStoreType");

        // when TLS is disabled, mTLS properties should not be set on the named datasource
        var config = configNoMTLS(dbKind);
        assertNullAdditionalJdbcProperty(config, null, "oracle.net.authentication_services");
        assertNullAdditionalJdbcProperty(config, "users", "oracle.net.authentication_services");

        // when TLS is enabled and mTLS options are set on the named datasource
        config = configWithMTLS(dbKind);

        assertNullAdditionalJdbcProperty(config, null, "oracle.net.authentication_services");
        assertAdditionalJdbcProperty(config, "users", "oracle.net.authentication_services", "(TCPS)");
    }

    private static void doDatabaseMtlsOptionTest(String dbKind,
                                                  String keyStoreFileProperty,
                                                  String keyStorePasswordProperty,
                                                  String keyStoreTypeProperty) {
        var h2Url = "jdbc:h2:mem:keycloakdb;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=0";

        // when TLS is disabled, mTLS properties should not be set on the named datasource
        var config = configNoMTLS(dbKind);
        assertNullAdditionalJdbcProperty(config, null, keyStoreFileProperty);
        assertNullAdditionalJdbcProperty(config, "users", keyStoreFileProperty);
        assertNullAdditionalJdbcProperty(config, "users", keyStorePasswordProperty);
        assertNullAdditionalJdbcProperty(config, "users", keyStoreTypeProperty);

        // when TLS is enabled and mTLS options are set on the named datasource
        config = configWithMTLS(dbKind);
        assertEquals(h2Url, config.getConfigValue("quarkus.datasource.jdbc.url").getValue());

        // main datasource should not have mTLS properties
        assertNullAdditionalJdbcProperty(config, null, keyStoreFileProperty);
        assertNullAdditionalJdbcProperty(config, null, keyStorePasswordProperty);
        assertNullAdditionalJdbcProperty(config, null, keyStoreTypeProperty);

        // named datasource should have mTLS properties
        assertAdditionalJdbcProperty(config, "users", keyStoreFileProperty, "keystore.p12");
        assertAdditionalJdbcProperty(config, "users", keyStorePasswordProperty, "secret");
        assertAdditionalJdbcProperty(config, "users", keyStoreTypeProperty, "PKCS12");
    }

    private static SmallRyeConfig configNoMTLS(String dbKind) {
        return createConfigFromCliArguments("--db=dev-mem", "--db-kind-users=" + dbKind, "--db-url-host-users=myhost", "--db-tls-mode-users=disabled",
              "--db-mtls-key-store-file-users=keystore.p12", "--db-mtls-key-store-password-users=secret", "--db-mtls-key-store-type-users=PKCS12");
    }

    private static SmallRyeConfig configWithMTLS(String dbKind) {
        return createConfigFromCliArguments("--db=dev-mem", "--db-kind-users=" + dbKind, "--db-url-host-users=myhost", "--db-tls-mode-users=verify-server",
              "--db-mtls-key-store-file-users=keystore.p12", "--db-mtls-key-store-password-users=secret", "--db-mtls-key-store-type-users=PKCS12");
    }

    @Test
    public void testRawDatabasePassword() {
        putEnvVar("KCRAW_DB_PASSWORD", "p@ss$$w0rd${special}");
        ConfigArgsConfigSource.setCliArgs("--db=postgres");
        SmallRyeConfig config = createConfig();
        // The raw password should be preserved exactly as-is, with no expression evaluation
        assertEquals("p@ss$$w0rd${special}", config.getConfigValue("kc.db-password").getValue());
        assertEquals("p@ss$$w0rd${special}", config.getConfigValue("quarkus.datasource.password").getValue());
    }

    @Test
    public void defaultPersistenceUnitHibernateMappings() {
        ConfigArgsConfigSource.setCliArgs("--db=postgres", "--db-log-slow-queries-threshold=5000", "--db-debug-jpql=true");
        initConfig();

        assertExternalConfig("quarkus.hibernate-orm.dialect", PostgreSQLDialect.class.getName());
        assertExternalConfig("quarkus.hibernate-orm.log.queries-slower-than-ms", "5000");
        assertExternalConfig("quarkus.hibernate-orm.unsupported-properties.\"hibernate.use_sql_comments\"", "true");

        onAfter();

        ConfigArgsConfigSource.setCliArgs("--db=postgres");
        initConfig();

        assertExternalConfig("quarkus.hibernate-orm.dialect", PostgreSQLDialect.class.getName());
        assertExternalConfig("quarkus.hibernate-orm.log.queries-slower-than-ms", "10000");
        assertExternalConfigNull("quarkus.hibernate-orm.unsupported-properties.\"hibernate.use_sql_comments\"");
    }

    @Test
    public void namedQueriesMappedForVendorSpecificDbKind() {
        ConfigArgsConfigSource.setCliArgs("--db=mariadb");
        SmallRyeConfig config = createConfig();
        String namedQuery = config.getConfigValue(
                "quarkus.hibernate-orm.unsupported-properties.\"kc.query.deleteExpiredClientSessions[native]\"").getValue();
        assertTrue("Expected the mariadb-specific named-query SQL, but was: " + namedQuery,
                namedQuery != null && namedQuery.contains("OFFLINE_CLIENT_SESSION"));
    }

    @Test
    public void namedQueryKeysAreEnumeratedForVendorSpecificDbKind() {
        String bracketedKey = "quarkus.hibernate-orm.unsupported-properties.\"kc.query.deleteExpiredClientSessions[native]\"";

        ConfigArgsConfigSource.setCliArgs("--db=mariadb");
        SmallRyeConfig mariadb = createConfig();
        assertTrue("Vendor-specific named-query key must be enumerated for mariadb (Quarkus' map would miss it otherwise)",
                StreamSupport.stream(mariadb.getPropertyNames().spliterator(), false).anyMatch(bracketedKey::equals));

        onAfter();

        ConfigArgsConfigSource.setCliArgs("--db=postgres");
        SmallRyeConfig postgres = createConfig();
        assertTrue("Named-query key must not be enumerated for a db kind without vendor-specific queries",
                StreamSupport.stream(postgres.getPropertyNames().spliterator(), false).noneMatch(bracketedKey::equals));
    }

    @Test
    public void dialectMappedForAllSupportedDbKinds() {
        for (String alias : Database.getDatabaseAliases()) {
            String expectedDialect = Database.getDialect(alias).orElse(null);
            if (expectedDialect == null) {
                continue;
            }
            ConfigArgsConfigSource.setCliArgs("--db=" + alias);
            initConfig();
            assertExternalConfig("quarkus.hibernate-orm.dialect", expectedDialect);
            onAfter();
        }
    }

    @Test
    public void originalDbMappingsUnaffectedByNewHibernateMappers() {
        ConfigArgsConfigSource.setCliArgs("--db=postgres", "--db-kind-user-store=mariadb",
                "--db-url-full-user-store=jdbc:mariadb://localhost/keycloak");
        initConfig();

        assertExternalConfig("quarkus.datasource.db-kind", "postgresql");
        assertConfig("db-dialect", PostgreSQLDialect.class.getName());
        assertConfig("db-dialect-user-store", MariaDBDialect.class.getName());
        assertExternalConfig("quarkus.hibernate-orm.dialect", PostgreSQLDialect.class.getName());
    }

    @Test
    public void namedDatasourceHibernatePropertiesNotMappedWithoutConfiguredPersistenceUnit() {
        ConfigArgsConfigSource.setCliArgs("--db-kind-my-store=mariadb", "--db-debug-jpql-my-store=true",
                "--db-log-slow-queries-threshold-my-store=5000", "--db-schema-my-store=other");
        initConfig();

        // the datasource options are resolved as usual, and some of them always have a value: the dialect is derived
        // from the kind, and the slow query threshold has a default
        assertConfig(Map.of(
                "db-dialect-my-store", MariaDBDialect.class.getName(),
                "db-debug-jpql-my-store", "true",
                "db-log-slow-queries-threshold-my-store", "5000",
                "db-schema-my-store", "other"));

        // Quarkus defines a persistence unit for every name it finds a quarkus.hibernate-orm property of, so none of the
        // options is mapped unless db-jpa-packages-my-store defines the persistence unit of the datasource: the values
        // that are always present must not define a unit for every named datasource, and a persistence unit defined by
        // a persistence.xml applies the options through KeycloakProcessor#getUserPersistenceUnitOverrides instead
        assertExternalConfigNull("quarkus.hibernate-orm.\"my-store\".packages");
        assertExternalConfigNull("quarkus.hibernate-orm.\"my-store\".datasource");
        assertExternalConfigNull("quarkus.hibernate-orm.\"my-store\".dialect");
        assertExternalConfigNull("quarkus.hibernate-orm.\"my-store\".unsupported-properties.\"hibernate.use_sql_comments\"");
        assertExternalConfigNull("quarkus.hibernate-orm.\"my-store\".log.queries-slower-than-ms");
        assertExternalConfigNull("quarkus.hibernate-orm.\"my-store\".database.default-schema");
    }

    @Test
    public void namedDatasourcePersistenceUnitProperties() {
        ConfigArgsConfigSource.setCliArgs("--db-kind-my-store=mariadb", "--db-jpa-packages-my-store=org.example.entities,org.example.more",
                "--db-debug-jpql-my-store=true", "--db-log-slow-queries-threshold-my-store=5000", "--db-schema-my-store=other");
        initConfig();

        assertExternalConfig(Map.of(
                "quarkus.hibernate-orm.\"my-store\".packages", "org.example.entities,org.example.more",
                "quarkus.hibernate-orm.\"my-store\".datasource", "my-store",
                "quarkus.hibernate-orm.\"my-store\".dialect", MariaDBDialect.class.getName(),
                "quarkus.hibernate-orm.\"my-store\".unsupported-properties.\"hibernate.use_sql_comments\"", "true",
                "quarkus.hibernate-orm.\"my-store\".log.queries-slower-than-ms", "5000",
                "quarkus.hibernate-orm.\"my-store\".database.default-schema", "other"));

        // Keycloak's named queries belong to the default persistence unit only
        assertExternalConfigNull("quarkus.hibernate-orm.\"my-store\".unsupported-properties.\"kc.query.deleteExpiredClientSessions[native]\"");
    }

    @Test
    public void namedDatasourcePersistenceUnitDefaultsAndOverrides() {
        ConfigArgsConfigSource.setCliArgs("--db=postgres", "--db-kind-my-store=mariadb", "--db-jpa-packages-my-store=org.example.entities",
                "--db-dialect-my-store=org.example.MyDialect");
        initConfig();

        assertExternalConfig(Map.of(
                "quarkus.hibernate-orm.\"my-store\".dialect", "org.example.MyDialect",
                "quarkus.hibernate-orm.\"my-store\".log.queries-slower-than-ms", "10000"));
        assertExternalConfigNull("quarkus.hibernate-orm.\"my-store\".unsupported-properties.\"hibernate.use_sql_comments\"");
        assertExternalConfigNull("quarkus.hibernate-orm.\"my-store\".database.default-schema");

        // the default persistence unit is not affected by the named one
        assertExternalConfig("quarkus.hibernate-orm.dialect", PostgreSQLDialect.class.getName());
        assertExternalConfig("quarkus.hibernate-orm.log.queries-slower-than-ms", "10000");
        assertExternalConfigNull("quarkus.hibernate-orm.unsupported-properties.\"hibernate.use_sql_comments\"");
    }

    @Test
    public void persistenceUnitPropertiesAdvertisedForConfiguredUnitOnly() {
        // Quarkus discovers the configuration of a named persistence unit from the property names, so the properties
        // of a configured unit must be advertised, including the defaults of the Keycloak options
        ConfigArgsConfigSource.setCliArgs("--db-kind-my-store=mariadb", "--db-jpa-packages-my-store=org.example.entities",
                "--db-kind-other-store=mariadb", "--db-debug-jpql-other-store=true");
        initConfig();

        Set<String> names = StreamSupport.stream(Configuration.getPropertyNames().spliterator(), false).collect(Collectors.toSet());
        assertTrue(names.toString(), names.contains("quarkus.hibernate-orm.\"my-store\".packages"));
        assertTrue(names.toString(), names.contains("quarkus.hibernate-orm.\"my-store\".datasource"));
        assertTrue(names.toString(), names.contains("quarkus.hibernate-orm.\"my-store\".dialect"));
        assertTrue(names.toString(), names.contains("quarkus.hibernate-orm.\"my-store\".log.queries-slower-than-ms"));
        assertFalse(names.toString(), names.contains("quarkus.hibernate-orm.\"my-store\".unsupported-properties.\"hibernate.use_sql_comments\""));
        assertFalse(names.toString(), names.contains("quarkus.hibernate-orm.\"my-store\".database.default-schema"));
        assertTrue(names.toString(), names.stream().noneMatch(n -> n.startsWith("quarkus.hibernate-orm.\"other-store\"")));
        // the Keycloak options of the datasource are advertised as before
        assertTrue(names.toString(), names.contains("kc.db-dialect-other-store"));
        assertTrue(names.toString(), names.contains("kc.db-log-slow-queries-threshold-other-store"));
    }

    @Test
    public void hibernateOrmOptionsFromEnvironmentVariables() {
        // the Hibernate ORM options cannot be set on the command line (see PicocliTest), they are set through the other
        // configuration sources, and like the other database options they apply to the persistence unit of a named
        // datasource with the -<datasource> suffix
        putEnvVar("KC_DB_ORM_QUERY_QUERY_PLAN_CACHE_MAX_SIZE", "512");
        putEnvVar("KC_DB_ORM_QUERY_QUERY_PLAN_CACHE_MAX_SIZE_MY_STORE", "256");
        putEnvVar("KC_DB_ORM_QUERY_QUERY_PLAN_CACHE_MAX_SIZE_OTHER_STORE", "128");
        ConfigArgsConfigSource.setCliArgs("--db=postgres", "--db-kind-my-store=mariadb", "--db-jpa-packages-my-store=org.example.entities",
                "--db-kind-other-store=mariadb");
        initConfig();

        assertConfig(Map.of(
                "db-orm-query-query-plan-cache-max-size", "512",
                "db-orm-query-query-plan-cache-max-size-my-store", "256",
                "db-orm-query-query-plan-cache-max-size-other-store", "128"));
        assertExternalConfig(Map.of(
                "quarkus.hibernate-orm.query.query-plan-cache-max-size", "512",
                "quarkus.hibernate-orm.\"my-store\".query.query-plan-cache-max-size", "256"));
        // db-jpa-packages-other-store does not define a persistence unit for other-store, so the option does not reach
        // its property, which stays at the default Quarkus supplies for any unit name (see QuarkusDefaultsTestConfigSource)
        assertExternalConfig("quarkus.hibernate-orm.\"other-store\".query.query-plan-cache-max-size", QuarkusDefaultsTestConfigSource.QUERY_PLAN_CACHE_MAX_SIZE_DEFAULT);

        // Quarkus discovers the properties from the property names, and defines a persistence unit for every name it
        // finds, so the property of other-store must not be advertised although it resolves to the Quarkus default
        Set<String> names = StreamSupport.stream(Configuration.getPropertyNames().spliterator(), false).collect(Collectors.toSet());
        assertTrue(names.toString(), names.contains("quarkus.hibernate-orm.query.query-plan-cache-max-size"));
        assertTrue(names.toString(), names.contains("quarkus.hibernate-orm.\"my-store\".query.query-plan-cache-max-size"));
        assertTrue(names.toString(), names.stream().noneMatch(n -> n.startsWith("quarkus.hibernate-orm.\"other-store\"")));
    }

    @Test
    public void hibernateOrmOptionsFromConfigurationFile() {
        String configFile = Paths.get("src/test/resources/conf/hibernate-orm.conf").toAbsolutePath().toString();
        setSystemProperty(KeycloakPropertiesConfigSource.KEYCLOAK_CONFIG_FILE_PROP, configFile, () -> {
            ConfigArgsConfigSource.setCliArgs("--db=postgres", "--db-kind-my-store=mariadb", "--db-jpa-packages-my-store=org.example.entities");
            initConfig();

            assertConfig(Map.of(
                    "db-orm-query-query-plan-cache-max-size", "1024",
                    "db-orm-query-query-plan-cache-max-size-my-store", "64"));
            assertExternalConfig(Map.of(
                    "quarkus.hibernate-orm.query.query-plan-cache-max-size", "1024",
                    "quarkus.hibernate-orm.\"my-store\".query.query-plan-cache-max-size", "64"));
        });
    }

    @Test
    public void hibernateOrmOptionsUnsetLeaveTheQuarkusDefaults() {
        ConfigArgsConfigSource.setCliArgs("--db=postgres", "--db-kind-my-store=mariadb", "--db-jpa-packages-my-store=org.example.entities",
                "--db-kind-other-store=mariadb");
        initConfig();

        assertConfigNull("db-orm-query-query-plan-cache-max-size");
        assertConfigNull("db-orm-query-query-plan-cache-max-size-my-store");
        // the options have no default of their own, the Quarkus defaults apply (see QuarkusDefaultsTestConfigSource)
        assertExternalConfig(Map.of(
                "quarkus.hibernate-orm.query.query-plan-cache-max-size", QuarkusDefaultsTestConfigSource.QUERY_PLAN_CACHE_MAX_SIZE_DEFAULT,
                "quarkus.hibernate-orm.\"my-store\".query.query-plan-cache-max-size", QuarkusDefaultsTestConfigSource.QUERY_PLAN_CACHE_MAX_SIZE_DEFAULT));

        // an unset option is not advertised, neither for the default unit nor for a named unit, defined or not: Quarkus
        // defines a unit for every advertised name, and applies its own default anyway. A Keycloak default, such as the
        // slow query threshold, is advertised for the defined unit as before.
        Set<String> names = StreamSupport.stream(Configuration.getPropertyNames().spliterator(), false).collect(Collectors.toSet());
        assertFalse(names.toString(), names.contains("quarkus.hibernate-orm.query.query-plan-cache-max-size"));
        assertFalse(names.toString(), names.contains("quarkus.hibernate-orm.\"my-store\".query.query-plan-cache-max-size"));
        assertTrue(names.toString(), names.contains("quarkus.hibernate-orm.\"my-store\".log.queries-slower-than-ms"));
        assertTrue(names.toString(), names.stream().noneMatch(n -> n.startsWith("quarkus.hibernate-orm.\"other-store\"")));
    }

    private static final Map<String, String> RAW_HIBERNATE_PROPERTIES = Map.of(
            "quarkus.hibernate-orm.\"my-store\".log.sql", "true",
            "quarkus.hibernate-orm.\"my-store\".log.queries-slower-than-ms", "777",
            "quarkus.hibernate-orm.\"my-store\".database.default-schema", "raw",
            "quarkus.hibernate-orm.\"my-store\".dialect", "org.example.RawDialect",
            "quarkus.hibernate-orm.log.sql", "true",
            "quarkus.hibernate-orm.log.queries-slower-than-ms", "888",
            "quarkus.hibernate-orm.database.default-schema", "raw-default");

    private static void withRawHibernateProperties(Runnable test) {
        // the configuration reset of the harness restores the system properties, so they are set right before the test
        RAW_HIBERNATE_PROPERTIES.forEach(System::setProperty);
        try {
            test.run();
        } finally {
            RAW_HIBERNATE_PROPERTIES.keySet().forEach(System::clearProperty);
        }
    }

    @Test
    public void rawQuarkusPropertiesOfPersistenceUnits() {
        // Hibernate ORM properties Keycloak does not map are configured with the raw (unsupported) Quarkus properties,
        // and a raw property wins over the default of a Keycloak option
        withRawHibernateProperties(() -> {
            ConfigArgsConfigSource.setCliArgs("--db=postgres", "--db-kind-my-store=mariadb", "--db-jpa-packages-my-store=org.example.entities");
            initConfig();

            assertExternalConfig(Map.of(
                    "quarkus.hibernate-orm.\"my-store\".log.sql", "true",
                    "quarkus.hibernate-orm.\"my-store\".log.queries-slower-than-ms", "777",
                    "quarkus.hibernate-orm.\"my-store\".database.default-schema", "raw",
                    // db-dialect-my-store is derived from db-kind-my-store, so it is always set
                    "quarkus.hibernate-orm.\"my-store\".dialect", MariaDBDialect.class.getName(),
                    "quarkus.hibernate-orm.log.sql", "true",
                    "quarkus.hibernate-orm.log.queries-slower-than-ms", "888",
                    "quarkus.hibernate-orm.database.default-schema", "raw-default"));
        });
    }

    @Test
    public void keycloakOptionsWinOverRawQuarkusProperties() {
        withRawHibernateProperties(() -> {
            ConfigArgsConfigSource.setCliArgs("--db=postgres", "--db-schema=kc-default", "--db-log-slow-queries-threshold=5000",
                    "--db-kind-my-store=mariadb", "--db-jpa-packages-my-store=org.example.entities",
                    "--db-dialect-my-store=org.example.KcDialect", "--db-log-slow-queries-threshold-my-store=1234", "--db-schema-my-store=kc");
            initConfig();

            assertExternalConfig(Map.of(
                    "quarkus.hibernate-orm.\"my-store\".log.sql", "true",
                    "quarkus.hibernate-orm.\"my-store\".log.queries-slower-than-ms", "1234",
                    "quarkus.hibernate-orm.\"my-store\".database.default-schema", "kc",
                    "quarkus.hibernate-orm.\"my-store\".dialect", "org.example.KcDialect",
                    "quarkus.hibernate-orm.log.queries-slower-than-ms", "5000",
                    "quarkus.hibernate-orm.database.default-schema", "kc-default"));
        });
    }

    @Test
    public void rawQuarkusPropertiesDoNotDefinePersistenceUnitsOfOtherDatasources() {
        // a raw property of a datasource without a configured persistence unit is passed through untouched, and the
        // Keycloak options of that datasource still do not configure a persistence unit
        System.setProperty("quarkus.hibernate-orm.\"other-store\".log.sql", "true");
        try {
            ConfigArgsConfigSource.setCliArgs("--db-kind-other-store=mariadb", "--db-debug-jpql-other-store=true");
            initConfig();

            assertExternalConfig("quarkus.hibernate-orm.\"other-store\".log.sql", "true");
            assertExternalConfigNull("quarkus.hibernate-orm.\"other-store\".dialect");
            assertExternalConfigNull("quarkus.hibernate-orm.\"other-store\".unsupported-properties.\"hibernate.use_sql_comments\"");
        } finally {
            System.clearProperty("quarkus.hibernate-orm.\"other-store\".log.sql");
        }
    }

    @Test
    public void defaultPersistenceUnitHibernatePropertiesUnset() {
        ConfigArgsConfigSource.setCliArgs("--db=mariadb", "--db-debug-jpql=false");
        initConfig();

        assertExternalConfigNull("quarkus.hibernate-orm.database.default-schema");
        assertExternalConfigNull("quarkus.hibernate-orm.unsupported-properties.\"hibernate.use_sql_comments\"");
        assertExternalConfig("quarkus.hibernate-orm.log.queries-slower-than-ms", "10000");
    }

    @Test
    public void defaultPersistenceUnitHibernateProperties() {
        ConfigArgsConfigSource.setCliArgs("--db=mariadb", "--db-schema=other", "--db-debug-jpql=true", "--db-log-slow-queries-threshold=5000");
        initConfig();

        assertExternalConfig(Map.of(
                "quarkus.hibernate-orm.dialect", MariaDBDialect.class.getName(),
                "quarkus.hibernate-orm.database.default-schema", "other",
                "quarkus.hibernate-orm.unsupported-properties.\"hibernate.use_sql_comments\"", "true",
                "quarkus.hibernate-orm.log.queries-slower-than-ms", "5000",
                "quarkus.hibernate-orm.unsupported-properties.\"kc.query.deleteExpiredClientSessions[native]\"",
                JpaUtils.loadSpecificNamedQueries("mariadb").getProperty("deleteExpiredClientSessions[native]")));
    }
    @Test
    public void persistenceUnitMappersAreSyntheticDuplicatesOfTheOptions() {
        SmallRyeConfig config = createConfigFromCliArguments("--db=postgres", "--db-schema=other", "--db-log-slow-queries-threshold=5000",
                "--db-kind-my-store=mariadb", "--db-jpa-packages-my-store=org.example.entities", "--db-schema-my-store=named",
                "--db-log-slow-queries-threshold-my-store=1234");

        for (String option : List.of("db-dialect", "db-schema", "db-log-slow-queries-threshold")) {
            List<PropertyMapper<?>> mappers = PropertyMappers.getMappers(NS_KEYCLOAK_PREFIX + option);
            assertEquals(option, 1, mappers.size());
            assertFalse(option, mappers.get(0).getOption().isSynthetic());
            List<PropertyMapper<?>> namedMappers = PropertyMappers.getMappers(NS_KEYCLOAK_PREFIX + option + "-my-store");
            assertEquals(option, 2, namedMappers.size());
            assertEquals(option, 1, namedMappers.stream().filter(m -> m.getOption().isSynthetic()).count());
            assertFalse(option, PropertyMappers.getMapper(NS_KEYCLOAK_PREFIX + option + "-my-store").getOption().isSynthetic());
        }
        for (String property : List.of("quarkus.hibernate-orm.dialect", "quarkus.hibernate-orm.database.default-schema",
                "quarkus.hibernate-orm.log.queries-slower-than-ms")) {
            List<PropertyMapper<?>> mappers = PropertyMappers.getMappers(property);
            assertEquals(property, 1, mappers.size());
            assertTrue(property, mappers.get(0).getOption().isSynthetic());
        }

        assertEquals(PostgreSQLDialect.class.getName(), config.getConfigValue("quarkus.hibernate-orm.dialect").getValue());
        assertEquals("other", config.getConfigValue("quarkus.hibernate-orm.database.default-schema").getValue());
        assertEquals("5000", config.getConfigValue("quarkus.hibernate-orm.log.queries-slower-than-ms").getValue());
        assertEquals(MariaDBDialect.class.getName(), config.getConfigValue("quarkus.hibernate-orm.\"my-store\".dialect").getValue());
        assertEquals("named", config.getConfigValue("quarkus.hibernate-orm.\"my-store\".database.default-schema").getValue());
        assertEquals("1234", config.getConfigValue("quarkus.hibernate-orm.\"my-store\".log.queries-slower-than-ms").getValue());
    }

    @Test
    public void hibernateOrmRunTimeOptions() {
        // an option is a build time option like its property, see HibernateOrmProperties
        putEnvVar("KC_DB_ORM_LOG_SQL", "true");
        putEnvVar("KC_DB_ORM_LOG_SQL_MY_STORE", "true");
        putEnvVar("KC_DB_ORM_LOG_SQL_OTHER_STORE", "true");
        putEnvVar("KC_DB_ORM_FLUSH_MODE", "commit");
        ConfigArgsConfigSource.setCliArgs("--db=postgres", "--db-kind-my-store=mariadb", "--db-jpa-packages-my-store=org.example.entities",
                "--db-kind-other-store=mariadb");
        initConfig();

        PropertyMapper<?> mapper = PropertyMappers.getMapper("kc.db-orm-log-sql");
        assertTrue(mapper.isRunTime());
        assertFalse(mapper.getOption().isCli());
        assertEquals(Boolean.class, mapper.getType());
        assertTrue(mapper.getOption().getDefaultValue().isEmpty());
        assertEquals(OptionCategory.DATABASE, mapper.getCategory());
        assertTrue(PropertyMappers.getMapper("kc.db-orm-query-query-plan-cache-max-size").isBuildTime());
        assertEquals(Integer.class, PropertyMappers.getMapper("kc.db-orm-query-query-plan-cache-max-size").getType());
        assertEquals(OptionCategory.DATABASE_DATASOURCES, PropertyMappers.getMapper("kc.db-orm-log-sql-my-store").getCategory());

        assertConfig(Map.of(
                "db-orm-log-sql", "true",
                "db-orm-log-sql-my-store", "true",
                "db-orm-flush-mode", "commit"));
        assertExternalConfig(Map.of(
                "quarkus.hibernate-orm.log.sql", "true",
                "quarkus.hibernate-orm.\"my-store\".log.sql", "true",
                "quarkus.hibernate-orm.flush.mode", "commit"));
        // db-jpa-packages-other-store does not define a persistence unit for other-store
        assertExternalConfigNull("quarkus.hibernate-orm.\"other-store\".log.sql");
    }

    @Test
    public void hibernateOrmOptionsOfOverlappingProperties() {
        // a property name may be the prefix of another one
        putEnvVar("KC_DB_ORM_SCRIPTS_GENERATION", "create");
        putEnvVar("KC_DB_ORM_SCRIPTS_GENERATION_CREATE_TARGET", "/tmp/create.sql");
        putEnvVar("KC_DB_ORM_SCRIPTS_GENERATION_MY_STORE", "drop-and-create");
        putEnvVar("KC_DB_ORM_SCRIPTS_GENERATION_CREATE_TARGET_MY_STORE", "/tmp/create-my-store.sql");
        ConfigArgsConfigSource.setCliArgs("--db=postgres", "--db-kind-my-store=mariadb", "--db-jpa-packages-my-store=org.example.entities");
        initConfig();

        assertEquals("db-orm-scripts-generation-<datasource>", PropertyMappers.getMapper("kc.db-orm-scripts-generation-my-store").getOption().getKey());
        assertEquals("db-orm-scripts-generation-create-target-<datasource>", PropertyMappers.getMapper("kc.db-orm-scripts-generation-create-target-my-store").getOption().getKey());
        assertConfig(Map.of(
                "db-orm-scripts-generation", "create",
                "db-orm-scripts-generation-create-target", "/tmp/create.sql",
                "db-orm-scripts-generation-my-store", "drop-and-create",
                "db-orm-scripts-generation-create-target-my-store", "/tmp/create-my-store.sql"));
        assertExternalConfig(Map.of(
                "quarkus.hibernate-orm.scripts.generation", "create",
                "quarkus.hibernate-orm.scripts.generation.create-target", "/tmp/create.sql",
                "quarkus.hibernate-orm.\"my-store\".scripts.generation", "drop-and-create",
                "quarkus.hibernate-orm.\"my-store\".scripts.generation.create-target", "/tmp/create-my-store.sql"));
    }

    @Test
    public void hibernateOrmOptionsNotExposed() {
        ConfigArgsConfigSource.setCliArgs("--db=postgres", "--db-kind-my-store=mariadb", "--db-jpa-packages-my-store=org.example.entities");
        initConfig();

        // properties that Keycloak configures itself, properties mapped from other options, and properties that would
        // break Keycloak
        for (String option : List.of("db-orm-packages", "db-orm-datasource", "db-orm-persistence-xml-ignore", "db-orm-mapping-files",
                "db-orm-dialect", "db-orm-database-default-schema", "db-orm-log-queries-slower-than-ms",
                "db-orm-enabled", "db-orm-active", "db-orm-jdbc-enabled", "db-orm-reactive-enabled", "db-orm-multitenant",
                "db-orm-dev-ui-allow-hql", "db-orm-validate-in-dev-mode", "db-orm-sql-load-script", "db-orm-physical-naming-strategy",
                "db-orm-implicit-naming-strategy", "db-orm-quote-identifiers-strategy", "db-orm-schema-management-strategy",
                "db-orm-schema-management-create-schemas", "db-orm-schema-management-halt-on-error", "db-orm-schema-management-extra-physical-table-types")) {
            assertNull(option, PropertyMappers.getMapper(NS_KEYCLOAK_PREFIX + option));
            assertNull(option, PropertyMappers.getMapper(NS_KEYCLOAK_PREFIX + option + "-my-store"));
        }
        // a property of the extension as a whole has no named datasource variant
        assertEquals("quarkus.hibernate-orm.metrics.enabled", PropertyMappers.getMapper("kc.db-orm-metrics-enabled").getTo());
        assertTrue(PropertyMappers.getMapper("kc.db-orm-metrics-enabled").isBuildTime());
        assertNull(PropertyMappers.getMapper("kc.db-orm-metrics-enabled-my-store"));
        assertEquals("quarkus.hibernate-orm.request-scoped.enabled", PropertyMappers.getMapper("kc.db-orm-request-scoped-enabled").getTo());
        assertNull(PropertyMappers.getMapper("kc.db-orm-request-scoped-enabled-my-store"));

        // every db-orm-* option maps to a Hibernate ORM property, has no default and is not a command line option
        List<PropertyMapper<?>> mappers = Stream.concat(PropertyMappers.getMappers().stream(), PropertyMappers.getWildcardMappers().stream())
                .filter(m -> m.getOption().getKey().startsWith(DatabaseOptions.DB_ORM_PREFIX)).toList();
        assertThat(mappers.stream().map(m -> m.getOption().getKey()).toList(), hasItem("db-orm-jdbc-statement-batch-size"));
        assertThat(mappers.stream().map(m -> m.getOption().getKey()).toList(), hasItem("db-orm-jdbc-statement-batch-size-<datasource>"));
        assertThat(mappers.stream().map(m -> m.getOption().getKey()).toList(), hasItem("db-orm-log-sql-<datasource>"));
        for (PropertyMapper<?> mapper : mappers) {
            assertFalse(mapper.getOption().getKey(), mapper.getOption().isCli());
            assertTrue(mapper.getOption().getKey(), mapper.getOption().getDefaultValue().isEmpty());
            assertTrue(mapper.getTo(), mapper.getTo().startsWith("quarkus.hibernate-orm."));
            assertEquals(mapper.getOption().getKey(), mapper.getOption().getKey().endsWith("-<datasource>") ? OptionCategory.DATABASE_DATASOURCES : OptionCategory.DATABASE, mapper.getCategory());
        }
    }
}
