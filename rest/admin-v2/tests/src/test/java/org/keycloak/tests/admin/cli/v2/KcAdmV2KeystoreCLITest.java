package org.keycloak.tests.admin.cli.v2;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.client.admin.cli.KcAdmMain;
import org.keycloak.client.admin.cli.v2.KcAdmV2Cmd;
import org.keycloak.client.cli.common.Globals;
import org.keycloak.client.cli.util.HttpUtil;
import org.keycloak.common.Profile;
import org.keycloak.common.util.PemUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

@KeycloakIntegrationTest(config = KcAdmV2KeystoreCLITest.V2ApiTlsServerConfig.class)
public class KcAdmV2KeystoreCLITest {

    private static final String JWT_CLIENT_ID = "admin-cli-jwt-test";
    private static final String KEY_ALIAS = "client-key";

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectCertificates(config = TlsEnabledConfig.class)
    ManagedCertificates managedCertificates;

    @TempDir
    static File tempDir;

    private static String configFilePath;

    @TestSetup
    public void login() {
        configFilePath = new File(tempDir, "kcadm.config").getAbsolutePath();
        HttpUtil.clearHttpClient();

        CommandResult result = kcAdmV2Cmd("config", "credentials",
                "--server", keycloakUrls.getBase(),
                "--realm", "master",
                "--client", Config.getAdminClientId(),
                "--secret", Config.getAdminClientSecret(),
                "--truststore", getClientTruststorePath(),
                "--trustpass", getStorePassword());

        assertThat("login should succeed: " + result.err(), result.exitCode(), is(0));

        setupJwtClient();
    }

    @BeforeEach
    void clearHttpClientBeforeEach() {
        // we need to reset the HTTP client as we switch the keystore in test methods
        HttpUtil.clearHttpClient();
    }

    @Test
    void testKeystoreAuthWithServiceAccount() {
        CommandResult result = kcAdmV2CmdRaw("client", "list", "-c",
                "--no-config",
                "--server", keycloakUrls.getBase(),
                "--client", JWT_CLIENT_ID,
                "--keystore", getClientKeystorePath(),
                "--storepass", getStorePassword(),
                "--alias", KEY_ALIAS,
                "--truststore", getClientTruststorePath(),
                "--trustpass", getStorePassword());

        assertThat("keystore auth should succeed: " + result.err(),
                result.exitCode(), is(0));
        assertThat(result.out(), containsString("clientId"));
        assertThat("should return a JSON array", result.out().trim(), startsWith("["));
    }

    @Test
    void testWithoutKeystoreFails() {
        // Same as testKeystoreAuthWithServiceAccount but without --keystore — proves keystore is required
        CommandResult result = kcAdmV2CmdRaw("client", "list", "-c",
                "--no-config",
                "--server", keycloakUrls.getBase(),
                "--client", JWT_CLIENT_ID,
                "--truststore", getClientTruststorePath(),
                "--trustpass", getStorePassword());

        assertThat("should fail without keystore", result.exitCode(), is(not(0)));
    }

    @Test
    void testKeystoreAuthWithWrongAlias() {
        CommandResult result = kcAdmV2CmdRaw("client", "list", "-c",
                "--no-config",
                "--server", keycloakUrls.getBase(),
                "--client", JWT_CLIENT_ID,
                "--keystore", getClientKeystorePath(),
                "--storepass", getStorePassword(),
                "--alias", "wrong-alias",
                "--truststore", getClientTruststorePath(),
                "--trustpass", getStorePassword());

        assertThat("wrong alias should fail", result.exitCode(), is(not(0)));
    }

    private void setupJwtClient() {
        var existing = adminClient.realm("master").clients().findByClientId(JWT_CLIENT_ID);
        if (!existing.isEmpty()) {
            return;
        }

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(JWT_CLIENT_ID);
        client.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
        client.setServiceAccountsEnabled(true);
        client.setDirectAccessGrantsEnabled(false);
        client.setPublicClient(false);

        var response = adminClient.realm("master").clients().create(client);
        String clientUuid = response.getLocation().getPath()
                .substring(response.getLocation().getPath().lastIndexOf('/') + 1);
        response.close();

        var serviceAccountUser = adminClient.realm("master").clients()
                .get(clientUuid).getServiceAccountUser();
        var adminRole = adminClient.realm("master").roles().get("admin").toRepresentation();
        adminClient.realm("master").users().get(serviceAccountUser.getId())
                .roles().realmLevel().add(java.util.List.of(adminRole));

        try {
            String keystorePath = getClientKeystorePath();
            KeyStore ks = KeyStore.getInstance("JKS");
            try (var fis = new FileInputStream(keystorePath)) {
                ks.load(fis, getStorePassword().toCharArray());
            }
            X509Certificate cert = (X509Certificate) ks.getCertificate(KEY_ALIAS);

            ClientRepresentation updatedClient = adminClient.realm("master").clients()
                    .get(clientUuid).toRepresentation();
            if (updatedClient.getAttributes() == null) {
                updatedClient.setAttributes(new java.util.HashMap<>());
            }
            updatedClient.getAttributes().put("jwt.credential.certificate",
                    PemUtils.encodeCertificate(cert));
            adminClient.realm("master").clients().get(clientUuid).update(updatedClient);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set client certificate", e);
        }
    }

    private String getClientKeystorePath() {
        // TODO: we should skip to the test framework methods once they are introduced
        String serverPath = managedCertificates.getServerKeyStorePath();
        return serverPath.replace("server-keystore", "client-keystore");
    }

    private String getClientTruststorePath() {
        // TODO: we should skip to the test framework methods once they are introduced
        String serverPath = managedCertificates.getServerKeyStorePath();
        return serverPath.replace("server-keystore", "client-truststore");
    }

    private String getStorePassword() {
        return managedCertificates.getServerKeyStorePassword();
    }

    private CommandResult kcAdmV2CmdRaw(String... args) {
        CommandLine cli = Globals.createCommandLine(new KcAdmV2Cmd(), KcAdmMain.CMD, new PrintWriter(System.err, true));

        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        cli.setOut(new PrintWriter(out));
        cli.setErr(new PrintWriter(err));

        int exitCode = cli.execute(args);
        return new CommandResult(exitCode, out.toString(), err.toString());
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
