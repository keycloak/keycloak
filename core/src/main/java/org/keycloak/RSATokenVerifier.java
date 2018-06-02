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
import org.keycloak.representations.AccessToken;

import java.security.PublicKey;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
//KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
public class RSATokenVerifier extends JWSTokenVerifier {

 private RSATokenVerifier(String tokenString) {
     super(tokenString);
 }

 public static JWSTokenVerifier create(String tokenString) {
     return new RSATokenVerifier(tokenString);
 }

 public static AccessToken verifyToken(String tokenString, PublicKey publicKey, String realmUrl) throws VerificationException {
     return RSATokenVerifier.create(tokenString).publicKey(publicKey).realmUrl(realmUrl).verify().getToken();
 }

 public static AccessToken verifyToken(String tokenString, PublicKey publicKey, String realmUrl, boolean checkActive, boolean checkTokenType) throws VerificationException {
     return RSATokenVerifier.create(tokenString).publicKey(publicKey).realmUrl(realmUrl).checkActive(checkActive).checkTokenType(checkTokenType).verify().getToken();
 }

}
