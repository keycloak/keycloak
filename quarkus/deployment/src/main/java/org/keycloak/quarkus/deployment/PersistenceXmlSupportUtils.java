package org.keycloak.quarkus.deployment;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.PersistenceXmlDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import jakarta.persistence.Entity;
import jakarta.persistence.PersistenceUnitTransactionType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;
import org.keycloak.config.DatabaseOptions;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.quarkus.runtime.KeycloakRecorder;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.storage.database.jpa.NamedJpaConnectionProviderFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static org.keycloak.connections.jpa.util.JpaUtils.loadSpecificNamedQueries;
import static org.keycloak.quarkus.deployment.KeycloakProcessor.getDefaultDataSource;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getOptionalBooleanKcValue;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getOptionalKcValue;
import static org.keycloak.quarkus.runtime.storage.database.jpa.QuarkusJpaConnectionProviderFactory.QUERY_PROPERTY_PREFIX;

/**
 * Util class for persistence.xml support. Once the support is removed, this class can be removed as well.
 */
public class PersistenceXmlSupportUtils {
    private static Logger logger = Logger.getLogger(PersistenceXmlSupportUtils.class);

    static Optional<PersistenceUnitDescriptor> getDefaultPersistenceUnit() {
        PersistenceXmlParser parser = PersistenceXmlParser.create();
        return parser.parse(Collections.singletonList(parser.getClassLoaderService().locateResource("default-persistence.xml")))
                .values()
                .stream()
                .findAny();
    }

    static void configurePersistenceUnits(HibernateOrmConfig config,
                                          List<PersistenceXmlDescriptorBuildItem> descriptors,
                                          List<JdbcDataSourceBuildItem> jdbcDataSources,
                                          CombinedIndexBuildItem indexBuildItem,
                                          BuildProducer<HibernateOrmIntegrationRuntimeConfiguredBuildItem> runtimeConfigured,
                                          KeycloakRecorder recorder) {
        ParsedPersistenceXmlDescriptor defaultUnitDescriptor = null;
        List<String> userManagedEntities = new ArrayList<>();

        for (PersistenceXmlDescriptorBuildItem item : descriptors) {
            ParsedPersistenceXmlDescriptor descriptor = (ParsedPersistenceXmlDescriptor) item.getDescriptor();

            if ("keycloak-default".equals(descriptor.getName())) {
                defaultUnitDescriptor = descriptor;
                configureDefaultPersistenceUnitProperties(defaultUnitDescriptor, config, getDefaultDataSource(jdbcDataSources));
                runtimeConfigured.produce(new HibernateOrmIntegrationRuntimeConfiguredBuildItem("keycloak", defaultUnitDescriptor.getName())
                        .setInitListener(recorder.createDefaultUnitListener()));
            } else {
                String datasourceName = getDatasourceNameFromPersistenceXml(descriptor);
                configurePersistenceUnitProperties(datasourceName, descriptor);
                // register a listener for customizing the unit configuration at runtime
                runtimeConfigured.produce(new HibernateOrmIntegrationRuntimeConfiguredBuildItem("keycloak", descriptor.getName())
                        .setInitListener(recorder.createUserDefinedUnitListener(datasourceName)));
                userManagedEntities.addAll(descriptor.getManagedClassNames());
            }
        }

        if (defaultUnitDescriptor == null) {
            throw new RuntimeException("No default persistence unit found.");
        }

        configureDefaultPersistenceUnitEntities(defaultUnitDescriptor, indexBuildItem, userManagedEntities);
    }

    static void configurePersistenceUnitProperties(String datasourceName, ParsedPersistenceXmlDescriptor descriptor) {
        Properties unitProperties = descriptor.getProperties();
        var isResourceLocalSpecified = PersistenceUnitTransactionType.RESOURCE_LOCAL.equals(descriptor.getPersistenceUnitTransactionType()) ||
                Optional.ofNullable(unitProperties.getProperty(AvailableSettings.JAKARTA_TRANSACTION_TYPE))
                        .map(f -> f.equalsIgnoreCase(PersistenceUnitTransactionType.RESOURCE_LOCAL.name()))
                        .orElse(false);
        if (isResourceLocalSpecified) {
            throw new IllegalArgumentException("You need to use '%s' transaction type in your persistence.xml file."
                    .formatted(PersistenceUnitTransactionType.JTA.name()));
        }

        unitProperties.setProperty(AvailableSettings.JAKARTA_TRANSACTION_TYPE, PersistenceUnitTransactionType.JTA.name());
        descriptor.setTransactionType(PersistenceUnitTransactionType.JTA);

        // set datasource name
        unitProperties.setProperty(JdbcSettings.JAKARTA_JTA_DATASOURCE, datasourceName);
        unitProperties.setProperty(AvailableSettings.DATASOURCE, datasourceName); // for backward compatibility

        DatabaseOptions.getNamedKey(DatabaseOptions.DB_SQL_JPA_DEBUG, datasourceName)
                .filter(Configuration::isKcPropertyTrue)
                .ifPresent(f -> unitProperties.put(AvailableSettings.USE_SQL_COMMENTS, "true"));

        DatabaseOptions.getNamedKey(DatabaseOptions.DB_SQL_LOG_SLOW_QUERIES, datasourceName)
                .flatMap(Configuration::getOptionalKcValue)
                .ifPresent(threshold -> unitProperties.put(AvailableSettings.LOG_SLOW_QUERY, threshold));
    }

