package org.keycloak.util;

import java.io.IOException;

import org.keycloak.OAuth2Constants;
import org.keycloak.representations.RefreshToken;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RefreshTokenUtil {

    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    public static final String TOKEN_TYPE_OFFLINE = "OFFLINE";

    public static boolean isOfflineTokenRequested(String scopeParam) {
        if (scopeParam == null) {
            return false;
        }

        String[] scopes = scopeParam.split(" ");
        for (String scope : scopes) {
            if (OAuth2Constants.OFFLINE_ACCESS.equals(scope)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Return refresh token or offline otkne
     *
     * @param decodedToken
     * @return
     */
    public static RefreshToken getRefreshToken(byte[] decodedToken) throws IOException {
        return JsonSerialization.readValue(decodedToken, RefreshToken.class);
    }

    private static RefreshToken getRefreshToken(String refreshToken) throws IOException {
        byte[] decodedToken = Base64Url.decode(refreshToken);
        return getRefreshToken(decodedToken);
    }

    /**
     * Return true if given refreshToken represents offline token
     *
     * @param refreshToken
     * @return
     */
    public static boolean isOfflineToken(String refreshToken) {
        try {
            RefreshToken token = getRefreshToken(refreshToken);
            return token.getType().equals(TOKEN_TYPE_OFFLINE);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

}
