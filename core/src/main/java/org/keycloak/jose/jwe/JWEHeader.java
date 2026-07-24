/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jose.jwe;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.keycloak.jose.JOSEHeader;
import org.keycloak.jose.jwk.ECPublicJWK;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JWEHeader implements JOSEHeader {

    @JsonProperty("alg")
    private String algorithm;

    @JsonProperty("enc")
    private String encryptionAlgorithm;

    @JsonProperty("zip")
    private String compressionAlgorithm;

    @JsonProperty("typ")
    private String type;

    @JsonProperty("cty")
    private String contentType;

    @JsonProperty("kid")
    private String keyId;

    @JsonProperty("epk")
    private ECPublicJWK ephemeralPublicKey;

    @JsonProperty("apu")
    private String agreementPartyUInfo;

    @JsonProperty("apv")
    private String agreementPartyVInfo;

    public JWEHeader() {
    }

    public JWEHeader(String algorithm, String encryptionAlgorithm, String compressionAlgorithm) {
        this.algorithm = algorithm;
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.compressionAlgorithm = compressionAlgorithm;
    }

    public JWEHeader(String algorithm, String encryptionAlgorithm, String compressionAlgorithm, String keyId) {
        this.algorithm = algorithm;
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.compressionAlgorithm = compressionAlgorithm;
        this.keyId = keyId;
    }

    public JWEHeader(String algorithm, String encryptionAlgorithm, String compressionAlgorithm, String keyId, String contentType) {
        this.algorithm = algorithm;
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.compressionAlgorithm = compressionAlgorithm;
        this.keyId = keyId;
        this.contentType = contentType;
    }

    public JWEHeader(String algorithm, String encryptionAlgorithm, String compressionAlgorithm, String keyId, String contentType, 
            String type, ECPublicJWK ephemeralPublicKey, String agreementPartyUInfo, String agreementPartyVInfo) {
        this.algorithm = algorithm;
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.compressionAlgorithm = compressionAlgorithm;
        this.keyId = keyId;
        this.type = type;
        this.contentType = contentType;
        this.ephemeralPublicKey = ephemeralPublicKey;
        this.agreementPartyUInfo = agreementPartyUInfo;
        this.agreementPartyVInfo = agreementPartyVInfo;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    @JsonIgnore
    @Override
    public String getRawAlgorithm() {
        return getAlgorithm();
    }

    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    public String getCompressionAlgorithm() {
        return compressionAlgorithm;
    }

    public String getType() {
        return type;
    }

    public String getContentType() {
        return contentType;
    }

    public String getKeyId() {
        return keyId;
    }

    public ECPublicJWK getEphemeralPublicKey() {
        return ephemeralPublicKey;
    }

    public String getAgreementPartyUInfo() {
        return agreementPartyUInfo;
    }

    public String getAgreementPartyVInfo() {
        return agreementPartyVInfo;
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public String toString() {
        try {
            return mapper.writeValueAsString(this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public JWEHeaderBuilder toBuilder() {
        return builder().algorithm(algorithm).encryptionAlgorithm(encryptionAlgorithm)
                .compressionAlgorithm(compressionAlgorithm).type(type).contentType(contentType)
                .keyId(keyId).ephemeralPublicKey(ephemeralPublicKey).agreementPartyUInfo(agreementPartyUInfo)
                .agreementPartyVInfo(agreementPartyVInfo);
    }

    public static JWEHeaderBuilder builder() {
        return new JWEHeaderBuilder();
    }

    public static class JWEHeaderBuilder {
        private String algorithm = null;
        private String encryptionAlgorithm = null;
        private String compressionAlgorithm = null;
        private String type = null;
        private String contentType = null;
        private String keyId = null;
        private ECPublicJWK ephemeralPublicKey = null;
        private String agreementPartyUInfo = null;
        private String agreementPartyVInfo = null;

        public JWEHeaderBuilder algorithm(String algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        public JWEHeaderBuilder encryptionAlgorithm(String encryptionAlgorithm) {
            this.encryptionAlgorithm = encryptionAlgorithm;
            return this;
        }

        public JWEHeaderBuilder compressionAlgorithm(String compressionAlgorithm) {
            this.compressionAlgorithm = compressionAlgorithm;
            return this;
        }

        public JWEHeaderBuilder type(String type) {
            this.type = type;
            return this;
        }

        public JWEHeaderBuilder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public JWEHeaderBuilder keyId(String keyId) {
            this.keyId = keyId;
            return this;
        }

        public JWEHeaderBuilder ephemeralPublicKey(ECPublicJWK ephemeralPublicKey) {
            this.ephemeralPublicKey = ephemeralPublicKey;
            return this;
        }

        public JWEHeaderBuilder agreementPartyUInfo(String agreementPartyUInfo) {
            this.agreementPartyUInfo = agreementPartyUInfo;
            return this;
        }

        public JWEHeaderBuilder agreementPartyVInfo(String agreementPartyVInfo) {
            this.agreementPartyVInfo = agreementPartyVInfo;
            return this;
        }

        public JWEHeader build() {
            return new JWEHeader(algorithm, encryptionAlgorithm, compressionAlgorithm, keyId, contentType,
                    type, ephemeralPublicKey, agreementPartyUInfo, agreementPartyVInfo);
        }
    }
}
