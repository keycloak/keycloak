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

import java.util.Collections;
import java.util.List;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;

import java.security.Key;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;

import static org.keycloak.jose.jwk.JWKUtil.toIntegerBytes;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JWKBuilder {

    public static final String DEFAULT_PUBLIC_KEY_USE = "sig";

    private String kid;

    private String algorithm;

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

    public JWK rs256(PublicKey key) {
        algorithm(Algorithm.RS256);
        return rsa(key);
    }

    public JWK rsa(Key key) {
        return rsa(key, null, KeyUse.SIG);
    }
    
    public JWK rsa(Key key, X509Certificate certificate) {
        return rsa(key, Collections.singletonList(certificate), KeyUse.SIG);
    }

    public JWK rsa(Key key, List<X509Certificate> certificates) {
        return rsa(key, certificates, null);
    }

    public JWK rsa(Key key, List<X509Certificate> certificates, KeyUse keyUse) {
        RSAPublicKey rsaKey = (RSAPublicKey) key;

        RSAPublicJWK k = new RSAPublicJWK();

        String kid = this.kid != null ? this.kid : KeyUtils.createKeyId(key);
        k.setKeyId(kid);
        k.setKeyType(KeyType.RSA);
        k.setAlgorithm(algorithm);
        k.setPublicKeyUse(keyUse == null ? KeyUse.SIG.getSpecName() : keyUse.getSpecName());
        k.setModulus(Base64Url.encode(toIntegerBytes(rsaKey.getModulus())));
        k.setPublicExponent(Base64Url.encode(toIntegerBytes(rsaKey.getPublicExponent())));

        if (certificates != null && !certificates.isEmpty()) {
            String[] certificateChain = new String[certificates.size()];
            for (int i = 0; i < certificates.size(); i++) {
                certificateChain[i] = PemUtils.encodeCertificate(certificates.get(i));
            }
            k.setX509CertificateChain(certificateChain);
        }

        return k;
    }

    public JWK rsa(Key key, KeyUse keyUse) {
        JWK k = rsa(key);
        String keyUseString = keyUse == null ? DEFAULT_PUBLIC_KEY_USE : keyUse.getSpecName();
        if (KeyUse.ENC == keyUse) keyUseString = "enc";
        k.setPublicKeyUse(keyUseString);
        return k;
    }

    public JWK ec(Key key) {
        ECPublicKey ecKey = (ECPublicKey) key;

        ECPublicJWK k = new ECPublicJWK();

        String kid = this.kid != null ? this.kid : KeyUtils.createKeyId(key);
        int fieldSize = ecKey.getParams().getCurve().getField().getFieldSize();

        k.setKeyId(kid);
        k.setKeyType(KeyType.EC);
        k.setAlgorithm(algorithm);
        k.setPublicKeyUse(DEFAULT_PUBLIC_KEY_USE);
        k.setCrv("P-" + fieldSize);
        k.setX(Base64Url.encode(toIntegerBytes(ecKey.getW().getAffineX())));
        k.setY(Base64Url.encode(toIntegerBytes(ecKey.getW().getAffineY())));
        
        return k;
    }
}
