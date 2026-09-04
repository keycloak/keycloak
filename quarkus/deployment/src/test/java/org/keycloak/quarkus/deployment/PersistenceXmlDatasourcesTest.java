package org.keycloak.quarkus.deployment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.keycloak.Config;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;

import io.smallrye.config.SmallRyeConfig;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.keycloak.quarkus.deployment.KeycloakProcessor.getDatasourceNameFromPersistenceXml;
import static org.keycloak.quarkus.deployment.KeycloakProcessor.getUserPersistenceUnitOverrides;
import static org.keycloak.quarkus.deployment.KeycloakProcessor.isResourceLocal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.wildfly.common.Assert.assertNotNull;

/**
 * Unit tests for the static persistence.xml helpers used by
 * {@link KeycloakProcessor#produceUserDefinedPersistenceUnits}: the datasource-name resolution order and the
 * RESOURCE_LOCAL rejection (Keycloak requires JTA for user persistence units).
 */
public class PersistenceXmlDatasourcesTest {
    private static final String PERSISTENCE_XML_BODY = """
            <persistence xmlns="https://jakarta.ee/xml/ns/persistence"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence https://jakarta.ee/xml/ns/persistence/persistence_3_0.xsd"
                         version="3.0">

                         %s

            </persistence>
            """;

    private static PersistenceXmlParser parser;

    @BeforeAll
    public static void setupParser() {
        parser = PersistenceXmlParser.create();
    }

    @Test
    public void datasourceNamesOrder() throws IOException {
        // use Jakarta property
        assertUsedName("""
                <persistence-unit name="user-store-pu" transaction-type="JTA">
                    <properties>
                        <property name="jakarta.persistence.jtaDataSource" value="user-store" />
                    </properties>
                </persistence-unit>
                """, "user-store");

        // use deprecated Hibernate property
        assertUsedName("""
                <persistence-unit name="user-store-pu" transaction-type="JTA">
                    <properties>
                        <property name="hibernate.connection.datasource" value="my-store" />
                    </properties>
                </persistence-unit>
                """, "my-store");

        // fall back to persistence unit name
        assertUsedName("""
                <persistence-unit name="user-store-pu" transaction-type="JTA">
                </persistence-unit>
                """, "user-store-pu");

        // prefer Jakarta property over the deprecated Hibernate one
        assertUsedName("""
                <persistence-unit name="user-store-pu" transaction-type="JTA">
                    <properties>
                        <property name="jakarta.persistence.jtaDataSource" value="user-store" />
                        <property name="hibernate.connection.datasource" value="my-store" />
                    </properties>
                </persistence-unit>
                """, "user-store");

        // nonJta datasource is not accepted, so the Hibernate property wins
        assertUsedName("""
                <persistence-unit name="user-store-pu" transaction-type="JTA">
                    <properties>
                        <property name="jakarta.persistence.nonJtaDataSource" value="user-store" />
                        <property name="hibernate.connection.datasource" value="my-store" />
                    </properties>
                </persistence-unit>
                """, "my-store");
    }

    @Test
    public void resourceLocalDetection() throws IOException {
        // explicit JTA -> not resource-local
        assertResourceLocal("""
                <persistence-unit name="user-store-pu" transaction-type="JTA">
                    <properties>
                        <property name="jakarta.persistence.jtaDataSource" value="user-store" />
                    </properties>
                </persistence-unit>
                """, false);

        // no transaction-type and only a datasource property -> parser defaults to RESOURCE_LOCAL -> rejected
        assertResourceLocal("""
                <persistence-unit name="user-store-pu">
                    <properties>
                        <property name="jakarta.persistence.jtaDataSource" value="user-store" />
                    </properties>
                </persistence-unit>
                """, true);

        // a <jta-data-source> element implies JTA -> not resource-local
        assertResourceLocal("""
                <persistence-unit name="user-store-pu">
                    <jta-data-source>JDBC/something</jta-data-source>
                    <properties>
                        <property name="jakarta.persistence.jtaDataSource" value="user-store" />
                    </properties>
                </persistence-unit>
                """, false);

        // transaction-type attribute RESOURCE_LOCAL -> rejected
        assertResourceLocal("""
                <persistence-unit name="user-store-pu" transaction-type="RESOURCE_LOCAL">
                    <properties>
                        <property name="jakarta.persistence.jtaDataSource" value="user-store" />
                    </properties>
                </persistence-unit>
                """, true);

        // jakarta.persistence.transactionType=RESOURCE_LOCAL property -> rejected
        assertResourceLocal("""
                <persistence-unit name="user-store-pu">
                    <jta-data-source>JDBC/something</jta-data-source>
                    <properties>
                        <property name="jakarta.persistence.jtaDataSource" value="user-store" />
                        <property name="jakarta.persistence.transactionType" value="RESOURCE_LOCAL" />
                    </properties>
                </persistence-unit>
                """, true);
    }

