package org.keycloak.operator.controllers;

import static org.keycloak.operator.controllers.KeycloakDeploymentDependentResource.managementEndpoint;
import static org.keycloak.operator.crds.v2alpha1.CRDUtils.LEGACY_MANAGEMENT_ENABLED;
import static org.keycloak.operator.crds.v2alpha1.CRDUtils.METRICS_ENABLED;
import static org.keycloak.operator.crds.v2alpha1.CRDUtils.configuredOptions;

import org.keycloak.operator.Constants;
import org.keycloak.operator.Utils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.ServiceMonitorSpec;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
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
            if (Boolean.parseBoolean(opts.get(LEGACY_MANAGEMENT_ENABLED)) || !Boolean.parseBoolean(opts.getOrDefault(METRICS_ENABLED, "false"))) {
                return false;
            }

            return ServiceMonitorSpec.get(primary).isEnabled();
        }
    }

    @Override
    protected ServiceMonitor desired(Keycloak primary, Context<Keycloak> context) {
        var endpoint = managementEndpoint(primary, context, true);
        var meta = primary.getMetadata();
        var spec = ServiceMonitorSpec.get(primary);
        return new ServiceMonitorBuilder()
              .withNewMetadata()
                .withName(meta.getName())
                .withNamespace(meta.getNamespace())
                .withLabels(Utils.allInstanceLabels(primary))
              .endMetadata()
              .withNewSpec()
                .withNewNamespaceSelector()
                  .addToMatchNames(meta.getNamespace())
                .endNamespaceSelector()
                .withNewSelector()
                  .addToMatchLabels(Utils.allInstanceLabels(primary))
                .endSelector()
                .withScrapeProtocols("OpenMetricsText1.0.0")
                .addNewEndpoint()
                  .withInterval(spec.getInterval())
                  .withPath(endpoint.relativePath() + "metrics")
                  .withPort(Constants.KEYCLOAK_MANAGEMENT_PORT_NAME)
                  .withScheme(endpoint.protocol().toLowerCase())
                  .withScrapeTimeout(spec.getScrapeTimeout())
                  .withNewTlsConfig()
                    .withInsecureSkipVerify(true)
                  .endTlsConfig()
                .endEndpoint()
              .endSpec()
              .build();
    }
}
