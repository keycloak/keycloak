package org.keycloak.authentication.authenticators.browser;

import org.keycloak.cookie.CookieMaxAge;

public final class TrustedDeviceConstants {
    // Controlling checkbox on forms
    public static final String AUTH_NOTE = "trust-device";
    public static final String AUTH_NOTE_SHOW = "show";
    public static final String AUTH_NOTE_CREATED = "created";

    // Realm attributes
    public static final String REALM_IS_ENABLED_ATTR = "trustedDeviceEnabled";
    public static final String REALM_EXPIRATION_ATTR = "trustedDeviceExpiration";

    public static final int DEFAULT_EXPIRATION = CookieMaxAge.WEEK;

    // Prevent instantiation
    private TrustedDeviceConstants() {
    }
}
