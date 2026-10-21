package org.keycloak.models;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.keycloak.models.BrowserSecurityHeaders.CONTENT_SECURITY_POLICY;
import static org.keycloak.models.BrowserSecurityHeaders.CONTENT_SECURITY_POLICY_REPORT_ONLY;
import static org.keycloak.models.BrowserSecurityHeaders.REFERRER_POLICY;
import static org.keycloak.models.BrowserSecurityHeaders.STRICT_TRANSPORT_SECURITY;
import static org.keycloak.models.BrowserSecurityHeaders.X_CONTENT_TYPE_OPTIONS;
import static org.keycloak.models.BrowserSecurityHeaders.X_FRAME_OPTIONS;
import static org.keycloak.models.BrowserSecurityHeaders.X_ROBOTS_TAG;
import static org.keycloak.models.BrowserSecurityHeaders.realmDefaultHeaders;

import static org.junit.Assert.assertEquals;

public class BrowserSecurityHeadersTest {

    @Test
    public void contentSecurityPolicyBuilderTest() {
        assertEquals("frame-src 'self'; frame-ancestors 'self'; object-src 'none';", ContentSecurityPolicyBuilder.create().build());
        assertEquals("frame-ancestors 'self'; object-src 'none';", ContentSecurityPolicyBuilder.create().frameSrc(null).build());
        assertEquals("frame-src 'self'; object-src 'none';", ContentSecurityPolicyBuilder.create().frameAncestors(null).build());
        assertEquals("frame-src 'custom-frame-src'; frame-ancestors 'custom-frame-ancestors'; object-src 'none';", ContentSecurityPolicyBuilder.create().frameSrc("'custom-frame-src'").frameAncestors("'custom-frame-ancestors'").build());
        assertEquals("frame-src localhost; frame-ancestors 'self'; object-src 'none';", ContentSecurityPolicyBuilder.create().frameSrc("localhost").build());
        assertEquals("frame-src 'self' localhost; frame-ancestors 'self'; object-src 'none';",
                ContentSecurityPolicyBuilder.create().addFrameSrc("localhost").build());
    }

    private void assertParsedDirectives(String directives) {
        assertEquals(directives, ContentSecurityPolicyBuilder.create(directives).build());
    }

    @Test
    public void parseSecurityPolicyBuilderTest() {
        assertParsedDirectives("frame-src 'self'; frame-ancestors 'self'; object-src 'none';");
        assertParsedDirectives("frame-ancestors 'self'; object-src 'none';");
        assertParsedDirectives("frame-src 'self'; object-src 'none';");
        assertParsedDirectives("frame-src 'custom-frame-src'; frame-ancestors 'custom-frame-ancestors'; object-src 'none';");
        assertParsedDirectives("frame-src 'custom-frame-src'; frame-ancestors 'custom-frame-ancestors'; object-src 'none'; style-src 'self';");
        assertParsedDirectives("frame-src 'custom-frame-src'; frame-ancestors 'custom-frame-ancestors'; object-src 'none'; sandbox;");
        assertEquals("frame-src 'custom-frame-src'; sandbox;", ContentSecurityPolicyBuilder.create("frame-src   'custom-frame-src' ; sandbox ;  ").build());
    }

    @Test
    public void testDefaultHeaders() {
        List<BrowserSecurityHeaders> expectedHeaders = Arrays.asList(
                X_FRAME_OPTIONS,
                CONTENT_SECURITY_POLICY,
                CONTENT_SECURITY_POLICY_REPORT_ONLY,
                X_CONTENT_TYPE_OPTIONS,
                X_ROBOTS_TAG,
                STRICT_TRANSPORT_SECURITY,
                REFERRER_POLICY
        );

        assertEquals(expectedHeaders.size(), realmDefaultHeaders.size());

        for (BrowserSecurityHeaders header : expectedHeaders) {
            assertEquals(header.getDefaultValue(), realmDefaultHeaders.get(header.getKey()));
        }
    }
}
