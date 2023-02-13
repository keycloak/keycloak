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

import org.keycloak.common.util.Base64Url;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jws.crypto.HMACProvider;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.util.JsonSerialization;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JWSBuilder {
    String type;
    String kid;
    String contentType;
    byte[] contentBytes;

    public JWSBuilder type(String type) {
        this.type = type;
        return this;
    }

    public JWSBuilder kid(String kid) {
        this.kid = kid;
        return this;
    }

    public JWSBuilder contentType(String type) {
        this.contentType = type;
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
        builder.append("\"alg\":\"").append(sigAlgName).append("\"");

        if (type != null) builder.append(",\"typ\" : \"").append(type).append("\"");
        if (kid != null) builder.append(",\"kid\" : \"").append(kid).append("\"");
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
