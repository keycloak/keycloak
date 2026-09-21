package org.keycloak.tests.docker;

import java.io.File;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Profile;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.Constants;
import org.keycloak.protocol.docker.DockerAuthV2Protocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.utils.admin.AdminApiUtil;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@KeycloakIntegrationTest(config = DockerClientTest.DockerServerConfig.class)
public class DockerClientTest {

    private static final Logger LOG = Logger.getLogger(DockerClientTest.class);

    public static final String REALM_ID = "docker-test-realm";
    public static final String CLIENT_ID = "docker-test-client";
    public static final String DOCKER_USER = "docker-user";
    public static final String DOCKER_USER_PASSWORD = "password";

    public static final String REGISTRY_HOSTNAME = "localhost";
    public static final Integer REGISTRY_PORT = 5000;
    public static final String MINIMUM_DOCKER_VERSION = "1.8.0";

    @InjectRealm(config = DockerRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectEvents
    Events events;

    private GenericContainer<?> dockerRegistryContainer;
    private GenericContainer<?> dockerClientContainer;

    @BeforeAll
    public static void verifyEnvironment() {
        final Optional<DockerVersion> dockerVersion = new DockerHostVersionSupplier().get();
        Assumptions.assumeTrue(dockerVersion.isPresent(),
                "Could not determine docker version for host machine. It either is not present or accessible to the JVM running the test harness.");
        Assumptions.assumeTrue(DockerVersion.COMPARATOR.compare(dockerVersion.get(), DockerVersion.parseVersionString(MINIMUM_DOCKER_VERSION)) >= 0,
                "Docker client on host machine is not a supported version. Please upgrade and try again.");
    }

    @BeforeEach
    public void beforeDockerClientTest() throws Exception {
        String realmCert = null;
        List<KeysMetadataRepresentation.KeyMetadataRepresentation> realmKeys = managedRealm.admin().keys().getKeyMetadata().getKeys();
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

        File tmpCertFile = File.createTempFile("keycloak-docker-realm-cert-", ".pem");
        tmpCertFile.deleteOnExit();
        try (PrintWriter tmpCertWriter = new PrintWriter(tmpCertFile)) {
            tmpCertWriter.println(PemUtils.BEGIN_CERT);
            tmpCertWriter.println(realmCert);
            tmpCertWriter.println(PemUtils.END_CERT);
        }

        String realmBaseUrl = managedRealm.getBaseUrl();

        final Map<String, String> environment = new HashMap<>();
        environment.put("REGISTRY_STORAGE_FILESYSTEM_ROOTDIRECTORY", "/tmp");
        environment.put("REGISTRY_AUTH_TOKEN_REALM", realmBaseUrl + "/protocol/docker-v2/auth");
        environment.put("REGISTRY_AUTH_TOKEN_SERVICE", CLIENT_ID);
        environment.put("REGISTRY_AUTH_TOKEN_ISSUER", realmBaseUrl);
        environment.put("REGISTRY_AUTH_TOKEN_ROOTCERTBUNDLE", "/opt/kc-certs/" + tmpCertFile.getCanonicalFile().getName());
        environment.put("INSECURE_REGISTRY", "--insecure-registry " + REGISTRY_HOSTNAME + ":" + REGISTRY_PORT);

        String dockerioPrefix = Boolean.parseBoolean(System.getProperty("docker.io-prefix-explicit")) ? "docker.io/" : "";

        dockerRegistryContainer = new GenericContainer<>(dockerioPrefix + "registry:2")
                .withFileSystemBind(tmpCertFile.getCanonicalPath(), "/opt/kc-certs/" + tmpCertFile.getCanonicalFile().getName(), BindMode.READ_ONLY)
                .withEnv(environment)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("dockerRegistryContainer")))
                .withNetworkMode("host")
                .withPrivilegedMode(true);
        dockerRegistryContainer.start();

