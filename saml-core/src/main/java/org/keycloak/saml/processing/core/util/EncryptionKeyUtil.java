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
package org.keycloak.saml.processing.core.util;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;

/**
 * Utility to generate symmetric key
 *
 * @author Anil.Saldhana@redhat.com
 * @since Feb 4, 2009
 */
public class EncryptionKeyUtil {

    /**
     * Generate a secret key useful for encryption/decryption
     *
     * @param encAlgo
     * @param keySize Length of the key (if 0, defaults to 128 bits)
     *
     * @return
     *
     * @throws GeneralSecurityException
     */
    public static SecretKey getSecretKey(String encAlgo, int keySize) throws GeneralSecurityException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(encAlgo);
        if (keySize == 0)
            keySize = 128;
        keyGenerator.init(keySize);
        return keyGenerator.generateKey();
    }

}