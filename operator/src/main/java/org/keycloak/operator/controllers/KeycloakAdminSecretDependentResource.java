package org.keycloak.operator.controllers;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.api.reconciler.dependent.GarbageCollected;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

import org.keycloak.operator.Constants;
import org.keycloak.operator.Utils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.BootstrapAdminSpec;

import java.util.Optional;
import java.util.UUID;

@KubernetesDependent(labelSelector = Constants.DEFAULT_LABELS_AS_STRING, resourceDiscriminator = KeycloakAdminSecretDependentResource.NameResourceDiscriminator.class)
public class KeycloakAdminSecretDependentResource extends KubernetesDependentResource<Secret, Keycloak> implements Creator<Secret, Keycloak>, GarbageCollected<Keycloak> {

    public static class EnabledCondition implements Condition<Ingress, Keycloak> {
        @Override
        public boolean isMet(DependentResource<Ingress, Keycloak> dependentResource, Keycloak primary,
                Context<Keycloak> context) {
            return Optional.ofNullable(primary.getSpec().getBootstrapAdminSpec()).map(BootstrapAdminSpec::getUser)
                    .map(BootstrapAdminSpec.User::getSecret).filter(s -> s.equals(KeycloakAdminSecretDependentResource.getName(primary))).isPresent();
        }
    }

    public static class NameResourceDiscriminator implements ResourceDiscriminator<Secret, Keycloak> {
        @Override
        public Optional<Secret> distinguish(Class<Secret> resource, Keycloak primary, Context<Keycloak> context) {
            return Utils.getByName(Secret.class, KeycloakAdminSecretDependentResource::getName, primary, context);
        }
    }

    public KeycloakAdminSecretDependentResource() {
        super(Secret.class);
    }

    @Override
    protected Secret desired(Keycloak primary, Context<Keycloak> context) {
        return new SecretBuilder()
                .withNewMetadata()
                .withName(getName(primary))
                .addToLabels(Utils.allInstanceLabels(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .endMetadata()
                .withType("Opaque")
                .withType("kubernetes.io/basic-auth")
                .addToData("username", Utils.asBase64("temp-admin"))
                .addToData("password", Utils.asBase64(UUID.randomUUID().toString().replace("-", "")))
                .build();
    }

    public static String getName(Keycloak keycloak) {
        return KubernetesResourceUtil.sanitizeName(keycloak.getMetadata().getName() + "-initial-admin");
    }

}
