package org.keycloak.quarkus.deployment;

import java.util.List;

import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.keycloak.runtime.KeycloakRecorder;

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
        ParsedPersistenceXmlDescriptor unit = descriptors.get(0).getDescriptor();
        unit.setTransactionType(PersistenceUnitTransactionType.JTA);
        unit.getProperties().setProperty(AvailableSettings.DIALECT,
                KeycloakRecorder.CONFIG.getRawValue("quarkus.datasource.dialect"));
    }
}
