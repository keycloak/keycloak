/*
 * Copyright 2017 Analytical Graphics, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.x509;

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.logging.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.authenticators.x509.ValidateX509CertificateUsernameFactory;
import org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel;
import org.keycloak.authentication.authenticators.x509.X509ClientCertificateAuthenticatorFactory;
import org.keycloak.common.util.Encode;
import org.keycloak.events.Details;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.pages.AbstractPage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.x509.X509IdentityConfirmationPage;
import org.keycloak.testsuite.updaters.SetSystemProperty;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.testsuite.util.AssertAdminEvents;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.PhantomJSBrowser;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USERNAME_EMAIL;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USER_ATTRIBUTE;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.ISSUERDN;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTALTNAME_EMAIL;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTALTNAME_OTHERNAME;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTDN;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTDN_CN;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTDN_EMAIL;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;

/**
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @since 10/28/2016
 */
@AuthServerContainerExclude(REMOTE)
public abstract class AbstractX509AuthenticationTest extends AbstractTestRealmKeycloakTest {

    public static final String EMPTY_CRL_PATH = "empty.crl";
    public static final String INTERMEDIATE_CA_CRL_PATH = "intermediate-ca.crl";
    public static final String INTERMEDIATE_CA_INVALID_SIGNATURE_CRL_PATH = "intermediate-ca-invalid-signature.crl";
    public static final String INTERMEDIATE_CA_3_CRL_PATH = "intermediate-ca-3.crl";
    protected final Logger log = Logger.getLogger(this.getClass());

    static final String REQUIRED = "REQUIRED";
    static final String OPTIONAL = "OPTIONAL";
    static final String DISABLED = "DISABLED";
    static final String ALTERNATIVE = "ALTERNATIVE";

    // TODO move to a base class
    public static final String REALM_NAME = "test";

    protected String userId;

    protected String userId2;

    protected AuthenticationManagementResource authMgmtResource;

    protected AuthenticationExecutionInfoRepresentation browserExecution;

    protected AuthenticationExecutionInfoRepresentation directGrantExecution;

    private static SetSystemProperty phantomjsCliArgs;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public AssertAdminEvents assertAdminEvents = new AssertAdminEvents(this);

    @Page
    @PhantomJSBrowser
    protected AppPage appPage;

    @Page
    @PhantomJSBrowser
    protected X509IdentityConfirmationPage loginConfirmationPage;

    @Page
    @PhantomJSBrowser
    protected LoginPage loginPage;


    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    @Before
    public void validateConfiguration() {
        Assume.assumeTrue(AUTH_SERVER_SSL_REQUIRED);
    }


    @BeforeClass
    public static void onBeforeTestClass() {
        configurePhantomJS("/ca.crt", "/client.crt", "/client.key", "password");
    }

    @AfterClass
    public static void onAfterTestClass() {
        phantomjsCliArgs.revert();
    }

    /**
     * Setup phantom JS to be used for mutual TLS testing. All file paths are relative to "authServerHome"
     *
     * @param certificatesPath
     * @param clientCertificateFile
     * @param clientKeyFile
     * @param clientKeyPassword
     */
    protected static void configurePhantomJS(String certificatesPath, String clientCertificateFile, String clientKeyFile, String clientKeyPassword) {
        String authServerHome = getAuthServerHome();

        if (authServerHome != null && System.getProperty("auth.server.ssl.required") != null) {
            StringBuilder cliArgs = new StringBuilder();

            cliArgs.append("--ignore-ssl-errors=true ");
            cliArgs.append("--web-security=false ");
            cliArgs.append("--ssl-certificates-path=").append(authServerHome).append(certificatesPath).append(" ");
            cliArgs.append("--ssl-client-certificate-file=").append(authServerHome).append(clientCertificateFile).append(" ");
            cliArgs.append("--ssl-client-key-file=").append(authServerHome).append(clientKeyFile).append(" ");
            cliArgs.append("--ssl-client-key-passphrase=" + clientKeyPassword).append(" ");

            phantomjsCliArgs = new SetSystemProperty("keycloak.phantomjs.cli.args", cliArgs.toString());
        }
    }


