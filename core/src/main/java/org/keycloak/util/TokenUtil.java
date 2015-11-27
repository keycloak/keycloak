package org.keycloak.util;

import java.io.IOException;

import org.keycloak.OAuth2Constants;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.RefreshToken;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TokenUtil {

    public static final String TOKEN_TYPE_BEARER = "Bearer";

    public static final String TOKEN_TYPE_ID = "ID";

    public static final String TOKEN_TYPE_REFRESH = "Refresh";

    public static final String TOKEN_TYPE_OFFLINE = "Offline";


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
     * Return refresh token or offline token
     *
     * @param decodedToken
     * @return
     */
    public static RefreshToken getRefreshToken(byte[] decodedToken) throws JWSInputException {
        try {
            return JsonSerialization.readValue(decodedToken, RefreshToken.class);
        } catch (IOException e) {
            throw new JWSInputException(e);
        }
    }

    public static RefreshToken getRefreshToken(String refreshToken) throws JWSInputException {
        byte[] encodedContent = new JWSInput(refreshToken).getContent();
        return getRefreshToken(encodedContent);
    }

    /**
     * Return true if given refreshToken represents offline token
     *
     * @param refreshToken
     * @return
     */
    public static boolean isOfflineToken(String refreshToken) throws JWSInputException {
        RefreshToken token = getRefreshToken(refreshToken);
        return token.getType().equals(TOKEN_TYPE_OFFLINE);
    }

}
