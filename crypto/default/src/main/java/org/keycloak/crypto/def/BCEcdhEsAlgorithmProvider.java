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

package org.keycloak.crypto.def;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.KeyAgreement;

import org.keycloak.common.util.Base64Url;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.jose.jwe.JWEHeader;
import org.keycloak.jose.jwe.JWEHeader.JWEHeaderBuilder;
import org.keycloak.jose.jwe.JWEKeyStorage;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;
import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;
import org.keycloak.jose.jwk.ECPublicJWK;
import org.keycloak.jose.jwk.JWKUtil;

import org.bouncycastle.crypto.Wrapper;
import org.bouncycastle.crypto.agreement.kdf.ConcatenationKDFGenerator;
import org.bouncycastle.crypto.engines.AESWrapEngine;
import org.bouncycastle.crypto.params.KDFParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.util.DigestFactory;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;

/**
 * ECDH Ephemeral Static Algorithm Provider.
 *
 * @author Justin Tay
 * @see <a href=
 *      "https://datatracker.ietf.org/doc/html/rfc7518#section-4.6.2">Key
 *      Derivation for ECDH Key Agreement</a>
 */
public class BCEcdhEsAlgorithmProvider implements JWEAlgorithmProvider {

    @Override
    public byte[] decodeCek(byte[] encodedCek, Key encryptionKey, JWEHeader header,
            JWEEncryptionProvider encryptionProvider) throws Exception {
        int keyDataLength = getKeyDataLength(header.getAlgorithm(), encryptionProvider);
        PublicKey sharedPublicKey = toPublicKey(header.getEphemeralPublicKey());

        String algorithmID = getAlgorithmID(header.getAlgorithm(), header.getEncryptionAlgorithm());
        byte[] derivedKey = deriveKey(sharedPublicKey, encryptionKey, keyDataLength, algorithmID,
                base64UrlDecode(header.getAgreementPartyUInfo()), base64UrlDecode(header.getAgreementPartyVInfo()));

        if (Algorithm.ECDH_ES.equals(header.getAlgorithm())) {
            return derivedKey;
        } else {
            Wrapper encrypter = new AESWrapEngine();
            encrypter.init(false, new KeyParameter(derivedKey));
            return encrypter.unwrap(encodedCek, 0, encodedCek.length);
        }
    }

    @Override
    public byte[] encodeCek(JWEEncryptionProvider encryptionProvider, JWEKeyStorage keyStorage, Key encryptionKey,
            JWEHeaderBuilder headerBuilder) throws Exception {
        JWEHeader header = headerBuilder.build();
        int keyDataLength = getKeyDataLength(header.getAlgorithm(), encryptionProvider);
        ECParameterSpec params = ((ECPublicKey) encryptionKey).getParams();
        KeyPair ephemeralKeyPair = generateEcKeyPair(params);
        ECPublicKey ephemeralPublicKey = (ECPublicKey) ephemeralKeyPair.getPublic();
        ECPrivateKey ephemeralPrivateKey = (ECPrivateKey) ephemeralKeyPair.getPrivate();

        byte[] agreementPartyUInfo = header.getAgreementPartyUInfo() != null
                ? base64UrlDecode(header.getAgreementPartyUInfo())
                : new byte[0];
        byte[] agreementPartyVInfo = header.getAgreementPartyVInfo() != null
                ? base64UrlDecode(header.getAgreementPartyVInfo())
                : new byte[0];

        headerBuilder.ephemeralPublicKey(toECPublicJWK(ephemeralPublicKey));

        String algorithmID = getAlgorithmID(header.getAlgorithm(), header.getEncryptionAlgorithm());
        byte[] derivedKey = deriveKey(encryptionKey, ephemeralPrivateKey, keyDataLength, algorithmID,
                agreementPartyUInfo, agreementPartyVInfo);
        if (Algorithm.ECDH_ES.equals(header.getAlgorithm())) {
            keyStorage.setCEKBytes(derivedKey);
            encryptionProvider.deserializeCEK(keyStorage);
            return new byte[0];
        } else {
            Wrapper encrypter = new AESWrapEngine();
            encrypter.init(true, new KeyParameter(derivedKey));
            byte[] cekBytes = keyStorage.getCekBytes();
            return encrypter.wrap(cekBytes, 0, cekBytes.length);
        }
    }

    private byte[] base64UrlDecode(String encoded) {
        return Base64Url.decode(encoded == null ? "" : encoded);
    }

    private static KeyPair generateEcKeyPair(ECParameterSpec params) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            SecureRandom randomGen = new SecureRandom();
            keyGen.initialize(params, randomGen);
            return keyGen.generateKeyPair();
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static byte[] deriveOtherInfo(int keyDataLength, String algorithmID, byte[] agreementPartyUInfo,
            byte[] agreementPartyVInfo) {
        byte[] algorithmId = encodeDataLengthData(algorithmID.getBytes(Charset.forName("ASCII")));
        byte[] partyUInfo = encodeDataLengthData(agreementPartyUInfo);
        byte[] partyVInfo = encodeDataLengthData(agreementPartyVInfo);
        byte[] suppPubInfo = toByteArray(keyDataLength);
        byte[] suppPrivInfo = emptyBytes();
        return concat(algorithmId, partyUInfo, partyVInfo, suppPubInfo, suppPrivInfo);
    }

