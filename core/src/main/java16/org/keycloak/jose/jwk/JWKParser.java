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

package org.keycloak.jose.jwk;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.EdECPoint;
import java.security.spec.EdECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.NamedParameterSpec;

import org.keycloak.common.util.Base64Url;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class JWKParser extends AbstractJWKParser {

    private JWKParser() {
    }

    public static JWKParser create() {
        return new JWKParser();
    }

    public JWKParser(JWK jwk) {
        this.jwk = jwk;
    }

    public static JWKParser create(JWK jwk) {
        return new JWKParser(jwk);
    }

    public JWKParser parse(String jwk) {
        try {
            this.jwk = JsonSerialization.mapper.readValue(jwk, JWK.class);
            return this;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PublicKey toPublicKey() {
        String keyType = jwk.getKeyType();
        if (keyType.equals(KeyType.RSA)) {
            return createRSAPublicKey();
        } else if (keyType.equals(KeyType.EC)) {
            return createECPublicKey();
        } else if (keyType.equals(KeyType.OKP)) {
            return createOKPPublicKey();
        } else {
            throw new RuntimeException("Unsupported keyType " + keyType);
        }
    }

    private PublicKey createOKPPublicKey() {
        String x = (String) jwk.getOtherClaims().get(OKPPublicJWK.X);
        String crv = (String) jwk.getOtherClaims().get(OKPPublicJWK.CRV);
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
        BigInteger y = new BigInteger(1, JWKBuilder.reverseBytes(decodedX));
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

    @Override
    public boolean isKeyTypeSupported(String keyType) {
        return (RSAPublicJWK.RSA.equals(keyType) || ECPublicJWK.EC.equals(keyType) || OKPPublicJWK.OKP.equals(keyType));
    }

}