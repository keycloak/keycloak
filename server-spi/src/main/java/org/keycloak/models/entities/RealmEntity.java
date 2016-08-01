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

package org.keycloak.models.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RealmEntity extends AbstractIdentifiableEntity {

    private String name;
    private String displayName;
    private String displayNameHtml;
    private boolean enabled;
    private String sslRequired;
    private boolean registrationAllowed;
    protected boolean registrationEmailAsUsername;
    private boolean rememberMe;
    private boolean verifyEmail;
    private boolean resetPasswordAllowed;
    private String passwordPolicy;

    protected String otpPolicyType;
    protected String otpPolicyAlgorithm;
    protected int otpPolicyInitialCounter;
    protected int otpPolicyDigits;
    protected int otpPolicyLookAheadWindow;
    protected int otpPolicyPeriod;


    private boolean editUsernameAllowed;
    //--- brute force settings
    private boolean bruteForceProtected;
    private int maxFailureWaitSeconds;
    private int minimumQuickLoginWaitSeconds;
    private int waitIncrementSeconds;
    private long quickLoginCheckMilliSeconds;
    private int maxDeltaTimeSeconds;
    private int failureFactor;
    //--- end brute force settings

    private boolean revokeRefreshToken;
    private int ssoSessionIdleTimeout;
    private int ssoSessionMaxLifespan;
    private int offlineSessionIdleTimeout;
    private int accessTokenLifespan;
    private int accessTokenLifespanForImplicitFlow;
    private int accessCodeLifespan;
    private int accessCodeLifespanUserAction;
    private int accessCodeLifespanLogin;
    private int notBefore;

    private String publicKeyPem;
    private String privateKeyPem;
    private String certificatePem;
    private String codeSecret;

    private String loginTheme;
    private String accountTheme;
    private String adminTheme;
    private String emailTheme;

    // We are using names of defaultRoles (not ids)
    private List<String> defaultRoles = new LinkedList<String>();
    private List<String> defaultGroups = new LinkedList<String>();

    private List<RequiredCredentialEntity> requiredCredentials = new LinkedList<>();
    private List<ComponentEntity> componentEntities = new LinkedList<>();
    private List<UserFederationProviderEntity> userFederationProviders = new LinkedList<UserFederationProviderEntity>();
    private List<UserFederationMapperEntity> userFederationMappers = new LinkedList<UserFederationMapperEntity>();
    private List<IdentityProviderEntity> identityProviders = new LinkedList<IdentityProviderEntity>();

    private Map<String, String> browserSecurityHeaders = new HashMap<String, String>();
    private Map<String, String> smtpConfig = new HashMap<String, String>();
    private Map<String, String> socialConfig = new HashMap<String, String>();

    private boolean eventsEnabled;
    private long eventsExpiration;
    private List<String> eventsListeners = new ArrayList<String>();
    private List<String> enabledEventTypes = new ArrayList<String>();
    
    protected boolean adminEventsEnabled;
    protected boolean adminEventsDetailsEnabled;
    
    private String masterAdminClient;

    private boolean internationalizationEnabled;
    private List<String> supportedLocales = new ArrayList<String>();
    private String defaultLocale;
    private List<IdentityProviderMapperEntity> identityProviderMappers = new ArrayList<IdentityProviderMapperEntity>();
    private List<AuthenticationFlowEntity> authenticationFlows = new ArrayList<>();
    private List<AuthenticatorConfigEntity> authenticatorConfigs = new ArrayList<>();
    private List<RequiredActionProviderEntity> requiredActionProviders = new ArrayList<>();
    private String browserFlow;
    private String registrationFlow;
    private String directGrantFlow;
    private String resetCredentialsFlow;
    private String clientAuthenticationFlow;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSslRequired() {
        return sslRequired;
    }

    public void setSslRequired(String sslRequired) {
        this.sslRequired = sslRequired;
    }

    public boolean isRegistrationAllowed() {
        return registrationAllowed;
    }

    public void setRegistrationAllowed(boolean registrationAllowed) {
        this.registrationAllowed = registrationAllowed;
    }

    public boolean isRegistrationEmailAsUsername() {
        return registrationEmailAsUsername;
    }

    public void setRegistrationEmailAsUsername(boolean registrationEmailAsUsername) {
        this.registrationEmailAsUsername = registrationEmailAsUsername;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public boolean isVerifyEmail() {
        return verifyEmail;
    }

    public void setVerifyEmail(boolean verifyEmail) {
        this.verifyEmail = verifyEmail;
    }

    public boolean isResetPasswordAllowed() {
        return resetPasswordAllowed;
    }

    public void setResetPasswordAllowed(boolean resetPasswordAllowed) {
        this.resetPasswordAllowed = resetPasswordAllowed;
    }

    public boolean isEditUsernameAllowed() {
        return editUsernameAllowed;
    }

    public void setEditUsernameAllowed(boolean editUsernameAllowed) {
        this.editUsernameAllowed = editUsernameAllowed;
    }

    public String getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(String passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public boolean isBruteForceProtected() {
        return bruteForceProtected;
    }

    public void setBruteForceProtected(boolean bruteForceProtected) {
        this.bruteForceProtected = bruteForceProtected;
    }

    public int getMaxFailureWaitSeconds() {
        return maxFailureWaitSeconds;
    }

    public void setMaxFailureWaitSeconds(int maxFailureWaitSeconds) {
        this.maxFailureWaitSeconds = maxFailureWaitSeconds;
    }

    public int getMinimumQuickLoginWaitSeconds() {
        return minimumQuickLoginWaitSeconds;
    }

    public void setMinimumQuickLoginWaitSeconds(int minimumQuickLoginWaitSeconds) {
        this.minimumQuickLoginWaitSeconds = minimumQuickLoginWaitSeconds;
    }

    public int getWaitIncrementSeconds() {
        return waitIncrementSeconds;
    }

    public void setWaitIncrementSeconds(int waitIncrementSeconds) {
        this.waitIncrementSeconds = waitIncrementSeconds;
    }

    public long getQuickLoginCheckMilliSeconds() {
        return quickLoginCheckMilliSeconds;
    }

    public void setQuickLoginCheckMilliSeconds(long quickLoginCheckMilliSeconds) {
        this.quickLoginCheckMilliSeconds = quickLoginCheckMilliSeconds;
    }

    public int getMaxDeltaTimeSeconds() {
        return maxDeltaTimeSeconds;
    }

    public void setMaxDeltaTimeSeconds(int maxDeltaTimeSeconds) {
        this.maxDeltaTimeSeconds = maxDeltaTimeSeconds;
    }

    public int getFailureFactor() {
        return failureFactor;
    }

    public void setFailureFactor(int failureFactor) {
        this.failureFactor = failureFactor;
    }

    public boolean isRevokeRefreshToken() {
        return revokeRefreshToken;
    }

    public void setRevokeRefreshToken(boolean revokeRefreshToken) {
        this.revokeRefreshToken = revokeRefreshToken;
    }

    public int getSsoSessionIdleTimeout() {
        return ssoSessionIdleTimeout;
    }

    public void setSsoSessionIdleTimeout(int ssoSessionIdleTimeout) {
        this.ssoSessionIdleTimeout = ssoSessionIdleTimeout;
    }

    public int getSsoSessionMaxLifespan() {
        return ssoSessionMaxLifespan;
    }

    public void setSsoSessionMaxLifespan(int ssoSessionMaxLifespan) {
        this.ssoSessionMaxLifespan = ssoSessionMaxLifespan;
    }

    public int getOfflineSessionIdleTimeout() {
        return offlineSessionIdleTimeout;
    }

    public void setOfflineSessionIdleTimeout(int offlineSessionIdleTimeout) {
        this.offlineSessionIdleTimeout = offlineSessionIdleTimeout;
    }

    public int getAccessTokenLifespan() {
        return accessTokenLifespan;
    }

    public void setAccessTokenLifespan(int accessTokenLifespan) {
        this.accessTokenLifespan = accessTokenLifespan;
    }

    public int getAccessTokenLifespanForImplicitFlow() {
        return accessTokenLifespanForImplicitFlow;
    }

    public void setAccessTokenLifespanForImplicitFlow(int accessTokenLifespanForImplicitFlow) {
        this.accessTokenLifespanForImplicitFlow = accessTokenLifespanForImplicitFlow;
    }

    public int getAccessCodeLifespan() {
        return accessCodeLifespan;
    }

    public void setAccessCodeLifespan(int accessCodeLifespan) {
        this.accessCodeLifespan = accessCodeLifespan;
    }

    public int getAccessCodeLifespanUserAction() {
        return accessCodeLifespanUserAction;
    }

    public void setAccessCodeLifespanUserAction(int accessCodeLifespanUserAction) {
        this.accessCodeLifespanUserAction = accessCodeLifespanUserAction;
    }
    public int getAccessCodeLifespanLogin() {
        return accessCodeLifespanLogin;
    }

    public void setAccessCodeLifespanLogin(int accessCodeLifespanLogin) {
        this.accessCodeLifespanLogin = accessCodeLifespanLogin;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(int notBefore) {
        this.notBefore = notBefore;
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public void setPublicKeyPem(String publicKeyPem) {
        this.publicKeyPem = publicKeyPem;
    }

    public String getPrivateKeyPem() {
        return privateKeyPem;
    }

    public void setPrivateKeyPem(String privateKeyPem) {
        this.privateKeyPem = privateKeyPem;
    }

    public String getCodeSecret() {
        return codeSecret;
    }

    public void setCodeSecret(String codeSecret) {
        this.codeSecret = codeSecret;
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

    public List<String> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(List<String> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    public List<RequiredCredentialEntity> getRequiredCredentials() {
        return requiredCredentials;
    }

    public void setRequiredCredentials(List<RequiredCredentialEntity> requiredCredentials) {
        this.requiredCredentials = requiredCredentials;
    }

    public Map<String, String> getBrowserSecurityHeaders() {
        return browserSecurityHeaders;
    }

    public void setBrowserSecurityHeaders(Map<String, String> browserSecurityHeaders) {
        this.browserSecurityHeaders = browserSecurityHeaders;
    }

    public Map<String, String> getSmtpConfig() {
        return smtpConfig;
    }

    public void setSmtpConfig(Map<String, String> smtpConfig) {
        this.smtpConfig = smtpConfig;
    }

    public Map<String, String> getSocialConfig() {
        return socialConfig;
    }

    public void setSocialConfig(Map<String, String> socialConfig) {
        this.socialConfig = socialConfig;
    }

    public boolean isEventsEnabled() {
        return eventsEnabled;
    }

    public void setEventsEnabled(boolean eventsEnabled) {
        this.eventsEnabled = eventsEnabled;
    }

    public long getEventsExpiration() {
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
    
    public boolean isAdminEventsEnabled() {
        return adminEventsEnabled;
    }

    public void setAdminEventsEnabled(boolean adminEventsEnabled) {
        this.adminEventsEnabled = adminEventsEnabled;
    }

    public boolean isAdminEventsDetailsEnabled() {
        return adminEventsDetailsEnabled;
    }

    public void setAdminEventsDetailsEnabled(boolean adminEventsDetailsEnabled) {
        this.adminEventsDetailsEnabled = adminEventsDetailsEnabled;
    }

    public String getMasterAdminClient() {
        return masterAdminClient;
    }

    public void setMasterAdminClient(String masterAdminClient) {
        this.masterAdminClient = masterAdminClient;
    }

    public List<UserFederationProviderEntity> getUserFederationProviders() {
        return userFederationProviders;
    }

    public void setUserFederationProviders(List<UserFederationProviderEntity> userFederationProviders) {
        this.userFederationProviders = userFederationProviders;
    }

    public List<UserFederationMapperEntity> getUserFederationMappers() {
        return userFederationMappers;
    }

    public void setUserFederationMappers(List<UserFederationMapperEntity> userFederationMappers) {
        this.userFederationMappers = userFederationMappers;
    }

    public List<IdentityProviderEntity> getIdentityProviders() {
        return identityProviders;
    }

    public void setIdentityProviders(List<IdentityProviderEntity> identityProviders) {
        this.identityProviders = identityProviders;
    }

    public String getCertificatePem() {
        return certificatePem;
    }

    public void setCertificatePem(String certificatePem) {
        this.certificatePem = certificatePem;
    }

    public boolean isInternationalizationEnabled() {
        return internationalizationEnabled;
    }

    public void setInternationalizationEnabled(boolean internationalizationEnabled) {
        this.internationalizationEnabled = internationalizationEnabled;
    }

    public List<String> getSupportedLocales() {
        return supportedLocales;
    }

    public void setSupportedLocales(List<String> supportedLocales) {
        this.supportedLocales = supportedLocales;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public List<IdentityProviderMapperEntity> getIdentityProviderMappers() {
        return identityProviderMappers;
    }

    public void setIdentityProviderMappers(List<IdentityProviderMapperEntity> identityProviderMappers) {
        this.identityProviderMappers = identityProviderMappers;
    }

    public List<AuthenticationFlowEntity> getAuthenticationFlows() {
        return authenticationFlows;
    }

    public void setAuthenticationFlows(List<AuthenticationFlowEntity> authenticationFlows) {
        this.authenticationFlows = authenticationFlows;
    }

    public List<AuthenticatorConfigEntity> getAuthenticatorConfigs() {
        return authenticatorConfigs;
    }

    public void setAuthenticatorConfigs(List<AuthenticatorConfigEntity> authenticators) {
        this.authenticatorConfigs = authenticators;
    }

    public List<RequiredActionProviderEntity> getRequiredActionProviders() {
        return requiredActionProviders;
    }

    public void setRequiredActionProviders(List<RequiredActionProviderEntity> requiredActionProviders) {
        this.requiredActionProviders = requiredActionProviders;
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

    public int getOtpPolicyInitialCounter() {
        return otpPolicyInitialCounter;
    }

    public void setOtpPolicyInitialCounter(int otpPolicyInitialCounter) {
        this.otpPolicyInitialCounter = otpPolicyInitialCounter;
    }

    public int getOtpPolicyDigits() {
        return otpPolicyDigits;
    }

    public void setOtpPolicyDigits(int otpPolicyDigits) {
        this.otpPolicyDigits = otpPolicyDigits;
    }

    public int getOtpPolicyLookAheadWindow() {
        return otpPolicyLookAheadWindow;
    }

    public void setOtpPolicyLookAheadWindow(int otpPolicyLookAheadWindow) {
        this.otpPolicyLookAheadWindow = otpPolicyLookAheadWindow;
    }

    public int getOtpPolicyPeriod() {
        return otpPolicyPeriod;
    }

    public void setOtpPolicyPeriod(int otpPolicyPeriod) {
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

    public List<String> getDefaultGroups() {
        return defaultGroups;
    }

    public void setDefaultGroups(List<String> defaultGroups) {
        this.defaultGroups = defaultGroups;
    }

    public List<ComponentEntity> getComponentEntities() {
        return componentEntities;
    }

    public void setComponentEntities(List<ComponentEntity> componentEntities) {
        this.componentEntities = componentEntities;
    }
}


