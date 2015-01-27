package org.keycloak.test;

import junit.framework.TestCase;

import org.keycloak.services.resources.CorsUtil;

public class CorsUtilTest extends TestCase {

    public void testParseMultipleHeaderValues() {

        String[] out = CorsUtil.parseMultipleHeaderValues(null);

        assertEquals(0, out.length);

        out = CorsUtil.parseMultipleHeaderValues("GET, PUT, HEAD");

        assertEquals("GET", out[0]);
        assertEquals("PUT", out[1]);
        assertEquals("HEAD", out[2]);
        assertEquals(3, out.length);

        out = CorsUtil.parseMultipleHeaderValues("GET,PUT,HEAD");

        assertEquals("GET", out[0]);
        assertEquals("PUT", out[1]);
        assertEquals("HEAD", out[2]);
        assertEquals(3, out.length);

        out = CorsUtil.parseMultipleHeaderValues("GET , PUT , HEAD");

        assertEquals("GET", out[0]);
        assertEquals("PUT", out[1]);
        assertEquals("HEAD", out[2]);
        assertEquals(3, out.length);

        out = CorsUtil.parseMultipleHeaderValues("GET PUT HEAD");

        assertEquals("GET", out[0]);
        assertEquals("PUT", out[1]);
        assertEquals("HEAD", out[2]);
        assertEquals(3, out.length);
    }

    public void testFormatCanonical() {

        assertEquals(CorsUtil.formatCanonical("content-type"), "Content-Type");
        assertEquals(CorsUtil.formatCanonical("CONTENT-TYPE"), "Content-Type");
        assertEquals(CorsUtil.formatCanonical("X-type"), "X-Type");
        assertEquals(CorsUtil.formatCanonical("Origin"), "Origin");
        assertEquals(CorsUtil.formatCanonical("A"), "A");

        try {
            assertEquals(CorsUtil.formatCanonical(""), "");
            fail("Failed to raise IllegalArgumentException on empty string");

        } catch (IllegalArgumentException e) {
            // ok
        }
    }
}
