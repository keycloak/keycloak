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

import java.util.LinkedHashSet;
import java.util.Set;

import org.keycloak.ssf.transmitter.support.SsfPushUrlValidator.Reason;
import org.keycloak.ssf.transmitter.support.SsfPushUrlValidator.SsfPushUrlValidationException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SsfPushUrlValidator}, the SSRF gate for
 * receiver-supplied push URLs.
 *
 * <p>The validator has two collaborating responsibilities:
 * <ol>
 *     <li>Match the receiver-supplied URL against the per-client allow-list
 *         using the same exact-or-trailing-wildcard semantics that OIDC
 *         redirect URI validation uses (with bare-{@code "*"} disallowed
 *         and a few SSRF-specific tightenings on entry shape).</li>
 *     <li>Re-validate the supplied URL's scheme and host class after a
 *         successful match — https-only and no loopback/private-network
 *         targets — unless the {@code allowInsecure} flag bypasses both
 *         checks (closed-network deployments, integration tests).</li>
 * </ol>
 *
 * <p>The two responsibilities are exercised separately so a regression in
 * one doesn't mask the other.
 */
class SsfPushUrlValidatorTest {

    /** Most production scenarios: scheme/host check enforced. */
    private final SsfPushUrlValidator strict = new SsfPushUrlValidator(false);

    /** Closed-network / integration-test scenarios: scheme/host check bypassed. */
    private final SsfPushUrlValidator relaxed = new SsfPushUrlValidator(true);

    // -- entry filtering ----------------------------------------------------

    @Test
    void filterUsableEntries_dropsBareWildcard() {
        Set<String> usable = strict.filterUsableEntries(set("*"));

        assertTrue(usable.isEmpty(),
                "bare '*' must be dropped — admins must not be able to disable the SSRF gate with one keystroke");
    }

    @Test
    void filterUsableEntries_dropsWildcardInHostPortion() {
        Set<String> usable = strict.filterUsableEntries(set(
                "https://*.example.com/*",
                "https://example.com/*"));

        assertEquals(set("https://example.com/*"), usable,
                "wildcard in the host portion must be rejected — only the well-formed entry survives");
    }

    @Test
    void match_rejectsWildcardEntryWithQuery() {
        // Entries with '?' that also end with '*' are not honoured as
        // wildcards by the matcher (see RedirectUtils.matchesRedirects).
        // They pass filterUsableEntries because the host portion parses
        // cleanly, but they have no effect at match time — admin should
        // not write them, but if they do they don't widen the gate.
        Set<String> allow = set("https://example.com/feeds/*?token=abc");

        assertEquals(null,
                SsfPushUrlValidator.matchesAllowList(allow, "https://example.com/feeds/x"));
    }

    @Test
    void filterUsableEntries_dropsNonHttpScheme() {
        Set<String> usable = strict.filterUsableEntries(set(
                "ftp://example.com/*",
                "javascript:alert(1)/*",
                "https://example.com/*"));

        assertEquals(set("https://example.com/*"), usable,
                "non-http(s) schemes must be dropped from the effective allow-list");
    }

    @Test
    void filterUsableEntries_dropsRelativeOrSchemeMissingEntries() {
        Set<String> usable = strict.filterUsableEntries(set(
                "/relative/path/*",
                "example.com/*",
                "https://example.com/*"));

        assertEquals(set("https://example.com/*"), usable,
                "relative/scheme-missing entries must be dropped");
    }

    @Test
    void filterUsableEntries_keepsBothExactAndWildcardEntries() {
        Set<String> usable = strict.filterUsableEntries(set(
                "https://example.com/exact",
                "https://example.com/wild/*"));

        assertEquals(set("https://example.com/exact", "https://example.com/wild/*"), usable);
    }

    // -- match semantics (mirror RedirectUtils.matchesRedirects + tightenings) ---

