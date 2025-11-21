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

package org.keycloak.models.cache.infinispan.entities;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.keycloak.common.enums.SslRequired;
import org.keycloak.common.util.ConcurrentMultivaluedHashMap;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.MultivaluedMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OAuth2DeviceConfig;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.ParConfig;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.WebAuthnPolicy;
import org.keycloak.models.cache.infinispan.DefaultLazyLoader;
import org.keycloak.models.cache.infinispan.LazyLoader;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedRealm extends AbstractExtendableRevisioned {

    protected String name;
    protected String displayName;
    protected String displayNameHtml;
    protected boolean enabled;
    protected SslRequired sslRequired;
    protected boolean registrationAllowed;
    protected boolean registrationEmailAsUsername;
    protected boolean rememberMe;
    protected boolean verifyEmail;
    protected boolean loginWithEmailAllowed;
    protected boolean duplicateEmailsAllowed;
    protected boolean resetPasswordAllowed;
    protected boolean identityFederationEnabled;
    protected boolean editUsernameAllowed;
    protected boolean organizationsEnabled;
    protected boolean adminPermissionsEnabled;
    protected boolean verifiableCredentialsEnabled;
    //--- brute force settings
    protected boolean bruteForceProtected;
    protected boolean permanentLockout;
    protected int maxTemporaryLockouts;
    protected RealmRepresentation.BruteForceStrategy bruteForceStrategy;
    protected int maxFailureWaitSeconds;
    protected int minimumQuickLoginWaitSeconds;
    protected int waitIncrementSeconds;
    protected long quickLoginCheckMilliSeconds;
    protected int maxDeltaTimeSeconds;
    protected int failureFactor;
    //--- end brute force settings

    protected String defaultSignatureAlgorithm;
    protected boolean revokeRefreshToken;
    protected int refreshTokenMaxReuse;
    protected int ssoSessionIdleTimeout;
    protected int ssoSessionMaxLifespan;
    protected int ssoSessionIdleTimeoutRememberMe;
    protected int ssoSessionMaxLifespanRememberMe;
    protected int offlineSessionIdleTimeout;
    // KEYCLOAK-7688 Offline Session Max for Offline Token
    protected boolean offlineSessionMaxLifespanEnabled;
    protected int offlineSessionMaxLifespan;
    protected int clientSessionIdleTimeout;
    protected int clientSessionMaxLifespan;
    protected int clientOfflineSessionIdleTimeout;
    protected int clientOfflineSessionMaxLifespan;
    protected int accessTokenLifespan;
    protected int accessTokenLifespanForImplicitFlow;
    protected int accessCodeLifespan;
    protected int accessCodeLifespanUserAction;
    protected int accessCodeLifespanLogin;
    protected LazyLoader<RealmModel, OAuth2DeviceConfig> deviceConfig;
    protected int actionTokenGeneratedByAdminLifespan;
    protected int actionTokenGeneratedByUserLifespan;
    protected int notBefore;
    protected PasswordPolicy passwordPolicy;
    protected OTPPolicy otpPolicy;
    protected WebAuthnPolicy webAuthnPolicy;
    protected WebAuthnPolicy webAuthnPasswordlessPolicy;

    protected String loginTheme;
    protected String accountTheme;
    protected String adminTheme;
    protected String emailTheme;
    protected String masterAdminClient;

    protected List<RequiredCredentialModel> requiredCredentials;
    protected MultivaluedMap<String, ComponentModel> componentsByParent = new MultivaluedHashMap<>();
    protected MultivaluedMap<String, ComponentModel> componentsByParentAndType = new ConcurrentMultivaluedHashMap<>();
    protected Map<String, ComponentModel> components;

    protected Map<String, String> browserSecurityHeaders;
    protected Map<String, String> smtpConfig;
    protected Map<String, AuthenticationFlowModel> authenticationFlows = new HashMap<>();
    protected List<AuthenticationFlowModel> authenticationFlowList;
    protected Map<String, AuthenticatorConfigModel> authenticatorConfigs;
    protected Map<String, RequiredActionConfigModel> requiredActionProviderConfigs = new HashMap<>();
    protected Map<String, RequiredActionConfigModel> requiredActionProviderConfigsByAlias = new HashMap<>();
    protected Map<String, RequiredActionProviderModel> requiredActionProviders = new HashMap<>();
    protected List<RequiredActionProviderModel> requiredActionProviderList;
    protected Map<String, RequiredActionProviderModel> requiredActionProvidersByAlias = new HashMap<>();
    protected MultivaluedHashMap<String, AuthenticationExecutionModel> authenticationExecutions = new MultivaluedHashMap<>();
    protected Map<String, AuthenticationExecutionModel> executionsById = new HashMap<>();
    protected Map<String, AuthenticationExecutionModel> executionsByFlowId = new HashMap<>();

    protected AuthenticationFlowModel browserFlow;
    protected AuthenticationFlowModel registrationFlow;
    protected AuthenticationFlowModel directGrantFlow;
    protected AuthenticationFlowModel resetCredentialsFlow;
    protected AuthenticationFlowModel clientAuthenticationFlow;
    protected AuthenticationFlowModel dockerAuthenticationFlow;
    protected AuthenticationFlowModel firstBrokerLoginFlow;

    protected boolean eventsEnabled;
    protected long eventsExpiration;
    protected Set<String> eventsListeners;
    protected Set<String> enabledEventTypes;
    protected boolean adminEventsEnabled;
    protected boolean adminEventsDetailsEnabled;
    protected String defaultRoleId;
    protected String adminPermissionsClientId;
    private boolean allowUserManagedAccess;

    protected List<String> defaultGroups;
    protected DefaultLazyLoader<RealmModel, List<String>> defaultDefaultClientScopes;
    protected DefaultLazyLoader<RealmModel, List<String>> optionalDefaultClientScopes;
    protected boolean internationalizationEnabled;
    protected Set<String> supportedLocales;
    protected String defaultLocale;

    protected Map<String, String> attributes;

    private Map<String, Integer> userActionTokenLifespans;

    protected Map<String, Map<String,String>> realmLocalizationTexts;

    public CachedRealm(Long revision, RealmModel model) {
        super(revision, model.getId());
        name = model.getName();
        displayName = model.getDisplayName();
        displayNameHtml = model.getDisplayNameHtml();
        enabled = model.isEnabled();
        allowUserManagedAccess = model.isUserManagedAccessAllowed();
        sslRequired = model.getSslRequired();
        registrationAllowed = model.isRegistrationAllowed();
        registrationEmailAsUsername = model.isRegistrationEmailAsUsername();
        rememberMe = model.isRememberMe();
        verifyEmail = model.isVerifyEmail();
        loginWithEmailAllowed = model.isLoginWithEmailAllowed();
        duplicateEmailsAllowed = model.isDuplicateEmailsAllowed();
        resetPasswordAllowed = model.isResetPasswordAllowed();
        editUsernameAllowed = model.isEditUsernameAllowed();
        organizationsEnabled = model.isOrganizationsEnabled();
        adminPermissionsEnabled = model.isAdminPermissionsEnabled();
        verifiableCredentialsEnabled = model.isVerifiableCredentialsEnabled();
        //--- brute force settings
        bruteForceProtected = model.isBruteForceProtected();
        permanentLockout = model.isPermanentLockout();
        maxTemporaryLockouts = model.getMaxTemporaryLockouts();
        bruteForceStrategy = model.getBruteForceStrategy();
        maxFailureWaitSeconds = model.getMaxFailureWaitSeconds();
        minimumQuickLoginWaitSeconds = model.getMinimumQuickLoginWaitSeconds();
        waitIncrementSeconds = model.getWaitIncrementSeconds();
        quickLoginCheckMilliSeconds = model.getQuickLoginCheckMilliSeconds();
        maxDeltaTimeSeconds = model.getMaxDeltaTimeSeconds();
        failureFactor = model.getFailureFactor();
        //--- end brute force settings

        defaultSignatureAlgorithm = model.getDefaultSignatureAlgorithm();
        revokeRefreshToken = model.isRevokeRefreshToken();
        refreshTokenMaxReuse = model.getRefreshTokenMaxReuse();
        ssoSessionIdleTimeout = model.getSsoSessionIdleTimeout();
        ssoSessionMaxLifespan = model.getSsoSessionMaxLifespan();
        ssoSessionIdleTimeoutRememberMe = model.getSsoSessionIdleTimeoutRememberMe();
        ssoSessionMaxLifespanRememberMe = model.getSsoSessionMaxLifespanRememberMe();
        offlineSessionIdleTimeout = model.getOfflineSessionIdleTimeout();
        // KEYCLOAK-7688 Offline Session Max for Offline Token
        offlineSessionMaxLifespanEnabled = model.isOfflineSessionMaxLifespanEnabled();
        offlineSessionMaxLifespan = model.getOfflineSessionMaxLifespan();
        clientSessionIdleTimeout = model.getClientSessionIdleTimeout();
        clientSessionMaxLifespan = model.getClientSessionMaxLifespan();
        clientOfflineSessionIdleTimeout = model.getClientOfflineSessionIdleTimeout();
        clientOfflineSessionMaxLifespan = model.getClientOfflineSessionMaxLifespan();
        accessTokenLifespan = model.getAccessTokenLifespan();
        accessTokenLifespanForImplicitFlow = model.getAccessTokenLifespanForImplicitFlow();
        accessCodeLifespan = model.getAccessCodeLifespan();
        deviceConfig = new DefaultLazyLoader<>(OAuth2DeviceConfig::new, null);
        accessCodeLifespanUserAction = model.getAccessCodeLifespanUserAction();
        accessCodeLifespanLogin = model.getAccessCodeLifespanLogin();
        actionTokenGeneratedByAdminLifespan = model.getActionTokenGeneratedByAdminLifespan();
        actionTokenGeneratedByUserLifespan = model.getActionTokenGeneratedByUserLifespan();
        notBefore = model.getNotBefore();
        passwordPolicy = model.getPasswordPolicy();
        otpPolicy = model.getOTPPolicy();
        webAuthnPolicy = model.getWebAuthnPolicy();
        webAuthnPasswordlessPolicy = model.getWebAuthnPolicyPasswordless();

        loginTheme = model.getLoginTheme();
        accountTheme = model.getAccountTheme();
        adminTheme = model.getAdminTheme();
        emailTheme = model.getEmailTheme();

        requiredCredentials = model.getRequiredCredentialsStream().collect(Collectors.toList());
        userActionTokenLifespans = Collections.unmodifiableMap(new HashMap<>(model.getUserActionTokenLifespans()));

        smtpConfig = model.getSmtpConfig();
        browserSecurityHeaders = model.getBrowserSecurityHeaders();

        eventsEnabled = model.isEventsEnabled();
        eventsExpiration = model.getEventsExpiration();
        eventsListeners = model.getEventsListenersStream().collect(Collectors.toSet());
        enabledEventTypes = model.getEnabledEventTypesStream().collect(Collectors.toSet());

        adminEventsEnabled = model.isAdminEventsEnabled();
        adminEventsDetailsEnabled = model.isAdminEventsDetailsEnabled();
        adminPermissionsClientId = model.getAdminPermissionsClient() == null ? null : model.getAdminPermissionsClient().getId();

        if(Objects.isNull(model.getDefaultRole())) {
            throw new ModelException("Default Role is null for Realm " + name);
        } else {
            defaultRoleId = model.getDefaultRole().getId();
        }
        ClientModel masterAdminClient = model.getMasterAdminClient();
        this.masterAdminClient = (masterAdminClient != null) ? masterAdminClient.getId() : null;

        defaultDefaultClientScopes = new DefaultLazyLoader<>(realm -> realm.getDefaultClientScopesStream(true).map(ClientScopeModel::getId)
                .collect(Collectors.toList()), null);
        optionalDefaultClientScopes = new DefaultLazyLoader<>(realm -> realm.getDefaultClientScopesStream(false).map(ClientScopeModel::getId)
                .collect(Collectors.toList()), null);

        internationalizationEnabled = model.isInternationalizationEnabled();
        supportedLocales = model.getSupportedLocalesStream().collect(Collectors.toSet());
        defaultLocale = model.getDefaultLocale();
        authenticationFlowList = model.getAuthenticationFlowsStream().collect(Collectors.toList());
        for (AuthenticationFlowModel flow : authenticationFlowList) {
            this.authenticationFlows.put(flow.getId(), flow);
            authenticationExecutions.put(flow.getId(), new LinkedList<>());
            model.getAuthenticationExecutionsStream(flow.getId()).forEachOrdered(execution -> {
                authenticationExecutions.add(flow.getId(), execution);
                executionsById.put(execution.getId(), execution);
                if (execution.getFlowId() != null) {
                    executionsByFlowId.put(execution.getFlowId(), execution);
                }
            });
        }

        authenticatorConfigs = model.getAuthenticatorConfigsStream()
                .collect(Collectors.toMap(AuthenticatorConfigModel::getId, Function.identity()));
        List<RequiredActionConfigModel> requiredActionConfigsList = model.getRequiredActionConfigsStream().collect(Collectors.toList());
        for (RequiredActionConfigModel requiredActionConfig : requiredActionConfigsList) {
            requiredActionProviderConfigs.put(requiredActionConfig.getId(), requiredActionConfig);
            requiredActionProviderConfigsByAlias.put(requiredActionConfig.getAlias(), requiredActionConfig);
        }

        requiredActionProviderList = model.getRequiredActionProvidersStream().collect(Collectors.toList());
        for (RequiredActionProviderModel action : requiredActionProviderList) {
            requiredActionProviders.put(action.getId(), action);
            requiredActionProvidersByAlias.put(action.getAlias(), action);
        }

        defaultGroups = model.getDefaultGroupsStream().map(GroupModel::getId).collect(Collectors.toList());

        browserFlow = model.getBrowserFlow();
        registrationFlow = model.getRegistrationFlow();
        directGrantFlow = model.getDirectGrantFlow();
        resetCredentialsFlow = model.getResetCredentialsFlow();
        clientAuthenticationFlow = model.getClientAuthenticationFlow();
        dockerAuthenticationFlow = model.getDockerAuthenticationFlow();
        firstBrokerLoginFlow = model.getFirstBrokerLoginFlow();

        model.getComponentsStream().forEach(component ->
            componentsByParentAndType.add(component.getParentId() + component.getProviderType(), component)
        );
        model.getComponentsStream().forEach(component ->
            componentsByParent.add(component.getParentId(), component)
        );
        components = model.getComponentsStream().collect(Collectors.toMap(component -> component.getId(), Function.identity()));

        try {
            attributes = model.getAttributes();
        } catch (UnsupportedOperationException ex) {
        }

        realmLocalizationTexts = model.getRealmLocalizationTexts();
    }

    public String getMasterAdminClient() {
        return masterAdminClient;
    }

    public String getDefaultRoleId() {
        return defaultRoleId;
    }

    public String getAdminPermissionsClientId() {
        return adminPermissionsClientId;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDisplayNameHtml() {
        return displayNameHtml;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public SslRequired getSslRequired() {
        return sslRequired;
    }

    public boolean isRegistrationAllowed() {
        return registrationAllowed;
    }

    public boolean isRegistrationEmailAsUsername() {
        return registrationEmailAsUsername;
    }

    public boolean isRememberMe() {
        return this.rememberMe;
    }

    public boolean isBruteForceProtected() {
        return bruteForceProtected;
    }

    public boolean isPermanentLockout() {
        return permanentLockout;
    }

    public int getMaxTemporaryLockouts() {
        return maxTemporaryLockouts;
    }

    public RealmRepresentation.BruteForceStrategy getBruteForceStrategy() {
        return bruteForceStrategy;
    }

    public int getMaxFailureWaitSeconds() {
        return this.maxFailureWaitSeconds;
    }

    public int getWaitIncrementSeconds() {
        return this.waitIncrementSeconds;
    }

    public int getMinimumQuickLoginWaitSeconds() {
        return this.minimumQuickLoginWaitSeconds;
    }

    public long getQuickLoginCheckMilliSeconds() {
        return quickLoginCheckMilliSeconds;
    }

    public int getMaxDeltaTimeSeconds() {
        return maxDeltaTimeSeconds;
    }

    public int getFailureFactor() {
        return failureFactor;
    }

    public boolean isVerifyEmail() {
        return verifyEmail;
    }

    public boolean isLoginWithEmailAllowed() {
        return loginWithEmailAllowed;
    }

    public boolean isDuplicateEmailsAllowed() {
        return duplicateEmailsAllowed;
    }

    public boolean isResetPasswordAllowed() {
        return resetPasswordAllowed;
    }

    public boolean isEditUsernameAllowed() {
        return editUsernameAllowed;
    }

    public boolean isOrganizationsEnabled() {
        return organizationsEnabled;
    }

    public boolean isAdminPermissionsEnabled() {
        return adminPermissionsEnabled;
    }

    public boolean isVerifiableCredentialsEnabled() {
        return verifiableCredentialsEnabled;
    }

    public String getDefaultSignatureAlgorithm() {
        return defaultSignatureAlgorithm;
    }

    public boolean isRevokeRefreshToken() {
        return revokeRefreshToken;
    }

    public int getRefreshTokenMaxReuse() {
        return refreshTokenMaxReuse;
    }

    public int getSsoSessionIdleTimeout() {
        return ssoSessionIdleTimeout;
    }

    public int getSsoSessionMaxLifespan() {
        return ssoSessionMaxLifespan;
    }

    public int getSsoSessionIdleTimeoutRememberMe() {
        return ssoSessionIdleTimeoutRememberMe;
    }

    public int getSsoSessionMaxLifespanRememberMe() {
        return ssoSessionMaxLifespanRememberMe;
    }

    public int getOfflineSessionIdleTimeout() {
        return offlineSessionIdleTimeout;
    }

    // KEYCLOAK-7688 Offline Session Max for Offline Token
    public boolean isOfflineSessionMaxLifespanEnabled() {
        return offlineSessionMaxLifespanEnabled;
    }

    public int getOfflineSessionMaxLifespan() {
        return offlineSessionMaxLifespan;
    }

    public int getClientSessionIdleTimeout() {
        return clientSessionIdleTimeout;
    }

    public int getClientSessionMaxLifespan() {
        return clientSessionMaxLifespan;
    }

    public int getClientOfflineSessionIdleTimeout() {
        return clientOfflineSessionIdleTimeout;
    }

    public int getClientOfflineSessionMaxLifespan() {
        return clientOfflineSessionMaxLifespan;
    }

    public int getAccessTokenLifespan() {
        return accessTokenLifespan;
    }

    public int getAccessTokenLifespanForImplicitFlow() {
        return accessTokenLifespanForImplicitFlow;
    }

    public int getAccessCodeLifespan() {
        return accessCodeLifespan;
    }

    public int getAccessCodeLifespanUserAction() {
        return accessCodeLifespanUserAction;
    }

    public Map<String, Integer> getUserActionTokenLifespans() {
        return userActionTokenLifespans;
    }

    public int getAccessCodeLifespanLogin() {
        return accessCodeLifespanLogin;
    }

    public OAuth2DeviceConfig getOAuth2DeviceConfig(KeycloakSession session, Supplier<RealmModel> modelSupplier) {
        return deviceConfig.get(session, modelSupplier);
    }

    public CibaConfig getCibaConfig(Supplier<RealmModel> modelSupplier) {
        return new CibaConfig(modelSupplier.get());
    }

    public ParConfig getParConfig(Supplier<RealmModel> modelSupplier) {
        return new ParConfig(modelSupplier.get());
    }

    public int getActionTokenGeneratedByAdminLifespan() {
        return actionTokenGeneratedByAdminLifespan;
    }

    public int getActionTokenGeneratedByUserLifespan() {
        return actionTokenGeneratedByUserLifespan;
    }

    /**
     * This method is supposed to return user lifespan based on the action token ID
     * provided. If nothing is provided, it will return the default lifespan.
     * @param actionTokenId
     * @return lifespan
     */
    public int getActionTokenGeneratedByUserLifespan(String actionTokenId) {
        if (actionTokenId == null || this.userActionTokenLifespans.get(actionTokenId) == null)
            return getActionTokenGeneratedByUserLifespan();
        return this.userActionTokenLifespans.get(actionTokenId);
    }

    public List<RequiredCredentialModel> getRequiredCredentials() {
        return requiredCredentials;
    }

    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy;
    }

    public Map<String, String> getSmtpConfig() {
        return smtpConfig;
    }

    public Map<String, String> getBrowserSecurityHeaders() {
        return browserSecurityHeaders;
    }

    public String getLoginTheme() {
        return loginTheme;
    }

    public String getAccountTheme() {
        return accountTheme;
    }

    public String getAdminTheme() {
        return this.adminTheme;
    }

    public String getEmailTheme() {
        return emailTheme;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public boolean isEventsEnabled() {
        return eventsEnabled;
    }

    public long getEventsExpiration() {
        return eventsExpiration;
    }

    public Set<String> getEventsListeners() {
        return eventsListeners;
    }

    public Set<String> getEnabledEventTypes() {
        return enabledEventTypes;
    }

    public boolean isAdminEventsEnabled() {
        return adminEventsEnabled;
    }

    public boolean isAdminEventsDetailsEnabled() {
        return adminEventsDetailsEnabled;
    }

    public boolean isInternationalizationEnabled() {
        return internationalizationEnabled;
    }

    public Set<String> getSupportedLocales() {
        return supportedLocales;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public Map<String, AuthenticationFlowModel> getAuthenticationFlows() {
        return authenticationFlows;
    }

    public Map<String, AuthenticatorConfigModel> getAuthenticatorConfigs() {
        return authenticatorConfigs;
    }

    public MultivaluedHashMap<String, AuthenticationExecutionModel> getAuthenticationExecutions() {
        return authenticationExecutions;
    }

    public AuthenticationExecutionModel getAuthenticationExecutionByFlowId(String flowId) {
        return executionsByFlowId.get(flowId);
    }

    public Map<String, AuthenticationExecutionModel> getExecutionsById() {
        return executionsById;
    }

    public Map<String, RequiredActionProviderModel> getRequiredActionProviders() {
        return requiredActionProviders;
    }

    public Map<String, RequiredActionProviderModel> getRequiredActionProvidersByAlias() {
        return requiredActionProvidersByAlias;
    }

    public OTPPolicy getOtpPolicy() {
        return otpPolicy;
    }

    public WebAuthnPolicy getWebAuthnPolicy() {
        return webAuthnPolicy;
    }

    public WebAuthnPolicy getWebAuthnPasswordlessPolicy() {
        return webAuthnPasswordlessPolicy;
    }

    public AuthenticationFlowModel getBrowserFlow() {
        return browserFlow;
    }

    public AuthenticationFlowModel getRegistrationFlow() {
        return registrationFlow;
    }

    public AuthenticationFlowModel getDirectGrantFlow() {
        return directGrantFlow;
    }

    public AuthenticationFlowModel getResetCredentialsFlow() {
        return resetCredentialsFlow;
    }

    public AuthenticationFlowModel getClientAuthenticationFlow() {
        return clientAuthenticationFlow;
    }

    public AuthenticationFlowModel getDockerAuthenticationFlow() {
        return dockerAuthenticationFlow;
    }

    public AuthenticationFlowModel getFirstBrokerLoginFlow() {
        return firstBrokerLoginFlow;
    }

    public List<String> getDefaultGroups() {
        return defaultGroups;
    }

    public List<String> getDefaultDefaultClientScopes(KeycloakSession session, Supplier<RealmModel> modelSupplier) {
        return defaultDefaultClientScopes.get(session, modelSupplier);
    }

    public List<String> getOptionalDefaultClientScopes(KeycloakSession session, Supplier<RealmModel> modelSupplier) {
        return optionalDefaultClientScopes.get(session, modelSupplier);
    }

    public List<AuthenticationFlowModel> getAuthenticationFlowList() {
        return authenticationFlowList;
    }

    public List<RequiredActionProviderModel> getRequiredActionProviderList() {
        return requiredActionProviderList;
    }

    public MultivaluedMap<String, ComponentModel> getComponentsByParent() {
        return new MultivaluedHashMap<>(componentsByParent);
    }

    public MultivaluedMap<String, ComponentModel> getComponentsByParentAndType() {
        return new ConcurrentMultivaluedHashMap<>(componentsByParentAndType);
    }

    public Map<String, ComponentModel> getComponents() {
        return components;
    }

    public String getAttribute(String name) {
        return attributes != null ? attributes.get(name) : null;
    }

    public Integer getAttribute(String name, Integer defaultValue) {
        String v = getAttribute(name);
        return v != null && !v.isEmpty() ? Integer.valueOf(v) : defaultValue;
    }

    public Long getAttribute(String name, Long defaultValue) {
        String v = getAttribute(name);
        return v != null && !v.isEmpty() ? Long.valueOf(v) : defaultValue;
    }

    public Boolean getAttribute(String name, Boolean defaultValue) {
        String v = getAttribute(name);
        return v != null && !v.isEmpty() ? Boolean.valueOf(v) : defaultValue;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public boolean isAllowUserManagedAccess() {
        return allowUserManagedAccess;
    }

    public Map<String, Map<String, String>> getRealmLocalizationTexts() {
        return realmLocalizationTexts;
    }

    public Map<String, RequiredActionConfigModel> getRequiredActionProviderConfigsByAlias() {
        return requiredActionProviderConfigsByAlias;
    }

    public Map<String, RequiredActionConfigModel> getRequiredActionProviderConfigs() {
        return requiredActionProviderConfigs;
    }
}
