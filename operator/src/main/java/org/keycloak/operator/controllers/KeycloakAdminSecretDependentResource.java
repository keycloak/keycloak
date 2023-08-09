package org.keycloak.operator.controllers;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.GarbageCollected;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;

import org.keycloak.operator.Constants;
import org.keycloak.operator.Utils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;

import java.util.UUID;

@KubernetesDependent(labelSelector = Constants.DEFAULT_LABELS_AS_STRING)
public class KeycloakAdminSecretDependentResource extends KubernetesDependentResource<Secret, Keycloak> implements Creator<Secret, Keycloak>, GarbageCollected<Keycloak> {

    public KeycloakAdminSecretDependentResource() {
        super(Secret.class);
    }

    @Override
    protected Secret desired(Keycloak primary, Context<Keycloak> context) {
        return new SecretBuilder()
                .withNewMetadata()
                .withName(getName(primary))
                .addToLabels(OperatorManagedResource.allInstanceLabels(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .endMetadata()
                .withType("kubernetes.io/basic-auth")
                .addToData("username", Utils.asBase64("admin"))
                .addToData("password", Utils.asBase64(UUID.randomUUID().toString().replace("-", "")))
                .build();
    }

    public static String getName(Keycloak keycloak) {
        return KubernetesResourceUtil.sanitizeName(keycloak.getMetadata().getName() + "-initial-admin");
    }

}
