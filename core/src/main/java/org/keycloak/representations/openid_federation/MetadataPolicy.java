package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MetadataPolicy {

    @JsonProperty("openid_provider")
    private OPMetadataPolicy openIdProviderMetadataPolicy;

    @JsonProperty("federation_entity")
    private OpenIdFederationEntityPolicy federationEntityPolicy;

    @JsonProperty("openid_relying_party")
    private RPMetadataPolicy relyingPartyMetadataPolicy;

    public OPMetadataPolicy getOpenIdProviderMetadataPolicy() {
        return openIdProviderMetadataPolicy;
    }

    public void setOpenIdProviderMetadataPolicy(OPMetadataPolicy openIdProviderMetadataPolicy) {
        this.openIdProviderMetadataPolicy = openIdProviderMetadataPolicy;
    }

    public OpenIdFederationEntityPolicy getFederationEntityPolicy() {
        return federationEntityPolicy;
    }

    public void setFederationEntityPolicy(OpenIdFederationEntityPolicy federationEntityPolicy) {
        this.federationEntityPolicy = federationEntityPolicy;
    }

    public RPMetadataPolicy getRelyingPartyMetadataPolicy() {
        return relyingPartyMetadataPolicy;
    }

    public void setRelyingPartyMetadataPolicy(RPMetadataPolicy relyingPartyMetadataPolicy) {
        this.relyingPartyMetadataPolicy = relyingPartyMetadataPolicy;
    }
}
