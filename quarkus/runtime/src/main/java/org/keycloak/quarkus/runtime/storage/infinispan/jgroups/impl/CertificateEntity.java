/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.storage.infinispan.jgroups.impl;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.common.util.PemUtils;

/**
 * JPA entity to store the {@link X509Certificate} and {@link KeyPair}.
 */
@SuppressWarnings("unused")
public class CertificateEntity {

    @JsonProperty("prvKey")
    private String privateKeyPem;

    @JsonProperty("pubKey")
    private String publicKeyPem;

    @JsonProperty("crt")
    private String certificatePem;

    public CertificateEntity() {
    }

    public CertificateEntity(String privateKeyPem, String publicKeyPem, String certificatePem) {
        this.privateKeyPem = Objects.requireNonNull(privateKeyPem);
        this.publicKeyPem = Objects.requireNonNull(publicKeyPem);
        this.certificatePem = Objects.requireNonNull(certificatePem);
    }

    public String getCertificatePem() {
        return certificatePem;
    }

    public void setCertificatePem(String certificatePem) {
        this.certificatePem = certificatePem;
    }

    public String getPrivateKeyPem() {
        return privateKeyPem;
    }

    public void setPrivateKeyPem(String privateKeyPem) {
        this.privateKeyPem = privateKeyPem;
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public void setPublicKeyPem(String publicKeyPem) {
        this.publicKeyPem = publicKeyPem;
    }

    public void setCertificate(X509Certificate certificate) {
        Objects.requireNonNull(certificate);
        setCertificatePem(PemUtils.encodeCertificate(certificate));
    }

    public void setKeyPair(KeyPair keyPair) {
        Objects.requireNonNull(keyPair);
        setPrivateKeyPem(PemUtils.encodeKey(keyPair.getPrivate()));
        setPublicKeyPem(PemUtils.encodeKey(keyPair.getPublic()));
    }

    public X509Certificate getCertificate() {
        return PemUtils.decodeCertificate(getCertificatePem());
    }

    public KeyPair getKeyPair() {
        var prv = PemUtils.decodePrivateKey(getPrivateKeyPem());
        var pub = PemUtils.decodePublicKey(getPublicKeyPem());
        return new KeyPair(pub, prv);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        CertificateEntity that = (CertificateEntity) o;
        return Objects.equals(privateKeyPem, that.privateKeyPem) &&
                Objects.equals(publicKeyPem, that.publicKeyPem) &&
                Objects.equals(certificatePem, that.certificatePem);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(privateKeyPem);
        result = 31 * result + Objects.hashCode(publicKeyPem);
        result = 31 * result + Objects.hashCode(certificatePem);
        return result;
    }
}
