package org.keycloak.jose.jws;

import org.keycloak.jose.jws.crypto.HMACProvider;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.util.Base64Url;
import org.keycloak.util.JsonSerialization;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JWSBuilder {
    String type;
    String contentType;
    byte[] contentBytes;

    public JWSBuilder type(String type) {
        this.type = type;
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


    protected String encodeHeader(Algorithm alg) {
        StringBuilder builder = new StringBuilder("{");
        builder.append("\"alg\":\"").append(alg.toString()).append("\"");

        if (type != null) builder.append(",\"typ\" : \"").append(type).append("\"");
        if (contentType != null) builder.append(",\"cty\":\"").append(contentType).append("\"");
        builder.append("}");
        try {
            return Base64Url.encode(builder.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    protected String encodeAll(StringBuffer encoding, byte[] signature) {
        encoding.append('.');
        if (signature != null) {
            encoding.append(Base64Url.encode(signature));
        }
        return encoding.toString();
    }

    protected void encode(Algorithm alg, byte[] data, StringBuffer encoding) {
        encoding.append(encodeHeader(alg));
        encoding.append('.');
        encoding.append(Base64Url.encode(data));
    }

    protected byte[] marshalContent() {
        return contentBytes;
    }

    public class EncodingBuilder {
        public String none() {
            StringBuffer buffer = new StringBuffer();
            byte[] data = marshalContent();
            encode(Algorithm.none, data, buffer);
            return encodeAll(buffer, null);
        }

        public String rsa256(PrivateKey privateKey) {
            StringBuffer buffer = new StringBuffer();
            byte[] data = marshalContent();
            encode(Algorithm.RS256, data, buffer);
            byte[] signature = null;
            try {
                signature = RSAProvider.sign(buffer.toString().getBytes("UTF-8"), Algorithm.RS256, privateKey);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return encodeAll(buffer, signature);
        }

        public String rsa384(PrivateKey privateKey) {
            StringBuffer buffer = new StringBuffer();
            byte[] data = marshalContent();
            encode(Algorithm.RS384, data, buffer);
            byte[] signature = null;
            try {
                signature = RSAProvider.sign(buffer.toString().getBytes("UTF-8"), Algorithm.RS384, privateKey);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return encodeAll(buffer, signature);
        }

        public String rsa512(PrivateKey privateKey) {
            StringBuffer buffer = new StringBuffer();
            byte[] data = marshalContent();
            encode(Algorithm.RS512, data, buffer);
            byte[] signature = null;
            try {
                signature = RSAProvider.sign(buffer.toString().getBytes("UTF-8"), Algorithm.RS512, privateKey);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return encodeAll(buffer, signature);
        }


        public String hmac256(byte[] sharedSecret) {
            StringBuffer buffer = new StringBuffer();
            byte[] data = marshalContent();
            encode(Algorithm.HS256, data, buffer);
            byte[] signature = null;
            try {
                signature = HMACProvider.sign(buffer.toString().getBytes("UTF-8"), Algorithm.HS256, sharedSecret);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return encodeAll(buffer, signature);
        }

        public String hmac384(byte[] sharedSecret) {
            StringBuffer buffer = new StringBuffer();
            byte[] data = marshalContent();
            encode(Algorithm.HS384, data, buffer);
            byte[] signature = null;
            try {
                signature = HMACProvider.sign(buffer.toString().getBytes("UTF-8"), Algorithm.HS384, sharedSecret);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return encodeAll(buffer, signature);
        }

        public String hmac512(byte[] sharedSecret) {
            StringBuffer buffer = new StringBuffer();
            byte[] data = marshalContent();
            encode(Algorithm.HS512, data, buffer);
            byte[] signature = null;
            try {
                signature = HMACProvider.sign(buffer.toString().getBytes("UTF-8"), Algorithm.HS512, sharedSecret);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return encodeAll(buffer, signature);
        }

        public String hmac256(SecretKey sharedSecret) {
            StringBuffer buffer = new StringBuffer();
            byte[] data = marshalContent();
            encode(Algorithm.HS256, data, buffer);
            byte[] signature = null;
            try {
                signature = HMACProvider.sign(buffer.toString().getBytes("UTF-8"), Algorithm.HS256, sharedSecret);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return encodeAll(buffer, signature);
        }

        public String hmac384(SecretKey sharedSecret) {
            StringBuffer buffer = new StringBuffer();
            byte[] data = marshalContent();
            encode(Algorithm.HS384, data, buffer);
            byte[] signature = null;
            try {
                signature = HMACProvider.sign(buffer.toString().getBytes("UTF-8"), Algorithm.HS384, sharedSecret);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return encodeAll(buffer, signature);
        }

        public String hmac512(SecretKey sharedSecret) {
            StringBuffer buffer = new StringBuffer();
            byte[] data = marshalContent();
            encode(Algorithm.HS512, data, buffer);
            byte[] signature = null;
            try {
                signature = HMACProvider.sign(buffer.toString().getBytes("UTF-8"), Algorithm.HS512, sharedSecret);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return encodeAll(buffer, signature);
        }
    }
}