    @Test
    void match_exactMatch() {
        String url = "https://example.com/feeds/business/caep/abc/123";
        Set<String> allow = set(url);

        assertEquals(url, SsfPushUrlValidator.matchesAllowList(allow, url));
    }

    @Test
    void match_trailingWildcardPrefix() {
        Set<String> allow = set("https://example.com/feeds/*");

        assertEquals("https://example.com/feeds/",
                SsfPushUrlValidator.matchesAllowList(allow, "https://example.com/feeds/business/caep/abc/123"),
                "wildcard match returns the entry with the trailing '*' stripped");
    }

    @Test
    void match_trailingSlashForgiveness() {
        // A wildcard entry like "https://example.com/feeds/*" must also
        // match "https://example.com/feeds" (no trailing slash) — admins
        // shouldn't have to register both forms. Mirrors RedirectUtils.
        Set<String> allow = set("https://example.com/feeds/*");

        assertEquals("https://example.com/feeds",
                SsfPushUrlValidator.matchesAllowList(allow, "https://example.com/feeds"));
    }

    @Test
    void match_stripsQueryUnderWildcard() {
        Set<String> allow = set("https://example.com/feeds/*");

        assertEquals("https://example.com/feeds/",
                SsfPushUrlValidator.matchesAllowList(allow, "https://example.com/feeds/abc?token=x"),
                "query string must be stripped before prefix-matching a wildcard entry");
    }

    @Test
    void match_stripsFragmentUnderWildcard() {
        Set<String> allow = set("https://example.com/feeds/*");

        assertEquals("https://example.com/feeds/",
                SsfPushUrlValidator.matchesAllowList(allow, "https://example.com/feeds/abc#frag"));
    }

    @Test
    void match_returnsNullWhenNoEntryMatches() {
        Set<String> allow = set("https://other.example.com/*");

        assertEquals(null,
                SsfPushUrlValidator.matchesAllowList(allow, "https://attacker.example.com/feeds/x"));
    }

    @Test
    void match_returnsFirstMatchingEntry() {
        // Multiple entries: stops at the first match. Insertion order
        // matters because LinkedHashSet preserves it.
        Set<String> allow = set(
                "https://primary.example.com/*",
                "https://backup.example.com/*");

        assertEquals("https://backup.example.com/",
                SsfPushUrlValidator.matchesAllowList(allow, "https://backup.example.com/feeds/x"));
    }

    // -- validate(): rejection codes ----------------------------------------

    @Test
    void validate_rejectsMissingUrl() {
        SsfPushUrlValidationException ex = assertThrows(SsfPushUrlValidationException.class,
                () -> strict.validate((String) null, set("https://example.com/*")));

        assertEquals(Reason.URL_MISSING, ex.getReason());
    }

    @Test
    void validate_rejectsBlankUrl() {
        SsfPushUrlValidationException ex = assertThrows(SsfPushUrlValidationException.class,
                () -> strict.validate("   ", set("https://example.com/*")));

        assertEquals(Reason.URL_MISSING, ex.getReason());
    }

    @Test
    void validate_rejectsEmptyAllowList() {
        SsfPushUrlValidationException ex = assertThrows(SsfPushUrlValidationException.class,
                () -> strict.validate("https://example.com/feeds/x", set()));

        assertEquals(Reason.ALLOWLIST_EMPTY, ex.getReason());
    }

    @Test
    void validate_rejectsAllowListWithOnlyBareStar() {
        // Bare '*' is filtered out — an allow-list containing only '*' has
        // zero usable entries and must be treated as effectively empty.
        SsfPushUrlValidationException ex = assertThrows(SsfPushUrlValidationException.class,
                () -> strict.validate("https://example.com/feeds/x", set("*")));

        assertEquals(Reason.ALLOWLIST_EMPTY, ex.getReason());
        assertTrue(ex.getMessage().contains("bare '*'"),
                "error message should explain why the bare-'*' entry is not honoured");
    }

