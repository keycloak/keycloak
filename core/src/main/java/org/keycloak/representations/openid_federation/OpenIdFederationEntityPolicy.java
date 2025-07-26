package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class OpenIdFederationEntityPolicy {

    @JsonProperty("federation_api_endpoint")
    private Policy<String> federationApiEndpoint;

    @JsonProperty("federation_list_endpoint")
    private Policy<String> federationListEndpoint;

    @JsonProperty("federation_resolve_endpoint")
    private Policy<String> federationResolveEndpoint;

    @JsonProperty("federation_trust_mark_status_endpoint")
    private Policy<String> federationTrustMarkStatusEndpoint;

    @JsonProperty("federation_trust_mark_list_endpoint")
    private Policy<String> federationTrustMarkListEndpoint;

    @JsonProperty("federation_trust_mark_endpoint")
    private Policy<String> federationTrustMarkEndpoint;

    @JsonProperty("federation_historical_keys_endpoint")
    private Policy<String> federationHistoricalKeysEndpoint;

    @JsonProperty("endpoint_auth_signing_alg_values_supported")
    private PolicyList<String> endpointAuthSigningAlgValuesSupported;

    @JsonUnwrapped
    private CommonMetadataPolicy commonMetadataPolicy;

    public Policy<String> getFederationApiEndpoint() {
        return federationApiEndpoint;
    }

    public void setFederationApiEndpoint(Policy<String> federationApiEndpoint) {
        this.federationApiEndpoint = federationApiEndpoint;
    }

    public Policy<String> getFederationListEndpoint() {
        return federationListEndpoint;
    }

    public void setFederationListEndpoint(Policy<String> federationListEndpoint) {
        this.federationListEndpoint = federationListEndpoint;
    }

    public Policy<String> getFederationResolveEndpoint() {
        return federationResolveEndpoint;
    }

    public void setFederationResolveEndpoint(Policy<String> federationResolveEndpoint) {
        this.federationResolveEndpoint = federationResolveEndpoint;
    }

    public Policy<String> getFederationTrustMarkStatusEndpoint() {
        return federationTrustMarkStatusEndpoint;
    }

    public void setFederationTrustMarkStatusEndpoint(Policy<String> federationTrustMarkStatusEndpoint) {
        this.federationTrustMarkStatusEndpoint = federationTrustMarkStatusEndpoint;
    }

    public Policy<String> getFederationTrustMarkListEndpoint() {
        return federationTrustMarkListEndpoint;
    }

    public void setFederationTrustMarkListEndpoint(Policy<String> federationTrustMarkListEndpoint) {
        this.federationTrustMarkListEndpoint = federationTrustMarkListEndpoint;
    }

    public Policy<String> getFederationTrustMarkEndpoint() {
        return federationTrustMarkEndpoint;
    }

    public void setFederationTrustMarkEndpoint(Policy<String> federationTrustMarkEndpoint) {
        this.federationTrustMarkEndpoint = federationTrustMarkEndpoint;
    }

    public Policy<String> getFederationHistoricalKeysEndpoint() {
        return federationHistoricalKeysEndpoint;
    }

    public void setFederationHistoricalKeysEndpoint(Policy<String> federationHistoricalKeysEndpoint) {
        this.federationHistoricalKeysEndpoint = federationHistoricalKeysEndpoint;
    }

    public PolicyList<String> getEndpointAuthSigningAlgValuesSupported() {
        return endpointAuthSigningAlgValuesSupported;
    }

    public void setEndpointAuthSigningAlgValuesSupported(PolicyList<String> endpointAuthSigningAlgValuesSupported) {
        this.endpointAuthSigningAlgValuesSupported = endpointAuthSigningAlgValuesSupported;
    }

    public CommonMetadataPolicy getCommonMetadataPolicy() {
        return commonMetadataPolicy;
    }

    public void setCommonMetadataPolicy(CommonMetadataPolicy commonMetadataPolicy) {
        this.commonMetadataPolicy = commonMetadataPolicy;
    }
}
