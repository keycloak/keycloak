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

package org.keycloak.quarkus.deployment;

import io.quarkus.agroal.runtime.DataSourcesJdbcBuildTimeConfig;
import io.quarkus.agroal.runtime.TransactionIntegration;
import io.quarkus.agroal.runtime.health.DataSourceHealthCheck;
import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.agroal.spi.JdbcDriverBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BuildTimeConditionBuildItem;
import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceResultBuildItem;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.StaticInitConfigBuilderBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.PersistenceXmlDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;
import io.quarkus.narayana.jta.runtime.TransactionManagerBuildTimeConfig;
import io.quarkus.narayana.jta.runtime.TransactionManagerBuildTimeConfig.UnsafeMultipleLastResourcesMode;
import io.quarkus.resteasy.reactive.server.spi.MethodScannerBuildItem;
import io.quarkus.resteasy.reactive.server.spi.PreExceptionMapperHandlerBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import jakarta.persistence.Entity;
import jakarta.persistence.PersistenceUnitTransactionType;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.hibernate.cfg.JdbcSettings;
import org.eclipse.microprofile.health.Readiness;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;
import org.infinispan.protostream.SerializationContextInitializer;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticatorSpi;
import org.keycloak.authentication.authenticators.browser.DeployedScriptAuthenticatorFactory;
import org.keycloak.authorization.policy.provider.PolicySpi;
import org.keycloak.authorization.policy.provider.js.DeployedScriptPolicyFactory;
import org.keycloak.common.Profile;
import org.keycloak.common.crypto.FipsMode;
import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.config.DatabaseOptions;
import org.keycloak.config.HealthOptions;
import org.keycloak.config.HttpOptions;
import org.keycloak.config.LoggingOptions;
import org.keycloak.config.ManagementOptions;
import org.keycloak.config.MetricsOptions;
import org.keycloak.config.SecurityOptions;
import org.keycloak.config.TracingOptions;
import org.keycloak.config.TransactionOptions;
import org.keycloak.config.database.Database;
import org.keycloak.connections.jpa.DefaultJpaConnectionProviderFactory;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionSpi;
import org.keycloak.connections.jpa.updater.liquibase.LiquibaseJpaUpdaterProviderFactory;
import org.keycloak.connections.jpa.updater.liquibase.conn.DefaultLiquibaseConnectionProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.policy.BlacklistPasswordPolicyProviderFactory;
import org.keycloak.protocol.ProtocolMapperSpi;
import org.keycloak.protocol.oidc.mappers.DeployedScriptOIDCProtocolMapper;
import org.keycloak.protocol.saml.mappers.DeployedScriptSAMLProtocolMapper;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderManager;
import org.keycloak.provider.Spi;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.KeycloakRecorder;
import org.keycloak.quarkus.runtime.cli.Picocli;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.KeycloakConfigSourceProvider;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;
import org.keycloak.quarkus.runtime.configuration.PersistedConfigSource;
import org.keycloak.quarkus.runtime.configuration.PropertyMappingInterceptor;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;
import org.keycloak.quarkus.runtime.configuration.mappers.WildcardPropertyMapper;
import org.keycloak.quarkus.runtime.integration.resteasy.KeycloakHandlerChainCustomizer;
import org.keycloak.quarkus.runtime.integration.resteasy.KeycloakTracingCustomizer;
import org.keycloak.quarkus.runtime.logging.ClearMappedDiagnosticContextFilter;
import org.keycloak.quarkus.runtime.services.health.KeycloakClusterReadyHealthCheck;
import org.keycloak.quarkus.runtime.services.health.KeycloakReadyHealthCheck;
import org.keycloak.quarkus.runtime.storage.database.jpa.NamedJpaConnectionProviderFactory;
import org.keycloak.quarkus.runtime.themes.FlatClasspathThemeResourceProviderFactory;
import org.keycloak.representations.provider.ScriptProviderDescriptor;
import org.keycloak.representations.provider.ScriptProviderMetadata;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.services.DefaultKeycloakSessionFactory;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.resources.LoadBalancerResource;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.theme.ClasspathThemeProviderFactory;
import org.keycloak.theme.ClasspathThemeResourceProviderFactory;
import org.keycloak.theme.FolderThemeProviderFactory;
import org.keycloak.theme.JarThemeProviderFactory;
import org.keycloak.theme.ThemeResourceSpi;
import org.keycloak.transaction.JBossJtaTransactionManagerLookup;
import org.keycloak.userprofile.config.UPConfigUtils;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;
import org.keycloak.vault.FilesKeystoreVaultProviderFactory;
import org.keycloak.vault.FilesPlainTextVaultProviderFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Handler;

import static org.keycloak.config.DatabaseOptions.DB;
import static org.keycloak.connections.jpa.util.JpaUtils.loadSpecificNamedQueries;
import static org.keycloak.quarkus.runtime.Environment.getCurrentOrCreateFeatureProfile;
import static org.keycloak.quarkus.runtime.Providers.getProviderManager;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getOptionalBooleanKcValue;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getOptionalKcValue;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getOptionalValue;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;
import static org.keycloak.quarkus.runtime.storage.database.jpa.QuarkusJpaConnectionProviderFactory.DEFAULT_PERSISTENCE_UNIT;
import static org.keycloak.quarkus.runtime.storage.database.jpa.QuarkusJpaConnectionProviderFactory.QUERY_PROPERTY_PREFIX;
import static org.keycloak.representations.provider.ScriptProviderDescriptor.AUTHENTICATORS;
import static org.keycloak.representations.provider.ScriptProviderDescriptor.MAPPERS;
import static org.keycloak.representations.provider.ScriptProviderDescriptor.POLICIES;
import static org.keycloak.representations.provider.ScriptProviderDescriptor.SAML_MAPPERS;
import static org.keycloak.theme.ClasspathThemeProviderFactory.KEYCLOAK_THEMES_JSON;

