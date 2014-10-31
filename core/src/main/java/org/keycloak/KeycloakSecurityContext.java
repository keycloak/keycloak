package org.keycloak;

import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.util.Base64Url;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakSecurityContext implements Serializable {
    protected String tokenString;
    protected String idTokenString;

    // Don't store parsed tokens into HTTP session
    protected transient AccessToken token;
    protected transient IDToken idToken;

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

    public String getRealm() {
        // Assumption that issuer contains realm name
        return token.getIssuer();
    }

    // SERIALIZATION

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        token = parseToken(tokenString, AccessToken.class);
        idToken = parseToken(idTokenString, IDToken.class);
    }

    // Just decode token without any verifications
    private <T> T parseToken(String encoded, Class<T> clazz) throws IOException {
        if (encoded == null)
            return null;

        String[] parts = encoded.split("\\.");
        if (parts.length < 2 || parts.length > 3) throw new IllegalArgumentException("Parsing error");

        byte[] bytes = Base64Url.decode(parts[1]);
        return JsonSerialization.readValue(bytes, clazz);
    }


}
