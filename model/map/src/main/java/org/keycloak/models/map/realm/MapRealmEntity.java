/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.realm;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.keycloak.common.util.Time;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.EntityWithAttributes;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.realm.entity.MapAuthenticationExecutionEntity;
import org.keycloak.models.map.realm.entity.MapAuthenticationFlowEntity;
import org.keycloak.models.map.realm.entity.MapAuthenticatorConfigEntity;
import org.keycloak.models.map.realm.entity.MapClientInitialAccessEntity;
import org.keycloak.models.map.realm.entity.MapComponentEntity;
import org.keycloak.models.map.realm.entity.MapIdentityProviderEntity;
import org.keycloak.models.map.realm.entity.MapIdentityProviderMapperEntity;
import org.keycloak.models.map.realm.entity.MapOTPPolicyEntity;
import org.keycloak.models.map.realm.entity.MapRequiredActionProviderEntity;
import org.keycloak.models.map.realm.entity.MapRequiredCredentialEntity;
import org.keycloak.models.map.realm.entity.MapWebAuthnPolicyEntity;

public class MapRealmEntity extends UpdatableEntity.Impl implements AbstractEntity, EntityWithAttributes {

    private String id;
    private String name;

    private Boolean enabled = false;
    private Boolean registrationAllowed = false;
    private Boolean registrationEmailAsUsername = false;
    private Boolean verifyEmail = false;
    private Boolean resetPasswordAllowed = false;
    private Boolean loginWithEmailAllowed = false;
    private Boolean duplicateEmailsAllowed = false;
    private Boolean rememberMe = false;
    private Boolean editUsernameAllowed = false;
    private Boolean revokeRefreshToken = false;
    private Boolean adminEventsEnabled = false;
    private Boolean adminEventsDetailsEnabled = false;
    private Boolean internationalizationEnabled = false;
    private Boolean allowUserManagedAccess = false;
    private Boolean offlineSessionMaxLifespanEnabled = false;
    private Boolean eventsEnabled = false;
    private Integer refreshTokenMaxReuse = 0;
    private Integer ssoSessionIdleTimeout = 0;
    private Integer ssoSessionMaxLifespan = 0;
    private Integer ssoSessionIdleTimeoutRememberMe = 0;
    private Integer ssoSessionMaxLifespanRememberMe = 0;
    private Integer offlineSessionIdleTimeout = 0;
    private Integer accessTokenLifespan = 0;
    private Integer accessTokenLifespanForImplicitFlow = 0;
    private Integer accessCodeLifespan = 0;
    private Integer accessCodeLifespanUserAction = 0;
    private Integer accessCodeLifespanLogin = 0;
    private Integer notBefore = 0;
    private Integer clientSessionIdleTimeout = 0;
    private Integer clientSessionMaxLifespan = 0;
    private Integer clientOfflineSessionIdleTimeout = 0;
    private Integer clientOfflineSessionMaxLifespan = 0;
    private Integer actionTokenGeneratedByAdminLifespan = 0;
    private Integer offlineSessionMaxLifespan = 0;
    private Long eventsExpiration = 0l;
    private String displayName;
    private String displayNameHtml;
    private String passwordPolicy;
    private String sslRequired;
    private String loginTheme;
    private String accountTheme;
    private String adminTheme;
    private String emailTheme;
    private String masterAdminClient;
    private String defaultRoleId;
    private String defaultLocale;
    private String browserFlow;
    private String registrationFlow;
    private String directGrantFlow;
    private String resetCredentialsFlow;
    private String clientAuthenticationFlow;
    private String dockerAuthenticationFlow;
    private MapOTPPolicyEntity otpPolicy = MapOTPPolicyEntity.fromModel(OTPPolicy.DEFAULT_POLICY);;
    private MapWebAuthnPolicyEntity webAuthnPolicy = MapWebAuthnPolicyEntity.defaultWebAuthnPolicy();;
    private MapWebAuthnPolicyEntity webAuthnPolicyPasswordless = MapWebAuthnPolicyEntity.defaultWebAuthnPolicy();;

    private Set<String> eventsListeners = new HashSet<>();
    private Set<String> enabledEventTypes = new HashSet<>();
    private Set<String> supportedLocales = new HashSet<>();
    private Map<String, String> browserSecurityHeaders = new HashMap<>();
    private Map<String, String> smtpConfig = new HashMap<>();

