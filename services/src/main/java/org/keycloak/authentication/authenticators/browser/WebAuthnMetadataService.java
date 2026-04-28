package org.keycloak.authentication.authenticators.browser;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.FileUtils;

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
    private static final int MAX_ICON_SIZE = 65_535;

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
                Map<String, String> iconPool = new HashMap<>();
                Map<String, WebAuthnAuthenticatorMetadata> result = new HashMap<>();
                for (Map.Entry<String, WebAuthnAuthenticatorMetadata> entry : parsed.entrySet()) {
                    String aaguid = entry.getKey();
                    WebAuthnAuthenticatorMetadata m = entry.getValue();
                    if (m.name() == null) {
                        throw new IllegalStateException("Not found 'name' for the AAGUID '" + aaguid + "' in the file '" + FILE_NAME + "'.");
                    }
                    String iconLight = filterOversizedIcon(aaguid, m.name(), intern(iconPool, m.iconLight()));
                    String iconDark = filterOversizedIcon(aaguid, m.name(), intern(iconPool, m.iconDark()));
                    result.put(aaguid, new WebAuthnAuthenticatorMetadata(m.name(), iconLight, iconDark));
                }
                return result;
            }
        } catch (IOException ioe) {
            throw new IllegalStateException("Error loading the webauthn metadata from file " + FILE_NAME, ioe);
        }
    }

    private static String filterOversizedIcon(String aaguid, String name, String icon) {
        if (icon != null && icon.length() > MAX_ICON_SIZE) {
            logger.debugf("Skipping oversized icon (%d bytes) for authenticator '%s' (AAGUID: %s)", icon.length(), name, aaguid);
            return null;
        }
        return icon;
    }

    // Deduplicates icon strings so identical data URIs share one object in memory
    private static String intern(Map<String, String> pool, String value) {
        if (value == null) return null;
        return pool.computeIfAbsent(value, v -> v);
    }

    public WebAuthnAuthenticatorMetadata getAuthenticatorMetadata(String aaguid) {
        return aaguid == null ? null : getAaguidToMetadata().get(aaguid);
    }

    public String getAuthenticatorProvider(String aaguid) {
        WebAuthnAuthenticatorMetadata metadata = getAuthenticatorMetadata(aaguid);
        return metadata == null ? null : metadata.name();
    }
}
