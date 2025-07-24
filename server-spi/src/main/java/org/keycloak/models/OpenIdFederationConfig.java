package org.keycloak.models;

import org.keycloak.models.enums.ClientRegistrationTypeEnum;
import org.keycloak.models.enums.EntityTypeEnum;

import java.io.Serializable;
import java.util.List;

public class OpenIdFederationConfig  implements Serializable {

    private String internalId;
    private String trustAnchor;
    private List<ClientRegistrationTypeEnum> clientRegistrationTypesSupported;

    private List<EntityTypeEnum> entityTypes;

    public OpenIdFederationConfig() {}

    public String getInternalId() {
        return internalId;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    public String getTrustAnchor() {
        return trustAnchor;
    }

    public void setTrustAnchor(String trustAnchor) {
        this.trustAnchor = trustAnchor;
    }

    public List<ClientRegistrationTypeEnum> getClientRegistrationTypesSupported() {
        return clientRegistrationTypesSupported;
    }

    public void setClientRegistrationTypesSupported(List<ClientRegistrationTypeEnum> clientRegistrationTypesSupported) {
        this.clientRegistrationTypesSupported = clientRegistrationTypesSupported;
    }

    public List<EntityTypeEnum> getEntityTypes() {
        return entityTypes;
    }

    public void setEntityTypes(List<EntityTypeEnum> entityTypes) {
        this.entityTypes = entityTypes;
    }
}
