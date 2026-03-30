package org.keycloak.tests.admin.cli.v2;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.keycloak.client.admin.cli.KcAdmMain;
import org.keycloak.client.admin.cli.v2.KcAdmV2Cmd;
import org.keycloak.client.cli.common.Globals;
import org.keycloak.client.cli.util.HttpUtil;
import org.keycloak.common.Profile;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.https.CertificatesConfig;
import org.keycloak.testframework.https.CertificatesConfigBuilder;
import org.keycloak.testframework.https.InjectCertificates;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

@KeycloakIntegrationTest(config = KcAdmV2TlsCLITest.V2ApiTlsServerConfig.class)
public class KcAdmV2TlsCLITest {

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectCertificates(config = TlsEnabledConfig.class)
    ManagedCertificates managedCertificates;

    @TempDir
    static File tempDir;

    private static String configFilePath;

    @TestSetup
    public void login() {
        configFilePath = new File(tempDir, "kcadm.config").getAbsolutePath();
        HttpUtil.clearHttpClient();

        String truststorePath = getClientTruststorePath();
        Assertions.assertTrue(new File(truststorePath).isFile(),
                "Client truststore should exist at: " + truststorePath);

        CommandResult result = kcAdmV2Cmd("config", "credentials",
                "--server", keycloakUrls.getBase(),
                "--realm", "master",
                "--client", Config.getAdminClientId(),
                "--secret", Config.getAdminClientSecret(),
                "--truststore", truststorePath,
                "--trustpass", getStorePassword());

        assertThat("login should succeed: " + result.err(), result.exitCode(), is(0));
    }

    @BeforeEach
    void clearHttpClientBeforeEach() {
        // we want a new client as we change a client truststore for each test
        HttpUtil.clearHttpClient();
    }

    @Test
    void testWithoutTruststoreFails() {
        CommandResult result = kcAdmV2Cmd("client", "list", "-c");

        assertThat("should fail without truststore on HTTPS server",
                result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("PKIX path building failed"));
    }

    @Test
    void testTruststoreWithCorrectPassword() {
        CommandResult result = kcAdmV2Cmd("client", "list", "-c",
                "--truststore", getClientTruststorePath(),
                "--trustpass", getStorePassword());

        assertThat("list with truststore should succeed: " + result.err(),
                result.exitCode(), is(0));
        assertThat(result.out(), containsString("clientId"));
        assertThat("should return a JSON array", result.out().trim(), startsWith("["));
    }

    @Test
    void testTruststoreWithWrongPassword() {
        CommandResult result = kcAdmV2Cmd("client", "list", "-c",
                "--truststore", getClientTruststorePath(),
                "--trustpass", "wrong-password");

        assertThat("wrong truststore password should fail", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("Failed to load truststore"));
    }

    @Test
    void testTruststoreWithNonExistentFile() {
        CommandResult result = kcAdmV2Cmd("client", "list", "-c",
                "--truststore", "/nonexistent/truststore.jks",
                "--trustpass", "password");

        assertThat("non-existent truststore should fail", result.exitCode(), is(not(0)));
        assertThat(result.err(), containsString("Failed to load truststore"));
    }

    @Test
    void testInsecureSkipsTlsValidation() {
        CommandResult result = kcAdmV2Cmd("client", "list", "-c", "--insecure");

        assertThat("--insecure should allow connection without truststore: " + result.err(),
                result.exitCode(), is(0));
        assertThat(result.out(), containsString("clientId"));
    }

    private String getClientTruststorePath() {
        // Derive client truststore path from the server keystore path which follows the same
        // naming convention: both are in the same directory with predictable names
        // TODO: the new test framework should expose the client truststore, once that is done, we need to switch to it
        String serverPath = managedCertificates.getServerKeyStorePath();
        return serverPath.replace("server-keystore", "client-truststore");
    }

    private String getStorePassword() {
        return managedCertificates.getServerKeyStorePassword();
    }

    private CommandResult kcAdmV2Cmd(String... args) {
        CommandLine cli = Globals.createCommandLine(new KcAdmV2Cmd(), KcAdmMain.CMD, new PrintWriter(System.err, true));

        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        cli.setOut(new PrintWriter(out));
        cli.setErr(new PrintWriter(err));

        String[] fullArgs = new String[args.length + 2];
        System.arraycopy(args, 0, fullArgs, 0, args.length);
        fullArgs[args.length] = "--config";
        fullArgs[args.length + 1] = configFilePath;

        int exitCode = cli.execute(fullArgs);
        return new CommandResult(exitCode, out.toString(), err.toString());
    }

    record CommandResult(int exitCode, String out, String err) {
    }

    private static class TlsEnabledConfig implements CertificatesConfig {
        @Override
        public CertificatesConfigBuilder configure(CertificatesConfigBuilder config) {
            return config.tlsEnabled(true);
        }
    }

    public static class V2ApiTlsServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_ADMIN_API_V2);
        }
    }
}
