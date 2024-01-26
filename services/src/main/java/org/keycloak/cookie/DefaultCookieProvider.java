package org.keycloak.cookie;

import jakarta.ws.rs.core.Cookie;
import org.keycloak.common.util.ServerCookie;
import org.keycloak.http.HttpCookie;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.urls.UrlType;

import java.net.URI;

public class DefaultCookieProvider implements CookieProvider {

    private static final String LEGACY_SUFFIX = "_LEGACY";

    private KeycloakSession session;

    private boolean legacyCookiesEnabled;

    public DefaultCookieProvider(KeycloakSession session, boolean legacyCookiesEnabled) {
        this.session = session;
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
        String name = cookieType.name();
        ServerCookie.SameSiteAttributeValue sameSite = cookieType.getScope().getSameSite();
        boolean secure = resolveSecure(sameSite);
        String path = resolvePath(cookieType);
        boolean httpOnly = cookieType.getScope().isHttpOnly();

        HttpCookie newCookie = new HttpCookie(1, name, value, path, null, null, maxAge, secure, httpOnly, sameSite);
        session.getContext().getHttpResponse().setCookieIfAbsent(newCookie);

        if (legacyCookiesEnabled) {
            if (ServerCookie.SameSiteAttributeValue.NONE.equals(sameSite)) {
                String legacyName = name + LEGACY_SUFFIX;
                HttpCookie legacyCookie = new HttpCookie(1, legacyName, value, path, null, null, maxAge, secure, httpOnly, null);
                session.getContext().getHttpResponse().setCookieIfAbsent(legacyCookie);
            }
        } else {
            expireLegacy(cookieType);
        }
    }

    @Override
    public String get(CookieType cookieType) {
        Cookie cookie = session.getContext().getRequestHeaders().getCookies().get(cookieType.name());
        return cookie != null ? cookie.getValue() : null;
    }

    @Override
    public void expire(CookieType cookieType) {
        Cookie cookie = session.getContext().getRequestHeaders().getCookies().get(cookieType.name());
        expire(cookie, cookieType);

        expireLegacy(cookieType);
    }

    private void expireLegacy(CookieType cookieType) {
        String legacyName = cookieType.name() + LEGACY_SUFFIX;
        Cookie legacyCookie = session.getContext().getRequestHeaders().getCookies().get(legacyName);
        expire(legacyCookie, cookieType);
    }

    private void expire(Cookie cookie, CookieType cookieType) {
        if (cookie != null) {
            String path = resolvePath(cookieType);
            HttpCookie newCookie = new HttpCookie(1, cookie.getName(), "", path, null, null, 0, false, false, null);
            session.getContext().getHttpResponse().setCookieIfAbsent(newCookie);
        }
    }

    @Override
    public void close() {
    }

    private String resolvePath(CookieType cookieType) {
        switch (cookieType.getPath()) {
            case REALM:
                return RealmsResource.realmBaseUrl(session.getContext().getUri()).path("/").build(session.getContext().getRealm().getName()).getRawPath();
            case REQUEST:
                return session.getContext().getUri().getRequestUri().getRawPath();
            default:
                throw new IllegalArgumentException("Unsupported enum value " + cookieType.getPath().name());
        }
    }

    private boolean resolveSecure(ServerCookie.SameSiteAttributeValue sameSite) {
        URI requestUri = session.getContext().getUri().getRequestUri();

        // SameSite=none requires secure context
        if (ServerCookie.SameSiteAttributeValue.NONE.equals(sameSite)) {
            return true;
        }

        RealmModel realm = session.getContext().getRealm();
        if (realm != null && realm.getSslRequired().isRequired(requestUri.getHost())) {
            return true;
        }

        if ("https".equals(requestUri.getScheme())) {
            return true;
        }

        // Browsers consider 127.0.0.1, localhost and *.localhost as secure contexts
        String frontendHostname = session.getContext().getUri(UrlType.FRONTEND).getRequestUri().getHost();
        if (frontendHostname.equals("127.0.0.1") || frontendHostname.equals("localhost") || frontendHostname.endsWith(".localhost")) {
            return true;
        }

        return false;
    }

}
