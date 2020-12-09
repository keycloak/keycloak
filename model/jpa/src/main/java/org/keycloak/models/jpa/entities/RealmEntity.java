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

package org.keycloak.models.jpa.entities;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKey;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Collection;
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
@Table(name="REALM")
@Entity
@NamedQueries({
        @NamedQuery(name="getAllRealmIds", query="select realm.id from RealmEntity realm"),
        @NamedQuery(name="getRealmIdByName", query="select realm.id from RealmEntity realm where realm.name = :name"),
        @NamedQuery(name="getRealmIdsWithProviderType", query="select distinct c.realm.id from ComponentEntity c where c.providerType = :providerType"),
})
public class RealmEntity {
    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name="NAME", unique = true)
    protected String name;

    @Column(name="ENABLED")
    protected boolean enabled;
    @Column(name="SSL_REQUIRED")
    protected String sslRequired;
    @Column(name="REGISTRATION_ALLOWED")
    protected boolean registrationAllowed;
    @Column(name = "REG_EMAIL_AS_USERNAME")
    protected boolean registrationEmailAsUsername;
    @Column(name="VERIFY_EMAIL")
    protected boolean verifyEmail;
    @Column(name="RESET_PASSWORD_ALLOWED")
    protected boolean resetPasswordAllowed;
    @Column(name="LOGIN_WITH_EMAIL_ALLOWED")
    protected boolean loginWithEmailAllowed;
    @Column(name="DUPLICATE_EMAILS_ALLOWED")
    protected boolean duplicateEmailsAllowed;
    @Column(name="REMEMBER_ME")
    protected boolean rememberMe;

    @Column(name="PASSWORD_POLICY")
    protected String passwordPolicy;

    @Column(name="OTP_POLICY_TYPE")
    protected String otpPolicyType;
    @Column(name="OTP_POLICY_ALG")
    protected String otpPolicyAlgorithm;
    @Column(name="OTP_POLICY_COUNTER")
    protected int otpPolicyInitialCounter;
    @Column(name="OTP_POLICY_DIGITS")
    protected int otpPolicyDigits;
    @Column(name="OTP_POLICY_WINDOW")
    protected int otpPolicyLookAheadWindow;
    @Column(name="OTP_POLICY_PERIOD")
    protected int otpPolicyPeriod;


    @Column(name="EDIT_USERNAME_ALLOWED")
    protected boolean editUsernameAllowed;

    @Column(name="REVOKE_REFRESH_TOKEN")
    private boolean revokeRefreshToken;
    @Column(name="REFRESH_TOKEN_MAX_REUSE")
    private int refreshTokenMaxReuse;
    @Column(name="SSO_IDLE_TIMEOUT")
    private int ssoSessionIdleTimeout;
    @Column(name="SSO_MAX_LIFESPAN")
    private int ssoSessionMaxLifespan;
    @Column(name="SSO_IDLE_TIMEOUT_REMEMBER_ME")
    private int ssoSessionIdleTimeoutRememberMe;
    @Column(name="SSO_MAX_LIFESPAN_REMEMBER_ME")
    private int ssoSessionMaxLifespanRememberMe;
    @Column(name="OFFLINE_SESSION_IDLE_TIMEOUT")
    private int offlineSessionIdleTimeout;
    @Column(name="ACCESS_TOKEN_LIFESPAN")
    protected int accessTokenLifespan;
    @Column(name="ACCESS_TOKEN_LIFE_IMPLICIT")
    protected int accessTokenLifespanForImplicitFlow;
    @Column(name="ACCESS_CODE_LIFESPAN")
    protected int accessCodeLifespan;
    @Column(name="USER_ACTION_LIFESPAN")
    protected int accessCodeLifespanUserAction;
    @Column(name="LOGIN_LIFESPAN")
    protected int accessCodeLifespanLogin;
    @Column(name="NOT_BEFORE")
    protected int notBefore;

    @Column(name="LOGIN_THEME")
    protected String loginTheme;
    @Column(name="ACCOUNT_THEME")
    protected String accountTheme;
    @Column(name="ADMIN_THEME")
    protected String adminTheme;
    @Column(name="EMAIL_THEME")
    protected String emailTheme;

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "realm", fetch = FetchType.EAGER)
    Collection<RealmAttributeEntity> attributes;

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "realm")
    Collection<RequiredCredentialEntity> requiredCredentials;

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "realm")
    List<UserFederationProviderEntity> userFederationProviders;

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "realm")
    Collection<UserFederationMapperEntity> userFederationMappers;

    @ElementCollection
    @MapKeyColumn(name="NAME")
    @Column(name="VALUE")
    @CollectionTable(name="REALM_SMTP_CONFIG", joinColumns={ @JoinColumn(name="REALM_ID") })
    protected Map<String, String> smtpConfig;

    @ElementCollection
    @Column(name="GROUP_ID")
    @CollectionTable(name="REALM_DEFAULT_GROUPS", joinColumns={ @JoinColumn(name="REALM_ID") })
    protected Set<String> defaultGroupIds;

    @Column(name="EVENTS_ENABLED")
    protected boolean eventsEnabled;
    @Column(name="EVENTS_EXPIRATION")
    protected long eventsExpiration;

    @ElementCollection
    @Column(name="VALUE")
    @CollectionTable(name="REALM_EVENTS_LISTENERS", joinColumns={ @JoinColumn(name="REALM_ID") })
    protected Set<String> eventsListeners;
    
    @ElementCollection
    @Column(name="VALUE")
    @CollectionTable(name="REALM_ENABLED_EVENT_TYPES", joinColumns={ @JoinColumn(name="REALM_ID") })
    protected Set<String> enabledEventTypes;
    
    @Column(name="ADMIN_EVENTS_ENABLED")
    protected boolean adminEventsEnabled;
    
    @Column(name="ADMIN_EVENTS_DETAILS_ENABLED")
    protected boolean adminEventsDetailsEnabled;

    @Column(name="MASTER_ADMIN_CLIENT")
    protected String masterAdminClient;

    @Column(name="DEFAULT_ROLE")
    protected String defaultRoleId;

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "realm")
    protected List<IdentityProviderEntity> identityProviders;

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "realm")
    Collection<IdentityProviderMapperEntity> identityProviderMappers;

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "realm")
    Collection<AuthenticatorConfigEntity> authenticators;

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "realm")
    Collection<RequiredActionProviderEntity> requiredActionProviders;

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "realm")
    Collection<AuthenticationFlowEntity> authenticationFlows;

    @OneToMany(fetch = FetchType.LAZY, cascade ={CascadeType.ALL}, orphanRemoval = true, mappedBy = "realm")
    Set<ComponentEntity> components;

    @Column(name="BROWSER_FLOW")
    protected String browserFlow;

    @Column(name="REGISTRATION_FLOW")
    protected String registrationFlow;


    @Column(name="DIRECT_GRANT_FLOW")
    protected String directGrantFlow;
    @Column(name="RESET_CREDENTIALS_FLOW")
    protected String resetCredentialsFlow;

    @Column(name="CLIENT_AUTH_FLOW")
    protected String clientAuthenticationFlow;

    @Column(name="DOCKER_AUTH_FLOW")
    protected String dockerAuthenticationFlow;


    @Column(name="INTERNATIONALIZATION_ENABLED")
    protected boolean internationalizationEnabled;

    @ElementCollection
    @Column(name="VALUE")
    @CollectionTable(name="REALM_SUPPORTED_LOCALES", joinColumns={ @JoinColumn(name="REALM_ID") })
    protected Set<String> supportedLocales;

    @Column(name="DEFAULT_LOCALE")
    protected String defaultLocale;

    @Column(name="ALLOW_USER_MANAGED_ACCESS")
    private boolean allowUserManagedAccess;

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "realmId")
    @MapKey(name="locale")
    Map<String, RealmLocalizationTextsEntity> realmLocalizationTexts;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
    
    public boolean isLoginWithEmailAllowed() {
        return loginWithEmailAllowed;
    }

    public void setLoginWithEmailAllowed(boolean loginWithEmailAllowed) {
        this.loginWithEmailAllowed = loginWithEmailAllowed;
    }
    
    public boolean isDuplicateEmailsAllowed() {
        return duplicateEmailsAllowed;
    }

    public void setDuplicateEmailsAllowed(boolean duplicateEmailsAllowed) {
        this.duplicateEmailsAllowed = duplicateEmailsAllowed;
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

    public boolean isRevokeRefreshToken() {
        return revokeRefreshToken;
    }

    public void setRevokeRefreshToken(boolean revokeRefreshToken) {
        this.revokeRefreshToken = revokeRefreshToken;
    }

    public int getRefreshTokenMaxReuse() {
        return refreshTokenMaxReuse;
    }

    public void setRefreshTokenMaxReuse(int revokeRefreshTokenCount) {
        this.refreshTokenMaxReuse = revokeRefreshTokenCount;
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

    public int getSsoSessionIdleTimeoutRememberMe() {
        return ssoSessionIdleTimeoutRememberMe;
    }

    public void setSsoSessionIdleTimeoutRememberMe(int ssoSessionIdleTimeoutRememberMe) {
        this.ssoSessionIdleTimeoutRememberMe = ssoSessionIdleTimeoutRememberMe;
    }

    public int getSsoSessionMaxLifespanRememberMe() {
        return ssoSessionMaxLifespanRememberMe;
    }

    public void setSsoSessionMaxLifespanRememberMe(int ssoSessionMaxLifespanRememberMe) {
        this.ssoSessionMaxLifespanRememberMe = ssoSessionMaxLifespanRememberMe;
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

    public Collection<RequiredCredentialEntity> getRequiredCredentials() {
        if (requiredCredentials == null) {
            requiredCredentials = new LinkedList<>();
        }
        return requiredCredentials;
    }

    public void setRequiredCredentials(Collection<RequiredCredentialEntity> requiredCredentials) {
        this.requiredCredentials = requiredCredentials;
    }
    public Map<String, String> getSmtpConfig() {
        if (smtpConfig == null) {
            smtpConfig = new HashMap<>();
        }
        return smtpConfig;
    }

    public void setSmtpConfig(Map<String, String> smtpConfig) {
        this.smtpConfig = smtpConfig;
    }

    public Set<String> getDefaultGroupIds() {
        if (defaultGroupIds == null) {
            defaultGroupIds = new HashSet<>();
        }
        return defaultGroupIds;
    }

    public void setDefaultGroupIds(Set<String> defaultGroups) {
        this.defaultGroupIds = defaultGroups;
    }

    public String getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(String passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public String getLoginTheme() {
        return loginTheme;
    }

    public void setLoginTheme(String theme) {
        this.loginTheme = theme;
    }

    public String getAccountTheme() {
        return accountTheme;
    }

    public void setAccountTheme(String theme) {
        this.accountTheme = theme;
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

    public int getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(int notBefore) {
        this.notBefore = notBefore;
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

    public Set<String> getEventsListeners() {
        if (eventsListeners == null) {
            eventsListeners = new HashSet<>();
        }
        return eventsListeners;
    }

    public void setEventsListeners(Set<String> eventsListeners) {
        this.eventsListeners = eventsListeners;
    }
    
    public Set<String> getEnabledEventTypes() {
        if (enabledEventTypes == null) {
            enabledEventTypes = new HashSet<>();
        }
        return enabledEventTypes;
    }

    public void setEnabledEventTypes(Set<String> enabledEventTypes) {
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

    public String getDefaultRoleId() {
        return defaultRoleId;
    }

    public void setDefaultRoleId(String defaultRoleId) {
        this.defaultRoleId = defaultRoleId;
    }

    public List<UserFederationProviderEntity> getUserFederationProviders() {
        if (userFederationProviders == null) {
            userFederationProviders = new LinkedList<>();
        }
        return userFederationProviders;
    }

    public void setUserFederationProviders(List<UserFederationProviderEntity> userFederationProviders) {
        this.userFederationProviders = userFederationProviders;
    }

    public Collection<UserFederationMapperEntity> getUserFederationMappers() {
        if (userFederationMappers == null) {
            userFederationMappers = new LinkedList<>();
        }
        return userFederationMappers;
    }

    public void setUserFederationMappers(Collection<UserFederationMapperEntity> userFederationMappers) {
        this.userFederationMappers = userFederationMappers;
    }

    public Collection<RealmAttributeEntity> getAttributes() {
        if (attributes == null) {
            attributes = new LinkedList<>();
        }
        return attributes;
    }

    public void setAttributes(Collection<RealmAttributeEntity> attributes) {
        this.attributes = attributes;
    }

    public List<IdentityProviderEntity> getIdentityProviders() {
        if (identityProviders == null) {
            identityProviders = new LinkedList<>();
        }
        return this.identityProviders;
    }

    public void setIdentityProviders(List<IdentityProviderEntity> identityProviders) {
        this.identityProviders = identityProviders;
    }

    public void addIdentityProvider(IdentityProviderEntity entity) {
        entity.setRealm(this);
        getIdentityProviders().add(entity);
    }

    public boolean isInternationalizationEnabled() {
        return internationalizationEnabled;
    }

    public void setInternationalizationEnabled(boolean internationalizationEnabled) {
        this.internationalizationEnabled = internationalizationEnabled;
    }

    public Set<String> getSupportedLocales() {
        if (supportedLocales == null) {
            supportedLocales = new HashSet<>();
        }
        return supportedLocales;
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

    public Collection<IdentityProviderMapperEntity> getIdentityProviderMappers() {
        if (identityProviderMappers == null) {
            identityProviderMappers = new LinkedList<>();
        }
        return identityProviderMappers;
    }

    public void setIdentityProviderMappers(Collection<IdentityProviderMapperEntity> identityProviderMappers) {
        this.identityProviderMappers = identityProviderMappers;
    }

    public Collection<AuthenticatorConfigEntity> getAuthenticatorConfigs() {
        if (authenticators == null) {
            authenticators = new LinkedList<>();
        }
        return authenticators;
    }
    
    public void setAuthenticatorConfigs(Collection<AuthenticatorConfigEntity> authenticators) {
        this.authenticators = authenticators;
    }

    public Collection<RequiredActionProviderEntity> getRequiredActionProviders() {
        if (requiredActionProviders == null) {
            requiredActionProviders = new LinkedList<>();
        }
        return requiredActionProviders;
    }

    public void setRequiredActionProviders(Collection<RequiredActionProviderEntity> requiredActionProviders) {
        this.requiredActionProviders = requiredActionProviders;
    }

    public Collection<AuthenticationFlowEntity> getAuthenticationFlows() {
        if (authenticationFlows == null) {
            authenticationFlows = new LinkedList<>();
        }
        return authenticationFlows;
    }

    public void setAuthenticationFlows(Collection<AuthenticationFlowEntity> authenticationFlows) {
        this.authenticationFlows = authenticationFlows;
    }

    public Set<ComponentEntity> getComponents() {
        if (components == null) {
            components = new HashSet<>();
        }
        return components;
    }

    public void setComponents(Set<ComponentEntity> components) {
        this.components = components;
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

    public String getDockerAuthenticationFlow() {
        return dockerAuthenticationFlow;
    }

    public RealmEntity setDockerAuthenticationFlow(String dockerAuthenticationFlow) {
        this.dockerAuthenticationFlow = dockerAuthenticationFlow;
        return this;
    }

    public void setAllowUserManagedAccess(boolean allowUserManagedAccess) {
        this.allowUserManagedAccess = allowUserManagedAccess;
    }

    public boolean isAllowUserManagedAccess() {
        return allowUserManagedAccess;
    }

    public Map<String, RealmLocalizationTextsEntity> getRealmLocalizationTexts() {
        if (realmLocalizationTexts == null) {
            realmLocalizationTexts = new HashMap<>();
        }
        return realmLocalizationTexts;
    }

    public void setRealmLocalizationTexts(Map<String, RealmLocalizationTextsEntity> realmLocalizationTexts) {
        this.realmLocalizationTexts = realmLocalizationTexts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof RealmEntity)) return false;

        RealmEntity that = (RealmEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

