/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.saml.processing.core.util;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import org.keycloak.common.VerificationException;
import org.keycloak.rotation.KeyLocator;
import org.keycloak.saml.SignatureAlgorithm;

import org.jboss.logging.Logger;

/**
 *
 * @author rmartinc
 */
public class RedirectBindingSignatureUtil {

    private static final Logger log = Logger.getLogger(RedirectBindingSignatureUtil.class);

    private RedirectBindingSignatureUtil (){
        // utility class
    }

    public static boolean validateRedirectBindingSignature(SignatureAlgorithm sigAlg, byte[] rawQueryBytes, byte[] decodedSignature,
            KeyLocator locator, String keyId) throws KeyManagementException, VerificationException {
        try {
            try {
                Key key = locator.getKey(keyId);
                if (key != null) {
                    return validateRedirectBindingSignatureForKey(sigAlg, rawQueryBytes, decodedSignature, key);
                }
            } catch (KeyManagementException ex) {
            }
        } catch (SignatureException ex) {
            log.debug("Verification failed for key %s: %s", keyId, ex);
            log.trace(ex);
        }

        log.trace("Trying hard to validate XML signature using all available keys.");

        for (Key key : locator) {
            try {
                if (validateRedirectBindingSignatureForKey(sigAlg, rawQueryBytes, decodedSignature, key)) {
                    return true;
                }
            } catch (SignatureException ex) {
                log.debug("Verification failed: %s", ex);
            }
        }

        return false;
    }

    public static boolean validateRedirectBindingSignatureForKey(SignatureAlgorithm sigAlg, byte[] rawQueryBytes, byte[] decodedSignature, Key key)
      throws SignatureException {
        if (key == null) {
            return false;
        }

        if (!(key instanceof PublicKey)) {
            log.warnf("Unusable key for signature validation: %s", key);
            return false;
        }

        Signature signature = sigAlg.createSignature(); // todo plugin signature alg
        try {
            signature.initVerify((PublicKey) key);
        } catch (InvalidKeyException ex) {
            log.warnf(ex, "Unusable key for signature validation: %s", key);
            return false;
        }

        signature.update(rawQueryBytes);

        return signature.verify(decodedSignature);
    }
}
