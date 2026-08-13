package org.keycloak.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Supplier;

import org.keycloak.representations.account.DeviceRepresentation;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SecureContextResolverTest {

    static DeviceRepresentation DEVICE_UNKOWN;
    static DeviceRepresentation DEVICE_SAFARI;

    static {
        DEVICE_UNKOWN = new DeviceRepresentation();
        DEVICE_UNKOWN.setBrowser(DeviceRepresentation.UNKNOWN);

        DEVICE_SAFARI = new DeviceRepresentation();
        DEVICE_SAFARI.setBrowser("Safari/18.0.1");
    }

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
        assertSecureContext("http://[0:0:0:0:0:0:0:1]", true);
        assertSecureContext("http://[0:0:0::1]", true);
        assertSecureContext("http://[::2]", false);
        assertSecureContext("http://[2001:0000:130F:0000:0000:09C0:876A:130B]", false);
        assertSecureContext("http://::1", false);
        assertSecureContext("http://[FE80:0000:130F:0000:0000:09C0:876A:130B]", false);
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
    public void testIsLocalhost() {
        assertTrue(SecureContextResolver.isLocalAddress("127.0.0.1"));
        assertFalse(SecureContextResolver.isLocalAddress("not.an.ip"));
        assertFalse(SecureContextResolver.isLocalAddress(null));
        assertFalse(SecureContextResolver.isLocalAddress(""));
        assertTrue(SecureContextResolver.isLocalAddress("::1"));
        assertTrue(SecureContextResolver.isLocalAddress("0:0:0:0:0:0:0:1"));
    }

    @Test
    public void testQuirksSafari() {
        assertSecureContext("https://127.0.0.1", DEVICE_SAFARI, true);
        assertSecureContext("https://something", DEVICE_SAFARI, true);
        assertSecureContext("http://[::1]", DEVICE_SAFARI,false);
        assertSecureContext("http://[0000:0000:0000:0000:0000:0000:0000:0001]", DEVICE_SAFARI, false);
        assertSecureContext("http://localhost", DEVICE_SAFARI, false);
        assertSecureContext("http://localhost.", DEVICE_SAFARI, false);
        assertSecureContext("http://test.localhost", DEVICE_SAFARI, false);
        assertSecureContext("http://test.localhost.", DEVICE_SAFARI, false);
    }

    @Test
    public void testNoDeviceRepresentation() {
        assertSecureContext("http://localhost", null, true);
    }

    @Test
    public void testHttpsIframeInInsecureParent() {
        // HTTPS iframe embedded in an HTTP parent is not a secure context
        assertSecureContext("https://keycloak.example.com/iframe", DEVICE_UNKOWN, "http://admin.internal:8080/admin", "iframe", false);

        // HTTPS iframe embedded in an HTTPS parent remains secure
        assertSecureContext("https://keycloak.example.com/iframe", DEVICE_UNKOWN, "https://admin.example.com/admin", "iframe", true);

        // HTTPS top-level navigation from an HTTP page is still a secure context (not an iframe)
        assertSecureContext("https://keycloak.example.com/admin", DEVICE_UNKOWN, "http://other.com", "document", true);

        // HTTPS iframe without a Referer header remains secure (can't determine parent scheme)
        assertSecureContext("https://keycloak.example.com/iframe", DEVICE_UNKOWN, null, "iframe", true);

        // HTTPS request with HTTP Referer but no Sec-Fetch-Dest remains secure (can't confirm it's an iframe)
        assertSecureContext("https://keycloak.example.com/admin", DEVICE_UNKOWN, "http://other.com", null, true);

        // HTTP iframe in HTTP parent still follows existing logic (localhost is secure)
        assertSecureContext("http://localhost/iframe", DEVICE_UNKOWN, "http://localhost/admin", "iframe", true);

        // HTTP iframe in HTTP parent, non-local host is insecure
        assertSecureContext("http://admin.internal:8080/iframe", DEVICE_UNKOWN, "http://admin.internal:8080/admin", "iframe", false);
    }

    void assertSecureContext(String url, boolean expectedSecureContext) {
        assertSecureContext(url, DEVICE_UNKOWN, expectedSecureContext);
    }

    void assertSecureContext(String url, DeviceRepresentation deviceRepresentation, boolean expectedSecureContext) {
        Supplier<DeviceRepresentation> deviceRepresentationSupplier = () -> deviceRepresentation;

        try {
            Assert.assertEquals(expectedSecureContext, SecureContextResolver.isSecureContext(new URI(url), deviceRepresentationSupplier));
        } catch (URISyntaxException e) {
            Assert.fail(e.getMessage());
        }
    }

    void assertSecureContext(String url, DeviceRepresentation deviceRepresentation, String referer, String secFetchDest, boolean expectedSecureContext) {
        Supplier<DeviceRepresentation> deviceRepresentationSupplier = () -> deviceRepresentation;

        try {
            Assert.assertEquals(expectedSecureContext, SecureContextResolver.isSecureContext(new URI(url), deviceRepresentationSupplier, referer, secFetchDest));
        } catch (URISyntaxException e) {
            Assert.fail(e.getMessage());
        }
    }

}
