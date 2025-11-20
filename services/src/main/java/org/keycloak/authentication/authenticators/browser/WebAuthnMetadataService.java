package org.keycloak.authentication.authenticators.browser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.FileUtils;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Provides metadata for WebAuthn credentials
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class WebAuthnMetadataService {

    // Based on https://github.com/duo-labs/webauthn.io/blob/master/_app/homepage/services/metadata.py
    private static final String FILE_NAME = "keycloak-webauthn-metadata.json";

    private Map<String, String> aaguidToProviderNames;

    private Map<String, String> readMetadata() {
        try {
            try (InputStream is = FileUtils.getJsonFileFromClasspathOrConfFolder(FILE_NAME)) {
                Map<String, Map<String, String>> map = JsonSerialization.readValue(is, new TypeReference<>() {});
                return map.entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                                    String value = entry.getValue().get("name");
                                    if (value == null) {
                                        throw new IllegalStateException("Not found 'name' for the AAGUID '" + entry.getKey() + "' in the file '" + FILE_NAME + "'.");
                                    }
                                    return value;
                                }));
            }
        } catch (IOException ioe) {
            throw new IllegalStateException("Error loading the webauthn metadata from file " + FILE_NAME, ioe);
        }

    }

    private Map<String, String> getAaguidToProviderNames() {
        if (aaguidToProviderNames == null) {
            synchronized (this) {
                if (aaguidToProviderNames == null) {
                    // Make sure the file is not parsed during server startup, but "lazily" when needed for the 1st time
                    this.aaguidToProviderNames = readMetadata();
                }
            }
        }
        return aaguidToProviderNames;
    }

    public String getAuthenticatorProvider(String aaguid) {
        return aaguid == null ? null : getAaguidToProviderNames().get(aaguid);
    }
}
