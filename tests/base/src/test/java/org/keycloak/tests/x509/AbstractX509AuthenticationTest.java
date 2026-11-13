package org.keycloak.tests.x509;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.authenticators.x509.ValidateX509CertificateUsernameFactory;
import org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel;
import org.keycloak.authentication.authenticators.x509.X509ClientCertificateAuthenticatorFactory;
import org.keycloak.common.util.Encode;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.events.Details;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.https.CertificatesConfig;
import org.keycloak.testframework.https.CertificatesConfigBuilder;
import org.keycloak.testframework.https.InjectCertificates;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.X509IdentityConfirmationPage;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.i18n.RealmWithInternationalization;
import org.keycloak.tests.utils.admin.AdminEventPaths;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;
import org.keycloak.truststore.FileTruststoreProviderFactory;
import org.keycloak.truststore.TruststoreProvider;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USERNAME_EMAIL;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USER_ATTRIBUTE;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.ISSUERDN;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTDN_CN;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTDN_EMAIL;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.ALTERNATIVE;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.REQUIRED;

/**
 *
 * @author rmartinc
 */
public class AbstractX509AuthenticationTest {

    public static final String INTERMEDIATE_CA_CRL_PATH = "intermediate-ca.crl";

    @InjectRealm(config = X509RealmConfig.class)
    ManagedRealm managedRealm;

    @InjectCertificates(config = X509CertificatesEnabled.class)
    ManagedCertificates managedCertificates;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectEvents
    Events events;

    @InjectAdminEvents
    AdminEvents adminEvents;

