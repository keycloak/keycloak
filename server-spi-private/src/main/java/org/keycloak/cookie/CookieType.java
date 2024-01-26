package org.keycloak.cookie;

public enum CookieType {

    KEYCLOAK_LOCALE(CookiePath.REALM, CookieScope.INTERNAL, CookieMaxAge.SESSION),
    WELCOME_STATE_CHECKER(CookiePath.REQUEST, CookieScope.INTERNAL, 300),
    KC_AUTH_STATE(CookiePath.REALM, CookieScope.LEGACY_JS), // TODO Change CookieScope
    KC_RESTART(CookiePath.REALM, CookieScope.LEGACY, CookieMaxAge.SESSION); // TODO Change CookieScope

    private final CookiePath path;
    private final CookieScope scope;

    private final Integer defaultMaxAge;

    CookieType(CookiePath path, CookieScope scope) {
        this.path = path;
        this.scope = scope;
        this.defaultMaxAge = null;
    }

    CookieType(CookiePath path, CookieScope scope, int defaultMaxAge) {
        this.path = path;
        this.scope = scope;
        this.defaultMaxAge = defaultMaxAge;
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
