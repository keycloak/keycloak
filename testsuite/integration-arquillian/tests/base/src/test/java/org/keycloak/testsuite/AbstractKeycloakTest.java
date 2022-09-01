/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite;

import io.appium.java_client.AppiumDriver;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.model.TestTimedOutException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.common.Profile;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.Time;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.UserCache;
import org.keycloak.provider.Provider;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.account.AccountFormService;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.KcArquillian;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.arquillian.TestContext;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.auth.page.AuthServer;
import org.keycloak.testsuite.auth.page.AuthServerContextRoot;
import org.keycloak.testsuite.auth.page.WelcomePage;
import org.keycloak.testsuite.auth.page.account.Account;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.auth.page.login.UpdatePassword;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.util.CryptoInitRule;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.TestCleanup;
import org.keycloak.testsuite.util.TestEventsLogger;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.UriBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_HOST;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_PORT;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SCHEME;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;
import static org.keycloak.testsuite.util.URLUtils.navigateToUri;
import static org.keycloak.testsuite.util.ServerURLs.removeDefaultPorts;

/**
 *
 * @author tkyjovsk
 */
@RunWith(KcArquillian.class)
@RunAsClient
public abstract class AbstractKeycloakTest {
    protected static final String ENGLISH_LOCALE_NAME = "English";

    protected Logger log = Logger.getLogger(this.getClass());

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @ArquillianResource
    protected SuiteContext suiteContext;

    @ArquillianResource
    protected TestContext testContext;

    protected Keycloak adminClient;

    protected KeycloakTestingClient testingClient;

    @ArquillianResource
    protected OAuthClient oauth;

    protected List<RealmRepresentation> testRealmReps;

    @Drone
    protected WebDriver driver;

    @Page
    protected AuthServerContextRoot authServerContextRootPage;
    @Page
    protected AuthServer authServerPage;

    @Page
    protected AuthRealm masterRealmPage;

    @Page
    protected Account accountPage;

    @Page
    protected OIDCLogin loginPage;

    @Page
    protected UpdatePassword updatePasswordPage;

    @Page
    protected LoginPasswordUpdatePage passwordUpdatePage;

    @Page
    protected WelcomePage welcomePage;

    private PropertiesConfiguration constantsProperties;

    private boolean resetTimeOffset;

    @Before
    public void beforeAbstractKeycloakTest() throws Exception {
        adminClient = testContext.getAdminClient();
        if (adminClient == null || adminClient.isClosed()) {
            reconnectAdminClient();
        }

        getTestingClient();

        setDefaultPageUriParameters();

        TestEventsLogger.setDriver(driver);

        // The backend cluster nodes may not be yet started. Password will be updated later for cluster setup.
        if (!AuthServerTestEnricher.AUTH_SERVER_CLUSTER) {
            updateMasterAdminPassword();
        }

        beforeAbstractKeycloakTestRealmImport();

        if (testContext.getTestRealmReps().isEmpty()) {
            importTestRealms();

            if (!isImportAfterEachMethod()) {
                testContext.setTestRealmReps(testRealmReps);
            }

            afterAbstractKeycloakTestRealmImport();
        }

        oauth.init(driver);

    }

    public void reconnectAdminClient() throws Exception {
        testContext.reconnectAdminClient();
        adminClient = testContext.getAdminClient();
    }

    /**
     * Executed before test realms import
     * <p>
     * In @Before block
     */
    protected void beforeAbstractKeycloakTestRealmImport() throws Exception {
    }

    /**
     * Executed after test realms import
     * <p>
     * In @Before block
     */
    protected void afterAbstractKeycloakTestRealmImport() {
    }

    /**
     * Executed as the last task of each test case
     * <p>
     * In @After block
     */
    protected void postAfterAbstractKeycloak() throws Exception {
    }

    @After
    public void afterAbstractKeycloakTest() throws Exception {
        if (resetTimeOffset) {
            resetTimeOffset();
        }

        if (isImportAfterEachMethod()) {
            log.info("removing test realms after test method");
            for (RealmRepresentation testRealm : testRealmReps) {
                removeRealm(testRealm.getRealm());
            }
        } else {
            log.info("calling all TestCleanup");
            // Remove all sessions
            testContext.getTestRealmReps().stream().forEach((r)->testingClient.testing().removeUserSessions(r.getRealm()));

            // Cleanup objects
            for (TestCleanup cleanup : testContext.getCleanups().values()) {
                try {
                    if (cleanup != null) cleanup.executeCleanup();
                } catch (Exception e) {
                    log.error("failed cleanup!", e);
                    throw new RuntimeException(e);
                }
            }
            testContext.getCleanups().clear();
        }

        postAfterAbstractKeycloak();

        // Remove all browsers from queue
        DroneUtils.resetQueue();
    }

