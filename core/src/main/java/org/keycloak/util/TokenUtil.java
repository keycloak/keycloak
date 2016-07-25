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


    public static String attachOIDCScope(String scopeParam) {
        if (scopeParam == null || scopeParam.isEmpty()) {
            return OAuth2Constants.SCOPE_OPENID;
        } else {
            return OAuth2Constants.SCOPE_OPENID + " " + scopeParam;
        }
    }

    public static boolean isOIDCRequest(String scopeParam) {
        return hasScope(scopeParam, OAuth2Constants.SCOPE_OPENID);
    }

    public static boolean isOfflineTokenRequested(String scopeParam) {
        return hasScope(scopeParam, OAuth2Constants.OFFLINE_ACCESS);
    }

    public static boolean hasScope(String scopeParam, String targetScope) {
        if (scopeParam == null || targetScope == null) {
            return false;
        }

        String[] scopes = scopeParam.split(" ");
        for (String scope : scopes) {
            if (targetScope.equals(scope)) {
                return true;
            }
        }
        return false;
    }


    public static boolean hasPrompt(String promptParam, String targetPrompt) {
        if (promptParam == null || targetPrompt == null) {
            return false;
        }

        String[] prompts = promptParam.split(" ");
        for (String prompt : prompts) {
            if (targetPrompt.equals(prompt)) {
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
