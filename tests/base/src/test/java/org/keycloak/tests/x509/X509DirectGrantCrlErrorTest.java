package org.keycloak.tests.x509;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.conditions.DisabledForServers;
import org.keycloak.tests.client.AbstractMutualTLSClientTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
@DisabledForServers({"embedded", "remote"})
public class X509DirectGrantCrlErrorTest extends AbstractX509AuthenticationTest {

    private static final String UNPARSABLE_CRL = "unparsable-test.crl";
    private static final String STALE_CRL = "stale-test.crl";
    private static final String MISSING_CRL = "missing-test.crl";

    private static Path unparsableCrl;
    private static Path staleCrl;

    @InjectHttpServer
    HttpServer httpServer;

    @TestSetup
    public void createCrlFiles() throws Exception {
        Path configDir = Paths.get(runOnServer.fetchString(CrlFileLoaderProbe.configDir()));
        unparsableCrl = Files.writeString(configDir.resolve(UNPARSABLE_CRL), "not a CRL", StandardCharsets.UTF_8);
        staleCrl = Files.write(configDir.resolve(STALE_CRL), CrlGenerator.generateStaleCrl());
    }

    @AfterAll
    public static void deleteCrlFiles() throws Exception {
        if (unparsableCrl != null) {
            Files.deleteIfExists(unparsableCrl);
        }
        if (staleCrl != null) {
            Files.deleteIfExists(staleCrl);
        }
    }

    @Test
    public void unparsableCrlFileDetailsAreNotReturned() {
        assertCrlFailure(UNPARSABLE_CRL, "Unable to load CRL");
    }

    @Test
    public void missingCrlFileDetailsAreNotReturned() {
        assertCrlFailure(MISSING_CRL, "Unable to load CRL");
    }

    @Test
    public void staleCrlFileDetailsAreNotReturned() {
        assertCrlFailure(STALE_CRL, "CRL is not refreshed");
    }

    @Test
    public void missingHttpCrlDetailsAreNotReturned() {
        String crlUrl = "http://" + httpServer.getAddress().getHostString() + ":" + httpServer.getAddress().getPort() + "/missing.crl";
        assertCrlFailure(crlUrl, "Unable to load CRL");
    }

    private void assertCrlFailure(String crlPath, String expectedDescription) {
        X509AuthenticatorConfigModel config = new X509AuthenticatorConfigModel()
                .setCRLEnabled(true)
                .setCRLRelativePath(crlPath)
                .setCrlAbortIfNonUpdated(true)
                .setCASubjectDN(List.of(AbstractMutualTLSClientTest.CA_CERTIFICATE_SUBJECT_DN));
        String configId = createConfig(directGrantExecution.getId(), newConfig("x509-crl", config.getConfig()));
        managedRealm.cleanup().add(r -> r.flows().removeAuthenticatorConfig(configId));

        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        Assertions.assertEquals(401, response.getStatusCode());
        Assertions.assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        Assertions.assertEquals(expectedDescription, response.getErrorDescription());
    }
}
