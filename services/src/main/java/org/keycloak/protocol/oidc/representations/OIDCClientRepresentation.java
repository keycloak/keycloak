package org.keycloak.protocol.oidc.representations;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OIDCClientRepresentation {

    @JsonProperty("redirect_uris")
    private List<String> redirectUris;

    @JsonProperty("token_endpoint_auth_method")
    private String tokenEndpointAuthMethod;

    @JsonProperty("grant_types")
    private String grantTypes;

    @JsonProperty("response_types")
    private String responseTypes;

    @JsonProperty("client_name")
    private String clientName;

    @JsonProperty("client_uri")
    private String clientUri;

    @JsonProperty("logo_uri")
    private String logoUri;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("contacts")
    private String contacts;

    @JsonProperty("tos_uri")
    private String tos_uri;

    @JsonProperty("policy_uri")
    private String policy_uri;

    @JsonProperty("jwks_uri")
    private String jwks_uri;

    @JsonProperty("jwks")
    private String jwks;

    @JsonProperty("software_id")
    private String softwareId;

    @JsonProperty("software_version")
    private String softwareVersion;

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public String getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) {
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }

    public String getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(String grantTypes) {
        this.grantTypes = grantTypes;
    }

    public String getResponseTypes() {
        return responseTypes;
    }

    public void setResponseTypes(String responseTypes) {
        this.responseTypes = responseTypes;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientUri() {
        return clientUri;
    }

    public void setClientUri(String clientUri) {
        this.clientUri = clientUri;
    }

    public String getLogoUri() {
        return logoUri;
    }

    public void setLogoUri(String logoUri) {
        this.logoUri = logoUri;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getContacts() {
        return contacts;
    }

    public void setContacts(String contacts) {
        this.contacts = contacts;
    }

    public String getTos_uri() {
        return tos_uri;
    }

    public void setTos_uri(String tos_uri) {
        this.tos_uri = tos_uri;
    }

    public String getPolicy_uri() {
        return policy_uri;
    }

    public void setPolicy_uri(String policy_uri) {
        this.policy_uri = policy_uri;
    }

    public String getJwks_uri() {
        return jwks_uri;
    }

    public void setJwks_uri(String jwks_uri) {
        this.jwks_uri = jwks_uri;
    }

    public String getJwks() {
        return jwks;
    }

    public void setJwks(String jwks) {
        this.jwks = jwks;
    }

    public String getSoftwareId() {
        return softwareId;
    }

    public void setSoftwareId(String softwareId) {
        this.softwareId = softwareId;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

}
