package org.keycloak.operator.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.keycloak.operator.Constants;
import org.keycloak.operator.Utils;
import org.keycloak.operator.crds.v2beta1.deployment.Keycloak;
import org.keycloak.operator.crds.v2beta1.deployment.spec.ServiceAccountSpec;

import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

@KubernetesDependent(
        informer = @Informer(labelSelector = Constants.DEFAULT_LABELS_AS_STRING)
)
public class KeycloakServiceAccountDependentResource
        extends CRUDKubernetesDependentResource<ServiceAccount, Keycloak> {

    public KeycloakServiceAccountDependentResource() {
        super(ServiceAccount.class);
    }

    public static class EnabledCondition implements Condition<ServiceAccount, Keycloak> {
        @Override
        public boolean isMet(DependentResource<ServiceAccount, Keycloak> dependentResource,
                             Keycloak primary, Context<Keycloak> context) {
            return primary.getSpec().getServiceAccountSpec() != null;
        }
    }

    @Override
    public ServiceAccount desired(Keycloak primary, Context<Keycloak> context) {
        var optionalSpec = Optional.ofNullable(primary.getSpec().getServiceAccountSpec());
        Map<String,String> annotations = optionalSpec.map(ServiceAccountSpec::getAnnotations).orElse(new HashMap<>());
        List<LocalObjectReference> imagePullSecrets = optionalSpec.map(ServiceAccountSpec::getImagePullSecrets).orElse(null);


        ServiceAccountBuilder builder = new ServiceAccountBuilder()
                .withNewMetadata()
                .withName(primary.getMetadata().getName())
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(Utils.allInstanceLabels(primary))
                .withAnnotations(annotations)
                .endMetadata();

        if (imagePullSecrets != null) {
            builder.withImagePullSecrets(imagePullSecrets);
        }

        return builder.build();
    }
}
