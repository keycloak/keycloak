package org.keycloak.operator.controllers;

import static org.keycloak.operator.crds.v2alpha1.CRDUtils.LEGACY_MANAGEMENT_ENABLED;
import static org.keycloak.operator.crds.v2alpha1.CRDUtils.METRICS_ENABLED;
import static org.keycloak.operator.crds.v2alpha1.CRDUtils.configuredOptions;
import static org.keycloak.operator.crds.v2alpha1.CRDUtils.isTlsConfigured;

import java.util.List;

import org.keycloak.operator.Constants;
import org.keycloak.operator.Utils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.ServiceMonitorSpec;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.openshift.api.model.monitoring.v1.NamespaceSelector;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitorBuilder;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

@KubernetesDependent(
      informer = @Informer(labelSelector = Constants.DEFAULT_LABELS_AS_STRING)
)
public class KeycloakServiceMonitorDependentResource extends CRUDKubernetesDependentResource<ServiceMonitor, Keycloak> {

    public static class ReconcilePrecondition implements Condition<Ingress, Keycloak> {
        @Override
        public boolean isMet(DependentResource<Ingress, Keycloak> dependentResource, Keycloak primary,
                             Context<Keycloak> context) {
            var opts = configuredOptions(primary);
            if (Boolean.parseBoolean(opts.get(LEGACY_MANAGEMENT_ENABLED)) || !Boolean.parseBoolean(opts.getOrDefault(METRICS_ENABLED, "false")))
                return false;

            return ServiceMonitorSpec.get(primary).isEnabled();
        }
    }

    @Override
    protected ServiceMonitor desired(Keycloak primary, Context<Keycloak> context) {
        var opts = configuredOptions(primary);
        var metricsPath = opts.getOrDefault("http-management-relative-path", "") + "/metrics";
        var meta = primary.getMetadata();
        var scheme = isTlsConfigured(primary) ? "https" : "http";
        var spec = ServiceMonitorSpec.get(primary);
        return new ServiceMonitorBuilder()
              .withNewMetadata()
                .withName(meta.getName())
                .withNamespace(meta.getNamespace())
              .endMetadata()
              .withNewSpec()
                .withNewNamespaceSelector()
                  .addToMatchNames(meta.getNamespace())
                .endNamespaceSelector()
                .withNewSelector()
                  .addToMatchLabels(Utils.allInstanceLabels(primary))
                .endSelector()
                .addNewEndpoint()
                  .withInterval(spec.getInterval())
                  .withPath(metricsPath)
                  .withPort(Constants.KEYCLOAK_MANAGEMENT_PORT_NAME)
                  .withScheme(scheme)
                  .withScrapeTimeout(spec.getScrapeTimeout())
                  .withNewTlsConfig()
                    .withInsecureSkipVerify(true)
                  .endTlsConfig()
                .endEndpoint()
              .endSpec()
              .build();
    }
}
