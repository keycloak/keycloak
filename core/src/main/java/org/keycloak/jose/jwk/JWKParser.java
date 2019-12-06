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

package org.keycloak.jose.jwk;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.keycloak.common.util.Base64Url;
import org.keycloak.crypto.KeyType;
import org.keycloak.util.JsonSerialization;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JWKParser {

    private JWK jwk;

    private JWKParser() {
    }

    public JWKParser(JWK jwk) {
        this.jwk = jwk;
    }

    public static JWKParser create() {
        return new JWKParser();
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

    public JWK getJwk() {
        return jwk;
    }

    public PublicKey toPublicKey() {
        String keyType = jwk.getKeyType();
        if (keyType.equals(KeyType.RSA)) {
            return createRSAPublicKey();
        } else if (keyType.equals(KeyType.EC)) {
            return createECPublicKey();

        } else {
            throw new RuntimeException("Unsupported keyType " + keyType);
        }
    }

    private PublicKey createECPublicKey() {
        String crv = (String) jwk.getOtherClaims().get(ECPublicJWK.CRV);
        BigInteger x = new BigInteger(1, Base64Url.decode((String) jwk.getOtherClaims().get(ECPublicJWK.X)));
        BigInteger y = new BigInteger(1, Base64Url.decode((String) jwk.getOtherClaims().get(ECPublicJWK.Y)));

        String name;
        switch (crv) {
            case "P-256" :
                name = "secp256r1";
                break;
            case "P-384" :
                name = "secp384r1";
                break;
            case "P-521" :
                name = "secp521r1";
                break;
            default :
                throw new RuntimeException("Unsupported curve");
        }

        try {
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(name);
            ECNamedCurveSpec params = new ECNamedCurveSpec("prime256v1", spec.getCurve(), spec.getG(), spec.getN());
            ECPoint point = new ECPoint(x, y);
            ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(point, params);

            KeyFactory kf = KeyFactory.getInstance("ECDSA");
            return kf.generatePublic(pubKeySpec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PublicKey createRSAPublicKey() {
        BigInteger modulus = new BigInteger(1, Base64Url.decode(jwk.getOtherClaims().get(RSAPublicJWK.MODULUS).toString()));
        BigInteger publicExponent = new BigInteger(1, Base64Url.decode(jwk.getOtherClaims().get(RSAPublicJWK.PUBLIC_EXPONENT).toString()));

        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(new RSAPublicKeySpec(modulus, publicExponent));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isKeyTypeSupported(String keyType) {
        return (RSAPublicJWK.RSA.equals(keyType) || ECPublicJWK.EC.equals(keyType));
    }

}
