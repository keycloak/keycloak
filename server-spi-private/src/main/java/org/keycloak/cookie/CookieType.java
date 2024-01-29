package org.keycloak.cookie;

import jakarta.annotation.Nullable;

public final class CookieType {

    public static final CookieType AUTH_DETACHED = new CookieType("KC_STATE_CHECKER", false, CookiePath.REALM, CookieScope.LEGACY, null);
    public static final CookieType AUTH_RESTART = new CookieType("KC_RESTART", false, CookiePath.REALM, CookieScope.LEGACY, CookieMaxAge.SESSION);
    public static final CookieType AUTH_SESSION_ID = new CookieType("AUTH_SESSION_ID", true, CookiePath.REALM, CookieScope.FEDERATION, CookieMaxAge.SESSION);
    public static final CookieType AUTH_STATE = new CookieType("KC_AUTH_STATE", false, CookiePath.REALM, CookieScope.LEGACY_JS, null);
    public static final CookieType IDENTITY = new CookieType("KEYCLOAK_IDENTITY", true, CookiePath.REALM, CookieScope.FEDERATION, null);
    public static final CookieType LOCALE = new CookieType("KEYCLOAK_LOCALE", false, CookiePath.REALM, CookieScope.LEGACY, CookieMaxAge.SESSION);
    public static final CookieType LOGIN_HINT = new CookieType("KEYCLOAK_REMEMBER_ME", false, CookiePath.REALM, CookieScope.LEGACY, CookieMaxAge.YEAR);
    public static final CookieType SESSION = new CookieType("KEYCLOAK_SESSION", true, CookiePath.REALM, CookieScope.FEDERATION_JS, null);
    public static final CookieType WELCOME_CSRF = new CookieType("WELCOME_STATE_CHECKER", false, CookiePath.REQUEST, CookieScope.INTERNAL, 300);

    private final String name;
    private final String sameSiteLegacyName;
    private final CookiePath path;
    private final CookieScope scope;

    private final Integer defaultMaxAge;

    private CookieType(String name, boolean supportsSameSiteLegacy, CookiePath path, CookieScope scope, @Nullable Integer defaultMaxAge) {
        this.name = name;
        this.sameSiteLegacyName = supportsSameSiteLegacy ? name + "_LEGACY" : null;
        this.path = path;
        this.scope = scope;
        this.defaultMaxAge = defaultMaxAge;
    }

    public String getName() {
        return name;
    }

    @Deprecated
    public boolean supportsSameSiteLegacy() {
        return sameSiteLegacyName != null;
    }

    @Deprecated
    public String getSameSiteLegacyName() {
        return sameSiteLegacyName;
    }

    public CookiePath getPath() {
        return path;
    }

    public CookieScope getScope() {
        return scope;
    }

    public Integer getDefaultMaxAge() {
        return defaultMaxAge;
    }

}
