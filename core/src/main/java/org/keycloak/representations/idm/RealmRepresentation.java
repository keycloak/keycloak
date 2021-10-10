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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmRepresentation {

    private static final Logger logger = Logger.getLogger(RealmRepresentation.class);

    protected String id;
    protected String realm;
    protected String displayName;
    protected String displayNameHtml;
    protected Integer notBefore;
    protected String defaultSignatureAlgorithm;
    protected Boolean revokeRefreshToken;
    protected Integer refreshTokenMaxReuse;
    protected Integer accessTokenLifespan;
    protected Integer accessTokenLifespanForImplicitFlow;
    protected Integer ssoSessionIdleTimeout;
    protected Integer ssoSessionMaxLifespan;
    protected Integer ssoSessionIdleTimeoutRememberMe;
    protected Integer ssoSessionMaxLifespanRememberMe;
    protected Integer offlineSessionIdleTimeout;
    // KEYCLOAK-7688 Offline Session Max for Offline Token
    protected Boolean offlineSessionMaxLifespanEnabled;
    protected Integer offlineSessionMaxLifespan;
    protected Integer clientSessionIdleTimeout;
    protected Integer clientSessionMaxLifespan;
    protected Integer clientOfflineSessionIdleTimeout;
    protected Integer clientOfflineSessionMaxLifespan;
    protected Integer accessCodeLifespan;
    protected Integer accessCodeLifespanUserAction;
    protected Integer accessCodeLifespanLogin;
    protected Integer actionTokenGeneratedByAdminLifespan;
    protected Integer actionTokenGeneratedByUserLifespan;
    protected Integer oauth2DeviceCodeLifespan;
    protected Integer oauth2DevicePollingInterval;
    protected Boolean enabled;
    protected String sslRequired;
    @Deprecated
    protected Boolean passwordCredentialGrantAllowed;
    protected Boolean registrationAllowed;
    protected Boolean registrationEmailAsUsername;
    protected Boolean rememberMe;
    protected Boolean verifyEmail;
    protected Boolean loginWithEmailAllowed;
    protected Boolean duplicateEmailsAllowed;
    protected Boolean resetPasswordAllowed;
    protected Boolean editUsernameAllowed;

    @Deprecated
    protected Boolean userCacheEnabled;
    @Deprecated
    protected Boolean realmCacheEnabled;

    //--- brute force settings
    protected Boolean bruteForceProtected;
    protected Boolean permanentLockout;
    protected Integer maxFailureWaitSeconds;
    protected Integer minimumQuickLoginWaitSeconds;
    protected Integer waitIncrementSeconds;
    protected Long quickLoginCheckMilliSeconds;
    protected Integer maxDeltaTimeSeconds;
    protected Integer failureFactor;
    //--- end brute force settings

    @Deprecated
    protected String privateKey;
    @Deprecated
    protected String publicKey;
    @Deprecated
    protected String certificate;
    @Deprecated
    protected String codeSecret;
    protected RolesRepresentation roles;
    protected List<GroupRepresentation> groups;
    @Deprecated
    protected List<String> defaultRoles;
    protected RoleRepresentation defaultRole;
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
    protected List<String> otpSupportedApplications;

    // WebAuthn 2-factor properties below

    protected String webAuthnPolicyRpEntityName;
    protected List<String> webAuthnPolicySignatureAlgorithms;
    protected String webAuthnPolicyRpId;
    protected String webAuthnPolicyAttestationConveyancePreference;
    protected String webAuthnPolicyAuthenticatorAttachment;
    protected String webAuthnPolicyRequireResidentKey;
    protected String webAuthnPolicyUserVerificationRequirement;
    protected Integer webAuthnPolicyCreateTimeout;
    protected Boolean webAuthnPolicyAvoidSameAuthenticatorRegister;
    protected List<String> webAuthnPolicyAcceptableAaguids;

    // WebAuthn passwordless properties below

    protected String webAuthnPolicyPasswordlessRpEntityName;
    protected List<String> webAuthnPolicyPasswordlessSignatureAlgorithms;
    protected String webAuthnPolicyPasswordlessRpId;
    protected String webAuthnPolicyPasswordlessAttestationConveyancePreference;
    protected String webAuthnPolicyPasswordlessAuthenticatorAttachment;
    protected String webAuthnPolicyPasswordlessRequireResidentKey;
    protected String webAuthnPolicyPasswordlessUserVerificationRequirement;
    protected Integer webAuthnPolicyPasswordlessCreateTimeout;
    protected Boolean webAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister;
    protected List<String> webAuthnPolicyPasswordlessAcceptableAaguids;

    // Client Policies/Profiles

    @JsonProperty("clientProfiles")
    protected JsonNode clientProfiles;

    @JsonProperty("clientPolicies")
    protected JsonNode clientPolicies;

    protected List<UserRepresentation> users;
    protected List<UserRepresentation> federatedUsers;
    protected List<ScopeMappingRepresentation> scopeMappings;
    protected Map<String, List<ScopeMappingRepresentation>> clientScopeMappings;
    protected List<ClientRepresentation> clients;
    protected List<ClientScopeRepresentation> clientScopes;
    protected List<String> defaultDefaultClientScopes;
    protected List<String> defaultOptionalClientScopes;
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
    protected String dockerAuthenticationFlow;

    protected Map<String, String> attributes;

    protected String keycloakVersion;

    protected Boolean userManagedAccessAllowed;

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
    @Deprecated
    protected List<ClientTemplateRepresentation> clientTemplates;

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
        if (users == null) users = new ArrayList<>();
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

    public String getDefaultSignatureAlgorithm() {
        return defaultSignatureAlgorithm;
    }

    public void setDefaultSignatureAlgorithm(String defaultSignatureAlgorithm) {
        this.defaultSignatureAlgorithm = defaultSignatureAlgorithm;
    }

    public Boolean getRevokeRefreshToken() {
        return revokeRefreshToken;
    }

    public void setRevokeRefreshToken(Boolean revokeRefreshToken) {
        this.revokeRefreshToken = revokeRefreshToken;
    }

    public Integer getRefreshTokenMaxReuse() {
        return refreshTokenMaxReuse;
    }

    public void setRefreshTokenMaxReuse(Integer refreshTokenMaxReuse) {
        this.refreshTokenMaxReuse = refreshTokenMaxReuse;
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

    public Integer getSsoSessionMaxLifespanRememberMe() {
        return ssoSessionMaxLifespanRememberMe;
    }

    public void setSsoSessionMaxLifespanRememberMe(Integer ssoSessionMaxLifespanRememberMe) {
        this.ssoSessionMaxLifespanRememberMe = ssoSessionMaxLifespanRememberMe;
    }

    public Integer getSsoSessionIdleTimeoutRememberMe() {
        return ssoSessionIdleTimeoutRememberMe;
    }

    public void setSsoSessionIdleTimeoutRememberMe(Integer ssoSessionIdleTimeoutRememberMe) {
        this.ssoSessionIdleTimeoutRememberMe = ssoSessionIdleTimeoutRememberMe;
    }

    public Integer getOfflineSessionIdleTimeout() {
        return offlineSessionIdleTimeout;
    }

    public void setOfflineSessionIdleTimeout(Integer offlineSessionIdleTimeout) {
        this.offlineSessionIdleTimeout = offlineSessionIdleTimeout;
    }

    // KEYCLOAK-7688 Offline Session Max for Offline Token
    public Boolean getOfflineSessionMaxLifespanEnabled() {
        return offlineSessionMaxLifespanEnabled;
    }

    public void setOfflineSessionMaxLifespanEnabled(Boolean offlineSessionMaxLifespanEnabled) {
        this.offlineSessionMaxLifespanEnabled = offlineSessionMaxLifespanEnabled;
    }

    public Integer getOfflineSessionMaxLifespan() {
        return offlineSessionMaxLifespan;
    }

    public void setOfflineSessionMaxLifespan(Integer offlineSessionMaxLifespan) {
        this.offlineSessionMaxLifespan = offlineSessionMaxLifespan;
    }

    public Integer getClientSessionIdleTimeout() {
        return clientSessionIdleTimeout;
    }

    public void setClientSessionIdleTimeout(Integer clientSessionIdleTimeout) {
        this.clientSessionIdleTimeout = clientSessionIdleTimeout;
    }

    public Integer getClientSessionMaxLifespan() {
        return clientSessionMaxLifespan;
    }

    public void setClientSessionMaxLifespan(Integer clientSessionMaxLifespan) {
        this.clientSessionMaxLifespan = clientSessionMaxLifespan;
    }

    public Integer getClientOfflineSessionIdleTimeout() {
        return clientOfflineSessionIdleTimeout;
    }

    public void setClientOfflineSessionIdleTimeout(Integer clientOfflineSessionIdleTimeout) {
        this.clientOfflineSessionIdleTimeout = clientOfflineSessionIdleTimeout;
    }

    public Integer getClientOfflineSessionMaxLifespan() {
        return clientOfflineSessionMaxLifespan;
    }

    public void setClientOfflineSessionMaxLifespan(Integer clientOfflineSessionMaxLifespan) {
        this.clientOfflineSessionMaxLifespan = clientOfflineSessionMaxLifespan;
    }

    public List<ScopeMappingRepresentation> getScopeMappings() {
        return scopeMappings;
    }

    public ScopeMappingRepresentation clientScopeMapping(String clientName) {
        ScopeMappingRepresentation mapping = new ScopeMappingRepresentation();
        mapping.setClient(clientName);
        if (scopeMappings == null) scopeMappings = new ArrayList<>();
        scopeMappings.add(mapping);
        return mapping;
    }

    public ScopeMappingRepresentation clientScopeScopeMapping(String clientScopeName) {
        ScopeMappingRepresentation mapping = new ScopeMappingRepresentation();
        mapping.setClientScope(clientScopeName);
        if (scopeMappings == null) scopeMappings = new ArrayList<>();
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

    public Integer getActionTokenGeneratedByAdminLifespan() {
        return actionTokenGeneratedByAdminLifespan;
    }

    public void setActionTokenGeneratedByAdminLifespan(Integer actionTokenGeneratedByAdminLifespan) {
        this.actionTokenGeneratedByAdminLifespan = actionTokenGeneratedByAdminLifespan;
    }

    public void setOAuth2DeviceCodeLifespan(Integer oauth2DeviceCodeLifespan) {
        this.oauth2DeviceCodeLifespan = oauth2DeviceCodeLifespan;
    }

    public Integer getOAuth2DeviceCodeLifespan() {
        return oauth2DeviceCodeLifespan;
    }

    public void setOAuth2DevicePollingInterval(Integer oauth2DevicePollingInterval) {
        this.oauth2DevicePollingInterval = oauth2DevicePollingInterval;
    }

    public Integer getOAuth2DevicePollingInterval() {
        return oauth2DevicePollingInterval;
    }

    public Integer getActionTokenGeneratedByUserLifespan() {
        return actionTokenGeneratedByUserLifespan;
    }

    public void setActionTokenGeneratedByUserLifespan(Integer actionTokenGeneratedByUserLifespan) {
        this.actionTokenGeneratedByUserLifespan = actionTokenGeneratedByUserLifespan;
    }

    @Deprecated
    public List<String> getDefaultRoles() {
        return defaultRoles;
    }

    @Deprecated
    public void setDefaultRoles(List<String> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    public RoleRepresentation getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(RoleRepresentation defaultRole) {
        this.defaultRole = defaultRole;
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
    
    public Boolean isLoginWithEmailAllowed() {
        return loginWithEmailAllowed;
    }

    public void setLoginWithEmailAllowed(Boolean loginWithEmailAllowed) {
        this.loginWithEmailAllowed = loginWithEmailAllowed;
    }
    
    public Boolean isDuplicateEmailsAllowed() {
        return duplicateEmailsAllowed;
    }

    public void setDuplicateEmailsAllowed(Boolean duplicateEmailsAllowed) {
        this.duplicateEmailsAllowed = duplicateEmailsAllowed;
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

    public Boolean isPermanentLockout() {
        return permanentLockout;
    }

    public void setPermanentLockout(Boolean permanentLockout) {
        this.permanentLockout = permanentLockout;
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

    public List<String> getOtpSupportedApplications() {
        return otpSupportedApplications;
    }

    public void setOtpSupportedApplications(List<String> otpSupportedApplications) {
        this.otpSupportedApplications = otpSupportedApplications;
    }

    // WebAuthn 2-factor properties below

    public String getWebAuthnPolicyRpEntityName() {
        return webAuthnPolicyRpEntityName;
    }

    public void setWebAuthnPolicyRpEntityName(String webAuthnPolicyRpEntityName) {
        this.webAuthnPolicyRpEntityName = webAuthnPolicyRpEntityName;
    }

    public List<String> getWebAuthnPolicySignatureAlgorithms() {
        return webAuthnPolicySignatureAlgorithms;
    }

    public void setWebAuthnPolicySignatureAlgorithms(List<String> webAuthnPolicySignatureAlgorithms) {
        this.webAuthnPolicySignatureAlgorithms = webAuthnPolicySignatureAlgorithms;
    }

    public String getWebAuthnPolicyRpId() {
        return webAuthnPolicyRpId;
    }

    public void setWebAuthnPolicyRpId(String webAuthnPolicyRpId) {
        this.webAuthnPolicyRpId = webAuthnPolicyRpId;
    }

    public String getWebAuthnPolicyAttestationConveyancePreference() {
        return webAuthnPolicyAttestationConveyancePreference;
    }

    public void setWebAuthnPolicyAttestationConveyancePreference(String webAuthnPolicyAttestationConveyancePreference) {
        this.webAuthnPolicyAttestationConveyancePreference = webAuthnPolicyAttestationConveyancePreference;
    }

    public String getWebAuthnPolicyAuthenticatorAttachment() {
        return webAuthnPolicyAuthenticatorAttachment;
    }

    public void setWebAuthnPolicyAuthenticatorAttachment(String webAuthnPolicyAuthenticatorAttachment) {
        this.webAuthnPolicyAuthenticatorAttachment = webAuthnPolicyAuthenticatorAttachment;
    }

    public String getWebAuthnPolicyRequireResidentKey() {
        return webAuthnPolicyRequireResidentKey;
    }

    public void setWebAuthnPolicyRequireResidentKey(String webAuthnPolicyRequireResidentKey) {
        this.webAuthnPolicyRequireResidentKey = webAuthnPolicyRequireResidentKey;
    }

    public String getWebAuthnPolicyUserVerificationRequirement() {
        return webAuthnPolicyUserVerificationRequirement;
    }

    public void setWebAuthnPolicyUserVerificationRequirement(String webAuthnPolicyUserVerificationRequirement) {
        this.webAuthnPolicyUserVerificationRequirement = webAuthnPolicyUserVerificationRequirement;
    }

    public Integer getWebAuthnPolicyCreateTimeout() {
        return webAuthnPolicyCreateTimeout;
    }

    public void setWebAuthnPolicyCreateTimeout(Integer webAuthnPolicyCreateTimeout) {
        this.webAuthnPolicyCreateTimeout = webAuthnPolicyCreateTimeout;
    }

    public Boolean isWebAuthnPolicyAvoidSameAuthenticatorRegister() {
        return webAuthnPolicyAvoidSameAuthenticatorRegister;
    }

    public void setWebAuthnPolicyAvoidSameAuthenticatorRegister(Boolean webAuthnPolicyAvoidSameAuthenticatorRegister) {
        this.webAuthnPolicyAvoidSameAuthenticatorRegister = webAuthnPolicyAvoidSameAuthenticatorRegister;
    }

    public List<String> getWebAuthnPolicyAcceptableAaguids() {
        return webAuthnPolicyAcceptableAaguids;
    }

    public void setWebAuthnPolicyAcceptableAaguids(List<String> webAuthnPolicyAcceptableAaguids) {
        this.webAuthnPolicyAcceptableAaguids = webAuthnPolicyAcceptableAaguids;
    }

    // WebAuthn passwordless properties below


    public String getWebAuthnPolicyPasswordlessRpEntityName() {
        return webAuthnPolicyPasswordlessRpEntityName;
    }

    public void setWebAuthnPolicyPasswordlessRpEntityName(String webAuthnPolicyPasswordlessRpEntityName) {
        this.webAuthnPolicyPasswordlessRpEntityName = webAuthnPolicyPasswordlessRpEntityName;
    }

    public List<String> getWebAuthnPolicyPasswordlessSignatureAlgorithms() {
        return webAuthnPolicyPasswordlessSignatureAlgorithms;
    }

    public void setWebAuthnPolicyPasswordlessSignatureAlgorithms(List<String> webAuthnPolicyPasswordlessSignatureAlgorithms) {
        this.webAuthnPolicyPasswordlessSignatureAlgorithms = webAuthnPolicyPasswordlessSignatureAlgorithms;
    }

    public String getWebAuthnPolicyPasswordlessRpId() {
        return webAuthnPolicyPasswordlessRpId;
    }

    public void setWebAuthnPolicyPasswordlessRpId(String webAuthnPolicyPasswordlessRpId) {
        this.webAuthnPolicyPasswordlessRpId = webAuthnPolicyPasswordlessRpId;
    }

    public String getWebAuthnPolicyPasswordlessAttestationConveyancePreference() {
        return webAuthnPolicyPasswordlessAttestationConveyancePreference;
    }

    public void setWebAuthnPolicyPasswordlessAttestationConveyancePreference(String webAuthnPolicyPasswordlessAttestationConveyancePreference) {
        this.webAuthnPolicyPasswordlessAttestationConveyancePreference = webAuthnPolicyPasswordlessAttestationConveyancePreference;
    }

    public String getWebAuthnPolicyPasswordlessAuthenticatorAttachment() {
        return webAuthnPolicyPasswordlessAuthenticatorAttachment;
    }

    public void setWebAuthnPolicyPasswordlessAuthenticatorAttachment(String webAuthnPolicyPasswordlessAuthenticatorAttachment) {
        this.webAuthnPolicyPasswordlessAuthenticatorAttachment = webAuthnPolicyPasswordlessAuthenticatorAttachment;
    }

    public String getWebAuthnPolicyPasswordlessRequireResidentKey() {
        return webAuthnPolicyPasswordlessRequireResidentKey;
    }

    public void setWebAuthnPolicyPasswordlessRequireResidentKey(String webAuthnPolicyPasswordlessRequireResidentKey) {
        this.webAuthnPolicyPasswordlessRequireResidentKey = webAuthnPolicyPasswordlessRequireResidentKey;
    }

    public String getWebAuthnPolicyPasswordlessUserVerificationRequirement() {
        return webAuthnPolicyPasswordlessUserVerificationRequirement;
    }

    public void setWebAuthnPolicyPasswordlessUserVerificationRequirement(String webAuthnPolicyPasswordlessUserVerificationRequirement) {
        this.webAuthnPolicyPasswordlessUserVerificationRequirement = webAuthnPolicyPasswordlessUserVerificationRequirement;
    }

    public Integer getWebAuthnPolicyPasswordlessCreateTimeout() {
        return webAuthnPolicyPasswordlessCreateTimeout;
    }

    public void setWebAuthnPolicyPasswordlessCreateTimeout(Integer webAuthnPolicyPasswordlessCreateTimeout) {
        this.webAuthnPolicyPasswordlessCreateTimeout = webAuthnPolicyPasswordlessCreateTimeout;
    }

    public Boolean isWebAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister() {
        return webAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister;
    }

    public void setWebAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister(Boolean webAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister) {
        this.webAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister = webAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister;
    }

    public List<String> getWebAuthnPolicyPasswordlessAcceptableAaguids() {
        return webAuthnPolicyPasswordlessAcceptableAaguids;
    }

    public void setWebAuthnPolicyPasswordlessAcceptableAaguids(List<String> webAuthnPolicyPasswordlessAcceptableAaguids) {
        this.webAuthnPolicyPasswordlessAcceptableAaguids = webAuthnPolicyPasswordlessAcceptableAaguids;
    }

    // Client Policies/Profiles

    @JsonIgnore
    public ClientProfilesRepresentation getParsedClientProfiles() {
        try {
            if (clientProfiles == null) return null;
            return JsonSerialization.mapper.convertValue(clientProfiles, ClientProfilesRepresentation.class);
        } catch (IllegalArgumentException ioe) {
            logger.warnf("Failed to deserialize client profiles in the realm %s. Fallback to return empty profiles. Details: %s", realm, ioe.getMessage());
            return null;
        }
    }

    @JsonIgnore
    public void setParsedClientProfiles(ClientProfilesRepresentation clientProfiles) {
        if (clientProfiles == null) {
            this.clientProfiles = null;
            return;
        }
        this.clientProfiles = JsonSerialization.mapper.convertValue(clientProfiles, JsonNode.class);
    }

    @JsonIgnore
    public ClientPoliciesRepresentation getParsedClientPolicies() {
        try {
            if (clientPolicies == null) return null;
            return JsonSerialization.mapper.convertValue(clientPolicies, ClientPoliciesRepresentation.class);
        } catch (IllegalArgumentException ioe) {
            logger.warnf("Failed to deserialize client policies in the realm %s. Fallback to return empty profiles. Details: %s", realm, ioe.getMessage());
            return null;
        }
    }

    @JsonIgnore
    public void setParsedClientPolicies(ClientPoliciesRepresentation clientPolicies) {
        if (clientPolicies == null) {
            this.clientPolicies = null;
            return;
        }
        this.clientPolicies = JsonSerialization.mapper.convertValue(clientPolicies, JsonNode.class);
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

    public String getDockerAuthenticationFlow() {
        return dockerAuthenticationFlow;
    }

    public RealmRepresentation setDockerAuthenticationFlow(final String dockerAuthenticationFlow) {
        this.dockerAuthenticationFlow = dockerAuthenticationFlow;
        return this;
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

    @Deprecated // use getClientScopes() instead
    public List<ClientTemplateRepresentation> getClientTemplates() {
        return clientTemplates;
    }

    public List<ClientScopeRepresentation> getClientScopes() {
        return clientScopes;
    }

    public void setClientScopes(List<ClientScopeRepresentation> clientScopes) {
        this.clientScopes = clientScopes;
    }

    public List<String> getDefaultDefaultClientScopes() {
        return defaultDefaultClientScopes;
    }

    public void setDefaultDefaultClientScopes(List<String> defaultDefaultClientScopes) {
        this.defaultDefaultClientScopes = defaultDefaultClientScopes;
    }

    public List<String> getDefaultOptionalClientScopes() {
        return defaultOptionalClientScopes;
    }

    public void setDefaultOptionalClientScopes(List<String> defaultOptionalClientScopes) {
        this.defaultOptionalClientScopes = defaultOptionalClientScopes;
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

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public List<UserRepresentation> getFederatedUsers() {
        return federatedUsers;
    }

    public void setFederatedUsers(List<UserRepresentation> federatedUsers) {
        this.federatedUsers = federatedUsers;
    }

    public void setUserManagedAccessAllowed(Boolean userManagedAccessAllowed) {
        this.userManagedAccessAllowed = userManagedAccessAllowed;
    }

    public Boolean isUserManagedAccessAllowed() {
        return userManagedAccessAllowed;
    }

    @JsonIgnore
    public Map<String, String> getAttributesOrEmpty() {
        return (Map<String, String>) (attributes == null ? Collections.emptyMap() : attributes);
    }
}
