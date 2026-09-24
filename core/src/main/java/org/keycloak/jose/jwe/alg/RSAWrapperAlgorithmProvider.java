/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jose.jwe.alg;

import java.security.Key;
import java.util.Objects;

import org.keycloak.jose.jwe.JWEHeader;
import org.keycloak.jose.jwe.JWEHeader.JWEHeaderBuilder;
import org.keycloak.jose.jwe.JWEKeyStorage;
import org.keycloak.jose.jwe.JWEUtils;
import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;

import org.jboss.logging.Logger;

/**
 * Substitutes a random CEK on RSA unwrap failures so content authentication still runs.
 */
public class RSAWrapperAlgorithmProvider implements JWEAlgorithmProvider {

    private static final Logger log = Logger.getLogger(RSAWrapperAlgorithmProvider.class);

    private final JWEAlgorithmProvider delegate;

    public RSAWrapperAlgorithmProvider(JWEAlgorithmProvider delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public byte[] decodeCek(byte[] encodedCek, Key encryptionKey, JWEHeader header, JWEEncryptionProvider encryptionProvider) throws Exception {
        int expectedCekLength = encryptionProvider.getExpectedCEKLength();
        byte[] fallbackCek = JWEUtils.generateSecret(expectedCekLength);

        try {
            byte[] decodedCek = delegate.decodeCek(encodedCek, encryptionKey, header, encryptionProvider);
            if (decodedCek != null && decodedCek.length == expectedCekLength) {
                return decodedCek;
            }
        } catch (Exception e) {
            if (log.isTraceEnabled()) {
                log.trace("Exception when decoding CEK", e);
            }
        }

        return fallbackCek;
    }

    @Override
    public byte[] encodeCek(JWEEncryptionProvider encryptionProvider, JWEKeyStorage keyStorage, Key encryptionKey, JWEHeaderBuilder headerBuilder) throws Exception {
        return delegate.encodeCek(encryptionProvider, keyStorage, encryptionKey, headerBuilder);
    }
}