    protected TestCleanup getCleanup(String realmName) {
        return testContext.getOrCreateCleanup(realmName);
    }

    protected TestCleanup getCleanup() {
        return getCleanup("test");
    }

    protected boolean isImportAfterEachMethod() {
        return false;
    }

    protected void updateMasterAdminPassword() {
        if (!suiteContext.isAdminPasswordUpdated()) {
            log.debug("updating admin password");

            welcomePage.navigateTo();
            if (!welcomePage.isPasswordSet()) {
                welcomePage.setPassword("admin", "admin");
            }

            suiteContext.setAdminPasswordUpdated(true);
        }
    }

    public void deleteAllCookiesForMasterRealm() {
        deleteAllCookiesForRealm(MASTER);
    }

    protected void deleteAllCookiesForRealm(String realmName) {
        // we can't use /auth/realms/{realmName} because some browsers (e.g. Chrome) apparently don't send cookies
        // to JSON pages and therefore can't delete realms cookies there; a non existing page will do just fine
        navigateToUri(accountPage.getAuthRoot() + "/realms/" + realmName + "/super-random-page");
        log.info("deleting cookies in '" + realmName + "' realm");
        driver.manage().deleteAllCookies();
    }

    // this is useful mainly for smartphones as cookies deletion doesn't work there
    protected void deleteAllSessionsInRealm(String realmName) {
        log.info("removing all sessions from '" + realmName + "' realm...");
        try {
            adminClient.realm(realmName).logoutAll();
            log.info("sessions successfully deleted");
        }
        catch (NotFoundException e) {
            log.warn("realm not found");
        }
    }

    protected void resetRealmSession(String realmName) {
        deleteAllCookiesForRealm(realmName);

        if (driver instanceof AppiumDriver) { // smartphone drivers don't support cookies deletion
            try {
                log.info("resetting realm session");

                final RealmRepresentation realmRep = adminClient.realm(realmName).toRepresentation();

                deleteAllSessionsInRealm(realmName); // logout users

                if (realmRep.isInternationalizationEnabled()) { // reset the locale
                    String locale = getDefaultLocaleName(realmRep.getRealm());
                    loginPage.localeDropdown().selectByText(locale);
                    log.info("locale reset to " + locale);
                }
            } catch (NotFoundException e) {
                log.warn("realm not found");
            }
        }
    }

    protected String getDefaultLocaleName(String realmName) {
        return ENGLISH_LOCALE_NAME;
    }

    public void setDefaultPageUriParameters() {
        masterRealmPage.setAuthRealm(MASTER);
        loginPage.setAuthRealm(MASTER);
    }

    public KeycloakTestingClient getTestingClient() {
        if (testingClient == null) {
            testingClient = testContext.getTestingClient();
        }
        return testingClient;
    }

    public TestContext getTestContext() {
        return testContext;
    }

    public Keycloak getAdminClient() {
        return adminClient;
    }

    public abstract void addTestRealms(List<RealmRepresentation> testRealms);

    private void addTestRealms() {
        log.debug("loading test realms");
        if (testRealmReps == null) {
            testRealmReps = new ArrayList<>();
        }
        if (testRealmReps.isEmpty()) {
            addTestRealms(testRealmReps);
        }
    }

    public void fixAuthServerHostAndPortForClientRepresentation(ClientRepresentation cr) {
        cr.setBaseUrl(removeDefaultPorts(replaceAuthHostWithRealHost(cr.getBaseUrl())));
        cr.setAdminUrl(removeDefaultPorts(replaceAuthHostWithRealHost(cr.getAdminUrl())));

        if (cr.getRedirectUris() != null && !cr.getRedirectUris().isEmpty()) {
            List<String> fixedUrls = new ArrayList<>(cr.getRedirectUris().size());
            for (String url : cr.getRedirectUris()) {
                fixedUrls.add(removeDefaultPorts(replaceAuthHostWithRealHost(url)));
            }

            cr.setRedirectUris(fixedUrls);
        }
    }