    @Test
    public void perUserPuKeycloakOptionsOverride() {
        // Per-named-datasource Keycloak options must be resolved and applied to the user persistence unit (they override
        // any equivalent persistence.xml value). This is the logic moved into configureStaticPersistenceUnitProperties.
        ConfigArgsConfigSource.setCliArgs("--db-kind-user-store=mariadb", "--db-dialect-user-store=org.hibernate.dialect.MariaDBDialect",
                "--db-schema-user-store=someSchema",
                "--db-debug-jpql-user-store=true", "--db-log-slow-queries-threshold-user-store=7500");
        initConfig();

        Map<String, String> overrides = getUserPersistenceUnitOverrides("user-store");
        assertEquals("org.hibernate.dialect.MariaDBDialect", overrides.get(AvailableSettings.DIALECT));
        assertEquals("someSchema", overrides.get(AvailableSettings.DEFAULT_SCHEMA));
        assertEquals("true", overrides.get(AvailableSettings.USE_SQL_COMMENTS));
        assertEquals("7500", overrides.get(AvailableSettings.LOG_SLOW_QUERY));
    }

    @Test
    public void dialectDerivedFromDbKindDoesNotOverridePersistenceXml() {
        ConfigArgsConfigSource.setCliArgs("--db-kind-user-store=mariadb");
        initConfig();

        Map<String, String> overrides = getUserPersistenceUnitOverrides("user-store");
        assertFalse(overrides.containsKey(AvailableSettings.DIALECT),
                "A db-kind-derived dialect must not be applied as an override; only an explicit --db-dialect-<ds> may");
    }

    @Test
    public void explicitDialectOverridesPersistenceXml() {
        ConfigArgsConfigSource.setCliArgs("--db-kind-user-store=mariadb",
                "--db-dialect-user-store=org.hibernate.dialect.MySQLDialect");
        initConfig();

        Map<String, String> overrides = getUserPersistenceUnitOverrides("user-store");
        assertEquals("org.hibernate.dialect.MySQLDialect", overrides.get(AvailableSettings.DIALECT));
    }

    private static void initConfig() {
        Config.init(new MicroProfileConfigProvider(createConfig()));
    }

    // inspired by AbstractConfigurationTest in quarkus/runtime
    private static SmallRyeConfig createConfig() {
        Configuration.resetConfig();
        Environment.getCurrentOrCreateFeatureProfile();
        return Configuration.getConfig();
    }

    private void assertUsedName(String content, String expectedName) throws IOException {
        assertSingle(content, descriptor -> assertThat(getDatasourceNameFromPersistenceXml(descriptor), is(expectedName)));
    }

    private void assertResourceLocal(String content, boolean expected) throws IOException {
        assertSingle(content, descriptor -> {
            if (expected) {
                assertTrue(isResourceLocal(descriptor));
            } else {
                assertFalse(isResourceLocal(descriptor));
            }
        });
    }

    private void assertSingle(String content, Consumer<PersistenceUnitDescriptor> asserts) throws IOException {
        String xml = PERSISTENCE_XML_BODY.formatted(content);
        Path file = null;
        try {
            file = Files.createTempFile("persistence", ".xml");
            Files.writeString(file, xml);
            List<PersistenceUnitDescriptor> descriptors = List.copyOf(parser.parse(List.of(file.toUri().toURL())).values());
            assertNotNull(descriptors);
            assertThat(descriptors.size(), is(1));
            asserts.accept(descriptors.get(0));
        } finally {
            if (file != null) {
                Files.deleteIfExists(file);
            }
        }
    }
}
