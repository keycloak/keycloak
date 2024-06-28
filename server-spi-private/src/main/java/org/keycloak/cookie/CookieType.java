package org.keycloak.cookie;

import jakarta.annotation.Nullable;

public final class CookieType {

    public static final CookieType AUTH_DETACHED = CookieType.create("KC_STATE_CHECKER")
            .scope(CookieScope.INTERNAL)
            .build();

    public static final CookieType AUTH_RESTART = CookieType.create("KC_RESTART")
            .scope(CookieScope.FEDERATION)
            .defaultMaxAge(CookieMaxAge.SESSION)
            .build();

    public static final CookieType AUTH_SESSION_ID = CookieType.create("AUTH_SESSION_ID")
            .scope(CookieScope.FEDERATION)
            .defaultMaxAge(CookieMaxAge.SESSION)
            .supportSameSiteLegacy()
            .build();

    public static final CookieType IDENTITY = CookieType.create("KEYCLOAK_IDENTITY")
            .scope(CookieScope.FEDERATION)
            .supportSameSiteLegacy()
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
            .supportSameSiteLegacy()
            .build();

    public static final CookieType WELCOME_CSRF = CookieType.create("WELCOME_STATE_CHECKER")
            .requestPath()
            .defaultMaxAge(300)
            .build();

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

    private static CookieTypeBuilder create(String name) {
        return new CookieTypeBuilder(name);
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

    private static class CookieTypeBuilder {

        private String name;
        private boolean supportSameSiteLegacy = false;
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

        CookieTypeBuilder supportSameSiteLegacy() {
            this.supportSameSiteLegacy = true;
            return this;
        }

        CookieTypeBuilder defaultMaxAge(int defaultMaxAge) {
            this.defaultMaxAge = defaultMaxAge;
            return this;
        }

        CookieType build() {
            return new CookieType(name, supportSameSiteLegacy, path, scope, defaultMaxAge);
        }

    }

}