    private final Set<String> defaultGroupIds = new HashSet<>();
    private final Set<String> defaultClientScopes = new HashSet<>();
    private final Set<String> optionalClientScopes = new HashSet<>();
    private final Map<String, List<String>> attributes = new HashMap<>();
    private final Map<String, Map<String, String>> localizationTexts = new HashMap<>();
    private final Map<String, MapClientInitialAccessEntity> clientInitialAccesses = new HashMap<>();
    private final Map<String, MapComponentEntity> components = new HashMap<>();
    private final Map<String, MapAuthenticationFlowEntity> authenticationFlows = new HashMap<>();
    private final Map<String, MapAuthenticationExecutionEntity> authenticationExecutions = new HashMap<>();
    private final Map<String, MapRequiredCredentialEntity> requiredCredentials = new HashMap<>();
    private final Map<String, MapAuthenticatorConfigEntity> authenticatorConfigs = new HashMap<>();
    private final Map<String, MapIdentityProviderEntity> identityProviders = new HashMap<>();
    private final Map<String, MapIdentityProviderMapperEntity> identityProviderMappers = new HashMap<>();
    private final Map<String, MapRequiredActionProviderEntity> requiredActionProviders = new HashMap<>();

    /**
     * Flag signalizing that any of the setters has been meaningfully used.
     */

    public MapRealmEntity() {}

    public MapRealmEntity(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String id) {
        if (this.id != null) throw new IllegalStateException("Id cannot be changed");
        this.id = id;
        this.updated |= id != null;
    }

    @Override
    public boolean isUpdated() {
        return this.updated
                || authenticationExecutions.values().stream().anyMatch(MapAuthenticationExecutionEntity::isUpdated)
                || authenticationFlows.values().stream().anyMatch(MapAuthenticationFlowEntity::isUpdated)
                || authenticatorConfigs.values().stream().anyMatch(MapAuthenticatorConfigEntity::isUpdated)
                || clientInitialAccesses.values().stream().anyMatch(MapClientInitialAccessEntity::isUpdated)
                || components.values().stream().anyMatch(MapComponentEntity::isUpdated)
                || identityProviders.values().stream().anyMatch(MapIdentityProviderEntity::isUpdated)
                || identityProviderMappers.values().stream().anyMatch(MapIdentityProviderMapperEntity::isUpdated)
                || requiredActionProviders.values().stream().anyMatch(MapRequiredActionProviderEntity::isUpdated)
                || requiredCredentials.values().stream().anyMatch(MapRequiredCredentialEntity::isUpdated)
                || otpPolicy.isUpdated()
                || webAuthnPolicy.isUpdated()
                || webAuthnPolicyPasswordless.isUpdated();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.updated |= ! Objects.equals(this.name, name);
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.updated |= ! Objects.equals(this.displayName, displayName);
        this.displayName = displayName;
    }

    public String getDisplayNameHtml() {
        return displayNameHtml;
    }

    public void setDisplayNameHtml(String displayNameHtml) {
        this.updated |= ! Objects.equals(this.displayNameHtml, displayNameHtml);
        this.displayNameHtml = displayNameHtml;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.updated |= ! Objects.equals(this.enabled, enabled);
        this.enabled = enabled;
    }

    public Boolean isRegistrationAllowed() {
        return registrationAllowed;
    }

    public void setRegistrationAllowed(Boolean registrationAllowed) {
        this.updated |= ! Objects.equals(this.registrationAllowed, registrationAllowed);
        this.registrationAllowed = registrationAllowed;
    }

    public Boolean isRegistrationEmailAsUsername() {
        return registrationEmailAsUsername;
    }

    public void setRegistrationEmailAsUsername(Boolean registrationEmailAsUsername) {
        this.updated |= ! Objects.equals(this.registrationEmailAsUsername, registrationEmailAsUsername);
        this.registrationEmailAsUsername = registrationEmailAsUsername;
    }

    public Boolean isVerifyEmail() {
        return verifyEmail;
    }

    public void setVerifyEmail(Boolean verifyEmail) {
        this.updated |= ! Objects.equals(this.verifyEmail, verifyEmail);
        this.verifyEmail = verifyEmail;
    }
    

    public Boolean isResetPasswordAllowed() {
        return resetPasswordAllowed;
    }

    public void setResetPasswordAllowed(Boolean resetPasswordAllowed) {
        this.updated |= ! Objects.equals(this.resetPasswordAllowed, resetPasswordAllowed);
        this.resetPasswordAllowed = resetPasswordAllowed;
    }

    public Boolean isLoginWithEmailAllowed() {
        return loginWithEmailAllowed;
    }

    public void setLoginWithEmailAllowed(Boolean loginWithEmailAllowed) {
        this.updated |= ! Objects.equals(this.loginWithEmailAllowed, loginWithEmailAllowed);
        this.loginWithEmailAllowed = loginWithEmailAllowed;
    }

    public Boolean isDuplicateEmailsAllowed() {
        return duplicateEmailsAllowed;
    }

    public void setDuplicateEmailsAllowed(Boolean duplicateEmailsAllowed) {
        this.updated |= ! Objects.equals(this.duplicateEmailsAllowed, duplicateEmailsAllowed);
        this.duplicateEmailsAllowed = duplicateEmailsAllowed;
    }

