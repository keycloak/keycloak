package org.keycloak.operator.crds.v2alpha1.client;

import org.keycloak.representations.admin.v2.SAMLClientRepresentation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.sundr.builder.annotations.Buildable;

@JsonTypeInfo(use = Id.NONE)
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder", lazyCollectionInitEnabled = false)
public class KeycloakSAMLClientRepresentation extends SAMLClientRepresentation {

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
