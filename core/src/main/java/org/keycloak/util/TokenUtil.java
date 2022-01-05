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

import org.keycloak.OAuth2Constants;
import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwe.JWEHeader;
import org.keycloak.jose.jwe.JWEKeyStorage;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;
import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.RefreshToken;

import java.io.IOException;
import java.security.Key;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TokenUtil {

    public static final String TOKEN_TYPE_BEARER = "Bearer";

    public static final String TOKEN_TYPE_KEYCLOAK_ID = "Serialized-ID";

    public static final String TOKEN_TYPE_ID = "ID";

    public static final String TOKEN_TYPE_REFRESH = "Refresh";

    public static final String TOKEN_TYPE_OFFLINE = "Offline";

    public static final String TOKEN_TYPE_LOGOUT = "Logout";

    public static final String TOKEN_BACKCHANNEL_LOGOUT_EVENT = "http://schemas.openid.net/event/backchannel-logout";
    
    public static final String TOKEN_BACKCHANNEL_LOGOUT_EVENT_REVOKE_OFFLINE_TOKENS = "revoke_offline_access";

    public static String attachOIDCScope(String scopeParam) {
        if (scopeParam == null || scopeParam.isEmpty()) {
            return OAuth2Constants.SCOPE_OPENID;
        } else if (hasScope(scopeParam, OAuth2Constants.SCOPE_OPENID)) {
            return scopeParam;
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


    public static String jweDirectEncode(Key aesKey, Key hmacKey, JsonWebToken jwt) throws JWEException {
        try {
            byte[] contentBytes = JsonSerialization.writeValueAsBytes(jwt);
            return jweDirectEncode(aesKey, hmacKey, contentBytes);
        } catch (IOException ioe) {
            throw new JWEException(ioe);
        }
    }


    public static <T extends JsonWebToken> T jweDirectVerifyAndDecode(Key aesKey, Key hmacKey, String jweStr, Class<T> expectedClass) throws JWEException {
        byte[] contentBytes = jweDirectVerifyAndDecode(aesKey, hmacKey, jweStr);
        try {
            return JsonSerialization.readValue(contentBytes, expectedClass);
        } catch (IOException ioe) {
            throw new JWEException(ioe);
        }
    }

    public static String jweKeyEncryptionEncode(Key encryptionKEK, byte[] contentBytes, String algAlgorithm, String encAlgorithm, String kid, JWEAlgorithmProvider jweAlgorithmProvider, JWEEncryptionProvider jweEncryptionProvider) throws JWEException {
        JWEHeader jweHeader = new JWEHeader(algAlgorithm, encAlgorithm, null, kid, "JWT");
        return jweKeyEncryptionEncode(encryptionKEK, contentBytes, jweHeader, jweAlgorithmProvider, jweEncryptionProvider);
    }

    private static String jweKeyEncryptionEncode(Key encryptionKEK, byte[] contentBytes, JWEHeader jweHeader, JWEAlgorithmProvider jweAlgorithmProvider, JWEEncryptionProvider jweEncryptionProvider) throws JWEException {
        JWE jwe = new JWE()
                .header(jweHeader)
                .content(contentBytes);
        jwe.getKeyStorage()
                .setEncryptionKey(encryptionKEK);
        String encodedContent = jwe.encodeJwe(jweAlgorithmProvider, jweEncryptionProvider);
        return encodedContent;
    }

    public static byte[] jweKeyEncryptionVerifyAndDecode(Key decryptionKEK, String encodedContent) throws JWEException {
        JWE jwe = new JWE();
        jwe.getKeyStorage()
            .setDecryptionKey(decryptionKEK);
        jwe.verifyAndDecodeJwe(encodedContent);
        return jwe.getContent();
    }

    public static byte[] jweKeyEncryptionVerifyAndDecode(Key decryptionKEK, String encodedContent, JWEAlgorithmProvider algorithmProvider, JWEEncryptionProvider encryptionProvider) throws JWEException {
        JWE jwe = new JWE();
        jwe.getKeyStorage()
            .setDecryptionKey(decryptionKEK);
        jwe.verifyAndDecodeJwe(encodedContent, algorithmProvider, encryptionProvider);
        return jwe.getContent();
    }

    public static String jweDirectEncode(Key aesKey, Key hmacKey, byte[] contentBytes) throws JWEException {
        int keyLength = aesKey.getEncoded().length;
        String encAlgorithm;
        switch (keyLength) {
            case 16: encAlgorithm = JWEConstants.A128CBC_HS256;
                break;
            case 24: encAlgorithm = JWEConstants.A192CBC_HS384;
                break;
            case 32: encAlgorithm = JWEConstants.A256CBC_HS512;
                break;
            default: throw new IllegalArgumentException("Bad size for Encryption key: " + aesKey + ". Valid sizes are 16, 24, 32.");
        }

        JWEHeader jweHeader = new JWEHeader(JWEConstants.DIR, encAlgorithm, null);
        JWE jwe = new JWE()
                .header(jweHeader)
                .content(contentBytes);

        jwe.getKeyStorage()
                .setCEKKey(aesKey, JWEKeyStorage.KeyUse.ENCRYPTION)
                .setCEKKey(hmacKey, JWEKeyStorage.KeyUse.SIGNATURE);

        return jwe.encodeJwe();

    }

    public static byte[] jweDirectVerifyAndDecode(Key aesKey, Key hmacKey, String jweStr) throws JWEException {
        JWE jwe = new JWE();
        jwe.getKeyStorage()
                .setCEKKey(aesKey, JWEKeyStorage.KeyUse.ENCRYPTION)
                .setCEKKey(hmacKey, JWEKeyStorage.KeyUse.SIGNATURE);

        jwe.verifyAndDecodeJwe(jweStr);

        return jwe.getContent();

    }
}