    private static void configureDefaultPersistenceUnitEntities(ParsedPersistenceXmlDescriptor descriptor, CombinedIndexBuildItem indexBuildItem,
                                                                List<String> userManagedEntities) {
        IndexView index = indexBuildItem.getIndex();
        Collection<AnnotationInstance> annotations = index.getAnnotations(DotName.createSimple(Entity.class.getName()));

        for (AnnotationInstance annotation : annotations) {
            AnnotationTarget target = annotation.target();
            String targetName = target.asClass().name().toString();

            if (!userManagedEntities.contains(targetName)
                    && (!targetName.startsWith("org.keycloak") || targetName.startsWith("org.keycloak.testsuite"))) {
                descriptor.addClasses(targetName);
            }
        }
    }

    static void configureDefaultPersistenceUnitProperties(ParsedPersistenceXmlDescriptor descriptor, HibernateOrmConfig config,
                                                          JdbcDataSourceBuildItem defaultDataSource) {
        if (defaultDataSource == null || !defaultDataSource.isDefault()) {
            throw new RuntimeException("The server datasource must be the default datasource.");
        }

        Properties unitProperties = descriptor.getProperties();

        final Optional<String> dialect = getOptionalKcValue(DatabaseOptions.DB_DIALECT.getKey());
        dialect.ifPresent(d -> unitProperties.setProperty(AvailableSettings.DIALECT, d));

        final Optional<String> defaultSchema = getOptionalKcValue(DatabaseOptions.DB_SCHEMA.getKey());
        defaultSchema.ifPresent(ds -> unitProperties.setProperty(AvailableSettings.DEFAULT_SCHEMA, ds));

        unitProperties.setProperty(AvailableSettings.JAKARTA_TRANSACTION_TYPE, PersistenceUnitTransactionType.JTA.name());
        descriptor.setTransactionType(PersistenceUnitTransactionType.JTA);

        unitProperties.setProperty(AvailableSettings.QUERY_STARTUP_CHECKING, Boolean.FALSE.toString());

        String dbKind = defaultDataSource.getDbKind();

        for (Map.Entry<Object, Object> query : loadSpecificNamedQueries(dbKind.toLowerCase()).entrySet()) {
            unitProperties.setProperty(QUERY_PROPERTY_PREFIX + query.getKey(), query.getValue().toString());
        }

        if (getOptionalBooleanKcValue(DatabaseOptions.DB_SQL_JPA_DEBUG.getKey()).orElse(false)) {
            unitProperties.put(AvailableSettings.USE_SQL_COMMENTS, "true");
        }

        getOptionalKcValue(DatabaseOptions.DB_SQL_LOG_SLOW_QUERIES.getKey())
                .ifPresent(v -> unitProperties.put(AvailableSettings.LOG_SLOW_QUERY, v));
    }

    static void configureUserDefinedPersistenceUnits(List<PersistenceXmlDescriptorBuildItem> descriptors,
                                                     Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> factories,
                                                     Map<String, ProviderFactory> preConfiguredProviders, Spi spi) {
        descriptors.stream()
                .map(PersistenceXmlDescriptorBuildItem::getDescriptor)
                .map(PersistenceUnitDescriptor::getName)
                .filter(Predicate.not("keycloak-default"::equals))
                .forEach((String unitName) -> {
                    NamedJpaConnectionProviderFactory factory = new NamedJpaConnectionProviderFactory();

                    factory.setUnitName(unitName);

                    factories.get(spi).get(JpaConnectionProvider.class).put(unitName, NamedJpaConnectionProviderFactory.class);
                    preConfiguredProviders.put(unitName, factory);
                });
    }

    /**
     * Get datasource name obtained from the persistence.xml file based on this order:
     * <ol>
     *      <li> return {@link JdbcSettings#JAKARTA_JTA_DATASOURCE} if specified
     *      <li> return {@link AvailableSettings#DATASOURCE} property if specified
     *      <li> return persistence unit name
     * </ol>
     * Can be removed after removing support for persistence.xml files
     */
    static String getDatasourceNameFromPersistenceXml(PersistenceUnitDescriptor descriptor) {
        if (descriptor == null) {
            throw new IllegalStateException("Descriptor cannot be null");
        }
        final BiConsumer<String, String> infoAboutUsedSourceForDsName = (source, name) -> logger.debugf(
                "Datasource name '%s' is obtained from the '%s' configuration property in persistence.xml file. " +
                        "Use '%s' name for datasource options like 'db-kind-%s'.", name, source, name, name);

        String persistenceUnitName = descriptor.getName();
        Properties properties = descriptor.getProperties();

        // 1. return Jakarta properties
        var jakartaProperty = properties.getProperty(JdbcSettings.JAKARTA_JTA_DATASOURCE);
        if (jakartaProperty != null) {
            infoAboutUsedSourceForDsName.accept(JdbcSettings.JAKARTA_JTA_DATASOURCE, jakartaProperty);
            return jakartaProperty;
        }

        // 2. return deprecated Hibernate property
        var deprecatedHibernateProperty = properties.getProperty(AvailableSettings.DATASOURCE);
        if (deprecatedHibernateProperty != null) {
            logger.warnf("Property '%s' is deprecated for some time and you should rather use '%s' property for datasource name in persistence.xml file.",
                    AvailableSettings.DATASOURCE, JdbcSettings.JAKARTA_JTA_DATASOURCE);
            infoAboutUsedSourceForDsName.accept(AvailableSettings.DATASOURCE, deprecatedHibernateProperty);
            return deprecatedHibernateProperty;
        }

        // 3. return persistence unit name
        infoAboutUsedSourceForDsName.accept("Persistence unit name", persistenceUnitName);
        return persistenceUnitName;
    }
}
