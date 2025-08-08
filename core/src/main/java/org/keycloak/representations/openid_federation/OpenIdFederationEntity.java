package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.keycloak.jose.jwk.JSONWebKeySet;

import java.util.List;

public class OpenIdFederationEntity {

    @JsonProperty("federation_fetch_endpoint")
    private String federationFetchEndpoint;

    @JsonProperty("federation_list_endpoint")
    private String federationListEndpoint;

    @JsonProperty("federation_resolve_endpoint")
    private String federationResolveEndpoint;

    @JsonProperty("federation_trust_mark_status_endpoint")
    private String federationTrustMarkStatusEndpoint;

    @JsonProperty("federation_trust_mark_list_endpoint")
    private String federationTrustMarkListEndpoint;

    @JsonProperty("federation_trust_mark_endpoint")
    private String federationTrustMarkEndpoint;

    @JsonProperty("federation_historical_keys_endpoint")
    private String federationHistoricalKeysEndpoint;

    @JsonProperty("endpoint_auth_signing_alg_values_supported")
    private List<String> endpointAuthSigningAlgValuesSupported;

    @JsonProperty("jwks_uri")
    private String jwksUri;

    protected JSONWebKeySet jwks;

    private List<String> contacts;

    @JsonProperty("logo_uri")
    private String logoUri;

    @JsonProperty("policy_uri")
    private String policyUri;

    @JsonUnwrapped
    private CommonMetadata commonMetadata;

    public String getFederationFetchEndpoint() {
        return federationFetchEndpoint;
    }

    public void setFederationFetchEndpoint(String federationFetchEndpoint) {
        this.federationFetchEndpoint = federationFetchEndpoint;
    }

    public String getFederationListEndpoint() {
        return federationListEndpoint;
    }

    public void setFederationListEndpoint(String federationListEndpoint) {
        this.federationListEndpoint = federationListEndpoint;
    }

    public String getFederationResolveEndpoint() {
        return federationResolveEndpoint;
    }

    public void setFederationResolveEndpoint(String federationResolveEndpoint) {
        this.federationResolveEndpoint = federationResolveEndpoint;
    }

    public String getFederationTrustMarkStatusEndpoint() {
        return federationTrustMarkStatusEndpoint;
    }

    public void setFederationTrustMarkStatusEndpoint(String federationTrustMarkStatusEndpoint) {
        this.federationTrustMarkStatusEndpoint = federationTrustMarkStatusEndpoint;
    }

    public String getFederationTrustMarkListEndpoint() {
        return federationTrustMarkListEndpoint;
    }

    public void setFederationTrustMarkListEndpoint(String federationTrustMarkListEndpoint) {
        this.federationTrustMarkListEndpoint = federationTrustMarkListEndpoint;
    }

    public String getFederationTrustMarkEndpoint() {
        return federationTrustMarkEndpoint;
    }

    public void setFederationTrustMarkEndpoint(String federationTrustMarkEndpoint) {
        this.federationTrustMarkEndpoint = federationTrustMarkEndpoint;
    }

    public String getFederationHistoricalKeysEndpoint() {
        return federationHistoricalKeysEndpoint;
    }

    public void setFederationHistoricalKeysEndpoint(String federationHistoricalKeysEndpoint) {
        this.federationHistoricalKeysEndpoint = federationHistoricalKeysEndpoint;
    }

    public List<String> getEndpointAuthSigningAlgValuesSupported() {
        return endpointAuthSigningAlgValuesSupported;
    }

    public void setEndpointAuthSigningAlgValuesSupported(List<String> endpointAuthSigningAlgValuesSupported) {
        this.endpointAuthSigningAlgValuesSupported = endpointAuthSigningAlgValuesSupported;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public JSONWebKeySet getJwks() {
        return jwks;
    }

    public void setJwks(JSONWebKeySet jwks) {
        this.jwks = jwks;
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