    @Test
    void validate_rejectsUrlOutsideAllowList() {
        SsfPushUrlValidationException ex = assertThrows(SsfPushUrlValidationException.class,
                () -> strict.validate("https://attacker.example.com/x", set("https://example.com/*")));

        assertEquals(Reason.NOT_IN_ALLOWLIST, ex.getReason());
    }

    // -- post-match scheme/host check (strict mode) -------------------------

    @Test
    void validate_strict_rejectsHttpScheme() {
        SsfPushUrlValidationException ex = assertThrows(SsfPushUrlValidationException.class,
                () -> strict.validate("http://example.com/feeds/x", set("http://example.com/*")));

        assertEquals(Reason.SCHEME_INSECURE, ex.getReason());
        assertTrue(ex.getMessage().contains("allow-insecure-push-targets"),
                "error message should point operators at the SPI flag that relaxes this check");
    }

    @Test
    void validate_strict_rejectsLoopbackIpv4() {
        SsfPushUrlValidationException ex = assertThrows(SsfPushUrlValidationException.class,
                () -> strict.validate("https://127.0.0.1/feeds/x", set("https://127.0.0.1/*")));

        assertEquals(Reason.HOST_PRIVATE, ex.getReason());
        assertTrue(ex.getMessage().contains("127.0.0.1"));
    }

    @Test
    void validate_strict_rejectsLoopbackIpv6() {
        // [::1] is the IPv6 loopback. URI host comes back without
        // the brackets; InetAddress recognises it as loopback all the same.
        SsfPushUrlValidationException ex = assertThrows(SsfPushUrlValidationException.class,
                () -> strict.validate("https://[::1]/feeds/x", set("https://[::1]/*")));

        assertEquals(Reason.HOST_PRIVATE, ex.getReason());
    }

    @Test
    void validate_strict_rejectsImdsLinkLocal() {
        // 169.254.169.254 is the cloud Instance Metadata Service endpoint
        // (AWS, GCP, Azure) — a high-value SSRF target. Caught by the
        // link-local predicate.
        SsfPushUrlValidationException ex = assertThrows(SsfPushUrlValidationException.class,
                () -> strict.validate("https://169.254.169.254/latest/meta-data",
                        set("https://169.254.169.254/*")));

        assertEquals(Reason.HOST_PRIVATE, ex.getReason());
    }

    @Test
    void validate_strict_rejectsRfc1918SiteLocal() {
        for (String addr : new String[]{"10.0.0.1", "172.16.0.1", "192.168.0.1"}) {
            String url = "https://" + addr + "/feeds/x";
            SsfPushUrlValidationException ex = assertThrows(SsfPushUrlValidationException.class,
                    () -> strict.validate(url, set("https://" + addr + "/*")));
            assertEquals(Reason.HOST_PRIVATE, ex.getReason(),
                    "RFC 1918 address " + addr + " must be classified as private");
        }
    }

    @Test
    void validate_strict_rejectsIpv6UniqueLocal() {
        // fc00::/7 — IPv6 unique-local addresses (RFC 4193). Java's
        // InetAddress doesn't expose isUniqueLocal(), so the validator
        // implements the byte-prefix check inline.
        SsfPushUrlValidationException ex = assertThrows(SsfPushUrlValidationException.class,
                () -> strict.validate("https://[fc00::1]/feeds/x", set("https://[fc00::1]/*")));

        assertEquals(Reason.HOST_PRIVATE, ex.getReason());
    }

    @Test
    void validate_strict_rejectsAnyLocal() {
        // 0.0.0.0 — the unspecified address. Should never appear as a
        // legitimate push target.
        SsfPushUrlValidationException ex = assertThrows(SsfPushUrlValidationException.class,
                () -> strict.validate("https://0.0.0.0/feeds/x", set("https://0.0.0.0/*")));

        assertEquals(Reason.HOST_PRIVATE, ex.getReason());
    }

