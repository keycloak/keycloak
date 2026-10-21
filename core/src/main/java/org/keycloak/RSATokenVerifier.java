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

import java.security.PublicKey;

import org.keycloak.common.VerificationException;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.representations.AccessToken;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Deprecated
public class RSATokenVerifier {

    private final TokenVerifier<AccessToken> tokenVerifier;

    private RSATokenVerifier(String tokenString) {
        this.tokenVerifier = TokenVerifier.create(tokenString, AccessToken.class).withDefaultChecks();
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
        tokenVerifier.publicKey(publicKey);
        return this;
    }

    public RSATokenVerifier realmUrl(String realmUrl) {
        tokenVerifier.realmUrl(realmUrl);
        return this;
    }

    public RSATokenVerifier checkTokenType(boolean checkTokenType) {
        tokenVerifier.checkTokenType(checkTokenType);
        return this;
    }

    public RSATokenVerifier checkActive(boolean checkActive) {
        tokenVerifier.checkActive(checkActive);
        return this;
    }

    public RSATokenVerifier checkRealmUrl(boolean checkRealmUrl) {
        tokenVerifier.checkRealmUrl(checkRealmUrl);
        return this;
    }

    public RSATokenVerifier parse() throws VerificationException {
        tokenVerifier.parse();
        return this;
    }

    public AccessToken getToken() throws VerificationException {
        return tokenVerifier.getToken();
    }

    public JWSHeader getHeader() throws VerificationException {
        return tokenVerifier.getHeader();
    }

    public RSATokenVerifier verify() throws VerificationException {
        tokenVerifier.verify();
        return this;
    }

}
