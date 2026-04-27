package org.keycloak.cookie;

import java.util.Map;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.SecureContextResolver;

import org.jboss.logging.Logger;

public class DefaultCookieProvider implements CookieProvider {

    private static final Logger logger = Logger.getLogger(DefaultCookieProvider.class);

    private final KeycloakSession session;

    private final CookiePathResolver pathResolver;

    private final boolean secure;
    private boolean warned;

    private final Map<String, Cookie> cookies;

    public DefaultCookieProvider(KeycloakSession session) {
        KeycloakContext context = session.getContext();

        this.session = session;
        this.cookies = context.getRequestHeaders().getCookies();
        this.pathResolver = new CookiePathResolver(context);
        this.secure = SecureContextResolver.isSecureContext(session);

        if (logger.isTraceEnabled()) {
            logger.tracef("Received cookies: %s, path: %s", String.join(", ", this.cookies.keySet()), context.getUri().getRequestUri().getRawPath());
        }

        expireOldUnusedCookies();
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
        if (NewCookie.SameSite.NONE.equals(sameSite) && !secure) {
            sameSite = NewCookie.SameSite.LAX;
        }

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

        session.getContext().getHttpResponse().setCookieIfAbsent(newCookie);

        logger.tracef("Setting cookie: name: %s, path: %s, same-site: %s, secure: %s, http-only: %s, max-age: %d", name, path, sameSite, secure, httpOnly, maxAge);

        if (!secure && !warned) {
            warned = true;

            StringBuilder warning = new StringBuilder("Non-secure context detected; cookies are not secured, and will not be available in cross-origin POST requests.");

            String forwarded = session.getContext().getRequestHeaders().getHeaderString("Forwarded");
            String xForwarded = session.getContext().getRequestHeaders().getHeaderString("X-Forwarded-Proto");

            // if we are getting here then:
            //   if passthrough or reencrypt, proxy-headers may be misconfigured
            //   if edge, then proxy-headers should be set (there's hostname validation for this) but they are not set by the proxy server
            //   not proxied, and not https (there's hostname validation for this)

            if (forwarded != null || xForwarded != null) {
                if (session.getContext().getHttpRequest().isProxyTrusted()) {
                    warning.append(" Please review your proxy settings as the request appears to have originated from a proxy.");
                } else {
                    warning.append(" This is likely due to the proxy not being trusted.");
                }
            } else {
                warning.append(" Please review whether this direct HTTP usage is expected.");
            }

            logger.warnf(warning.toString());
        }
    }

    @Override
    public String get(CookieType cookieType) {
        Cookie cookie = cookies.get(cookieType.getName());
        return cookie != null ? cookie.getValue() : null;
    }

    @Override
    public void expire(CookieType cookieType) {
        String cookieName = cookieType.getName();
        Cookie cookie = cookies.get(cookieName);
        if (cookie != null) {
            String path = pathResolver.resolvePath(cookieType);
            NewCookie newCookie = new NewCookie.Builder(cookieName)
                    .version(1)
                    .path(path)
                    .maxAge(CookieMaxAge.EXPIRED)
                    .build();

            session.getContext().getHttpResponse().setCookieIfAbsent(newCookie);

            logger.tracef("Expiring cookie: name: %s, path: %s", cookie.getName(), path);
        }
    }

    private void expireOldUnusedCookies() {
        for (CookieType cookieType : CookieType.OLD_UNUSED_COOKIES) {
            expire(cookieType);
        }
    }

    @Override
    public void close() {
    }

}
