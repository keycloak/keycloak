package org.keycloak.services.resources.account;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;

import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.cookie.CookieProvider;
import org.keycloak.device.DeviceRepresentationProvider;
import org.keycloak.locale.DefaultLocaleSelectorProvider;
import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.locale.LocaleUpdaterProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.Theme;
import org.keycloak.theme.freemarker.FreeMarkerProvider;
import org.keycloak.urls.HostnameProvider;
import org.keycloak.urls.UrlType;

import static org.junit.Assert.assertEquals;

public class AccountConsoleLocaleTest {

    @Test
    public void accountConsoleRespectsKcLocaleParameter() throws IOException, FreeMarkerException {
        assertRenderedLocale("el", "kc_locale=el&scope=openid");
    }

    @Test
    public void accountConsoleRespectsRegionalKcLocaleParameter() throws IOException, FreeMarkerException {
        assertRenderedLocale("es-CO", "kc_locale=es-CO&scope=openid");
    }

    @Test
    public void accountConsoleFallsBackForUnsupportedKcLocaleParameter() throws IOException, FreeMarkerException {
        assertRenderedLocale("en", "kc_locale=fr&scope=openid");
    }

    @Test
    public void accountConsoleUsesDefaultLocaleWithoutKcLocaleParameter() throws IOException, FreeMarkerException {
        assertRenderedLocale("en", "scope=openid");
    }

    @Test
    public void accountConsolePreservesPreviouslySelectedLocaleWithoutKcLocaleParameter() throws IOException, FreeMarkerException {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(LocaleSelectorProvider.USER_REQUEST_LOCALE, "es-CO");

        assertRenderedLocale("es-CO", "scope=openid", attributes);
    }

    private void assertRenderedLocale(String expectedLocale, String query) throws IOException, FreeMarkerException {
        assertRenderedLocale(expectedLocale, query, new HashMap<>());
    }

    private void assertRenderedLocale(String expectedLocale, String query, Map<String, Object> attributes) throws IOException, FreeMarkerException {
        Profile.defaults();
        CapturingAccountConsole console = new CapturingAccountConsole(testSession(query, attributes));

        console.renderAccountConsole();

        assertEquals(expectedLocale, console.environment.get("locale"));
    }

    private static KeycloakSession testSession(String query, Map<String, Object> attributes) {
        RealmModel realm = realm();
        KeycloakSession[] session = new KeycloakSession[1];

        session[0] = proxy(KeycloakSession.class, invocation -> {
            String method = invocation.getMethod().getName();
            if (method.equals("getContext")) {
                return context(session[0], realm, query);
            }
            if (method.equals("getProvider")) {
                Class<?> providerClass = (Class<?>) invocation.getArguments()[0];
                if (providerClass.equals(LocaleSelectorProvider.class)) {
                    return new DefaultLocaleSelectorProvider(session[0]);
                }
                if (providerClass.equals(LocaleUpdaterProvider.class)) {
                    return proxy(LocaleUpdaterProvider.class, ignored -> null);
                }
                if (providerClass.equals(CookieProvider.class)) {
                    return proxy(CookieProvider.class, ignored -> null);
                }
                if (providerClass.equals(HostnameProvider.class)) {
                    return proxy(HostnameProvider.class, ignored -> URI.create("http://localhost/"));
                }
                if (providerClass.equals(DeviceRepresentationProvider.class)) {
                    return proxy(DeviceRepresentationProvider.class, ignored -> null);
                }
                return null;
            }
            if (method.equals("getAttribute")) {
                return attributes.get(invocation.getArguments()[0]);
            }
            if (method.equals("setAttribute")) {
                attributes.put((String) invocation.getArguments()[0], invocation.getArguments()[1]);
                return null;
            }
            if (method.equals("getAttributes")) {
                return attributes;
            }
            return defaultValue(invocation.getMethod().getReturnType());
        });

        return session[0];
    }

