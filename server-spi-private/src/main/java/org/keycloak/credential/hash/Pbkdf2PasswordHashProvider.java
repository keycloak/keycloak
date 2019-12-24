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

package org.keycloak.credential.hash;

import org.keycloak.common.util.Base64;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.credential.PasswordCredentialModel;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * @author <a href="mailto:me@tsudot.com">Kunal Kerkar</a>
 */
public class Pbkdf2PasswordHashProvider implements PasswordHashProvider {

    private final String providerId;

    private final String pbkdf2Algorithm;
    private final int defaultIterations;
    private final int derivedKeySize;
    public static final int DEFAULT_DERIVED_KEY_SIZE = 512;

    public Pbkdf2PasswordHashProvider(String providerId, String pbkdf2Algorithm, int defaultIterations) {
        this(providerId, pbkdf2Algorithm, defaultIterations, DEFAULT_DERIVED_KEY_SIZE);
    }
    public Pbkdf2PasswordHashProvider(String providerId, String pbkdf2Algorithm, int defaultIterations, int derivedKeySize) {
        this.providerId = providerId;
        this.pbkdf2Algorithm = pbkdf2Algorithm;
        this.defaultIterations = defaultIterations;
        this.derivedKeySize = derivedKeySize;
    }

    @Override
    public boolean policyCheck(PasswordPolicy policy, PasswordCredentialModel credential) {
        int policyHashIterations = policy.getHashIterations();
        if (policyHashIterations == -1) {
            policyHashIterations = defaultIterations;
        }

        return credential.getPasswordCredentialData().getHashIterations() == policyHashIterations
                && providerId.equals(credential.getPasswordCredentialData().getAlgorithm())
                && derivedKeySize == keySize(credential);
    }

    @Override
    public PasswordCredentialModel encodedCredential(String rawPassword, int iterations) {
        if (iterations == -1) {
            iterations = defaultIterations;
        }

        byte[] salt = getSalt();
        String encodedPassword = encodedCredential(rawPassword, iterations, salt, derivedKeySize);

        return PasswordCredentialModel.createFromValues(providerId, salt, iterations, encodedPassword);
    }

    @Override
    public String encode(String rawPassword, int iterations) {
        if (iterations == -1) {
            iterations = defaultIterations;
        }

        byte[] salt = getSalt();
        return encodedCredential(rawPassword, iterations, salt, derivedKeySize);
    }

    @Override
    public boolean verify(String rawPassword, PasswordCredentialModel credential) {
        return encodedCredential(rawPassword, credential.getPasswordCredentialData().getHashIterations(), credential.getPasswordSecretData().getSalt(), keySize(credential)).equals(credential.getPasswordSecretData().getValue());
    }

    private int keySize(PasswordCredentialModel credential) {
        try {
            byte[] bytes = Base64.decode(credential.getPasswordSecretData().getValue());
            return bytes.length * 8;
        } catch (IOException e) {
            throw new RuntimeException("Credential could not be decoded", e);
        }
    }

    public void close() {
    }

    private String encodedCredential(String rawPassword, int iterations, byte[] salt, int derivedKeySize) {
        KeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, derivedKeySize);

        try {
            byte[] key = getSecretKeyFactory().generateSecret(spec).getEncoded();
            return Base64.encodeBytes(key);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Credential could not be encoded", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getSalt() {
        byte[] buffer = new byte[16];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(buffer);
        return buffer;
    }

    private SecretKeyFactory getSecretKeyFactory() {
        try {
            return SecretKeyFactory.getInstance(pbkdf2Algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("PBKDF2 algorithm not found", e);
        }
    }
}
