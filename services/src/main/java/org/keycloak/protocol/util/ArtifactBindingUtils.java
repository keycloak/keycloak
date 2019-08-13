package org.keycloak.protocol.util;

import org.keycloak.protocol.saml.DefaultSamlArtifactResolverFactory;

import java.util.Base64;

public class ArtifactBindingUtils {
    public static String artifactToResolverProviderId(String artifact) {
        return byteArrayToResolverProviderId(Base64.getDecoder().decode(artifact));
    }
    
    public static String byteArrayToResolverProviderId(byte[] ar) {
        return String.format("%02X%02X", ar[0], ar[1]);
    }
}
