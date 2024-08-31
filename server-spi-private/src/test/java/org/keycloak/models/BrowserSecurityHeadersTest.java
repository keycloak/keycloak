package org.keycloak.models;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.keycloak.models.BrowserSecurityHeaders.CONTENT_SECURITY_POLICY;
import static org.keycloak.models.BrowserSecurityHeaders.CONTENT_SECURITY_POLICY_REPORT_ONLY;
import static org.keycloak.models.BrowserSecurityHeaders.REFERRER_POLICY;
import static org.keycloak.models.BrowserSecurityHeaders.STRICT_TRANSPORT_SECURITY;
import static org.keycloak.models.BrowserSecurityHeaders.X_CONTENT_TYPE_OPTIONS;
import static org.keycloak.models.BrowserSecurityHeaders.X_FRAME_OPTIONS;
import static org.keycloak.models.BrowserSecurityHeaders.X_ROBOTS_TAG;
import static org.keycloak.models.BrowserSecurityHeaders.X_XSS_PROTECTION;
import static org.keycloak.models.BrowserSecurityHeaders.realmDefaultHeaders;

import java.util.Arrays;
import java.util.List;

public class BrowserSecurityHeadersTest {

    @Test
    public void contentSecurityPolicyBuilderTest() {
        assertEquals("form-action 'self'; frame-src 'self'; frame-ancestors 'self'; object-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';", ContentSecurityPolicyBuilder.create().build());
        assertEquals("form-action 'self'; frame-ancestors 'self'; object-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';", ContentSecurityPolicyBuilder.create().frameSrc(null).build());
        assertEquals("form-action 'self'; frame-src 'self'; object-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';", ContentSecurityPolicyBuilder.create().frameAncestors(null).build());
        assertEquals("form-action 'self'; frame-src 'custom-frame-src'; frame-ancestors 'custom-frame-ancestors'; object-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';", ContentSecurityPolicyBuilder.create().frameSrc("'custom-frame-src'").frameAncestors("'custom-frame-ancestors'").build());
        assertEquals("form-action 'self'; frame-src localhost; frame-ancestors 'self'; object-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';", ContentSecurityPolicyBuilder.create().frameSrc("localhost").build());
        assertEquals("form-action 'self'; frame-src 'self' localhost; frame-ancestors 'self'; object-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';", ContentSecurityPolicyBuilder.create().addFrameSrc("localhost").build());
        assertEquals("form-action 'self'; frame-src 'self'; frame-ancestors 'self'; object-src 'none'; script-src localhost; style-src 'self' 'unsafe-inline';", ContentSecurityPolicyBuilder.create().scriptSrc("localhost").build());
        assertEquals("form-action 'self'; frame-src 'self'; frame-ancestors 'self'; object-src 'none'; script-src 'self' 'unsafe-inline'; style-src localhost;", ContentSecurityPolicyBuilder.create().styleSrc("localhost").build());

        // Adding a nonce or hash to 'unsafe-inline' should have no effect.
        assertEquals("form-action 'self'; frame-src 'self'; frame-ancestors 'self'; object-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';", ContentSecurityPolicyBuilder.create().addScriptSrc("'nonce-46cc8f91-509f-4f80-ba93-943431630d46'").build());
        assertEquals("form-action 'self'; frame-src 'self'; frame-ancestors 'self'; object-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';", ContentSecurityPolicyBuilder.create().addStyleSrc("'nonce-46cc8f91-509f-4f80-ba93-943431630d46'").build());

        assertEquals("form-action 'self'; frame-src 'self'; frame-ancestors 'self'; object-src 'none'; script-src 'self' https://github.com; style-src 'self' 'unsafe-inline';", ContentSecurityPolicyBuilder.create().scriptSrc("'self'").addScriptSrc("https://github.com").build());
        assertEquals("form-action 'self'; frame-src 'self'; frame-ancestors 'self'; object-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' https://github.com;", ContentSecurityPolicyBuilder.create().styleSrc("'self'").addStyleSrc("https://github.com").build());
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
                X_XSS_PROTECTION,
                STRICT_TRANSPORT_SECURITY,
                REFERRER_POLICY
        );

        assertEquals(expectedHeaders.size(), realmDefaultHeaders.size());

        for (BrowserSecurityHeaders header : expectedHeaders) {
            assertEquals(header.getDefaultValue(), realmDefaultHeaders.get(header.getKey()));
        }
    }
}
