package org.keycloak.operator.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImport;

import java.util.Optional;

public class KeycloakRealmImportSecret extends OperatorManagedResource {

    private final KeycloakRealmImport realmCR;
    private final String secretName;
    private final ObjectMapper jsonMapper;

    public KeycloakRealmImportSecret(KubernetesClient client, KeycloakRealmImport realmCR, ObjectMapper jsonMapper) {
        super(client, realmCR);
        this.realmCR = realmCR;
        this.jsonMapper = jsonMapper;
        this.secretName = KubernetesResourceUtil.sanitizeName(getName() + "-" + realmCR.getSpec().getRealm().getRealm() + "-realm");
    }

    @Override
    protected Optional<HasMetadata> getReconciledResource() {
        return Optional.of(createSecret());
    }

    private Secret createSecret() {
        var fileName = getRealmName() + "-realm.json";
        var content = "";
        try {
            content = jsonMapper.writeValueAsString(this.realmCR.getSpec().getRealm());
        } catch (JsonProcessingException cause) {
            throw new RuntimeException("Failed to read the Realm Representation", cause);
        }

        return new SecretBuilder()
                .withNewMetadata()
                .withName(secretName)
                .withNamespace(getNamespace())
                .endMetadata()
                .addToStringData(fileName, content)
                .build();
    }

    @Override
    protected String getName() {
        return realmCR.getMetadata().getName();
    }

    private String getRealmName() { return realmCR.getSpec().getRealm().getRealm(); }

    public String getSecretName() {
        return secretName;
    }
}
