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
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.WebAuthnPolicy;
import org.keycloak.models.cache.infinispan.DefaultLazyLoader;
import org.keycloak.models.cache.infinispan.LazyLoader;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

    private static Set<IdentityProviderMapperModel> fetchIdentityProviderMapperSet(RealmModel r) {
        return r.getIdentityProviderMappersStream().collect(Collectors.toSet());
    }

    private static List<RequiredCredentialModel> fetchRequiredCredentials(RealmModel r) {
        return r.getRequiredCredentialsStream().collect(Collectors.toList());
    }

    private static List<IdentityProviderModel> fetchIdentityProviders(RealmModel r) {
        return r.getIdentityProvidersStream().map(IdentityProviderModel::new)
                .collect(Collectors.toList());
    }

    private Map<String, AuthenticationFlowModel> fetchAuthenticationFlows(RealmModel r) {
        Map<String, AuthenticationFlowModel> flows = new HashMap<>();
        for (AuthenticationFlowModel flow : getAuthenticationFlowList(() -> r)) {
            flows.put(flow.getId(), flow);
        }
        return flows;
    }

    private MultivaluedHashMap<String, AuthenticationExecutionModel> fetchAuthenticationExecutions(RealmModel r) {
        return new MultivaluedHashMap<>(getAuthenticationFlowList(() -> r).stream()
                .map(AuthenticationFlowModel::getId)
                .collect(Collectors.toMap(
                        id -> id,
                        id -> r.getAuthenticationExecutionsStream(id).collect(Collectors.toList()))
                ));
    }

    private Map<String, AuthenticationExecutionModel> fetchAuthenticationExecutionsById(RealmModel r) {
        Map<String, AuthenticationExecutionModel> flows = new HashMap<>();
        for (AuthenticationFlowModel flow : getAuthenticationFlowList(() -> r)) {
            r.getAuthenticationExecutionsStream(flow.getId()).forEachOrdered(execution -> {
                flows.put(execution.getId(), execution);
            });
        }
        return flows;
    }

    private Map<String, AuthenticationExecutionModel> fetchAuthenticationExecutionsByFlow(RealmModel r) {
        Map<String, AuthenticationExecutionModel> flows = new HashMap<>();
        for (AuthenticationFlowModel flow : getAuthenticationFlowList(() -> r)) {
            r.getAuthenticationExecutionsStream(flow.getId()).forEachOrdered(execution -> {
                if (execution.getFlowId() != null) {
                    flows.put(execution.getFlowId(), execution);
                }
            });
        }
        return flows;
    }

    private Map<String, RequiredActionProviderModel> fetchRequiredActionProviders(RealmModel r) {
        Map<String, RequiredActionProviderModel> actions = new HashMap<>();
        for (RequiredActionProviderModel action : getRequiredActionProviderList(() -> r)) {
            actions.put(action.getId(), action);
        }
        return actions;
    }

    private Map<String, RequiredActionProviderModel> fetchRequiredActionsByAlias(RealmModel r) {
        Map<String, RequiredActionProviderModel> actions = new HashMap<>();
        for (RequiredActionProviderModel action : getRequiredActionProviderList(() -> r)) {
            actions.put(action.getAlias(), action);
        }
        return actions;
    }

    private static String fetchMasterAdminClient(RealmModel r) {
        ClientModel masterAdminClient = r.getMasterAdminClient();
        return (masterAdminClient != null) ? masterAdminClient.getId() : null;
    }

    private static Set<String> fetchSupportedLocales(RealmModel r) {
        return r.getSupportedLocalesStream().collect(Collectors.toSet());
    }

    private static List<AuthenticationFlowModel> fetchAuthenticationFlowList(RealmModel r) {
        return r.getAuthenticationFlowsStream().collect(Collectors.toList());
    }

    private static Map<String, AuthenticatorConfigModel> fetchAuthenticatorConfigs(RealmModel r) {
        return r.getAuthenticatorConfigsStream()
                .collect(Collectors.toMap(AuthenticatorConfigModel::getId, Function.identity()));
    }

    private static List<RequiredActionProviderModel> fetchRequiredActionProviderList(RealmModel r) {
        return r.getRequiredActionProvidersStream().collect(Collectors.toList());
    }

    private static List<String> fetchDefaultGroups(RealmModel r) {
        return r.getDefaultGroupsStream().map(GroupModel::getId).collect(Collectors.toList());
    }

    private static MultivaluedHashMap<String, ComponentModel> fetchComponentsByParentAndType(RealmModel r) {
        MultivaluedHashMap<String, ComponentModel> map = new MultivaluedHashMap<>();

        r.getComponentsStream().forEach(component ->
                map.add(component.getParentId() + component.getProviderType(), component)
        );

        return map;
    }

    private static MultivaluedHashMap<String, ComponentModel> fetchComponentsByParent(RealmModel r) {
        MultivaluedHashMap<String, ComponentModel> map = new MultivaluedHashMap<>();

        r.getComponentsStream().forEach(component ->
                map.add(component.getParentId(), component)
        );

        return map;
    }

    private static Map<String, ComponentModel> fetchComponents(RealmModel r) {
        return r.getComponentsStream()
                .collect(Collectors.toMap(component -> component.getId(), Function.identity()));
    }

    private static Map<String, String> fetchAttributes(RealmModel r) {
        try {
            return r.getAttributes();
        } catch (UnsupportedOperationException ignore) {
            return null;
        }
    }

    private static Map<String, String> fetchClientScopes(RealmModel r) {
        return r.getClientScopesStream().collect(Collectors.toMap(ClientScopeModel::getName, ClientScopeModel::getId));
    }

    private static List<String> fetchDefaultClientScopes(RealmModel r) {
        return r.getDefaultClientScopesStream(true).map(ClientScopeModel::getId).collect(Collectors.toList());
    }

    private static List<String> fetchOptionalClientScopes(RealmModel r) {
        return r.getDefaultClientScopesStream(false).map(ClientScopeModel::getId).collect(Collectors.toList());
    }

    private MultivaluedHashMap<String, IdentityProviderMapperModel> fetchIdentityProviderMappers(RealmModel r) {
        MultivaluedHashMap<String, IdentityProviderMapperModel> mappers = new MultivaluedHashMap<>();
        for (IdentityProviderMapperModel mapper : getIdentityProviderMapperSet(() -> r)) {
            mappers.add(mapper.getIdentityProviderAlias(), mapper);
        }
        return mappers;
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
    protected LazyLoader<RealmModel, Boolean> identityFederationEnabled;
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
    protected LazyLoader<RealmModel, String> masterAdminClient;

    protected LazyLoader<RealmModel, List<RequiredCredentialModel>> requiredCredentials;
    protected LazyLoader<RealmModel, MultivaluedHashMap<String, ComponentModel>> componentsByParent;
    protected LazyLoader<RealmModel, MultivaluedHashMap<String, ComponentModel>> componentsByParentAndType;
    protected LazyLoader<RealmModel, Map<String, ComponentModel>> components;
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
    protected Set<String> adminEnabledEventOperations = new HashSet<>();
    protected boolean adminEventsDetailsEnabled;
    protected LazyLoader<RealmModel, String> defaultRoleId;
    private boolean allowUserManagedAccess;

    protected LazyLoader<RealmModel, List<String>> defaultGroups;
    protected LazyLoader<RealmModel, Map<String, String>> clientScopes;
    protected LazyLoader<RealmModel, List<String>> defaultDefaultClientScopes;
    protected LazyLoader<RealmModel, List<String>> optionalDefaultClientScopes;
    protected boolean internationalizationEnabled;
    protected LazyLoader<RealmModel, Set<String>> supportedLocales;
    protected String defaultLocale;
    protected LazyLoader<RealmModel, MultivaluedHashMap<String, IdentityProviderMapperModel>> identityProviderMappers;
    protected LazyLoader<RealmModel, Set<IdentityProviderMapperModel>> identityProviderMapperSet;

    protected LazyLoader<RealmModel, Map<String, String>> attributes;

    private Map<String, Integer> userActionTokenLifespans;

    protected LazyLoader<RealmModel, Map<String, Map<String,String>>> realmLocalizationTexts;

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
        identityFederationEnabled = new DefaultLazyLoader<>(RealmModel::isIdentityFederationEnabled, () -> false);
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

        requiredCredentials = new DefaultLazyLoader<>(CachedRealm::fetchRequiredCredentials, Collections::emptyList);
        userActionTokenLifespans = Collections.unmodifiableMap(new HashMap<>(model.getUserActionTokenLifespans()));

        this.identityProviders = new DefaultLazyLoader<>(CachedRealm::fetchIdentityProviders, Collections::emptyList);

        this.identityProviderMapperSet = new DefaultLazyLoader<>(CachedRealm::fetchIdentityProviderMapperSet, Collections::emptySet);
        this.identityProviderMappers = new DefaultLazyLoader<>(this::fetchIdentityProviderMappers, MultivaluedHashMap::new);

        smtpConfig = new DefaultLazyLoader<>(RealmModel::getSmtpConfig, Collections::emptyMap);
        browserSecurityHeaders = model.getBrowserSecurityHeaders();

        eventsEnabled = model.isEventsEnabled();
        eventsExpiration = model.getEventsExpiration();
        eventsListeners = new DefaultLazyLoader<>(r -> r.getEventsListenersStream().collect(Collectors.toSet()), Collections::emptySet);
        enabledEventTypes = new DefaultLazyLoader<>(r -> r.getEnabledEventTypesStream().collect(Collectors.toSet()), Collections::emptySet);;

        adminEventsEnabled = model.isAdminEventsEnabled();
        adminEventsDetailsEnabled = model.isAdminEventsDetailsEnabled();

        defaultRoleId = new DefaultLazyLoader<>(r -> r.getDefaultRole().getId());
        this.masterAdminClient = new DefaultLazyLoader<>(CachedRealm::fetchMasterAdminClient, () -> null);

        cacheClientScopes(model);

        internationalizationEnabled = model.isInternationalizationEnabled();
        supportedLocales = new DefaultLazyLoader<>(CachedRealm::fetchSupportedLocales, () -> Collections.emptySet());
        defaultLocale = model.getDefaultLocale();
        authenticationFlowList = new DefaultLazyLoader<>(CachedRealm::fetchAuthenticationFlowList, Collections::emptyList);
        this.authenticationFlows = new DefaultLazyLoader<>(this::fetchAuthenticationFlows, Collections::emptyMap);
        this.authenticationExecutions = new DefaultLazyLoader<>(this::fetchAuthenticationExecutions, MultivaluedHashMap::new);

        this.executionsById = new DefaultLazyLoader<>(this::fetchAuthenticationExecutionsById, Collections::emptyMap);

        this.executionsByFlowId = new DefaultLazyLoader<>(this::fetchAuthenticationExecutionsByFlow, Collections::emptyMap);

        authenticatorConfigs = new DefaultLazyLoader<>(CachedRealm::fetchAuthenticatorConfigs, Collections::emptyMap);
        requiredActionProviderList = new DefaultLazyLoader<>(CachedRealm::fetchRequiredActionProviderList, () -> Collections.emptyList());
        this.requiredActionProviders = new DefaultLazyLoader<>(this::fetchRequiredActionProviders, Collections::emptyMap);
        this.requiredActionProvidersByAlias = new DefaultLazyLoader<>(this::fetchRequiredActionsByAlias, Collections::emptyMap);

        defaultGroups = new DefaultLazyLoader<>(CachedRealm::fetchDefaultGroups, Collections::emptyList);;

        browserFlow = new DefaultLazyLoader<>(RealmModel::getBrowserFlow);
        registrationFlow = new DefaultLazyLoader<>(RealmModel::getRegistrationFlow);
        directGrantFlow = new DefaultLazyLoader<>(RealmModel::getDirectGrantFlow);
        resetCredentialsFlow = new DefaultLazyLoader<>(RealmModel::getResetCredentialsFlow);
        clientAuthenticationFlow = new DefaultLazyLoader<>(RealmModel::getClientAuthenticationFlow);
        dockerAuthenticationFlow = new DefaultLazyLoader<>(RealmModel::getDockerAuthenticationFlow);

        componentsByParentAndType = new DefaultLazyLoader<>(CachedRealm::fetchComponentsByParentAndType, MultivaluedHashMap::new);

        componentsByParent = new DefaultLazyLoader<>(CachedRealm::fetchComponentsByParent, MultivaluedHashMap::new);

        components = new DefaultLazyLoader<>(CachedRealm::fetchComponents, Collections::emptyMap);

        attributes = new DefaultLazyLoader<>(CachedRealm::fetchAttributes, Collections::emptyMap);

        realmLocalizationTexts = new DefaultLazyLoader<>(RealmModel::getRealmLocalizationTexts, Collections::emptyMap);
    }

    protected void cacheClientScopes(RealmModel model) {
        clientScopes = new DefaultLazyLoader<>(CachedRealm::fetchClientScopes,Collections::emptyMap);
        defaultDefaultClientScopes = new DefaultLazyLoader<>(CachedRealm::fetchDefaultClientScopes, Collections::emptyList);
        optionalDefaultClientScopes = new DefaultLazyLoader<>(CachedRealm::fetchOptionalClientScopes, Collections::emptyList);
    }

    public String getMasterAdminClient(Supplier<RealmModel> realm) {
        return masterAdminClient.get(realm);
    }

    public String getDefaultRoleId(Supplier<RealmModel> realm) {
        return defaultRoleId.get(realm);
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

    public List<RequiredCredentialModel> getRequiredCredentials(Supplier<RealmModel> realm) {
        return requiredCredentials.get(realm);
    }

    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy;
    }

    public boolean isIdentityFederationEnabled(Supplier<RealmModel> realm) {
        return identityFederationEnabled.get(realm);
    }

    public Map<String, String> getSmtpConfig(Supplier<RealmModel> realm) {
        return smtpConfig.get(realm);
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

    public Set<String> getEventsListeners(Supplier<RealmModel> realm) {
        return eventsListeners.get(realm);
    }

    public Set<String> getEnabledEventTypes(Supplier<RealmModel> realm) {
        return enabledEventTypes.get(realm);
    }

    public boolean isAdminEventsEnabled() {
        return adminEventsEnabled;
    }

    public Set<String> getAdminEnabledEventOperations() {
        return adminEnabledEventOperations;
    }

    public boolean isAdminEventsDetailsEnabled() {
        return adminEventsDetailsEnabled;
    }

    public List<IdentityProviderModel> getIdentityProviders(Supplier<RealmModel> realm) {
        return identityProviders.get(realm);
    }

    public boolean isInternationalizationEnabled() {
        return internationalizationEnabled;
    }

    public Set<String> getSupportedLocales(Supplier<RealmModel> realm) {
        return supportedLocales.get(realm);
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public MultivaluedHashMap<String, IdentityProviderMapperModel> getIdentityProviderMappers(Supplier<RealmModel> realm) {
        return identityProviderMappers.get(realm);
    }

    public Set<IdentityProviderMapperModel> getIdentityProviderMapperSet(Supplier<RealmModel> realm) {
        return identityProviderMapperSet.get(realm);
    }

    public Map<String, AuthenticationFlowModel> getAuthenticationFlows(Supplier<RealmModel> realm) {
        return authenticationFlows.get(realm);
    }

    public Map<String, AuthenticatorConfigModel> getAuthenticatorConfigs(Supplier<RealmModel> realm) {
        return authenticatorConfigs.get(realm);
    }

    public MultivaluedHashMap<String, AuthenticationExecutionModel> getAuthenticationExecutions(Supplier<RealmModel> realm) {
        return authenticationExecutions.get(realm);
    }

    public AuthenticationExecutionModel getAuthenticationExecutionByFlowId(Supplier<RealmModel> realm, String flowId) {
        return executionsByFlowId.get(realm).get(flowId);
    }
    
    public Map<String, AuthenticationExecutionModel> getExecutionsById(Supplier<RealmModel> realm) {
        return executionsById.get(realm);
    }

    public Map<String, RequiredActionProviderModel> getRequiredActionProviders(Supplier<RealmModel> realm) {
        return requiredActionProviders.get(realm);
    }

    public Map<String, RequiredActionProviderModel> getRequiredActionProvidersByAlias(Supplier<RealmModel> realm) {
        return requiredActionProvidersByAlias.get(realm);
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

    public AuthenticationFlowModel getBrowserFlow(Supplier<RealmModel> realm) {
        return browserFlow.get(realm);
    }

    public AuthenticationFlowModel getRegistrationFlow(Supplier<RealmModel> realm) {
        return registrationFlow.get(realm);
    }

    public AuthenticationFlowModel getDirectGrantFlow(Supplier<RealmModel> realm) {
        return directGrantFlow.get(realm);
    }

    public AuthenticationFlowModel getResetCredentialsFlow(Supplier<RealmModel> realm) {
        return resetCredentialsFlow.get(realm);
    }

    public AuthenticationFlowModel getClientAuthenticationFlow(Supplier<RealmModel> realm) {
        return clientAuthenticationFlow.get(realm);
    }

    public AuthenticationFlowModel getDockerAuthenticationFlow(Supplier<RealmModel> realm) {
        return dockerAuthenticationFlow.get(realm);
    }

    public List<String> getDefaultGroups(Supplier<RealmModel> realm) {
        return defaultGroups.get(realm);
    }

    public Collection<String> getClientScopes(Supplier<RealmModel> realm) {
        return clientScopes.get(realm).values();
    }

    public List<String> getDefaultDefaultClientScopes(Supplier<RealmModel> realm) {
        return defaultDefaultClientScopes.get(realm);
    }

    public List<String> getOptionalDefaultClientScopes(Supplier<RealmModel> realm) {
        return optionalDefaultClientScopes.get(realm);
    }

    public List<AuthenticationFlowModel> getAuthenticationFlowList(Supplier<RealmModel> realm) {
        return authenticationFlowList.get(realm);
    }

    public List<RequiredActionProviderModel> getRequiredActionProviderList(Supplier<RealmModel> realm) {
        return requiredActionProviderList.get(realm);
    }

    public MultivaluedHashMap<String, ComponentModel> getComponentsByParent(Supplier<RealmModel> realm) {
        return componentsByParent.get(realm);
    }

    public MultivaluedHashMap<String, ComponentModel> getComponentsByParentAndType(Supplier<RealmModel> realm) {
        return componentsByParentAndType.get(realm);
    }

    public Map<String, ComponentModel> getComponents(Supplier<RealmModel> realm) {
        return components.get(realm);
    }

    public String getAttribute(Supplier<RealmModel> realm, String name) {
        return attributes != null ? attributes.get(realm).get(name) : null;
    }

    public Integer getAttribute(Supplier<RealmModel> realm, String name, Integer defaultValue) {
        String v = getAttribute(realm, name);
        return v != null ? Integer.parseInt(v) : defaultValue;
    }

    public Long getAttribute(Supplier<RealmModel> realm, String name, Long defaultValue) {
        String v = getAttribute(realm, name);
        return v != null ? Long.parseLong(v) : defaultValue;
    }

    public Boolean getAttribute(Supplier<RealmModel> realm, String name, Boolean defaultValue) {
        String v = getAttribute(realm, name);
        return v != null ? Boolean.parseBoolean(v) : defaultValue;
    }

    public Map<String, String> getAttributes(Supplier<RealmModel> realm) {
        return attributes.get(realm);
    }

    public boolean isAllowUserManagedAccess() {
        return allowUserManagedAccess;
    }

    public Map<String, Map<String, String>> getRealmLocalizationTexts(Supplier<RealmModel> model) {
        return realmLocalizationTexts.get(model);
    }

    public String getClientScopeByName(Supplier<RealmModel> realm, String name) {
        return clientScopes.get(realm).get(name);
    }
}
