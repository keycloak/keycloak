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

package org.keycloak.adapters.rotation;

import org.keycloak.RSATokenVerifier;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AdapterRSATokenVerifier {

    private static final Logger LOG = LoggerFactory.getLogger(AdapterRSATokenVerifier.class);

    public static AccessToken verifyToken(String tokenString, KeycloakDeployment deployment) throws VerificationException {
        return verifyToken(tokenString, deployment, true, true);
    }


    public static PublicKey getPublicKey(String kid, KeycloakDeployment deployment) throws VerificationException {
        PublicKeyLocator pkLocator = deployment.getPublicKeyLocator();

        PublicKey publicKey = pkLocator.getPublicKey(kid, deployment);
        if (publicKey == null) {
            LOG.error("Didn't find publicKey for kid: {}", kid);
            throw new VerificationException("Didn't find publicKey for specified kid");
        }

        return publicKey;
    }

    public static AccessToken verifyToken(String tokenString, KeycloakDeployment deployment, boolean checkActive, boolean checkTokenType) throws VerificationException {
        RSATokenVerifier verifier = RSATokenVerifier.create(tokenString).realmUrl(deployment.getRealmInfoUrl()).checkActive(checkActive).checkTokenType(checkTokenType);
        PublicKey publicKey = getPublicKey(verifier.getHeader().getKeyId(), deployment);
        return verifier.publicKey(publicKey).verify().getToken();
    }
}
