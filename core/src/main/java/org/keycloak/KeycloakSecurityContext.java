/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak;

import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.DelegatingSerializationFilter;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Available in secured requests under HttpServletRequest.getAttribute()
 * Also available in HttpSession.getAttribute under the classname of this class
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakSecurityContext implements Serializable {
    protected String tokenString;
    protected String idTokenString;

    // Don't store parsed tokens into HTTP session
    protected transient AccessToken token;
    protected transient IDToken idToken;
    protected transient AuthorizationContext authorizationContext;

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

    public AuthorizationContext getAuthorizationContext() {
        return authorizationContext;
    }

    public IDToken getIdToken() {
        return idToken;
    }

    public String getIdTokenString() {
        return idTokenString;
    }

    public String getRealm() {
        // Assumption that issuer contains realm name
        return token.getIssuer().substring(token.getIssuer().lastIndexOf('/') + 1);
    }

    // SERIALIZATION

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        DelegatingSerializationFilter.builder()
                .addAllowedClass(KeycloakSecurityContext.class)
                .setFilter(in);
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
