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
package org.keycloak.ssf.transmitter.support;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.keycloak.ssf.SsfException;

/**
 * Per-receiver SSRF gate for the {@code ssf.validPushUrls} client-attribute
 * allow-list. Validates the receiver-supplied {@code delivery.endpoint_url}
 * against the configured allow-list using the same exact-or-trailing-wildcard
 * match semantics that OIDC redirect URI validation uses, plus three SSRF-
 * specific tightenings:
 *
 * <ol>
 *     <li>Bare {@code "*"} entries are silently dropped from the effective
 *         allow-list — a single keystroke must not be able to disable the
 *         SSRF defence.</li>
 *     <li>Wildcard entries must contain a host portion that is itself wildcard-
 *         and query-free; this prevents constructions like {@code "https://*"}
 *         or {@code "https://*.example.com/*"} that would expand the allow-list
 *         beyond a host the operator explicitly reviewed.</li>
 *     <li>After a successful match, the receiver-supplied URL itself is
 *         re-validated: scheme must be {@code https} and the host must not
 *         resolve to a loopback / link-local / site-local / unique-local /
 *         multicast / any-local address. Both checks are bypassed when
 *         {@link #allowInsecure} is {@code true} (closed-network deployments
 *         and integration tests pushing to a local mock server).</li>
 * </ol>
 *
 * <p>The match logic in {@link #matchesAllowList(Set, String)} is a deliberate
 * copy of {@code RedirectUtils.matchesRedirects} — kept SSF-local so the
 * security surface of the OIDC redirect-URI matcher is not touched by this
 * gate. Behaviour MUST be kept consistent between the two; track changes in
 * {@code RedirectUtils} and mirror them here when warranted.
 */
public final class SsfPushUrlValidator {

    private final boolean allowInsecure;

    public SsfPushUrlValidator(boolean allowInsecure) {
        this.allowInsecure = allowInsecure;
    }

    /**
     * Validates the receiver-supplied push URL against the receiver client's
     * allow-list. Throws {@link SsfPushUrlValidationException} with a stable
     * machine-readable reason code when the URL must be rejected; returns
     * silently on a successful match.
     */
    public void validate(String pushUrl, Set<String> validPushUrls) {
        if (pushUrl == null || pushUrl.isBlank()) {
            throw new SsfPushUrlValidationException(Reason.URL_MISSING,
                    "delivery.endpoint_url is required for push delivery");
        }
        if (validPushUrls == null || validPushUrls.isEmpty()) {
            throw new SsfPushUrlValidationException(Reason.ALLOWLIST_EMPTY,
                    "delivery method 'push' requires the receiver client to declare ssf.validPushUrls");
        }
        Set<String> effective = filterUsableEntries(validPushUrls);
        if (effective.isEmpty()) {
            throw new SsfPushUrlValidationException(Reason.ALLOWLIST_EMPTY,
                    "delivery method 'push' requires at least one usable ssf.validPushUrls entry"
                            + " (bare '*' entries are not honoured)");
        }
        if (matchesAllowList(effective, pushUrl) == null) {
            throw new SsfPushUrlValidationException(Reason.NOT_IN_ALLOWLIST,
                    "delivery.endpoint_url is not in the receiver client's ssf.validPushUrls");
        }
        validateSchemeAndHost(pushUrl);
    }

