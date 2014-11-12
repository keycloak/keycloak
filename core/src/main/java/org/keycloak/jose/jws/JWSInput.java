package org.keycloak.jose.jws;

import org.keycloak.util.Base64Url;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JWSInput {
    String wireString;
    String encodedHeader;
    String encodedContent;
    String encodedSignature;
    String encodedSignatureInput;
    JWSHeader header;
    byte[] content;
    byte[] signature;


    public JWSInput(String wire) {
        this.wireString = wire;
        String[] parts = wire.split("\\.");
        if (parts.length < 2 || parts.length > 3) throw new IllegalArgumentException("Parsing error");
        encodedHeader = parts[0];
        encodedContent = parts[1];
        encodedSignatureInput = encodedHeader + '.' + encodedContent;
        try {
            content = Base64Url.decode(encodedContent);
            if (parts.length > 2) {
                encodedSignature = parts[2];
                signature = Base64Url.decode(encodedSignature);

            }
            byte[] headerBytes = Base64Url.decode(encodedHeader);
            header = JsonSerialization.readValue(headerBytes, JWSHeader.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getWireString() {
        return wireString;
    }

    public String getEncodedHeader() {
        return encodedHeader;
    }

    public String getEncodedContent() {
        return encodedContent;
    }

    public String getEncodedSignature() {
        return encodedSignature;
    }
    public String getEncodedSignatureInput() {
        return encodedSignatureInput;
    }

    public JWSHeader getHeader() {
        return header;
    }

    public byte[] getContent() {
        return content;
    }

    public byte[] getSignature() {
        return signature;
    }

    public <T> T readJsonContent(Class<T> type) throws IOException {
        return JsonSerialization.readValue(content, type);
    }

    public String readContentAsString() {
        try {
            return new String(content, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
