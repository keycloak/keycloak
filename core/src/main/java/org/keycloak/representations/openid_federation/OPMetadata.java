package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;

import java.util.List;

public class OPMetadata extends OIDCConfigurationRepresentation {

    @JsonProperty("client_registration_types")
    private List<String> clientRegistrationTypes;

    @JsonProperty("federation_registration_endpoint")
    private String federationRegistrationEndpoint;

    private List<String> contacts;

    @JsonProperty("logo_uri")
    private String logoUri;

    @JsonProperty("policy_uri")
    private String policyUri;

    @JsonUnwrapped
    private CommonMetadata commonMetadata;

    public List<String> getClientRegistrationTypes() {
        return clientRegistrationTypes;
    }

    public void setClientRegistrationTypes(List<String> clientRegistrationTypes) {
        this.clientRegistrationTypes = clientRegistrationTypes;
    }

    public String getFederationRegistrationEndpoint() {
        return federationRegistrationEndpoint;
    }

    public void setFederationRegistrationEndpoint(String federationRegistrationEndpoint) {
        this.federationRegistrationEndpoint = federationRegistrationEndpoint;
    }

    public List<String> getContacts() {
        return contacts;
    }

    public void setContacts(List<String> contacts) {
        this.contacts = contacts;
    }

    public String getLogoUri() {
        return logoUri;
    }

    public void setLogoUri(String logoUri) {
        this.logoUri = logoUri;
    }

    public String getPolicyUri() {
        return policyUri;
    }

    public void setPolicyUri(String policyUri) {
        this.policyUri = policyUri;
    }

    public CommonMetadata getCommonMetadata() {
        return commonMetadata;
    }

    public void setCommonMetadata(CommonMetadata commonMetadata) {
        this.commonMetadata = commonMetadata;
    }

}
