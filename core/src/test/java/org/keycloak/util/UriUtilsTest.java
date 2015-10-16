package org.keycloak.util;

import org.junit.Test;
import org.keycloak.common.util.UriUtils;

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

    public void assertValid(String origin) {
        assertTrue(UriUtils.isOrigin(origin));
    }

    public void assertInvalid(String origin) {
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
