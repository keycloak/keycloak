/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.provider.quarkus;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import io.quarkus.hibernate.orm.runtime.dialect.QuarkusH2Dialect;
import io.smallrye.config.SmallRyeConfig;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.hibernate.dialect.MariaDBDialect;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.Config;
import org.keycloak.configuration.KeycloakConfigSourceProvider;
import org.keycloak.configuration.MicroProfileConfigProvider;

import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.SmallRyeConfigProviderResolver;

public class ConfigurationTest {

    private static final Properties SYSTEM_PROPERTIES = (Properties) System.getProperties().clone();
    private static final Map<String, String> ENVIRONMENT_VARIABLES = new HashMap<>(System.getenv());

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
        putEnvVar("KC_SPI_HOSTNAME_DEFAULT_FRONTEND_URL", "http://envvar.com");
        assertEquals("http://envvar.com", initConfig("hostname", "default").get("frontendUrl"));
    }

    @Test
    public void testSysPropPriorityOverEnvVar() {
        putEnvVar("KC_SPI_HOSTNAME_DEFAULT_FRONTEND_URL", "http://envvar.com");
        System.setProperty("kc.spi.hostname.default.frontend-url", "http://propvar.com");
        assertEquals("http://propvar.com", initConfig("hostname", "default").get("frontendUrl"));
    }

    @Test
    public void testCLIPriorityOverSysProp() {
        System.setProperty("kc.spi.hostname.default.frontend-url", "http://propvar.com");
        System.setProperty("kc.config.args", "--spi-hostname-default-frontend-url=http://cli.com");
        assertEquals("http://cli.com", initConfig("hostname", "default").get("frontendUrl"));
    }

    @Test
    public void testDefaultValueFromProperty() {
        System.setProperty("keycloak.frontendUrl", "http://defaultvalueprop.com");
        assertEquals("http://defaultvalueprop.com", initConfig("hostname", "default").get("frontendUrl"));
    }

    @Test
    public void testDefaultValue() {
        assertEquals("http://filepropdefault.com", initConfig("hostname", "default").get("frontendUrl"));
    }

    @Test
    public void testKeycloakProfilePropertySubstitution() {
        System.setProperty("kc.profile", "user-profile");
        assertEquals("http://filepropprofile.com", initConfig("hostname", "default").get("frontendUrl"));
    }

    @Test
    public void testQuarkusProfilePropertyStillWorks() {
        System.setProperty("quarkus.profile", "user-profile");
        assertEquals("http://filepropprofile.com", initConfig("hostname", "default").get("frontendUrl"));
    }

    @Test
    public void testCommandLineArguments() {
        System.setProperty("kc.config.args", "--spi-hostname-default-frontend-url=http://fromargs.com,--no-ssl");
        assertEquals("http://fromargs.com", initConfig("hostname", "default").get("frontendUrl"));
    }
    
    @Test
    public void testSpiConfigurationUsingCommandLineArguments() {
        System.setProperty("kc.config.args", "--spi-hostname-default-frontend-url=http://spifull.com");
        assertEquals("http://spifull.com", initConfig("hostname", "default").get("frontendUrl"));

        // test multi-word SPI names using camel cases
        System.setProperty("kc.config.args", "--spi-action-token-handler-verify-email-some-property=test");
        assertEquals("test", initConfig("action-token-handler", "verify-email").get("some-property"));
        System.setProperty("kc.config.args", "--spi-action-token-handler-verify-email-some-property=test");
        assertEquals("test", initConfig("actionTokenHandler", "verifyEmail").get("someProperty"));

        // test multi-word SPI names using slashes
        System.setProperty("kc.config.args", "--spi-client-registration-openid-connect-static-jwk-url=http://c.jwk.url");
        assertEquals("http://c.jwk.url", initConfig("client-registration", "openid-connect").get("static-jwk-url"));
    }

    @Test
    public void testPropertyMapping() {
        System.setProperty("kc.config.args", "--db=mariadb,--db-url=jdbc:mariadb://localhost/keycloak");
        SmallRyeConfig config = createConfig();
        assertEquals(MariaDBDialect.class.getName(), config.getConfigValue("quarkus.hibernate-orm.dialect").getValue());
        assertEquals("jdbc:mariadb://localhost/keycloak", config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
    }

    @Test
    public void testDatabaseUrlProperties() {
        System.setProperty("kc.config.args", "--db=mariadb,--db-url=jdbc:mariadb:aurora://foo/bar?a=1&b=2");
        SmallRyeConfig config = createConfig();
        assertEquals(MariaDBDialect.class.getName(), config.getConfigValue("quarkus.hibernate-orm.dialect").getValue());
        assertEquals("jdbc:mariadb:aurora://foo/bar?a=1&b=2", config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
    }

    @Test
    public void testDatabaseDefaults() {
        System.setProperty("kc.config.args", "--db=h2-file");
        SmallRyeConfig config = createConfig();
        assertEquals(QuarkusH2Dialect.class.getName(), config.getConfigValue("quarkus.hibernate-orm.dialect").getValue());
        assertEquals("jdbc:h2:file:~/data/keycloakdb;;AUTO_SERVER=TRUE", config.getConfigValue("quarkus.datasource.jdbc.url").getValue());

        System.setProperty("kc.config.args", "--db=h2-mem");
        config = createConfig();
        assertEquals(QuarkusH2Dialect.class.getName(), config.getConfigValue("quarkus.hibernate-orm.dialect").getValue());
        assertEquals("jdbc:h2:mem:keycloakdb", config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
        assertEquals("h2", config.getConfigValue("quarkus.datasource.db-kind").getValue());
    }

    @Test
    public void testDatabaseKindProperties() {
        System.setProperty("kc.config.args", "--db=postgres-10,--db-url=jdbc:postgresql://localhost/keycloak");
        SmallRyeConfig config = createConfig();
        assertEquals("io.quarkus.hibernate.orm.runtime.dialect.QuarkusPostgreSQL10Dialect",
            config.getConfigValue("quarkus.hibernate-orm.dialect").getValue());
        assertEquals("jdbc:postgresql://localhost/keycloak", config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
        assertEquals("postgresql", config.getConfigValue("quarkus.datasource.db-kind").getValue());
    }

    @Test
    public void testDatabaseProperties() {
        System.setProperty("kc.db.url.properties", ";;test=test;test1=test1");
        System.setProperty("kc.db.url.path", "test-dir");
        System.setProperty("kc.config.args", "--db=h2-file");
        SmallRyeConfig config = createConfig();
        assertEquals(QuarkusH2Dialect.class.getName(), config.getConfigValue("quarkus.hibernate-orm.dialect").getValue());
        assertEquals("jdbc:h2:file:test-dir" + File.separator + "data" + File.separator + "keycloakdb;;test=test;test1=test1", config.getConfigValue("quarkus.datasource.jdbc.url").getValue());

        System.setProperty("kc.db.url.properties", "?test=test&test1=test1");
        System.setProperty("kc.config.args", "--db=mariadb");
        config = createConfig();
        assertEquals("jdbc:mariadb://localhost/keycloak?test=test&test1=test1", config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
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
        Assert.assertEquals("cluster-default.xml", initConfig("connectionsInfinispan", "quarkus").get("configFile"));

        // If explicitly set, then it is always used regardless of the profile
        System.clearProperty("kc.profile");
        System.setProperty("kc.config.args", "--cluster=foo");

        Assert.assertEquals("cluster-foo.xml", initConfig("connectionsInfinispan", "quarkus").get("configFile"));
        System.setProperty("kc.profile", "dev");
        Assert.assertEquals("cluster-foo.xml", initConfig("connectionsInfinispan", "quarkus").get("configFile"));

        System.setProperty("kc.config.args", "--cluster-stack=foo");
        Assert.assertEquals("foo", initConfig("connectionsInfinispan", "quarkus").get("stack"));
    }

    private Config.Scope initConfig(String... scope) {
        Config.init(new MicroProfileConfigProvider(createConfig()));
        return Config.scope(scope);
    }

    private SmallRyeConfig createConfig() {
        KeycloakConfigSourceProvider.reload();
        return ConfigUtils.configBuilder(true, true).build();
    }
}
