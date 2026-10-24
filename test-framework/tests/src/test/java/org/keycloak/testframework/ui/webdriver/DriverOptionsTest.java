package org.keycloak.testframework.ui.webdriver;

import java.util.List;
import java.util.Map;

import org.keycloak.testframework.https.CertificatesConfigBuilder;
import org.keycloak.testframework.https.ManagedCertificates;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;

public class DriverOptionsTest {

    @Test
    public void chromeTrustsManagedCertificate() {
        ManagedCertificates certs = new ManagedCertificates(new CertificatesConfigBuilder().tlsEnabled(true));

        String[] extra = DriverUtils.chromeTlsArguments(certs);
        ChromeOptions options = DriverOptions.createChromeOptions(true);
        options.addArguments(extra);

        Assertions.assertTrue(chromeArgs(options).contains("--ignore-certificate-errors-spki-list=" + certs.getServerCertificateSpkiSha256()));
        Assertions.assertFalse(chromeArgs(options).contains("--ignore-certificate-errors"));
    }

    @Test
    public void noSpkiArgumentWithoutTls() {
        ManagedCertificates certs = new ManagedCertificates(new CertificatesConfigBuilder());
        Assertions.assertEquals(0, DriverUtils.chromeTlsArguments(certs).length);
    }

    @SuppressWarnings("unchecked")
    private static List<String> chromeArgs(ChromeOptions options) {
        return (List<String>) ((Map<String, Object>) options.asMap().get("goog:chromeOptions")).get("args");
    }
}