    public String replaceAuthHostWithRealHost(String url) {
        if (url != null && (url.contains("localhost:8180") || url.contains("localhost:8543"))) {
            return url.replaceFirst("localhost:(\\d)+", AUTH_SERVER_HOST + ":" + AUTH_SERVER_PORT);
        }

        return url;
    }

    public void importTestRealms() {
        addTestRealms();
        log.info("importing test realms");
        for (RealmRepresentation testRealm : testRealmReps) {
            importRealm(testRealm);
        }
    }

    private void modifySamlAttributes(ClientRepresentation cr) {
        if (cr.getProtocol() != null && cr.getProtocol().equals("saml")) {
            log.debug("Modifying attributes of SAML client: " + cr.getClientId());
            for (Map.Entry<String, String> entry : cr.getAttributes().entrySet()) {
                cr.getAttributes().put(entry.getKey(), replaceHttpValuesWithHttps(entry.getValue()));
            }
        }
    }

    private void modifyRedirectUrls(ClientRepresentation cr) {
        if (cr.getRedirectUris() != null && cr.getRedirectUris().size() > 0) {
            List<String> redirectUrls = cr.getRedirectUris();
            List<String> fixedRedirectUrls = new ArrayList<>(redirectUrls.size());
            for (String url : redirectUrls) {
                fixedRedirectUrls.add(replaceHttpValuesWithHttps(url));
            }
            cr.setRedirectUris(fixedRedirectUrls);
        }
    }

    private void modifyMainUrls(ClientRepresentation cr) {
        cr.setBaseUrl(replaceHttpValuesWithHttps(cr.getBaseUrl()));
        cr.setAdminUrl(replaceHttpValuesWithHttps(cr.getAdminUrl()));
    }

    private String replaceHttpValuesWithHttps(String input) {
        if (input == null) {
            return null;
        }
        if ("".equals(input)) {
            return "";
        }
        return input
              .replace("http", "https")
              .replace("8080", "8543")
              .replace("8180", "8543");
    }

    protected interface ExecutableTestMethod {
        void execute() throws Exception;
    }

