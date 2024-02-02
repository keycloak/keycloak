package org.keycloak.cookie;

import org.keycloak.common.util.ServerCookie;

public enum CookieScope {
    // Internal cookies are only available for direct requests to Keycloak
    INTERNAL(ServerCookie.SameSiteAttributeValue.STRICT, true),

    // Federation cookies are available after redirect from applications, and are also available in an iframe context
    // unless the browser blocks third-party cookies
    FEDERATION(ServerCookie.SameSiteAttributeValue.NONE, true),

    // Federation cookies that are also available from JavaScript
    FEDERATION_JS(ServerCookie.SameSiteAttributeValue.NONE, false),

    // Legacy cookies do not set the SameSite attribute and will default to SameSite=Lax in modern browsers
    @Deprecated
    LEGACY(null, true),

    // Legacy cookies that are also available from JavaScript
    @Deprecated
    LEGACY_JS(null, false);

    private final ServerCookie.SameSiteAttributeValue sameSite;
    private final boolean httpOnly;

    CookieScope(ServerCookie.SameSiteAttributeValue sameSite, boolean httpOnly) {
        this.sameSite = sameSite;
        this.httpOnly = httpOnly;
    }

    public ServerCookie.SameSiteAttributeValue getSameSite() {
        return sameSite;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }
}
