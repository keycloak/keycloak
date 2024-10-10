package org.keycloak.utils;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.representations.account.DeviceRepresentation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Supplier;

public class SecureContextResolverTest {

    static final String BROWSER_SAFARI = "Safari/18.0.1";

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

    @Test
    public void testQuirksSafari() {
        assertSecureContext("https://127.0.0.1", BROWSER_SAFARI, true);
        assertSecureContext("https://something", BROWSER_SAFARI, true);
        assertSecureContext("http://[::1]", BROWSER_SAFARI,false);
        assertSecureContext("http://[0000:0000:0000:0000:0000:0000:0000:0001]", BROWSER_SAFARI, false);
        assertSecureContext("http://localhost", BROWSER_SAFARI, false);
        assertSecureContext("http://localhost.", BROWSER_SAFARI, false);
        assertSecureContext("http://test.localhost", BROWSER_SAFARI, false);
        assertSecureContext("http://test.localhost.", BROWSER_SAFARI, false);
    }

    void assertSecureContext(String url, boolean expectedSecureContext) {
        assertSecureContext(url, null, expectedSecureContext);
    }

    void assertSecureContext(String url, String browser, boolean expectedSecureContext) {
        DeviceRepresentation deviceRepresentation = new DeviceRepresentation();
        Supplier<DeviceRepresentation> deviceRepresentationSupplier = () -> deviceRepresentation;

        if (browser != null) {
            deviceRepresentation.setBrowser(browser);
        }

        try {
            Assert.assertEquals(expectedSecureContext, SecureContextResolver.isSecureContext(new URI(url), deviceRepresentationSupplier));
        } catch (URISyntaxException e) {
            Assert.fail(e.getMessage());
        }
    }

}
