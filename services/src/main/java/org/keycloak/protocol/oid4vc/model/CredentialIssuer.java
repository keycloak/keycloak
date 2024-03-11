package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URL;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialIssuer {

    @JsonProperty("credential_issuer")
    private String credentialIssuer;

    @JsonProperty("credential_endpoint")
    private String credentialEndpoint;

    @JsonProperty("batch_credential_endpoint")
    private String batchCredentialEndpoint;

    @JsonProperty("credentials_supported")
    private Map<String, SupportedCredential> credentialsSupported;

    private DisplayObject display;

    public String getCredentialIssuer() {
        return credentialIssuer;
    }

    public CredentialIssuer setCredentialIssuer(String credentialIssuer) {
        this.credentialIssuer = credentialIssuer;
        return this;
    }

    public String getCredentialEndpoint() {
        return credentialEndpoint;
    }

    public CredentialIssuer setCredentialEndpoint(String credentialEndpoint) {
        this.credentialEndpoint = credentialEndpoint;
        return this;
    }

    public String getBatchCredentialEndpoint() {
        return batchCredentialEndpoint;
    }

    public CredentialIssuer setBatchCredentialEndpoint(String batchCredentialEndpoint) {
        this.batchCredentialEndpoint = batchCredentialEndpoint;
        return this;
    }

    public Map<String, SupportedCredential> getCredentialsSupported() {
        return credentialsSupported;
    }

    public CredentialIssuer setCredentialsSupported(Map<String, SupportedCredential> credentialsSupported) {
        this.credentialsSupported = credentialsSupported;
        return this;
    }

    public DisplayObject getDisplay() {
        return display;
    }

    public CredentialIssuer setDisplay(DisplayObject display) {
        this.display = display;
        return this;
    }
}