class KeycloakProcessor {

    private static final Logger logger = Logger.getLogger(KeycloakProcessor.class);

    private static final String JAR_FILE_SEPARATOR = "!/";
    private static final Map<String, Function<ScriptProviderMetadata, ProviderFactory>> DEPLOYEABLE_SCRIPT_PROVIDERS = new HashMap<>();
    private static final String KEYCLOAK_SCRIPTS_JSON_PATH = "META-INF/keycloak-scripts.json";

    private static final List<Class<? extends ProviderFactory>> IGNORED_PROVIDER_FACTORY = List.of(
            JBossJtaTransactionManagerLookup.class,
            DefaultJpaConnectionProviderFactory.class,
            DefaultLiquibaseConnectionProvider.class,
            FolderThemeProviderFactory.class,
            LiquibaseJpaUpdaterProviderFactory.class,
            FilesKeystoreVaultProviderFactory.class,
            FilesPlainTextVaultProviderFactory.class,
            BlacklistPasswordPolicyProviderFactory.class,
            ClasspathThemeResourceProviderFactory.class,
            JarThemeProviderFactory.class);

    static {
        DEPLOYEABLE_SCRIPT_PROVIDERS.put(AUTHENTICATORS, KeycloakProcessor::registerScriptAuthenticator);
        DEPLOYEABLE_SCRIPT_PROVIDERS.put(POLICIES, KeycloakProcessor::registerScriptPolicy);
        DEPLOYEABLE_SCRIPT_PROVIDERS.put(MAPPERS, KeycloakProcessor::registerScriptMapper);
        DEPLOYEABLE_SCRIPT_PROVIDERS.put(SAML_MAPPERS, KeycloakProcessor::registerSAMLScriptMapper);
    }

    private static ProviderFactory registerScriptAuthenticator(ScriptProviderMetadata metadata) {
        return new DeployedScriptAuthenticatorFactory(metadata);
    }

    private static ProviderFactory registerScriptPolicy(ScriptProviderMetadata metadata) {
        return new DeployedScriptPolicyFactory(metadata);
    }

    private static ProviderFactory registerScriptMapper(ScriptProviderMetadata metadata) {
        return new DeployedScriptOIDCProtocolMapper(metadata);
    }

    private static ProviderFactory registerSAMLScriptMapper(ScriptProviderMetadata metadata) {
        return new DeployedScriptSAMLProtocolMapper(metadata);
    }

