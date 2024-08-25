package org.keycloak.cookie;

import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

public class SecureContextResolverTest {

    @Test
    public void testHttps() {
        assertSecureContext("https://127.0.0.1", true);
        assertSecureContext("https://something", true);
    }

    @Test
    public void testIp4() {
        assertSecureContext("http://127.0.0.1", true);
        assertSecureContext("http://127.0.0.128", true);
        assertSecureContext("http://127.0.0.255", true);
        assertSecureContext("http://127.0.0.256", false);
        assertSecureContext("http://127.0.1.1", true);
        assertSecureContext("http://127.254.232.123", true);
        assertSecureContext("http://127.256.232.123", false);
        assertSecureContext("http://10.0.0.10", false);
        assertSecureContext("http://127.0.0.1.nip.io", false);
    }

    @Test
    public void testIp6() {
        assertSecureContext("http://[::1]", true);
        assertSecureContext("http://[0000:0000:0000:0000:0000:0000:0000:0001]", true);
        assertSecureContext("http://[::2]", false);
        assertSecureContext("http://[2001:0000:130F:0000:0000:09C0:876A:130B]", false);
        assertSecureContext("http://::1", false);
    }

    @Test
    public void testLocalhost() {
        assertSecureContext("http://localhost", true);
        assertSecureContext("http://localhost.", true);
        assertSecureContext("http://localhostn", false);
        assertSecureContext("http://test.localhost", true);
        assertSecureContext("http://test.localhost.", true);
        assertSecureContext("http://test.localhostn", false);
        assertSecureContext("http://test.localhost.not", false);
    }

    void assertSecureContext(String url, boolean expectedSecureContext) {
        try {
            Assert.assertEquals(expectedSecureContext, SecureContextResolver.isSecureContext(new URI(url)));
        } catch (URISyntaxException e) {
            Assert.fail(e.getMessage());
        }
    }

}
