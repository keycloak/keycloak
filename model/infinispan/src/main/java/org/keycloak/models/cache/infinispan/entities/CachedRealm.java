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

import org.keycloak.common.enums.SslRequired;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.OAuth2DeviceConfig;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.ParConfig;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.WebAuthnPolicy;
import org.keycloak.models.cache.infinispan.DefaultLazyLoader;
import org.keycloak.models.cache.infinispan.LazyLoader;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedRealm extends AbstractExtendableRevisioned {

    private static List<RequiredCredentialModel> fetchRequiredCredentials(RealmModel r) {
        return r.getRequiredCredentialsStream().collect(Collectors.toList());
    }

    private static List<IdentityProviderModel> fetchIdentityProviders(RealmModel r) {
        return Collections.unmodifiableList(r.getIdentityProvidersStream().map(IdentityProviderModel::new).collect(Collectors.toList()));
    }

    private static Set<IdentityProviderMapperModel> fetchIdentityProviderMappers(RealmModel r) {
        return r.getIdentityProviderMappersStream().collect(Collectors.toSet());
    }

    private static MultivaluedHashMap<String, IdentityProviderMapperModel> fetchIdentityProviderMappersMap(RealmModel r) {
        MultivaluedHashMap<String, IdentityProviderMapperModel> mappers = new MultivaluedHashMap<>();

        for (IdentityProviderMapperModel mapper : r.getIdentityProviderMappersStream().collect(Collectors.toList())) {
            mappers.add(mapper.getIdentityProviderAlias(), mapper);
        }

        return mappers;
    }

    private static Set<String> fetchEventListeners(RealmModel r) {
        return r.getEventsListenersStream().collect(Collectors.toSet());
    }

    private static Set<String> fetchEnabledEventTypes(RealmModel r) {
        return r.getEnabledEventTypesStream().collect(Collectors.toSet());
    }

    private static List<String> fetchClientScopes(RealmModel r) {
        return r.getClientScopesStream().map(ClientScopeModel::getId).collect(Collectors.toList());
    }

    private static List<String> fetchDefaultClientScopes(RealmModel r) {
        return r.getDefaultClientScopesStream(true).map(ClientScopeModel::getId).collect(Collectors.toList());
    }

    private static List<String> fetchOptionalClientScopes(RealmModel r) {
        return r.getDefaultClientScopesStream(false).map(ClientScopeModel::getId).collect(Collectors.toList());
    }

    private static Set<String> fetchSupportedLocales(RealmModel r) {
        return r.getSupportedLocalesStream().collect(Collectors.toSet());
    }

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
    protected boolean editUsernameAllowed;
    //--- brute force settings
    protected boolean bruteForceProtected;
    protected boolean permanentLockout;
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
    protected LazyLoader<RealmModel, CibaConfig> cibaConfig;
    protected LazyLoader<RealmModel, ParConfig> parConfig;
    protected int actionTokenGeneratedByAdminLifespan;
    protected int actionTokenGeneratedByUserLifespan;
    protected int notBefore;
    protected LazyLoader<RealmModel, PasswordPolicy> passwordPolicy;
    protected LazyLoader<RealmModel, OTPPolicy> otpPolicy;
    protected LazyLoader<RealmModel, WebAuthnPolicy> webAuthnPolicy;

    protected LazyLoader<RealmModel, WebAuthnPolicy> webAuthnPasswordlessPolicy;
    protected String loginTheme;
    protected String accountTheme;
    protected String adminTheme;
    protected String emailTheme;

    protected String masterAdminClient;
    protected LazyLoader<RealmModel, List<RequiredCredentialModel>> requiredCredentials;
    protected MultivaluedHashMap<String, ComponentModel> componentsByParent = new MultivaluedHashMap<>();
    protected MultivaluedHashMap<String, ComponentModel> componentsByParentAndType = new MultivaluedHashMap<>();
    protected Map<String, ComponentModel> components;

    protected LazyLoader<RealmModel, List<IdentityProviderModel>> identityProviders;
    protected Map<String, String> browserSecurityHeaders;
    protected LazyLoader<RealmModel, Map<String, String>> smtpConfig;
    protected LazyLoader<RealmModel, Map<String, AuthenticationFlowModel>> authenticationFlows;
    protected LazyLoader<RealmModel, List<AuthenticationFlowModel>> authenticationFlowList;
    protected LazyLoader<RealmModel, Map<String, AuthenticatorConfigModel>> authenticatorConfigs;
    protected LazyLoader<RealmModel, Map<String, RequiredActionProviderModel>> requiredActionProviders;
    protected LazyLoader<RealmModel, List<RequiredActionProviderModel>> requiredActionProviderList;
    protected LazyLoader<RealmModel, Map<String, RequiredActionProviderModel>> requiredActionProvidersByAlias;
    protected LazyLoader<RealmModel, MultivaluedHashMap<String, AuthenticationExecutionModel>> authenticationExecutions;
    protected LazyLoader<RealmModel, Map<String, AuthenticationExecutionModel>> executionsById;

    protected LazyLoader<RealmModel, Map<String, AuthenticationExecutionModel>> executionsByFlowId;
    protected LazyLoader<RealmModel, AuthenticationFlowModel> browserFlow;
    protected LazyLoader<RealmModel, AuthenticationFlowModel> registrationFlow;
    protected LazyLoader<RealmModel, AuthenticationFlowModel> directGrantFlow;
    protected LazyLoader<RealmModel, AuthenticationFlowModel> resetCredentialsFlow;
    protected LazyLoader<RealmModel, AuthenticationFlowModel> clientAuthenticationFlow;

    protected LazyLoader<RealmModel, AuthenticationFlowModel> dockerAuthenticationFlow;
    protected boolean eventsEnabled;
    protected long eventsExpiration;
    protected LazyLoader<RealmModel, Set<String>> eventsListeners;
    protected LazyLoader<RealmModel, Set<String>> enabledEventTypes;
    protected boolean adminEventsEnabled;
    protected LazyLoader<RealmModel, Set<String>> adminEnabledEventOperations;
    protected boolean adminEventsDetailsEnabled;
    protected String defaultRoleId;
    private boolean allowUserManagedAccess;

    public Set<IdentityProviderMapperModel> getIdentityProviderMapperSet(Supplier<RealmModel> model) {
        return identityProviderMapperSet.get(model);
    }

    protected LazyLoader<RealmModel, List<String>> defaultGroups;
    protected LazyLoader<RealmModel, List<String>> clientScopes;
    protected LazyLoader<RealmModel, List<String>> defaultDefaultClientScopes;
    protected LazyLoader<RealmModel, List<String>> optionalDefaultClientScopes;
    protected boolean internationalizationEnabled;
    protected LazyLoader<RealmModel, Set<String>> supportedLocales;
    protected String defaultLocale;
    protected LazyLoader<RealmModel, MultivaluedHashMap<String, IdentityProviderMapperModel>> identityProviderMappers;

    protected LazyLoader<RealmModel, Set<IdentityProviderMapperModel>> identityProviderMapperSet;

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
        //--- brute force settings
        bruteForceProtected = model.isBruteForceProtected();
        permanentLockout = model.isPermanentLockout();
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
        cibaConfig = new DefaultLazyLoader<>(CibaConfig::new, null);
        parConfig = new DefaultLazyLoader<>(ParConfig::new, null);
        accessCodeLifespanUserAction = model.getAccessCodeLifespanUserAction();
        accessCodeLifespanLogin = model.getAccessCodeLifespanLogin();
        actionTokenGeneratedByAdminLifespan = model.getActionTokenGeneratedByAdminLifespan();
        actionTokenGeneratedByUserLifespan = model.getActionTokenGeneratedByUserLifespan();
        notBefore = model.getNotBefore();
        passwordPolicy = new DefaultLazyLoader<>(RealmModel::getPasswordPolicy, null);
        otpPolicy = new DefaultLazyLoader<>(RealmModel::getOTPPolicy, null);
        webAuthnPolicy = new DefaultLazyLoader<>(RealmModel::getWebAuthnPolicy, null);
        webAuthnPasswordlessPolicy = new DefaultLazyLoader<>(RealmModel::getWebAuthnPolicyPasswordless, null);

        loginTheme = model.getLoginTheme();
        accountTheme = model.getAccountTheme();
        adminTheme = model.getAdminTheme();
        emailTheme = model.getEmailTheme();

        requiredCredentials = new DefaultLazyLoader<>(CachedRealm::fetchRequiredCredentials, Collections::emptyList);
        userActionTokenLifespans = Collections.unmodifiableMap(new HashMap<>(model.getUserActionTokenLifespans()));

        this.identityProviders = new DefaultLazyLoader<>(CachedRealm::fetchIdentityProviders, Collections::emptyList);
        this.identityProviderMapperSet = new DefaultLazyLoader<>(CachedRealm::fetchIdentityProviderMappers, Collections::emptySet);
        this.identityProviderMappers = new DefaultLazyLoader<>(CachedRealm::fetchIdentityProviderMappersMap, MultivaluedHashMap::new);

        smtpConfig = new DefaultLazyLoader<>(RealmModel::getSmtpConfig, null);
        browserSecurityHeaders = model.getBrowserSecurityHeaders();

        eventsEnabled = model.isEventsEnabled();
        eventsExpiration = model.getEventsExpiration();
        eventsListeners = new DefaultLazyLoader<>(CachedRealm::fetchEventListeners, Collections::emptySet);
        enabledEventTypes = new DefaultLazyLoader<>(CachedRealm::fetchEnabledEventTypes, Collections::emptySet);

        adminEventsEnabled = model.isAdminEventsEnabled();
        adminEventsDetailsEnabled = model.isAdminEventsDetailsEnabled();

        defaultRoleId = model.getDefaultRole().getId();
        ClientModel masterAdminClient = model.getMasterAdminClient();
        this.masterAdminClient = (masterAdminClient != null) ? masterAdminClient.getId() : null;

        cacheClientScopes();

        internationalizationEnabled = model.isInternationalizationEnabled();
        supportedLocales = new DefaultLazyLoader<>(CachedRealm::fetchSupportedLocales, Collections::emptySet);
        defaultLocale = model.getDefaultLocale();
        authenticationFlowList = new DefaultLazyLoader<>(CachedRealm::fetchAuthenticationFlowsList, Collections::emptyList);
        this.authenticationFlows = new DefaultLazyLoader<>(CachedRealm::fetchAuthenticationFlows, Collections::emptyMap);
        this.authenticationExecutions = new DefaultLazyLoader<>(CachedRealm::fetchAuthenticationExecutions, MultivaluedHashMap::new);
        this.executionsById = new DefaultLazyLoader<>(CachedRealm::fetchAuthenticationExecutionsById, HashMap::new);
        this.executionsByFlowId = new DefaultLazyLoader<>(CachedRealm::fetchAuthenticationExecutionsByFlowId, HashMap::new);

        authenticatorConfigs = new DefaultLazyLoader<>(CachedRealm::fetchAuthenticatorConfigs, HashMap::new);
        requiredActionProviderList = new DefaultLazyLoader<>(CachedRealm::fetchRequiredActionProviderList, Collections::emptyList);
        requiredActionProviders = new DefaultLazyLoader<>(CachedRealm::fetchRequiredActionProviders, HashMap::new);
        requiredActionProvidersByAlias = new DefaultLazyLoader<>(CachedRealm::fetchRequiredActionProviderByAlias, HashMap::new);

        defaultGroups = new DefaultLazyLoader<>(CachedRealm::fetchDefaultGroups, Collections::emptyList);

        browserFlow = new DefaultLazyLoader<>(RealmModel::getBrowserFlow, null);
        registrationFlow = new DefaultLazyLoader<>(RealmModel::getRegistrationFlow, null);
        directGrantFlow = new DefaultLazyLoader<>(RealmModel::getDirectGrantFlow, null);
        resetCredentialsFlow = new DefaultLazyLoader<>(RealmModel::getResetCredentialsFlow, null);
        clientAuthenticationFlow = new DefaultLazyLoader<>(RealmModel::getClientAuthenticationFlow, null);
        dockerAuthenticationFlow = new DefaultLazyLoader<>(RealmModel::getDockerAuthenticationFlow, null);

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

    private static List<AuthenticationFlowModel> fetchAuthenticationFlowsList(RealmModel r) {
        return r.getAuthenticationFlowsStream().collect(Collectors.toList());
    }

    private static Map<String, AuthenticationFlowModel> fetchAuthenticationFlows(RealmModel r) {
        Map<String, AuthenticationFlowModel> flows = new HashMap<>();

        for (AuthenticationFlowModel flow : r.getAuthenticationFlowsStream().collect(Collectors.toList())) {
            flows.put(flow.getId(), flow);
        }

        return flows;
    }

    private static MultivaluedHashMap<String, AuthenticationExecutionModel> fetchAuthenticationExecutions(RealmModel r) {
        MultivaluedHashMap<String, AuthenticationExecutionModel> executions = new MultivaluedHashMap<>();

        for (AuthenticationFlowModel flow : r.getAuthenticationFlowsStream().collect(Collectors.toList())) {
            executions.put(flow.getId(), new LinkedList<>());
            r.getAuthenticationExecutionsStream(flow.getId()).forEachOrdered(execution -> {
                executions.add(flow.getId(), execution);
            });
        }

        return executions;
    }

    private static Map<String, AuthenticationExecutionModel> fetchAuthenticationExecutionsById(RealmModel r) {
        Map<String, AuthenticationExecutionModel> executions = new HashMap<>();

        for (AuthenticationFlowModel flow : r.getAuthenticationFlowsStream().collect(Collectors.toList())) {
            r.getAuthenticationExecutionsStream(flow.getId()).forEachOrdered(execution -> {
                executions.put(execution.getId(), execution);
            });
        }

        return executions;
    }

    private static Map<String, AuthenticationExecutionModel> fetchAuthenticationExecutionsByFlowId(RealmModel r) {
        Map<String, AuthenticationExecutionModel> executions = new HashMap<>();

        for (AuthenticationFlowModel flow : r.getAuthenticationFlowsStream().collect(Collectors.toList())) {
            r.getAuthenticationExecutionsStream(flow.getId()).forEachOrdered(execution -> {
                if (execution.getFlowId() != null) {
                    executions.put(execution.getFlowId(), execution);
                }
            });
        }

        return executions;
    }

    private static Map<String, AuthenticatorConfigModel> fetchAuthenticatorConfigs(RealmModel r) {
        return r.getAuthenticatorConfigsStream().collect(Collectors.toMap(AuthenticatorConfigModel::getId, Function.identity()));
    }

    private static List<RequiredActionProviderModel> fetchRequiredActionProviderList(RealmModel r) {
        return r.getRequiredActionProvidersStream().collect(Collectors.toList());
    }

    private static Map<String, RequiredActionProviderModel> fetchRequiredActionProviders(RealmModel r) {
        Map<String, RequiredActionProviderModel> actions = new HashMap<>();

        for (RequiredActionProviderModel action : r.getRequiredActionProvidersStream().collect(Collectors.toList())) {
            actions.put(action.getId(), action);
        }

        return actions;
    }

    private static Map<String, RequiredActionProviderModel> fetchRequiredActionProviderByAlias(RealmModel r) {
        Map<String, RequiredActionProviderModel> actions = new HashMap<>();

        for (RequiredActionProviderModel action : r.getRequiredActionProvidersStream().collect(Collectors.toList())) {
            actions.put(action.getAlias(), action);
        }

        return actions;
    }

    private static List<String> fetchDefaultGroups(RealmModel r) {
        return r.getDefaultGroupsStream().map(GroupModel::getId).collect(Collectors.toList());
    }

    protected void cacheClientScopes() {
        clientScopes = new DefaultLazyLoader<>(CachedRealm::fetchClientScopes, Collections::emptyList);
        defaultDefaultClientScopes = new DefaultLazyLoader<>(CachedRealm::fetchDefaultClientScopes, Collections::emptyList);
        optionalDefaultClientScopes = new DefaultLazyLoader<>(CachedRealm::fetchOptionalClientScopes, Collections::emptyList);;
    }

    public String getMasterAdminClient() {
        return masterAdminClient;
    }

    public String getDefaultRoleId() {
        return defaultRoleId;
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

    public OAuth2DeviceConfig getOAuth2DeviceConfig(Supplier<RealmModel> modelSupplier) {
        return deviceConfig.get(modelSupplier);
    }

    public CibaConfig getCibaConfig(Supplier<RealmModel> modelSupplier) {
        return cibaConfig.get(modelSupplier);
    }

    public ParConfig getParConfig(Supplier<RealmModel> modelSupplier) {
        return parConfig.get(modelSupplier);
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

    public List<RequiredCredentialModel> getRequiredCredentials(Supplier<RealmModel> model) {
        return requiredCredentials.get(model);
    }

    public PasswordPolicy getPasswordPolicy(Supplier<RealmModel> model) {
        return passwordPolicy.get(model);
    }

    public boolean isIdentityFederationEnabled(Supplier<RealmModel> model) {
        return !identityProviders.get(model).isEmpty();
    }

    public Map<String, String> getSmtpConfig(Supplier<RealmModel> model) {
        return smtpConfig.get(model);
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

    public Set<String> getEventsListeners(Supplier<RealmModel> model) {
        return eventsListeners.get(model);
    }

    public Set<String> getEnabledEventTypes(Supplier<RealmModel> model) {
        return enabledEventTypes.get(model);
    }

    public boolean isAdminEventsEnabled() {
        return adminEventsEnabled;
    }

    public boolean isAdminEventsDetailsEnabled() {
        return adminEventsDetailsEnabled;
    }

    public List<IdentityProviderModel> getIdentityProviders(Supplier<RealmModel> model) {
        return identityProviders.get(model);
    }

    public boolean isInternationalizationEnabled() {
        return internationalizationEnabled;
    }

    public Set<String> getSupportedLocales(Supplier<RealmModel> model) {
        return supportedLocales.get(model);
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public MultivaluedHashMap<String, IdentityProviderMapperModel> getIdentityProviderMappers(Supplier<RealmModel> model) {
        return identityProviderMappers.get(model);
    }

    public Map<String, AuthenticationFlowModel> getAuthenticationFlows(Supplier<RealmModel> model) {
        return authenticationFlows.get(model);
    }

    public Map<String, AuthenticatorConfigModel> getAuthenticatorConfigs(Supplier<RealmModel> model) {
        return authenticatorConfigs.get(model);
    }

    public MultivaluedHashMap<String, AuthenticationExecutionModel> getAuthenticationExecutions(Supplier<RealmModel> model) {
        return authenticationExecutions.get(model);
    }

    public AuthenticationExecutionModel getAuthenticationExecutionByFlowId(Supplier<RealmModel> model, String flowId) {
        return executionsByFlowId.get(model).get(flowId);
    }
    
    public Map<String, AuthenticationExecutionModel> getExecutionsById(Supplier<RealmModel> model) {
        return executionsById.get(model);
    }

    public Map<String, RequiredActionProviderModel> getRequiredActionProviders(Supplier<RealmModel> model) {
        return requiredActionProviders.get(model);
    }

    public Map<String, RequiredActionProviderModel> getRequiredActionProvidersByAlias(Supplier<RealmModel> model) {
        return requiredActionProvidersByAlias.get(model);
    }

    public OTPPolicy getOtpPolicy(Supplier<RealmModel> model) {
        return otpPolicy.get(model);
    }

    public WebAuthnPolicy getWebAuthnPolicy(Supplier<RealmModel> model) {
        return webAuthnPolicy.get(model);
    }

    public WebAuthnPolicy getWebAuthnPasswordlessPolicy(Supplier<RealmModel> model) {
        return webAuthnPasswordlessPolicy.get(model);
    }

    public AuthenticationFlowModel getBrowserFlow(Supplier<RealmModel> model) {
        return browserFlow.get(model);
    }

    public AuthenticationFlowModel getRegistrationFlow(Supplier<RealmModel> model) {
        return registrationFlow.get(model);
    }

    public AuthenticationFlowModel getDirectGrantFlow(Supplier<RealmModel> model) {
        return directGrantFlow.get(model);
    }

    public AuthenticationFlowModel getResetCredentialsFlow(Supplier<RealmModel> model) {
        return resetCredentialsFlow.get(model);
    }

    public AuthenticationFlowModel getClientAuthenticationFlow(Supplier<RealmModel> model) {
        return clientAuthenticationFlow.get(model);
    }

    public AuthenticationFlowModel getDockerAuthenticationFlow(Supplier<RealmModel> model) {
        return dockerAuthenticationFlow.get(model);
    }

    public List<String> getDefaultGroups(Supplier<RealmModel> model) {
        return defaultGroups.get(model);
    }

    public List<String> getClientScopes(Supplier<RealmModel> model) {
        return clientScopes.get(model);
    }

    public List<String> getDefaultDefaultClientScopes(Supplier<RealmModel> model) {
        return defaultDefaultClientScopes.get(model);
    }

    public List<String> getOptionalDefaultClientScopes(Supplier<RealmModel> model) {
        return optionalDefaultClientScopes.get(model);
    }

    public List<AuthenticationFlowModel> getAuthenticationFlowList(Supplier<RealmModel> model) {
        return authenticationFlowList.get(model);
    }

    public List<RequiredActionProviderModel> getRequiredActionProviderList(Supplier<RealmModel> model) {
        return requiredActionProviderList.get(model);
    }

    public MultivaluedHashMap<String, ComponentModel> getComponentsByParent() {
        return componentsByParent;
    }

    public MultivaluedHashMap<String, ComponentModel> getComponentsByParentAndType() {
        return componentsByParentAndType;
    }

    public Map<String, ComponentModel> getComponents() {
        return components;
    }

    public String getAttribute(String name) {
        return attributes != null ? attributes.get(name) : null;
    }

    public Integer getAttribute(String name, Integer defaultValue) {
        String v = getAttribute(name);
        return v != null ? Integer.valueOf(v) : defaultValue;
    }

    public Long getAttribute(String name, Long defaultValue) {
        String v = getAttribute(name);
        return v != null ? Long.valueOf(v) : defaultValue;
    }

    public Boolean getAttribute(String name, Boolean defaultValue) {
        String v = getAttribute(name);
        return v != null ? Boolean.valueOf(v) : defaultValue;
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
}
