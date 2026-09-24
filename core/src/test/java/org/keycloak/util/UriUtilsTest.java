/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.util;

import java.net.URI;

import org.keycloak.common.util.UriUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UriUtilsTest {

    @Test
    public void testOrigins() {
        assertValid("http://test");
        assertValid("http://test:8080");
        assertValid("https://test");
        assertValid("http://test.com");
        assertValid("https://test.com");
        assertValid("https://test.com:8080");
        assertValid("http://sub.test.com");
        assertValid("https://sub.test.com");
        assertValid("https://sub.test.com:8080");
        assertValid("http://192.168.123.123");
        assertValid("https://192.168.123.123");
        assertValid("https://192.168.123.123:8080");
        assertValid("https://sub-sub.test.com");
        assertValid("https://sub.test-test.com");

        assertInvalid("https://test/");
        assertInvalid("{");
        assertInvalid("https://{}");
        assertInvalid("https://)");
        assertInvalid("http://test:test");
        assertInvalid("http://test:8080:8080");
    }

    private void assertValid(String origin) {
        assertTrue(UriUtils.isOrigin(origin));
    }

    private void assertInvalid(String origin) {
        assertFalse(UriUtils.isOrigin(origin));
    }

    @Test
    public void testOriginEqualsIgnoresSchemeAndHostCase() {
        assertTrue(UriUtils.originEquals("https://Example.COM:8443", "https://example.com:8443"));
        assertTrue(UriUtils.originEquals("HTTPS://EXAMPLE.COM", "https://example.com"));
        assertFalse(UriUtils.originEquals("https://Example.COM:8443", "https://example.com:8444"));
        assertFalse(UriUtils.originEquals("https://Example.COM:8443", "https://other.com:8443"));
        assertFalse(UriUtils.originEquals("https://allowed_host", "https://evil_host"));
        // Origin is scheme + host + port; user-info is not part of the origin
        assertTrue(UriUtils.originEquals("https://Alice@allowed_host", "https://alice@ALLOWED_HOST"));
        assertTrue(UriUtils.originEquals("https://Alice@example.com", "https://alice@example.com"));
        // Opaque URIs with no authority must not match on scheme alone
        assertFalse(UriUtils.schemeAndHostEqual(URI.create("mailto:alice@example.com"),
                URI.create("mailto:bob@evil.test")));
        assertTrue(UriUtils.schemeAndHostEqual(URI.create("mailto:alice@example.com"),
                URI.create("MAILTO:alice@example.com")));
        // Registry-name host case-insensitive; port still compared for origin equality
        assertTrue(UriUtils.originEquals("https://ALLOWED_HOST:444", "https://allowed_host:444"));
        assertFalse(UriUtils.originEquals("https://allowed_host:444", "https://allowed_host:443"));
        // Nonnumeric registry-name ports must not collapse to "no port" and match each other
        assertFalse(UriUtils.schemeHostAndPortEqual(URI.create("https://foo:bar/path"),
                URI.create("https://foo:baz/path")));
        assertTrue(UriUtils.schemeHostAndPortEqual(URI.create("https://foo:bar/path"),
                URI.create("https://FOO:bar/path")));
        // Empty explicit port is distinct from an absent port
        assertFalse(UriUtils.schemeHostAndPortEqual(URI.create("https://keycloak:"),
                URI.create("https://keycloak")));
    }

    @Test
    public void testHasExplicitPortIgnoresIpv6LiteralColons() {
        assertFalse(UriUtils.hasExplicitPort(URI.create("https://[::1]/callback")));
        assertTrue(UriUtils.hasExplicitPort(URI.create("https://[::1]:443/callback")));
        assertTrue(UriUtils.hasExplicitPort(URI.create("https://example.com:8443/callback")));
        assertFalse(UriUtils.hasExplicitPort(URI.create("https://example.com/callback")));
        assertTrue(UriUtils.hasExplicitPort(URI.create("https://example.com:/callback")));
    }

    @Test
    public void testStripQueryParam(){
        assertEquals("http://localhost",UriUtils.stripQueryParam("http://localhost?login_hint=michael","login_hint"));
        assertEquals("http://localhost",UriUtils.stripQueryParam("http://localhost?login_hint=michael@me.com","login_hint"));
        assertEquals("http://localhost?param=test",UriUtils.stripQueryParam("http://localhost?param=test&login_hint=michael","login_hint"));
        assertEquals("http://localhost?param=test",UriUtils.stripQueryParam("http://localhost?param=test&login_hint=michael@me.com","login_hint"));
        assertEquals("http://localhost?param=test", UriUtils.stripQueryParam("http://localhost?login_hint=michael&param=test", "login_hint"));
        assertEquals("http://localhost?param=test",UriUtils.stripQueryParam("http://localhost?login_hint=michael@me.com&param=test","login_hint"));
        assertEquals("http://localhost?pre=test&param=test",UriUtils.stripQueryParam("http://localhost?pre=test&login_hint=michael&param=test","login_hint"));
        assertEquals("http://localhost?pre=test&param=test",UriUtils.stripQueryParam("http://localhost?pre=test&login_hint=michael@me.com&param=test","login_hint"));
    }
}