    public Boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(Boolean rememberMe) {
        this.updated |= ! Objects.equals(this.rememberMe, rememberMe);
        this.rememberMe = rememberMe;
    }

    public Boolean isEditUsernameAllowed() {
        return editUsernameAllowed;
    }

    public void setEditUsernameAllowed(Boolean editUsernameAllowed) {
        this.updated |= ! Objects.equals(this.editUsernameAllowed, editUsernameAllowed);
        this.editUsernameAllowed = editUsernameAllowed;
    }

    public Boolean isRevokeRefreshToken() {
        return revokeRefreshToken;
    }

    public void setRevokeRefreshToken(Boolean revokeRefreshToken) {
        this.updated |= ! Objects.equals(this.revokeRefreshToken, revokeRefreshToken);
        this.revokeRefreshToken = revokeRefreshToken;
    }

    public Boolean isAdminEventsEnabled() {
        return adminEventsEnabled;
    }

    public void setAdminEventsEnabled(Boolean adminEventsEnabled) {
        this.updated |= ! Objects.equals(this.adminEventsEnabled, adminEventsEnabled);
        this.adminEventsEnabled = adminEventsEnabled;
    }

    public Boolean isAdminEventsDetailsEnabled() {
        return adminEventsDetailsEnabled;
    }

    public void setAdminEventsDetailsEnabled(Boolean adminEventsDetailsEnabled) {
        this.updated |= ! Objects.equals(this.adminEventsDetailsEnabled, adminEventsDetailsEnabled);
        this.adminEventsDetailsEnabled = adminEventsDetailsEnabled;
    }

    public Boolean isInternationalizationEnabled() {
        return internationalizationEnabled;
    }

    public void setInternationalizationEnabled(Boolean internationalizationEnabled) {
        this.updated |= ! Objects.equals(this.internationalizationEnabled, internationalizationEnabled);
        this.internationalizationEnabled = internationalizationEnabled;
    }

    public Boolean isAllowUserManagedAccess() {
        return allowUserManagedAccess;
    }

    public void setAllowUserManagedAccess(Boolean allowUserManagedAccess) {
        this.updated |= ! Objects.equals(this.allowUserManagedAccess, allowUserManagedAccess);
        this.allowUserManagedAccess = allowUserManagedAccess;
    }

    public Boolean isOfflineSessionMaxLifespanEnabled() {
        return offlineSessionMaxLifespanEnabled;
    }

    public void setOfflineSessionMaxLifespanEnabled(Boolean offlineSessionMaxLifespanEnabled) {
        this.updated |= ! Objects.equals(this.offlineSessionMaxLifespanEnabled, offlineSessionMaxLifespanEnabled);
        this.offlineSessionMaxLifespanEnabled = offlineSessionMaxLifespanEnabled;
    }

    public Boolean isEventsEnabled() {
        return eventsEnabled;
    }

    public void setEventsEnabled(Boolean eventsEnabled) {
        this.updated |= ! Objects.equals(this.eventsEnabled, eventsEnabled);
        this.eventsEnabled = eventsEnabled;
    }

    public Integer getRefreshTokenMaxReuse() {
        return refreshTokenMaxReuse;
    }

    public void setRefreshTokenMaxReuse(Integer refreshTokenMaxReuse) {
        this.updated |= ! Objects.equals(this.refreshTokenMaxReuse, refreshTokenMaxReuse);
        this.refreshTokenMaxReuse = refreshTokenMaxReuse;
    }

    public Integer getSsoSessionIdleTimeout() {
        return ssoSessionIdleTimeout;
    }

    public void setSsoSessionIdleTimeout(Integer ssoSessionIdleTimeout) {
        this.updated |= ! Objects.equals(this.ssoSessionIdleTimeout, ssoSessionIdleTimeout);
        this.ssoSessionIdleTimeout = ssoSessionIdleTimeout;
    }

    public Integer getSsoSessionMaxLifespan() {
        return ssoSessionMaxLifespan;
    }

    public void setSsoSessionMaxLifespan(Integer ssoSessionMaxLifespan) {
        this.updated |= ! Objects.equals(this.ssoSessionMaxLifespan, ssoSessionMaxLifespan);
        this.ssoSessionMaxLifespan = ssoSessionMaxLifespan;
    }

    public Integer getSsoSessionIdleTimeoutRememberMe() {
        return ssoSessionIdleTimeoutRememberMe;
    }

    public void setSsoSessionIdleTimeoutRememberMe(Integer ssoSessionIdleTimeoutRememberMe) {
        this.updated |= ! Objects.equals(this.ssoSessionIdleTimeoutRememberMe, ssoSessionIdleTimeoutRememberMe);
        this.ssoSessionIdleTimeoutRememberMe = ssoSessionIdleTimeoutRememberMe;
    }

