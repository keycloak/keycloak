package org.keycloak.protocol.saml.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class ArtifactBindingUtils {
    public static String artifactToResolverProviderId(String artifact) {
        return byteArrayToResolverProviderId(Base64.getDecoder().decode(artifact));
    }
    
    public static String byteArrayToResolverProviderId(byte[] ar) {
        return String.format("%02X%02X", ar[0], ar[1]);
    }

    /**
     * Computes identifier from the given String, for example, from entityId
     *
     * @param identifierFrom String that will be turned into an identifier
     * @return Base64 of SHA-1 hash of the identifierFrom
     */
    public static String computeArtifactBindingIdentifierString(String identifierFrom) {
        return Base64.getEncoder().encodeToString(computeArtifactBindingIdentifier(identifierFrom));
    }

    /**
     * Turns byte representation of the identifier into readable String
     *
     * @param identifier byte representation of the identifier
     * @return Base64 of the identifier
     */
    public static String getArtifactBindingIdentifierString(byte[] identifier) {
        return Base64.getEncoder().encodeToString(identifier);
    }

    /**
     * Computes 20 bytes long byte identifier of the given string, for example, from entityId
     *
     * @param identifierFrom String that will be turned into an identifier
     * @return SHA-1 hash of the given identifierFrom
     */
    public static byte[] computeArtifactBindingIdentifier(String identifierFrom) {
        try {
            MessageDigest sha1Digester = MessageDigest.getInstance("SHA-1");
            return sha1Digester.digest(identifierFrom.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("JVM does not support required cryptography algorithms: SHA-1/SHA1PRNG.", e);
        }
    }
}
