package org.keycloak.cookie;

import jakarta.annotation.Nullable;

public final class CookieType {

    public static final CookieType[] OLD_UNUSED_COOKIES = new CookieType[] {
            CookieType.create("AUTH_SESSION_ID_LEGACY").build(),
            CookieType.create("KEYCLOAK_IDENTITY_LEGACY").build(),
            CookieType.create("KEYCLOAK_SESSION_LEGACY").build()
    };

     /**
     * KC_STATE_CHECKER is set on ERROR and INFO pages that terminate or detach the user session.
     * It is needed when the user switches the language on these pages.
     * Since the necessary information to render the page can no longer be retrieved from the session,
     * this information is taken from the KC_STATE_CHECKER cookie.
     */
    public static final CookieType AUTH_DETACHED = CookieType.create("KC_STATE_CHECKER")
            .scope(CookieScope.INTERNAL)
            .build();

    /**
     * KC_RESTART is a cookie created at the beginning of the authentication flow.
     * It contains client information encoded in a signed JWT token.
     * When the root authentication session expires, this cookie can be used to create
     * a new authentication session using the client information stored in the cookie.
     * This is useful for handling client-side timeouts during login.
     */
    public static final CookieType AUTH_RESTART = CookieType.create("KC_RESTART")
            .scope(CookieScope.FEDERATION)
            .defaultMaxAge(CookieMaxAge.SESSION)
            .build();

    /**
     * KC_AUTH_SESSION_HASH contains the hash of the root authentication session ID.
     * It is used to refresh the login page when the root authentication session changes.
     * After a 1-second timeout, the login page is reloaded if the current value of the cookie
     * differs from its initial value. This can happen when multiple login pages are
     * open simultaneously in the same browser.
     */
    public static final CookieType AUTH_SESSION_ID_HASH = CookieType.create("KC_AUTH_SESSION_HASH")
            .scope(CookieScope.INTERNAL_JS)
            .defaultMaxAge(60)
            .build();

    /**
     * AUTH_SESSION_ID stores the root authentication session ID, which is used across multiple
     * browser tabs to identify the root authentication session.
     */
    public static final CookieType AUTH_SESSION_ID = CookieType.create("AUTH_SESSION_ID")
            .scope(CookieScope.FEDERATION)
            .defaultMaxAge(CookieMaxAge.SESSION)
            .build();

    /**
     * Creates a signed identity token containing user ID, session ID, issuer, and other details.
     * It is used to authenticate the user.
     */
    public static final CookieType IDENTITY = CookieType.create("KEYCLOAK_IDENTITY")
            .scope(CookieScope.FEDERATION)
            .build();

    /**
     * KEYCLOAK_LOCALE contains user- or session-specific overrides of the realm's default locale.
     */
    public static final CookieType LOCALE = CookieType.create("KEYCLOAK_LOCALE")
            .scope(CookieScope.FEDERATION)
            .defaultMaxAge(CookieMaxAge.SESSION)
            .build();

    /**
     * KEYCLOAK_REMEMBER_ME stores the username of the user from the current session.
     * It is set when the "remember me" functionality is enabled.
     * If the authentication session expires, the username in this cookie is used to prefill the login form.
     */
    public static final CookieType LOGIN_HINT = CookieType.create("KEYCLOAK_REMEMBER_ME")
            .scope(CookieScope.FEDERATION)
            .defaultMaxAge(CookieMaxAge.YEAR)
            .build();

    /**
     * KEYCLOAK_SESSION contains the hashed session ID.
     * It is used for single sign-on and session status checks in the session status iframe.
     */
    public static final CookieType SESSION = CookieType.create("KEYCLOAK_SESSION")
            .scope(CookieScope.FEDERATION_JS)
            .build();

    /**
     * WELCOME_STATE_CHECKER is used for CSRF protection on the welcome page.
     * This page is used to create the first administrative user the very first time Keycloak is started.
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