    private static KeycloakContext context(KeycloakSession session, RealmModel realm, String query) {
        KeycloakUriInfo uriInfo = new KeycloakUriInfo(session, UrlType.FRONTEND, uriInfo(query));

        return proxy(KeycloakContext.class, invocation -> {
            String method = invocation.getMethod().getName();
            if (method.equals("getRealm")) {
                return realm;
            }
            if (method.equals("getUri")) {
                return uriInfo;
            }
            if (method.equals("getRequestHeaders")) {
                return proxy(HttpHeaders.class, headers -> headers.getMethod().getName().equals("getAcceptableLanguages")
                        ? Collections.emptyList()
                        : defaultValue(headers.getMethod().getReturnType()));
            }
            if (method.equals("resolveLocale")) {
                UserModel user = (UserModel) invocation.getArguments()[0];
                return session.getProvider(LocaleSelectorProvider.class).resolveLocale(realm, user);
            }
            return defaultValue(invocation.getMethod().getReturnType());
        });
    }

    private static UriInfo uriInfo(String query) {
        URI requestUri = URI.create("http://localhost/realms/test/account/?" + query);
        URI baseUri = URI.create("http://localhost/");

        return proxy(UriInfo.class, invocation -> switch (invocation.getMethod().getName()) {
            case "getRequestUri" -> requestUri;
            case "getBaseUri" -> baseUri;
            case "getQueryParameters" -> {
                MultivaluedHashMap<String, String> parameters = new MultivaluedHashMap<>();
                for (String parameter : query.split("&")) {
                    String[] pair = parameter.split("=", 2);
                    parameters.add(pair[0], pair.length > 1 ? pair[1] : "");
                }
                yield parameters;
            }
            default -> defaultValue(invocation.getMethod().getReturnType());
        });
    }

    private static RealmModel realm() {
        return proxy(RealmModel.class, invocation -> switch (invocation.getMethod().getName()) {
            case "getName" -> "test";
            case "isInternationalizationEnabled" -> true;
            case "getDefaultLocale" -> "en";
            case "getSupportedLocalesStream" -> Stream.of("en", "el", "es-CO");
            case "getClientByClientId" -> proxy(ClientModel.class, ignored -> null);
            case "getAttribute" -> invocation.getArguments().length > 1 ? invocation.getArguments()[1] : null;
            default -> defaultValue(invocation.getMethod().getReturnType());
        });
    }

    private static Theme theme() {
        return proxy(Theme.class, invocation -> {
            String method = invocation.getMethod().getName();
            if (method.equals("getName")) {
                return "keycloak.v3";
            }
            if (method.equals("getEnhancedMessages")) {
                Properties messages = new Properties();
                messages.setProperty("locale_en", "English");
                messages.setProperty("locale_el", "Greek");
                messages.setProperty("locale_es-CO", "Spanish (Colombia)");
                return messages;
            }
            if (method.equals("getProperties")) {
                return new Properties();
            }
            return defaultValue(invocation.getMethod().getReturnType());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                (proxy, method, args) -> invocation.invoke(new InvocationContext(method, args == null ? new Object[0] : args)));
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type.equals(boolean.class)) {
            return false;
        }
        if (type.equals(void.class)) {
            return null;
        }
        return 0;
    }

    private record InvocationContext(java.lang.reflect.Method method, Object[] arguments) {
        java.lang.reflect.Method getMethod() {
            return method;
        }

        Object[] getArguments() {
            return arguments;
        }
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(InvocationContext invocation) throws Throwable;
    }

    private static class CapturingAccountConsole extends AccountConsole {

        private Map<String, Object> environment;

        CapturingAccountConsole(KeycloakSession session) {
            super(session, null, theme());
        }

        @Override
        public void init() {
        }

        @Override
        protected String renderAccountConsole(FreeMarkerProvider freeMarkerUtil, Map<String, Object> map) {
            environment = map;
            return "";
        }
    }
}