    protected void runTestWithTimeout(long timeout, ExecutableTestMethod executableTestMethod) throws Exception {
        ExecutorService service = Executors.newSingleThreadExecutor();
        Callable<Object> callable = new Callable<Object>() {
            public Object call() throws Exception {
                executableTestMethod.execute();
                return null;
            }
        };
        Future<Object> result = service.submit(callable);
        service.shutdown();
        try {
            boolean terminated = service.awaitTermination(timeout,
                    TimeUnit.MILLISECONDS);
            if (!terminated) {
                service.shutdownNow();
            }
            result.get(0, TimeUnit.MILLISECONDS); // throws the exception if one occurred during the invocation
        } catch (TimeoutException e) {
            throw new TestTimedOutException(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    /**
     * @return Return <code>true</code> if you wish to automatically post-process realm and replace
     * all http values with https (and correct ports).
     */
    protected boolean modifyRealmForSSL() {
        return false;
    }


    protected void removeAllRealmsDespiteMaster() {
        // remove all realms (accidentally left by other tests) except for master
        adminClient.realms().findAll().stream()
                .map(RealmRepresentation::getRealm)
                .filter(realmName -> ! realmName.equals("master"))
                .forEach(this::removeRealm);
        assertThat(adminClient.realms().findAll().size(), is(equalTo(1)));
    }


    public void importRealm(RealmRepresentation realm) {
        if (modifyRealmForSSL()) {
            if (AUTH_SERVER_SSL_REQUIRED) {
                log.debugf("Modifying %s for SSL", realm.getId());
                for (ClientRepresentation cr : realm.getClients()) {
                    modifyMainUrls(cr);
                    modifyRedirectUrls(cr);
                    modifySamlAttributes(cr);
                }
            }
        }

        if (!AUTH_SERVER_HOST.equals("localhost")) {
            if (!AUTH_SERVER_SSL_REQUIRED) {
                realm.setSslRequired("none");
            }
            if (realm.getClients() != null) {
                for (ClientRepresentation cr : realm.getClients()) {
                    fixAuthServerHostAndPortForClientRepresentation(cr);
                }
            }

            if (realm.getApplications() != null) {
                for (ClientRepresentation cr : realm.getApplications()) {
                    fixAuthServerHostAndPortForClientRepresentation(cr);
                }
            }
        }

        log.debug("--importing realm: " + realm.getRealm());
        try {
            adminClient.realms().realm(realm.getRealm()).remove();
            log.debug("realm already existed on server, re-importing");
        } catch (NotFoundException ignore) {
            // expected when realm does not exist
        }
        adminClient.realms().create(realm);
    }

    public void removeRealm(String realmName) {
        log.info("removing realm: " + realmName);
        try {
            adminClient.realms().realm(realmName).remove();
        } catch (NotFoundException e) {
        }
    }

    public RealmsResource realmsResouce() {
        return adminClient.realms();
    }

    /**
     * Creates a user in the given realm and returns its ID.
     *
     * @param realm           Realm name
     * @param username        Username
     * @param password        Password
     * @param requiredActions
     * @return ID of the newly created user
     */
    public String createUser(String realm, String username, String password, String... requiredActions) {
        UserRepresentation homer = createUserRepresentation(username, password);
        homer.setRequiredActions(Arrays.asList(requiredActions));

        return ApiUtil.createUserWithAdminClient(adminClient.realm(realm), homer);
    }

    public String createUser(String realm, String username, String password, String firstName, String lastName, String email, Consumer<UserRepresentation> customizer) {
        UserRepresentation user = createUserRepresentation(username, email, firstName, lastName, true, password);
        customizer.accept(user);
        return ApiUtil.createUserWithAdminClient(adminClient.realm(realm), user);
    }

    public String createUser(String realm, String username, String password, String firstName, String lastName, String email) {
        UserRepresentation homer = createUserRepresentation(username, email, firstName, lastName, true, password);
        return ApiUtil.createUserWithAdminClient(adminClient.realm(realm), homer);
    }

    public static UserRepresentation createUserRepresentation(String username, String email, String firstName, String lastName, List<String> groups, boolean enabled) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setGroups(groups);
        user.setEnabled(enabled);
        return user;
    }

    public static UserRepresentation createUserRepresentation(String username, String email, String firstName, String lastName, boolean enabled) {
        return createUserRepresentation(username, email, firstName, lastName, null, enabled);
    }

    public static UserRepresentation createUserRepresentation(String username, String email, String firstName, String lastName, boolean enabled, String password) {
        UserRepresentation user = createUserRepresentation(username, email, firstName, lastName, enabled);
        setPasswordFor(user, password);
        return user;
    }

    public static UserRepresentation createUserRepresentation(String username, String password) {
        UserRepresentation user = createUserRepresentation(username, null, null, null, true, password);
        return user;
    }

    public void setRequiredActionEnabled(String realm, String requiredAction, boolean enabled, boolean defaultAction) {
        AuthenticationManagementResource managementResource = adminClient.realm(realm).flows();

        RequiredActionProviderRepresentation action = managementResource.getRequiredAction(requiredAction);
        action.setEnabled(enabled);
        action.setDefaultAction(defaultAction);

        managementResource.updateRequiredAction(requiredAction, action);
    }

    public void setRequiredActionEnabled(String realm, String userId, String requiredAction, boolean enabled) {
        UsersResource usersResource = adminClient.realm(realm).users();

        UserResource userResource = usersResource.get(userId);
        UserRepresentation userRepresentation = userResource.toRepresentation();

        List<String> requiredActions = userRepresentation.getRequiredActions();
        if (enabled && !requiredActions.contains(requiredAction)) {
            requiredActions.add(requiredAction);
        } else if (!enabled && requiredActions.contains(requiredAction)) {
            requiredActions.remove(requiredAction);
        }

        userResource.update(userRepresentation);
    }

    /**
     * Sets time of day by calculating time offset and using setTimeOffset() to set it.
     *
     * @param hour hour of day
     * @param minute minute
     * @param second second
     */
    public void setTimeOfDay(int hour, int minute, int second) {
        setTimeOfDay(hour, minute, second, 0);
    }

    /**
     * Sets time of day by calculating time offset and using setTimeOffset() to set it.
     *
     * @param hour hour of day
     * @param minute minute
     * @param second second
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
     * Sets time offset in seconds that will be added to Time.currentTime() and Time.currentTimeMillis() both for client and server.
     *
     * @param offset
     */
    public void setTimeOffset(int offset) {
        String response = invokeTimeOffset(offset);
        resetTimeOffset = offset != 0;
        log.debugv("Set time offset, response {0}", response);
    }

    public void resetTimeOffset() {
        String response = invokeTimeOffset(0);
        resetTimeOffset = false;
        log.debugv("Reset time offset, response {0}", response);
    }

    public int getCurrentTime() {
        return Time.currentTime();
    }

    protected String invokeTimeOffset(int offset) {
        // adminClient depends on Time.offset for auto-refreshing tokens
        Time.setOffset(offset);
        Map result = testingClient.testing().setTimeOffset(Collections.singletonMap("offset", String.valueOf(offset)));
        return String.valueOf(result);
    }

    private void loadConstantsProperties() throws ConfigurationException {
        constantsProperties = new PropertiesConfiguration(System.getProperty("testsuite.constants"));
        constantsProperties.setThrowExceptionOnMissing(true);
    }

    protected PropertiesConfiguration getConstantsProperties() throws ConfigurationException {
        if (constantsProperties == null) {
            loadConstantsProperties();
        }
        return constantsProperties;
    }

    public URI getAuthServerRoot() {
        try {
            return KeycloakUriBuilder.fromUri(suiteContext.getAuthServerInfo().getContextRoot().toURI()).path("/auth/").build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Logger getLogger() {
        return log;
    }

    protected String getAccountRedirectUrl(String realm) {
        return AccountFormService
              .loginRedirectUrl(UriBuilder.fromUri(oauth.AUTH_SERVER_ROOT))
              .build(realm)
              .toString();
    }

    protected String getAccountRedirectUrl() {
        return getAccountRedirectUrl("test");
    }

    protected static InputStream httpsAwareConfigurationStream(InputStream input) throws IOException {
        if (!AUTH_SERVER_SSL_REQUIRED) {
            return input;
        }
        PipedInputStream in = new PipedInputStream();
        final PipedOutputStream out = new PipedOutputStream(in);
        try (PrintWriter pw = new PrintWriter(out)) {
            try (Scanner s = new Scanner(input)) {
                while (s.hasNextLine()) {
                    String lineWithReplaces = s.nextLine().replace("http://localhost:8180/auth", AUTH_SERVER_SCHEME + "://localhost:" + AUTH_SERVER_PORT + "/auth");
                    pw.println(lineWithReplaces);
                }
            }
        }
        return in;
    }

    /**
     * Get product/project name
     *
     * @return f.e. 'RH-SSO' or 'Keycloak'
     */
    protected String getProjectName() {
        final boolean isProduct = adminClient.serverInfo().getInfo().getProfileInfo().getName().equals("product");
        return isProduct ? Profile.PRODUCT_NAME : Profile.PROJECT_NAME;
    }

    /**
     * MapRealmProvider uses session.invalidate() instead of calling e.g. 
     * session.clients().removeClients(realm); for clients (where clients are being removed one by one)
     *
     * Therefore it doesn't call session.users().preRemove(realm, client) for each client.
     * Due to that JpaUserFederatedStorageProvider.preRemove(realm, client) is not called.
     * So there remains objects in the database in user federation related tables after realm removal.
     *
     * Same for roles etc. 
     *
     * Legacy federated storage is NOT supposed to work with map storage, so this method 
     * returns true if realm provider is "jpa" to be able to skip particular tests.
     */
    protected boolean isJpaRealmProvider() {
        return keycloakUsingProviderWithId(RealmProvider.class, "jpa");
    }

    protected boolean keycloakUsingProviderWithId(Class<? extends Provider> providerClass, String requiredId) {
        String providerId = testingClient.server()
                .fetchString(s -> s.getKeycloakSessionFactory().getProviderFactory(providerClass).getId());
        return Objects.equals(providerId, "\"" + requiredId + "\"");
    }

    protected boolean isRealmCacheEnabled() {
        String realmCache = testingClient.server()
                .fetchString(s -> s.getKeycloakSessionFactory().getProviderFactory(CacheRealmProvider.class));
        return Objects.nonNull(realmCache);
    }

    protected boolean isUserCacheEnabled() {
        String userCache = testingClient.server()
                .fetchString(s -> s.getKeycloakSessionFactory().getProviderFactory(UserCache.class));
        return Objects.nonNull(userCache);
    }
}
