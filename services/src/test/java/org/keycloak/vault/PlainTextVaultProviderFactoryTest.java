package org.keycloak.vault;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.keycloak.Config;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.WebAuthnPolicy;
import org.keycloak.services.DefaultKeycloakSession;
import org.keycloak.services.DefaultKeycloakSessionFactory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link FilesPlainTextVaultProviderFactory}.
 *
 * @author Sebastian Łaskawiec
 */
public class PlainTextVaultProviderFactoryTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldInitializeVaultCorrectly() {
        //given
        VaultConfig config = new VaultConfig(Scenario.EXISTING.getAbsolutePathAsString());
        KeycloakSession session = new DefaultKeycloakSession(new DefaultKeycloakSessionFactory());
        FilesPlainTextVaultProviderFactory factory = new FilesPlainTextVaultProviderFactory() {
            @Override
            protected String getRealmName(KeycloakSession session) {
                return "test";
            }
        };

        //when
        factory.init(config);
        VaultProvider provider = factory.create(session);

        //then
        assertNotNull(provider);
    }

    @Test
    public void shouldThrowAnExceptionWhenUsingNonExistingDirectory() {
        //given
        VaultConfig config = new VaultConfig(Scenario.NON_EXISTING.getAbsolutePathAsString());
        FilesPlainTextVaultProviderFactory factory = new FilesPlainTextVaultProviderFactory();

        expectedException.expect(VaultNotFoundException.class);

        //when
        factory.init(config);

        //then - verified by the ExpectedException rule
    }

    @Test
    public void shouldReturnNullWhenWithNullDirectory() {
        //given
        VaultConfig config = new VaultConfig(null);
        FilesPlainTextVaultProviderFactory factory = new FilesPlainTextVaultProviderFactory();

        //when
        factory.init(config);
        VaultProvider provider = factory.create(null);

        //then
        assertNull(provider);
    }

    /**
     * A whitebox implementation of the Realm model, which is needed to extract realm name.
     * Please use only for testing {@link FilesPlainTextVaultProviderFactory}.
     */
    private static class VaultRealmModel implements RealmModel {

        @Override
        public String getId() {
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
        public Set<RoleModel> getRoles() {
            return null;
        }

        @Override
        public Set<RoleModel> getRoles(Integer firstResult, Integer maxResults) {
            return null;
        }

        @Override
        public Set<RoleModel> searchForRoles(String search, Integer first, Integer max) {
            return null;
        }

        @Override
        public List<String> getDefaultRoles() {
            return null;
        }

        @Override
        public void addDefaultRole(String name) {
          
        }

        @Override
        public void updateDefaultRoles(String... defaultRoles) {
          
        }

        @Override
        public void removeDefaultRoles(String... defaultRoles) {
          
        }

        @Override
        public String getName() {
            return "test";
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
        public void setAttribute(String name, Boolean value) {
          
        }

        @Override
        public void setAttribute(String name, Integer value) {
          
        }

        @Override
        public void setAttribute(String name, Long value) {
          
        }

        @Override
        public void removeAttribute(String name) {
          
        }

        @Override
        public String getAttribute(String name) {
            return null;
        }

        @Override
        public Integer getAttribute(String name, Integer defaultValue) {
            return null;
        }

        @Override
        public Long getAttribute(String name, Long defaultValue) {
            return null;
        }

        @Override
        public Boolean getAttribute(String name, Boolean defaultValue) {
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
        public List<RequiredCredentialModel> getRequiredCredentials() {
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
        public List<GroupModel> getDefaultGroups() {
            return null;
        }


        @Override
        public void addDefaultGroup(GroupModel group) {
          
        }

        @Override
        public void removeDefaultGroup(GroupModel group) {
          
        }

        @Override
        public List<ClientModel> getClients() {
            return null;
        }

        @Override
        public List<ClientModel> getClients(Integer firstResult, Integer maxResults) {
            return null;
        }

        @Override
        public Long getClientsCount() {
            return null;
        }

        @Override
        public List<ClientModel> getAlwaysDisplayInConsoleClients() {
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
        public List<ClientModel> searchClientByClientId(String clientId, Integer firstResult, Integer maxResults) {
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
        public List<AuthenticationFlowModel> getAuthenticationFlows() {
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
        public List<AuthenticationExecutionModel> getAuthenticationExecutions(String flowId) {
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
        public List<AuthenticatorConfigModel> getAuthenticatorConfigs() {
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
        public List<RequiredActionProviderModel> getRequiredActionProviders() {
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
        public List<IdentityProviderModel> getIdentityProviders() {
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
        public Set<IdentityProviderMapperModel> getIdentityProviderMappers() {
            return null;
        }

        @Override
        public Set<IdentityProviderMapperModel> getIdentityProviderMappersByAlias(String brokerAlias) {
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
        public List<ComponentModel> getComponents(String parentId, String providerType) {
            return null;
        }

        @Override
        public List<ComponentModel> getComponents(String parentId) {
            return null;
        }

        @Override
        public List<ComponentModel> getComponents() {
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
        public Set<String> getEventsListeners() {
            return null;
        }

        @Override
        public void setEventsListeners(Set<String> listeners) {
          
        }

        @Override
        public Set<String> getEnabledEventTypes() {
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
        public Set<String> getSupportedLocales() {
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
        public GroupModel createGroup(String name) {
            return null;
        }

        @Override
        public GroupModel createGroup(String id, String name) {
            return null;
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
        public List<GroupModel> getGroups() {
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
        public List<GroupModel> getTopLevelGroups() {
            return null;
        }

        @Override
        public List<GroupModel> getTopLevelGroups(Integer first, Integer max) {
            return null;
        }

        @Override
        public List<GroupModel> searchForGroupByName(String search, Integer first, Integer max) {
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
        public List<ClientScopeModel> getClientScopes() {
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
        public List<ClientScopeModel> getDefaultClientScopes(boolean defaultScope) {
            return null;
        }
    }

    /**
>>>>>>> KEYCLOAK-953: add allowing user to delete his own account feature
     * A whitebox implementation of the config. Please use only for testing {@link FilesPlainTextVaultProviderFactory}.
     */
    private static class VaultConfig implements Config.Scope {

        private String vaultDirectory;

        public VaultConfig(String vaultDirectory) {
            this.vaultDirectory = vaultDirectory;
        }

        @Override
        public String get(String key) {
            return "dir".equals(key) ? vaultDirectory : null;
        }

        @Override
        public String get(String key, String defaultValue) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public String[] getArray(String key) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Integer getInt(String key) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Integer getInt(String key, Integer defaultValue) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Long getLong(String key) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Long getLong(String key, Long defaultValue) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Boolean getBoolean(String key) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Boolean getBoolean(String key, Boolean defaultValue) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Config.Scope scope(String... scope) {
            throw new UnsupportedOperationException("not implemented");
        }
    }

}