package org.keycloak.cookie;

import jakarta.ws.rs.core.NewCookie;

public enum CookieScope {
    // Internal cookies are only available for direct requests to Keycloak
    INTERNAL(NewCookie.SameSite.STRICT, true),

    // Internal cookies that are also available from JavaScript
    INTERNAL_JS(NewCookie.SameSite.STRICT, false),

    // Federation cookies are available after redirect from applications, and are also available in an iframe context
    // unless the browser blocks third-party cookies
    FEDERATION(NewCookie.SameSite.NONE, true),

    // Federation cookies that are also available from JavaScript
    FEDERATION_JS(NewCookie.SameSite.NONE, false),

    // Legacy cookies do not set the SameSite attribute and will default to SameSite=Lax in modern browsers
    @Deprecated
    LEGACY(null, true),

    // Legacy cookies that are also available from JavaScript
    @Deprecated
    LEGACY_JS(null, false);

    private final NewCookie.SameSite sameSite;
    private final boolean httpOnly;

    CookieScope(NewCookie.SameSite sameSite, boolean httpOnly) {
        this.sameSite = sameSite;
        this.httpOnly = httpOnly;
    }

    public NewCookie.SameSite getSameSite() {
        return sameSite;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }
}
