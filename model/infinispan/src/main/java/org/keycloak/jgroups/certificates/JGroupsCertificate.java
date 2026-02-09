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

package org.keycloak.jgroups.certificates;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Objects;

import org.keycloak.common.util.PemUtils;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * JPA entity to store the {@link X509Certificate} and {@link KeyPair}.
 */
@SuppressWarnings("unused")
public class JGroupsCertificate {

    @JsonProperty("prvKey")
    private String privateKeyPem;

    @JsonProperty("pubKey")
    private String publicKeyPem;

    @JsonProperty("crt")
    private String certificatePem;

    @JsonProperty("alias")
    private String alias;

    @JsonProperty("generatedMillis")
    private long generatedMillis;

    public JGroupsCertificate() {
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

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public long getGeneratedMillis() {
        return generatedMillis;
    }

    public void setGeneratedMillis(long generatedMillis) {
        this.generatedMillis = generatedMillis;
    }

    @JsonIgnore
    public void setCertificate(X509Certificate certificate) {
        Objects.requireNonNull(certificate);
        setCertificatePem(PemUtils.encodeCertificate(certificate));
    }

    @JsonIgnore
    public void setKeyPair(KeyPair keyPair) {
        Objects.requireNonNull(keyPair);
        setPrivateKeyPem(PemUtils.encodeKey(keyPair.getPrivate()));
        setPublicKeyPem(PemUtils.encodeKey(keyPair.getPublic()));
    }

    @JsonIgnore
    public X509Certificate getCertificate() {
        return PemUtils.decodeCertificate(getCertificatePem());
    }

    @JsonIgnore
    public KeyPair getKeyPair() {
        var prv = PemUtils.decodePrivateKey(getPrivateKeyPem());
        var pub = PemUtils.decodePublicKey(getPublicKeyPem());
        return new KeyPair(pub, prv);
    }

    @JsonIgnore
    public PrivateKey getPrivateKey() {
        return PemUtils.decodePrivateKey(getPrivateKeyPem());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JGroupsCertificate that = (JGroupsCertificate) o;
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

    public boolean isSameAlias(String jsonCertificate) {
        return Objects.equals(alias, fromJson(jsonCertificate).getAlias());
    }

    public static String toJson(JGroupsCertificate entity) {
        try {
            return JsonSerialization.mapper.writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Should never happen!", e);
        }
    }

    public static JGroupsCertificate fromJson(String json) {
        try {
            return JsonSerialization.mapper.readValue(json, JGroupsCertificate.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Should never happen!", e);
        }
    }
}