    private static boolean isAuthServerJBoss() {
        return Boolean.parseBoolean(System.getProperty("auth.server.jboss"));
    }

    /**
     * @return server home directory. This directory is supposed to contain client key, certificate and CRLs used in the tests
     */
    protected static String getAuthServerHome() {
        String authServerHome = System.getProperty(AuthServerTestEnricher.AUTH_SERVER_HOME_PROPERTY);
        if (authServerHome == null) {
            return null;
        }

        if (isAuthServerJBoss()) {
            authServerHome = authServerHome + "/standalone/configuration";
        }

        return authServerHome;
    }

    @Before
    public void configureFlows() {
        authMgmtResource = adminClient.realms().realm(REALM_NAME).flows();

        AuthenticationFlowRepresentation browserFlow = copyBrowserFlow();
        Assert.assertNotNull(browserFlow);

        AuthenticationFlowRepresentation directGrantFlow = createDirectGrantFlow();
        Assert.assertNotNull(directGrantFlow);

        setBrowserFlow(browserFlow);
        Assert.assertEquals(testRealm().toRepresentation().getBrowserFlow(), browserFlow.getAlias());

        setDirectGrantFlow(directGrantFlow);
        Assert.assertEquals(testRealm().toRepresentation().getDirectGrantFlow(), directGrantFlow.getAlias());
        Assert.assertEquals(0, directGrantFlow.getAuthenticationExecutions().size());

        // Add X509 cert authenticator to the direct grant flow
        directGrantExecution = addAssertExecution(directGrantFlow, ValidateX509CertificateUsernameFactory.PROVIDER_ID, REQUIRED);
        Assert.assertNotNull(directGrantExecution);

        directGrantFlow = authMgmtResource.getFlow(directGrantFlow.getId());
        Assert.assertNotNull(directGrantFlow.getAuthenticationExecutions());
        Assert.assertEquals(1, directGrantFlow.getAuthenticationExecutions().size());

        // Add X509 authenticator to the browser flow
        browserExecution = addAssertExecution(browserFlow, X509ClientCertificateAuthenticatorFactory.PROVIDER_ID, ALTERNATIVE);
        Assert.assertNotNull(browserExecution);

        // Raise the priority of the authenticator to position it right before
        // the Username/password authentication
        // TODO find a better, more explicit way to specify the position
        // of authenticator within the flow relative to other authenticators
        authMgmtResource.raisePriority(browserExecution.getId());
        // TODO raising the priority didn't generate the event?
        //assertAdminEvents.assertEvent(REALM_NAME, OperationType.UPDATE, AdminEventPaths.authRaiseExecutionPath(exec.getId()));

        UserRepresentation user = findUser("test-user@localhost");
        userId = user.getId();

        user.singleAttribute("x509_certificate_identity","-");
        user.singleAttribute("alternative_email", "test-user-altmail@localhost");
        user.singleAttribute("upn", "test_upn_name@localhost");
        updateUser(user);
    }

    private AuthenticationExecutionInfoRepresentation addAssertExecution(AuthenticationFlowRepresentation flow, String providerId, String requirement) {
        AuthenticationExecutionRepresentation rep = new AuthenticationExecutionRepresentation();
        rep.setPriority(10);
        rep.setAuthenticator(providerId);
        rep.setRequirement(requirement);
        rep.setParentFlow(flow.getId());

        Response response = authMgmtResource.addExecution(rep);
        // TODO the following statement asserts, the actual value is null?
        //assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AssertAdminEvents.isExpectedPrefixFollowedByUuid(AdminEventPaths.authMgmtBasePath() + "/executions"), rep);
        try {
            Assert.assertEquals("added execution", 201, response.getStatus());
        } finally {
            response.close();
        }
        List<AuthenticationExecutionInfoRepresentation> executionReps = authMgmtResource.getExecutions(flow.getAlias());
        return findExecution(providerId, executionReps);
    }

