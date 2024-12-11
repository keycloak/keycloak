package org.keycloak.cookie;

import jakarta.annotation.Nullable;

public final class CookieType {

    public static final CookieType[] OLD_UNUSED_COOKIES = new CookieType[] {
            CookieType.create("AUTH_SESSION_ID_LEGACY").build(),
            CookieType.create("KEYCLOAK_IDENTITY_LEGACY").build(),
            CookieType.create("KEYCLOAK_SESSION_LEGACY").build()
    };

    /**
     * Set on ERROR and INFO pages that terminate or detach the user session.
     *
     * Needed when the user switches the language on these pages.
     * The necessary information to render the page can no longer be retrieved from the session.
     * Therefore, this information is stored in the KC_STATE_CHECKER cookie.
     */
    public static final CookieType AUTH_DETACHED = CookieType.create("KC_STATE_CHECKER")
            .scope(CookieScope.INTERNAL)
            .build();

    /**
     * KC_RESTART is the cookie, which is created at the beginning of the authentication flow.
     * It contains client information encoded in a signed JWT token.
     * When the root authentication session expires this cookie can be used to re-create
     * a new authentication session from the client informations supplied in the cookie.
     * This could happen on a client side timeout during login.
     */
    public static final CookieType AUTH_RESTART = CookieType.create("KC_RESTART")
            .scope(CookieScope.FEDERATION)
            .defaultMaxAge(CookieMaxAge.SESSION)
            .build();

    /**
     * KC_AUTH_SESSION_HASH Contains the hash of the root auth session id.
     * Used to refresh the login page when root auth session changes.
     * After a 1-second timeout the login page is reloaded if the current value of the cookie differs from the initial value.
     * This can happen when multiple login pages are opened simultaneously in the same browser.
     */
    public static final CookieType AUTH_SESSION_ID_HASH = CookieType.create("KC_AUTH_SESSION_HASH")
            .scope(CookieScope.INTERNAL_JS)
            .defaultMaxAge(60)
            .build();

    /**
     * Storing the root authentication session id that is used across multiple browser tabs,
     * to be able to identify the root authentication session
     */
    public static final CookieType AUTH_SESSION_ID = CookieType.create("AUTH_SESSION_ID")
            .scope(CookieScope.FEDERATION)
            .defaultMaxAge(CookieMaxAge.SESSION)
            .build();

    /**
     * Creates a signed identity token that contains user id, session id, issuer, etc.
     * It is used to authenticate a user.
     */
    public static final CookieType IDENTITY = CookieType.create("KEYCLOAK_IDENTITY")
            .scope(CookieScope.FEDERATION)
            .build();

    public static final CookieType LOCALE = CookieType.create("KEYCLOAK_LOCALE")
            .scope(CookieScope.FEDERATION)
            .defaultMaxAge(CookieMaxAge.SESSION)
            .build();

    public static final CookieType LOGIN_HINT = CookieType.create("KEYCLOAK_REMEMBER_ME")
            .scope(CookieScope.FEDERATION)
            .defaultMaxAge(CookieMaxAge.YEAR)
            .build();

    public static final CookieType SESSION = CookieType.create("KEYCLOAK_SESSION")
            .scope(CookieScope.FEDERATION_JS)
            .build();

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
