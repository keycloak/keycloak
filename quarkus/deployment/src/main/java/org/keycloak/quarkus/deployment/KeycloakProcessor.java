package org.keycloak.quarkus.deployment;

import java.util.List;

import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.keycloak.connections.jpa.DelegatingDialect;

import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import org.keycloak.runtime.KeycloakRecorder;

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
    }
    
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void configureDataSource(KeycloakRecorder recorder, BuildProducer<BeanContainerListenerBuildItem> container) {
        container.produce(new BeanContainerListenerBuildItem(recorder.configureDataSource()));
    }
}
