package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialsOffer {

    @JsonProperty("credential_issuer")
    private String credentialIssuer;

    //Either the id of a credential, offered in the issuer metadata or a supported credential object
    private List<Object> credentials;

    private PreAuthorizedGrant grants;

    public String getCredentialIssuer() {
        return credentialIssuer;
    }

    public CredentialsOffer setCredentialIssuer(String credentialIssuer) {
        this.credentialIssuer = credentialIssuer;
        return this;
    }

    public List<Object> getCredentials() {
        return credentials;
    }

    public CredentialsOffer setCredentials(List<Object> credentials) {
        this.credentials = credentials;
        return this;
    }

    public PreAuthorizedGrant getGrants() {
        return grants;
    }

    public CredentialsOffer setGrants(PreAuthorizedGrant grants) {
        this.grants = grants;
        return this;
    }
}