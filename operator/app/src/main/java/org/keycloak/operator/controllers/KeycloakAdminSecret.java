package org.keycloak.operator.controllers;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;

import java.util.Optional;
import java.util.UUID;

public class KeycloakAdminSecret extends OperatorManagedResource {

    private final String secretName;

    public KeycloakAdminSecret(KubernetesClient client, Keycloak keycloak) {
        super(client, keycloak);
        this.secretName = KubernetesResourceUtil.sanitizeName(keycloak.getMetadata().getName() + "-initial-admin");
    }

    @Override
    protected Optional<HasMetadata> getReconciledResource() {
        if (client.secrets().inNamespace(getNamespace()).withName(secretName).get() != null) {
            return Optional.empty();
        } else {
            return Optional.of(createSecret());
        }
    }

    private Secret createSecret() {
        return new SecretBuilder()
                .withNewMetadata()
                .withName(secretName)
                .withNamespace(getNamespace())
                .endMetadata()
                .withType("kubernetes.io/basic-auth")
                .addToStringData("username", "admin")
                .addToStringData("password", UUID.randomUUID().toString().replace("-", ""))
                .build();
    }

    @Override
    public String getName() { return secretName; }

}
