package org.keycloak.testsuite.docker;

import java.io.File;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.keycloak.common.Profile;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_PORT_HTTP;
import static org.keycloak.testsuite.util.WaitUtils.pause;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeTrue;

@EnableFeature(Profile.Feature.DOCKER)
public class DockerClientTest extends AbstractKeycloakTest {
    public static final String REALM_ID = "docker-test-realm";
    public static final String CLIENT_ID = "docker-test-client";
    public static final String DOCKER_USER = "docker-user";
    public static final String DOCKER_USER_PASSWORD = "password";

    public static final String REGISTRY_HOSTNAME = "localhost";
    public static final Integer REGISTRY_PORT = 5000;
    public static final String MINIMUM_DOCKER_VERSION = "1.8.0";

    private GenericContainer dockerRegistryContainer = null;
    private GenericContainer dockerClientContainer = null;

    private static String hostIp;

    @BeforeClass
    public static void verifyEnvironment() {
        final Optional<DockerVersion> dockerVersion = new DockerHostVersionSupplier().get();
        assumeTrue("Could not determine docker version for host machine.  It either is not present or accessible to the JVM running the test harness.", dockerVersion.isPresent());
        assumeTrue("Docker client on host machine is not a supported version.  Please upgrade and try again.", DockerVersion.COMPARATOR.compare(dockerVersion.get(), DockerVersion.parseVersionString(MINIMUM_DOCKER_VERSION)) >= 0);

        hostIp = System.getProperty("host.ip");

        if (hostIp == null) {
            final Optional<String> foundHostIp = new DockerHostIpSupplier().get();
            if (foundHostIp.isPresent()) {
                hostIp = foundHostIp.get();
            }
        }
        Assert.assertNotNull("Could not resolve host machine's IP address for docker adapter, and 'host.ip' system poperty not set. Client will not be able to authenticate against the keycloak server!", hostIp);
    }

    @Override
    public void addTestRealms(final List<RealmRepresentation> testRealms) {
        final RealmRepresentation dockerRealm = DockerTestRealmSetup.createRealm(REALM_ID);
        DockerTestRealmSetup.configureDockerRegistryClient(dockerRealm, CLIENT_ID);
        DockerTestRealmSetup.configureUser(dockerRealm, DOCKER_USER, DOCKER_USER_PASSWORD);

        testRealms.add(dockerRealm);
    }

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();

        // find the realm cert
        String realmCert = null;
        List<KeysMetadataRepresentation.KeyMetadataRepresentation> realmKeys = adminClient.realm(REALM_ID).keys().getKeyMetadata().getKeys();
        for (KeysMetadataRepresentation.KeyMetadataRepresentation key : realmKeys) {
            if (Constants.DEFAULT_SIGNATURE_ALGORITHM.equals(key.getAlgorithm()) && KeyStatus.ACTIVE.name().equals(key.getStatus())) {
                if (realmCert != null) {
                     throw new IllegalStateException("More than one public realm cert enabled");
                }
                realmCert = key.getCertificate();
            }
        }
        if (realmCert == null) {
            throw new IllegalStateException("Cannot find public realm cert");
        }

        // save the cert to a file
        File tmpCertFile = File.createTempFile("keycloak-docker-realm-cert-", ".pem");
        tmpCertFile.deleteOnExit();
        PrintWriter tmpCertWriter = new PrintWriter(tmpCertFile);
        tmpCertWriter.println(PemUtils.BEGIN_CERT);
        tmpCertWriter.println(realmCert);
        tmpCertWriter.println(PemUtils.END_CERT);
        tmpCertWriter.close();

        final Map<String, String> environment = new HashMap<>();
        environment.put("REGISTRY_STORAGE_FILESYSTEM_ROOTDIRECTORY", "/tmp");
        environment.put("REGISTRY_AUTH_TOKEN_REALM", "http://" + hostIp + ":" + AUTH_SERVER_PORT_HTTP + "/auth/realms/" + REALM_ID + "/protocol/docker-v2/auth");
        environment.put("REGISTRY_AUTH_TOKEN_SERVICE", CLIENT_ID);
        environment.put("REGISTRY_AUTH_TOKEN_ISSUER", "http://" + hostIp + ":" + AUTH_SERVER_PORT_HTTP + "/auth/realms/" + REALM_ID);
        environment.put("REGISTRY_AUTH_TOKEN_ROOTCERTBUNDLE", "/opt/kc-certs/" + tmpCertFile.getCanonicalFile().getName());
        environment.put("INSECURE_REGISTRY", "--insecure-registry " + REGISTRY_HOSTNAME + ":" + REGISTRY_PORT);

        String dockerioPrefix = Boolean.parseBoolean(System.getProperty("docker.io-prefix-explicit")) ? "docker.io/" : "";

        dockerRegistryContainer = new GenericContainer(dockerioPrefix + "registry:2")
                .withFileSystemBind(tmpCertFile.getCanonicalPath(), "/opt/kc-certs/" + tmpCertFile.getCanonicalFile().getName(), BindMode.READ_ONLY)
                .withEnv(environment)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("dockerRegistryContainer")))
                .withNetworkMode("host")
                .withPrivilegedMode(true);
        dockerRegistryContainer.start();

        dockerClientContainer = new GenericContainer(dockerioPrefix + "docker:dind")
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("dockerClientContainer")))
                .withNetworkMode("host")
                .withPrivilegedMode(true)
                .waitingFor(Wait.forLogMessage(".*API listen on /var/run/docker.sock.*\\n", 1))
                .withStartupTimeout(Duration.ofSeconds(120));
        dockerClientContainer.start();
    }

    @Override
    public void afterAbstractKeycloakTest() throws Exception {
        super.afterAbstractKeycloakTest();

        pause(5000); // wait for the container logs

        dockerClientContainer.close();
        dockerRegistryContainer.close();
    }

    @Test
    public void shouldPerformDockerAuthAgainstRegistry() throws Exception {
        log.info("Starting the attempt for login...");
        Container.ExecResult result = dockerClientContainer.execInContainer("docker", "login", "-u", DOCKER_USER, "-p", DOCKER_USER_PASSWORD, REGISTRY_HOSTNAME + ":" + REGISTRY_PORT);
        printCommandResult(result);
        assertThat("Error performing login", result.getExitCode(), is(0));

        // create a empty Dockerfile in /tmp
        result = dockerClientContainer.execInContainer("sh", "-c", "echo -e \"FROM scratch\\nWORKDIR /\" > /tmp/Dockerfile");
        printCommandResult(result);
        assertThat("Error creating dockerfile for empty image", result.getExitCode(), is(0));

        // build the empty image
        result = dockerClientContainer.execInContainer("docker", "build", "--tag", REGISTRY_HOSTNAME + ":" + REGISTRY_PORT + "/empty", "/tmp");
        printCommandResult(result);
        assertThat("Error building empty image", result.getExitCode(), is(0));

        // push the image
        result = dockerClientContainer.execInContainer("docker", "push", REGISTRY_HOSTNAME + ":" + REGISTRY_PORT + "/empty");
        printCommandResult(result);
        assertThat("Error pushing to registry", result.getExitCode(), is(0));
    }

    private void printCommandResult(Container.ExecResult result) {
        log.infof("Command executed with exit code %d. Output follows:\nSTDOUT: %s\n---\nSTDERR: %s",
                result.getExitCode(), result.getStdout(), result.getStderr());
    }
}
