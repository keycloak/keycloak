package org.keycloak;

import org.keycloak.adapters.ResourceMetadata;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakAuthenticatedSession implements Serializable {
    protected String tokenString;
    protected AccessToken token;
    protected IDToken idToken;
    protected String idTokenString;
    protected transient ResourceMetadata metadata;

    public KeycloakAuthenticatedSession() {
    }

    public KeycloakAuthenticatedSession(String tokenString, AccessToken token, String idTokenString, IDToken idToken, ResourceMetadata metadata) {
        this.tokenString = tokenString;
        this.token = token;
        this.idToken = idToken;
        this.idTokenString = idTokenString;
        this.metadata = metadata;
    }

    public AccessToken getToken() {
        return token;
    }

    public String getTokenString() {
        return tokenString;
    }

    public ResourceMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ResourceMetadata metadata) {
        this.metadata = metadata;
    }

    public IDToken getIdToken() {
        return idToken;
    }

    public String getIdTokenString() {
        return idTokenString;
    }
}