    AuthenticationExecutionInfoRepresentation findExecution(String providerId, List<AuthenticationExecutionInfoRepresentation> reps) {
        for (AuthenticationExecutionInfoRepresentation exec : reps) {
            if (providerId.equals(exec.getProviderId())) {
                return exec;
            }
        }
        return null;
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

        ClientRepresentation app = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("resource-owner")
                .directAccessGrants()
                .secret("secret")
                .build();

        UserRepresentation user = UserBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .username("Keycloak")
                .email("localhost@localhost")
                .enabled(true)
                .password("password")
                .addAttribute("x509_issuer_identity", "Keycloak Intermediate CA")
                .build();

        userId2 = user.getId();

        ClientRepresentation client = findTestApp(testRealm);
        URI baseUri = URI.create(client.getRedirectUris().get(0));
        URI redir = URI.create("https://localhost:" + System.getProperty("auth.server.https.port", "8543") + baseUri.getRawPath());
        client.getRedirectUris().add(redir.toString());

        testRealm.setBruteForceProtected(true);
        testRealm.setFailureFactor(2);

        RealmBuilder.edit(testRealm)
                .user(user)
                .client(app);
    }

    AuthenticationFlowRepresentation createFlow(AuthenticationFlowRepresentation flowRep) {
        Response response = authMgmtResource.createFlow(flowRep);
        try {
            org.keycloak.testsuite.Assert.assertEquals(201, response.getStatus());
        }
        finally {
            response.close();
        }
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AssertAdminEvents.isExpectedPrefixFollowedByUuid(AdminEventPaths.authFlowsPath()), flowRep, ResourceType.AUTH_FLOW);

