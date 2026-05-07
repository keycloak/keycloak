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
package org.keycloak.mdoc;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.EdECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Objects;

import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;

import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.authenticator.EC2COSEKey;
import com.webauthn4j.data.attestation.authenticator.EdDSACOSEKey;
import com.webauthn4j.data.attestation.authenticator.RSACOSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;

/**
 * Holder binding key for an ISO mdoc Mobile Security Object.
 *
 * Wallet proofs arrive as JWT proofs containing a JWK, but mDoc DeviceKeyInfo stores the holder public key as a
 * COSE_Key.
 */
final class MdocDeviceKey {

    private final COSEKey coseKey;

    private MdocDeviceKey(COSEKey coseKey) {
        this.coseKey = Objects.requireNonNull(coseKey, "coseKey");
    }

    static MdocDeviceKey fromProofJwk(JWK jwk) {
        Objects.requireNonNull(jwk, "jwk");

        PublicKey publicKey = JWKParser.create(jwk).toPublicKey();
        COSEAlgorithmIdentifier algorithm = coseAlgorithm(jwk);
        byte[] keyId = jwk.getKeyId() == null ? null : jwk.getKeyId().getBytes(StandardCharsets.UTF_8);

        return switch (jwk.getKeyType()) {
            case KeyType.EC -> new MdocDeviceKey(ecKey(keyId, algorithm, asEcPublicKey(publicKey)));
            case KeyType.RSA -> new MdocDeviceKey(rsaKey(keyId, algorithm, asRsaPublicKey(publicKey)));
            case KeyType.OKP -> new MdocDeviceKey(okpKey(keyId, algorithm, asEdEcPublicKey(publicKey)));
            default -> throw new MdocException("Unsupported proof key type for mDoc COSE_Key: " + jwk.getKeyType());
        };
    }

    COSEKey toCoseKey() {
        return coseKey;
    }

    private static COSEAlgorithmIdentifier coseAlgorithm(JWK jwk) {
        if (jwk.getAlgorithm() == null) {
            return null;
        }
        return MdocAlgorithm.fromJoseAlgorithm(jwk.getAlgorithm()).toCoseAlgorithmIdentifier();
    }

    private static ECPublicKey asEcPublicKey(PublicKey publicKey) {
        if (publicKey instanceof ECPublicKey ecPublicKey) {
            return ecPublicKey;
        }
        throw new MdocException("Unsupported EC proof key for mDoc COSE_Key");
    }

    private static RSAPublicKey asRsaPublicKey(PublicKey publicKey) {
        if (publicKey instanceof RSAPublicKey rsaPublicKey) {
            return rsaPublicKey;
        }
        throw new MdocException("Unsupported RSA proof key for mDoc COSE_Key");
    }

    private static EdECPublicKey asEdEcPublicKey(PublicKey publicKey) {
        if (publicKey instanceof EdECPublicKey edEcPublicKey) {
            return edEcPublicKey;
        }
        throw new MdocException("Unsupported OKP proof key for mDoc COSE_Key");
    }

    private static EC2COSEKey ecKey(byte[] keyId, COSEAlgorithmIdentifier algorithm, ECPublicKey publicKey) {
        EC2COSEKey key = algorithm == null ? EC2COSEKey.create(publicKey) : EC2COSEKey.create(publicKey, algorithm);
        return new EC2COSEKey(keyId, algorithm, null, key.getCurve(), key.getX(), key.getY());
    }

    private static RSACOSEKey rsaKey(byte[] keyId, COSEAlgorithmIdentifier algorithm, RSAPublicKey publicKey) {
        RSACOSEKey key = algorithm == null ? RSACOSEKey.create(publicKey) : RSACOSEKey.create(publicKey, algorithm);
        return new RSACOSEKey(keyId, algorithm, null, key.getN(), key.getE());
    }

    private static EdDSACOSEKey okpKey(byte[] keyId, COSEAlgorithmIdentifier algorithm, EdECPublicKey publicKey) {
        if (!Algorithm.Ed25519.equals(publicKey.getParams().getName())) {
            throw new MdocException("Unsupported OKP proof key curve for mDoc COSE_Key: " + publicKey.getParams().getName());
        }
        EdDSACOSEKey key = algorithm == null ? EdDSACOSEKey.create(publicKey) : EdDSACOSEKey.create(publicKey, algorithm);
        return new EdDSACOSEKey(keyId, algorithm, null, key.getCurve(), key.getX(), null);
    }
}
