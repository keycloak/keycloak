package org.keycloak.tests.providers.rest;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import org.keycloak.cookie.CookieProvider;
import org.keycloak.cookie.CookieType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class CookieTestingRealmResourceProvider implements RealmResourceProvider {

    private static final Map<String, CookieType> COOKIE_TYPES = Map.of(
            CookieType.AUTH_SESSION_ID.getName(), CookieType.AUTH_SESSION_ID,
            CookieType.AUTH_SESSION_ID_HASH.getName(), CookieType.AUTH_SESSION_ID_HASH,
            CookieType.AUTH_RESTART.getName(), CookieType.AUTH_RESTART,
            CookieType.AUTH_DETACHED.getName(), CookieType.AUTH_DETACHED,
            CookieType.IDENTITY.getName(), CookieType.IDENTITY,
            CookieType.LOCALE.getName(), CookieType.LOCALE,
            CookieType.LOGIN_HINT.getName(), CookieType.LOGIN_HINT,
            CookieType.SESSION.getName(), CookieType.SESSION,
            CookieType.WELCOME_CSRF.getName(), CookieType.WELCOME_CSRF
    );

    private final KeycloakSession session;

    CookieTestingRealmResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return this;
    }

    @POST
    @Path("set-all")
    public void setAll() {
        CookieProvider cookies = session.getProvider(CookieProvider.class);
        cookies.set(CookieType.AUTH_SESSION_ID, "my-auth-session-id");
        cookies.set(CookieType.AUTH_SESSION_ID_HASH, "my-kc-auth-session");
        cookies.set(CookieType.AUTH_RESTART, "my-auth-restart");
        cookies.set(CookieType.AUTH_DETACHED, "my-auth-detached", 222);
        cookies.set(CookieType.IDENTITY, "my-identity", 333);
        cookies.set(CookieType.LOCALE, "my-locale");
        cookies.set(CookieType.LOGIN_HINT, "my-username");
        cookies.set(CookieType.SESSION, "my-session", 444);
        cookies.set(CookieType.WELCOME_CSRF, "my-welcome-csrf");
    }

    @POST
    @Path("set-session")
    public void setSession(@QueryParam("value") String value, @QueryParam("maxAge") int maxAge) {
        session.getProvider(CookieProvider.class).set(CookieType.SESSION, value, maxAge);
    }

    @POST
    @Path("expire")
    public void expire(@QueryParam("type") List<String> typeNames) {
        CookieProvider cookies = session.getProvider(CookieProvider.class);
        for (String typeName : typeNames) {
            cookies.expire(resolve(typeName));
        }
    }

    @GET
    @Path("get")
    @Produces(MediaType.TEXT_PLAIN)
    public Response get(@QueryParam("type") String typeName) {
        CookieType type = resolve(typeName);
        String value = session.getProvider(CookieProvider.class).get(type);
        if (value == null) {
            return Response.noContent().build();
        }
        return Response.ok(value).build();
    }

    @POST
    @Path("set-custom")
    public void setCustom(@QueryParam("name") String name, @QueryParam("value") String value,
            @QueryParam("maxAge") int maxAge) {
        NewCookie newCookie = new NewCookie.Builder(name)
                .maxAge(maxAge)
                .value(value)
                .path(session.getContext().getUri().getRequestUri().getRawPath())
                .build();
        session.getContext().getHttpResponse().setCookieIfAbsent(newCookie);
    }

    private static CookieType resolve(String name) {
        CookieType type = COOKIE_TYPES.get(name);
        if (type == null) {
            throw new IllegalArgumentException("Unknown cookie type: " + name);
        }
        return type;
    }

    @Override
    public void close() {
    }
}
