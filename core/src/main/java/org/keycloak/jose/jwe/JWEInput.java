package org.keycloak.jose.jwe;

import org.keycloak.common.util.Base64Url;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.util.JsonSerialization;

public class JWEInput {

    private final String wireString;

    private final String encodedHeader;

    private final String encodedEncryptedKey;

    private final String encodedInitializationVector;

    private final String encodedCipherText;

    private final String encodedAuthenticationTag;

    private final JWEHeader header;

    private final byte[] encryptedKey;

    private final byte[] initializationVector;

    private final byte[] cipherText;

    private final byte[] authenticationTag;

    public JWEInput(String wire) throws JWSInputException {

        try {
            this.wireString = wire;

            String[] parts = wire.split("\\.");
            if (parts.length < 4 || parts.length > 5){
                throw new IllegalArgumentException("Parsing error");
            }

            encodedHeader = parts[0];
            encodedEncryptedKey = parts[1];
            encodedInitializationVector = parts[2];
            encodedCipherText = parts[3];
            encodedAuthenticationTag = parts[4];

            encryptedKey = Base64Url.decode(encodedEncryptedKey);
            initializationVector = Base64Url.decode(encodedInitializationVector);
            cipherText  = Base64Url.decode(encodedCipherText);
            authenticationTag = Base64Url.decode(encodedAuthenticationTag);

            byte[] headerBytes = Base64Url.decode(encodedHeader);
            header = JsonSerialization.readValue(headerBytes, JWEHeader.class);
        } catch (Throwable t) {
            throw new JWSInputException(t);
        }
    }

    public String getWireString() {
        return wireString;
    }

    public String getEncodedHeader() {
        return encodedHeader;
    }

    public String getEncodedEncryptedKey() {
        return encodedEncryptedKey;
    }

    public String getEncodedInitializationVector() {
        return encodedInitializationVector;
    }

    public String getEncodedCipherText() {
        return encodedCipherText;
    }

    public String getEncodedAuthenticationTag() {
        return encodedAuthenticationTag;
    }

    public JWEHeader getHeader() {
        return header;
    }

    public byte[] getEncryptedKey() {
        return encryptedKey;
    }

    public byte[] getInitializationVector() {
        return initializationVector;
    }

    public byte[] getCipherText() {
        return cipherText;
    }

    public byte[] getAuthenticationTag() {
        return authenticationTag;
    }
}