    @InjectRunOnServer
    protected RunOnServerClient runOnServer;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectUser(config = X509UserConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedUser x509User;

    @InjectPage
    protected X509IdentityConfirmationPage loginConfirmationPage;

    @InjectPage
    protected LoginPage loginPage;

    protected static AuthenticationExecutionInfoRepresentation browserExecution;

    protected static AuthenticationExecutionInfoRepresentation directGrantExecution;

    @TestSetup
    public void configureTestRealm() {
        // configure the flows
        configureFlows();
        // disable user profile
        UserProfileUtil.enableUnmanagedAttributes(managedRealm.admin().users().userProfile());
    }

    private void configureFlows() {
        AuthenticationManagementResource authMgmtResource = managedRealm.admin().flows();

        AuthenticationFlowRepresentation browserFlow = copyBrowserFlow();
        Assertions.assertNotNull(browserFlow);

        AuthenticationFlowRepresentation directGrantFlow = createDirectGrantFlow();
        Assertions.assertNotNull(directGrantFlow);

        setBrowserFlow(browserFlow);
        Assertions.assertEquals(managedRealm.admin().toRepresentation().getBrowserFlow(), browserFlow.getAlias());

        setDirectGrantFlow(directGrantFlow);
        Assertions.assertEquals(managedRealm.admin().toRepresentation().getDirectGrantFlow(), directGrantFlow.getAlias());
        Assertions.assertEquals(0, directGrantFlow.getAuthenticationExecutions().size());

        // Add X509 cert authenticator to the direct grant flow
        directGrantExecution = addAssertExecution(directGrantFlow, ValidateX509CertificateUsernameFactory.PROVIDER_ID, REQUIRED.name());
        Assertions.assertNotNull(directGrantExecution);

        directGrantFlow = authMgmtResource.getFlow(directGrantFlow.getId());
        Assertions.assertNotNull(directGrantFlow.getAuthenticationExecutions());
        Assertions.assertEquals(1, directGrantFlow.getAuthenticationExecutions().size());

        // Add X509 authenticator to the browser flow
        browserExecution = addAssertExecution(browserFlow, X509ClientCertificateAuthenticatorFactory.PROVIDER_ID, ALTERNATIVE.name());
        Assertions.assertNotNull(browserExecution);

        // Raise the priority of the authenticator to position it right before
        // the Username/password authentication
        // TODO find a better, more explicit way to specify the position
        // of authenticator within the flow relative to other authenticators
        authMgmtResource.raisePriority(browserExecution.getId());
        // TODO raising the priority didn't generate the event?
        //assertAdminEvents.assertEvent(REALM_NAME, OperationType.UPDATE, AdminEventPaths.authRaiseExecutionPath(exec.getId()));
    }

    private AuthenticationExecutionInfoRepresentation addAssertExecution(AuthenticationFlowRepresentation flow, String providerId, String requirement) {
        AuthenticationManagementResource authMgmtResource = managedRealm.admin().flows();
        AuthenticationExecutionRepresentation rep = new AuthenticationExecutionRepresentation();
        rep.setPriority(10);
        rep.setAuthenticator(providerId);
        rep.setRequirement(requirement);
        rep.setParentFlow(flow.getId());

        // TODO the following statement asserts, the actual value is null?
        //assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AssertAdminEvents.isExpectedPrefixFollowedByUuid(AdminEventPaths.authMgmtBasePath() + "/executions"), rep);
        try (Response response = authMgmtResource.addExecution(rep)) {
            Assertions.assertEquals(201, response.getStatus(), "added execution");
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

    AuthenticationFlowRepresentation createFlow(AuthenticationFlowRepresentation flowRep) {
        AuthenticationManagementResource authMgmtResource = managedRealm.admin().flows();
        try (Response response = authMgmtResource.createFlow(flowRep)) {
            Assertions.assertEquals(201, response.getStatus());
            String id = ApiUtil.getCreatedId(response);
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE,
                AdminEventPaths.authFlowsPath() + "/" + id, flowRep, ResourceType.AUTH_FLOW);
        }

        for (AuthenticationFlowRepresentation flow : authMgmtResource.getFlows()) {
            if (flow.getAlias().equalsIgnoreCase(flowRep.getAlias())) {
                return flow;
            }
        }
        return null;
    }

    AuthenticationFlowRepresentation copyFlow(String existingFlow, String newFlow) {
        AuthenticationManagementResource authMgmtResource = managedRealm.admin().flows();
        // copy that should succeed
        HashMap<String, Object> params = new HashMap<>();
        params.put("newName", newFlow);
        try (Response response = authMgmtResource.copy(existingFlow, params)) {
            Assertions.assertEquals(201, response.getStatus(), "Copy flow");
        }
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, Encode.decode(AdminEventPaths.authCopyFlowPath(existingFlow)), params, ResourceType.AUTH_FLOW);
        for (AuthenticationFlowRepresentation flow : authMgmtResource.getFlows()) {
            if (flow.getAlias().equalsIgnoreCase(newFlow)) {
                return flow;
            }
        }
        return null;
    }

    AuthenticationFlowRepresentation copyBrowserFlow() {

        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        return copyFlow(realm.getBrowserFlow(), "Copy-of-browser");
    }

    void setBrowserFlow(AuthenticationFlowRepresentation flow) {
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        realm.setBrowserFlow(flow.getAlias());
        managedRealm.admin().update(realm);
    }

    void setDirectGrantFlow(AuthenticationFlowRepresentation flow) {
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        realm.setDirectGrantFlow(flow.getAlias());
        managedRealm.admin().update(realm);
    }

    protected static X509AuthenticatorConfigModel createLoginSubjectEmail2UsernameOrEmailConfig() {
        return new X509AuthenticatorConfigModel()
                .setConfirmationPageAllowed(true)
                .setMappingSourceType(SUBJECTDN_EMAIL)
                .setUserIdentityMapperType(USERNAME_EMAIL);
    }

    protected static X509AuthenticatorConfigModel createLoginSubjectEmailWithKeyUsage(String keyUsage) {
        return createLoginSubjectEmail2UsernameOrEmailConfig()
                .setKeyUsage(keyUsage);
    }

    protected static X509AuthenticatorConfigModel createLoginSubjectEmailWithExtendedKeyUsage(String extendedKeyUsage) {
        return createLoginSubjectEmail2UsernameOrEmailConfig()
                .setExtendedKeyUsage(extendedKeyUsage);
    }

    protected static X509AuthenticatorConfigModel createLoginSubjectEmailWithRevalidateCert(boolean revalidateCertEnabled, String... caSubjectDN) {
        return createLoginSubjectEmail2UsernameOrEmailConfig()
                .setCASubjectDN(caSubjectDN != null ? Arrays.asList(caSubjectDN) : null)
                .setRevalidateCertificateEnabled(revalidateCertEnabled);
    }

    protected static X509AuthenticatorConfigModel createLoginSubjectEmailWithRevalidateCert(String... caSubjectDN) {
        return createLoginSubjectEmail2UsernameOrEmailConfig()
                .setCASubjectDN(caSubjectDN != null ? Arrays.asList(caSubjectDN) : null);
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

    protected void disableTruststoreSpi() {
        runOnServer.run(session -> {
            ProviderFactory<TruststoreProvider> factory = session.getKeycloakSessionFactory().getProviderFactory(TruststoreProvider.class);
            if (factory instanceof FileTruststoreProviderFactory fileFactory) {
                fileFactory.setProvider(null);
            }
        });
    }

    protected void reenableTruststoreSpi() {
        runOnServer.run(session -> {
            ProviderFactory<TruststoreProvider> factory = session.getKeycloakSessionFactory().getProviderFactory(TruststoreProvider.class);
            if (factory instanceof FileTruststoreProviderFactory fileFactory) {
                fileFactory.init(Config.scope("truststore", fileFactory.getId()));
                Assertions.assertNotNull(fileFactory.create(session), "Truststore provider was not re-initialized");
            }
        });
    }

    static AuthenticatorConfigRepresentation newConfig(String alias, Map<String,String> params) {
        AuthenticatorConfigRepresentation config = new AuthenticatorConfigRepresentation();
        config.setAlias(alias);
        config.setConfig(params);
        return config;
    }

    protected String createConfig(String executionId, AuthenticatorConfigRepresentation cfg) {
        try (Response resp = managedRealm.admin().flows().newExecutionConfig(executionId, cfg)) {
            Assertions.assertEquals(201, resp.getStatus());
            return ApiUtil.getCreatedId(resp);
        }
    }

    protected void removeConfig(String configId) {
        managedRealm.admin().flows().removeAuthenticatorConfig(configId);
    }

    protected static X509AuthenticatorConfigModel createLoginIssuerDN_OU2CustomAttributeConfig() {
        return new X509AuthenticatorConfigModel()
                .setConfirmationPageAllowed(true)
                .setMappingSourceType(ISSUERDN)
                .setRegularExpression("O=(.*?)(?:,|$)")
                .setUserIdentityMapperType(USER_ATTRIBUTE)
                .setCustomAttributeName("x509_certificate_identity");
    }

    protected void x509BrowserLogin(X509AuthenticatorConfigModel config, String userId, String username, String attemptedUsername) {

        AuthenticatorConfigRepresentation cfg = newConfig("x509-browser-config", config.getConfig());
        String cfgId = createConfig(browserExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        oauth.openLoginForm();

        Assertions.assertTrue(loginConfirmationPage.getSubjectDistinguishedNameText().startsWith("EMAILADDRESS=test-user@localhost"));
        Assertions.assertEquals(username, loginConfirmationPage.getUsernameText());

        loginConfirmationPage.confirm();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        EventRepresentation eventRep = events.poll();
        EventAssertion.expectLoginSuccess(eventRep)
                .userId(userId)
                .details(Details.USERNAME, attemptedUsername);

        MatcherAssert.assertThat(eventRep.getDetails().get(Details.X509_CERTIFICATE_SERIAL_NUMBER), Matchers.not(Matchers.is(Matchers.emptyOrNullString())));
        MatcherAssert.assertThat(eventRep.getDetails().get(Details.X509_CERTIFICATE_SUBJECT_DISTINGUISHED_NAME), Matchers.startsWith("EMAILADDRESS=test-user@localhost"));
        MatcherAssert.assertThat(eventRep.getDetails().get(Details.X509_CERTIFICATE_ISSUER_DISTINGUISHED_NAME), Matchers.startsWith("EMAILADDRESS=contact@keycloak.org"));
    }

    private static class X509CertificatesEnabled implements CertificatesConfig {

        @Override
        public CertificatesConfigBuilder configure(CertificatesConfigBuilder config) {
            // use pem file that would use the normal trust-store
            return config
                    .tlsEnabled(true)
                    .mTlsEnabled(true)
                    .keystoreFormat(KeystoreUtil.KeystoreFormat.BCFKS)
                    .stores("keycloak.bcfks", "keycloak-truststore.pem", "client.bcfks", "keycloak-truststore.bcfks");
        }
    }

    protected static class X509RealmConfig extends RealmWithInternationalization {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            super.configure(realm);
            return realm;
        }
    }

    private static class X509UserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder config) {
            return config.username("test-user@localhost")
                    .password("password")
                    .emailVerified(true)
                    .name("Tom", "Brady")
                    .email("test-user@localhost")
                    .attribute("x509_certificate_identity", "-")
                    .attribute("alternative_email", "test-user-altmail@localhost")
                    .attribute("upn", "test_upn_name@localhost");
        }
    }

}