    public Integer getSsoSessionMaxLifespanRememberMe() {
        return ssoSessionMaxLifespanRememberMe;
    }

    public void setSsoSessionMaxLifespanRememberMe(Integer ssoSessionMaxLifespanRememberMe) {
        this.updated |= ! Objects.equals(this.ssoSessionMaxLifespanRememberMe, ssoSessionMaxLifespanRememberMe);
        this.ssoSessionMaxLifespanRememberMe = ssoSessionMaxLifespanRememberMe;
    }

    public Integer getOfflineSessionIdleTimeout() {
        return offlineSessionIdleTimeout;
    }

    public void setOfflineSessionIdleTimeout(Integer offlineSessionIdleTimeout) {
        this.updated |= ! Objects.equals(this.offlineSessionIdleTimeout, offlineSessionIdleTimeout);
        this.offlineSessionIdleTimeout = offlineSessionIdleTimeout;
    }

    public Integer getAccessTokenLifespan() {
        return accessTokenLifespan;
    }

    public void setAccessTokenLifespan(Integer accessTokenLifespan) {
        this.updated |= ! Objects.equals(this.accessTokenLifespan, accessTokenLifespan);
        this.accessTokenLifespan = accessTokenLifespan;
    }

    public Integer getAccessTokenLifespanForImplicitFlow() {
        return accessTokenLifespanForImplicitFlow;
    }

    public void setAccessTokenLifespanForImplicitFlow(Integer accessTokenLifespanForImplicitFlow) {
        this.updated |= ! Objects.equals(this.accessTokenLifespanForImplicitFlow, accessTokenLifespanForImplicitFlow);
        this.accessTokenLifespanForImplicitFlow = accessTokenLifespanForImplicitFlow;
    }

    public Integer getAccessCodeLifespan() {
        return accessCodeLifespan;
    }

    public void setAccessCodeLifespan(Integer accessCodeLifespan) {
        this.updated |= ! Objects.equals(this.accessCodeLifespan, accessCodeLifespan);
        this.accessCodeLifespan = accessCodeLifespan;
    }

    public Integer getAccessCodeLifespanUserAction() {
        return accessCodeLifespanUserAction;
    }

    public void setAccessCodeLifespanUserAction(Integer accessCodeLifespanUserAction) {
        this.updated |= ! Objects.equals(this.accessCodeLifespanUserAction, accessCodeLifespanUserAction);
        this.accessCodeLifespanUserAction = accessCodeLifespanUserAction;
    }

    public Integer getAccessCodeLifespanLogin() {
        return accessCodeLifespanLogin;
    }

    public void setAccessCodeLifespanLogin(Integer accessCodeLifespanLogin) {
        this.updated |= ! Objects.equals(this.accessCodeLifespanLogin, accessCodeLifespanLogin);
        this.accessCodeLifespanLogin = accessCodeLifespanLogin;
    }

