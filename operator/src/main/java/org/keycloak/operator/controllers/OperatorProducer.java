/*
 Fork of the upstream class as a workaround for https://github.com/quarkiverse/quarkus-operator-sdk/issues/780
 So that we can associate the itemStore
 */

package org.keycloak.operator.controllers;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.config.BaseConfigurationService;
import io.javaoperatorsdk.operator.api.config.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.config.Utils;
import io.javaoperatorsdk.operator.api.config.dependent.DependentResourceConfigurationResolver;
import io.javaoperatorsdk.operator.api.config.dependent.DependentResourceSpec;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResourceFactory;
import io.quarkiverse.operatorsdk.runtime.QuarkusConfigurationService;
import io.quarkiverse.operatorsdk.runtime.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Singleton;

import static io.quarkiverse.operatorsdk.runtime.CRDUtils.applyCRD;

@Singleton
public class OperatorProducer {
    private static final Logger log = LoggerFactory.getLogger(OperatorProducer.class);

    /**
     * Produces an application-scoped Operator, given the provided configuration and
     * detected reconcilers. We previously produced the operator instance as
     * singleton-scoped but this prevents being able to inject the operator instance
     * in reconcilers (which we don't necessarily recommend but might be needed for
     * corner cases) due to an infinite loop. ApplicationScoped being proxy-based
     * allows for breaking the cycle, thus allowing the operator-reconciler
     * parent-child relation to be handled by CDI.
     *
     * @param configuration the {@link QuarkusConfigurationService} providing the
     *                      configuration for the operator and controllers
     * @param reconcilers   the detected {@link Reconciler} implementations
     * @return a properly configured {@link Operator} instance
     */
    @Produces
    @ApplicationScoped
    Operator operator(QuarkusConfigurationService configuration,
            Instance<Reconciler<? extends HasMetadata>> reconcilers) {
        if (configuration.getVersion() instanceof Version) {
            final var version = ((Version) configuration.getVersion());
            final var branch = !version.getExtensionBranch().equals(Version.UNKNOWN)
                    ? " on branch: " + version.getExtensionBranch()
                    : "";
            log.info("Quarkus Java Operator SDK extension {} (commit: {}{}) built on {}", version.getExtensionVersion(),
                    version.getExtensionCommit(), branch, version.getExtensionBuildTime());
        }

        // if some CRDs just got generated and need to be applied, apply them
        final var crdInfo = configuration.getCRDGenerationInfo();
        if (crdInfo.isApplyCRDs()) {
            for (String generatedCrdName : crdInfo.getGenerated()) {
                applyCRD(configuration.getKubernetesClient(), crdInfo, generatedCrdName);
            }
        }

        Operator operator = new Operator(new BaseConfigurationService() {
            @Override
            public KubernetesClient getKubernetesClient() {
                return configuration.getKubernetesClient();
            }

            @Override
            public DependentResourceFactory dependentResourceFactory() {
                return new DependentResourceFactory<ControllerConfiguration<?>>() {
                    @Override
                    public DependentResource createFrom(DependentResourceSpec spec,
                            ControllerConfiguration<?> configuration) {
                        final var dependentResourceClass = spec.getDependentResourceClass();
                        // workaround for https://github.com/operator-framework/java-operator-sdk/issues/2010
                        // create a fresh instance of the dependentresource
                        CDI.current().destroy(CDI.current().select(dependentResourceClass).get());
                        DependentResource instance = (DependentResource) CDI.current().select(dependentResourceClass).get();
                        var context = Utils.contextFor(configuration, dependentResourceClass, Dependent.class);
                        DependentResourceConfigurationResolver.configure(instance, spec, configuration);
                        return instance;
                    }
                };
            }
        });
        for (Reconciler<? extends HasMetadata> reconciler : reconcilers) {
            var quarkusConfig = configuration.getConfigurationFor(reconciler);
            operator.register(reconciler,
                    override -> override
                            .withItemStore(
                                    (reconciler instanceof WatchedSecretsController) ? new WatchedStore<>() : null)
                            .settingNamespaces(quarkusConfig.getNamespaces()));
        }

        return operator;
    }
}
