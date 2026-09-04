package org.keycloak.tests.cli;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectCryptoHelper;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.crypto.CryptoHelper;
import org.keycloak.testframework.https.CertificatesConfig;
import org.keycloak.testframework.https.CertificatesConfigBuilder;
import org.keycloak.testframework.https.InjectCertificates;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.tests.cli.exec.AbstractExec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public abstract class AbstractCliTest {

    // Enables TLS on the Keycloak server (https://localhost:8443) while HTTP stays available on localhost
    @InjectCertificates(config = TlsEnabledConfig.class)
    ManagedCertificates managedCertificates;

    @InjectKeycloakUrls
    protected KeycloakUrls keycloakUrls;

    @InjectAdminClient
    protected Keycloak adminClient;

    @InjectCryptoHelper
    protected CryptoHelper cryptoHelper;

    protected String serverUrl;

    @BeforeEach
    public void initServerUrl() {
        serverUrl = getHttpBaseUrl();
    }

    // TODO replace hardcoded port with server API once https://github.com/keycloak/keycloak/issues/48089 is resolved
    private String getHttpBaseUrl() {
        try {
            return new URL("http", new URL(keycloakUrls.getBase()).getHost(), 8080, "").toExternalForm();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    protected static void assertExitCodeAndStdErrSize(AbstractExec exe, int exitCode, int stdErrLineCount) {
        assertExitCodeAndStreamSizes(exe, exitCode, -1, stdErrLineCount);
    }

    protected static void assertExitCodeAndStreamSizes(AbstractExec exe, int exitCode, int stdOutLineCount, int stdErrLineCount) {
        Assertions.assertEquals(exitCode, exe.exitCode(), "exitCode == " + exitCode);
        if (stdOutLineCount != -1) {
            assertLineCount("STDOUT: " + exe.stdoutString(), exe.stdoutLines(), stdOutLineCount);
        }
        // There is additional logging in case that BC FIPS libraries are used, so the count of logged lines don't match with the case with plain BC used
        // Hence we test count of lines just with FIPS disabled
        if (stdErrLineCount != -1 && isFipsDisabled()) {
            assertLineCount("STDERR: " + exe.stderrString(), exe.stderrLines(), stdErrLineCount);
        }
    }

    private static void assertLineCount(String label, List<String> lines, int count) {
        if (lines.size() == count) {
            return;
        }
        // there is some kind of race condition in 'kcreg' that results in intermittent extra empty line
        if (lines.size() == count + 1) {
            if ("".equals(lines.get(lines.size()-1))) {
                return;
            }
        }
        Assertions.assertEquals(lines.size(), count, label + " has " + lines.size() + " lines (expected: " + count + ")");
    }

    private static boolean isFipsDisabled() {
        return !AbstractExec.isFips();
    }

    /**
     * Removes a realm created on the fly through the CLI, so it does not leak into other test methods or classes.
     */
    protected void removeRealmIfExists(String realm) {
        try {
            adminClient.realm(realm).remove();
        } catch (NotFoundException e) {
            // already removed
        }
    }

    protected void assumeKeystoreTypeSupported(KeystoreUtil.KeystoreFormat keystoreType) {
        List<String> supportedKeystoreTypes = Arrays.asList(cryptoHelper.getExpectedSupportedKeyStoreTypes());
        Assumptions.assumeTrue(supportedKeystoreTypes.contains(keystoreType.name()),
                "Keystore type '" + keystoreType + "' not supported. Supported keystore types: " + supportedKeystoreTypes);
    }

    protected static File resourceFile(String path) {
        URL resource = AbstractCliTest.class.getClassLoader().getResource(path);
        Assertions.assertNotNull(resource, "Cannot load resource from path: " + path);
        return new File(resource.getFile());
    }

    /**
     * Points the client tools at the FIPS-enabled copy built into {@code target/containers-fips} (which bundles the
     * BouncyCastle FIPS provider) so the external kcadm/kcreg processes run under FIPS. Invoked from the FIPS test
     * suites before any test runs; non-FIPS runs use the default client tools, which keep the non-FIPS provider.
     */
    public static void useFipsClientTools() {
        System.setProperty(AbstractExec.CLI_TOOLS_DIR_PROPERTY,
                System.getProperty("user.dir") + "/target/containers-fips/keycloak-client-tools");
    }

    static class TlsEnabledConfig implements CertificatesConfig {

        @Override
        public CertificatesConfigBuilder configure(CertificatesConfigBuilder config) {
            return config.tlsEnabled(true);
        }
    }

}
