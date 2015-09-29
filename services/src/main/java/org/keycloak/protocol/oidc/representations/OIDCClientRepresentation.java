package org.keycloak.protocol.oidc.representations;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OIDCClientRepresentation {

    @JsonProperty("redirect_uris")
    private List<String> redirectUris;

    @JsonProperty("client_name")
    private String clientName;

    @JsonProperty("client_uri")
    private String clientUri;

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
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

}