        dockerClientContainer = new GenericContainer<>(dockerioPrefix + "docker:dind")
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("dockerClientContainer")))
                .withNetworkMode("host")
                .withPrivilegedMode(true)
                .waitingFor(Wait.forLogMessage(".*API listen on /var/run/docker.sock.*\\n", 1))
                .withStartupTimeout(Duration.ofSeconds(120));
        dockerClientContainer.start();

        events.clear();
    }

    @AfterEach
    public void afterDockerClientTest() throws Exception {
        Thread.sleep(5000); // wait for the container logs

        if (dockerClientContainer != null) {
            dockerClientContainer.close();
        }
        if (dockerRegistryContainer != null) {
            dockerRegistryContainer.close();
        }
    }

    @Test
    public void shouldPerformDockerAuthAgainstRegistry() throws Exception {
        UserRepresentation dockerUser = AdminApiUtil.findUserByUsername(managedRealm.admin(), DOCKER_USER);

        LOG.info("Starting the attempt for login...");
        Container.ExecResult result = dockerClientContainer.execInContainer("docker", "login", "-u", DOCKER_USER, "-p", DOCKER_USER_PASSWORD, REGISTRY_HOSTNAME + ":" + REGISTRY_PORT);
        printCommandResult(result);
        assertThat("Error performing login", result.getExitCode(), is(0));
        assertLogin(dockerUser);

        result = dockerClientContainer.execInContainer("sh", "-c", "echo -e \"FROM scratch\\nWORKDIR /\" > /tmp/Dockerfile");
        printCommandResult(result);
        assertThat("Error creating dockerfile for empty image", result.getExitCode(), is(0));

        result = dockerClientContainer.execInContainer("docker", "build", "--tag", REGISTRY_HOSTNAME + ":" + REGISTRY_PORT + "/empty", "/tmp");
        printCommandResult(result);
        assertThat("Error building empty image", result.getExitCode(), is(0));

        result = dockerClientContainer.execInContainer("docker", "push", REGISTRY_HOSTNAME + ":" + REGISTRY_PORT + "/empty");
        printCommandResult(result);
        assertThat("Error pushing to registry", result.getExitCode(), is(0));
        assertLogin(dockerUser);

        result = dockerClientContainer.execInContainer("docker", "logout");
        printCommandResult(result);
        assertThat("Error performing logout", result.getExitCode(), is(0));
        assertLogin(dockerUser);

        ClientResource client = AdminApiUtil.findClientByClientId(managedRealm.admin(), CLIENT_ID);
        ClientRepresentation clientRep = client.toRepresentation();
        clientRep.setEnabled(Boolean.FALSE);
        client.update(clientRep);

        result = dockerClientContainer.execInContainer("docker", "login", "-u", DOCKER_USER, "-p", DOCKER_USER_PASSWORD, REGISTRY_HOSTNAME + ":" + REGISTRY_PORT);
        printCommandResult(result);
        assertThat("Error performing login", result.getExitCode(), not(is(0)));
        assertThat("Service is not disabled", result.getStderr(), containsString("Client specified by 'service' is disabled"));
        assertLoginErrorClientDisabled();
    }

    private void printCommandResult(Container.ExecResult result) {
        LOG.info("Command executed with exit code " + result.getExitCode() + ". Output follows:\nSTDOUT: "
                + result.getStdout() + "\n---\nSTDERR: " + result.getStderr());
    }

    private void assertLogin(UserRepresentation dockerUser) {
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .hasCodeId()
                .clientId(CLIENT_ID)
                .userId(dockerUser.getId())
                .details(Details.AUTH_METHOD, DockerAuthV2Protocol.LOGIN_PROTOCOL)
                .details(Details.USERNAME, DOCKER_USER)
                .withoutDetails(Details.REDIRECT_URI);
    }

    private void assertLoginErrorClientDisabled() {
        EventRepresentation eventRep = EventAssertion.assertError(events.poll())
                .type(EventType.LOGIN_ERROR)
                .clientId(CLIENT_ID)
                .userId(null)
                .error(Errors.CLIENT_DISABLED)
                .getEvent();
        MatcherAssert.assertThat(eventRep.getIpAddress(), Matchers.any(String.class));
    }

    public static class DockerServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.DOCKER);
        }
    }

    public static class DockerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realmBuilder) {
            RealmRepresentation dockerRealm = DockerTestRealmSetup.createRealm(REALM_ID);
            DockerTestRealmSetup.configureDockerRegistryClient(dockerRealm, CLIENT_ID);
            DockerTestRealmSetup.configureUser(dockerRealm, DOCKER_USER, DOCKER_USER_PASSWORD);
            return RealmBuilder.update(dockerRealm);
        }
    }
}
