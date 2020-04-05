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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    protected String masterAdminClient;

    protected List<RequiredCredentialModel> requiredCredentials;
    protected MultivaluedHashMap<String, ComponentModel> componentsByParent = new MultivaluedHashMap<>();
    protected MultivaluedHashMap<String, ComponentModel> componentsByParentAndType = new MultivaluedHashMap<>();
    protected Map<String, ComponentModel> components = new HashMap<>();
    protected List<IdentityProviderModel> identityProviders;

    protected Map<String, String> browserSecurityHeaders;
    protected Map<String, String> smtpConfig;
    protected Map<String, AuthenticationFlowModel> authenticationFlows = new HashMap<>();
    protected List<AuthenticationFlowModel> authenticationFlowList;
    protected Map<String, AuthenticatorConfigModel> authenticatorConfigs = new HashMap<>();
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

    protected boolean eventsEnabled;
    protected long eventsExpiration;
    protected Set<String> eventsListeners;
    protected Set<String> enabledEventTypes;
    protected boolean adminEventsEnabled;
    protected Set<String> adminEnabledEventOperations = new HashSet<>();
    protected boolean adminEventsDetailsEnabled;
    protected List<String> defaultRoles;
    private boolean allowUserManagedAccess;

    public Set<IdentityProviderMapperModel> getIdentityProviderMapperSet() {
        return identityProviderMapperSet;
    }

    protected List<String> defaultGroups = new LinkedList<>();
    protected List<String> clientScopes = new LinkedList<>();
    protected List<String> defaultDefaultClientScopes = new LinkedList<>();
    protected List<String> optionalDefaultClientScopes = new LinkedList<>();
    protected boolean internationalizationEnabled;
    protected Set<String> supportedLocales;
    protected String defaultLocale;
    protected MultivaluedHashMap<String, IdentityProviderMapperModel> identityProviderMappers = new MultivaluedHashMap<>();
    protected Set<IdentityProviderMapperModel> identityProviderMapperSet;

    protected Map<String, String> attributes;

    private Map<String, Integer> userActionTokenLifespans;

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
        webAuthnPasswordlessPolicy = model.getWebAuthnPolicyPasswordless();

        loginTheme = model.getLoginTheme();
        accountTheme = model.getAccountTheme();
        adminTheme = model.getAdminTheme();
        emailTheme = model.getEmailTheme();

        requiredCredentials = model.getRequiredCredentials();
        userActionTokenLifespans = Collections.unmodifiableMap(new HashMap<>(model.getUserActionTokenLifespans()));

        this.identityProviders = new ArrayList<>();

        for (IdentityProviderModel identityProviderModel : model.getIdentityProviders()) {
            this.identityProviders.add(new IdentityProviderModel(identityProviderModel));
        }
        this.identityProviders = Collections.unmodifiableList(this.identityProviders);

        this.identityProviderMapperSet = model.getIdentityProviderMappers();
        for (IdentityProviderMapperModel mapper : identityProviderMapperSet) {
            identityProviderMappers.add(mapper.getIdentityProviderAlias(), mapper);
        }



        smtpConfig = model.getSmtpConfig();
        browserSecurityHeaders = model.getBrowserSecurityHeaders();

        eventsEnabled = model.isEventsEnabled();
        eventsExpiration = model.getEventsExpiration();
        eventsListeners = model.getEventsListeners();
        enabledEventTypes = model.getEnabledEventTypes();

        adminEventsEnabled = model.isAdminEventsEnabled();
        adminEventsDetailsEnabled = model.isAdminEventsDetailsEnabled();

        defaultRoles = model.getDefaultRoles();
        ClientModel masterAdminClient = model.getMasterAdminClient();
        this.masterAdminClient = (masterAdminClient != null) ? masterAdminClient.getId() : null;

        cacheClientScopes(model);

        internationalizationEnabled = model.isInternationalizationEnabled();
        supportedLocales = model.getSupportedLocales();
        defaultLocale = model.getDefaultLocale();
        authenticationFlowList = model.getAuthenticationFlows();
        for (AuthenticationFlowModel flow : authenticationFlowList) {
            this.authenticationFlows.put(flow.getId(), flow);
            authenticationExecutions.put(flow.getId(), new LinkedList<>());
            for (AuthenticationExecutionModel execution : model.getAuthenticationExecutions(flow.getId())) {
                authenticationExecutions.add(flow.getId(), execution);
                executionsById.put(execution.getId(), execution);
                if (execution.getFlowId() != null) {
                    executionsByFlowId.put(execution.getFlowId(), execution);
                }
            }
        }

        for (AuthenticatorConfigModel authenticator : model.getAuthenticatorConfigs()) {
            authenticatorConfigs.put(authenticator.getId(), authenticator);
        }
        requiredActionProviderList = model.getRequiredActionProviders();
        for (RequiredActionProviderModel action : requiredActionProviderList) {
            this.requiredActionProviders.put(action.getId(), action);
            requiredActionProvidersByAlias.put(action.getAlias(), action);
        }

        for (GroupModel group : model.getDefaultGroups()) {
            defaultGroups.add(group.getId());
        }

        browserFlow = model.getBrowserFlow();
        registrationFlow = model.getRegistrationFlow();
        directGrantFlow = model.getDirectGrantFlow();
        resetCredentialsFlow = model.getResetCredentialsFlow();
        clientAuthenticationFlow = model.getClientAuthenticationFlow();
        dockerAuthenticationFlow = model.getDockerAuthenticationFlow();

        for (ComponentModel component : model.getComponents()) {
            componentsByParentAndType.add(component.getParentId() + component.getProviderType(), component);
        }
        for (ComponentModel component : model.getComponents()) {
            componentsByParent.add(component.getParentId(), component);
        }
        for (ComponentModel component : model.getComponents()) {
            components.put(component.getId(), component);
        }

        try {
            attributes = model.getAttributes();
        } catch (UnsupportedOperationException ex) {
        }

    }

    protected void cacheClientScopes(RealmModel model) {
        for (ClientScopeModel clientScope : model.getClientScopes()) {
            clientScopes.add(clientScope.getId());
        }
        for (ClientScopeModel clientScope : model.getDefaultClientScopes(true)) {
            defaultDefaultClientScopes.add(clientScope.getId());
        }
        for (ClientScopeModel clientScope : model.getDefaultClientScopes(false)) {
            optionalDefaultClientScopes.add(clientScope.getId());
        }
    }

    public String getMasterAdminClient() {
        return masterAdminClient;
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

    public List<String> getDefaultRoles() {
        return defaultRoles;
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

    public List<RequiredCredentialModel> getRequiredCredentials() {
        return requiredCredentials;
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

    public Set<String> getAdminEnabledEventOperations() {
        return adminEnabledEventOperations;
    }

    public boolean isAdminEventsDetailsEnabled() {
        return adminEventsDetailsEnabled;
    }

    public List<IdentityProviderModel> getIdentityProviders() {
        return identityProviders;
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

    public MultivaluedHashMap<String, IdentityProviderMapperModel> getIdentityProviderMappers() {
        return identityProviderMappers;
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

    public List<String> getDefaultGroups() {
        return defaultGroups;
    }

    public List<String> getClientScopes() {
        return clientScopes;
    }

    public List<String> getDefaultDefaultClientScopes() {
        return defaultDefaultClientScopes;
    }

    public List<String> getOptionalDefaultClientScopes() {
        return optionalDefaultClientScopes;
    }

    public List<AuthenticationFlowModel> getAuthenticationFlowList() {
        return authenticationFlowList;
    }

    public List<RequiredActionProviderModel> getRequiredActionProviderList() {
        return requiredActionProviderList;
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
        return v != null ? Integer.parseInt(v) : defaultValue;
    }

    public Long getAttribute(String name, Long defaultValue) {
        String v = getAttribute(name);
        return v != null ? Long.parseLong(v) : defaultValue;
    }

    public Boolean getAttribute(String name, Boolean defaultValue) {
        String v = getAttribute(name);
        return v != null ? Boolean.parseBoolean(v) : defaultValue;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public boolean isAllowUserManagedAccess() {
        return allowUserManagedAccess;
    }
}
