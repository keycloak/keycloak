package org.keycloak.tests.x509;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.conditions.DisabledForServers;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
@DisabledForServers({"embedded", "remote"})
public class X509CrlFileTraversalTest {

    private static final String CONTAINED_CRL = "contained-test.crl";
    private static final String TRAVERSAL_TARGET = "traversal-target.crl";
    private static final String TRAVERSAL_PATH = "../" + TRAVERSAL_TARGET;

    private static Path containedCrl;
    private static Path outsideCrl;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests.x509")
    RunOnServerClient runOnServer;

    @TestSetup
    public void createCrlFiles() throws Exception {
        Path configDir = Paths.get(runOnServer.fetchString(CrlFileLoaderProbe.configDir()));
        byte[] crl = CrlGenerator.generateValidCrl();
        containedCrl = Files.write(configDir.resolve(CONTAINED_CRL), crl);
        outsideCrl = Files.write(configDir.getParent().resolve(TRAVERSAL_TARGET), crl);
    }

    @AfterAll
    public static void deleteCrlFiles() throws Exception {
        if (containedCrl != null) {
            Files.deleteIfExists(containedCrl);
        }
        if (outsideCrl != null) {
            Files.deleteIfExists(outsideCrl);
        }
    }

    @Test
    public void crlRelativePathStayingInConfigDirIsLoaded() {
        String result = runOnServer.fetch(CrlFileLoaderProbe.loadCrl(CONTAINED_CRL), String.class);
        Assertions.assertEquals("LOADED:1", result, "A CRL located inside the configuration directory must be loaded");
    }

    @Test
    public void crlRelativePathEscapingConfigDirIsRejected() {
        String result = runOnServer.fetch(CrlFileLoaderProbe.loadCrl(TRAVERSAL_PATH), String.class);
        Assertions.assertFalse(result.startsWith("LOADED"),
                "Path traversal succeeded: a CRL file outside the configuration directory was read via " + TRAVERSAL_PATH + " -> " + result);
        Assertions.assertTrue(result.contains("Unable to load CRL"),
                "Expected the traversal path to be rejected by containment, but got " + result);
    }
}
