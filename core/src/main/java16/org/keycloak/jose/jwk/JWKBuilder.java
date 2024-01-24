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
import java.security.Key;
import java.security.interfaces.EdECPublicKey;
import java.security.spec.EdECPoint;
import java.util.Arrays;
import java.util.Optional;

import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class JWKBuilder extends AbstractJWKBuilder {

    private JWKBuilder() {
    }

    public static JWKBuilder create() {
        return new JWKBuilder();
    }

    public JWKBuilder kid(String kid) {
        this.kid = kid;
        return this;
    }

    public JWKBuilder algorithm(String algorithm) {
        this.algorithm = algorithm;
        return this;
    }

    @Override
    public JWK okp(Key key) {
        return okp(key, DEFAULT_PUBLIC_KEY_USE);
    }

    @Override
    public JWK okp(Key key, KeyUse keyUse) {
        EdECPublicKey eddsaPublicKey = (EdECPublicKey) key;

        OKPPublicJWK k = new OKPPublicJWK();

        String kid = this.kid != null ? this.kid : KeyUtils.createKeyId(key);

        k.setKeyId(kid);
        k.setKeyType(KeyType.OKP);
        k.setAlgorithm(algorithm);
        k.setPublicKeyUse(keyUse == null ? DEFAULT_PUBLIC_KEY_USE.getSpecName() : keyUse.getSpecName());
        k.setCrv(eddsaPublicKey.getParams().getName());

        Optional<String> x = edPublicKeyInJwkRepresentation(eddsaPublicKey);
        k.setX(x.orElse(""));

        return k;
    }

    private Optional<String> edPublicKeyInJwkRepresentation(EdECPublicKey eddsaPublicKey) {
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
        yCoordinateLittleEndianBytes = Arrays.copyOf(reverseBytes(yCoordinate.toByteArray()), bytesLength);

        // set a parity of x-coordinate to the most significant bit of the last octet (RFC 8032, 8037)
        if (edEcPoint.isXOdd()) {
            yCoordinateLittleEndianBytes[yCoordinateLittleEndianBytes.length - 1] |= -128; // 0b10000000
        }

        return Optional.ofNullable(Base64Url.encode(yCoordinateLittleEndianBytes));
    }

}
