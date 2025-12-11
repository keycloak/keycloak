package org.keycloak.protocol.saml;

import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.common.Profile;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.http.HttpRequest;
import org.keycloak.keys.DefaultKeyManager;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.ClientInitialAccessModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuth2DeviceConfig;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.ParConfig;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.WebAuthnPolicy;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.DefaultKeycloakSessionFactory;
import org.keycloak.services.resteasy.HttpRequestImpl;
import org.keycloak.services.resteasy.ResteasyKeycloakSession;
import org.keycloak.services.resteasy.ResteasyKeycloakSessionFactory;

import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.junit.BeforeClass;
import org.junit.Test;

import static  org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class SamlProtocolTest {

    static {
        try {
            KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
            rsa.initialize(2048);
            rsaKeyPair = rsa.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
    private static final KeyPair rsaKeyPair;

    // reference RedirectUtilsTest
    private static KeycloakSession session;

    @BeforeClass
    public static void beforeClass() {
        HttpRequest httpRequest = new HttpRequestImpl(MockHttpRequest.create("GET", URI.create("https://keycloak.org/"), URI.create("https://keycloak.org")));
        ResteasyContext.getContextDataMap().put(HttpRequest.class, httpRequest);
        Profile.defaults();
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
        DefaultKeycloakSessionFactory sessionFactory = new ResteasyKeycloakSessionFactory();
        sessionFactory.init();
        session = new ResteasyKeycloakSession(sessionFactory) {
            @Override
            public KeyManager keys() {
                return new DefaultKeyManager(null) {
                    @Override
                    public ActiveRsaKey getActiveRsaKey(RealmModel realm) {
                        KeyWrapper key = new KeyWrapper();
                        key.setProviderId("dummy");
                        key.setKid("1234");
                        key.setAlgorithm(Algorithm.RS256);
                        key.setType(KeyType.RSA);
                        key.setUse(KeyUse.SIG);
                        key.setStatus(KeyStatus.ACTIVE);
                        key.setPrivateKey(rsaKeyPair.getPrivate());
                        key.setPublicKey(rsaKeyPair.getPublic());
                        return new ActiveRsaKey(key);
                    }
                };
            }
        };
        session.getContext().setHttpRequest(httpRequest);
    }


    @Test
    public void testGetLogoutServiceUrl() {
        TestClientModel client = new TestClientModel();

        String logoutServiceUrl = SamlProtocol.getLogoutServiceUrl(session, client, SamlProtocol.SAML_ARTIFACT_BINDING, false);
        assertNull( logoutServiceUrl);

        client.defineSetLogoutUrls(true, true, true, true);
        logoutServiceUrl = SamlProtocol.getLogoutServiceUrl(session, client, SamlProtocol.SAML_ARTIFACT_BINDING, false);
        assertEquals("saml-logout-artifact-attribute", logoutServiceUrl);

        logoutServiceUrl = SamlProtocol.getLogoutServiceUrl(session, client, SamlProtocol.SAML_ARTIFACT_BINDING, true);
        assertEquals(logoutServiceUrl, "saml-logout-redirect-attribute");

        logoutServiceUrl = SamlProtocol.getLogoutServiceUrl(session, client, SamlProtocol.SAML_SOAP_BINDING, false);
        assertEquals(logoutServiceUrl, "saml-logout-soap-attribute");

        logoutServiceUrl = SamlProtocol.getLogoutServiceUrl(session, client, SamlProtocol.SAML_POST_BINDING, false);
        assertEquals(logoutServiceUrl, "saml-logout-post-attribute");

        logoutServiceUrl = SamlProtocol.getLogoutServiceUrl(session, client, SamlProtocol.SAML_REDIRECT_BINDING, false);
        assertEquals(logoutServiceUrl, "saml-logout-redirect-attribute");

    }

    @Test
    public void testGetLogoutBindingTypeForClientSession() {
        TestClientModel client = new TestClientModel();
        UserSessionModel userSession = new TestUserSessionModel();
        AuthenticatedClientSessionModel clientSession = new TestAuthenticatedClientSessionModel(client, userSession);

        // default REDIRECT
        String bindingType = SamlProtocol.getLogoutBindingTypeForClientSession(clientSession);
        assertEquals(SamlProtocol.SAML_REDIRECT_BINDING, bindingType);

        // default to POST if forced with no url
        client.setForcePostBinding(true);
        assertEquals(SamlProtocol.SAML_REDIRECT_BINDING, bindingType);

        client.setForcePostBinding(false);
        clientSession.setNote(SamlProtocol.SAML_BINDING, SamlProtocol.SAML_ARTIFACT_BINDING);
        client.defineSetLogoutUrls(false, false, true, false);
        bindingType = SamlProtocol.getLogoutBindingTypeForClientSession(clientSession);
        clientSession.removeNote(SamlProtocol.SAML_BINDING);
        assertEquals(SamlProtocol.SAML_ARTIFACT_BINDING, bindingType);

        client.defineSetLogoutUrls(true, false, false, false);
        bindingType = SamlProtocol.getLogoutBindingTypeForClientSession(clientSession);
        assertEquals(SamlProtocol.SAML_POST_BINDING, bindingType);

        client.defineSetLogoutUrls(false, false, false, true);
        bindingType = SamlProtocol.getLogoutBindingTypeForClientSession(clientSession);
        assertEquals(SamlProtocol.SAML_REDIRECT_BINDING, bindingType);

        client.setForceArtifactBinding(true);
        client.defineSetLogoutUrls(false, false, true, false);
        bindingType = SamlProtocol.getLogoutBindingTypeForClientSession(clientSession);
        assertEquals(SamlProtocol.SAML_ARTIFACT_BINDING, bindingType);
    }

    @Test
    public void frontchannelLogoutSignsLogoutRequestsEvenThoughArtifactWasUsed() {
        TestClientModel client = new TestClientModel();
        // dont force a binding and fallback to redirect behaviour
        client.setForceArtifactBinding(false);
        client.setForcePostBinding(false);
        client.defineSetLogoutUrls(false, false, false, true);

        RealmModel realm = new TestRealmModel();
        UriInfo uriInfo = new ResteasyUriInfo("http://localhost:8080", "/");

        SamlProtocol protocol = new SamlProtocol();
        protocol.setSession(session);
        protocol.setUriInfo(uriInfo);
        protocol.setRealm(realm);

        UserSessionModel userSession = new TestUserSessionModel();
        AuthenticatedClientSessionModel clientSession = new TestAuthenticatedClientSessionModel(client, userSession);
        // store, that Artifact binding hsa been used for login
        clientSession.setNote(SamlProtocol.SAML_BINDING, SamlProtocol.SAML_ARTIFACT_BINDING);

        try (Response response = protocol.frontchannelLogout(userSession, clientSession)) {
            assertEquals(302, response.getStatus());
            assertThat(response.getHeaderString("location"), containsString("&Signature="));
        }
    }

    private class TestRealmModel implements RealmModel {
        @Override
        public String getId() {
            return null;
        }

        @Override
        public String getName() {
            return "test-realm";
        }

        @Override
        public void setName(String name) {

        }

        @Override
        public String getDisplayName() {
            return null;
        }

        @Override
        public void setDisplayName(String displayName) {

        }

        @Override
        public String getDisplayNameHtml() {
            return null;
        }

        @Override
        public void setDisplayNameHtml(String displayNameHtml) {

        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public void setEnabled(boolean enabled) {

        }

        @Override
        public SslRequired getSslRequired() {
            return null;
        }

        @Override
        public void setSslRequired(SslRequired sslRequired) {

        }

        @Override
        public boolean isRegistrationAllowed() {
            return false;
        }

        @Override
        public void setRegistrationAllowed(boolean registrationAllowed) {

        }

        @Override
        public boolean isRegistrationEmailAsUsername() {
            return false;
        }

        @Override
        public void setRegistrationEmailAsUsername(boolean registrationEmailAsUsername) {

        }

        @Override
        public boolean isRememberMe() {
            return false;
        }

        @Override
        public void setRememberMe(boolean rememberMe) {

        }

        @Override
        public boolean isEditUsernameAllowed() {
            return false;
        }

        @Override
        public void setEditUsernameAllowed(boolean editUsernameAllowed) {

        }

        @Override
        public boolean isUserManagedAccessAllowed() {
            return false;
        }

        @Override
        public void setUserManagedAccessAllowed(boolean userManagedAccessAllowed) {

        }

        @Override
        public void setAttribute(String name, String value) {

        }

        @Override
        public void removeAttribute(String name) {

        }

        @Override
        public String getAttribute(String name) {
            return null;
        }

        @Override
        public Map<String, String> getAttributes() {
            return null;
        }

        @Override
        public boolean isBruteForceProtected() {
            return false;
        }

        @Override
        public void setBruteForceProtected(boolean value) {

        }

        @Override
        public boolean isPermanentLockout() {
            return false;
        }

        @Override
        public void setPermanentLockout(boolean val) {

        }

        @Override
        public int getMaxTemporaryLockouts() {
            return 0;
        }

        @Override
        public void setMaxTemporaryLockouts(int val) {

        }

        @Override
        public RealmRepresentation.BruteForceStrategy getBruteForceStrategy() {
            return null;
        }

        @Override
        public void setBruteForceStrategy(RealmRepresentation.BruteForceStrategy val) {

        }

        @Override
        public int getMaxFailureWaitSeconds() {
            return 0;
        }

        @Override
        public void setMaxFailureWaitSeconds(int val) {

        }

        @Override
        public int getWaitIncrementSeconds() {
            return 0;
        }

        @Override
        public void setWaitIncrementSeconds(int val) {

        }

        @Override
        public int getMinimumQuickLoginWaitSeconds() {
            return 0;
        }

        @Override
        public void setMinimumQuickLoginWaitSeconds(int val) {

        }

        @Override
        public long getQuickLoginCheckMilliSeconds() {
            return 0;
        }

        @Override
        public void setQuickLoginCheckMilliSeconds(long val) {

        }

        @Override
        public int getMaxDeltaTimeSeconds() {
            return 0;
        }

        @Override
        public void setMaxDeltaTimeSeconds(int val) {

        }

        @Override
        public int getFailureFactor() {
            return 0;
        }

        @Override
        public void setFailureFactor(int failureFactor) {

        }

        @Override
        public boolean isVerifyEmail() {
            return false;
        }

        @Override
        public void setVerifyEmail(boolean verifyEmail) {

        }

        @Override
        public boolean isLoginWithEmailAllowed() {
            return false;
        }

        @Override
        public void setLoginWithEmailAllowed(boolean loginWithEmailAllowed) {

        }

        @Override
        public boolean isDuplicateEmailsAllowed() {
            return false;
        }

        @Override
        public void setDuplicateEmailsAllowed(boolean duplicateEmailsAllowed) {

        }

        @Override
        public boolean isResetPasswordAllowed() {
            return false;
        }

        @Override
        public void setResetPasswordAllowed(boolean resetPasswordAllowed) {

        }

        @Override
        public String getDefaultSignatureAlgorithm() {
            return null;
        }

        @Override
        public void setDefaultSignatureAlgorithm(String defaultSignatureAlgorithm) {

        }

        @Override
        public boolean isRevokeRefreshToken() {
            return false;
        }

        @Override
        public void setRevokeRefreshToken(boolean revokeRefreshToken) {

        }

        @Override
        public int getRefreshTokenMaxReuse() {
            return 0;
        }

        @Override
        public void setRefreshTokenMaxReuse(int revokeRefreshTokenCount) {

        }

        @Override
        public int getSsoSessionIdleTimeout() {
            return 0;
        }

        @Override
        public void setSsoSessionIdleTimeout(int seconds) {

        }

        @Override
        public int getSsoSessionMaxLifespan() {
            return 0;
        }

        @Override
        public void setSsoSessionMaxLifespan(int seconds) {

        }

        @Override
        public int getSsoSessionIdleTimeoutRememberMe() {
            return 0;
        }

        @Override
        public void setSsoSessionIdleTimeoutRememberMe(int seconds) {

        }

        @Override
        public int getSsoSessionMaxLifespanRememberMe() {
            return 0;
        }

        @Override
        public void setSsoSessionMaxLifespanRememberMe(int seconds) {

        }

        @Override
        public int getOfflineSessionIdleTimeout() {
            return 0;
        }

        @Override
        public void setOfflineSessionIdleTimeout(int seconds) {

        }

        @Override
        public int getAccessTokenLifespan() {
            return 0;
        }

        @Override
        public boolean isOfflineSessionMaxLifespanEnabled() {
            return false;
        }

        @Override
        public void setOfflineSessionMaxLifespanEnabled(boolean offlineSessionMaxLifespanEnabled) {

        }

        @Override
        public int getOfflineSessionMaxLifespan() {
            return 0;
        }

        @Override
        public void setOfflineSessionMaxLifespan(int seconds) {

        }

        @Override
        public int getClientSessionIdleTimeout() {
            return 0;
        }

        @Override
        public void setClientSessionIdleTimeout(int seconds) {

        }

        @Override
        public int getClientSessionMaxLifespan() {
            return 0;
        }

        @Override
        public void setClientSessionMaxLifespan(int seconds) {

        }

        @Override
        public int getClientOfflineSessionIdleTimeout() {
            return 0;
        }

        @Override
        public void setClientOfflineSessionIdleTimeout(int seconds) {

        }

        @Override
        public int getClientOfflineSessionMaxLifespan() {
            return 0;
        }

        @Override
        public void setClientOfflineSessionMaxLifespan(int seconds) {

        }

        @Override
        public void setAccessTokenLifespan(int seconds) {

        }

        @Override
        public int getAccessTokenLifespanForImplicitFlow() {
            return 0;
        }

        @Override
        public void setAccessTokenLifespanForImplicitFlow(int seconds) {

        }

        @Override
        public int getAccessCodeLifespan() {
            return 0;
        }

        @Override
        public void setAccessCodeLifespan(int seconds) {

        }

        @Override
        public int getAccessCodeLifespanUserAction() {
            return 0;
        }

        @Override
        public void setAccessCodeLifespanUserAction(int seconds) {

        }

        @Override
        public OAuth2DeviceConfig getOAuth2DeviceConfig() {
            return null;
        }

        @Override
        public CibaConfig getCibaPolicy() {
            return null;
        }

        @Override
        public ParConfig getParPolicy() {
            return null;
        }

        @Override
        public Map<String, Integer> getUserActionTokenLifespans() {
            return null;
        }

        @Override
        public int getAccessCodeLifespanLogin() {
            return 0;
        }

        @Override
        public void setAccessCodeLifespanLogin(int seconds) {

        }

        @Override
        public int getActionTokenGeneratedByAdminLifespan() {
            return 0;
        }

        @Override
        public void setActionTokenGeneratedByAdminLifespan(int seconds) {

        }

        @Override
        public int getActionTokenGeneratedByUserLifespan() {
            return 0;
        }

        @Override
        public void setActionTokenGeneratedByUserLifespan(int seconds) {

        }

        @Override
        public int getActionTokenGeneratedByUserLifespan(String actionTokenType) {
            return 0;
        }

        @Override
        public void setActionTokenGeneratedByUserLifespan(String actionTokenType, Integer seconds) {

        }

        @Override
        public Stream<RequiredCredentialModel> getRequiredCredentialsStream() {
            return null;
        }

        @Override
        public void addRequiredCredential(String cred) {

        }

        @Override
        public PasswordPolicy getPasswordPolicy() {
            return null;
        }

        @Override
        public void setPasswordPolicy(PasswordPolicy policy) {

        }

        @Override
        public OTPPolicy getOTPPolicy() {
            return null;
        }

        @Override
        public void setOTPPolicy(OTPPolicy policy) {

        }

        @Override
        public WebAuthnPolicy getWebAuthnPolicy() {
            return null;
        }

        @Override
        public void setWebAuthnPolicy(WebAuthnPolicy policy) {

        }

        @Override
        public WebAuthnPolicy getWebAuthnPolicyPasswordless() {
            return null;
        }

        @Override
        public void setWebAuthnPolicyPasswordless(WebAuthnPolicy policy) {

        }

        @Override
        public RoleModel getRoleById(String id) {
            return null;
        }

        @Override
        public Stream<GroupModel> getDefaultGroupsStream() {
            return null;
        }

        @Override
        public void addDefaultGroup(GroupModel group) {

        }

        @Override
        public void removeDefaultGroup(GroupModel group) {

        }

        @Override
        public Stream<ClientModel> getClientsStream() {
            return null;
        }

        @Override
        public Stream<ClientModel> getClientsStream(Integer firstResult, Integer maxResults) {
            return null;
        }

        @Override
        public Long getClientsCount() {
            return null;
        }

        @Override
        public Stream<ClientModel> getAlwaysDisplayInConsoleClientsStream() {
            return null;
        }

        @Override
        public ClientModel addClient(String name) {
            return null;
        }

        @Override
        public ClientModel addClient(String id, String clientId) {
            return null;
        }

        @Override
        public boolean removeClient(String id) {
            return false;
        }

        @Override
        public ClientModel getClientById(String id) {
            return null;
        }

        @Override
        public ClientModel getClientByClientId(String clientId) {
            return null;
        }

        @Override
        public Stream<ClientModel> searchClientByClientIdStream(String clientId, Integer firstResult, Integer maxResults) {
            return null;
        }

        @Override
        public Stream<ClientModel> searchClientByAttributes(Map<String, String> attributes, Integer firstResult, Integer maxResults) {
            return null;
        }

        @Override
        public Stream<ClientModel> searchClientByAuthenticationFlowBindingOverrides(Map<String, String> overrides, Integer firstResult, Integer maxResults) {
            return null;
        }

        @Override
        public void updateRequiredCredentials(Set<String> creds) {

        }

        @Override
        public Map<String, String> getBrowserSecurityHeaders() {
            return null;
        }

        @Override
        public void setBrowserSecurityHeaders(Map<String, String> headers) {

        }

        @Override
        public Map<String, String> getSmtpConfig() {
            return null;
        }

        @Override
        public void setSmtpConfig(Map<String, String> smtpConfig) {

        }

        @Override
        public AuthenticationFlowModel getBrowserFlow() {
            return null;
        }

        @Override
        public void setBrowserFlow(AuthenticationFlowModel flow) {

        }

        @Override
        public AuthenticationFlowModel getRegistrationFlow() {
            return null;
        }

        @Override
        public void setRegistrationFlow(AuthenticationFlowModel flow) {

        }

        @Override
        public AuthenticationFlowModel getDirectGrantFlow() {
            return null;
        }

        @Override
        public void setDirectGrantFlow(AuthenticationFlowModel flow) {

        }

        @Override
        public AuthenticationFlowModel getResetCredentialsFlow() {
            return null;
        }

        @Override
        public void setResetCredentialsFlow(AuthenticationFlowModel flow) {

        }

        @Override
        public AuthenticationFlowModel getClientAuthenticationFlow() {
            return null;
        }

        @Override
        public void setClientAuthenticationFlow(AuthenticationFlowModel flow) {

        }

        @Override
        public AuthenticationFlowModel getDockerAuthenticationFlow() {
            return null;
        }

        @Override
        public void setDockerAuthenticationFlow(AuthenticationFlowModel flow) {

        }

        @Override
        public AuthenticationFlowModel getFirstBrokerLoginFlow() {
            return null;
        }

        @Override
        public Stream<AuthenticationFlowModel> getAuthenticationFlowsStream() {
            return null;
        }

        @Override
        public AuthenticationFlowModel getFlowByAlias(String alias) {
            return null;
        }

        @Override
        public AuthenticationFlowModel addAuthenticationFlow(AuthenticationFlowModel model) {
            return null;
        }

        @Override
        public AuthenticationFlowModel getAuthenticationFlowById(String id) {
            return null;
        }

        @Override
        public void removeAuthenticationFlow(AuthenticationFlowModel model) {

        }

        @Override
        public void updateAuthenticationFlow(AuthenticationFlowModel model) {

        }

        @Override
        public Stream<AuthenticationExecutionModel> getAuthenticationExecutionsStream(String flowId) {
            return null;
        }

        @Override
        public AuthenticationExecutionModel getAuthenticationExecutionById(String id) {
            return null;
        }

        @Override
        public AuthenticationExecutionModel getAuthenticationExecutionByFlowId(String flowId) {
            return null;
        }

        @Override
        public AuthenticationExecutionModel addAuthenticatorExecution(AuthenticationExecutionModel model) {
            return null;
        }

        @Override
        public void updateAuthenticatorExecution(AuthenticationExecutionModel model) {

        }

        @Override
        public void removeAuthenticatorExecution(AuthenticationExecutionModel model) {

        }

        @Override
        public Stream<AuthenticatorConfigModel> getAuthenticatorConfigsStream() {
            return null;
        }

        @Override
        public AuthenticatorConfigModel addAuthenticatorConfig(AuthenticatorConfigModel model) {
            return null;
        }

        @Override
        public void updateAuthenticatorConfig(AuthenticatorConfigModel model) {

        }

        @Override
        public void removeAuthenticatorConfig(AuthenticatorConfigModel model) {

        }

        @Override
        public AuthenticatorConfigModel getAuthenticatorConfigById(String id) {
            return null;
        }

        @Override
        public AuthenticatorConfigModel getAuthenticatorConfigByAlias(String alias) {
            return null;
        }

        @Override
        public Stream<RequiredActionProviderModel> getRequiredActionProvidersStream() {
            return null;
        }

        @Override
        public RequiredActionProviderModel addRequiredActionProvider(RequiredActionProviderModel model) {
            return null;
        }

        @Override
        public void updateRequiredActionProvider(RequiredActionProviderModel model) {

        }

        @Override
        public void removeRequiredActionProvider(RequiredActionProviderModel model) {

        }

        @Override
        public RequiredActionProviderModel getRequiredActionProviderById(String id) {
            return null;
        }

        @Override
        public RequiredActionProviderModel getRequiredActionProviderByAlias(String alias) {
            return null;
        }

        @Override
        public Stream<IdentityProviderModel> getIdentityProvidersStream() {
            return null;
        }

        @Override
        public IdentityProviderModel getIdentityProviderByAlias(String alias) {
            return null;
        }

        @Override
        public void addIdentityProvider(IdentityProviderModel identityProvider) {

        }

        @Override
        public void removeIdentityProviderByAlias(String alias) {

        }

        @Override
        public void updateIdentityProvider(IdentityProviderModel identityProvider) {

        }

        @Override
        public Stream<IdentityProviderMapperModel> getIdentityProviderMappersStream() {
            return null;
        }

        @Override
        public Stream<IdentityProviderMapperModel> getIdentityProviderMappersByAliasStream(String brokerAlias) {
            return null;
        }

        @Override
        public IdentityProviderMapperModel addIdentityProviderMapper(IdentityProviderMapperModel model) {
            return null;
        }

        @Override
        public void removeIdentityProviderMapper(IdentityProviderMapperModel mapping) {

        }

        @Override
        public void updateIdentityProviderMapper(IdentityProviderMapperModel mapping) {

        }

        @Override
        public IdentityProviderMapperModel getIdentityProviderMapperById(String id) {
            return null;
        }

        @Override
        public IdentityProviderMapperModel getIdentityProviderMapperByName(String brokerAlias, String name) {
            return null;
        }

        @Override
        public ComponentModel addComponentModel(ComponentModel model) {
            return null;
        }

        @Override
        public ComponentModel importComponentModel(ComponentModel model) {
            return null;
        }

        @Override
        public void updateComponent(ComponentModel component) {

        }

        @Override
        public void removeComponent(ComponentModel component) {

        }

        @Override
        public void removeComponents(String parentId) {

        }

        @Override
        public Stream<ComponentModel> getComponentsStream(String parentId, String providerType) {
            return null;
        }

        @Override
        public Stream<ComponentModel> getComponentsStream(String parentId) {
            return null;
        }

        @Override
        public Stream<ComponentModel> getComponentsStream() {
            return null;
        }

        @Override
        public ComponentModel getComponent(String id) {
            return null;
        }

        @Override
        public String getLoginTheme() {
            return null;
        }

        @Override
        public void setLoginTheme(String name) {

        }

        @Override
        public String getAccountTheme() {
            return null;
        }

        @Override
        public void setAccountTheme(String name) {

        }

        @Override
        public String getAdminTheme() {
            return null;
        }

        @Override
        public void setAdminTheme(String name) {

        }

        @Override
        public String getEmailTheme() {
            return null;
        }

        @Override
        public void setEmailTheme(String name) {

        }

        @Override
        public int getNotBefore() {
            return 0;
        }

        @Override
        public void setNotBefore(int notBefore) {

        }

        @Override
        public boolean isEventsEnabled() {
            return false;
        }

        @Override
        public void setEventsEnabled(boolean enabled) {

        }

        @Override
        public long getEventsExpiration() {
            return 0;
        }

        @Override
        public void setEventsExpiration(long expiration) {

        }

        @Override
        public Stream<String> getEventsListenersStream() {
            return null;
        }

        @Override
        public void setEventsListeners(Set<String> listeners) {

        }

        @Override
        public Stream<String> getEnabledEventTypesStream() {
            return null;
        }

        @Override
        public void setEnabledEventTypes(Set<String> enabledEventTypes) {

        }

        @Override
        public boolean isAdminEventsEnabled() {
            return false;
        }

        @Override
        public void setAdminEventsEnabled(boolean enabled) {

        }

        @Override
        public boolean isAdminEventsDetailsEnabled() {
            return false;
        }

        @Override
        public void setAdminEventsDetailsEnabled(boolean enabled) {

        }

        @Override
        public ClientModel getMasterAdminClient() {
            return null;
        }

        @Override
        public void setMasterAdminClient(ClientModel client) {

        }

        @Override
        public RoleModel getDefaultRole() {
            return null;
        }

        @Override
        public void setDefaultRole(RoleModel role) {

        }

        @Override
        public ClientModel getAdminPermissionsClient() {
            return null;
        }

        @Override
        public void setAdminPermissionsClient(ClientModel client) {

        }

        @Override
        public boolean isIdentityFederationEnabled() {
            return false;
        }

        @Override
        public boolean isInternationalizationEnabled() {
            return false;
        }

        @Override
        public void setInternationalizationEnabled(boolean enabled) {

        }

        @Override
        public Stream<String> getSupportedLocalesStream() {
            return null;
        }

        @Override
        public void setSupportedLocales(Set<String> locales) {

        }

        @Override
        public String getDefaultLocale() {
            return null;
        }

        @Override
        public void setDefaultLocale(String locale) {

        }

        @Override
        public GroupModel createGroup(String id, String name, GroupModel toParent) {
            return null;
        }

        @Override
        public GroupModel getGroupById(String id) {
            return null;
        }

        @Override
        public Stream<GroupModel> getGroupsStream() {
            return null;
        }

        @Override
        public Long getGroupsCount(Boolean onlyTopGroups) {
            return null;
        }

        @Override
        public Long getGroupsCountByNameContaining(String search) {
            return null;
        }

        @Override
        public Stream<GroupModel> getTopLevelGroupsStream() {
            return null;
        }

        @Override
        public Stream<GroupModel> getTopLevelGroupsStream(Integer first, Integer max) {
            return null;
        }

        @Override
        public boolean removeGroup(GroupModel group) {
            return false;
        }

        @Override
        public void moveGroup(GroupModel group, GroupModel toParent) {

        }

        @Override
        public Stream<ClientScopeModel> getClientScopesStream() {
            return null;
        }

        @Override
        public ClientScopeModel addClientScope(String name) {
            return null;
        }

        @Override
        public ClientScopeModel addClientScope(String id, String name) {
            return null;
        }

        @Override
        public boolean removeClientScope(String id) {
            return false;
        }

        @Override
        public ClientScopeModel getClientScopeById(String id) {
            return null;
        }

        @Override
        public void addDefaultClientScope(ClientScopeModel clientScope, boolean defaultScope) {

        }

        @Override
        public void removeDefaultClientScope(ClientScopeModel clientScope) {

        }

        @Override
        public void createOrUpdateRealmLocalizationTexts(String locale, Map<String, String> localizationTexts) {

        }

        @Override
        public boolean removeRealmLocalizationTexts(String locale) {
            return false;
        }

        @Override
        public Map<String, Map<String, String>> getRealmLocalizationTexts() {
            return null;
        }

        @Override
        public Map<String, String> getRealmLocalizationTextsByLocale(String locale) {
            return null;
        }

        @Override
        public Stream<ClientScopeModel> getDefaultClientScopesStream(boolean defaultScope) {
            return null;
        }

        @Override
        public ClientInitialAccessModel createClientInitialAccessModel(int expiration, int count) {
            return null;
        }

        @Override
        public ClientInitialAccessModel getClientInitialAccessModel(String id) {
            return null;
        }

        @Override
        public void removeClientInitialAccessModel(String id) {

        }

        @Override
        public Stream<ClientInitialAccessModel> getClientInitialAccesses() {
            return null;
        }

        @Override
        public void decreaseRemainingCount(ClientInitialAccessModel clientInitialAccess) {

        }

        @Override
        public RoleModel getRole(String name) {
            return null;
        }

        @Override
        public RoleModel addRole(String name) {
            return null;
        }

        @Override
        public RoleModel addRole(String id, String name) {
            return null;
        }

        @Override
        public boolean removeRole(RoleModel role) {
            return false;
        }

        @Override
        public Stream<RoleModel> getRolesStream() {
            return null;
        }

        @Override
        public Stream<RoleModel> getRolesStream(Integer firstResult, Integer maxResults) {
            return null;
        }

        @Override
        public Stream<RoleModel> searchForRolesStream(String search, Integer first, Integer max) {
            return null;
        }

        @Override
        public void setFirstBrokerLoginFlow(org.keycloak.models.AuthenticationFlowModel flow) {}

        @Override
        public boolean isOrganizationsEnabled() {
            return false;
        }

        @Override
        public void setOrganizationsEnabled(boolean organizationsEnabled) {}

        @Override
        public boolean isAdminPermissionsEnabled() {
            return false;
        }

        @Override
        public void setAdminPermissionsEnabled(boolean adminPermissionsEnabled) {
            // noop
        }

        @Override
        public boolean isVerifiableCredentialsEnabled() {
            return false;
        }

        @Override
        public void setVerifiableCredentialsEnabled(boolean verifiableCredentialsEnabled) {
            // noop
        }

        @Override
        public RequiredActionConfigModel getRequiredActionConfigById(String id) {
            return null;
        }

        @Override
        public RequiredActionConfigModel getRequiredActionConfigByAlias(String alias) {
            return null;
        }

        @Override
        public void removeRequiredActionProviderConfig(RequiredActionConfigModel model) {}

        @Override
        public void updateRequiredActionConfig(RequiredActionConfigModel model) {}

        @Override
        public Stream<RequiredActionConfigModel> getRequiredActionConfigsStream() {
            return null;
        }
    }

    private class TestAuthenticatedClientSessionModel implements AuthenticatedClientSessionModel {
        private ClientModel client;
        private UserSessionModel userSession;

        private Map<String, String> notes;
        public TestAuthenticatedClientSessionModel(ClientModel client, UserSessionModel userSession) {
            this.client = client;
            this.userSession = userSession;
            notes = new HashMap<>();
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public int getTimestamp() {
            return 0;
        }

        @Override
        public void setTimestamp(int timestamp) {

        }

        @Override
        public void detachFromUserSession() {

        }

        @Override
        public UserSessionModel getUserSession() {
            return userSession;
        }

        @Override
        public String getCurrentRefreshToken() {
            return null;
        }

        @Override
        public void setCurrentRefreshToken(String currentRefreshToken) {

        }

        @Override
        public int getCurrentRefreshTokenUseCount() {
            return 0;
        }

        @Override
        public void setCurrentRefreshTokenUseCount(int currentRefreshTokenUseCount) {

        }

        @Override
        public String getNote(String name) {
            return notes.get(name);
        }

        @Override
        public void setNote(String name, String value) {
            notes.put(name, value);
        }

        @Override
        public void removeNote(String name) {
            notes.remove(name);
        }

        @Override
        public Map<String, String> getNotes() {
            return notes;
        }

        @Override
        public String getRedirectUri() {
            return null;
        }

        @Override
        public void setRedirectUri(String uri) {

        }

        @Override
        public RealmModel getRealm() {
            return null;
        }

        @Override
        public ClientModel getClient() {
            return client;
        }

        @Override
        public String getAction() {
            return null;
        }

        @Override
        public void setAction(String action) {

        }

        @Override
        public String getProtocol() {
            return null;
        }

        @Override
        public void setProtocol(String method) {

        }
    }

    private class TestUserSessionModel implements UserSessionModel {
        @Override
        public String getId() {
            return null;
        }

        @Override
        public RealmModel getRealm() {
            return null;
        }

        @Override
        public String getBrokerSessionId() {
            return null;
        }

        @Override
        public String getBrokerUserId() {
            return null;
        }

        @Override
        public UserModel getUser() {
            return null;
        }

        @Override
        public String getLoginUsername() {
            return null;
        }

        @Override
        public String getIpAddress() {
            return null;
        }

        @Override
        public String getAuthMethod() {
            return null;
        }

        @Override
        public boolean isRememberMe() {
            return false;
        }

        @Override
        public int getStarted() {
            return 0;
        }

        @Override
        public int getLastSessionRefresh() {
            return 0;
        }

        @Override
        public void setLastSessionRefresh(int seconds) {

        }

        @Override
        public boolean isOffline() {
            return false;
        }

        @Override
        public Map<String, AuthenticatedClientSessionModel> getAuthenticatedClientSessions() {
            return null;
        }

        @Override
        public void removeAuthenticatedClientSessions(Collection<String> removedClientUUIDS) {

        }

        @Override
        public String getNote(String name) {
            return null;
        }

        @Override
        public void setNote(String name, String value) {

        }

        @Override
        public void removeNote(String name) {

        }

        @Override
        public Map<String, String> getNotes() {
            return null;
        }

        @Override
        public UserSessionModel.State getState() {
            return null;
        }

        @Override
        public void setState(UserSessionModel.State state) {

        }

        @Override
        public void restartSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId) {

        }
    }

    private class TestClientModel implements ClientModel {

        private boolean forceArtifactBinding = false;

        private boolean forcePostBinding = false;

        private boolean logoutPostUrlSet = false;

        private boolean logoutSoapUrlSet = false;

        private boolean logoutArtifactUrlSet = false;

        private boolean logoutRedirectUrlSet = false;

        public void defineSetLogoutUrls(boolean post, boolean soap, boolean artifact, boolean redirect) {
            logoutPostUrlSet = post;
            logoutArtifactUrlSet = artifact;
            logoutRedirectUrlSet = redirect;
            logoutSoapUrlSet = soap;
        }

        public void setForceArtifactBinding(boolean forceArtifactBinding) {
            this.forceArtifactBinding = forceArtifactBinding;
        }

        public void setForcePostBinding(boolean forcePostBinding) {
            this.forcePostBinding = forcePostBinding;
        }

        @Override
        public void updateClient() {

        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public String getClientId() {
            return null;
        }

        @Override
        public void setClientId(String clientId) {

        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public void setName(String name) {

        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public void setDescription(String description) {

        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public void setEnabled(boolean enabled) {

        }

        @Override
        public boolean isAlwaysDisplayInConsole() {
            return false;
        }

        @Override
        public void setAlwaysDisplayInConsole(boolean alwaysDisplayInConsole) {

        }

        @Override
        public boolean isSurrogateAuthRequired() {
            return false;
        }

        @Override
        public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {

        }

        @Override
        public Set<String> getWebOrigins() {
            return null;
        }

        @Override
        public void setWebOrigins(Set<String> webOrigins) {

        }

        @Override
        public void addWebOrigin(String webOrigin) {

        }

        @Override
        public void removeWebOrigin(String webOrigin) {

        }

        @Override
        public Set<String> getRedirectUris() {
            return null;
        }

        @Override
        public void setRedirectUris(Set<String> redirectUris) {

        }

        @Override
        public void addRedirectUri(String redirectUri) {

        }

        @Override
        public void removeRedirectUri(String redirectUri) {

        }

        @Override
        public String getManagementUrl() {
            return null;
        }

        @Override
        public void setManagementUrl(String url) {

        }

        @Override
        public String getRootUrl() {
            return null;
        }

        @Override
        public void setRootUrl(String url) {

        }

        @Override
        public String getBaseUrl() {
            return null;
        }

        @Override
        public void setBaseUrl(String url) {

        }

        @Override
        public boolean isBearerOnly() {
            return false;
        }

        @Override
        public void setBearerOnly(boolean only) {

        }

        @Override
        public int getNodeReRegistrationTimeout() {
            return 0;
        }

        @Override
        public void setNodeReRegistrationTimeout(int timeout) {

        }

        @Override
        public String getClientAuthenticatorType() {
            return null;
        }

        @Override
        public void setClientAuthenticatorType(String clientAuthenticatorType) {

        }

        @Override
        public boolean validateSecret(String secret) {
            return false;
        }

        @Override
        public String getSecret() {
            return null;
        }

        @Override
        public void setSecret(String secret) {

        }

        @Override
        public String getRegistrationToken() {
            return null;
        }

        @Override
        public void setRegistrationToken(String registrationToken) {

        }

        @Override
        public String getProtocol() {
            return null;
        }

        @Override
        public void setProtocol(String protocol) {

        }

        @Override
        public void setAttribute(String name, String value) {

        }

        @Override
        public void removeAttribute(String name) {

        }

        @Override
        public String getAttribute(String name) {
            if (SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE.equals(name) && logoutArtifactUrlSet) {
                return "saml-logout-artifact-attribute";
            }
            if (SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE.equals(name) && logoutPostUrlSet) {
                return "saml-logout-post-attribute";
            }
            if (SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE.equals(name) && logoutRedirectUrlSet) {
                return "saml-logout-redirect-attribute";
            }
            if (SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP_ATTRIBUTE.equals(name) && logoutSoapUrlSet) {
                return "saml-logout-soap-attribute";
            }
            if (SamlConfigAttributes.SAML_FORCE_POST_BINDING.equals(name)) {
                return forcePostBinding ? "true" : "false";
            }
            if (SamlConfigAttributes.SAML_ARTIFACT_BINDING.equals(name)) {
                return forceArtifactBinding ? "true" : "false";
            }
            if (SamlConfigAttributes.SAML_SERVER_SIGNATURE.equals(name)) {
                return "true";
            }

            return null;
        }

        @Override
        public Map<String, String> getAttributes() {
            return null;
        }

        @Override
        public String getAuthenticationFlowBindingOverride(String binding) {
            return null;
        }

        @Override
        public Map<String, String> getAuthenticationFlowBindingOverrides() {
            return null;
        }

        @Override
        public void removeAuthenticationFlowBindingOverride(String binding) {

        }

        @Override
        public void setAuthenticationFlowBindingOverride(String binding, String flowId) {

        }

        @Override
        public boolean isFrontchannelLogout() {
            return false;
        }

        @Override
        public void setFrontchannelLogout(boolean flag) {

        }

        @Override
        public boolean isFullScopeAllowed() {
            return false;
        }

        @Override
        public void setFullScopeAllowed(boolean value) {

        }

        @Override
        public boolean isPublicClient() {
            return false;
        }

        @Override
        public void setPublicClient(boolean flag) {

        }

        @Override
        public boolean isConsentRequired() {
            return false;
        }

        @Override
        public void setConsentRequired(boolean consentRequired) {

        }

        @Override
        public boolean isStandardFlowEnabled() {
            return false;
        }

        @Override
        public void setStandardFlowEnabled(boolean standardFlowEnabled) {

        }

        @Override
        public boolean isImplicitFlowEnabled() {
            return false;
        }

        @Override
        public void setImplicitFlowEnabled(boolean implicitFlowEnabled) {

        }

        @Override
        public boolean isDirectAccessGrantsEnabled() {
            return false;
        }

        @Override
        public void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled) {

        }

        @Override
        public boolean isServiceAccountsEnabled() {
            return false;
        }

        @Override
        public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) {

        }

        @Override
        public RealmModel getRealm() {
            return null;
        }

        @Override
        public void addClientScope(ClientScopeModel clientScope, boolean defaultScope) {

        }

        @Override
        public void addClientScopes(Set<ClientScopeModel> clientScopes, boolean defaultScope) {

        }

        @Override
        public void removeClientScope(ClientScopeModel clientScope) {

        }

        @Override
        public Map<String, ClientScopeModel> getClientScopes(boolean defaultScope) {
            return null;
        }

        @Override
        public int getNotBefore() {
            return 0;
        }

        @Override
        public void setNotBefore(int notBefore) {

        }

        @Override
        public Map<String, Integer> getRegisteredNodes() {
            return null;
        }

        @Override
        public void registerNode(String nodeHost, int registrationTime) {

        }

        @Override
        public void unregisterNode(String nodeHost) {

        }

        @Override
        public Stream<ProtocolMapperModel> getProtocolMappersStream() {
            return null;
        }

        @Override
        public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
            return null;
        }

        @Override
        public void removeProtocolMapper(ProtocolMapperModel mapping) {

        }

        @Override
        public void updateProtocolMapper(ProtocolMapperModel mapping) {

        }

        @Override
        public ProtocolMapperModel getProtocolMapperById(String id) {
            return null;
        }

        @Override
        public ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
            return null;
        }

        @Override
        public RoleModel getRole(String name) {
            return null;
        }

        @Override
        public RoleModel addRole(String name) {
            return null;
        }

        @Override
        public RoleModel addRole(String id, String name) {
            return null;
        }

        @Override
        public boolean removeRole(RoleModel role) {
            return false;
        }

        @Override
        public Stream<RoleModel> getRolesStream() {
            return null;
        }

        @Override
        public Stream<RoleModel> getRolesStream(Integer firstResult, Integer maxResults) {
            return null;
        }

        @Override
        public Stream<RoleModel> searchForRolesStream(String search, Integer first, Integer max) {
            return null;
        }

        @Override
        public Stream<RoleModel> getScopeMappingsStream() {
            return null;
        }

        @Override
        public Stream<RoleModel> getRealmScopeMappingsStream() {
            return null;
        }

        @Override
        public void addScopeMapping(RoleModel role) {

        }

        @Override
        public void deleteScopeMapping(RoleModel role) {

        }

        @Override
        public boolean hasScope(RoleModel role) {
            return false;
        }
    };

}
