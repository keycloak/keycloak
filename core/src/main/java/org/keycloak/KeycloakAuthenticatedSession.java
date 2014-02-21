package org.keycloak;

import org.keycloak.adapters.ResourceMetadata;
import org.keycloak.representations.AccessToken;

import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakAuthenticatedSession implements Serializable {
    protected String tokenString;
    protected AccessToken token;
    protected transient ResourceMetadata metadata;

    public KeycloakAuthenticatedSession() {
    }

    public KeycloakAuthenticatedSession(String tokenString, AccessToken token, ResourceMetadata metadata) {
        this.tokenString = tokenString;
        this.token = token;
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

}
