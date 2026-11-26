package org.keycloak.authentication.authenticators.browser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.FileUtils;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;

/**
 * Provides metadata for WebAuthn credentials.
 * Based on <a href="https://github.com/passkeydeveloper/passkey-authenticator-aaguids">passkey-authenticator-aaguids</a>
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class WebAuthnMetadataService {

    private static final Logger logger = Logger.getLogger(WebAuthnMetadataService.class);
    private static final String FILE_NAME = "keycloak-webauthn-metadata.json";

    private static volatile Map<String, WebAuthnAuthenticatorMetadata> aaguidToMetadata;

    public static void setDefaultMetadata(Map<String, WebAuthnAuthenticatorMetadata> metadata) {
        if (aaguidToMetadata == null) {
            aaguidToMetadata = metadata;
        }
    }

    private Map<String, WebAuthnAuthenticatorMetadata> getAaguidToMetadata() {
        if (aaguidToMetadata == null) {
            synchronized (this) {
                if (aaguidToMetadata == null) {
                    aaguidToMetadata = parseMetadata();
                }
            }
        }
        return aaguidToMetadata;
    }

    public static Map<String, WebAuthnAuthenticatorMetadata> parseMetadata() {
        try {
            try (InputStream is = FileUtils.getJsonFileFromClasspathOrConfFolder(FILE_NAME)) {
                Map<String, WebAuthnAuthenticatorMetadata> parsed = JsonSerialization.readValue(is, new TypeReference<>() {});
                for (Map.Entry<String, WebAuthnAuthenticatorMetadata> entry : parsed.entrySet()) {
                    if (StringUtil.isBlank(entry.getValue().name())) {
                        throw new IllegalStateException("Not found 'name' for the AAGUID '" + entry.getKey() + "' in the file '" + FILE_NAME + "'.");
                    }
                }
                return parsed;
            }
        } catch (IOException ioe) {
            throw new IllegalStateException("Error loading the webauthn metadata from file " + FILE_NAME, ioe);
        }
    }

    public WebAuthnAuthenticatorMetadata getAuthenticatorMetadata(String aaguid) {
        return aaguid == null ? null : getAaguidToMetadata().get(aaguid);
    }

    public String getAuthenticatorProvider(String aaguid) {
        WebAuthnAuthenticatorMetadata metadata = getAuthenticatorMetadata(aaguid);
        return metadata == null ? null : metadata.name();
    }
}
