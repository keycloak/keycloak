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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.http.ssl.SSLContexts;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.Time;
import org.keycloak.testsuite.arquillian.KcArquillian;
import org.keycloak.testsuite.arquillian.TestContext;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.ws.rs.NotFoundException;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.auth.page.AuthServer;
import org.keycloak.testsuite.auth.page.AuthServerContextRoot;
import org.keycloak.testsuite.auth.page.WelcomePage;
import org.keycloak.testsuite.auth.page.account.Account;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.auth.page.login.UpdatePassword;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.TestCleanup;
import org.keycloak.testsuite.util.TestEventsLogger;
import org.openqa.selenium.WebDriver;

import static org.keycloak.testsuite.admin.Users.setPasswordFor;
import static org.keycloak.testsuite.auth.page.AuthRealm.ADMIN;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;
import static org.keycloak.testsuite.util.IOUtil.PROJECT_BUILD_DIRECTORY;

/**
 *
 * @author tkyjovsk
 */
@RunWith(KcArquillian.class)
@RunAsClient
public abstract class AbstractKeycloakTest {

    protected Logger log = Logger.getLogger(this.getClass());

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
    protected WelcomePage welcomePage;

    protected UserRepresentation adminUser;

    private PropertiesConfiguration constantsProperties;

    private boolean resetTimeOffset;

    @Before
    public void beforeAbstractKeycloakTest() throws Exception {
        adminClient = testContext.getAdminClient();
        if (adminClient == null) {
            adminClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting());
            testContext.setAdminClient(adminClient);
        }

        getTestingClient();

        adminUser = createAdminUserRepresentation();

        setDefaultPageUriParameters();

        TestEventsLogger.setDriver(driver);

        if (!suiteContext.isAdminPasswordUpdated()) {
            log.debug("updating admin password");
            updateMasterAdminPassword();
            suiteContext.setAdminPasswordUpdated(true);
        }

        if (testContext.getTestRealmReps() == null) {
            importTestRealms();

            if (!isImportAfterEachMethod()) {
                testContext.setTestRealmReps(testRealmReps);
            }
        }

        oauth.init(adminClient, driver);
    }

    @After
    public void afterAbstractKeycloakTest() {
        if (resetTimeOffset) {
            resetTimeOffset();
        }

        if (isImportAfterEachMethod()) {
            log.info("removing test realms after test method");
            for (RealmRepresentation testRealm : testRealmReps) {
                removeRealm(testRealm.getRealm());
            }
        } else {
            // Logout all users after the test
            List<RealmRepresentation> realms = testContext.getTestRealmReps();
            for (RealmRepresentation realm : realms) {
                adminClient.realm(realm.getRealm()).logoutAll();
            }

            // Cleanup objects
            for (TestCleanup cleanup : testContext.getCleanups().values()) {
                cleanup.executeCleanup();
            }
            testContext.getCleanups().clear();
        }
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

    private void updateMasterAdminPassword() {
        welcomePage.navigateTo();
        if (!welcomePage.isPasswordSet()) {
            welcomePage.setPassword("admin", "admin");
        }
    }

    public void deleteAllCookiesForMasterRealm() {
        deleteAllCookiesForRealm(accountPage);
    }

    protected void deleteAllCookiesForRealm(Account realmAccountPage) {
        // masterRealmPage.navigateTo();
        realmAccountPage.navigateTo(); // Because IE webdriver freezes when loading a JSON page (realm page), we need to use this alternative
        log.info("deleting cookies in '" + realmAccountPage.getAuthRealm() + "' realm");
        driver.manage().deleteAllCookies();
    }

    protected void deleteAllCookiesForRealm(String realmName) {
        // masterRealmPage.navigateTo();
        driver.navigate().to(oauth.AUTH_SERVER_ROOT + "/realms/" + realmName + "/account"); // Because IE webdriver freezes when loading a JSON page (realm page), we need to use this alternative
        log.info("deleting cookies in '" + realmName + "' realm");
        driver.manage().deleteAllCookies();
    }

    public void setDefaultPageUriParameters() {
        masterRealmPage.setAuthRealm(MASTER);
        loginPage.setAuthRealm(MASTER);
    }

    public KeycloakTestingClient getTestingClient() {
        if (testingClient == null) {
            testingClient = testContext.getTestingClient();
            if (testingClient == null) {
                testingClient = KeycloakTestingClient.getInstance(AuthServerTestEnricher.getAuthServerContextRoot() + "/auth");
                testContext.setTestingClient(testingClient);
            }
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

    public void importTestRealms() {
        addTestRealms();
        log.info("importing test realms");
        for (RealmRepresentation testRealm : testRealmReps) {
            importRealm(testRealm);
        }
    }

    private UserRepresentation createAdminUserRepresentation() {
        UserRepresentation adminUserRep = new UserRepresentation();
        adminUserRep.setUsername(ADMIN);
        setPasswordFor(adminUserRep, ADMIN);
        return adminUserRep;
    }

    public void importRealm(RealmRepresentation realm) {
        log.debug("importing realm: " + realm.getRealm());
        try { // TODO - figure out a way how to do this without try-catch
            RealmResource realmResource = adminClient.realms().realm(realm.getRealm());
            RealmRepresentation rRep = realmResource.toRepresentation();
            log.debug("realm already exists on server, re-importing");
            realmResource.remove();
        } catch (NotFoundException nfe) {
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
     * @param realm Realm name
     * @param username Username
     * @param password Password
     * @param requiredActions
     * @return ID of the newly created user
     */
    public String createUser(String realm, String username, String password, String ... requiredActions) {
        List<String> requiredUserActions = Arrays.asList(requiredActions);

        UserRepresentation homer = new UserRepresentation();
        homer.setEnabled(true);
        homer.setUsername(username);
        homer.setRequiredActions(requiredUserActions);

        return ApiUtil.createUserAndResetPasswordWithAdminClient(adminClient.realm(realm), homer, password);
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

    private String invokeTimeOffset(int offset) {
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
}
