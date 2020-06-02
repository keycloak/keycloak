package org.keycloak.quarkus.deployment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.keycloak.Config;
import org.keycloak.connections.jpa.DefaultJpaConnectionProviderFactory;
import org.keycloak.connections.jpa.DelegatingDialect;
import org.keycloak.connections.jpa.updater.liquibase.LiquibaseJpaUpdaterProviderFactory;
import org.keycloak.connections.jpa.updater.liquibase.conn.DefaultLiquibaseConnectionProvider;
import org.keycloak.provider.KeycloakDeploymentInfo;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderManager;
import org.keycloak.provider.Spi;
import org.keycloak.runtime.KeycloakRecorder;
import org.keycloak.transaction.JBossJtaTransactionManagerLookup;

import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;

class KeycloakProcessor {

    @BuildStep
    FeatureBuildItem getFeature() {
        return new FeatureBuildItem("keycloak");
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void configureHibernate(KeycloakRecorder recorder, List<PersistenceUnitDescriptorBuildItem> descriptors) {
        // TODO: ORM extension is going to provide build items that we can rely on to create our own PU instead of relying
        // on the parsed descriptor and assume that the order that build steps are executed is always the same (although dialect 
        // is only created during runtime)
        ParsedPersistenceXmlDescriptor unit = descriptors.get(0).getDescriptor();
        unit.setTransactionType(PersistenceUnitTransactionType.JTA);
        unit.getProperties().setProperty(AvailableSettings.DIALECT, DelegatingDialect.class.getName());
        unit.getProperties().setProperty(AvailableSettings.QUERY_STARTUP_CHECKING, Boolean.FALSE.toString());
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void configureDataSource(KeycloakRecorder recorder, BuildProducer<BeanContainerListenerBuildItem> container) {
        container.produce(new BeanContainerListenerBuildItem(recorder.configureDataSource()));
    }

    /**
     * <p>Load the built-in provider factories during build time so we don't spend time looking up them at runtime.
     * 
     * <p>User-defined providers are going to be loaded at startup</p>
     */
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void configureBuiltInProviders(KeycloakRecorder recorder) {
        recorder.configSessionFactory(loadBuiltInFactories());
    }

    private Map<Spi, Set<Class<? extends ProviderFactory>>> loadBuiltInFactories() {
        ProviderManager pm = new ProviderManager(
                KeycloakDeploymentInfo.create().services(), Thread.currentThread().getContextClassLoader(), Config.scope().getArray("providers"));
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
