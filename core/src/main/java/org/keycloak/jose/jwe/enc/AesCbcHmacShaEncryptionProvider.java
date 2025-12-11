/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jose.jwe.enc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jwe.JWEKeyStorage;
import org.keycloak.jose.jwe.JWEUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AesCbcHmacShaEncryptionProvider implements JWEEncryptionProvider {


    @Override
    public void encodeJwe(JWE jwe) throws IOException, GeneralSecurityException {

        byte[] contentBytes = jwe.getContent();

        byte[] initializationVector = JWEUtils.generateSecret(16);

        Key aesKey = jwe.getKeyStorage().getCEKKey(JWEKeyStorage.KeyUse.ENCRYPTION, false);
        if (aesKey == null) {
            throw new IllegalArgumentException("AES CEK key not present");
        }

        Key hmacShaKey = jwe.getKeyStorage().getCEKKey(JWEKeyStorage.KeyUse.SIGNATURE, false);
        if (hmacShaKey == null) {
            throw new IllegalArgumentException("HMAC CEK key not present");
        }

        int expectedAesKeyLength = getExpectedAesKeyLength();
        if (expectedAesKeyLength != aesKey.getEncoded().length) {
            throw new IllegalStateException("Length of aes key should be " + expectedAesKeyLength +", but was " + aesKey.getEncoded().length);
        }

        byte[] cipherBytes = encryptBytes(contentBytes, initializationVector, aesKey);

        byte[] aad = jwe.getBase64Header().getBytes(StandardCharsets.UTF_8);
        byte[] authenticationTag = computeAuthenticationTag(aad, initializationVector, cipherBytes, hmacShaKey);

        jwe.setEncryptedContentInfo(initializationVector, cipherBytes, authenticationTag);
    }


    @Override
    public void verifyAndDecodeJwe(JWE jwe) throws IOException, GeneralSecurityException {
        Key aesKey = jwe.getKeyStorage().getCEKKey(JWEKeyStorage.KeyUse.ENCRYPTION, false);
        if (aesKey == null) {
            throw new IllegalArgumentException("AES CEK key not present");
        }

        Key hmacShaKey = jwe.getKeyStorage().getCEKKey(JWEKeyStorage.KeyUse.SIGNATURE, false);
        if (hmacShaKey == null) {
            throw new IllegalArgumentException("HMAC CEK key not present");
        }

        int expectedAesKeyLength = getExpectedAesKeyLength();
        if (expectedAesKeyLength != aesKey.getEncoded().length) {
            throw new IllegalStateException("Length of aes key should be " + expectedAesKeyLength +", but was " + aesKey.getEncoded().length);
        }

        byte[] aad = jwe.getBase64Header().getBytes(StandardCharsets.UTF_8);
        byte[] authenticationTag = computeAuthenticationTag(aad, jwe.getInitializationVector(), jwe.getEncryptedContent(), hmacShaKey);

        byte[] expectedAuthTag = jwe.getAuthenticationTag();
        boolean digitsEqual = MessageDigest.isEqual(expectedAuthTag, authenticationTag);

        if (!digitsEqual) {
            throw new IllegalArgumentException("Signature validations failed");
        }

        byte[] contentBytes = decryptBytes(jwe.getEncryptedContent(), jwe.getInitializationVector(), aesKey);

        jwe.content(contentBytes);
    }


    protected abstract int getExpectedAesKeyLength();

    protected abstract String getHmacShaAlgorithm();

    protected abstract int getAuthenticationTagLength();


    private byte[] encryptBytes(byte[] contentBytes, byte[] ivBytes, Key aesKey) throws GeneralSecurityException {
        Cipher cipher = CryptoIntegration.getProvider().getAesCbcCipher();
          AlgorithmParameterSpec ivParamSpec = new IvParameterSpec(ivBytes);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivParamSpec);
        return cipher.doFinal(contentBytes);
    }

    private byte[] decryptBytes(byte[] encryptedBytes, byte[] ivBytes, Key aesKey) throws GeneralSecurityException {
        Cipher cipher = CryptoIntegration.getProvider().getAesCbcCipher();
        AlgorithmParameterSpec ivParamSpec = new IvParameterSpec(ivBytes);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, ivParamSpec);
        return cipher.doFinal(encryptedBytes);
    }


    private byte[] computeAuthenticationTag(byte[] aadBytes, byte[] ivBytes, byte[] cipherBytes, Key hmacKeySpec) throws NoSuchAlgorithmException, InvalidKeyException {
        // Compute "al"
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.BIG_ENDIAN); // optional, the initial order of a byte buffer is always BIG_ENDIAN.
        int aadLengthInBits = aadBytes.length * 8;
        b.putInt(aadLengthInBits);
        byte[] result1 = b.array();
        byte[] al = new byte[8];
        System.arraycopy(result1, 0, al, 4, 4);

        byte[] concatenatedHmacInput = new byte[aadBytes.length + ivBytes.length + cipherBytes.length + al.length];
        System.arraycopy(aadBytes, 0, concatenatedHmacInput, 0, aadBytes.length);
        System.arraycopy(ivBytes, 0, concatenatedHmacInput, aadBytes.length, ivBytes.length );
        System.arraycopy(cipherBytes, 0, concatenatedHmacInput, aadBytes.length + ivBytes.length , cipherBytes.length);
        System.arraycopy(al, 0, concatenatedHmacInput, aadBytes.length + ivBytes.length + cipherBytes.length, al.length);

        String hmacShaAlg = getHmacShaAlgorithm();
        Mac macImpl = Mac.getInstance(hmacShaAlg);
        macImpl.init(hmacKeySpec);
        macImpl.update(concatenatedHmacInput);
        byte[] macEncoded =  macImpl.doFinal();

        int authTagLength = getAuthenticationTagLength();
        return Arrays.copyOf(macEncoded, authTagLength);
    }


    @Override
    public void deserializeCEK(JWEKeyStorage keyStorage) {
        byte[] cekBytes = keyStorage.getCekBytes();

        int cekLength = getExpectedCEKLength();
        byte[] cekMacKey = Arrays.copyOf(cekBytes, cekLength / 2);
        byte[] cekAesKey = Arrays.copyOfRange(cekBytes, cekLength / 2, cekLength);

        SecretKeySpec aesKey = new SecretKeySpec(cekAesKey, "AES");
        SecretKeySpec hmacKey = new SecretKeySpec(cekMacKey, "HMACSHA2");

        keyStorage.setCEKKey(aesKey, JWEKeyStorage.KeyUse.ENCRYPTION);
        keyStorage.setCEKKey(hmacKey, JWEKeyStorage.KeyUse.SIGNATURE);
    }


    @Override
    public byte[] serializeCEK(JWEKeyStorage keyStorage) {
        Key aesKey = keyStorage.getCEKKey(JWEKeyStorage.KeyUse.ENCRYPTION, false);
        if (aesKey == null) {
            throw new IllegalArgumentException("AES CEK key not present");
        }

        Key hmacShaKey = keyStorage.getCEKKey(JWEKeyStorage.KeyUse.SIGNATURE, false);
        if (hmacShaKey == null) {
            throw new IllegalArgumentException("HMAC CEK key not present");
        }

        byte[] hmacBytes = hmacShaKey.getEncoded();
        byte[] aesBytes = aesKey.getEncoded();

        byte[] result = new byte[hmacBytes.length + aesBytes.length];
        System.arraycopy(hmacBytes, 0, result, 0, hmacBytes.length);
        System.arraycopy(aesBytes, 0, result, hmacBytes.length, aesBytes.length);

        return result;
    }



    public static class Aes128CbcHmacSha256Provider extends AesCbcHmacShaEncryptionProvider {

        @Override
        protected int getExpectedAesKeyLength() {
            return 16;
        }

        @Override
        protected String getHmacShaAlgorithm() {
            return "HMACSHA256";
        }

        @Override
        protected int getAuthenticationTagLength() {
            return 16;
        }

        @Override
        public int getExpectedCEKLength() {
            return 32;
        }
    }


    public static class Aes192CbcHmacSha384Provider extends AesCbcHmacShaEncryptionProvider {

        @Override
        protected int getExpectedAesKeyLength() {
            return 24;
        }

        @Override
        protected String getHmacShaAlgorithm() {
            return "HMACSHA384";
        }

        @Override
        protected int getAuthenticationTagLength() {
            return 24;
        }

        @Override
        public int getExpectedCEKLength() {
            return 48;
        }
    }


    public static class Aes256CbcHmacSha512Provider extends AesCbcHmacShaEncryptionProvider {

        @Override
        protected int getExpectedAesKeyLength() {
            return 32;
        }

        @Override
        protected String getHmacShaAlgorithm() {
            return "HMACSHA512";
        }

        @Override
        protected int getAuthenticationTagLength() {
            return 32;
        }

        @Override
        public int getExpectedCEKLength() {
            return 64;
        }
    }


}
