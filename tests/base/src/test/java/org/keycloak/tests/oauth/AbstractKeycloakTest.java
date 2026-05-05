package org.keycloak.tests.oauth;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;

import org.jboss.logging.Logger;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.runners.MethodSorters;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Base class for Keycloak OAuth tests - migrated to new test framework.
 * Removes all Arquillian dependencies and uses new framework patterns.
 *
 * @author tkyjovsk
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class AbstractKeycloakTest {

    protected Logger log = Logger.getLogger(this.getClass());

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    protected OAuthClient oauth;

    @InjectKeycloakUrls
    protected KeycloakUrls keycloakUrls;

    @InjectWebDriver
    protected ManagedWebDriver driver;

    @InjectPage
    protected LoginPage loginPage;

    @InjectTimeOffSet
    protected TimeOffSet timeOffSet;

    private boolean resetTimeOffset;

    protected List<RealmRepresentation> testRealmReps;

    @BeforeEach
    public void beforeAbstractKeycloakTest(TestInfo testInfo) throws Exception {
        log.info("========== Running test: " + testInfo.getDisplayName() + " ==========");

        beforeAbstractKeycloakTestRealmImport();

        // OAuth client is automatically initialized by the framework

        afterAbstractKeycloakTestRealmImport();
    }

    /**
     * Executed before test realms import
     * Override to add custom setup before realm configuration
     */
    protected void beforeAbstractKeycloakTestRealmImport() throws Exception {
    }

    /**
     * Executed after test realms import
     * Override to add custom setup after realm configuration
     */
    protected void afterAbstractKeycloakTestRealmImport() {
    }

    /**
     * Executed as the last task of each test case
     * Override to add custom cleanup logic
     */
    protected void postAfterAbstractKeycloak() throws Exception {
    }

    @AfterEach
    public void afterAbstractKeycloakTest() throws Exception {
        if (resetTimeOffset) {
            resetTimeOffset();
        }

        // Cleanup is now handled by ManagedRealm automatically
        // But we can still do custom cleanup if needed
        postAfterAbstractKeycloak();

        // Reset OAuth client
        if (oauth != null && oauth.httpClient() != null) {
            oauth.httpClient().reset();
        }
    }

    /**
     * Override to indicate if realm should be re-imported after each test method.
     * Default is false - realm persists across test methods.
     */
    protected boolean isImportAfterEachMethod() {
        return false;
    }

    public void deleteAllCookiesForRealm(String realmName) {
        driver.driver().navigate().to(keycloakUrls.getBase() + "/realms/" + realmName + "/testing/blank");
        log.info("deleting cookies in '" + realmName + "' realm");
        driver.driver().manage().deleteAllCookies();
    }


    /**
     * Creates a user in the given realm and returns its ID.
     *
     * @param realm           Managed realm
     * @param username        Username
     * @param password        Password
     * @param requiredActions Required actions
     * @return ID of the newly created user
     */
    public String createUser(ManagedRealm realm, String username, String password, String... requiredActions) {
        UserRepresentation user = createUserRepresentation(username, password);
        user.setRequiredActions(Arrays.asList(requiredActions));

        return createUserWithAdminClient(realm.admin(), user);
    }

    public String createUser(ManagedRealm realm, String username, String password, String firstName,
                           String lastName, String email, Consumer<UserRepresentation> customizer) {
        UserRepresentation user = createUserRepresentation(username, email, firstName, lastName, true, password);
        customizer.accept(user);
        return createUserWithAdminClient(realm.admin(), user);
    }

    public String createUser(ManagedRealm realm, String username, String password, String firstName,
                           String lastName, String email) {
        UserRepresentation user = createUserRepresentation(username, email, firstName, lastName, true, password);
        return createUserWithAdminClient(realm.admin(), user);
    }

    private String createUserWithAdminClient(org.keycloak.admin.client.resource.RealmResource realm,
                                            UserRepresentation user) {
        Response response = realm.users().create(user);
        String userId = ApiUtil.getCreatedId(response);
        response.close();
        return userId;
    }

    public static UserRepresentation createUserRepresentation(String id, String username, String email,
                                                             String firstName, String lastName,
                                                             List<String> groups, boolean enabled) {
        UserRepresentation user = new UserRepresentation();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setGroups(groups);
        user.setEnabled(enabled);
        return user;
    }

    public static UserRepresentation createUserRepresentation(String username, String email, String firstName,
                                                             String lastName, List<String> groups, boolean enabled) {
        return createUserRepresentation(null, username, email, firstName, lastName, groups, enabled);
    }

    public static UserRepresentation createUserRepresentation(String username, String email, String firstName,
                                                             String lastName, boolean enabled) {
        return createUserRepresentation(username, email, firstName, lastName, null, enabled);
    }

    public static UserRepresentation createUserRepresentation(String username, String email, String firstName,
                                                             String lastName, boolean enabled, String password) {
        UserRepresentation user = createUserRepresentation(username, email, firstName, lastName, enabled);
        setPasswordFor(user, password);
        return user;
    }

    public static UserRepresentation createUserRepresentation(String username, String password) {
        return createUserRepresentation(username, null, null, null, true, password);
    }

    /**
     * Set password for a user representation.
     *
     * @param user     User representation
     * @param password Password to set
     */
    protected static void setPasswordFor(UserRepresentation user, String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.setCredentials(Collections.singletonList(credential));
    }

    protected void createAppClientInRealm(ManagedRealm realm) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("test-app");
        client.setName("test-app");
        client.setSecret("password");
        client.setEnabled(true);
        client.setDirectAccessGrantsEnabled(true);

        String serverRoot = keycloakUrls.getBase();
        client.setRedirectUris(Collections.singletonList(serverRoot + "/*"));
        client.setBaseUrl(serverRoot + "/realms/" + realm + "/app");

        OIDCAdvancedConfigWrapper.fromClientRepresentation(client)
                .setPostLogoutRedirectUris(Collections.singletonList("+"));

        Response response = realm.admin().clients().create(client);
        response.close();
    }

    /**
     * Sets time of day by calculating time offset and using setTimeOffset() to set it.
     *
     * @param hour   hour of day
     * @param minute minute
     * @param second second
     */
    public void setTimeOfDay(int hour, int minute, int second) {
        setTimeOfDay(hour, minute, second, 0);
    }

    /**
     * Sets time of day by calculating time offset and using setTimeOffset() to set it.
     *
     * @param hour       hour of day
     * @param minute     minute
     * @param second     second
     * @param addSeconds additional seconds to add to offset time
     */
    public void setTimeOfDay(int hour, int minute, int second, int addSeconds) {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, hour);
        now.set(Calendar.MINUTE, minute);
        now.set(Calendar.SECOND, second);
        int offset = (int) ((now.getTime().getTime() - System.currentTimeMillis()) / 1000);

        setTimeOffset(offset + addSeconds);
    }

    /**
     * Sets time offset in seconds that will be added to Time.currentTime() and Time.currentTimeMillis().
     *
     * @param offset Time offset in seconds
     */
    public void setTimeOffset(int offset) {
        timeOffSet.set(offset);
        resetTimeOffset = offset != 0;
        log.debugv("Set time offset to {0}", offset);
    }

    public void resetTimeOffset() {
        timeOffSet.set(0);
        resetTimeOffset = false;
        log.debug("Reset time offset");
    }

    public void setOtpTimeOffset(int offsetSeconds, TimeBasedOTP otp) {
        setTimeOffset(offsetSeconds);
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, offsetSeconds);
        otp.setCalendar(calendar);
    }

    public int getCurrentTime() {
        return Time.currentTime();
    }

    public URI getAuthServerRoot() {
        try {
            return new URI(keycloakUrls.getBase());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Logger getLogger() {
        return log;
    }

    protected void assertResponseSuccessful(Response response) {
        try {
            assertEquals(Response.Status.Family.SUCCESSFUL, response.getStatusInfo().getFamily());
        } catch (AssertionError ex) {
            throw new AssertionError("unexpected response code " + response.getStatus() +
                    ", body is:\n" + response.readEntity(String.class), ex);
        }
    }

    public static <T> void eventuallyEquals(String message, T expected, Supplier<T> actual) {
        eventuallyEquals(message, expected, actual, 10000, 100, MILLISECONDS);
    }

    public static <T> void eventuallyEquals(String message, T expected, Supplier<T> actual, long timeout,
                                           long pollInterval, TimeUnit unit) {
        if (pollInterval <= 0) {
            throw new IllegalArgumentException("Check interval must be positive");
        }
        try {
            long expectedEndTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeout, unit);
            long sleepMillis = MILLISECONDS.convert(pollInterval, unit);
            do {
                if (Objects.equals(expected, actual.get())) {
                    return;
                }

                Thread.sleep(sleepMillis);
            } while (expectedEndTime - System.nanoTime() > 0);

            // Last attempt
            assertEquals(expected, actual.get(), message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting", e);
        }
    }

    protected static String generatePassword() {
        return generatePassword(64);
    }

    protected static String generatePassword(int length) {
        return SecretGenerator.getInstance().randomString(length);
    }

    protected String getAccountRootUrl() {
        return keycloakUrls.getBase() + "/realms/test/account";
    }

    /**
     * Execute a test method with a timeout.
     *
     * @param timeout               Timeout in milliseconds
     * @param executableTestMethod  Method to execute
     * @throws Exception if execution fails or times out
     */
    protected void runTestWithTimeout(long timeout, ExecutableTestMethod executableTestMethod) throws Exception {
        ExecutorService service = Executors.newSingleThreadExecutor();
        Callable<Object> callable = () -> {
            executableTestMethod.execute();
            return null;
        };
        Future<Object> result = service.submit(callable);
        service.shutdown();
        try {
            boolean terminated = service.awaitTermination(timeout, TimeUnit.MILLISECONDS);
            if (!terminated) {
                service.shutdownNow();
            }
            result.get(0, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new AssertionError("Test timed out after " + timeout + "ms");
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    protected interface ExecutableTestMethod {
        void execute() throws Exception;
    }

    /**
     * Helper method for HTTPS-aware configuration stream processing.
     * Replaces HTTP URLs with HTTPS when SSL is required.
     */
    protected static InputStream httpsAwareConfigurationStream(InputStream input) throws IOException {
        boolean sslRequired = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required", "false"));
        if (!sslRequired) {
            return input;
        }

        PipedInputStream in = new PipedInputStream();
        final PipedOutputStream out = new PipedOutputStream(in);
        new Thread(() -> {
            try (PrintWriter pw = new PrintWriter(out);
                 Scanner s = new Scanner(input)) {
                while (s.hasNextLine()) {
                    String lineWithReplaces = s.nextLine()
                            .replace("http://localhost:8180", "https://localhost:8443")
                            .replace("http://localhost:8080", "https://localhost:8443");
                    pw.println(lineWithReplaces);
                }
            }
        }).start();

        return in;
    }

    /**
     * Remove a specific realm.
     *
     * @param realm Realm to be removed
     */
    public void removeRealm(ManagedRealm realm) {
        String realmName = realm.getName();
        log.info("removing realm: " + realmName);
        try {
            realm.admin().remove();
        } catch (NotFoundException e) {
            log.warn("Realm " + realmName + " not found for removal");
        }
    }
}
