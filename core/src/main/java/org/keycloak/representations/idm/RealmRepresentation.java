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

package org.keycloak.representations.idm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.keycloak.common.util.MultivaluedHashMap;

import java.util.*;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmRepresentation {
    protected String id;
    protected String realm;
    protected String displayName;
    protected String displayNameHtml;
    protected Integer notBefore;
    protected Boolean revokeRefreshToken;
    protected Integer accessTokenLifespan;
    protected Integer accessTokenLifespanForImplicitFlow;
    protected Integer ssoSessionIdleTimeout;
    protected Integer ssoSessionMaxLifespan;
    protected Integer offlineSessionIdleTimeout;
    protected Integer accessCodeLifespan;
    protected Integer accessCodeLifespanUserAction;
    protected Integer accessCodeLifespanLogin;
    protected Boolean enabled;
    protected String sslRequired;
    @Deprecated
    protected Boolean passwordCredentialGrantAllowed;
    protected Boolean registrationAllowed;
    protected Boolean registrationEmailAsUsername;
    protected Boolean rememberMe;
    protected Boolean verifyEmail;
    protected Boolean resetPasswordAllowed;
    protected Boolean editUsernameAllowed;

    @Deprecated
    protected Boolean userCacheEnabled;
    @Deprecated
    protected Boolean realmCacheEnabled;

    //--- brute force settings
    protected Boolean bruteForceProtected;
    protected Integer maxFailureWaitSeconds;
    protected Integer minimumQuickLoginWaitSeconds;
    protected Integer waitIncrementSeconds;
    protected Long quickLoginCheckMilliSeconds;
    protected Integer maxDeltaTimeSeconds;
    protected Integer failureFactor;
    //--- end brute force settings

    protected String privateKey;
    protected String publicKey;
    protected String certificate;
    protected String codeSecret;
    protected RolesRepresentation roles;
    protected List<GroupRepresentation> groups;
    protected List<String> defaultRoles;
    protected List<String> defaultGroups;
    @Deprecated
    protected Set<String> requiredCredentials;
    protected String passwordPolicy;
    protected String otpPolicyType;
    protected String otpPolicyAlgorithm;
    protected Integer otpPolicyInitialCounter;
    protected Integer otpPolicyDigits;
    protected Integer otpPolicyLookAheadWindow;
    protected Integer otpPolicyPeriod;

    protected List<UserRepresentation> users;
    protected List<ScopeMappingRepresentation> scopeMappings;
    protected Map<String, List<ScopeMappingRepresentation>> clientScopeMappings;
    protected List<ClientRepresentation> clients;
    protected List<ClientTemplateRepresentation> clientTemplates;
    protected Map<String, String> browserSecurityHeaders;
    protected Map<String, String> smtpServer;
    protected List<UserFederationProviderRepresentation> userFederationProviders;
    protected List<UserFederationMapperRepresentation> userFederationMappers;
    protected String loginTheme;
    protected String accountTheme;
    protected String adminTheme;
    protected String emailTheme;
    
    protected Boolean eventsEnabled;
    protected Long eventsExpiration;
    protected List<String> eventsListeners;
    protected List<String> enabledEventTypes;
    
    protected Boolean adminEventsEnabled;
    protected Boolean adminEventsDetailsEnabled;
    
    private List<IdentityProviderRepresentation> identityProviders;
    private List<IdentityProviderMapperRepresentation> identityProviderMappers;
    private List<ProtocolMapperRepresentation> protocolMappers;
    private MultivaluedHashMap<String, ComponentExportRepresentation> components;
    protected Boolean internationalizationEnabled;
    protected Set<String> supportedLocales;
    protected String defaultLocale;
    protected List<AuthenticationFlowRepresentation> authenticationFlows;
    protected List<AuthenticatorConfigRepresentation> authenticatorConfig;
    protected List<RequiredActionProviderRepresentation> requiredActions;
    protected String browserFlow;
    protected String registrationFlow;
    protected String directGrantFlow;
    protected String resetCredentialsFlow;
    protected String clientAuthenticationFlow;

    protected String keycloakVersion;

    @Deprecated
    protected Boolean social;
    @Deprecated
    protected Boolean updateProfileOnInitialSocialLogin;
    @Deprecated
    protected Map<String, String> socialProviders;
    @Deprecated
    protected Map<String, List<ScopeMappingRepresentation>> applicationScopeMappings;
    @Deprecated
    protected List<ApplicationRepresentation> applications;
    @Deprecated
    protected List<OAuthClientRepresentation> oauthClients;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayNameHtml() {
        return displayNameHtml;
    }

    public void setDisplayNameHtml(String displayNameHtml) {
        this.displayNameHtml = displayNameHtml;
    }

    public List<UserRepresentation> getUsers() {
        return users;
    }

    public List<ApplicationRepresentation> getApplications() {
        return applications;
    }

    public void setUsers(List<UserRepresentation> users) {
        this.users = users;
    }

    public UserRepresentation user(String username) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        if (users == null) users = new ArrayList<UserRepresentation>();
        users.add(user);
        return user;
    }

    public List<ClientRepresentation> getClients() {
        return clients;
    }

    public void setClients(List<ClientRepresentation> clients) {
        this.clients = clients;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getSslRequired() {
        return sslRequired;
    }

    public void setSslRequired(String sslRequired) {
        this.sslRequired = sslRequired;
    }

    public Boolean getRevokeRefreshToken() {
        return revokeRefreshToken;
    }

    public void setRevokeRefreshToken(Boolean revokeRefreshToken) {
        this.revokeRefreshToken = revokeRefreshToken;
    }

    public Integer getAccessTokenLifespan() {
        return accessTokenLifespan;
    }

    public void setAccessTokenLifespan(Integer accessTokenLifespan) {
        this.accessTokenLifespan = accessTokenLifespan;
    }

    public Integer getAccessTokenLifespanForImplicitFlow() {
        return accessTokenLifespanForImplicitFlow;
    }

    public void setAccessTokenLifespanForImplicitFlow(Integer accessTokenLifespanForImplicitFlow) {
        this.accessTokenLifespanForImplicitFlow = accessTokenLifespanForImplicitFlow;
    }

    public Integer getSsoSessionIdleTimeout() {
        return ssoSessionIdleTimeout;
    }

    public void setSsoSessionIdleTimeout(Integer ssoSessionIdleTimeout) {
        this.ssoSessionIdleTimeout = ssoSessionIdleTimeout;
    }

    public Integer getSsoSessionMaxLifespan() {
        return ssoSessionMaxLifespan;
    }

    public void setSsoSessionMaxLifespan(Integer ssoSessionMaxLifespan) {
        this.ssoSessionMaxLifespan = ssoSessionMaxLifespan;
    }

    public Integer getOfflineSessionIdleTimeout() {
        return offlineSessionIdleTimeout;
    }

    public void setOfflineSessionIdleTimeout(Integer offlineSessionIdleTimeout) {
        this.offlineSessionIdleTimeout = offlineSessionIdleTimeout;
    }

    public List<ScopeMappingRepresentation> getScopeMappings() {
        return scopeMappings;
    }

    public ScopeMappingRepresentation clientScopeMapping(String clientName) {
        ScopeMappingRepresentation mapping = new ScopeMappingRepresentation();
        mapping.setClient(clientName);
        if (scopeMappings == null) scopeMappings = new ArrayList<ScopeMappingRepresentation>();
        scopeMappings.add(mapping);
        return mapping;
    }

    public ScopeMappingRepresentation clientTemplateScopeMapping(String clientTemplateName) {
        ScopeMappingRepresentation mapping = new ScopeMappingRepresentation();
        mapping.setClientTemplate(clientTemplateName);
        if (scopeMappings == null) scopeMappings = new ArrayList<ScopeMappingRepresentation>();
        scopeMappings.add(mapping);
        return mapping;
    }

    @Deprecated
    public Set<String> getRequiredCredentials() {
        return requiredCredentials;
    }
    @Deprecated
    public void setRequiredCredentials(Set<String> requiredCredentials) {
        this.requiredCredentials = requiredCredentials;
    }

    public String getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(String passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public Integer getAccessCodeLifespan() {
        return accessCodeLifespan;
    }

    public void setAccessCodeLifespan(Integer accessCodeLifespan) {
        this.accessCodeLifespan = accessCodeLifespan;
    }

    public Integer getAccessCodeLifespanUserAction() {
        return accessCodeLifespanUserAction;
    }

    public void setAccessCodeLifespanUserAction(Integer accessCodeLifespanUserAction) {
        this.accessCodeLifespanUserAction = accessCodeLifespanUserAction;
    }

    public Integer getAccessCodeLifespanLogin() {
        return accessCodeLifespanLogin;
    }

    public void setAccessCodeLifespanLogin(Integer accessCodeLifespanLogin) {
        this.accessCodeLifespanLogin = accessCodeLifespanLogin;
    }

    public List<String> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(List<String> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    public List<String> getDefaultGroups() {
        return defaultGroups;
    }

    public void setDefaultGroups(List<String> defaultGroups) {
        this.defaultGroups = defaultGroups;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getCodeSecret() {
        return codeSecret;
    }

    public void setCodeSecret(String codeSecret) {
        this.codeSecret = codeSecret;
    }

    public Boolean isPasswordCredentialGrantAllowed() {
        return passwordCredentialGrantAllowed;
    }

    public Boolean isRegistrationAllowed() {
        return registrationAllowed;
    }

    public void setRegistrationAllowed(Boolean registrationAllowed) {
        this.registrationAllowed = registrationAllowed;
    }

    public Boolean isRegistrationEmailAsUsername() {
        return registrationEmailAsUsername;
    }

    public void setRegistrationEmailAsUsername(Boolean registrationEmailAsUsername) {
        this.registrationEmailAsUsername = registrationEmailAsUsername;
    }

    public Boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public Boolean isVerifyEmail() {
        return verifyEmail;
    }

    public void setVerifyEmail(Boolean verifyEmail) {
        this.verifyEmail = verifyEmail;
    }

    public Boolean isResetPasswordAllowed() {
        return resetPasswordAllowed;
    }

    public void setResetPasswordAllowed(Boolean resetPassword) {
        this.resetPasswordAllowed = resetPassword;
    }

    public Boolean isEditUsernameAllowed() {
        return editUsernameAllowed;
    }

    public void setEditUsernameAllowed(Boolean editUsernameAllowed) {
        this.editUsernameAllowed = editUsernameAllowed;
    }

    @Deprecated
    public Boolean isSocial() {
        return social;
    }

    @Deprecated
    public Boolean isUpdateProfileOnInitialSocialLogin() {
        return updateProfileOnInitialSocialLogin;
    }

    public Map<String, String> getBrowserSecurityHeaders() {
        return browserSecurityHeaders;
    }

    public void setBrowserSecurityHeaders(Map<String, String> browserSecurityHeaders) {
        this.browserSecurityHeaders = browserSecurityHeaders;
    }

    @Deprecated
    public Map<String, String> getSocialProviders() {
        return socialProviders;
    }

    public Map<String, String> getSmtpServer() {
        return smtpServer;
    }

    public void setSmtpServer(Map<String, String> smtpServer) {
        this.smtpServer = smtpServer;
    }

    @Deprecated
    public List<OAuthClientRepresentation> getOauthClients() {
        return oauthClients;
    }

    public Map<String, List<ScopeMappingRepresentation>> getClientScopeMappings() {
        return clientScopeMappings;
    }

    public void setClientScopeMappings(Map<String, List<ScopeMappingRepresentation>> clientScopeMappings) {
        this.clientScopeMappings = clientScopeMappings;
    }

    @Deprecated
    public Map<String, List<ScopeMappingRepresentation>> getApplicationScopeMappings() {
        return applicationScopeMappings;
    }

    public RolesRepresentation getRoles() {
        return roles;
    }

    public void setRoles(RolesRepresentation roles) {
        this.roles = roles;
    }

    public String getLoginTheme() {
        return loginTheme;
    }

    public void setLoginTheme(String loginTheme) {
        this.loginTheme = loginTheme;
    }

    public String getAccountTheme() {
        return accountTheme;
    }

    public void setAccountTheme(String accountTheme) {
        this.accountTheme = accountTheme;
    }

    public String getAdminTheme() {
        return adminTheme;
    }

    public void setAdminTheme(String adminTheme) {
        this.adminTheme = adminTheme;
    }

    public String getEmailTheme() {
        return emailTheme;
    }

    public void setEmailTheme(String emailTheme) {
        this.emailTheme = emailTheme;
    }

    public Integer getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Integer notBefore) {
        this.notBefore = notBefore;
    }

    public Boolean isBruteForceProtected() {
        return bruteForceProtected;
    }

    public void setBruteForceProtected(Boolean bruteForceProtected) {
        this.bruteForceProtected = bruteForceProtected;
    }

    public Integer getMaxFailureWaitSeconds() {
        return maxFailureWaitSeconds;
    }

    public void setMaxFailureWaitSeconds(Integer maxFailureWaitSeconds) {
        this.maxFailureWaitSeconds = maxFailureWaitSeconds;
    }

    public Integer getMinimumQuickLoginWaitSeconds() {
        return minimumQuickLoginWaitSeconds;
    }

    public void setMinimumQuickLoginWaitSeconds(Integer minimumQuickLoginWaitSeconds) {
        this.minimumQuickLoginWaitSeconds = minimumQuickLoginWaitSeconds;
    }

    public Integer getWaitIncrementSeconds() {
        return waitIncrementSeconds;
    }

    public void setWaitIncrementSeconds(Integer waitIncrementSeconds) {
        this.waitIncrementSeconds = waitIncrementSeconds;
    }

    public Long getQuickLoginCheckMilliSeconds() {
        return quickLoginCheckMilliSeconds;
    }

    public void setQuickLoginCheckMilliSeconds(Long quickLoginCheckMilliSeconds) {
        this.quickLoginCheckMilliSeconds = quickLoginCheckMilliSeconds;
    }

    public Integer getMaxDeltaTimeSeconds() {
        return maxDeltaTimeSeconds;
    }

    public void setMaxDeltaTimeSeconds(Integer maxDeltaTimeSeconds) {
        this.maxDeltaTimeSeconds = maxDeltaTimeSeconds;
    }

    public Integer getFailureFactor() {
        return failureFactor;
    }

    public void setFailureFactor(Integer failureFactor) {
        this.failureFactor = failureFactor;
    }

    public Boolean isEventsEnabled() {
        return eventsEnabled;
    }

    public void setEventsEnabled(boolean eventsEnabled) {
        this.eventsEnabled = eventsEnabled;
    }

    public Long getEventsExpiration() {
        return eventsExpiration;
    }

    public void setEventsExpiration(long eventsExpiration) {
        this.eventsExpiration = eventsExpiration;
    }

    public List<String> getEventsListeners() {
        return eventsListeners;
    }

    public void setEventsListeners(List<String> eventsListeners) {
        this.eventsListeners = eventsListeners;
    }
    
    public List<String> getEnabledEventTypes() {
        return enabledEventTypes;
    }

    public void setEnabledEventTypes(List<String> enabledEventTypes) {
        this.enabledEventTypes = enabledEventTypes;
    }

    public Boolean isAdminEventsEnabled() {
        return adminEventsEnabled;
    }

    public void setAdminEventsEnabled(Boolean adminEventsEnabled) {
        this.adminEventsEnabled = adminEventsEnabled;
    }

    public Boolean isAdminEventsDetailsEnabled() {
        return adminEventsDetailsEnabled;
    }

    public void setAdminEventsDetailsEnabled(Boolean adminEventsDetailsEnabled) {
        this.adminEventsDetailsEnabled = adminEventsDetailsEnabled;
    }

    public List<UserFederationProviderRepresentation> getUserFederationProviders() {
        return userFederationProviders;
    }

    public void setUserFederationProviders(List<UserFederationProviderRepresentation> userFederationProviders) {
        this.userFederationProviders = userFederationProviders;
    }

    public List<UserFederationMapperRepresentation> getUserFederationMappers() {
        return userFederationMappers;
    }

    public void setUserFederationMappers(List<UserFederationMapperRepresentation> userFederationMappers) {
        this.userFederationMappers = userFederationMappers;
    }

    public void addUserFederationMapper(UserFederationMapperRepresentation userFederationMapper) {
        if (userFederationMappers == null) userFederationMappers = new LinkedList<>();
        userFederationMappers.add(userFederationMapper);
    }

    public List<IdentityProviderRepresentation> getIdentityProviders() {
        return identityProviders;
    }

    public void setIdentityProviders(List<IdentityProviderRepresentation> identityProviders) {
        this.identityProviders = identityProviders;
    }

    public void addIdentityProvider(IdentityProviderRepresentation identityProviderRepresentation) {
        if (identityProviders == null) identityProviders = new LinkedList<>();
        identityProviders.add(identityProviderRepresentation);
    }

    public List<ProtocolMapperRepresentation> getProtocolMappers() {
        return protocolMappers;
    }

    public void addProtocolMapper(ProtocolMapperRepresentation rep) {
        if (protocolMappers == null) protocolMappers = new LinkedList<ProtocolMapperRepresentation>();
        protocolMappers.add(rep);
    }

    public void setProtocolMappers(List<ProtocolMapperRepresentation> protocolMappers) {
        this.protocolMappers = protocolMappers;
    }

    public Boolean isInternationalizationEnabled() {
        return internationalizationEnabled;
    }

    public void setInternationalizationEnabled(Boolean internationalizationEnabled) {
        this.internationalizationEnabled = internationalizationEnabled;
    }

    public Set<String> getSupportedLocales() {
        return supportedLocales;
    }

    public void addSupportedLocales(String locale) {
        if(supportedLocales == null){
            supportedLocales = new HashSet<>();
        }
        supportedLocales.add(locale);
    }

    public void setSupportedLocales(Set<String> supportedLocales) {
        this.supportedLocales = supportedLocales;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public List<IdentityProviderMapperRepresentation> getIdentityProviderMappers() {
        return identityProviderMappers;
    }

    public void setIdentityProviderMappers(List<IdentityProviderMapperRepresentation> identityProviderMappers) {
        this.identityProviderMappers = identityProviderMappers;
    }

    public void addIdentityProviderMapper(IdentityProviderMapperRepresentation rep) {
        if (identityProviderMappers == null) identityProviderMappers = new LinkedList<>();
        identityProviderMappers.add(rep);
    }

    public List<AuthenticationFlowRepresentation> getAuthenticationFlows() {
        return authenticationFlows;
    }

    public void setAuthenticationFlows(List<AuthenticationFlowRepresentation> authenticationFlows) {
        this.authenticationFlows = authenticationFlows;
    }

    public List<AuthenticatorConfigRepresentation> getAuthenticatorConfig() {
        return authenticatorConfig;
    }

    public void setAuthenticatorConfig(List<AuthenticatorConfigRepresentation> authenticatorConfig) {
        this.authenticatorConfig = authenticatorConfig;
    }

    public List<RequiredActionProviderRepresentation> getRequiredActions() {
        return requiredActions;
    }

    public void setRequiredActions(List<RequiredActionProviderRepresentation> requiredActions) {
        this.requiredActions = requiredActions;
    }

    public String getOtpPolicyType() {
        return otpPolicyType;
    }

    public void setOtpPolicyType(String otpPolicyType) {
        this.otpPolicyType = otpPolicyType;
    }

    public String getOtpPolicyAlgorithm() {
        return otpPolicyAlgorithm;
    }

    public void setOtpPolicyAlgorithm(String otpPolicyAlgorithm) {
        this.otpPolicyAlgorithm = otpPolicyAlgorithm;
    }

    public Integer getOtpPolicyInitialCounter() {
        return otpPolicyInitialCounter;
    }

    public void setOtpPolicyInitialCounter(Integer otpPolicyInitialCounter) {
        this.otpPolicyInitialCounter = otpPolicyInitialCounter;
    }

    public Integer getOtpPolicyDigits() {
        return otpPolicyDigits;
    }

    public void setOtpPolicyDigits(Integer otpPolicyDigits) {
        this.otpPolicyDigits = otpPolicyDigits;
    }

    public Integer getOtpPolicyLookAheadWindow() {
        return otpPolicyLookAheadWindow;
    }

    public void setOtpPolicyLookAheadWindow(Integer otpPolicyLookAheadWindow) {
        this.otpPolicyLookAheadWindow = otpPolicyLookAheadWindow;
    }

    public Integer getOtpPolicyPeriod() {
        return otpPolicyPeriod;
    }

    public void setOtpPolicyPeriod(Integer otpPolicyPeriod) {
        this.otpPolicyPeriod = otpPolicyPeriod;
    }

    public String getBrowserFlow() {
        return browserFlow;
    }

    public void setBrowserFlow(String browserFlow) {
        this.browserFlow = browserFlow;
    }

    public String getRegistrationFlow() {
        return registrationFlow;
    }

    public void setRegistrationFlow(String registrationFlow) {
        this.registrationFlow = registrationFlow;
    }

    public String getDirectGrantFlow() {
        return directGrantFlow;
    }

    public void setDirectGrantFlow(String directGrantFlow) {
        this.directGrantFlow = directGrantFlow;
    }

    public String getResetCredentialsFlow() {
        return resetCredentialsFlow;
    }

    public void setResetCredentialsFlow(String resetCredentialsFlow) {
        this.resetCredentialsFlow = resetCredentialsFlow;
    }

    public String getClientAuthenticationFlow() {
        return clientAuthenticationFlow;
    }

    public void setClientAuthenticationFlow(String clientAuthenticationFlow) {
        this.clientAuthenticationFlow = clientAuthenticationFlow;
    }

    public String getKeycloakVersion() {
        return keycloakVersion;
    }

    public void setKeycloakVersion(String keycloakVersion) {
        this.keycloakVersion = keycloakVersion;
    }

    public List<GroupRepresentation> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupRepresentation> groups) {
        this.groups = groups;
    }

    public List<ClientTemplateRepresentation> getClientTemplates() {
        return clientTemplates;
    }

    public void setClientTemplates(List<ClientTemplateRepresentation> clientTemplates) {
        this.clientTemplates = clientTemplates;
    }

    public MultivaluedHashMap<String, ComponentExportRepresentation> getComponents() {
        return components;
    }

    public void setComponents(MultivaluedHashMap<String, ComponentExportRepresentation> components) {
        this.components = components;
    }

    @JsonIgnore
    public boolean isIdentityFederationEnabled() {
        return identityProviders != null && !identityProviders.isEmpty();
    }

}