    @Test
    void validate_strict_rejectsMulticast() {
        // 224.0.0.0/4 — IPv4 multicast.
        SsfPushUrlValidationException ex = assertThrows(SsfPushUrlValidationException.class,
                () -> strict.validate("https://224.0.0.1/feeds/x", set("https://224.0.0.1/*")));

        assertEquals(Reason.HOST_PRIVATE, ex.getReason());
    }

    @Test
    void validate_strict_acceptsUnresolvableHostname() {
        // Unresolvable at config time. The push will fail at delivery
        // time anyway — that's not an SSRF, so we accept the URL rather
        // than gate stream-create on DNS availability at the moment of
        // registration.
        assertDoesNotThrow(() -> strict.validate(
                "https://this-host-does-not-exist.invalid/feeds/x",
                set("https://this-host-does-not-exist.invalid/*")));
    }

    @Test
    void validate_strict_acceptsLegitimatePublicHttpsUrl() {
        // 203.0.113.1 — RFC 5737 TEST-NET-3 documentation range. Not
        // routable in practice, but classified as global by Java; the
        // validator must accept it.
        assertDoesNotThrow(() -> strict.validate(
                "https://203.0.113.1/feeds/x",
                set("https://203.0.113.1/*")));
    }

    // -- post-match scheme/host check (relaxed mode) ------------------------

    @Test
    void validate_relaxed_acceptsHttpScheme() {
        // The closed-network / integration-test path: http is fine.
        assertDoesNotThrow(() -> relaxed.validate(
                "http://internal.example.com/feeds/x",
                set("http://internal.example.com/*")));
    }

    @Test
    void validate_relaxed_acceptsLoopback() {
        assertDoesNotThrow(() -> relaxed.validate(
                "http://127.0.0.1:8500/feeds/x",
                set("http://127.0.0.1:8500/*")));
    }

    @Test
    void validate_relaxed_acceptsRfc1918() {
        assertDoesNotThrow(() -> relaxed.validate(
                "http://10.0.0.1/feeds/x",
                set("http://10.0.0.1/*")));
    }

    @Test
    void validate_relaxed_stillRejectsAllowListMismatch() {
        // The flag relaxes the scheme/host check but does NOT bypass
        // the allow-list itself. ssf.validPushUrls remains the primary
        // SSRF defence regardless of the flag.
        SsfPushUrlValidationException ex = assertThrows(SsfPushUrlValidationException.class,
                () -> relaxed.validate(
                        "http://attacker.example.com/feeds/x",
                        set("http://internal.example.com/*")));

        assertEquals(Reason.NOT_IN_ALLOWLIST, ex.getReason());
    }

    @Test
    void validate_relaxed_stillRejectsEmptyAllowList() {
        SsfPushUrlValidationException ex = assertThrows(SsfPushUrlValidationException.class,
                () -> relaxed.validate("http://127.0.0.1/feeds/x", set()));

        assertEquals(Reason.ALLOWLIST_EMPTY, ex.getReason());
    }

    // -- cross-cutting: bare-* still no-op even when host check is relaxed --

    @Test
    void validate_relaxed_doesNotHonourBareWildcardEntry() {
        // The bare-'*' tightening is independent of allowInsecure —
        // dropping the SSRF defence in one keystroke is never permitted,
        // regardless of mode.
        SsfPushUrlValidationException ex = assertThrows(SsfPushUrlValidationException.class,
                () -> relaxed.validate("http://attacker.example.com/x", set("*")));

        assertEquals(Reason.ALLOWLIST_EMPTY, ex.getReason());
    }

    // -- helpers ------------------------------------------------------------

    /**
     * Linked hash set so insertion order is preserved. The wildcard
     * matcher is order-sensitive (returns the first matching entry).
     */
    private static Set<String> set(String... entries) {
        Set<String> out = new LinkedHashSet<>();
        for (String entry : entries) {
            out.add(entry);
        }
        return out;
    }
}
