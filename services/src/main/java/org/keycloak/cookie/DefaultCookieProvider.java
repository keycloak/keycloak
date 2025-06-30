package org.keycloak.cookie;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakContext;

import java.util.Map;
public class DefaultCookieProvider implements CookieProvider {

    private static final Logger logger = Logger.getLogger(DefaultCookieProvider.class);

    private final KeycloakContext context;

    private CookiePathResolver pathResolver;

    private CookieSecureResolver secureResolver;

    private final Map<String, Cookie> cookies;

    private final boolean sameSiteLegacyEnabled;

    public DefaultCookieProvider(KeycloakContext context, boolean sameSiteLegacyEnabled) {
        this.context = context;
        this.cookies = context.getRequestHeaders().getCookies();
        this.pathResolver = new CookiePathResolver(context);
        this.secureResolver = new CookieSecureResolver(context, sameSiteLegacyEnabled);
        this.sameSiteLegacyEnabled = sameSiteLegacyEnabled;

        if (logger.isTraceEnabled()) {
            String cookieNames = String.join(", ", this.cookies.keySet());
            logger.tracef("Path: %s, cookies: %s", context.getUri().getRequestUri().getRawPath(), cookieNames);
        }
    }

    @Override
    public void set(CookieType cookieType, String value) {
        if (cookieType.getDefaultMaxAge() == null) {
            throw new IllegalArgumentException(cookieType + " has no default max-age");
        }

        set(cookieType, value, cookieType.getDefaultMaxAge());
    }

    @Override
    public void set(CookieType cookieType, String value, int maxAge) {
        String name = cookieType.getName();
        NewCookie.SameSite sameSite = cookieType.getScope().getSameSite();
        boolean secure = secureResolver.resolveSecure(sameSite);
        String path = pathResolver.resolvePath(cookieType);
        boolean httpOnly = cookieType.getScope().isHttpOnly();

        NewCookie newCookie = new NewCookie.Builder(name)
                .version(1)
                .value(value)
                .path(path)
                .maxAge(maxAge)
                .secure(secure)
                .httpOnly(httpOnly)
                .sameSite(sameSite)
                .build();

        context.getHttpResponse().setCookieIfAbsent(newCookie);

        logger.tracef("Setting cookie: name: %s, path: %s, same-site: %s, secure: %s, http-only: %s, max-age: %d", name, path, sameSite, secure, httpOnly, maxAge);

        setSameSiteLegacy(cookieType, value, maxAge);
    }

    private void setSameSiteLegacy(CookieType cookieType, String value, int maxAge) {
        if (sameSiteLegacyEnabled && cookieType.supportsSameSiteLegacy()) {
            String legacyName = cookieType.getSameSiteLegacyName();
            boolean legacySecure = secureResolver.resolveSecure(null);
            String path = pathResolver.resolvePath(cookieType);
            boolean httpOnly = cookieType.getScope().isHttpOnly();

            NewCookie legacyCookie = new NewCookie.Builder(legacyName)
                    .version(1)
                    .value(value)
                    .maxAge(maxAge)
                    .path(path)
                    .secure(legacySecure)
                    .httpOnly(httpOnly)
                    .build();
            context.getHttpResponse().setCookieIfAbsent(legacyCookie);

            logger.tracef("Setting legacy cookie: name: %s, path: %s, same-site: %s, secure: %s, http-only: %s, max-age: %d", legacyName, path, null, legacySecure, httpOnly, maxAge);
        } else if (cookieType.supportsSameSiteLegacy()) {
            expireSameSiteLegacy(cookieType);
        }
    }

    @Override
    public String get(CookieType cookieType) {
        Cookie cookie = cookies.get(cookieType.getName());
        if (cookie == null) {
            cookie = getSameSiteLegacyCookie(cookieType);
        }
        return cookie != null ? cookie.getValue() : null;
    }

    private Cookie getSameSiteLegacyCookie(CookieType cookieType) {
        if (cookieType.supportsSameSiteLegacy()) {
            return cookies.get(cookieType.getSameSiteLegacyName());
        } else {
            return null;
        }
    }

    @Override
    public void expire(CookieType cookieType) {
        expire(cookieType.getName(), cookieType);
        expireSameSiteLegacy(cookieType);
    }

    private void expireSameSiteLegacy(CookieType cookieType) {
        if (cookieType.supportsSameSiteLegacy()) {
            expire(cookieType.getSameSiteLegacyName(), cookieType);
        }
    }

    private void expire(String cookieName, CookieType cookieType) {
        Cookie cookie = cookies.get(cookieName);
        if (cookie != null) {
            String path = pathResolver.resolvePath(cookieType);
            NewCookie newCookie = new NewCookie.Builder(cookieName)
                    .version(1)
                    .path(path)
                    .maxAge(CookieMaxAge.EXPIRED)
                    .build();

            context.getHttpResponse().setCookieIfAbsent(newCookie);

            logger.tracef("Expiring cookie: name: %s, path: %s", cookie.getName(), path);
        }
    }

    @Override
    public void close() {
    }

}
