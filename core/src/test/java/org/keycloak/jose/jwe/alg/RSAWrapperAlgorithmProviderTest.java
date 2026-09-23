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

import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwe.JWEHeader;
import org.keycloak.jose.jwe.JWEHeader.JWEHeaderBuilder;
import org.keycloak.jose.jwe.JWEKeyStorage;
import org.keycloak.jose.jwe.enc.AesGcmJWEEncryptionProvider;
import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;

import org.junit.Assert;
import org.junit.Test;

public class RSAWrapperAlgorithmProviderTest {

    private final JWEEncryptionProvider encryptionProvider = new AesGcmJWEEncryptionProvider(JWEConstants.A128GCM);

    @Test
    public void testNullDelegateIsRejected() {
        Assert.assertThrows(NullPointerException.class, () -> new RSAWrapperAlgorithmProvider(null));
    }

    @Test
    public void testValidCekIsReturned() throws Exception {
        byte[] cek = new byte[encryptionProvider.getExpectedCEKLength()];
        RSAWrapperAlgorithmProvider wrapper = new RSAWrapperAlgorithmProvider(new TestProvider(cek, false));

        Assert.assertSame(cek, wrapper.decodeCek(new byte[0], null, null, encryptionProvider));
    }

    @Test
    public void testNullAndWrongLengthCeksAreSubstituted() throws Exception {
        for (byte[] invalidCek : new byte[][] { null, new byte[8] }) {
            RSAWrapperAlgorithmProvider wrapper = new RSAWrapperAlgorithmProvider(new TestProvider(invalidCek, false));

            byte[] fallback = wrapper.decodeCek(new byte[0], null, null, encryptionProvider);
            Assert.assertNotSame(invalidCek, fallback);
            Assert.assertEquals(encryptionProvider.getExpectedCEKLength(), fallback.length);
        }
    }

    @Test
    public void testUnwrapExceptionIsSubstituted() throws Exception {
        RSAWrapperAlgorithmProvider wrapper = new RSAWrapperAlgorithmProvider(new TestProvider(null, true));

        byte[] fallback = wrapper.decodeCek(new byte[0], null, null, encryptionProvider);
        Assert.assertEquals(encryptionProvider.getExpectedCEKLength(), fallback.length);
    }

    @Test
    public void testEncodingIsDelegated() throws Exception {
        TestProvider delegate = new TestProvider(null, false);
        RSAWrapperAlgorithmProvider wrapper = new RSAWrapperAlgorithmProvider(delegate);

        Assert.assertSame(delegate.encodedCek, wrapper.encodeCek(encryptionProvider, new JWEKeyStorage(), null, null));
    }

    private static class TestProvider implements JWEAlgorithmProvider {

        private final byte[] decodedCek;
        private final boolean failOnDecode;
        private final byte[] encodedCek = new byte[] { 1 };

        private TestProvider(byte[] decodedCek, boolean failOnDecode) {
            this.decodedCek = decodedCek;
            this.failOnDecode = failOnDecode;
        }

        @Override
        public byte[] decodeCek(byte[] encodedCek, Key encryptionKey, JWEHeader header, JWEEncryptionProvider encryptionProvider) throws Exception {
            if (failOnDecode) {
                throw new Exception("Unable to unwrap CEK");
            }
            return decodedCek;
        }

        @Override
        public byte[] encodeCek(JWEEncryptionProvider encryptionProvider, JWEKeyStorage keyStorage, Key encryptionKey, JWEHeaderBuilder headerBuilder) {
            return encodedCek;
        }
    }
}
