package org.keycloak.quarkus.deployment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.keycloak.Config;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.KeycloakConfigSourceProvider;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;

import io.smallrye.config.SmallRyeConfig;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.keycloak.quarkus.deployment.KeycloakProcessor.configurePersistenceUnitProperties;
import static org.keycloak.quarkus.deployment.KeycloakProcessor.getDatasourceNameFromPersistenceXml;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.wildfly.common.Assert.assertNotNull;

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
        var content = """
                <persistence-unit name="user-store-pu" transaction-type="JTA">
                    <properties>
                        <property name="jakarta.persistence.jtaDataSource" value="user-store" />
                    </properties>
                </persistence-unit>
                """;
        assertUsedName(content, "user-store");

        // use Hibernate property
        content = """
                <persistence-unit name="user-store-pu" transaction-type="JTA">
                    <properties>
                        <property name="hibernate.connection.datasource" value="my-store" />
                    </properties>
                </persistence-unit>
                """;
        assertUsedName(content, "my-store");

        // use persistence unit name
        content = """
                <persistence-unit name="user-store-pu" transaction-type="JTA">
                </persistence-unit>
                """;
        assertUsedName(content, "user-store-pu");

        // prefer Jakarta property
        content = """
                <persistence-unit name="user-store-pu" transaction-type="JTA">
                    <properties>
                        <property name="jakarta.persistence.jtaDataSource" value="user-store" />
                        <property name="hibernate.connection.datasource" value="my-store" />
                    </properties>
                </persistence-unit>
                """;
        assertUsedName(content, "user-store");

        // prefer Hibernate property as not accepting nonJta datasource
        content = """
                <persistence-unit name="user-store-pu" transaction-type="JTA">
                    <properties>
                        <property name="jakarta.persistence.nonJtaDataSource" value="user-store" />
                        <property name="hibernate.connection.datasource" value="my-store" />
                    </properties>
                </persistence-unit>
                """;
        assertUsedName(content, "my-store");
    }

    @Test
    public void transactionTypes() throws IOException {
        // not specified transaction-type -> error
        var content = """
                <persistence-unit name="user-store-pu">
                    <properties>
                        <property name="jakarta.persistence.jtaDataSource" value="user-store" />
                    </properties>
                </persistence-unit>
                """;
        assertPersistenceXmlSingleDS(content, descriptor -> {
            var exception = assertThrows(IllegalArgumentException.class, () -> configurePersistenceUnitProperties("user-store", descriptor));
            assertThat(exception.getMessage(), is("You need to use 'JTA' transaction type in your persistence.xml file."));
        });

        // jta data source is specified, so the tx type is JTA by default -> ok
        content = """
                <persistence-unit name="user-store-pu">
                    <jta-data-source>JDBC/something</jta-data-source>
                    <properties>
                        <property name="jakarta.persistence.jtaDataSource" value="user-store" />
                    </properties>
                </persistence-unit>
                """;
        assertPersistenceXmlSingleDS(content, descriptor -> {
            assertDoesNotThrow(() -> configurePersistenceUnitProperties("user-store", descriptor));
        });

        // tx type is set to RESOURCE_LOCAL -> error
        content = """
                <persistence-unit name="user-store-pu" transaction-type="RESOURCE_LOCAL">
                    <properties>
                        <property name="jakarta.persistence.jtaDataSource" value="user-store" />
                    </properties>
                </persistence-unit>
                """;
        assertPersistenceXmlSingleDS(content, descriptor -> {
            var exception = assertThrows(IllegalArgumentException.class, () -> configurePersistenceUnitProperties("user-store", descriptor));
            assertThat(exception.getMessage(), is("You need to use 'JTA' transaction type in your persistence.xml file."));
        });

        // Jakarta TX prop is set to RESOURCE_LOCAL -> error
        content = """
                <persistence-unit name="user-store-pu">
                    <jta-data-source>JDBC/something</jta-data-source>
                    <properties>
                        <property name="jakarta.persistence.jtaDataSource" value="user-store" />
                        <property name="jakarta.persistence.transactionType" value="RESOURCE_LOCAL" />
                    </properties>
                </persistence-unit>
                """;
        assertPersistenceXmlSingleDS(content, descriptor -> {
            var exception = assertThrows(IllegalArgumentException.class, () -> configurePersistenceUnitProperties("user-store", descriptor));
            assertThat(exception.getMessage(), is("You need to use 'JTA' transaction type in your persistence.xml file."));
        });

        // Everything is correct, we can check if the Jakarta prop is automatically set -> ok
        content = """
                <persistence-unit name="user-store-pu" transaction-type="JTA">
                    <properties>
                        <property name="jakarta.persistence.jtaDataSource" value="user-store" />
                    </properties>
                </persistence-unit>
                """;
        assertPersistenceXmlSingleDS(content, descriptor -> {
            configurePersistenceUnitProperties("user-store", descriptor);
            assertThat(descriptor.getProperties().getProperty(AvailableSettings.JAKARTA_TRANSACTION_TYPE), is("JTA"));
        });
    }

    @Test
    public void sqlProperties() throws IOException {
        ConfigArgsConfigSource.setCliArgs("--db-kind-user-store=dev-mem", "--db-debug-jpql-user-store=true", "--db-log-slow-queries-threshold-user-store=7500");
        initConfig();

        var content = """
                <persistence-unit name="user-store-pu" transaction-type="JTA">
                    <properties>
                        <property name="jakarta.persistence.jtaDataSource" value="user-store" />
                    </properties>
                </persistence-unit>
                """;
        assertPersistenceXmlSingleDS(content, descriptor -> {
            configurePersistenceUnitProperties("user-store", descriptor);
            var properties = descriptor.getProperties();

            assertThat(properties.getProperty(AvailableSettings.USE_SQL_COMMENTS), is("true"));
            assertThat(properties.getProperty(AvailableSettings.LOG_SLOW_QUERY), is("7500"));
        });
    }

    @Test
    public void dialectAndSchema() throws IOException {
        ConfigArgsConfigSource.setCliArgs("--db-kind-user-store=mariadb");
        initConfig();

        var content = """
                <persistence-unit name="user-store-pu" transaction-type="JTA">
                    <properties>
                        <property name="jakarta.persistence.jtaDataSource" value="user-store" />
                    </properties>
                </persistence-unit>
                """;
        assertPersistenceXmlSingleDS(content, descriptor -> {
            configurePersistenceUnitProperties("user-store", descriptor);
            var properties = descriptor.getProperties();
            assertThat(properties.getProperty(AvailableSettings.DIALECT), is("org.hibernate.dialect.MariaDBDialect"));
            assertThat(properties.getProperty(AvailableSettings.DEFAULT_SCHEMA), is(nullValue()));
        });

        ConfigArgsConfigSource.setCliArgs("--db-kind-user-store=mariadb", "--db-schema-user-store=someSchema");
        initConfig();

        assertPersistenceXmlSingleDS(content, descriptor -> {
            configurePersistenceUnitProperties("user-store", descriptor);
            var properties = descriptor.getProperties();
            assertThat(properties.getProperty(AvailableSettings.DIALECT), is("org.hibernate.dialect.MariaDBDialect"));
            assertThat(properties.getProperty(AvailableSettings.DEFAULT_SCHEMA), is("someSchema"));
        });
    }

    private static void initConfig(){
        Config.init(new MicroProfileConfigProvider(createConfig()));
    }

    // inspired in AbstractConfigurationTest in quarkus/runtime
    private static SmallRyeConfig createConfig() {
        Configuration.resetConfig();
        KeycloakConfigSourceProvider.reload();
        Environment.getCurrentOrCreateFeatureProfile();
        return Configuration.getConfig();
    }

    private void assertUsedName(String content, String expectedName) throws IOException {
        assertPersistenceXmlSingleDS(content, descriptor -> {
            var name = getDatasourceNameFromPersistenceXml(descriptor);
            assertThat(name, is(expectedName));
            configurePersistenceUnitProperties(name, descriptor);
            var properties = descriptor.getProperties();
            assertNotNull(properties);
            assertThat(properties.getProperty(JdbcSettings.JAKARTA_JTA_DATASOURCE), is(expectedName));
            assertThat(properties.getProperty(AvailableSettings.DATASOURCE), is(expectedName));
        });
    }

    private void assertPersistenceXmlSingleDS(String content, Consumer<ParsedPersistenceXmlDescriptor> asserts) throws IOException {
        assertPersistenceXml(content, descriptors -> {
            assertNotNull(descriptors);
            assertThat(descriptors.size(), is(1));
            var descriptor = descriptors.get(0);
            assertNotNull(descriptor);
            asserts.accept(descriptor);
        });
    }

    private void assertPersistenceXml(String content, Consumer<List<ParsedPersistenceXmlDescriptor>> asserts) throws IOException {
        String finalPersistenceXmlFileContent = PERSISTENCE_XML_BODY.formatted(content);
        Path persistenceXmlFile = null;
        try {
            persistenceXmlFile = Files.createTempFile("persistence", ".xml");
            Files.writeString(persistenceXmlFile, finalPersistenceXmlFileContent);
            asserts.accept(parser.parse(List.of(persistenceXmlFile.toUri().toURL())).values().stream().map(f -> (ParsedPersistenceXmlDescriptor) f).toList());
        } finally {
            if (persistenceXmlFile != null) {
                Files.deleteIfExists(persistenceXmlFile);
            }
        }
    }
}
