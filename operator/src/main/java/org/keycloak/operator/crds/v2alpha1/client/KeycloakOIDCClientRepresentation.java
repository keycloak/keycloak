package org.keycloak.operator.crds.v2alpha1.client;

import org.keycloak.representations.admin.v2.OIDCClientRepresentation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.fabric8.crd.generator.annotation.SchemaSwap;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.sundr.builder.annotations.Buildable;

@JsonTypeInfo(use = Id.NONE)
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder", lazyCollectionInitEnabled = false)
@SchemaSwap(fieldName = "auth", originalType = KeycloakOIDCClientRepresentation.class, targetType = KeycloakOIDCClientRepresentation.AuthWithSecretRef.class)
public class KeycloakOIDCClientRepresentation extends OIDCClientRepresentation {

    public static class AuthWithSecretRef extends OIDCClientRepresentation.Auth {

        private SecretKeySelector secretRef;

        @JsonPropertyDescription("Secret containing the client secret")
        public SecretKeySelector getSecretRef() {
            return secretRef;
        }

        public void setSecretRef(SecretKeySelector secretRef) {
            this.secretRef = secretRef;
        }

        @JsonIgnore
        @Override
        public String getSecret() {
            return super.getSecret();
        }

    }

    @JsonIgnore
    @Override
    public String getProtocol() {
        return super.getProtocol();
    }

    @JsonIgnore
    @Override
    public String getClientId() {
        return super.getClientId();
    }

}