    public Integer getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Integer notBefore) {
        this.updated |= ! Objects.equals(this.notBefore, notBefore);
        this.notBefore = notBefore;
    }

    public Integer getClientSessionIdleTimeout() {
        return clientSessionIdleTimeout;
    }

    public void setClientSessionIdleTimeout(Integer clientSessionIdleTimeout) {
        this.updated |= ! Objects.equals(this.clientSessionIdleTimeout, clientSessionIdleTimeout);
        this.clientSessionIdleTimeout = clientSessionIdleTimeout;
    }

    public Integer getClientSessionMaxLifespan() {
        return clientSessionMaxLifespan;
    }

    public void setClientSessionMaxLifespan(Integer clientSessionMaxLifespan) {
        this.updated |= ! Objects.equals(this.clientSessionMaxLifespan, clientSessionMaxLifespan);
        this.clientSessionMaxLifespan = clientSessionMaxLifespan;
    }

    public Integer getClientOfflineSessionIdleTimeout() {
        return clientOfflineSessionIdleTimeout;
    }

    public void setClientOfflineSessionIdleTimeout(Integer clientOfflineSessionIdleTimeout) {
        this.updated |= ! Objects.equals(this.clientOfflineSessionIdleTimeout, clientOfflineSessionIdleTimeout);
        this.clientOfflineSessionIdleTimeout = clientOfflineSessionIdleTimeout;
    }

    public Integer getClientOfflineSessionMaxLifespan() {
        return clientOfflineSessionMaxLifespan;
    }

    public void setClientOfflineSessionMaxLifespan(Integer clientOfflineSessionMaxLifespan) {
        this.updated |= ! Objects.equals(this.clientOfflineSessionMaxLifespan, clientOfflineSessionMaxLifespan);
        this.clientOfflineSessionMaxLifespan = clientOfflineSessionMaxLifespan;
    }

    public Integer getActionTokenGeneratedByAdminLifespan() {
        return actionTokenGeneratedByAdminLifespan;
    }

    public void setActionTokenGeneratedByAdminLifespan(Integer actionTokenGeneratedByAdminLifespan) {
        this.updated |= ! Objects.equals(this.actionTokenGeneratedByAdminLifespan, actionTokenGeneratedByAdminLifespan);
        this.actionTokenGeneratedByAdminLifespan = actionTokenGeneratedByAdminLifespan;
    }

    public Integer getOfflineSessionMaxLifespan() {
        return offlineSessionMaxLifespan;
    }

    public void setOfflineSessionMaxLifespan(Integer offlineSessionMaxLifespan) {
        this.updated |= ! Objects.equals(this.offlineSessionMaxLifespan, offlineSessionMaxLifespan);
        this.offlineSessionMaxLifespan = offlineSessionMaxLifespan;
    }

    public Long getEventsExpiration() {
        return eventsExpiration;
    }

    public void setEventsExpiration(Long eventsExpiration) {
        this.updated |= ! Objects.equals(this.eventsExpiration, eventsExpiration);
        this.eventsExpiration = eventsExpiration;
    }

    public String getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(String passwordPolicy) {
        this.updated |= ! Objects.equals(this.passwordPolicy, passwordPolicy);
        this.passwordPolicy = passwordPolicy;
    }

    public String getSslRequired() {
        return sslRequired;
    }

    public void setSslRequired(String sslRequired) {
        this.updated |= ! Objects.equals(this.sslRequired, sslRequired);
        this.sslRequired = sslRequired;
    }

    public String getLoginTheme() {
        return loginTheme;
    }

    public void setLoginTheme(String loginTheme) {
        this.updated |= ! Objects.equals(this.loginTheme, loginTheme);
        this.loginTheme = loginTheme;
    }

    public String getAccountTheme() {
        return accountTheme;
    }

    public void setAccountTheme(String accountTheme) {
        this.updated |= ! Objects.equals(this.accountTheme, accountTheme);
        this.accountTheme = accountTheme;
    }

    public String getAdminTheme() {
        return adminTheme;
    }

    public void setAdminTheme(String adminTheme) {
        this.updated |= ! Objects.equals(this.adminTheme, adminTheme);
        this.adminTheme = adminTheme;
    }

    public String getEmailTheme() {
        return emailTheme;
    }

    public void setEmailTheme(String emailTheme) {
        this.updated |= ! Objects.equals(this.emailTheme, emailTheme);
        this.emailTheme = emailTheme;
    }

    public String getMasterAdminClient() {
        return masterAdminClient;
    }

    public void setMasterAdminClient(String masterAdminClient) {
        this.updated |= ! Objects.equals(this.masterAdminClient, masterAdminClient);
        this.masterAdminClient = masterAdminClient;
    }

    public String getDefaultRoleId() {
        return defaultRoleId;
    }

    public void setDefaultRoleId(String defaultRoleId) {
        this.updated |= ! Objects.equals(this.defaultRoleId, defaultRoleId);
        this.defaultRoleId = defaultRoleId;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.updated |= ! Objects.equals(this.defaultLocale, defaultLocale);
        this.defaultLocale = defaultLocale;
    }

    public String getBrowserFlow() {
        return browserFlow;
    }

    public void setBrowserFlow(String browserFlow) {
        this.updated |= ! Objects.equals(this.browserFlow, browserFlow);
        this.browserFlow = browserFlow;
    }

    public String getRegistrationFlow() {
        return registrationFlow;
    }

    public void setRegistrationFlow(String registrationFlow) {
        this.updated |= ! Objects.equals(this.registrationFlow, registrationFlow);
        this.registrationFlow = registrationFlow;
    }

    public String getDirectGrantFlow() {
        return directGrantFlow;
    }

    public void setDirectGrantFlow(String directGrantFlow) {
        this.updated |= ! Objects.equals(this.directGrantFlow, directGrantFlow);
        this.directGrantFlow = directGrantFlow;
    }

    public String getResetCredentialsFlow() {
        return resetCredentialsFlow;
    }

    public void setResetCredentialsFlow(String resetCredentialsFlow) {
        this.updated |= ! Objects.equals(this.resetCredentialsFlow, resetCredentialsFlow);
        this.resetCredentialsFlow = resetCredentialsFlow;
    }

    public String getClientAuthenticationFlow() {
        return clientAuthenticationFlow;
    }

    public void setClientAuthenticationFlow(String clientAuthenticationFlow) {
        this.updated |= ! Objects.equals(this.clientAuthenticationFlow, clientAuthenticationFlow);
        this.clientAuthenticationFlow = clientAuthenticationFlow;
    }

    public String getDockerAuthenticationFlow() {
        return dockerAuthenticationFlow;
    }

    public void setDockerAuthenticationFlow(String dockerAuthenticationFlow) {
        this.updated |= ! Objects.equals(this.dockerAuthenticationFlow, dockerAuthenticationFlow);
        this.dockerAuthenticationFlow = dockerAuthenticationFlow;
    }

    public MapOTPPolicyEntity getOTPPolicy() {
        return otpPolicy;
    }

    public void setOTPPolicy(MapOTPPolicyEntity otpPolicy) {
        this.updated |= ! Objects.equals(this.otpPolicy, otpPolicy);
        this.otpPolicy = otpPolicy;
    }

    public MapWebAuthnPolicyEntity getWebAuthnPolicy() {
        return webAuthnPolicy;
    }

    public void setWebAuthnPolicy(MapWebAuthnPolicyEntity webAuthnPolicy) {
        this.updated |= ! Objects.equals(this.webAuthnPolicy, webAuthnPolicy);
        this.webAuthnPolicy = webAuthnPolicy;
    }

    public MapWebAuthnPolicyEntity getWebAuthnPolicyPasswordless() {
        return webAuthnPolicyPasswordless;
    }

    public void setWebAuthnPolicyPasswordless(MapWebAuthnPolicyEntity webAuthnPolicyPasswordless) {
        this.updated |= ! Objects.equals(this.webAuthnPolicyPasswordless, webAuthnPolicyPasswordless);
        this.webAuthnPolicyPasswordless = webAuthnPolicyPasswordless;
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        this.updated |= ! Objects.equals(this.attributes.put(name, values), values);
    }

    @Override
    public void removeAttribute(String name) {
        this.updated |= attributes.remove(name) != null;
    }

    @Override
    public List<String> getAttribute(String name) {
        return attributes.getOrDefault(name, Collections.EMPTY_LIST);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes.clear();
        this.attributes.putAll(attributes);
        this.updated = true;
    }

    public void addDefaultClientScope(String scopeId) {
        this.updated |= this.defaultClientScopes.add(scopeId);
    }

    public Stream<String> getDefaultClientScopeIds() {
        return defaultClientScopes.stream();
    }

    public void addOptionalClientScope(String scopeId) {
        this.updated |= this.optionalClientScopes.add(scopeId);
    }

    public Stream<String> getOptionalClientScopeIds() {
        return optionalClientScopes.stream();
    }

    public void removeDefaultOrOptionalClientScope(String scopeId) {
        if (this.defaultClientScopes.remove(scopeId)) {
            this.updated = true;
            return ;
        }
        this.updated |= this.optionalClientScopes.remove(scopeId);
    }

    public Stream<String> getDefaultGroupIds() {
        return defaultGroupIds.stream();
    }

    public void addDefaultGroup(String groupId) {
        this.updated |= this.defaultGroupIds.add(groupId);
    }

    public void removeDefaultGroup(String groupId) {
        this.updated |= this.defaultGroupIds.remove(groupId);
    }

    public Set<String> getEventsListeners() {
        return eventsListeners;
    }

    public void setEventsListeners(Set<String> eventsListeners) {
        if (eventsListeners == null) return;
        this.updated |= ! Objects.equals(eventsListeners, this.eventsListeners);
        this.eventsListeners = eventsListeners;
    }

    public Set<String> getEnabledEventTypes() {
        return enabledEventTypes;
    }

    public void setEnabledEventTypes(Set<String> enabledEventTypes) {
        if (enabledEventTypes == null) return;
        this.updated |= ! Objects.equals(enabledEventTypes, this.enabledEventTypes);
        this.enabledEventTypes = enabledEventTypes;
    }

    public Set<String> getSupportedLocales() {
        return supportedLocales;
    }

    public void setSupportedLocales(Set<String> supportedLocales) {
        if (supportedLocales == null) return;
        this.updated |= ! Objects.equals(supportedLocales, this.supportedLocales);
        this.supportedLocales = supportedLocales;
    }

    public Map<String, Map<String, String>> getLocalizationTexts() {
        return localizationTexts;
    }

    public Map<String, String> getLocalizationText(String locale) {
        if (localizationTexts.containsKey(locale)) {
            return localizationTexts.get(locale);
        }
        return Collections.emptyMap();
    }

    public void addLocalizationTexts(String locale, Map<String, String> texts) {
        if (! localizationTexts.containsKey(locale)) {
            updated = true;
            localizationTexts.put(locale, texts);
        }
    }

    public void updateLocalizationTexts(String locale, Map<String, String> texts) {
        this.updated |= localizationTexts.replace(locale, texts) != null;
    }

    public boolean removeLocalizationTexts(String locale) {
        boolean removed = localizationTexts.remove(locale) != null;
        updated |= removed;
        return removed;
    }

    public Map<String, String> getBrowserSecurityHeaders() {
        return browserSecurityHeaders;
    }

    public void setBrowserSecurityHeaders(Map<String, String> headers) {
        if (headers == null) return;
        this.updated |= ! Objects.equals(this.browserSecurityHeaders, headers);
        this.browserSecurityHeaders = headers;
    }

    public Map<String, String> getSmtpConfig() {
        return smtpConfig;
    }

    public void setSmtpConfig(Map<String, String> smtpConfig) {
        if (smtpConfig == null) return;
        this.updated |= ! Objects.equals(this.smtpConfig, smtpConfig);
        this.smtpConfig = smtpConfig;
    }

    public Stream<MapRequiredCredentialEntity> getRequiredCredentials() {
        return requiredCredentials.values().stream();
    }

    public void addRequiredCredential(MapRequiredCredentialEntity requiredCredential) {
        if (requiredCredentials.containsKey(requiredCredential.getType())) {
            throw new ModelDuplicateException("An RequiredCredential with given type already exists");
        }
        this.updated = true;
        requiredCredentials.put(requiredCredential.getType(), requiredCredential);
    }

    public void updateRequiredCredential(MapRequiredCredentialEntity requiredCredential) {
        this.updated |= requiredCredentials.replace(requiredCredential.getType(), requiredCredential) != null;
    }

    public Stream<MapComponentEntity> getComponents() {
        return components.values().stream();
    }

    public MapComponentEntity getComponent(String id) {
        return components.get(id);
    }

    public void addComponent(MapComponentEntity component) {
        if (components.containsKey(component.getId())) {
            throw new ModelDuplicateException("A Component with given id already exists");
        }
        this.updated = true;
        components.put(component.getId(), component);
    }

    public void updateComponent(MapComponentEntity component) {
        this.updated |= components.replace(component.getId(), component) != null;
    }

    public boolean removeComponent(String id) {
        boolean removed = this.components.remove(id) != null;
        this.updated |= removed;
        return removed;
    }

    public Stream<MapAuthenticationFlowEntity> getAuthenticationFlows() {
        return authenticationFlows.values().stream();
    }

    public MapAuthenticationFlowEntity getAuthenticationFlow(String flowId) {
        return authenticationFlows.get(flowId);
    }

    public void addAuthenticationFlow(MapAuthenticationFlowEntity authenticationFlow) {
        if (authenticationFlows.containsKey(authenticationFlow.getId())) {
            throw new ModelDuplicateException("An AuthenticationFlow with given id already exists");
        }
        this.updated = true;
        authenticationFlows.put(authenticationFlow.getId(), authenticationFlow);
    }

    public boolean removeAuthenticationFlow(String flowId) {
        boolean removed = this.authenticationFlows.remove(flowId) != null;
        updated |= removed;
        return removed;
    }

    public void updateAuthenticationFlow(MapAuthenticationFlowEntity authenticationFlow) {
        this.updated |= authenticationFlows.replace(authenticationFlow.getId(), authenticationFlow) != null;
    }

    public void addAuthenticatonExecution(MapAuthenticationExecutionEntity authenticationExecution) {
        if (authenticationExecutions.containsKey(authenticationExecution.getId())) {
            throw new ModelDuplicateException("An RequiredActionProvider with given id already exists");
        }

        this.updated = true;
        authenticationExecutions.put(authenticationExecution.getId(), authenticationExecution);
    }

    public void updateAuthenticatonExecution(MapAuthenticationExecutionEntity authenticationExecution) {
        this.updated |= authenticationExecutions.replace(authenticationExecution.getId(), authenticationExecution) != null;
    }

    public boolean removeAuthenticatonExecution(String id) {
        boolean removed = this.authenticationExecutions.remove(id) != null;
        updated |= removed;
        return removed;
    }

    public MapAuthenticationExecutionEntity getAuthenticationExecution(String id) {
        return authenticationExecutions.get(id);
    }

    public Stream<MapAuthenticationExecutionEntity> getAuthenticationExecutions() {
        return authenticationExecutions.values().stream();
    }

    public Stream<MapAuthenticatorConfigEntity> getAuthenticatorConfigs() {
        return authenticatorConfigs.values().stream();
    }

    public void addAuthenticatorConfig(MapAuthenticatorConfigEntity authenticatorConfig) {
        this.updated |= ! Objects.equals(authenticatorConfigs.put(authenticatorConfig.getId(), authenticatorConfig), authenticatorConfig);
    }

    public void updateAuthenticatorConfig(MapAuthenticatorConfigEntity authenticatorConfig) {
        this.updated |= authenticatorConfigs.replace(authenticatorConfig.getId(), authenticatorConfig) != null;
    }

    public boolean removeAuthenticatorConfig(String id) {
        boolean removed = this.authenticatorConfigs.remove(id) != null;
        updated |= removed;
        return removed;
    }

    public MapAuthenticatorConfigEntity getAuthenticatorConfig(String id) {
        return authenticatorConfigs.get(id);
    }

    public Stream<MapRequiredActionProviderEntity> getRequiredActionProviders() {
        return requiredActionProviders.values().stream();
    }

    public void addRequiredActionProvider(MapRequiredActionProviderEntity requiredActionProvider) {
        if (requiredActionProviders.containsKey(requiredActionProvider.getId())) {
            throw new ModelDuplicateException("An RequiredActionProvider with given id already exists");
        }

        this.updated = true;
        requiredActionProviders.put(requiredActionProvider.getId(), requiredActionProvider);
    }

    public void updateRequiredActionProvider(MapRequiredActionProviderEntity requiredActionProvider) {
        this.updated |= requiredActionProviders.replace(requiredActionProvider.getId(), requiredActionProvider) != null;
    }

    public boolean removeRequiredActionProvider(String id) {
        boolean removed = this.requiredActionProviders.remove(id) != null;
        updated |= removed;
        return removed;
    }

    public MapRequiredActionProviderEntity getRequiredActionProvider(String id) {
        return requiredActionProviders.get(id);
    }

    public Stream<MapIdentityProviderEntity> getIdentityProviders() {
        return identityProviders.values().stream();
    }

    public void addIdentityProvider(MapIdentityProviderEntity identityProvider) {
        if (identityProviders.containsKey(identityProvider.getId())) {
            throw new ModelDuplicateException("An IdentityProvider with given id already exists");
        }

        this.updated = true;
        identityProviders.put(identityProvider.getId(), identityProvider);
    }

    public boolean removeIdentityProvider(String id) {
        boolean removed = this.identityProviders.remove(id) != null;
        updated |= removed;
        return removed;
    }

    public void updateIdentityProvider(MapIdentityProviderEntity identityProvider) {
        this.updated |= identityProviders.replace(identityProvider.getId(), identityProvider) != null;
    }

    public Stream<MapIdentityProviderMapperEntity> getIdentityProviderMappers() {
        return identityProviderMappers.values().stream();
    }

    public void addIdentityProviderMapper(MapIdentityProviderMapperEntity identityProviderMapper) {
        if (identityProviderMappers.containsKey(identityProviderMapper.getId())) {
            throw new ModelDuplicateException("An IdentityProviderMapper with given id already exists");
        }

        this.updated = true;
        identityProviderMappers.put(identityProviderMapper.getId(), identityProviderMapper);
    }

    public boolean removeIdentityProviderMapper(String id) {
        boolean removed = this.identityProviderMappers.remove(id) != null;
        updated |= removed;
        return removed;
    }

    public void updateIdentityProviderMapper(MapIdentityProviderMapperEntity identityProviderMapper) {
        this.updated |= identityProviderMappers.replace(identityProviderMapper.getId(), identityProviderMapper) != null;
    }

    public MapIdentityProviderMapperEntity getIdentityProviderMapper(String id) {
        return identityProviderMappers.get(id);
    }

    public boolean hasClientInitialAccess() {
        return !clientInitialAccesses.isEmpty();
    }

    public void removeExpiredClientInitialAccesses() {
        clientInitialAccesses.values().stream()
            .filter(this::checkIfExpired)
            .map(MapClientInitialAccessEntity::getId)
            .collect(Collectors.toSet())
            .forEach(this::removeClientInitialAccess);
    }

    private boolean checkIfExpired(MapClientInitialAccessEntity cia) {
        return cia.getRemainingCount() < 1 || 
                (cia.getExpiration() > 0 && (cia.getTimestamp() + cia.getExpiration()) < Time.currentTime());
    }

    public void addClientInitialAccess(MapClientInitialAccessEntity clientInitialAccess) {
        this.updated = true;
        clientInitialAccesses.put(clientInitialAccess.getId(), clientInitialAccess);
    }

    public void updateClientInitialAccess(MapClientInitialAccessEntity clientInitialAccess) {
        this.updated |= clientInitialAccesses.replace(clientInitialAccess.getId(), clientInitialAccess) != null;
    }

    public MapClientInitialAccessEntity getClientInitialAccess(String id) {
        return clientInitialAccesses.get(id);
    }

    public boolean removeClientInitialAccess(String id) {
        boolean removed = this.clientInitialAccesses.remove(id) != null;
        updated |= removed;
        return removed;
    }

    public Collection<MapClientInitialAccessEntity> getClientInitialAccesses() {
        return clientInitialAccesses.values();
    }
}
