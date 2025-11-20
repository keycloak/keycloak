/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.jose.jwk;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.EdECPublicKey;
import java.security.spec.EdECPoint;
import java.security.spec.EdECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.NamedParameterSpec;
import java.util.Optional;

import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;

/**
 * <p>Class that uses Java 15+ EdEC classes and implements the EdECUtils interface.</p>
 *
 * @author rmartinc
 */
class EdECUtilsImpl implements EdECUtils {

    public EdECUtilsImpl() {
    }

    @Override
    public boolean isEdECSupported() {
        return true;
    }

    @Override
    public JWK okp(String kid, String algorithm, Key key, KeyUse keyUse) {
        EdECPublicKey eddsaPublicKey = (EdECPublicKey) key;

        OKPPublicJWK k = new OKPPublicJWK();

        kid = kid != null ? kid : KeyUtils.createKeyId(key);

        k.setKeyId(kid);
        k.setKeyType(KeyType.OKP);
        k.setAlgorithm(algorithm);
        k.setPublicKeyUse(keyUse == null ? JWKBuilder.DEFAULT_PUBLIC_KEY_USE.getSpecName() : keyUse.getSpecName());
        k.setCrv(eddsaPublicKey.getParams().getName());

        Optional<String> x = edPublicKeyInJwkRepresentation(eddsaPublicKey);
        k.setX(x.orElse(""));

        return k;
    }

    @Override
    public PublicKey createOKPPublicKey(JWK jwk) {
        String x = jwk.getOtherClaim(OKPPublicJWK.X, String.class);
        String crv = jwk.getOtherClaim(OKPPublicJWK.CRV, String.class);
        // JWK representation "x" of a public key
        int bytesLength = 0;
        if (Algorithm.Ed25519.equals(crv)) {
            bytesLength = 32;
        } else if (Algorithm.Ed448.equals(crv)) {
            bytesLength = 57;
        } else {
            throw new RuntimeException("Invalid JWK representation of OKP type algorithm");
        }

        byte[] decodedX = Base64Url.decode(x);
        if (decodedX.length != bytesLength) {
            throw new RuntimeException("Invalid JWK representation of OKP type public key");
        }

        // x-coordinate's parity check shown by MSB(bit) of MSB(byte) of decoded "x": 1 is odd, 0 is even
        boolean isOddX = false;
        if ((decodedX[decodedX.length - 1] & -128) != 0) { // 0b10000000
            isOddX = true;
        }

        // MSB(bit) of MSB(byte) showing x-coodinate's parity is set to 0
        decodedX[decodedX.length - 1] &= 127; // 0b01111111

        // both x and y-coordinate in twisted Edwards curve are always 0 or natural number
        BigInteger y = new BigInteger(1, reverseBytes(decodedX));
        NamedParameterSpec spec = new NamedParameterSpec(crv);
        EdECPoint ep = new EdECPoint(isOddX, y);
        EdECPublicKeySpec keySpec = new EdECPublicKeySpec(spec, ep);

        PublicKey publicKey = null;
        try {
            publicKey = KeyFactory.getInstance(crv).generatePublic(keySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return publicKey;
    }

    private static Optional<String> edPublicKeyInJwkRepresentation(EdECPublicKey eddsaPublicKey) {
        EdECPoint edEcPoint = eddsaPublicKey.getPoint();
        BigInteger yCoordinate = edEcPoint.getY();

        // JWK representation "x" of a public key
        int bytesLength = 0;
        if (Algorithm.Ed25519.equals(eddsaPublicKey.getParams().getName())) {
            bytesLength = 32;
        } else if (Algorithm.Ed448.equals(eddsaPublicKey.getParams().getName())) {
            bytesLength = 57;
        } else {
            return Optional.ofNullable(null);
        }

        // consider the case where yCoordinate.toByteArray() is less than bytesLength due to relatively small value of y-coordinate.
        byte[] yCoordinateLittleEndianBytes = new byte[bytesLength];

        // convert big endian representation of BigInteger to little endian representation of JWK representation (RFC 8032,8027)
        byte[] yCoordinateLittleEndian = reverseBytes(yCoordinate.toByteArray());
        System.arraycopy(yCoordinateLittleEndian, 0, yCoordinateLittleEndianBytes, 0, yCoordinateLittleEndian.length);

        // set a parity of x-coordinate to the most significant bit of the last octet (RFC 8032, 8037)
        if (edEcPoint.isXOdd()) {
            yCoordinateLittleEndianBytes[yCoordinateLittleEndianBytes.length - 1] |= -128; // 0b10000000
        }

        return Optional.ofNullable(Base64Url.encode(yCoordinateLittleEndianBytes));
    }

    private static byte[] reverseBytes(byte[] array) {
        if (array == null || array.length == 0) {
            return null;
        }

        int length = array.length;
        byte[] reversedArray = new byte[length];

        for (int i = 0; i < length; i++) {
            reversedArray[length - 1 - i] = array[i];
        }

        return reversedArray;
    }
}