    /**
     * Drops bare-{@code *} entries and structurally invalid wildcard entries
     * (host portion contains {@code *} or {@code ?}, missing scheme/host,
     * non-http(s) scheme). The check is conservative — anything we can't
     * confidently parse and approve is removed rather than rejected, on the
     * theory that a misconfigured entry shouldn't silently widen the gate.
     */
    Set<String> filterUsableEntries(Set<String> entries) {
        Set<String> usable = new LinkedHashSet<>();
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            if ("*".equals(entry)) {
                continue;
            }
            String stripped = entry.endsWith("*")
                    ? entry.substring(0, entry.length() - 1)
                    : entry;
            URI parsed;
            try {
                parsed = new URI(stripped);
            } catch (Exception e) {
                continue;
            }
            if (!parsed.isAbsolute()) {
                continue;
            }
            String scheme = parsed.getScheme();
            if (scheme == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                continue;
            }
            String host = parsed.getHost();
            if (host == null || host.isBlank()) {
                continue;
            }
            if (host.contains("*") || host.contains("?")) {
                continue;
            }
            usable.add(entry);
        }
        return usable;
    }

    /**
     * Copy of {@code RedirectUtils.matchesRedirects} with {@code allowWildcards=true}
     * hard-coded — SSF push URL allow-listing always supports trailing-{@code *}
     * wildcards. Returns the matched entry (post-{@code *} stripping for the
     * wildcard case), or {@code null} when no entry matches.
     *
     * <p>Keep behaviour aligned with the OIDC matcher; if that one is fixed
     * for an edge case, mirror the fix here.
     */
    static String matchesAllowList(Set<String> validUrls, String pushUrl) {
        for (String validUrl : validUrls) {
            // Bare-* is filtered out upstream by filterUsableEntries; no need
            // to special-case it here. Branch left commented to flag the
            // intentional divergence from RedirectUtils.matchesRedirects.
            // if ("*".equals(validUrl)) return validUrl;
            if (validUrl.endsWith("*") && !validUrl.contains("?")) {
                int idx = pushUrl.indexOf('?');
                if (idx == -1) {
                    idx = pushUrl.indexOf('#');
                }
                String r = idx == -1 ? pushUrl : pushUrl.substring(0, idx);
                int length = validUrl.length() - 1;
                String trimmed = validUrl.substring(0, length);
                if (r.startsWith(trimmed)) {
                    return trimmed;
                }
                if (length - 1 > 0 && trimmed.charAt(length - 1) == '/') {
                    length--;
                }
                trimmed = validUrl.substring(0, length);
                if (trimmed.equals(r)) {
                    return trimmed;
                }
            } else if (validUrl.equals(pushUrl)) {
                return validUrl;
            }
        }
        return null;
    }

    /**
     * Verifies the receiver-supplied URL's scheme and host class. With
     * {@link #allowInsecure} {@code false} the URL must be {@code https} and
     * the host must not resolve to a loopback / link-local / site-local /
     * unique-local / multicast / any-local address. Unresolvable hostnames
     * are accepted — a push to an unresolvable host fails harmlessly at
     * delivery time and is not an SSRF.
     *
     * <p>The predicate combination ({@code isLoopbackAddress() ||
     * isAnyLocalAddress() || isLinkLocalAddress() || isSiteLocalAddress()
     * || isMulticastAddress()} plus the IPv6 unique-local check) mirrors
     * {@code SslRequired.isLocal} in {@code keycloak-common}.
     */
    private void validateSchemeAndHost(String pushUrl) {
        if (allowInsecure) {
            return;
        }
        URI uri;
        try {
            uri = new URI(pushUrl);
        } catch (Exception e) {
            throw new SsfPushUrlValidationException(Reason.URL_MALFORMED,
                    "delivery.endpoint_url is not a valid URI");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equalsIgnoreCase("https")) {
            throw new SsfPushUrlValidationException(Reason.SCHEME_INSECURE,
                    "delivery.endpoint_url must use https; set the "
                            + "allow-insecure-push-targets SPI option to permit http for closed-network deployments");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new SsfPushUrlValidationException(Reason.URL_MALFORMED,
                    "delivery.endpoint_url must include a host");
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            // Unresolvable at config time. Accept — the receiver's push
            // attempt will fail at delivery time, which is not an SSRF.
            return;
        }
        for (InetAddress address : addresses) {
            if (isPrivateOrLoopback(address)) {
                throw new SsfPushUrlValidationException(Reason.HOST_PRIVATE,
                        "delivery.endpoint_url resolves to a non-routable, loopback, link-local,"
                                + " or private-network address (" + address.getHostAddress() + "); set the "
                                + "allow-insecure-push-targets SPI option to permit this for closed-network deployments");
            }
        }
    }

    private static boolean isPrivateOrLoopback(InetAddress address) {
        return address.isLoopbackAddress()
                || address.isAnyLocalAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || isUniqueLocalIpv6(address);
    }

    /**
     * IPv6 unique-local address range fc00::/7 (RFC 4193). Mirrors
     * {@code SslRequired.isUniqueLocal}.
     */
    private static boolean isUniqueLocalIpv6(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return false;
        }
        byte[] bytes = address.getAddress();
        return ((byte) (bytes[0] & 0b11111110)) == (byte) 0xFC;
    }

    /** Stable reason codes for callers that want to map exceptions to specific HTTP responses or logs. */
    public enum Reason {
        URL_MISSING,
        URL_MALFORMED,
        ALLOWLIST_EMPTY,
        NOT_IN_ALLOWLIST,
        SCHEME_INSECURE,
        HOST_PRIVATE
    }

    /**
     * Specialised {@link SsfException} carrying a stable {@link Reason} so
     * tests / integrations can map a rejection to a specific cause without
     * string-matching the user-facing message. The base {@code SsfException}
     * is mapped to HTTP 400 by the existing exception mapper, so callers
     * don't need to do anything special — throwing this is enough to surface
     * a clean rejection to the receiver.
     */
    public static class SsfPushUrlValidationException extends SsfException {

        private final Reason reason;

        public SsfPushUrlValidationException(Reason reason, String message) {
            super(message);
            this.reason = reason;
        }

        public Reason getReason() {
            return reason;
        }
    }
}
