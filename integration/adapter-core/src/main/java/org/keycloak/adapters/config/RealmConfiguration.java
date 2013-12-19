package org.keycloak.adapters.config;

import org.apache.http.client.HttpClient;
import org.keycloak.adapters.ResourceMetadata;
import org.keycloak.util.KeycloakUriBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmConfiguration {
    protected ResourceMetadata metadata;
    protected HttpClient client;
    protected KeycloakUriBuilder authUrl;
    protected String codeUrl;
    protected Map<String, String> resourceCredentials = new HashMap<String, String>();
    protected boolean sslRequired = true;
    protected String stateCookieName = "OAuth_Token_Request_State";

    public RealmConfiguration() {
    }

    public ResourceMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ResourceMetadata metadata) {
        this.metadata = metadata;
    }


    public boolean isSslRequired() {
        return sslRequired;
    }

    public void setSslRequired(boolean sslRequired) {
        this.sslRequired = sslRequired;
    }

    public String getStateCookieName() {
        return stateCookieName;
    }

    public void setStateCookieName(String stateCookieName) {
        this.stateCookieName = stateCookieName;
    }

    public HttpClient getClient() {
        return client;
    }

    public void setClient(HttpClient client) {
        this.client = client;
    }

    public KeycloakUriBuilder getAuthUrl() {
        return authUrl;
    }

    public void setAuthUrl(KeycloakUriBuilder authUrl) {
        this.authUrl = authUrl;
    }

    public String getCodeUrl() {
        return codeUrl;
    }

    public void setCodeUrl(String codeUrl) {
        this.codeUrl = codeUrl;
    }

    public Map<String, String> getResourceCredentials() {
        return resourceCredentials;
    }

    public void setResourceCredentials(Map<String, String> resourceCredentials) {
        this.resourceCredentials = resourceCredentials;
    }

}
