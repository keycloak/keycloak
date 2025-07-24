package org.keycloak.models;


import java.io.Serializable;

public class OpenIdFederationConfig  implements Serializable {

    private String internalId;
    private String trustAnchor;

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


}
