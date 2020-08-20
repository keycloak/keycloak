package org.keycloak.models;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BrowserSecurityHeadersTest {

    @Test
    public void contentSecurityPolicyBuilderTest() {
        assertEquals("frame-src 'self'; frame-ancestors 'self'; object-src 'none';", ContentSecurityPolicyBuilder.create().build());
        assertEquals("frame-ancestors 'self'; object-src 'none';", ContentSecurityPolicyBuilder.create().frameSrc(null).build());
        assertEquals("frame-src 'self'; object-src 'none';", ContentSecurityPolicyBuilder.create().frameAncestors(null).build());
        assertEquals("frame-src 'custom-frame-src'; frame-ancestors 'custom-frame-ancestors'; object-src 'none';", ContentSecurityPolicyBuilder.create().frameSrc("'custom-frame-src'").frameAncestors("'custom-frame-ancestors'").build());
    }

}
