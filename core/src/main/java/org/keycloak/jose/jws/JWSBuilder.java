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

package org.keycloak.jose.jws;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;

import org.keycloak.common.util.Base64Url;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.crypto.HMACProvider;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JWSBuilder {
    protected String type;
    protected String kid;
    protected String x5t;
    protected JWK jwk;
    protected List<String> x5c;
    protected String contentType;
    protected byte[] contentBytes;

    public JWSBuilder type(String type) {
        this.type = type;
        return this;
    }

    public JWSBuilder kid(String kid) {
        this.kid = kid;
        return this;
    }

    public JWSBuilder x5t(String x5t) {
        this.x5t = x5t;
        return this;
    }

    public JWSBuilder jwk(JWK jwk) {
        this.jwk = jwk;
        return this;
    }

    public JWSBuilder x5c(List<X509Certificate> x5c) {
        this.x5c = x5c.stream()
                .map(x509Certificate -> {
                    try {
                        return Base64.getEncoder().encodeToString(x509Certificate.getEncoded());
                    } catch (CertificateEncodingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        return this;
    }

    public JWSBuilder contentType(String type) {
        this.contentType = type;
        return this;
    }

    public JWSBuilder header(JWSHeader header) {
        this.type = header.getType();
        this.kid = header.getKeyId();
        this.jwk = header.getKey();
        this.x5c = header.getX5c();
        this.contentType = header.getContentType();
        return this;
    }

    public EncodingBuilder content(byte[] bytes) {
        this.contentBytes = bytes;
        return new EncodingBuilder();
    }

    public EncodingBuilder jsonContent(Object object) {
        try {
            this.contentBytes = JsonSerialization.writeValueAsBytes(object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new EncodingBuilder();
    }


    protected String encodeHeader(String sigAlgName) {
        StringBuilder builder = new StringBuilder("{");

        if (org.keycloak.crypto.Algorithm.Ed25519.equals(sigAlgName) || org.keycloak.crypto.Algorithm.Ed448.equals(sigAlgName)) {
            builder.append("\"alg\":\"").append(org.keycloak.crypto.Algorithm.EdDSA).append("\"");
            builder.append(",\"crv\":\"").append(sigAlgName).append("\"");
        } else {
            builder.append("\"alg\":\"").append(sigAlgName).append("\"");
        }

        if (type != null) builder.append(",\"typ\" : \"").append(type).append("\"");
        if (kid != null) builder.append(",\"kid\" : \"").append(kid).append("\"");
        if (x5t != null) builder.append(",\"x5t\" : \"").append(x5t).append("\"");
        if (x5c != null && !x5c.isEmpty()) {
            builder.append(",\"x5c\" : [");
            for (int i = 0; i < x5c.size(); i++) {
                String certificate = x5c.get(i);
                if (i > 0) {
                    builder.append(",");
                }
                builder.append("\"").append(certificate).append("\"");
            }
            builder.append("]");
        }
        if (jwk != null) {
            try {
                builder.append(",\"jwk\" : ").append(JsonSerialization.mapper.writeValueAsString(jwk));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        if (contentType != null) builder.append(",\"cty\":\"").append(contentType).append("\"");
        builder.append("}");
        return Base64Url.encode(builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    protected String encodeAll(StringBuilder encoding, byte[] signature) {
        encoding.append('.');
        if (signature != null) {
            encoding.append(Base64Url.encode(signature));
        }
        return encoding.toString();
    }

    protected void encode(Algorithm alg, byte[] data, StringBuilder encoding) {
        encode(alg.name(), data, encoding);
    }

    protected void encode(String sigAlgName, byte[] data, StringBuilder encoding) {
        encoding.append(encodeHeader(sigAlgName));
        encoding.append('.');
        encoding.append(Base64Url.encode(data));
    }

    protected byte[] marshalContent() {
        return contentBytes;
    }

    public class EncodingBuilder {

        public String sign(SignatureSignerContext signer) {
            kid = signer.getKid();

            StringBuilder buffer = new StringBuilder();
            byte[] data = marshalContent();
            encode(signer.getAlgorithm(), data, buffer);
            byte[] signature = null;
            try {
                signature = signer.sign(buffer.toString().getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return encodeAll(buffer, signature);
        }

        public String none() {
            StringBuilder buffer = new StringBuilder();
            byte[] data = marshalContent();
            encode(Algorithm.none, data, buffer);
            return encodeAll(buffer, null);
        }

        @Deprecated
        public String sign(Algorithm algorithm, PrivateKey privateKey) {
            StringBuilder buffer = new StringBuilder();
            byte[] data = marshalContent();
            encode(algorithm, data, buffer);
            byte[] signature = RSAProvider.sign(buffer.toString().getBytes(StandardCharsets.UTF_8), algorithm, privateKey);
            return encodeAll(buffer, signature);
        }

        @Deprecated
        public String rsa256(PrivateKey privateKey) {
            return sign(Algorithm.RS256, privateKey);
        }

        @Deprecated
        public String rsa384(PrivateKey privateKey) {
            return sign(Algorithm.RS384, privateKey);
        }

        @Deprecated
        public String rsa512(PrivateKey privateKey) {
            return sign(Algorithm.RS512, privateKey);
        }

        @Deprecated
        public String hmac256(byte[] sharedSecret) {
            StringBuilder buffer = new StringBuilder();
            byte[] data = marshalContent();
            encode(Algorithm.HS256, data, buffer);
            byte[] signature = HMACProvider.sign(buffer.toString().getBytes(StandardCharsets.UTF_8), Algorithm.HS256, sharedSecret);
            return encodeAll(buffer, signature);
        }

        @Deprecated
        public String hmac384(byte[] sharedSecret) {
            StringBuilder buffer = new StringBuilder();
            byte[] data = marshalContent();
            encode(Algorithm.HS384, data, buffer);
            byte[] signature = HMACProvider.sign(buffer.toString().getBytes(StandardCharsets.UTF_8), Algorithm.HS384, sharedSecret);
            return encodeAll(buffer, signature);
        }

        @Deprecated
        public String hmac512(byte[] sharedSecret) {
            StringBuilder buffer = new StringBuilder();
            byte[] data = marshalContent();
            encode(Algorithm.HS512, data, buffer);
            byte[] signature = HMACProvider.sign(buffer.toString().getBytes(StandardCharsets.UTF_8), Algorithm.HS512, sharedSecret);
            return encodeAll(buffer, signature);
        }

        @Deprecated
        public String hmac256(SecretKey sharedSecret) {
            StringBuilder buffer = new StringBuilder();
            byte[] data = marshalContent();
            encode(Algorithm.HS256, data, buffer);
            byte[] signature = HMACProvider.sign(buffer.toString().getBytes(StandardCharsets.UTF_8), Algorithm.HS256, sharedSecret);
            return encodeAll(buffer, signature);
        }

        @Deprecated
        public String hmac384(SecretKey sharedSecret) {
            StringBuilder buffer = new StringBuilder();
            byte[] data = marshalContent();
            encode(Algorithm.HS384, data, buffer);
            byte[] signature = HMACProvider.sign(buffer.toString().getBytes(StandardCharsets.UTF_8), Algorithm.HS384, sharedSecret);
            return encodeAll(buffer, signature);
        }

        @Deprecated
        public String hmac512(SecretKey sharedSecret) {
            StringBuilder buffer = new StringBuilder();
            byte[] data = marshalContent();
            encode(Algorithm.HS512, data, buffer);
            byte[] signature = HMACProvider.sign(buffer.toString().getBytes(StandardCharsets.UTF_8), Algorithm.HS512, sharedSecret);
            return encodeAll(buffer, signature);
        }
    }
}
