package org.keycloak.quarkus.runtime.logging;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.keycloak.quarkus.runtime.configuration.Configuration;

import io.quarkus.vertx.http.runtime.attribute.AllRequestHeadersAttribute;
import io.quarkus.vertx.http.runtime.attribute.ExchangeAttribute;
import io.quarkus.vertx.http.runtime.attribute.ExchangeAttributeBuilder;
import io.quarkus.vertx.http.runtime.attribute.ReadOnlyAttributeException;
import io.smallrye.config.ConfigValue;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

// modified AllRequestHeadersAttribute class -> https://github.com/quarkusio/quarkus/blob/main/extensions/vertx-http/runtime/src/main/java/io/quarkus/vertx/http/runtime/attribute/AllRequestHeadersAttribute.java
// remove once current Quarkus version includes the https://github.com/quarkusio/quarkus/pull/50672
public class CustomAllRequestHeadersAttribute implements ExchangeAttribute {

    //---------------------Keycloak-changes-BEGIN----------------------------//
    private static Set<String> getMaskedCookies(){
        return Optional.ofNullable(Configuration.getConfigValue("quarkus.http.access-log.masked-cookies"))
                .map(ConfigValue::getValue)
                .map(f -> f.split(","))
                .map(f -> new HashSet<>(Arrays.stream(f).toList()))
                .orElseGet(HashSet::new);
    }
    //---------------------Keycloak-changes-END----------------------------//

    private static final String AUTHORIZATION_HEADER = String.valueOf(HttpHeaders.AUTHORIZATION).toLowerCase();
    private static final String COOKIE_HEADER = String.valueOf(HttpHeaders.COOKIE).toLowerCase();

    private final Set<String> maskedHeaders;
    private final Set<String> maskedCookies;

    CustomAllRequestHeadersAttribute() {
        this(Set.of(), Set.of());
    }

    CustomAllRequestHeadersAttribute(Set<String> maskedHeaders, Set<String> maskedCookies) {
        this.maskedHeaders = toLowerCaseStringSet(maskedHeaders);
        this.maskedCookies = toLowerCaseStringSet(maskedCookies);
    }

    private static Set<String> toLowerCaseStringSet(Set<String> set) {
        return set.stream().map(String::toLowerCase).collect(Collectors.toSet());
    }

    @Override
    public String readAttribute(RoutingContext exchange) {
        return readAttribute(exchange.request().headers());
    }

    String readAttribute(MultiMap headers) {
        if (headers.isEmpty()) {
            return null;
        } else {
            final StringJoiner joiner = new StringJoiner(System.lineSeparator());

            for (Map.Entry<String, String> header : headers) {
                joiner.add(header.getKey() + ": " + maskHeaderValue(header.getKey(), header.getValue()));
            }

            return joiner.toString();
        }
    }

    String maskHeaderValue(String headerName, String headerValue) {
        if (headerValue == null) {
            return null;
        }

        String headerNameLowerCase = headerName.toLowerCase();

        if (AUTHORIZATION_HEADER.equals(headerNameLowerCase)) {
            return maskAuthorizationHeaderValue(headerValue);
        }

        if (COOKIE_HEADER.equals(headerNameLowerCase)) {
            return maskCookieHeaderValue(headerValue);
        }

        if (maskedHeaders.contains(headerNameLowerCase)) {
            return "...";
        }

        return headerValue;
    }

    private String maskAuthorizationHeaderValue(String headerValue) {
        int idx = headerValue.indexOf(' ');
        final String scheme = idx > 0 ? headerValue.substring(0, idx) : null;

        if (scheme != null) {
            return scheme + " ...";
        } else {
            return "...";
        }
    }

    private String maskCookieHeaderValue(String headerValue) {
        int idx = headerValue.indexOf('=');

        final String cookieName = idx > 0 ? headerValue.substring(0, idx) : null;

        if (cookieName != null && maskedCookies.contains(cookieName.toLowerCase())) {
            return cookieName + "=...";
        }

        return headerValue;
    }

    @Override
    public void writeAttribute(RoutingContext exchange, String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("Headers", newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Headers";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals("%{ALL_REQUEST_HEADERS}")) {
                //---------------------Keycloak-changes-BEGIN----------------------------//
                return new CustomAllRequestHeadersAttribute(Set.of(), getMaskedCookies());
                //---------------------Keycloak-changes-END----------------------------//
            }
            return null;
        }

        @Override
        public int priority() {
            //---------------------Keycloak-changes-BEGIN----------------------------//
            // increase the priority
            return new AllRequestHeadersAttribute.Builder().priority() + 1;
            //---------------------Keycloak-changes-END----------------------------//
        }

    }

}
