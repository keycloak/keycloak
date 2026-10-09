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

package org.keycloak.crypto.elytron;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;

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

import org.jose4j.jwe.kdf.ConcatKeyDerivationFunction;
import org.jose4j.keys.EcKeyUtil;
import org.jose4j.keys.EllipticCurves;
import org.jose4j.lang.JoseException;

/**
 * ECDH Ephemeral Static Algorithm Provider.
 *
 * @author Justin Tay
 * @see <a href=
 *      "https://datatracker.ietf.org/doc/html/rfc7518#section-4.6.2">Key
 *      Derivation for ECDH Key Agreement</a>
 */
public class ElytronEcdhEsAlgorithmProvider implements JWEAlgorithmProvider {

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
            Cipher cipher = Cipher.getInstance(getAesWrapAlgorithm(header.getAlgorithm()));
            cipher.init(Cipher.UNWRAP_MODE, new SecretKeySpec(derivedKey, "AES"));
            return cipher.unwrap(encodedCek, "AES", Cipher.SECRET_KEY).getEncoded();
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
            Cipher cipher = Cipher.getInstance(getAesWrapAlgorithm(header.getAlgorithm()));
            cipher.init(Cipher.WRAP_MODE, new SecretKeySpec(derivedKey, "AES"));
            byte[] cekBytes = keyStorage.getCekBytes();
            return cipher.wrap(new SecretKeySpec(cekBytes, "AES"));
        }
    }

    private byte[] base64UrlDecode(String encoded) {
        return Base64Url.decode(encoded == null ? "" : encoded);
    }

    private static KeyPair generateEcKeyPair(ECParameterSpec params) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            SecureRandom randomGen = SecureRandom.getInstance("SHA1PRNG");
            keyGen.initialize(params, randomGen);
            return keyGen.generateKeyPair();
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static byte[] deriveKey(Key publicKey, Key privateKey, int keyDataLength, String algorithmID,
            byte[] agreementPartyUInfo, byte[] agreementPartyVInfo)
            throws InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {
        byte[] z = deriveSharedSecret(publicKey, privateKey);
        byte[] suppPrivInfo = emptyBytes();
        ConcatKeyDerivationFunction concatKdf = new ConcatKeyDerivationFunction("SHA-256");
        return concatKdf.kdf(z, keyDataLength, encodeDataLengthData(algorithmID.getBytes(Charset.forName("ASCII"))),
                encodeDataLengthData(agreementPartyUInfo), encodeDataLengthData(agreementPartyVInfo),
                toByteArray(keyDataLength), suppPrivInfo);
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

        ECParameterSpec spec = EllipticCurves.getSpec(crv);
        if (spec == null) {
            throw new IllegalArgumentException("Unsupported curve: " + crv);
        }

        validatePointOnCurve(x, y, spec);

        EcKeyUtil ecKeyUtil = new EcKeyUtil();
        try {
            return ecKeyUtil.publicKey(x, y, spec);
        } catch (JoseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Validates that the point (x, y) lies on the given prime-field elliptic curve.
     * Checks field membership (0 <= x, y < p) and the curve equation y² ≡ x³ + ax + b (mod p).
     * All curves supported by jose4j (P-256, P-384, P-521, secp256k1) have prime fields
     * and cofactor one, so this check is sufficient to reject invalid-curve attacks.
     */
    private static void validatePointOnCurve(BigInteger x, BigInteger y, ECParameterSpec spec) {
        BigInteger p = ((java.security.spec.ECFieldFp) spec.getCurve().getField()).getP();
        BigInteger a = spec.getCurve().getA();
        BigInteger b = spec.getCurve().getB();

        if (x.signum() < 0 || x.compareTo(p) >= 0) {
            throw new IllegalArgumentException("EPK x coordinate is not in the field");
        }
        if (y.signum() < 0 || y.compareTo(p) >= 0) {
            throw new IllegalArgumentException("EPK y coordinate is not in the field");
        }

        // y² mod p
        BigInteger lhs = y.modPow(BigInteger.TWO, p);
        // (x³ + ax + b) mod p
        BigInteger rhs = x.modPow(BigInteger.valueOf(3), p)
                .add(a.multiply(x))
                .add(b)
                .mod(p);

        if (!lhs.equals(rhs)) {
            throw new IllegalArgumentException("EPK point is not on the named curve");
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

    private static String getAesWrapAlgorithm(String alg) {
        if (Algorithm.ECDH_ES_A128KW.equals(alg)) {
            return "AESWrap_128";
        } else if (Algorithm.ECDH_ES_A192KW.equals(alg)) {
            return "AESWrap_192";
        } else if (Algorithm.ECDH_ES_A256KW.equals(alg)) {
            return "AESWrap_256";
        } else {
            throw new IllegalArgumentException("Unsupported algorithm");
        }
    }
}
