/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.tests.conformance.vp;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLContext;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.UriUtils;
import org.keycloak.protocol.oidc.utils.PkceUtils;
import org.keycloak.tests.conformance.containers.OpenIdConformanceSuite;
import org.keycloak.tests.conformance.runner.ModuleRun;

/**
 * Plays the user's browser in the same device flow. It starts a Keycloak login, extracts the wallet
 * link parameters from the OID4VP login page, hands them to the conformance suite's wallet, and
 * follows redirects back to Keycloak over one cookie session.
 */
final class KeycloakVerifierBrowser {

    // Matches the wallet link by its scheme so it does not depend on the login page markup.
    private static final Pattern WALLET_LINK = Pattern.compile("openid4vp://[^\"'\\s]+");

    private final OpenIdConformanceSuite suite;
    private final String keycloakLocalBaseUrl;
    private final HttpClient httpClient;

    KeycloakVerifierBrowser(OpenIdConformanceSuite suite, String keycloakLocalBaseUrl, SSLContext sslContext) {
        this.suite = suite;
        this.keycloakLocalBaseUrl = keycloakLocalBaseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .sslContext(sslContext)
                .build();
    }

    void login(String realm, String clientId, String idpAlias, ModuleRun moduleRun) {
        WalletLink walletLink = fetchWalletLink(realm, clientId, idpAlias);
        deliverToWallet(moduleRun, walletLink);
    }

    private record WalletLink(String clientId, String requestUri) {
    }

    private WalletLink fetchWalletLink(String realm, String clientId, String idpAlias) {
        String codeVerifier = PkceUtils.generateCodeVerifier();
        String loginUrl = keycloakLocalBaseUrl + "/realms/" + realm + "/protocol/openid-connect/auth"
                + "?client_id=" + urlEncode(clientId)
                + "&response_type=code"
                + "&scope=openid"
                + "&redirect_uri=" + urlEncode(OpenIdConformanceSuite.KEYCLOAK_BASE_URI + "/callback")
                + "&code_challenge=" + urlEncode(PkceUtils.generateS256CodeChallenge(codeVerifier))
                + "&code_challenge_method=S256"
                + "&kc_idp_hint=" + urlEncode(idpAlias);

        HttpResponse<String> response = getFollowingRedirects(URI.create(loginUrl));
        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Keycloak login page returned HTTP " + response.statusCode() + ": " + response.body());
        }
        Matcher matcher = WALLET_LINK.matcher(response.body());
        if (!matcher.find()) {
            throw new IllegalStateException("Keycloak login page did not contain the same device wallet link");
        }
        String walletUrl = matcher.group().replace("&amp;", "&");
        String query = walletUrl.contains("?") ? walletUrl.substring(walletUrl.indexOf('?') + 1) : "";
        MultivaluedHashMap<String, String> parameters = UriUtils.parseQueryParameters(query, true);
        return new WalletLink(required(parameters, "client_id", walletUrl), required(parameters, "request_uri", walletUrl));
    }

    private void deliverToWallet(ModuleRun moduleRun, WalletLink walletLink) {
        URI authorizationEndpoint = suite.externalUri(moduleRun.authorizationEndpoint());
        String url = authorizationEndpoint
                + (authorizationEndpoint.getQuery() != null ? "&" : "?")
                + "client_id=" + urlEncode(walletLink.clientId())
                + "&request_uri=" + urlEncode(walletLink.requestUri());
        getFollowingRedirects(URI.create(url));
    }

    private HttpResponse<String> get(URI uri) {
        try {
            return httpClient.send(
                    HttpRequest.newBuilder(uri)
                            .timeout(Duration.ofMinutes(1))
                            .header("Accept", "text/html,application/json,application/oauth-authz-req+jwt")
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new RuntimeException("Request failed: " + uri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while requesting " + uri, e);
        }
    }

    // Redirects are followed by hand rather than with HttpClient.followRedirects, because Keycloak
    // and the suite emit Location headers with their container network hostnames
    // (host.testcontainers.internal, nginx) that the test JVM cannot reach. Each hop is rewritten to
    // a host-reachable URL.
    private HttpResponse<String> getFollowingRedirects(URI uri) {
        URI current = uri;
        for (int redirects = 0; redirects < 10; redirects++) {
            HttpResponse<String> response = get(current);
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Authorization flow returned HTTP " + response.statusCode()
                        + " for " + current + ": " + response.body());
            }
            if (response.statusCode() < 300) {
                return response;
            }
            URI next = current.resolve(response.headers().firstValue("Location")
                    .orElseThrow(() -> new IllegalStateException("Redirect without Location header")));
            // A redirect to the suite results page means the wallet interaction is complete. The
            // module verdict is read from the suite module status, not from the browser response.
            if (next.getPath() != null && next.getPath().contains("log-detail")) {
                return response;
            }
            current = rewriteToHostReachable(next);
        }
        throw new IllegalStateException("Too many redirects, last URL: " + current);
    }

    private URI rewriteToHostReachable(URI uri) {
        if (OpenIdConformanceSuite.KEYCLOAK_BASE_URI.getHost().equals(uri.getHost())) {
            return URI.create(keycloakLocalBaseUrl + uri.getRawPath()
                    + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : ""));
        }
        return suite.externalUri(uri);
    }

    private static String required(MultivaluedHashMap<String, String> parameters, String name, String url) {
        String value = parameters.getFirst(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Wallet link did not contain " + name + ": " + url);
        }
        return value;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
