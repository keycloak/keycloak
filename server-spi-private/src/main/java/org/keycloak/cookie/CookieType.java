package org.keycloak.cookie;

import jakarta.annotation.Nullable;

/**
 * Central registry of the cookies managed by Keycloak together with their default
 * path, scope, and lifetime settings.
 */
public final class CookieType {

    /**
     * Legacy cookie names from older releases that are no longer used and are
     * proactively expired when they are still present in the browser.
     */
    public static final CookieType[] OLD_UNUSED_COOKIES = new CookieType[] {
            CookieType.create("AUTH_SESSION_ID_LEGACY").build(),
            CookieType.create("KEYCLOAK_IDENTITY_LEGACY").build(),
            CookieType.create("KEYCLOAK_SESSION_LEGACY").build()
    };

    /**
     * Stores state for detached info and error pages so Keycloak can verify the
     * request when the page is refreshed or the locale is changed.
     */
    public static final CookieType AUTH_DETACHED = CookieType.create("KC_STATE_CHECKER")
            .scope(CookieScope.INTERNAL)
            .build();

    /**
     * Stores the data needed to restart an interrupted browser login flow after
     * the original authentication session was lost or expired.
     */
    public static final CookieType AUTH_RESTART = CookieType.create("KC_RESTART")
            .scope(CookieScope.FEDERATION)
            .defaultMaxAge(CookieMaxAge.SESSION)
            .build();

    /**
     * Short-lived hash of the current root authentication session that login
     * pages read from JavaScript to detect auth-session changes in another tab.
     */
    public static final CookieType AUTH_SESSION_ID_HASH = CookieType.create("KC_AUTH_SESSION_HASH")
            .scope(CookieScope.FEDERATION_JS)
            .defaultMaxAge(60)
            .build();

    /**
     * Identifies the current root authentication session during browser login
     * flows. The stored value is signed and may also contain route information in
     * clustered deployments.
     */
    public static final CookieType AUTH_SESSION_ID = CookieType.create("AUTH_SESSION_ID")
            .scope(CookieScope.FEDERATION)
            .defaultMaxAge(CookieMaxAge.SESSION)
            .build();

    /**
     * Holds the signed identity token for the logged-in browser session so
     * Keycloak can authenticate the user from the cookie on subsequent requests.
     */
    public static final CookieType IDENTITY = CookieType.create("KEYCLOAK_IDENTITY")
            .scope(CookieScope.FEDERATION)
            .build();

    /**
     * Remembers the locale selected on Keycloak-managed pages so later requests
     * can render the UI in the same language.
     */
    public static final CookieType LOCALE = CookieType.create("KEYCLOAK_LOCALE")
            .scope(CookieScope.FEDERATION)
            .defaultMaxAge(CookieMaxAge.SESSION)
            .build();

    /**
     * Stores the remembered username used to prefill the login form when the
     * realm allows the Remember Me feature.
     */
    public static final CookieType LOGIN_HINT = CookieType.create("KEYCLOAK_REMEMBER_ME")
            .scope(CookieScope.FEDERATION)
            .defaultMaxAge(CookieMaxAge.YEAR)
            .build();

    /**
     * JavaScript-readable marker for the current user session. It is used by
     * session-status checks and cross-tab login detection without exposing the
     * raw user session id.
     */
    public static final CookieType SESSION = CookieType.create("KEYCLOAK_SESSION")
            .scope(CookieScope.FEDERATION_JS)
            .build();

    /**
     * CSRF protection cookie for the welcome page bootstrap form, including local
     * admin creation.
     */
    public static final CookieType WELCOME_CSRF = CookieType.create("WELCOME_STATE_CHECKER")
            .requestPath()
            .defaultMaxAge(300)
            .build();

    private final String name;
    private final CookiePath path;
    private final CookieScope scope;

    private final Integer defaultMaxAge;

    private CookieType(String name, CookiePath path, CookieScope scope, @Nullable Integer defaultMaxAge) {
        this.name = name;
        this.path = path;
        this.scope = scope;
        this.defaultMaxAge = defaultMaxAge;
    }

    private static CookieTypeBuilder create(String name) {
        return new CookieTypeBuilder(name);
    }

    public String getName() {
        return name;
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

    private static class CookieTypeBuilder {

        private String name;
        private CookiePath path = CookiePath.REALM;
        private CookieScope scope = CookieScope.INTERNAL;
        private Integer defaultMaxAge;

        CookieTypeBuilder(String name) {
            this.name = name;
        }

        CookieTypeBuilder requestPath() {
            this.path = CookiePath.REQUEST;
            return this;
        }

        CookieTypeBuilder scope(CookieScope scope) {
            this.scope = scope;
            return this;
        }

        CookieTypeBuilder defaultMaxAge(int defaultMaxAge) {
            this.defaultMaxAge = defaultMaxAge;
            return this;
        }

        CookieType build() {
            return new CookieType(name, path, scope, defaultMaxAge);
        }

    }

}
