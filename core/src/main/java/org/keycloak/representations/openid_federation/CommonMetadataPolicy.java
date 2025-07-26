package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.jose.jwk.JSONWebKeySet;

import java.util.List;

public class CommonMetadataPolicy {

    @JsonProperty("signed_jwks_uri")
    private  Policy<String> signedJwksUri;

    @JsonProperty("jwks_uri")
    private  Policy<String> jwksUri;

    protected  Policy<JSONWebKeySet> jwks;

    @JsonProperty("organization_name")
    private  Policy<String> organizationName;

    private  PolicyList<String> contacts;
    private  Policy<String> logoUri;
    private  Policy<String> policyUri;
    private  Policy<String> homepageUri;

    public Policy<String> getSignedJwksUri() {
        return signedJwksUri;
    }

    public void setSignedJwksUri(Policy<String> signedJwksUri) {
        this.signedJwksUri = signedJwksUri;
    }

    public Policy<String> getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(Policy<String> jwksUri) {
        this.jwksUri = jwksUri;
    }

    public Policy<JSONWebKeySet> getJwks() {
        return jwks;
    }

    public void setJwks(Policy<JSONWebKeySet> jwks) {
        this.jwks = jwks;
    }

    public Policy<String> getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(Policy<String> organizationName) {
        this.organizationName = organizationName;
    }

    public PolicyList<String> getContacts() {
        return contacts;
    }

    public void setContacts(PolicyList<String> contacts) {
        this.contacts = contacts;
    }

    public Policy<String> getLogoUri() {
        return logoUri;
    }

    public void setLogoUri(Policy<String> logoUri) {
        this.logoUri = logoUri;
    }

    public Policy<String> getPolicyUri() {
        return policyUri;
    }

    public void setPolicyUri(Policy<String> policyUri) {
        this.policyUri = policyUri;
    }

    public Policy<String> getHomepageUri() {
        return homepageUri;
    }

    public void setHomepageUri(Policy<String> homepageUri) {
        this.homepageUri = homepageUri;
    }
}
