package org.keycloak.operator.crds.v2alpha1.client;

import org.keycloak.representations.admin.v2.OIDCClientRepresentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;

import io.fabric8.crd.generator.annotation.SchemaSwap;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder", lazyCollectionInitEnabled = false)
public class KeycloakOIDCClientRepresentation extends OIDCClientRepresentation {

    // hide the secret field for CRD generation
    @SchemaSwap(originalType = AuthWithSecretRef.class, fieldName = "secret")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class AuthWithSecretRef extends OIDCClientRepresentation.Auth {

        private SecretKeySelector secretRef;

        public SecretKeySelector getSecretRef() {
            return secretRef;
        }

        public void setSecretRef(SecretKeySelector secretRef) {
            this.secretRef = secretRef;
        }

    }

    @Override
    public AuthWithSecretRef getAuth() {
        return (AuthWithSecretRef)super.getAuth();
    }

    @Override
    public void setAuth(OIDCClientRepresentation.Auth auth) {
        throw new IllegalArgumentException();
    }

    @JsonSetter
    public void setAuth(AuthWithSecretRef auth) {
        super.setAuth(auth);
    }

}
