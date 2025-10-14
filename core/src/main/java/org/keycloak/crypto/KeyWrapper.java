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
package org.keycloak.crypto;

import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;

public class KeyWrapper {

    private String providerId;
    private long providerPriority;
    private String kid;
    private String algorithm;
    private String type;
    private KeyUse use;
    private KeyStatus status;
    private SecretKey secretKey;
    private Key publicKey;
    private Key privateKey;
    private X509Certificate certificate;
    private List<X509Certificate> certificateChain;
    private boolean isDefaultClientCertificate;
    private String curve;

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public long getProviderPriority() {
        return providerPriority;
    }

    public void setProviderPriority(long providerPriority) {
        this.providerPriority = providerPriority;
    }

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    /**
     * <p>Returns the value of the optional {@code alg} claim.
     *
     * @return the algorithm value
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * <p>Returns the value of the optional {@code alg} claim. If not defined, a default is
     * inferred for some algorithms.
     *
     * <p>For keys of type {@link KeyType#RSA}, the default algorithm is {@link Algorithm#RS256} as this is the default
     * algorithm recommended by OIDC specs.
     *
     * <p>For keys of type {@link KeyType#EC}, {@link Algorithm#ES256}, {@link Algorithm#ES384}, or {@link Algorithm#ES512}
     * is returned based on the curve
     *
     * @return the algorithm set or a default based on the key type.
     */
    public String getAlgorithmOrDefault() {
        if (algorithm == null && type != null) {
            switch (type) {
                case KeyType.EC:
                    if (curve != null) {
                        switch (curve) {
                            case "P-256":
                                return Algorithm.ES256;
                            case "P-384":
                                return Algorithm.ES384;
                            case "P-512":
                                return Algorithm.ES512;
                        }
                    }
                case KeyType.RSA:
                    return Algorithm.RS256;
            }
        }
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public KeyUse getUse() {
        return use;
    }

    public void setUse(KeyUse use) {
        this.use = use;
    }

    public KeyStatus getStatus() {
        return status;
    }

    public void setStatus(KeyStatus status) {
        this.status = status;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public Key getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(Key privateKey) {
        this.privateKey = privateKey;
    }

    public Key getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(Key publicKey) {
        this.publicKey = publicKey;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }

    public List<X509Certificate> getCertificateChain() {
        return certificateChain;
    }

    public void setCertificateChain(List<X509Certificate> certificateChain) {
        this.certificateChain = certificateChain;
    }

    public boolean isDefaultClientCertificate() {
        return isDefaultClientCertificate;
    }

    public void setIsDefaultClientCertificate(boolean isDefaultClientCertificate) {
        this.isDefaultClientCertificate = isDefaultClientCertificate;
    }

    public void setCurve(String curve) {
        this.curve = curve;
    }

    public String getCurve() {
        return curve;
    }

    public KeyWrapper cloneKey() {
        KeyWrapper key = new KeyWrapper();
        key.providerId = this.providerId;
        key.providerPriority = this.providerPriority;
        key.kid = this.kid;
        key.algorithm = this.algorithm;
        key.type = this.type;
        key.use = this.use;
        key.status = this.status;
        key.secretKey = this.secretKey;
        key.publicKey = this.publicKey;
        key.privateKey = this.privateKey;
        key.certificate = this.certificate;
        key.curve = this.curve;
        if (this.certificateChain != null) {
            key.certificateChain = new ArrayList<>(this.certificateChain);
        }
        key.isDefaultClientCertificate = this.isDefaultClientCertificate;
        return key;
    }
}
