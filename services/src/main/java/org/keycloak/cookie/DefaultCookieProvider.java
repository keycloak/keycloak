package org.keycloak.cookie;

import jakarta.ws.rs.core.Cookie;
import org.jboss.logging.Logger;
import org.keycloak.common.util.ServerCookie;
import org.keycloak.http.HttpCookie;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.urls.UrlType;

import java.net.URI;
import java.util.Map;

public class DefaultCookieProvider implements CookieProvider {

    private static final Logger logger = Logger.getLogger(DefaultCookieProvider.class);

    private final KeycloakContext context;

    private final Map<String, Cookie> cookies;

    private final boolean legacyCookiesEnabled;

    public DefaultCookieProvider(KeycloakContext context, boolean legacyCookiesEnabled) {
        this.context = context;
        this.cookies = context.getRequestHeaders().getCookies();
        this.legacyCookiesEnabled = legacyCookiesEnabled;
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
        ServerCookie.SameSiteAttributeValue sameSite = cookieType.getScope().getSameSite();
        boolean secure = resolveSecure(sameSite);
        String path = resolvePath(cookieType);
        boolean httpOnly = cookieType.getScope().isHttpOnly();

        HttpCookie newCookie = new HttpCookie(1, name, value, path, null, null, maxAge, secure, httpOnly, sameSite);
        context.getHttpResponse().setCookieIfAbsent(newCookie);

        logger.tracef("Setting cookie: name: %s, path: %s, same-site: %s, secure: %s, http-only: %s, max-age: %d", name, path, sameSite, secure, httpOnly, maxAge);

        if (legacyCookiesEnabled && cookieType.supportsSameSiteLegacy()) {
            if (ServerCookie.SameSiteAttributeValue.NONE.equals(sameSite)) {
                secure = resolveSecure(null);
                String legacyName = cookieType.getSameSiteLegacyName();
                HttpCookie legacyCookie = new HttpCookie(1, legacyName, value, path, null, null, maxAge, secure, httpOnly, null);
                context.getHttpResponse().setCookieIfAbsent(legacyCookie);

                logger.tracef("Setting legacy cookie: name: %s, path: %s, same-site: %s, secure: %s, http-only: %s, max-age: %d", legacyName, path, sameSite, secure, httpOnly, maxAge);
            }
        } else {
            expireLegacy(cookieType);
        }
    }

    @Override
    public String get(CookieType cookieType) {
        Cookie cookie = cookies.get(cookieType.getName());
        if (cookie == null && cookieType.supportsSameSiteLegacy()) {
            cookie = cookies.get(cookieType.getSameSiteLegacyName());
        }
        return cookie != null ? cookie.getValue() : null;
    }

    @Override
    public void expire(CookieType cookieType) {
        Cookie cookie = cookies.get(cookieType.getName());
        expire(cookie, cookieType);

        expireLegacy(cookieType);
    }

    private void expireLegacy(CookieType cookieType) {
        if (cookieType.supportsSameSiteLegacy()) {
            String legacyName = cookieType.getSameSiteLegacyName();
            Cookie legacyCookie = cookies.get(legacyName);
            expire(legacyCookie, cookieType);
        }
    }

    private void expire(Cookie cookie, CookieType cookieType) {
        if (cookie != null) {
            String path = resolvePath(cookieType);
            HttpCookie newCookie = new HttpCookie(1, cookie.getName(), "", path, null, null, CookieMaxAge.EXPIRED, false, false, null);
            context.getHttpResponse().setCookieIfAbsent(newCookie);

            logger.tracef("Expiring cookie: name: %s, path: %s", cookie.getName(), path);
        }
    }

    @Override
    public void close() {
    }

    private String resolvePath(CookieType cookieType) {
        switch (cookieType.getPath()) {
            case REALM:
                return RealmsResource.realmBaseUrl(context.getUri()).path("/").build(context.getRealm().getName()).getRawPath();
            case REQUEST:
                return context.getUri().getRequestUri().getRawPath();
            default:
                throw new IllegalArgumentException("Unsupported enum value " + cookieType.getPath().name());
        }
    }

    private boolean resolveSecure(ServerCookie.SameSiteAttributeValue sameSite) {
        URI requestUri = context.getUri().getRequestUri();

        // SameSite=none requires secure context
        if (ServerCookie.SameSiteAttributeValue.NONE.equals(sameSite)) {
            return true;
        }

        RealmModel realm = context.getRealm();
        if (realm != null && realm.getSslRequired().isRequired(requestUri.getHost())) {
            return true;
        }

        return false;
    }

}