    public static byte[] deriveKey(Key publicKey, Key privateKey, int keyDataLength, String algorithmID,
            byte[] agreementPartyUInfo, byte[] agreementPartyVInfo)
            throws InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {
        byte[] z = deriveSharedSecret(publicKey, privateKey);
        byte[] otherInfo = deriveOtherInfo(keyDataLength, algorithmID, agreementPartyUInfo, agreementPartyVInfo);
        KDFParameters param = new KDFParameters(z, otherInfo);
        ConcatenationKDFGenerator concatKdf = new ConcatenationKDFGenerator(DigestFactory.createSHA256());
        concatKdf.init(param);
        int derivedKeyLength = keyDataLength / 8;
        byte[] derivedKeyBytes = new byte[derivedKeyLength];
        concatKdf.generateBytes(derivedKeyBytes, 0, derivedKeyLength);
        return derivedKeyBytes;
    }

    private static ECPublicJWK toECPublicJWK(ECPublicKey ecKey) {
        ECPublicJWK k = new ECPublicJWK();
        int fieldSize = ecKey.getParams().getCurve().getField().getFieldSize();
        k.setCrv("P-" + fieldSize);
        k.setKeyType(KeyType.EC);
        k.setX(Base64Url.encode(JWKUtil.toIntegerBytes(ecKey.getW().getAffineX(), fieldSize)));
        k.setY(Base64Url.encode(JWKUtil.toIntegerBytes(ecKey.getW().getAffineY(), fieldSize)));
        return k;
    }

    private static PublicKey toPublicKey(ECPublicJWK jwk) {
        String crv = jwk.getCrv();
        String xStr = jwk.getX();
        String yStr = jwk.getY();

        if (crv == null) {
            throw new IllegalArgumentException("JWK crv must be set");
        }
        if (xStr == null) {
            throw new IllegalArgumentException("JWK x must be set");
        }
        if (yStr == null) {
            throw new IllegalArgumentException("JWK y must be set");
        }

        BigInteger x = new BigInteger(1, Base64Url.decode(xStr));
        BigInteger y = new BigInteger(1, Base64Url.decode(yStr));

        String name = nistToSecCurveName(crv);
        try {
            ECPoint point = new ECPoint(x, y);
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(name);
            ECParameterSpec params = new ECNamedCurveSpec(name, spec.getCurve(), spec.getG(), spec.getN());
            ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(point, params);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePublic(pubKeySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static byte[] deriveSharedSecret(Key publicKey, Key privateKey)
            throws NoSuchAlgorithmException, InvalidKeyException, IllegalStateException {
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(publicKey, true);
        return keyAgreement.generateSecret();
    }

    private static String getAlgorithmID(String alg, String enc) {
        if (Algorithm.ECDH_ES_A128KW.equals(alg) || Algorithm.ECDH_ES_A192KW.equals(alg)
                || Algorithm.ECDH_ES_A256KW.equals(alg)) {
            return alg;
        } else if (Algorithm.ECDH_ES.equals(alg)) {
            return enc;
        } else {
            throw new IllegalArgumentException("Unsupported algorithm");
        }
    }

    private static String nistToSecCurveName(String nistCurveName) {
        switch (nistCurveName) {
        case "P-256":
            return "secp256r1";
        case "P-384":
            return "secp384r1";
        case "P-521":
            return "secp521r1";
        default:
            throw new IllegalArgumentException("Unsupported curve");
        }
    }

    private static int getKeyDataLength(String alg, JWEEncryptionProvider encryptionProvider) {
        if (Algorithm.ECDH_ES_A128KW.equals(alg)) {
            return 128;
        } else if (Algorithm.ECDH_ES_A192KW.equals(alg)) {
            return 192;
        } else if (Algorithm.ECDH_ES_A256KW.equals(alg)) {
            return 256;
        } else if (Algorithm.ECDH_ES.equals(alg)) {
            return encryptionProvider.getExpectedCEKLength() * 8;
        } else {
            throw new IllegalArgumentException("Unsupported algorithm");
        }
    }

    private static byte[] encodeDataLengthData(final byte[] data) {
        byte[] databytes = data != null ? data : new byte[0];
        byte[] datalen = toByteArray(databytes.length);
        return concat(datalen, databytes);
    }

    private static byte[] emptyBytes() {
        return new byte[0];
    }

    private static byte[] toByteArray(int intValue) {
        return new byte[] { (byte) (intValue >> 24), (byte) (intValue >> 16), (byte) (intValue >> 8), (byte) intValue };
    }

    private static byte[] concat(byte[]... byteArrays) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            for (byte[] bytes : byteArrays) {
                if (bytes != null) {
                    baos.write(bytes);
                }
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