        for (AuthenticationFlowRepresentation flow : authMgmtResource.getFlows()) {
            if (flow.getAlias().equalsIgnoreCase(flowRep.getAlias())) {
                return flow;
            }
        }
        return null;
    }

    AuthenticationFlowRepresentation copyFlow(String existingFlow, String newFlow) {
        // copy that should succeed
        HashMap<String, String> params = new HashMap<>();
        params.put("newName", newFlow);
        Response response = authMgmtResource.copy(existingFlow, params);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, Encode.decode(AdminEventPaths.authCopyFlowPath(existingFlow)), params, ResourceType.AUTH_FLOW);
        try {
            Assert.assertEquals("Copy flow", 201, response.getStatus());
        } finally {
            response.close();
        }
        for (AuthenticationFlowRepresentation flow : authMgmtResource.getFlows()) {
            if (flow.getAlias().equalsIgnoreCase(newFlow)) {
                return flow;
            }
        }
        return null;
    }

    AuthenticationFlowRepresentation createDirectGrantFlow() {
        AuthenticationFlowRepresentation newFlow = newFlow("Copy-of-direct-grant", "desc", AuthenticationFlow.BASIC_FLOW, true, false);
        return createFlow(newFlow);
    }

    AuthenticationFlowRepresentation newFlow(String alias, String description,
                                             String providerId, boolean topLevel, boolean builtIn) {
        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        flow.setAlias(alias);
        flow.setDescription(description);
        flow.setProviderId(providerId);
        flow.setTopLevel(topLevel);
        flow.setBuiltIn(builtIn);
        return flow;
    }

    AuthenticationFlowRepresentation copyBrowserFlow() {

        RealmRepresentation realm = testRealm().toRepresentation();
        return copyFlow(realm.getBrowserFlow(), "Copy-of-browser");
    }

    void setBrowserFlow(AuthenticationFlowRepresentation flow) {
        RealmRepresentation realm = testRealm().toRepresentation();
        realm.setBrowserFlow(flow.getAlias());
        testRealm().update(realm);
    }

    void setDirectGrantFlow(AuthenticationFlowRepresentation flow) {
        RealmRepresentation realm = testRealm().toRepresentation();
        realm.setDirectGrantFlow(flow.getAlias());
        testRealm().update(realm);
    }

    static AuthenticatorConfigRepresentation newConfig(String alias, Map<String,String> params) {
        AuthenticatorConfigRepresentation config = new AuthenticatorConfigRepresentation();
        config.setAlias(alias);
        config.setConfig(params);
        return config;
    }

    protected String createConfig(String executionId, AuthenticatorConfigRepresentation cfg) {
        Response resp = authMgmtResource.newExecutionConfig(executionId, cfg);
        try {
            Assert.assertEquals(201, resp.getStatus());
        }
        finally {
            resp.close();
        }
        return ApiUtil.getCreatedId(resp);
    }

    protected static X509AuthenticatorConfigModel createLoginSubjectEmail2UsernameOrEmailConfig() {
        return new X509AuthenticatorConfigModel()
                .setConfirmationPageAllowed(true)
                .setMappingSourceType(SUBJECTDN_EMAIL)
                .setUserIdentityMapperType(USERNAME_EMAIL);
    }

    protected static X509AuthenticatorConfigModel createLoginSubjectAltNameEmail2UserAttributeConfig() {
        return new X509AuthenticatorConfigModel()
                .setConfirmationPageAllowed(true)
                .setMappingSourceType(SUBJECTALTNAME_EMAIL)
                .setUserIdentityMapperType(USER_ATTRIBUTE)
                .setCustomAttributeName("alternative_email");
    }

    protected static X509AuthenticatorConfigModel createLoginSubjectAltNameOtherName2UserAttributeConfig() {
        return new X509AuthenticatorConfigModel()
                .setConfirmationPageAllowed(true)
                .setMappingSourceType(SUBJECTALTNAME_OTHERNAME)
                .setUserIdentityMapperType(USER_ATTRIBUTE)
                .setCustomAttributeName("upn");
    }

    protected static X509AuthenticatorConfigModel createLoginSubjectEmailWithKeyUsage(String keyUsage) {
        return createLoginSubjectEmail2UsernameOrEmailConfig()
                .setKeyUsage(keyUsage);
    }

    protected static X509AuthenticatorConfigModel createLoginSubjectEmailWithExtendedKeyUsage(String extendedKeyUsage) {
        return createLoginSubjectEmail2UsernameOrEmailConfig()
                .setExtendedKeyUsage(extendedKeyUsage);
    }

    protected static X509AuthenticatorConfigModel createLoginSubjectEmailWithRevalidateCert(boolean revalidateCertEnabled) {
        return createLoginSubjectEmail2UsernameOrEmailConfig()
                .setRevalidateCertificateEnabled(revalidateCertEnabled);
    }

    protected static X509AuthenticatorConfigModel createLoginSubjectCN2UsernameOrEmailConfig() {
        return new X509AuthenticatorConfigModel()
                .setConfirmationPageAllowed(true)
                .setMappingSourceType(SUBJECTDN_CN)
                .setUserIdentityMapperType(USERNAME_EMAIL);
    }

    protected static X509AuthenticatorConfigModel createLoginWithSpecifiedSourceTypeToCustomAttributeConfig(X509AuthenticatorConfigModel.MappingSourceType sourceType, String userAttributeName) {
        return new X509AuthenticatorConfigModel()
                .setConfirmationPageAllowed(true)
                .setMappingSourceType(sourceType)
                .setUserIdentityMapperType(USER_ATTRIBUTE)
                .setCustomAttributeName(userAttributeName);
    }

    protected static X509AuthenticatorConfigModel createLoginIssuerDN_OU2CustomAttributeConfig() {
        return new X509AuthenticatorConfigModel()
                .setConfirmationPageAllowed(true)
                .setMappingSourceType(ISSUERDN)
                .setRegularExpression("O=(.*?)(?:,|$)")
                .setUserIdentityMapperType(USER_ATTRIBUTE)
                .setCustomAttributeName("x509_certificate_identity");
    }

    protected static X509AuthenticatorConfigModel createLoginSubjectDNToCustomAttributeConfig(boolean canonicalDnEnabled) {
        return new X509AuthenticatorConfigModel()
                .setConfirmationPageAllowed(true)
                .setCanonicalDnEnabled(canonicalDnEnabled)
                .setMappingSourceType(SUBJECTDN)
                .setRegularExpression("(.*?)(?:$)")
                .setUserIdentityMapperType(USER_ATTRIBUTE)
                .setCustomAttributeName("x509_certificate_identity");
    }

    protected static X509AuthenticatorConfigModel createLoginIssuerDNToCustomAttributeConfig(boolean canonicalDnEnabled) {
        return new X509AuthenticatorConfigModel()
                .setConfirmationPageAllowed(true)
                .setCanonicalDnEnabled(canonicalDnEnabled)
                .setMappingSourceType(ISSUERDN)
                .setRegularExpression("(.*?)(?:$)")
                .setUserIdentityMapperType(USER_ATTRIBUTE)
                .setCustomAttributeName("x509_certificate_identity");
    }

    protected void setUserEnabled(String userName, boolean enabled) {
        UserRepresentation user = findUser(userName);
        Assert.assertNotNull(user);

        user.setEnabled(enabled);

        updateUser(user);
    }


    public void replaceDefaultWebDriver(WebDriver driver) {
        this.driver = driver;
        DroneUtils.addWebDriver(driver);

        List<Field> allFields = new ArrayList<>();

        // Add all fields of this class and superclasses
        Class<?> testClass = this.getClass();
        while (AbstractX509AuthenticationTest.class.isAssignableFrom(testClass)) {
            allFields.addAll(Arrays.asList(testClass.getDeclaredFields()));
            allFields.addAll(Arrays.asList(testClass.getFields()));
            testClass = testClass.getSuperclass();
        }

        for (Field f : allFields) {
            if (f.getAnnotation(Page.class) != null) {
                try {
                    AbstractPage page = (AbstractPage) f.get(this);
                    page.setDriver(driver);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Could not replace the driver in " + f, e);
                }
            }
        }
    }


    protected void x509BrowserLogin(X509AuthenticatorConfigModel config, String userId, String username, String attemptedUsername) {

        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", config.getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assert.assertNotNull(cfgId);

        loginConfirmationPage.open();

        Assert.assertTrue(loginConfirmationPage.getSubjectDistinguishedNameText().startsWith("EMAILADDRESS=test-user@localhost"));
        Assert.assertEquals(username, loginConfirmationPage.getUsernameText());

        loginConfirmationPage.confirm();

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        AssertEvents.ExpectedEvent expectedEvent = events.expectLogin()
                .user(userId)
                .detail(Details.USERNAME, attemptedUsername)
                .removeDetail(Details.REDIRECT_URI);

        addX509CertificateDetails(expectedEvent)
                .assertEvent();
    }


    protected AssertEvents.ExpectedEvent addX509CertificateDetails(AssertEvents.ExpectedEvent expectedEvent) {
        return expectedEvent
                .detail(Details.X509_CERTIFICATE_SERIAL_NUMBER, Matchers.not(Matchers.isEmptyOrNullString()))
                .detail(Details.X509_CERTIFICATE_SUBJECT_DISTINGUISHED_NAME, Matchers.startsWith("EMAILADDRESS=test-user@localhost"))
                .detail(Details.X509_CERTIFICATE_ISSUER_DISTINGUISHED_NAME, Matchers.startsWith("EMAILADDRESS=contact@keycloak.org"));
    }
}
