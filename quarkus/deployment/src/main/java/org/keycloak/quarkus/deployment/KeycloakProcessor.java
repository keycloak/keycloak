package org.keycloak.quarkus.deployment;

import javax.persistence.spi.PersistenceUnitTransactionType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.keycloak.Config;
import org.keycloak.connections.jpa.DefaultJpaConnectionProviderFactory;
import org.keycloak.connections.jpa.updater.liquibase.LiquibaseJpaUpdaterProviderFactory;
import org.keycloak.connections.jpa.updater.liquibase.conn.DefaultLiquibaseConnectionProvider;
import org.keycloak.provider.KeycloakDeploymentInfo;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderManager;
import org.keycloak.provider.Spi;
import org.keycloak.provider.quarkus.QuarkusRequestFilter;
import org.keycloak.runtime.KeycloakRecorder;
import org.keycloak.transaction.JBossJtaTransactionManagerLookup;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import org.keycloak.util.Environment;

class KeycloakProcessor {

    @BuildStep
    FeatureBuildItem getFeature() {
        return new FeatureBuildItem("keycloak");
    }

    /**
     * <p>Configures the persistence unit for Quarkus.
     * 
     * <p>The main reason we have this build step is because we re-use the same persistence unit from {@code keycloak-model-jpa} 
     * module, the same used by the Wildfly distribution. The {@code hibernate-orm} extension expects that the dialect is statically
     * set to the persistence unit if there is any from the classpath and use this method to obtain the dialect from the configuration
     * file so that we can re-augment the application with whatever dialect we want. In addition to the dialect, we should also be 
     * allowed to set any additional defaults that we think that makes sense.
     * 
     * @param recorder
     * @param config
     * @param descriptors
     */
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void configureHibernate(KeycloakRecorder recorder, HibernateOrmConfig config, List<PersistenceUnitDescriptorBuildItem> descriptors) {
        PersistenceUnitDescriptor unit = descriptors.get(0).asOutputPersistenceUnitDefinition().getActualHibernateDescriptor();
        
        unit.getProperties().setProperty(AvailableSettings.DIALECT, config.dialect.get());
        unit.getProperties().setProperty(AvailableSettings.JPA_TRANSACTION_TYPE, PersistenceUnitTransactionType.JTA.name());
        unit.getProperties().setProperty(AvailableSettings.QUERY_STARTUP_CHECKING, Boolean.FALSE.toString());
    }

    /**
     * <p>Load the built-in provider factories during build time so we don't spend time looking up them at runtime.
     * 
     * <p>User-defined providers are going to be loaded at startup</p>
     * 
     * @param recorder
     */
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void configureProviders(KeycloakRecorder recorder) {
        recorder.configSessionFactory(loadFactories(), Environment.isRebuild());
    }

    @BuildStep
    void initializeRouter(BuildProducer<FilterBuildItem> routes) {
        routes.produce(new FilterBuildItem(new QuarkusRequestFilter(), FilterBuildItem.AUTHORIZATION - 10));
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void configureDevMode(BuildProducer<HotDeploymentWatchedFileBuildItem> hotFiles) {
        hotFiles.produce(new HotDeploymentWatchedFileBuildItem("META-INF/keycloak.properties"));
    }

    private Map<Spi, Set<Class<? extends ProviderFactory>>> loadFactories() {
        ProviderManager pm = new ProviderManager(
                KeycloakDeploymentInfo.create().services(), new BuildClassLoader(),
                Config.scope().getArray("providers"));
        Map<Spi, Set<Class<? extends ProviderFactory>>> result = new HashMap<>();

        for (Spi spi : pm.loadSpis()) {
            Set<Class<? extends ProviderFactory>> factories = new HashSet<>();

            for (ProviderFactory factory : pm.load(spi)) {
                if (Arrays.asList(
                        JBossJtaTransactionManagerLookup.class,
                        DefaultJpaConnectionProviderFactory.class,
                        DefaultLiquibaseConnectionProvider.class,
                        LiquibaseJpaUpdaterProviderFactory.class).contains(factory.getClass())) {
                    continue;
                }

                factories.add(factory.getClass());
            }

            result.put(spi, factories);
        }

        return result;
    }
}
