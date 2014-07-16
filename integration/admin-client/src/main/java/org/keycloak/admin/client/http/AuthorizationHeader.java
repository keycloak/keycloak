package org.keycloak.admin.client.http;

import org.apache.commons.codec.binary.Base64;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public final class AuthorizationHeader {

    private AuthorizationHeader() {}

    public static String generateBasicHeader(String username, String password){
        String pair = username + ":" + password;
        return "Basic " + new String(Base64.encodeBase64(pair.getBytes()));
    }

    public static String generateBearerHeader(String accessToken) {
        return "Bearer " + accessToken;
    }

}