    @BuildStep
    FeatureBuildItem getFeature() {
        return new FeatureBuildItem("keycloak");
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    @Produce(ConfigBuildItem.class)
    void initConfig(KeycloakRecorder recorder) {
        // other buildsteps directly use the Config
        // so directly init it
        Config.init(new MicroProfileConfigProvider());
        // also init in byte code for the actual server start
        recorder.initConfig();
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    @Consume(ConfigBuildItem.class)
    @Produce(ProfileBuildItem.class)
    void configureProfile(KeycloakRecorder recorder) {
        Profile profile = getCurrentOrCreateFeatureProfile();

        // record the features so that they are not calculated again at runtime
        recorder.configureProfile(profile.getName(), profile.getFeatures());
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    @Consume(ConfigBuildItem.class)
    void configureRedirectForRootPath(BuildProducer<RouteBuildItem> routes,
                                      HttpRootPathBuildItem httpRootPathBuildItem,
                                      KeycloakRecorder recorder) {
        Configuration.getOptionalKcValue(HttpOptions.HTTP_RELATIVE_PATH)
                .filter(StringUtil::isNotBlank)
                .filter(f -> !f.equals("/"))
                .ifPresent(relativePath ->
                        routes.produce(httpRootPathBuildItem.routeBuilder()
                                .route("/")
                                .handler(recorder.getRedirectHandler(relativePath))
                                .build())
                );
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(onlyIf = IsManagementEnabled.class)
    @Consume(ConfigBuildItem.class)
    void configureManagementInterface(BuildProducer<RouteBuildItem> routes,
                                      NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
                                      KeycloakRecorder recorder) {
        final var relativePath = Configuration.getOptionalKcValue(ManagementOptions.HTTP_MANAGEMENT_RELATIVE_PATH).orElse("/");

        if (StringUtil.isNotBlank(relativePath) && !relativePath.equals("/")) {
            // redirect from / to the relativePath
            routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .management()
                    .route("/")
                    .handler(recorder.getRedirectHandler(relativePath))
                    .build());
        }

        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management()
                .route(relativePath)
                .handler(recorder.getManagementHandler())
                .build());
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    @Consume(ConfigBuildItem.class)
    void configureTruststore(KeycloakRecorder recorder) {
        recorder.configureTruststore();
    }

    /**
     * Check whether JDBC driver is present for the specified DB
     *
     * @param ignore used for changing build items execution order with regards to AgroalProcessor
     */
    @BuildStep
    @Produce(CheckJdbcBuildStep.class)
    void checkJdbcDriver(BuildProducer<JdbcDriverBuildItem> ignore) {
        final Optional<String> dbDriver = Configuration.getOptionalValue("quarkus.datasource.jdbc.driver");

        if (dbDriver.isPresent()) {
            try {
                // We do not want to initialize the JDBC driver class
                Class.forName(dbDriver.get(), false, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throwConfigError(String.format("Unable to find the JDBC driver (%s). You need to install it.", dbDriver.get()));
            }
        }
    }

    // Inspired by AgroalProcessor
    @BuildStep
    @Produce(CheckMultipleDatasourcesBuildStep.class)
    void checkMultipleDatasourcesUseXA(TransactionManagerBuildTimeConfig transactionManagerConfig, DataSourcesBuildTimeConfig dataSourcesConfig, DataSourcesJdbcBuildTimeConfig jdbcConfig) {
        Set<String> datasources = dataSourcesConfig.dataSources().keySet();
        if (datasources.size() > 1) {
            logger.infof("Multiple datasources are specified: %s", String.join(", ", datasources));
        }

        if (transactionManagerConfig.unsafeMultipleLastResources()
                .orElse(UnsafeMultipleLastResourcesMode.DEFAULT) != UnsafeMultipleLastResourcesMode.FAIL) {
            return;
        }

        List<String> nonXADatasources = datasources.stream()
                .filter(ds -> !Configuration.isKcPropertyTrue(TransactionOptions.getNamedTxXADatasource(ds)))
                .filter(ds -> {
                    var jdbc = jdbcConfig.dataSources().get(ds).jdbc();
                    return jdbc.enabled() && jdbc.transactions() != TransactionIntegration.XA;
                })
                .toList();

        if (nonXADatasources.size() > 1) {
            throwConfigError("Multiple datasources are configured but more than 1 (%s) is using non-XA transactions. ".formatted(String.join(", ", nonXADatasources)) +
                    "All the datasources except one must must be XA to be able to use Last Resource Commit Optimization (LRCO). " +
                    "Please update your configuration by setting --transaction-xa-enabled=true " +
                    "and/or --transaction-xa-enabled-<your-datasource-name>=true.");
        }
    }

    private void throwConfigError(String msg) {
        // Ignore queued TRACE and DEBUG messages for not initialized log handlers
        InitialConfigurator.DELAYED_HANDLER.setBuildTimeHandlers(new Handler[]{});
        throw new ConfigurationException(msg);
    }

    /**
     * Parse the default configuration for the User Profile provider
     */
    @BuildStep
    @Produce(UserProfileBuildItem.class)
    UserProfileBuildItem parseDefaultUserProfileConfig() {
        UPConfig defaultConfig = UPConfigUtils.parseSystemDefaultConfig();
        logger.debug("Parsing default configuration for the User Profile provider");
        return new UserProfileBuildItem(defaultConfig);
    }

    /**
     * Set the default configuration to the User Profile provider
     */
    @BuildStep
    @Consume(ProfileBuildItem.class)
    @Record(ExecutionTime.STATIC_INIT)
    void setDefaultUserProfileConfig(KeycloakRecorder recorder, UserProfileBuildItem configuration) {
        recorder.setDefaultUserProfileConfiguration(configuration.getDefaultConfig());
    }

    @BuildStep
    @Produce(ValidatePersistenceUnitsBuildItem.class)
    void checkPersistenceUnits(List<PersistenceXmlDescriptorBuildItem> descriptors) {
        if (Database.Vendor.TIDB.isOfKind(Configuration.getConfigValue(DB).getValue())) {
            if (!Profile.isFeatureEnabled(Profile.Feature.DB_TIDB)){
                throw new RuntimeException("The feature TiDB is not enabled");
            }
        }

        List<String> notSetPersistenceUnitsDBKinds = descriptors.stream()
                .map(PersistenceXmlDescriptorBuildItem::getDescriptor)
                .filter(descriptor -> !descriptor.getName().equals(DEFAULT_PERSISTENCE_UNIT)) // not default persistence unit
                .map(KeycloakProcessor::getDatasourceNameFromPersistenceXml)
                .filter(this::missingDbKind)
                .map(datasourceName -> DatabaseOptions.Datasources.getNamedKey(DatabaseOptions.DB, datasourceName).orElseThrow()).toList();

        if (!notSetPersistenceUnitsDBKinds.isEmpty()) {
            throwConfigError("Detected additional named datasources without a DB kind set, please specify: %s".formatted(String.join(",", notSetPersistenceUnitsDBKinds)));
        }
    }

    /**
     * Try to find if DB kind is specified for the descriptor name.
     * <p>
     * Check it in order:
     * <ol>
     * <li> {@code db-kind-<descriptorName}
     * <li> {@code quarkus.datasource."<descriptorName>".db-kind}
     * <li> {@code quarkus.datasource.<descriptorName>.db-kind}
     * </ol>
     */
    private boolean missingDbKind(String datasourceName) {
        String key = NS_KEYCLOAK_PREFIX.concat(DatabaseOptions.Datasources.getNamedKey(DatabaseOptions.DB, datasourceName).orElseThrow());
        PropertyMappingInterceptor.disable();
        try {
            var from = Configuration.getConfigValue(key);

            if (from.getValue() != null) {
                return false; // user has directly specified
            }

            WildcardPropertyMapper<?> mapper = (WildcardPropertyMapper<?>)PropertyMappers.getMapper(key);

            // quarkus properties
            boolean missing = Configuration.getOptionalValue(mapper.getTo(datasourceName))
                    .or(() -> Configuration.getOptionalValue(mapper.getTo(datasourceName).replaceAll("\"", "")))
                    .isEmpty();

            if (!missing) {
                logger.warnf(
                        "You have set DB kind for '%s' datasource via a Quarkus property. This approach is deprecated and you should use the Keycloak 'db-kind-%s' property.",
                        datasourceName, datasourceName);
            }
            return missing;
        } finally {
            PropertyMappingInterceptor.enable();
        }
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

    /**
     * <p>Configures the persistence unit for Quarkus.
     *
     * <p>The {@code hibernate-orm} extension expects that the dialect is statically
     * set to the persistence unit if there is any from the classpath and we use this method to obtain the dialect from the configuration
     * file so that we can build the application with whatever dialect we want. In addition to the dialect, we should also be
     * allowed to set any additional defaults that we think that makes sense.
     *
     * @param config
     * @param descriptors
     */
    @BuildStep
    @Consume(ValidatePersistenceUnitsBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void configurePersistenceUnits(HibernateOrmConfig config,
            List<PersistenceXmlDescriptorBuildItem> descriptors,
            List<JdbcDataSourceBuildItem> jdbcDataSources,
            BuildProducer<AdditionalJpaModelBuildItem> additionalJpaModel,
            CombinedIndexBuildItem indexBuildItem,
            BuildProducer<HibernateOrmIntegrationRuntimeConfiguredBuildItem> runtimeConfigured,
            KeycloakRecorder recorder) {
        ParsedPersistenceXmlDescriptor defaultUnitDescriptor = null;
        List<String> userManagedEntities = new ArrayList<>();

        for (PersistenceXmlDescriptorBuildItem item : descriptors) {
            ParsedPersistenceXmlDescriptor descriptor = (ParsedPersistenceXmlDescriptor) item.getDescriptor();

            if (DEFAULT_PERSISTENCE_UNIT.equals(descriptor.getName())) {
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

    @BuildStep
    @Consume(CheckJdbcBuildStep.class)
    @Consume(CheckMultipleDatasourcesBuildStep.class)
    void produceDefaultPersistenceUnit(BuildProducer<PersistenceXmlDescriptorBuildItem> producer) {
        PersistenceXmlParser parser = PersistenceXmlParser.create();
        PersistenceUnitDescriptor descriptor = parser.parse(Collections.singletonList(parser.getClassLoaderService().locateResource("default-persistence.xml")))
                .values()
                .stream()
                .findAny()
                .orElseThrow(() -> new NoSuchElementException("Cannot find the file 'default-persistence.xml'"));

        producer.produce(new PersistenceXmlDescriptorBuildItem(descriptor));
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

        // db-dialect
        DatabaseOptions.Datasources.getNamedKey(DatabaseOptions.DB_DIALECT, datasourceName)
                .flatMap(Configuration::getOptionalKcValue)
                .ifPresent(dialect -> unitProperties.setProperty(AvailableSettings.DIALECT, dialect));

        // db-schema
        DatabaseOptions.Datasources.getNamedKey(DatabaseOptions.DB_SCHEMA, datasourceName)
                .flatMap(Configuration::getOptionalKcValue)
                .ifPresent(schema -> unitProperties.setProperty(AvailableSettings.DEFAULT_SCHEMA, schema));

        unitProperties.setProperty(AvailableSettings.JAKARTA_TRANSACTION_TYPE, PersistenceUnitTransactionType.JTA.name());
        descriptor.setTransactionType(PersistenceUnitTransactionType.JTA);

        // set datasource name
        unitProperties.setProperty(JdbcSettings.JAKARTA_JTA_DATASOURCE,datasourceName);
        unitProperties.setProperty(AvailableSettings.DATASOURCE, datasourceName); // for backward compatibility

        // db-debug-jpql
        DatabaseOptions.Datasources.getNamedKey(DatabaseOptions.DB_SQL_JPA_DEBUG, datasourceName)
                .filter(Configuration::isKcPropertyTrue)
                .ifPresent(f -> unitProperties.put(AvailableSettings.USE_SQL_COMMENTS, "true"));

        // db-log-slow-queries-threshold
        DatabaseOptions.Datasources.getNamedKey(DatabaseOptions.DB_SQL_LOG_SLOW_QUERIES, datasourceName)
                .flatMap(Configuration::getOptionalKcValue)
                .ifPresent(threshold -> unitProperties.put(AvailableSettings.LOG_SLOW_QUERY, threshold));
    }

    private void configureDefaultPersistenceUnitProperties(ParsedPersistenceXmlDescriptor descriptor, HibernateOrmConfig config,
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

        for (Entry<Object, Object> query : loadSpecificNamedQueries(dbKind.toLowerCase()).entrySet()) {
            unitProperties.setProperty(QUERY_PROPERTY_PREFIX + query.getKey(), query.getValue().toString());
        }

        if (getOptionalBooleanKcValue(DatabaseOptions.DB_SQL_JPA_DEBUG.getKey()).orElse(false)) {
            unitProperties.put(AvailableSettings.USE_SQL_COMMENTS, "true");
        }

        getOptionalKcValue(DatabaseOptions.DB_SQL_LOG_SLOW_QUERIES.getKey())
                .ifPresent(v -> unitProperties.put(AvailableSettings.LOG_SLOW_QUERY, v));
    }

    private void configureDefaultPersistenceUnitEntities(ParsedPersistenceXmlDescriptor descriptor, CombinedIndexBuildItem indexBuildItem,
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

    /**
     * <p>Load the built-in provider factories during build time so we don't spend time looking up them at runtime. By loading
     * providers at this stage we are also able to perform a more dynamic configuration based on the default providers.
     *
     * <p>User-defined providers are going to be loaded at startup</p>
     *
     * @param recorder
     */
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    @Consume(ConfigBuildItem.class)
    @Consume(CryptoProviderInitBuildItem.class)
    @Produce(KeycloakSessionFactoryPreInitBuildItem.class)
    void configureKeycloakSessionFactory(KeycloakRecorder recorder, List<PersistenceXmlDescriptorBuildItem> descriptors) {
        Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> factories = new HashMap<>();
        Map<Class<? extends Provider>, String> defaultProviders = new HashMap<>();
        Map<String, ProviderFactory> preConfiguredProviders = new HashMap<>();

        for (Entry<Spi, Map<Class<? extends Provider>, Map<String, ProviderFactory>>> entry : loadFactories(preConfiguredProviders)
                .entrySet()) {
            Spi spi = entry.getKey();

            checkProviders(spi, entry.getValue(), defaultProviders);

            for (Entry<Class<? extends Provider>, Map<String, ProviderFactory>> value : entry.getValue().entrySet()) {
                for (ProviderFactory factory : value.getValue().values()) {
                    factories.computeIfAbsent(spi,
                            key -> new HashMap<>())
                            .computeIfAbsent(spi.getProviderClass(), aClass -> new HashMap<>()).put(factory.getId(),factory.getClass());
                }
            }

            if (spi instanceof JpaConnectionSpi) {
                configureUserDefinedPersistenceUnits(descriptors, factories, preConfiguredProviders, spi);
            }

            if (spi instanceof ThemeResourceSpi) {
                configureThemeResourceProviders(factories, spi);
            }
        }

        recorder.configSessionFactory(factories, defaultProviders, preConfiguredProviders, loadThemesFromClassPath());
    }

    private List<ClasspathThemeProviderFactory.ThemesRepresentation> loadThemesFromClassPath() {
        try {
            List<ClasspathThemeProviderFactory.ThemesRepresentation> themes = new ArrayList<>();
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(KEYCLOAK_THEMES_JSON);

            while (resources.hasMoreElements()) {
                themes.add(JsonSerialization.readValue(resources.nextElement().openStream(), ClasspathThemeProviderFactory.ThemesRepresentation.class));
            }

            return themes;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load themes", e);
        }
    }

    private void configureThemeResourceProviders(Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> factories, Spi spi) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(FlatClasspathThemeResourceProviderFactory.THEME_RESOURCES);

            if (resources.hasMoreElements()) {
                // make sure theme resources are loaded using a flat classpath. if no resources are available the provider is not registered
                factories.computeIfAbsent(spi, key -> new HashMap<>()).computeIfAbsent(spi.getProviderClass(), aClass -> new HashMap<>()).put(FlatClasspathThemeResourceProviderFactory.ID, FlatClasspathThemeResourceProviderFactory.class);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to install default theme resource provider", e);
        }
    }

    private void configureUserDefinedPersistenceUnits(List<PersistenceXmlDescriptorBuildItem> descriptors,
            Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> factories,
            Map<String, ProviderFactory> preConfiguredProviders, Spi spi) {
        descriptors.stream()
                .map(PersistenceXmlDescriptorBuildItem::getDescriptor)
                .map(PersistenceUnitDescriptor::getName)
                .filter(Predicate.not(DEFAULT_PERSISTENCE_UNIT::equals))
                .forEach((String unitName) -> {
                    NamedJpaConnectionProviderFactory factory = new NamedJpaConnectionProviderFactory();

                    factory.setUnitName(unitName);

                    factories.get(spi).get(JpaConnectionProvider.class).put(unitName, NamedJpaConnectionProviderFactory.class);
                    preConfiguredProviders.put(unitName, factory);
                });
    }

    /**
     * Register the custom {@link ConfigSource} implementations.
     *
     * @param configSources
     */
    @BuildStep(onlyIfNot = IsIntegrationTest.class )
    void configureConfigSources(BuildProducer<StaticInitConfigBuilderBuildItem> configSources) {
        configSources.produce(new StaticInitConfigBuilderBuildItem(KeycloakConfigSourceProvider.class.getName()));
    }

    @BuildStep(onlyIf = IsIntegrationTest.class)
    void prepareTestEnvironment(BuildProducer< StaticInitConfigBuilderBuildItem> configSources, DevServicesDatasourceResultBuildItem dbConfig) {
        configSources.produce(new StaticInitConfigBuilderBuildItem("org.keycloak.quarkus.runtime.configuration.test.TestKeycloakConfigSourceProvider"));

        // we do not enable dev services by default and the DevServicesDatasourceResultBuildItem might not be available when discovering build steps
        // Quarkus seems to allow that when the DevServicesDatasourceResultBuildItem is not the only parameter to the build step
        // this might be too sensitive and break if Quarkus changes the behavior
        if (dbConfig != null && dbConfig.getDefaultDatasource() != null) {
            Map<String, String> configProperties = dbConfig.getDefaultDatasource().getConfigProperties();

            for (Entry<String, String> dbConfigProperty : configProperties.entrySet()) {
                PropertyMapper<?> mapper = PropertyMappers.getMapper(dbConfigProperty.getKey());

                if (mapper == null) {
                    continue;
                }

                String kcProperty = mapper.getFrom();

                if (kcProperty.endsWith("db")) {
                    // db kind set when running tests
                    continue;
                }

                System.setProperty(kcProperty, dbConfigProperty.getValue());
            }
        }
    }

    /**
     * <p>Make the build time configuration available at runtime so that the server can run without having to specify some of
     * the properties again.
     */
    @BuildStep(onlyIf = IsReAugmentation.class)
    void persistBuildTimeProperties(BuildProducer<GeneratedResourceBuildItem> resources) {
        Properties properties = Picocli.getNonPersistedBuildTimeOptions();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            properties.store(outputStream, " Auto-generated, DO NOT change this file");
            resources.produce(new GeneratedResourceBuildItem(PersistedConfigSource.PERSISTED_PROPERTIES, outputStream.toByteArray()));
        } catch (Exception cause) {
            throw new RuntimeException("Failed to persist configuration", cause);
        }
    }

    /**
     * This will cause quarkus to include specified modules in the jandex index. For example keycloak-services is needed as it includes
     * most of the JAX-RS resources, which are required to register Resteasy builtin providers.
     * Similar reason is liquibase
     *
     * @param indexDependencyBuildItemBuildProducer
     */
    @BuildStep
    void index(BuildProducer<IndexDependencyBuildItem> indexDependencyBuildItemBuildProducer) {
        indexDependencyBuildItemBuildProducer.produce(new IndexDependencyBuildItem("org.liquibase", "liquibase-core"));
        indexDependencyBuildItemBuildProducer.produce(new IndexDependencyBuildItem("org.keycloak", "keycloak-services"));
    }

    @BuildStep
    @Consume(CheckJdbcBuildStep.class)
    void indexJpaStore(BuildProducer<IndexDependencyBuildItem> indexDependencyBuildItemBuildProducer) {
        indexDependencyBuildItemBuildProducer.produce(new IndexDependencyBuildItem("org.keycloak", "keycloak-model-jpa"));
    }

    @BuildStep
    void disableHealthCheckBean(BuildProducer<BuildTimeConditionBuildItem> removeBeans, CombinedIndexBuildItem index) {
        if (isHealthDisabled()) {
            disableReadyHealthCheck(removeBeans, index);
            disableClusterHealthCheck(removeBeans, index);
            return;
        }
        if (isMetricsDisabled()) {
            // disables the single check we provide which depends on metrics enabled.
            disableReadyHealthCheck(removeBeans, index);
        }
        if (InfinispanUtils.isRemoteInfinispan()) {
            // no cluster when the remote infinispan is used.
            disableClusterHealthCheck(removeBeans, index);
        }
    }

    private static void disableClusterHealthCheck(BuildProducer<BuildTimeConditionBuildItem> removeBeans, CombinedIndexBuildItem index) {
        ClassInfo clusterHealth = index.getIndex().getClassByName(DotName.createSimple(KeycloakClusterReadyHealthCheck.class));
        removeBeans.produce(new BuildTimeConditionBuildItem(clusterHealth.asClass(), false));
    }

    private static void disableReadyHealthCheck(BuildProducer<BuildTimeConditionBuildItem> removeBeans, CombinedIndexBuildItem index) {
        ClassInfo disabledBean = index.getIndex().getClassByName(DotName.createSimple(KeycloakReadyHealthCheck.class.getName()));
        removeBeans.produce(new BuildTimeConditionBuildItem(disabledBean.asClass(), false));
    }

    @BuildStep
    void disableMdcContextFilter(BuildProducer<BuildTimeConditionBuildItem> removeBeans, CombinedIndexBuildItem index) {
        if (!Configuration.isTrue(LoggingOptions.LOG_MDC_ENABLED)) {
            // disables the filter
            ClassInfo disabledBean = index.getIndex()
                    .getClassByName(DotName.createSimple(ClearMappedDiagnosticContextFilter.class.getName()));
            removeBeans.produce(new BuildTimeConditionBuildItem(disabledBean.asClass(), false));
        }
    }

    // We can't use quarkus.datasource.health.enabled=false as that would remove the DataSourceHealthCheck from CDI and
    // it can't be instantiated via constructor as it now includes some field injection points. So we just make it a regular
    // bean without the @Readiness annotation so it won't be used as a health check on it's own.
    @BuildStep
    AnnotationsTransformerBuildItem disableDefaultDataSourceHealthCheck() {
        return new AnnotationsTransformerBuildItem(AnnotationTransformation.forClasses()
                .whenClass(c -> c.name().equals(DotName.createSimple(DataSourceHealthCheck.class)))
                .transform(t -> t.remove(
                        a -> a.name().equals(DotName.createSimple(Readiness.class)))));
    }

    @BuildStep
    void configureResteasy(CombinedIndexBuildItem index,
            BuildProducer<BuildTimeConditionBuildItem> buildTimeConditionBuildItemBuildProducer,
            BuildProducer<MethodScannerBuildItem> scanner,
           BuildProducer<PreExceptionMapperHandlerBuildItem> preExceptionMapperHandlerBuildItemBuildProducer) {
        if (!Profile.isFeatureEnabled(Profile.Feature.ADMIN_API)) {
            buildTimeConditionBuildItemBuildProducer.produce(new BuildTimeConditionBuildItem(index.getIndex().getClassByName(DotName.createSimple(
                    AdminRoot.class.getName())), false));
        }

        if (!MultiSiteUtils.isMultiSiteEnabled()) {
            buildTimeConditionBuildItemBuildProducer.produce(new BuildTimeConditionBuildItem(index.getIndex().getClassByName(DotName.createSimple(
                    LoadBalancerResource.class.getName())), false));
        }

        ArrayList<HandlerChainCustomizer> chainCustomizers = new ArrayList<>();

        chainCustomizers.add(new KeycloakHandlerChainCustomizer());

        if (Configuration.isTrue(TracingOptions.TRACING_ENABLED)) {
            chainCustomizers.add(new KeycloakTracingCustomizer());
            // Exception handler is necessary to handle exceptions that are thrown by the bean methods,
            // otherwise the spans will not be closed.
            preExceptionMapperHandlerBuildItemBuildProducer
                    .produce(new PreExceptionMapperHandlerBuildItem(new KeycloakTracingCustomizer.EndHandler()));
        }

        scanner.produce(new MethodScannerBuildItem(new MethodScanner() {
            @Override
            public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
                    Map<String, Object> methodContext) {
                return chainCustomizers;
            }
        }));
    }

    @Consume(ProfileBuildItem.class)
    @Produce(CryptoProviderInitBuildItem.class)
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setCryptoProvider(KeycloakRecorder recorder) {
        FipsMode fipsMode = getOptionalValue(NS_KEYCLOAK_PREFIX + SecurityOptions.FIPS_MODE.getKey())
                .map(FipsMode::valueOfOption)
                .orElse(FipsMode.DISABLED);
        if (Profile.isFeatureEnabled(Profile.Feature.FIPS) && !fipsMode.isFipsEnabled()) {
            // default to non strict when fips feature enabled
            fipsMode = FipsMode.NON_STRICT;
        } else if (fipsMode.isFipsEnabled() && !Profile.isFeatureEnabled(Profile.Feature.FIPS)) {
            throw new RuntimeException("FIPS mode cannot be enabled without enabling the FIPS feature --features=fips");
        }

        recorder.setCryptoProvider(fipsMode);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void configureDevMode(BuildProducer<HotDeploymentWatchedFileBuildItem> hotFiles) {
        hotFiles.produce(new HotDeploymentWatchedFileBuildItem("META-INF/keycloak.conf"));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void configureProtoStreamSchemas(KeycloakRecorder recorder) {
        var schemas = ServiceLoader.load(SerializationContextInitializer.class).stream()
                .map(ServiceLoader.Provider::get)
                .toList();
        recorder.configureProtoStreamSchemas(schemas);
    }

    private Map<Spi, Map<Class<? extends Provider>, Map<String, ProviderFactory>>> loadFactories(
            Map<String, ProviderFactory> preConfiguredProviders) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        ProviderManager pm = getProviderManager(classLoader);
        Map<Spi, Map<Class<? extends Provider>, Map<String, ProviderFactory>>> factories = new HashMap<>();

        for (Spi spi : pm.loadSpis()) {
            Map<Class<? extends Provider>, Map<String, ProviderFactory>> providers = new HashMap<>();
            List<ProviderFactory> loadedFactories = new ArrayList<>();
            String provider = Config.getProvider(spi.getName());

            if (provider == null) {
                loadedFactories.addAll(pm.load(spi));
            } else {
                ProviderFactory factory = pm.load(spi, provider);

                if (factory != null) {
                    loadedFactories.add(factory);
                }
            }

            Map<String, ProviderFactory<?>> deployedScriptProviders = loadDeployedScriptProviders(classLoader, spi);

            loadedFactories.addAll(deployedScriptProviders.values());
            preConfiguredProviders.putAll(deployedScriptProviders);

            for (ProviderFactory<?> factory : loadedFactories) {
                if (IGNORED_PROVIDER_FACTORY.contains(factory.getClass())) {
                    continue;
                }

                Config.Scope scope = Config.scope(spi.getName(), factory.getId());

                if (isEnabled(factory, scope)) {
                    if (spi.isInternal() && !isInternal(factory)) {
                        ServicesLogger.LOGGER.spiMayChange(factory.getId(), factory.getClass().getName(), spi.getName());
                    }

                    providers.computeIfAbsent(spi.getProviderClass(), aClass -> new HashMap<>()).put(factory.getId(),
                            factory);
                } else {
                    logger.debugv("SPI {0} provider {1} disabled", spi.getName(), factory.getId());
                }
            }

            factories.put(spi, providers);
        }

        return factories;
    }

    private Map<String, ProviderFactory<?>> loadDeployedScriptProviders(ClassLoader classLoader, Spi spi) {
        Map<String, ProviderFactory<?>> providers = new HashMap<>();

        if (supportsDeployeableScripts(spi)) {
            try {
                Enumeration<URL> descriptorsUrls = classLoader.getResources(KEYCLOAK_SCRIPTS_JSON_PATH);

                while (descriptorsUrls.hasMoreElements()) {
                    URL url = descriptorsUrls.nextElement();
                    List<ScriptProviderDescriptor> descriptors = getScriptProviderDescriptorsFromJarFile(url);

                    if (!Environment.isDistribution()) {
                        // script providers are only loaded from classpath when running embedded
                        descriptors = new ArrayList<>(descriptors);
                        descriptors.addAll(getScriptProviderDescriptorsFromClassPath(url));
                    }

                    for (ScriptProviderDescriptor descriptor : descriptors) {
                        for (Entry<String, List<ScriptProviderMetadata>> entry : descriptor.getProviders().entrySet()) {
                            if (isScriptForSpi(spi, entry.getKey())) {
                                for (ScriptProviderMetadata metadata : entry.getValue()) {
                                    ProviderFactory<?> factory = DEPLOYEABLE_SCRIPT_PROVIDERS.get(entry.getKey()).apply(metadata);
                                    providers.put(metadata.getId(), factory);
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to discover script providers", e);
            }
        }

        return providers;
    }

    private List<ScriptProviderDescriptor> getScriptProviderDescriptorsFromClassPath(URL url) throws IOException {
        String file = url.getFile();

        if (!file.endsWith(".json")) {
            return List.of();
        }

        List<ScriptProviderDescriptor> descriptors = new ArrayList<>();

        try (InputStream is = url.openStream()) {
            ScriptProviderDescriptor descriptor = JsonSerialization.readValue(is, ScriptProviderDescriptor.class);

            configureScriptDescriptor(descriptor, fileName -> {
                // descriptor is at META-INF/
                Path basePath = Path.of(url.getPath()).getParent().getParent();

                try {
                    return basePath.resolve(fileName).toUri().toURL().openStream();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read script file from: " + fileName);
                }
            });
            descriptors.add(descriptor);
        }

        return descriptors;
    }

    private List<ScriptProviderDescriptor> getScriptProviderDescriptorsFromJarFile(URL url) throws IOException {
        String file = url.getFile();

        if (!file.contains(JAR_FILE_SEPARATOR)) {
            return List.of();
        }

        List<ScriptProviderDescriptor> descriptors = new ArrayList<>();

        try (JarFile jarFile = new JarFile(file.substring("file:".length(), file.indexOf(JAR_FILE_SEPARATOR)))) {
            JarEntry descriptorEntry = jarFile.getJarEntry(KEYCLOAK_SCRIPTS_JSON_PATH);

            try (InputStream is = jarFile.getInputStream(descriptorEntry)) {
                ScriptProviderDescriptor descriptor = JsonSerialization.readValue(is, ScriptProviderDescriptor.class);

                configureScriptDescriptor(descriptor, fileName -> {
                    try {
                        JarEntry scriptFile = jarFile.getJarEntry(fileName);
                        return jarFile.getInputStream(scriptFile);
                    } catch (IOException cause) {
                        throw new RuntimeException("Failed to read script file from file: " + fileName, cause);
                    }
                });

                descriptors.add(descriptor);
            }
        }

        return descriptors;
    }

    private static void configureScriptDescriptor(ScriptProviderDescriptor descriptor, Function<String, InputStream> jsFileLoader) throws IOException {
        for (List<ScriptProviderMetadata> metadatas : descriptor.getProviders().values()) {
            for (ScriptProviderMetadata metadata : metadatas) {
                String fileName = metadata.getFileName();

                if (fileName == null) {
                    throw new RuntimeException("You must provide the script file name");
                }

                try (InputStream in = jsFileLoader.apply(fileName)) {
                    metadata.setCode(StreamUtil.readString(in, StandardCharsets.UTF_8));
                }

                metadata.setId("script-" + fileName);

                String name = metadata.getName();

                if (name == null) {
                    name = fileName;
                }

                metadata.setName(name);
            }
        }
    }

    private boolean isScriptForSpi(Spi spi, String type) {
        if (spi instanceof ProtocolMapperSpi && (MAPPERS.equals(type) || SAML_MAPPERS.equals(type))) {
            return true;
        } else if (spi instanceof PolicySpi && POLICIES.equals(type)) {
            return true;
        } else if (spi instanceof AuthenticatorSpi && AUTHENTICATORS.equals(type)) {
            return true;
        }
        return false;
    }

    private boolean supportsDeployeableScripts(Spi spi) {
        return spi instanceof ProtocolMapperSpi || spi instanceof PolicySpi || spi instanceof AuthenticatorSpi;
    }

    private boolean isEnabled(ProviderFactory factory, Config.Scope scope) {
        if (!scope.getBoolean("enabled", true)) {
            return false;
        }
        if (factory instanceof EnvironmentDependentProviderFactory environmentDependentProviderFactory) {
            return environmentDependentProviderFactory.isSupported(scope);
        }
        return true;
    }

    private boolean isInternal(ProviderFactory<?> factory) {
        String packageName = factory.getClass().getPackage().getName();
        return packageName.startsWith("org.keycloak") && !packageName.startsWith("org.keycloak.examples");
    }

    private void checkProviders(Spi spi,
                                Map<Class<? extends Provider>, Map<String, ProviderFactory>> factoriesMap,
                                Map<Class<? extends Provider>, String> defaultProviders) {
        String provider = Config.getProvider(spi.getName());
        if (provider != null) {
            Map<String, ProviderFactory> map = factoriesMap.get(spi.getProviderClass());
            if (map == null || map.get(provider) == null) {
                throw new RuntimeException("Failed to find provider " + provider + " for " + spi.getName());
            }
            defaultProviders.put(spi.getProviderClass(), provider);
        } else {
            Map<String, ProviderFactory> factories = factoriesMap.get(spi.getProviderClass());
            String defaultProvider = DefaultKeycloakSessionFactory.resolveDefaultProvider(factories, spi);
            if (defaultProvider != null) {
                defaultProviders.put(spi.getProviderClass(), defaultProvider);
            }
        }
    }

    private static boolean isMetricsDisabled() {
        return !Configuration.isTrue(MetricsOptions.METRICS_ENABLED);
    }

    private static boolean isHealthDisabled() {
        return !Configuration.isTrue(HealthOptions.HEALTH_ENABLED);
    }

    static JdbcDataSourceBuildItem getDefaultDataSource(List<JdbcDataSourceBuildItem> jdbcDataSources) {
        for (JdbcDataSourceBuildItem jdbcDataSource : jdbcDataSources) {
            if (jdbcDataSource.isDefault()) {
                return jdbcDataSource;
            }
        }

        throw new RuntimeException("No default datasource found. The server datasource must be the default datasource.");
    }
}
