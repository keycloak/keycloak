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
