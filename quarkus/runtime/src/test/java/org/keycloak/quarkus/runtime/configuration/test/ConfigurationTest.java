/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.quarkus.runtime.configuration.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource.CLI_ARGS;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import io.quarkus.hibernate.orm.runtime.dialect.QuarkusH2Dialect;
import io.quarkus.hibernate.orm.runtime.dialect.QuarkusPostgreSQL10Dialect;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.config.SmallRyeConfig;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.hibernate.dialect.MariaDBDialect;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.Config;
import org.keycloak.quarkus.runtime.configuration.KeycloakConfigSourceProvider;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;

import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.SmallRyeConfigProviderResolver;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.vault.FilesPlainTextVaultProviderFactory;
import org.mariadb.jdbc.MySQLDataSource;
import org.postgresql.xa.PGXADataSource;

public class ConfigurationTest {

    private static final Properties SYSTEM_PROPERTIES = (Properties) System.getProperties().clone();
    private static final Map<String, String> ENVIRONMENT_VARIABLES = new HashMap<>(System.getenv());
    private static final String ARG_SEPARATOR = ";;";

    @SuppressWarnings("unchecked")
    public static void putEnvVar(String name, String value) {
        Map<String, String> env = System.getenv();
        Field field = null;
        try {
            field = env.getClass().getDeclaredField("m");
            field.setAccessible(true);
            ((Map<String, String>) field.get(env)).put(name, value);
        } catch (Exception cause) {
            throw new RuntimeException("Failed to update environment variables", cause);
        } finally {
            if (field != null) {
                field.setAccessible(false);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void removeEnvVar(String name) {
        Map<String, String> env = System.getenv();
        Field field = null;
        try {
            field = env.getClass().getDeclaredField("m");
            field.setAccessible(true);
            ((Map<String, String>) field.get(env)).remove(name);
        } catch (Exception cause) {
            throw new RuntimeException("Failed to update environment variables", cause);
        } finally {
            if (field != null) {
                field.setAccessible(false);
            }
        }
    }

    @After
    public void onAfter() {
        Properties current = System.getProperties();

        for (String name : current.stringPropertyNames()) {
            if (!SYSTEM_PROPERTIES.containsKey(name)) {
                current.remove(name);
            }
        }

        for (String name : new HashMap<>(System.getenv()).keySet()) {
            if (!ENVIRONMENT_VARIABLES.containsKey(name)) {
                removeEnvVar(name);
            }
        }

        SmallRyeConfigProviderResolver.class.cast(ConfigProviderResolver.instance()).releaseConfig(ConfigProvider.getConfig());
    }

    @Test
    public void testCamelCase() {
        putEnvVar("KC_SPI_CAMEL_CASE_SCOPE_CAMEL_CASE_PROP", "foobar");
        initConfig();
        String value = Config.scope("camelCaseScope").get("camelCaseProp");
        assertEquals(value, "foobar");
    }

    @Test
    public void testEnvVarPriorityOverPropertiesFile() {
        putEnvVar("KC_SPI_HOSTNAME_DEFAULT_FRONTEND_URL", "http://envvar.unittest");
        assertEquals("http://envvar.unittest", initConfig("hostname", "default").get("frontendUrl"));
    }

    @Test
    public void testEnvVarAvailableFromPropertyNames() {
        putEnvVar("KC_VAULT_DIR", "/foo/bar");
        Config.Scope config = initConfig("vault", FilesPlainTextVaultProviderFactory.ID);
        assertEquals("/foo/bar", config.get("dir"));
        assertTrue(config.getPropertyNames()
                .contains("kc.spi-vault-".concat(FilesPlainTextVaultProviderFactory.ID).concat("-dir")));
    }

    @Test
    public void testEnvVarPriorityOverSysProps() {
        putEnvVar("KC_SPI_HOSTNAME_DEFAULT_FRONTEND_URL", "http://envvar.unittest");
        System.setProperty("kc.spi-hostname-default-frontend-url", "http://propvar.unittest");
        assertEquals("http://envvar.unittest", initConfig("hostname", "default").get("frontendUrl"));
    }

    @Test
    public void testCLIPriorityOverSysProp() {
        System.setProperty("kc.spi.hostname.default.frontend-url", "http://propvar.unittest");
        System.setProperty(CLI_ARGS, "--spi-hostname-default-frontend-url=http://cli.unittest");
        assertEquals("http://cli.unittest", initConfig("hostname", "default").get("frontendUrl"));
    }

    @Test
    public void testDefaultValueFromProperty() {
        System.setProperty("keycloak.frontendUrl", "http://defaultvalueprop.unittest");
        assertEquals("http://defaultvalueprop.unittest", initConfig("hostname", "default").get("frontendUrl"));
    }

    @Test
    public void testDefaultValue() {
        assertEquals("http://filepropdefault.unittest", initConfig("hostname", "default").get("frontendUrl"));
    }

    @Test
    public void testKeycloakProfilePropertySubstitution() {
        System.setProperty(Environment.PROFILE, "user-profile");
        assertEquals("http://filepropprofile.unittest", initConfig("hostname", "default").get("frontendUrl"));
    }

    @Test
    public void testQuarkusProfilePropertyStillWorks() {
        System.setProperty("quarkus.profile", "user-profile");
        assertEquals("http://filepropprofile.unittest", initConfig("hostname", "default").get("frontendUrl"));
    }

    @Test
    public void testCommandLineArguments() {
        System.setProperty(CLI_ARGS, "--spi-hostname-default-frontend-url=http://fromargs.unittest" + ARG_SEPARATOR + "--no-ssl");
        assertEquals("http://fromargs.unittest", initConfig("hostname", "default").get("frontendUrl"));
    }
    
    @Test
    public void testSpiConfigurationUsingCommandLineArguments() {
        System.setProperty(CLI_ARGS, "--spi-hostname-default-frontend-url=http://spifull.unittest");
        assertEquals("http://spifull.unittest", initConfig("hostname", "default").get("frontendUrl"));

        // test multi-word SPI names using camel cases
        System.setProperty(CLI_ARGS, "--spi-action-token-handler-verify-email-some-property=test");
        assertEquals("test", initConfig("action-token-handler", "verify-email").get("some-property"));
        System.setProperty(CLI_ARGS, "--spi-action-token-handler-verify-email-some-property=test");
        assertEquals("test", initConfig("actionTokenHandler", "verifyEmail").get("someProperty"));

        // test multi-word SPI names using slashes
        System.setProperty(CLI_ARGS, "--spi-client-registration-openid-connect-static-jwk-url=http://c.jwk.url");
        assertEquals("http://c.jwk.url", initConfig("client-registration", "openid-connect").get("static-jwk-url"));
    }

    @Test
    public void testPropertyNamesFromConfig() {
        System.setProperty(CLI_ARGS, "--spi-client-registration-openid-connect-static-jwk-url=http://c.jwk.url");
        Config.Scope config = initConfig("client-registration", "openid-connect");
        assertEquals(1, config.getPropertyNames().size());
        assertEquals("http://c.jwk.url", config.get("static-jwk-url"));

        System.setProperty(CLI_ARGS, "--vault-dir=secrets");
        config = initConfig("vault", FilesPlainTextVaultProviderFactory.ID);
        assertEquals(1, config.getPropertyNames().size());
        assertEquals("secrets", config.get("dir"));

        System.getProperties().remove(CLI_ARGS);
        System.setProperty("kc.spi-client-registration-openid-connect-static-jwk-url", "http://c.jwk.url");
        config = initConfig("client-registration", "openid-connect");
        assertEquals(1, config.getPropertyNames().size());
        assertEquals("http://c.jwk.url", config.get("static-jwk-url"));

        System.getProperties().remove(CLI_ARGS);
        System.getProperties().remove("kc.spi-client-registration-openid-connect-static-jwk-url");
        putEnvVar("KC_SPI_CLIENT_REGISTRATION_OPENID_CONNECT_STATIC_JWK_URL", "http://c.jwk.url/from-env");
        config = initConfig("client-registration", "openid-connect");
        assertEquals(1, config.getPropertyNames().size());
        assertEquals("http://c.jwk.url/from-env", config.get("static-jwk-url"));
    }


    @Test
    public void testPropertyMapping() {
        System.setProperty(CLI_ARGS, "--db=mariadb" + ARG_SEPARATOR + "--db-url=jdbc:mariadb://localhost/keycloak");
        SmallRyeConfig config = createConfig();
        assertEquals(MariaDBDialect.class.getName(), config.getConfigValue("quarkus.hibernate-orm.dialect").getValue());
        assertEquals("jdbc:mariadb://localhost/keycloak", config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
    }

    @Test
    public void testDatabaseUrlProperties() {
        System.setProperty(CLI_ARGS, "--db=mariadb" + ARG_SEPARATOR + "--db-url=jdbc:mariadb:aurora://foo/bar?a=1&b=2");
        SmallRyeConfig config = createConfig();
        assertEquals(MariaDBDialect.class.getName(), config.getConfigValue("quarkus.hibernate-orm.dialect").getValue());
        assertEquals("jdbc:mariadb:aurora://foo/bar?a=1&b=2", config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
    }

    @Test
    public void testDatabaseDefaults() {
        System.setProperty(CLI_ARGS, "--db=dev-file");
        SmallRyeConfig config = createConfig();
        assertEquals(QuarkusH2Dialect.class.getName(), config.getConfigValue("quarkus.hibernate-orm.dialect").getValue());
        assertEquals("jdbc:h2:file:~/data/h2/keycloakdb;;AUTO_SERVER=TRUE", config.getConfigValue("quarkus.datasource.jdbc.url").getValue());

        System.setProperty(CLI_ARGS, "--db=dev-mem");
        config = createConfig();
        assertEquals(QuarkusH2Dialect.class.getName(), config.getConfigValue("quarkus.hibernate-orm.dialect").getValue());
        assertEquals("jdbc:h2:mem:keycloakdb", config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
        assertEquals("h2", config.getConfigValue("quarkus.datasource.db-kind").getValue());

        System.setProperty(CLI_ARGS, "--db=dev-mem" + ARG_SEPARATOR + "--db-username=other");
        config = createConfig();
        assertEquals("sa", config.getConfigValue("quarkus.datasource.username").getValue());

        System.setProperty(CLI_ARGS, "--db=postgres" + ARG_SEPARATOR + "--db-username=other");
        config = createConfig();
        assertEquals("other", config.getConfigValue("quarkus.datasource.username").getValue());

        System.setProperty(CLI_ARGS, "--db=postgres");
        config = createConfig();
        assertEquals(null, config.getConfigValue("quarkus.datasource.username").getValue());
    }

    @Test
    public void testDatabaseKindProperties() {
        System.setProperty(CLI_ARGS, "--db=postgres" + ARG_SEPARATOR + "--db-url=jdbc:postgresql://localhost/keycloak");
        SmallRyeConfig config = createConfig();
        assertEquals("io.quarkus.hibernate.orm.runtime.dialect.QuarkusPostgreSQL10Dialect",
            config.getConfigValue("quarkus.hibernate-orm.dialect").getValue());
        assertEquals("jdbc:postgresql://localhost/keycloak", config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
        assertEquals("postgresql", config.getConfigValue("quarkus.datasource.db-kind").getValue());
    }

    @Test
    public void testDefaultDbPropertiesGetApplied() {
        System.setProperty(CLI_ARGS, "--db=postgres" + ARG_SEPARATOR + "--db-url-host=myhost" + ARG_SEPARATOR + "--db-url-database=kcdb" + ARG_SEPARATOR + "--db-url-properties=?foo=bar");
        SmallRyeConfig config = createConfig();
        assertEquals("io.quarkus.hibernate.orm.runtime.dialect.QuarkusPostgreSQL10Dialect",
                config.getConfigValue("quarkus.hibernate-orm.dialect").getValue());
        assertEquals("jdbc:postgresql://myhost/kcdb?foo=bar", config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
        assertEquals("postgresql", config.getConfigValue("quarkus.datasource.db-kind").getValue());
    }

    @Test
    public void testSetDbUrlOverridesDefaultDataSource() {
        System.setProperty(CLI_ARGS, "--db=mariadb" + ARG_SEPARATOR + "--db-url-host=myhost" + ARG_SEPARATOR + "--db-url=jdbc:mariadb://localhost/keycloak");
        SmallRyeConfig config = createConfig();
        assertEquals("org.hibernate.dialect.MariaDBDialect",
                config.getConfigValue("quarkus.hibernate-orm.dialect").getValue());
        assertEquals("jdbc:mariadb://localhost/keycloak", config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
        assertEquals("mariadb", config.getConfigValue("quarkus.datasource.db-kind").getValue());
    }

    @Test
    public void testDatabaseProperties() {
        System.setProperty("kc.db-url-properties", ";;test=test;test1=test1");
        System.setProperty("kc.db-url-path", "test-dir");
        System.setProperty(CLI_ARGS, "--db=dev-file");
        SmallRyeConfig config = createConfig();
        assertEquals(QuarkusH2Dialect.class.getName(), config.getConfigValue("quarkus.hibernate-orm.dialect").getValue());
        assertEquals("jdbc:h2:file:test-dir" + File.separator + "data" + File.separator + "h2" + File.separator + "keycloakdb;;test=test;test1=test1", config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
        assertEquals("xa", config.getConfigValue("quarkus.datasource.jdbc.transactions").getValue());

        System.setProperty(CLI_ARGS, "");
        config = createConfig();
        assertEquals(QuarkusH2Dialect.class.getName(), config.getConfigValue("quarkus.hibernate-orm.dialect").getValue());
        assertEquals("jdbc:h2:file:test-dir" + File.separator + "data" + File.separator + "h2" + File.separator + "keycloakdb;;test=test;test1=test1", config.getConfigValue("quarkus.datasource.jdbc.url").getValue());

        System.setProperty("kc.db-url-properties", "?test=test&test1=test1");
        System.setProperty(CLI_ARGS, "--db=mariadb");
        config = createConfig();
        assertEquals("jdbc:mariadb://localhost/keycloak?test=test&test1=test1", config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
        assertEquals(MariaDBDialect.class.getName(), config.getConfigValue("quarkus.hibernate-orm.dialect").getValue());
        assertEquals(MySQLDataSource.class.getName(), config.getConfigValue("quarkus.datasource.jdbc.driver").getValue());

        System.setProperty(CLI_ARGS, "--db=postgres");
        config = createConfig();
        assertEquals("jdbc:postgresql://localhost/keycloak?test=test&test1=test1", config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
        assertEquals(QuarkusPostgreSQL10Dialect.class.getName(), config.getConfigValue("quarkus.hibernate-orm.dialect").getValue());
        assertEquals(PGXADataSource.class.getName(), config.getConfigValue("quarkus.datasource.jdbc.driver").getValue());

        System.setProperty(CLI_ARGS, "--db-schema=test-schema");
        config = createConfig();
        assertEquals("test-schema", config.getConfigValue("kc.db-schema").getValue());
        assertEquals("test-schema", config.getConfigValue("quarkus.hibernate-orm.database.default-schema").getValue());
    }

    // KEYCLOAK-15632
    @Test
    public void testNestedDatabaseProperties() {
        System.setProperty("kc.home.dir", "/tmp/kc/bin/../");
        SmallRyeConfig config = createConfig();
        assertEquals("jdbc:h2:file:/tmp/kc/bin/..//data/keycloakdb", config.getConfigValue("quarkus.datasource.foo").getValue());

        Assert.assertEquals("foo-def-suffix", config.getConfigValue("quarkus.datasource.bar").getValue());

        System.setProperty("kc.prop5", "val5");
        config = createConfig();
        Assert.assertEquals("foo-val5-suffix", config.getConfigValue("quarkus.datasource.bar").getValue());

        System.setProperty("kc.prop4", "val4");
        config = createConfig();
        Assert.assertEquals("foo-val4", config.getConfigValue("quarkus.datasource.bar").getValue());

        System.setProperty("kc.prop3", "val3");
        config = createConfig();
        Assert.assertEquals("foo-val3", config.getConfigValue("quarkus.datasource.bar").getValue());
    }

    @Test
    public void testClusterConfig() {
        // Cluster enabled by default, but disabled for the "dev" profile
        Assert.assertEquals("cache-ispn.xml", initConfig("connectionsInfinispan", "quarkus").get("configFile"));

        // If explicitly set, then it is always used regardless of the profile
        System.clearProperty(Environment.PROFILE);
        System.setProperty(CLI_ARGS, "--cache=cluster-foo.xml");

        Assert.assertEquals("cluster-foo.xml", initConfig("connectionsInfinispan", "quarkus").get("configFile"));
        System.setProperty(Environment.PROFILE, "dev");
        Assert.assertEquals("cluster-foo.xml", initConfig("connectionsInfinispan", "quarkus").get("configFile"));

        System.setProperty(CLI_ARGS, "--cache-stack=foo");
        Assert.assertEquals("foo", initConfig("connectionsInfinispan", "quarkus").get("stack"));
    }

    @Test
    public void testCommaSeparatedArgValues() {
        System.setProperty(CLI_ARGS, "--spi-client-jpa-searchable-attributes=bar,foo");
        assertEquals("bar,foo", initConfig("client-jpa").get("searchable-attributes"));

        System.setProperty(CLI_ARGS, "--spi-client-jpa-searchable-attributes=bar,foo,foo bar");
        assertEquals("bar,foo,foo bar", initConfig("client-jpa").get("searchable-attributes"));

        System.setProperty(CLI_ARGS, "--spi-client-jpa-searchable-attributes=bar,foo, \"foo bar\"");
        assertEquals("bar,foo, \"foo bar\"", initConfig("client-jpa").get("searchable-attributes"));

        System.setProperty(CLI_ARGS, "--spi-client-jpa-searchable-attributes=bar,foo, \"foo bar\"" + ARG_SEPARATOR + "--spi-hostname-default-frontend-url=http://foo.unittest");
        assertEquals("http://foo.unittest", initConfig("hostname-default").get("frontend-url"));
    }

    @Test
    public void testDatabaseDriverSetExplicitly() {
        System.setProperty(CLI_ARGS, "--db=mssql" + ARG_SEPARATOR + "--db-url=jdbc:sqlserver://localhost/keycloak");
        System.setProperty("kc.db-driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        System.setProperty("kc.db-tx-type", "enabled");
        assertTrue(System.getProperty(CLI_ARGS, "").contains("mssql"));
        SmallRyeConfig config = createConfig();
        assertEquals("jdbc:sqlserver://localhost/keycloak", config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
        assertEquals("mssql", config.getConfigValue("quarkus.datasource.db-kind").getValue());
        assertEquals("com.microsoft.sqlserver.jdbc.SQLServerDriver", config.getConfigValue("quarkus.datasource.jdbc.driver").getValue());
        assertEquals("enabled", config.getConfigValue("quarkus.datasource.jdbc.transactions").getValue());
    }

    @Test
    public void testResolveMetricsOption() {
        System.setProperty(CLI_ARGS, "--metrics-enabled=true");
        SmallRyeConfig config = createConfig();
        assertEquals("true", config.getConfigValue("quarkus.datasource.metrics.enabled").getValue());
    }

    @Test
    public void testOptionValueWithEqualSign() {
        System.setProperty(CLI_ARGS, "--db-password=my_secret=");
        SmallRyeConfig config = createConfig();
        assertEquals("my_secret=", config.getConfigValue("kc.db-password").getValue());
    }

    @Test
    public void testResolvePropertyFromDefaultProfile() {
        Environment.setProfile("import_export");
        assertEquals("false", createConfig().getConfigValue("kc.hostname-strict").getValue());

        Environment.setProfile("prod");
        assertEquals("true", createConfig().getConfigValue("kc.hostname-strict").getValue());
    }

    private Config.Scope initConfig(String... scope) {
        Config.init(new MicroProfileConfigProvider(createConfig()));
        return Config.scope(scope);
    }

    private SmallRyeConfig createConfig() {
        KeycloakConfigSourceProvider.reload();
        return ConfigUtils.configBuilder(true, LaunchMode.NORMAL).build();
    }
}
