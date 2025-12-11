package org.keycloak.testsuite.cli.admin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.client.cli.config.ConfigData;
import org.keycloak.client.cli.config.FileConfigHandler;
import org.keycloak.client.cli.config.RealmConfigData;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.cli.AbstractCliTest;
import org.keycloak.testsuite.cli.KcAdmExec;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;

import org.junit.Assert;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.cli.KcAdmExec.WORK_DIR;
import static org.keycloak.testsuite.cli.KcAdmExec.execute;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public abstract class AbstractAdmCliTest extends AbstractCliTest {

    static boolean runIntermittentlyFailingTests() {
        return "true".equals(System.getProperty("test.intermittent"));
    }

    static File getDefaultConfigFilePath() {
        return new File(System.getProperty("user.home") + "/.keycloak/kcadm.config");
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realmRepresentation);

        // create admin user account with permissions to manage clients
        UserRepresentation admin = UserBuilder.create()
                .username("user1")
                .password("userpass")
                .enabled(true)
                .build();
        HashMap<String, List<String>> clientRoles = new HashMap<>();
        clientRoles.put("realm-management", Arrays.asList("realm-admin"));
        admin.setClientRoles(clientRoles);
        realmRepresentation.getUsers().add(admin);



        // create client with service account to use Signed JWT credentials with
        ClientRepresentation regClient = ClientBuilder.create()
                .clientId("admin-cli-jwt")
                .attribute(JWTClientAuthenticator.CERTIFICATE_ATTR, "MIICnTCCAYUCBgFXUhpRTTANBgkqhkiG9w0BAQsFADASMRAwDgYDVQQDDAdyZWctY2xpMB4XDTE2MDkyMjEzMzIxOFoXDTI2MDkyMjEzMzM1OFowEjEQMA4GA1UEAwwHcmVnLWNsaTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMHZn/0Bk1M9oKcTHxzn2cGvBWwO1m6OVLQ8LSVwNIf4ixfGkVIkhI5iEGYND+uD8ame54ZPClTVxMra3JldClLIG+L+ymnbT2vKIhEsVvCROs9PnYxbFALt1dXneLIio2uzF+d7/zQWlmeaWfNunSJT1aHNJDkGgDeUuQa25b0IMqsFjsN8Dg4ATkA97r3wKn4Tp3SE7sTM/B2pmra4atNxGeShVrgihqUiQ/PwDiDGwry64AsexkZnQsCR3bJWBAVUiHef3JWzTfWWN5bfCBG6Mnq1xw7YN+YpV1nR3CGmcKJuLe6aTe7Ps8hYejYiQA7Mp7ZQsoImsVFV5HDOlb0CAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAZl8XvLfKXTPYvq/QyHOg7EDlAdlV3HkmHP9SBAV4BccmHmorMkm5I6I21UA5mfju+0nhbEd0bm0kvJFxIfNU6lJyyVvQx3Gns37KYUOzIV/ocWZuOTBLp5tfIBYbBwfE/s1J4PhpA/3WhBY9JKiLvdJfxECGIgaLs2M0UsylW/7o04+18Od8j/m7crQc7fpe5gJB5m/+hxUDowIjG5CumffX9OHYGDvHBpaUl7QNSGgjP8Bn9ogmIMUBJ7XSYUcohKuk2Cnj6p+GlLuqHbOISUXLVjf0DxhCu6diVxvacKbgAZmyCIO1tGL/UVRxg9GOYdCiC9vHfPuZ8US+ZB0P9g==")
                .authenticatorType(JWTClientAuthenticator.PROVIDER_ID)
                .serviceAccount()
                .build();

        realmRepresentation.getClients().add(regClient);

        // create service account for client reg-cli with permissions to manage clients
        addServiceAccount(realmRepresentation, "admin-cli-jwt");



        // create client to use with user account - enable direct grants
        regClient = ClientBuilder.create()
                .clientId("admin-cli-jwt-direct")
                .attribute(JWTClientAuthenticator.CERTIFICATE_ATTR, "MIICnTCCAYUCBgFXUhpRTTANBgkqhkiG9w0BAQsFADASMRAwDgYDVQQDDAdyZWctY2xpMB4XDTE2MDkyMjEzMzIxOFoXDTI2MDkyMjEzMzM1OFowEjEQMA4GA1UEAwwHcmVnLWNsaTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMHZn/0Bk1M9oKcTHxzn2cGvBWwO1m6OVLQ8LSVwNIf4ixfGkVIkhI5iEGYND+uD8ame54ZPClTVxMra3JldClLIG+L+ymnbT2vKIhEsVvCROs9PnYxbFALt1dXneLIio2uzF+d7/zQWlmeaWfNunSJT1aHNJDkGgDeUuQa25b0IMqsFjsN8Dg4ATkA97r3wKn4Tp3SE7sTM/B2pmra4atNxGeShVrgihqUiQ/PwDiDGwry64AsexkZnQsCR3bJWBAVUiHef3JWzTfWWN5bfCBG6Mnq1xw7YN+YpV1nR3CGmcKJuLe6aTe7Ps8hYejYiQA7Mp7ZQsoImsVFV5HDOlb0CAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAZl8XvLfKXTPYvq/QyHOg7EDlAdlV3HkmHP9SBAV4BccmHmorMkm5I6I21UA5mfju+0nhbEd0bm0kvJFxIfNU6lJyyVvQx3Gns37KYUOzIV/ocWZuOTBLp5tfIBYbBwfE/s1J4PhpA/3WhBY9JKiLvdJfxECGIgaLs2M0UsylW/7o04+18Od8j/m7crQc7fpe5gJB5m/+hxUDowIjG5CumffX9OHYGDvHBpaUl7QNSGgjP8Bn9ogmIMUBJ7XSYUcohKuk2Cnj6p+GlLuqHbOISUXLVjf0DxhCu6diVxvacKbgAZmyCIO1tGL/UVRxg9GOYdCiC9vHfPuZ8US+ZB0P9g==")
                .authenticatorType(JWTClientAuthenticator.PROVIDER_ID)
                .directAccessGrants()
                .build();

        realmRepresentation.getClients().add(regClient);




        // create client with service account to use client secret with
        regClient = ClientBuilder.create()
                .clientId("admin-cli-secret")
                .secret("password")
                .authenticatorType(ClientIdAndSecretAuthenticator.PROVIDER_ID)
                .serviceAccount()
                .build();

        realmRepresentation.getClients().add(regClient);

        // create service account for client reg-cli with permissions to manage clients
        addServiceAccount(realmRepresentation, "admin-cli-secret");




        // create client to use with user account - enable direct grants
        regClient = ClientBuilder.create()
                .clientId("admin-cli-secret-direct")
                .secret("password")
                .authenticatorType(ClientIdAndSecretAuthenticator.PROVIDER_ID)
                .directAccessGrants()
                .build();

        realmRepresentation.getClients().add(regClient);

    }

    FileConfigHandler initCustomConfigFile() {
        String filename = UUID.randomUUID().toString() + ".config";
        File cfgFile = new File(WORK_DIR + "/" + filename);
        FileConfigHandler handler = new FileConfigHandler();
        handler.setConfigFile(cfgFile.getAbsolutePath());
        return handler;
    }

    void assertFieldsEqualWithExclusions(ConfigData config1, ConfigData config2, String ... excluded) {

        HashSet<String> exclusions = new HashSet<>(Arrays.asList(excluded));

        if (!exclusions.contains("serverUrl")) {
            Assert.assertEquals("serverUrl", config1.getServerUrl(), config2.getServerUrl());
        }
        if (!exclusions.contains("realm")) {
            Assert.assertEquals("realm", config1.getRealm(), config2.getRealm());
        }
        if (!exclusions.contains("truststore")) {
            Assert.assertEquals("truststore", config1.getTruststore(), config2.getTruststore());
        }
        if (!exclusions.contains("endpoints")) {
            Map<String, Map<String, RealmConfigData>> endp1 = config1.getEndpoints();
            Map<String, Map<String, RealmConfigData>> endp2 = config2.getEndpoints();

            Iterator<Map.Entry<String, Map<String, RealmConfigData>>> it1 = endp1.entrySet().iterator();
            Iterator<Map.Entry<String, Map<String, RealmConfigData>>> it2 = endp2.entrySet().iterator();

            while (it1.hasNext()) {
                Map.Entry<String, Map<String, RealmConfigData>> ent1 = it1.next();
                Map.Entry<String, Map<String, RealmConfigData>> ent2 = it2.next();

                String serverUrl = ent1.getKey();
                String endpskey = "endpoints." + serverUrl;
                if (!exclusions.contains(endpskey)) {
                    Assert.assertEquals(endpskey, ent1.getKey(), ent2.getKey());

                    Map<String, RealmConfigData> realms1 = ent1.getValue();
                    Map<String, RealmConfigData> realms2 = ent2.getValue();

                    Iterator<Map.Entry<String, RealmConfigData>> rit1 = realms1.entrySet().iterator();
                    Iterator<Map.Entry<String, RealmConfigData>> rit2 = realms2.entrySet().iterator();

                    while (rit1.hasNext()) {
                        Map.Entry<String, RealmConfigData> rent1 = rit1.next();
                        Map.Entry<String, RealmConfigData> rent2 = rit2.next();

                        String realm = rent1.getKey();
                        String rkey = endpskey + "." + realm;
                        if (!exclusions.contains(endpskey)) {
                            Assert.assertEquals(rkey, rent1.getKey(), rent2.getKey());

                            RealmConfigData rdata1 = rent1.getValue();
                            RealmConfigData rdata2 = rent2.getValue();

                            assertFieldsEqualWithExclusions(serverUrl, realm, rdata1, rdata2, excluded);
                        }
                    }
                }
            }
        }
    }

    void assertFieldsEqualWithExclusions(String server, String realm, RealmConfigData data1, RealmConfigData data2, String ... excluded) {

        HashSet<String> exclusions = new HashSet<>(Arrays.asList(excluded));

        String pfix = "";
        if (server != null || realm != null) {
            pfix = "endpoints." + server + "." + realm + ".";
        }

        String ekey = pfix + "serverUrl";
        if (!exclusions.contains(ekey)) {
            Assert.assertEquals(ekey, data1.serverUrl(), data2.serverUrl());
        }

        ekey = pfix + "realm";
        if (!exclusions.contains(ekey)) {
            Assert.assertEquals(ekey, data1.realm(), data2.realm());
        }

        ekey = pfix + "clientId";
        if (!exclusions.contains(ekey)) {
            Assert.assertEquals(ekey, data1.getClientId(), data2.getClientId());
        }

        ekey = pfix + "token";
        if (!exclusions.contains(ekey)) {
            Assert.assertEquals(ekey, data1.getToken(), data2.getToken());
        }

        ekey = pfix + "refreshToken";
        if (!exclusions.contains(ekey)) {
            Assert.assertEquals(ekey, data1.getRefreshToken(), data2.getRefreshToken());
        }

        ekey = pfix + "expiresAt";
        if (!exclusions.contains(ekey)) {
            Assert.assertEquals(ekey, data1.getExpiresAt(), data2.getExpiresAt());
        }

        ekey = pfix + "refreshExpiresAt";
        if (!exclusions.contains(ekey)) {
            Assert.assertEquals(ekey, data1.getRefreshExpiresAt(), data2.getRefreshExpiresAt());
        }

        ekey = pfix + "secret";
        if (!exclusions.contains(ekey)) {
            Assert.assertEquals(ekey, data1.getSecret(), data2.getSecret());
        }

        ekey = pfix + "signingToken";
        if (!exclusions.contains(ekey)) {
            Assert.assertEquals(ekey, data1.getSigningToken(), data2.getSigningToken());
        }

        ekey = pfix + "sigExpiresAt";
        if (!exclusions.contains(ekey)) {
            Assert.assertEquals(ekey, data1.getSigExpiresAt(), data2.getSigExpiresAt());
        }
    }

    void testCRUDWithOnTheFlyAuth(String serverUrl, String credentials, String extraOptions, String loginMessage) throws IOException {

        File configFile = getDefaultConfigFilePath();
        long lastModified = configFile.exists() ? configFile.lastModified() : 0;

        // This test assumes it is the only user of any instance of on the system
        KcAdmExec exe = execute("create clients --no-config --server " + serverUrl +
                " --realm test " + credentials + " " + extraOptions + " -s clientId=test-client -o");

        Assert.assertEquals("exitCode == 0", 0, exe.exitCode());
        Assert.assertTrue("login message expected. But the messages are: " + exe.stderrLines(), exe.stderrLines().stream().anyMatch(message -> message.equals(loginMessage)));

        ClientRepresentation client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
        Assert.assertEquals("clientId", "test-client", client.getClientId());

        long lastModified2 = configFile.exists() ? configFile.lastModified() : 0;
        Assert.assertEquals("config file not modified", lastModified, lastModified2);




        exe = execute("get clients/" + client.getId() + " --no-config --server " + serverUrl + " --realm test " + credentials + " " + extraOptions);

        assertExitCodeAndStdErrSize(exe, 0, 1);

        ClientRepresentation client2 = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
        Assert.assertEquals("clientId", "test-client", client2.getClientId());

        lastModified2 = configFile.exists() ? configFile.lastModified() : 0;
        Assert.assertEquals("config file not modified", lastModified, lastModified2);




        exe = execute("update clients/" + client.getId() + " --no-config --server " + serverUrl + " --realm test " +
                credentials + " " + extraOptions + " -s enabled=false -o");

        assertExitCodeAndStdErrSize(exe, 0, 1);

        ClientRepresentation client4 = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
        Assert.assertEquals("clientId", "test-client", client4.getClientId());
        Assert.assertFalse("enabled", client4.isEnabled());

        lastModified2 = configFile.exists() ? configFile.lastModified() : 0;
        Assert.assertEquals("config file not modified", lastModified, lastModified2);




        exe = execute("delete clients/" + client.getId() + " --no-config --server " + serverUrl + " --realm test " + credentials + " " + extraOptions);

        int linecountOffset = "".equals(loginMessage) ? 1 : 0; // if there is no login, then there is one less stdErrLinecount
        assertExitCodeAndStreamSizes(exe, 0, 0, 1 - linecountOffset);

        lastModified2 = configFile.exists() ? configFile.lastModified() : 0;
        Assert.assertEquals("config file not modified", lastModified, lastModified2);




        // subsequent delete should fail
        exe = execute("delete clients/" + client.getId() + " --no-config --server " + serverUrl + " --realm test " + credentials + " " + extraOptions);

        assertExitCodeAndStreamSizes(exe, 1, 0, 2 - linecountOffset);
        String resourceUri = serverUrl + "/admin/realms/test/clients/" + client.getId();
        Assert.assertEquals("error message", "Resource not found for url: " + resourceUri, exe.stderrLines().get(exe.stderrLines().size() - 1));

        lastModified2 = configFile.exists() ? configFile.lastModified() : 0;
        Assert.assertEquals("config file not modified", lastModified, lastModified2);
    }

    File initTempFile(String extension) throws IOException {
        return initTempFile(extension, null);
    }

    File initTempFile(String extension, String content) throws IOException {
        String filename = UUID.randomUUID().toString() + extension;
        File file = new File(KcAdmExec.WORK_DIR + "/" + filename);
        if (content != null) {
            OutputStream os = new FileOutputStream(file);
            os.write(content.getBytes(Charset.forName("iso_8859_1")));
            os.close();
        }
        return file;
    }

    void addServiceAccount(RealmRepresentation realm, String clientId) {

        UserRepresentation account = UserBuilder.create()
                .username("service-account-" + clientId)
                .enabled(true)
                .serviceAccountId(clientId)
                .build();

        HashMap<String, List<String>> clientRoles = new HashMap<>();
        clientRoles.put("realm-management", Arrays.asList("realm-admin"));

        account.setClientRoles(clientRoles);

        realm.getUsers().add(account);
    }

    void loginAsUser(File configFile, String server, String realm, String user, String password, boolean envPassword) {
        KcAdmExec exe = KcAdmExec.newBuilder()
                .argsLine("config credentials --server " + server + " --realm " + realm +
                        " --user " + user + (envPassword ? "" : (" --password " + password)) + " --config " + configFile.getAbsolutePath())
                .env(envPassword ? "KC_CLI_PASSWORD=" + password : null)
                .execute();

        assertExitCodeAndStreamSizes(exe, 0, 0, 1);
    }

    void loginAsUser(File configFile, String server, String realm, String user, String password) {
        loginAsUser(configFile, server, realm, user, password, false);
    }

}
