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

import org.keycloak.common.VerificationException;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.TokenUtil;

import java.security.PublicKey;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RSATokenVerifier {

    private final String tokenString;
    private PublicKey publicKey;
    private String realmUrl;
    private boolean checkTokenType = true;
    private boolean checkActive = true;
    private boolean checkRealmUrl = true;

    private JWSInput jws;
    private AccessToken token;

    private RSATokenVerifier(String tokenString) {
        this.tokenString = tokenString;
    }

    public static RSATokenVerifier create(String tokenString) {
        return new RSATokenVerifier(tokenString);
    }

    public static AccessToken verifyToken(String tokenString, PublicKey publicKey, String realmUrl) throws VerificationException {
        return RSATokenVerifier.create(tokenString).publicKey(publicKey).realmUrl(realmUrl).verify().getToken();
    }

    public static AccessToken verifyToken(String tokenString, PublicKey publicKey, String realmUrl, boolean checkActive, boolean checkTokenType) throws VerificationException {
        return RSATokenVerifier.create(tokenString).publicKey(publicKey).realmUrl(realmUrl).checkActive(checkActive).checkTokenType(checkTokenType).verify().getToken();
    }

    public RSATokenVerifier publicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    public RSATokenVerifier realmUrl(String realmUrl) {
        this.realmUrl = realmUrl;
        return this;
    }

    public RSATokenVerifier checkTokenType(boolean checkTokenType) {
        this.checkTokenType = checkTokenType;
        return this;
    }

    public RSATokenVerifier checkActive(boolean checkActive) {
        this.checkActive = checkActive;
        return this;
    }

    public RSATokenVerifier checkRealmUrl(boolean checkRealmUrl) {
        this.checkRealmUrl = checkRealmUrl;
        return this;
    }

    public RSATokenVerifier parse() throws VerificationException {
        if (jws == null) {
            if (tokenString == null) {
                throw new VerificationException("Token not set");
            }

            try {
                jws = new JWSInput(tokenString);
            } catch (JWSInputException e) {
                throw new VerificationException("Failed to parse JWT", e);
            }


            try {
                token = jws.readJsonContent(AccessToken.class);
            } catch (JWSInputException e) {
                throw new VerificationException("Failed to read access token from JWT", e);
            }
        }
        return this;
    }

    public AccessToken getToken() throws VerificationException {
        parse();
        return token;
    }

    public JWSHeader getHeader() throws VerificationException {
        parse();
        return jws.getHeader();
    }

    public RSATokenVerifier verify() throws VerificationException {
        parse();

        if (publicKey == null) {
            throw new VerificationException("Public key not set");
        }

        if (checkRealmUrl && realmUrl == null) {
            throw new VerificationException("Realm URL not set");
        }

        if (!RSAProvider.verify(jws, publicKey)) {
            throw new VerificationException("Invalid token signature");
        }

        String user = token.getSubject();
        if (user == null) {
            throw new VerificationException("Subject missing in token");
        }

        if (checkRealmUrl && !realmUrl.equals(token.getIssuer())) {
            throw new VerificationException("Invalid token issuer. Expected '" + realmUrl + "', but was '" + token.getIssuer() + "'");
        }

        if (checkTokenType && !TokenUtil.TOKEN_TYPE_BEARER.equalsIgnoreCase(token.getType())) {
            throw new VerificationException("Token type is incorrect. Expected '" + TokenUtil.TOKEN_TYPE_BEARER + "' but was '" + token.getType() + "'");
        }

        if (checkActive && !token.isActive()) {
            throw new VerificationException("Token is not active");
        }

        return this;
    }

}
