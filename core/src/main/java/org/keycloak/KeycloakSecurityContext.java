package org.keycloak;

import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakSecurityContext implements Serializable {
    protected String tokenString;
    protected AccessToken token;
    protected IDToken idToken;
    protected String idTokenString;

    public KeycloakSecurityContext() {
    }

    public KeycloakSecurityContext(String tokenString, AccessToken token, String idTokenString, IDToken idToken) {
        this.tokenString = tokenString;
        this.token = token;
        this.idToken = idToken;
        this.idTokenString = idTokenString;
    }

    public AccessToken getToken() {
        return token;
    }

    public String getTokenString() {
        return tokenString;
    }

    public IDToken getIdToken() {
        return idToken;
    }

    public String getIdTokenString() {
        return idTokenString;
    }
}
