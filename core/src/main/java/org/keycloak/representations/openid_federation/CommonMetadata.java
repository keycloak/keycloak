package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CommonMetadata {

    @JsonProperty("signed_jwks_uri")
    private String signedJwksUri;

    @JsonProperty("organization_name")
    private String organizationName;

    @JsonProperty("organization_uri")
    private String organizationUri;

    public String getSignedJwksUri() {
        return signedJwksUri;
    }

    public void setSignedJwksUri(String signedJwksUri) {
        this.signedJwksUri = signedJwksUri;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationUri() {
        return organizationUri;
    }

    public void setOrganizationUri(String organizationUri) {
        this.organizationUri = organizationUri;
    }
}
