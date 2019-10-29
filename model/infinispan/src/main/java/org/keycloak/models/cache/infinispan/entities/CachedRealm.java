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
import org.keycloak.models.*;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.cache.infinispan.DefaultLazyLoader;
import org.keycloak.models.cache.infinispan.LazyLoader;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
    private boolean identityFederationEnabled;
    protected boolean editUsernameAllowed;
    //--- brute force settings
    protected boolean bruteForceProtected;
    private boolean permanentLockout;
    protected int maxFailureWaitSeconds;
    protected int minimumQuickLoginWaitSeconds;
    protected int waitIncrementSeconds;
    protected long quickLoginCheckMilliSeconds;
    protected int maxDeltaTimeSeconds;
    protected int failureFactor;
    //--- end brute force settings

    private String defaultSignatureAlgorithm;
    protected boolean revokeRefreshToken;
    protected int refreshTokenMaxReuse;
    protected int ssoSessionIdleTimeout;
    protected int ssoSessionMaxLifespan;
    private int ssoSessionIdleTimeoutRememberMe;
    private int ssoSessionMaxLifespanRememberMe;
    protected int offlineSessionIdleTimeout;
    // KEYCLOAK-7688 Offline Session Max for Offline Token
    private boolean offlineSessionMaxLifespanEnabled;
    private int offlineSessionMaxLifespan;
    protected int accessTokenLifespan;
    protected int accessTokenLifespanForImplicitFlow;
    protected int accessCodeLifespan;
    protected int accessCodeLifespanUserAction;
    protected int accessCodeLifespanLogin;
    private int actionTokenGeneratedByAdminLifespan;
    private int actionTokenGeneratedByUserLifespan;
    protected int notBefore;
    protected PasswordPolicy passwordPolicy;
    private OTPPolicy otpPolicy;
    private WebAuthnPolicy webAuthnPolicy;

    private String loginTheme;
    private String accountTheme;
    private String adminTheme;
    private String emailTheme;
    private final transient LazyLoader<RealmModel, String> masterAdminClient = DefaultLazyLoader.create(rm -> {
        ClientModel cm = rm.getMasterAdminClient();
        return (cm != null) ? cm.getId() : null;
    }, null);

    protected final transient LazyLoader<RealmModel, List<RequiredCredentialModel>> requiredCredentials =
            DefaultLazyLoader.forList(RealmModel::getRequiredCredentials);
    private final transient LazyLoader<RealmModel, List<ComponentModel>> componentsList =
            new DefaultLazyLoader<>(RealmModel::getComponents, LinkedList::new);
    private final transient LazyLoader<RealmModel, MultivaluedHashMap<String, ComponentModel>> componentsByParent =
            DefaultLazyLoader.forMultivaluedMap(rm -> componentsList.get(() -> rm), ComponentModel::getParentId);
    private final transient LazyLoader<RealmModel, MultivaluedHashMap<String, ComponentModel>> componentsByParentAndType =
            DefaultLazyLoader.forMultivaluedMap(rm -> componentsList.get(() -> rm), cm -> cm.getParentId() + cm.getProviderType());
    protected final transient LazyLoader<RealmModel, Map<String, ComponentModel>> components =
            DefaultLazyLoader.forStreamAsMap(rm -> componentsList.get(() -> rm).stream(), ComponentModel::getId);
    protected final transient LazyLoader<RealmModel, List<IdentityProviderModel>> identityProviders =
            DefaultLazyLoader.forStreamAsList(rm -> rm.getIdentityProviders().stream().map(IdentityProviderModel::new));

    protected Map<String, String> browserSecurityHeaders;
    private Map<String, String> smtpConfig;
    private final transient LazyLoader<RealmModel, List<AuthenticationFlowModel>> authenticationFlowList
            = DefaultLazyLoader.forList(RealmModel::getAuthenticationFlows);
    protected final transient LazyLoader<RealmModel, Map<String, AuthenticationFlowModel>> authenticationFlows =
            DefaultLazyLoader.forStreamAsMap(
                    rm -> authenticationFlowList.get(() -> rm).stream(), AuthenticationFlowModel::getId);
    private final transient LazyLoader<RealmModel, Map<String, AuthenticatorConfigModel>> authenticatorConfigs =
            DefaultLazyLoader.forStreamAsMap(rm -> rm.getAuthenticatorConfigs().stream(), AuthenticatorConfigModel::getId);
    private final transient LazyLoader<RealmModel, List<RequiredActionProviderModel>> requiredActionProviderList =
            DefaultLazyLoader.forList(RealmModel::getRequiredActionProviders);
    private final transient LazyLoader<RealmModel, Map<String, RequiredActionProviderModel>> requiredActionProviders =
            DefaultLazyLoader.forStreamAsMap(
                    rm -> requiredActionProviderList.get(() -> rm).stream(), RequiredActionProviderModel::getId);
    private final transient LazyLoader<RealmModel, Map<String, RequiredActionProviderModel>> requiredActionProvidersByAlias =
            DefaultLazyLoader.forStreamAsMap(
                    rm -> requiredActionProviderList.get(() -> rm).stream(), RequiredActionProviderModel::getAlias);
    protected final transient LazyLoader<RealmModel, MultivaluedHashMap<String, AuthenticationExecutionModel>> authenticationExecutions =
            DefaultLazyLoader.forMultivaluedMap(rm -> {
                MultivaluedHashMap<String, AuthenticationExecutionModel> result = new MultivaluedHashMap<>();
                authenticationFlowList.get(() -> rm)
                        .forEach(flow -> result.addAll(flow.getId(), rm.getAuthenticationExecutions(flow.getId())));
                return result;
            });
    private final transient LazyLoader<RealmModel, Map<String, AuthenticationExecutionModel>> executionsById =
            DefaultLazyLoader.forMap(rm -> {
                HashMap<String, AuthenticationExecutionModel> result = new HashMap<>();
                authenticationFlowList.get(() -> rm).forEach(flow ->
                        rm.getAuthenticationExecutions(flow.getId())
                                .forEach(execution -> result.put(execution.getId(), execution)));
                return result;
            });

    protected AuthenticationFlowModel browserFlow;
    protected AuthenticationFlowModel registrationFlow;
    protected AuthenticationFlowModel directGrantFlow;
    protected AuthenticationFlowModel resetCredentialsFlow;
    protected AuthenticationFlowModel clientAuthenticationFlow;
    private AuthenticationFlowModel dockerAuthenticationFlow;

    protected boolean eventsEnabled;
    private long eventsExpiration;
    protected Set<String> eventsListeners;
    protected Set<String> enabledEventTypes;
    protected boolean adminEventsEnabled;
    private transient LazyLoader<RealmModel, Set<String>> adminEnabledEventOperations;
    protected boolean adminEventsDetailsEnabled;
    protected final transient LazyLoader<RealmModel, List<String>> defaultRoles =
            DefaultLazyLoader.forList(RealmModel::getDefaultRoles);
    private boolean allowUserManagedAccess;

    private final transient LazyLoader<RealmModel, List<String>> defaultGroups =
            DefaultLazyLoader.forList(rm -> rm.getDefaultGroups().stream().map(GroupModel::getId).collect(Collectors.toList()));
    protected final transient LazyLoader<RealmModel, List<String>> clientScopes =
            DefaultLazyLoader.forStreamAsList(rm -> rm.getClientScopes().stream().map(ClientScopeModel::getId));
    private final transient LazyLoader<RealmModel, List<String>> defaultDefaultClientScopes =
            DefaultLazyLoader.forStreamAsList(
                    rm -> rm.getDefaultClientScopes(true).stream().map(ClientScopeModel::getId));
    private final transient LazyLoader<RealmModel, List<String>> optionalDefaultClientScopes =
            DefaultLazyLoader.forStreamAsList(
                    rm -> rm.getDefaultClientScopes(false).stream().map(ClientScopeModel::getId));
    protected boolean internationalizationEnabled;
    protected Set<String> supportedLocales;
    private String defaultLocale;
    private final transient LazyLoader<RealmModel, Set<IdentityProviderMapperModel>> identityProviderMapperSet =
            DefaultLazyLoader.forSet(RealmModel::getIdentityProviderMappers);
    private final transient LazyLoader<RealmModel, MultivaluedHashMap<String, IdentityProviderMapperModel>> identityProviderMappers =
            DefaultLazyLoader.forMultivaluedMap(
                    realmModel -> identityProviderMapperSet.get(() -> realmModel),
                    IdentityProviderMapperModel::getIdentityProviderAlias);

    protected final transient LazyLoader<RealmModel, Map<String, String>> attributes =
            DefaultLazyLoader.forMap(realmModel -> {
                try {
                    return realmModel.getAttributes();
                } catch (UnsupportedOperationException ex) {
                    return Collections.emptyMap();
                }
            });

    private final LazyLoader<RealmModel, Map<String, Integer>> userActionTokenLifespans =
            DefaultLazyLoader.forMap(rm -> Collections.unmodifiableMap(new HashMap<>(rm.getUserActionTokenLifespans())));

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
        identityFederationEnabled = model.isIdentityFederationEnabled();
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

        loginTheme = model.getLoginTheme();
        accountTheme = model.getAccountTheme();
        adminTheme = model.getAdminTheme();
        emailTheme = model.getEmailTheme();

        smtpConfig = model.getSmtpConfig();
        browserSecurityHeaders = model.getBrowserSecurityHeaders();

        eventsEnabled = model.isEventsEnabled();
        eventsExpiration = model.getEventsExpiration();
        eventsListeners = model.getEventsListeners();
        enabledEventTypes = model.getEnabledEventTypes();

        adminEventsEnabled = model.isAdminEventsEnabled();
        adminEventsDetailsEnabled = model.isAdminEventsDetailsEnabled();

        internationalizationEnabled = model.isInternationalizationEnabled();
        supportedLocales = model.getSupportedLocales();
        defaultLocale = model.getDefaultLocale();

        browserFlow = model.getBrowserFlow();
        registrationFlow = model.getRegistrationFlow();
        directGrantFlow = model.getDirectGrantFlow();
        resetCredentialsFlow = model.getResetCredentialsFlow();
        clientAuthenticationFlow = model.getClientAuthenticationFlow();
        dockerAuthenticationFlow = model.getDockerAuthenticationFlow();
    }

    public Set<IdentityProviderMapperModel> getIdentityProviderMapperSet(Supplier<RealmModel> realmModel) {
        return identityProviderMapperSet.get(realmModel);
    }

    public String getMasterAdminClient(Supplier<RealmModel> realmModel) {
        return masterAdminClient.get(realmModel);
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

    public List<String> getDefaultRoles(Supplier<RealmModel> realmModel) {
        return defaultRoles.get(realmModel);
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

    public Map<String, Integer> getUserActionTokenLifespans(Supplier<RealmModel> realmModel) {
        return userActionTokenLifespans.get(realmModel);
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
    public int getActionTokenGeneratedByUserLifespan(Supplier<RealmModel> realmModel, String actionTokenId) {
        if (actionTokenId == null || this.userActionTokenLifespans.get(realmModel).get(actionTokenId) == null)
            return getActionTokenGeneratedByUserLifespan();
        return this.userActionTokenLifespans.get(realmModel).get(actionTokenId);
    }

    public List<RequiredCredentialModel> getRequiredCredentials(Supplier<RealmModel> realmModel) {
        return requiredCredentials.get(realmModel);
    }

    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy;
    }

    public boolean isIdentityFederationEnabled() {
        return identityFederationEnabled;
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

    public Set<String> getAdminEnabledEventOperations(Supplier<RealmModel> realmModel) {
        return adminEnabledEventOperations.get(realmModel);
    }

    public boolean isAdminEventsDetailsEnabled() {
        return adminEventsDetailsEnabled;
    }

    public List<IdentityProviderModel> getIdentityProviders(Supplier<RealmModel> realmModel) {
        return identityProviders.get(realmModel);
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

    public MultivaluedHashMap<String, IdentityProviderMapperModel> getIdentityProviderMappers(Supplier<RealmModel> realmModel) {
        return identityProviderMappers.get(realmModel);
    }

    public Map<String, AuthenticationFlowModel> getAuthenticationFlows(Supplier<RealmModel> realmModel) {
        return authenticationFlows.get(realmModel);
    }

    public Map<String, AuthenticatorConfigModel> getAuthenticatorConfigs(Supplier<RealmModel> realmModel) {
        return authenticatorConfigs.get(realmModel);
    }

    public MultivaluedHashMap<String, AuthenticationExecutionModel> getAuthenticationExecutions(Supplier<RealmModel> realmModel) {
        return authenticationExecutions.get(realmModel);
    }

    public Map<String, AuthenticationExecutionModel> getExecutionsById(Supplier<RealmModel> realmModel) {
        return executionsById.get(realmModel);
    }

    public Map<String, RequiredActionProviderModel> getRequiredActionProviders(Supplier<RealmModel> realmModel) {
        return requiredActionProviders.get(realmModel);
    }

    public Map<String, RequiredActionProviderModel> getRequiredActionProvidersByAlias(Supplier<RealmModel> realmModel) {
        return requiredActionProvidersByAlias.get(realmModel);
    }

    public OTPPolicy getOtpPolicy() {
        return otpPolicy;
    }

    public WebAuthnPolicy getWebAuthnPolicy() {
        return webAuthnPolicy;
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

    public List<String> getDefaultGroups(Supplier<RealmModel> realmModel) {
        return defaultGroups.get(realmModel);
    }

    public List<String> getClientScopes(Supplier<RealmModel> realmModel) {
        return clientScopes.get(realmModel);
    }

    public List<String> getDefaultDefaultClientScopes(Supplier<RealmModel> realmModel) {
        return defaultDefaultClientScopes.get(realmModel);
    }

    public List<String> getOptionalDefaultClientScopes(Supplier<RealmModel> realmModel) {
        return optionalDefaultClientScopes.get(realmModel);
    }

    public List<AuthenticationFlowModel> getAuthenticationFlowList(Supplier<RealmModel> realmModel) {
        return authenticationFlowList.get(realmModel);
    }

    public List<RequiredActionProviderModel> getRequiredActionProviderList(Supplier<RealmModel> realmModel) {
        return requiredActionProviderList.get(realmModel);
    }

    public MultivaluedHashMap<String, ComponentModel> getComponentsByParent(Supplier<RealmModel> realmModel) {
        return componentsByParent.get(realmModel);
    }

    public MultivaluedHashMap<String, ComponentModel> getComponentsByParentAndType(Supplier<RealmModel> realmModel) {
        return componentsByParentAndType.get(realmModel);
    }

    public Map<String, ComponentModel> getComponents(Supplier<RealmModel> realmModel) {
        return components.get(realmModel);
    }

    public String getAttribute(Supplier<RealmModel> realmModel, String name) {
        return attributes != null ? attributes.get(realmModel).get(name) : null;
    }

    public Integer getAttribute(Supplier<RealmModel> realmModel, String name, Integer defaultValue) {
        String v = getAttribute(realmModel, name);
        return v != null ? Integer.parseInt(v) : defaultValue;
    }

    public Long getAttribute(Supplier<RealmModel> realmModel, String name, Long defaultValue) {
        String v = getAttribute(realmModel, name);
        return v != null ? Long.parseLong(v) : defaultValue;
    }

    public Boolean getAttribute(Supplier<RealmModel> realmModel, String name, Boolean defaultValue) {
        String v = getAttribute(realmModel, name);
        return v != null ? Boolean.parseBoolean(v) : defaultValue;
    }

    public Map<String, String> getAttributes(Supplier<RealmModel> realmModel) {
        return attributes.get(realmModel);
    }

    public boolean isAllowUserManagedAccess() {
        return allowUserManagedAccess;
    }
}
