/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.jpa.realm.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UuidValidator;
import org.keycloak.models.map.realm.MapRealmEntity;
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
import org.keycloak.models.map.storage.jpa.JpaRootVersionedEntity;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.JsonbType;

import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_REALM;
import static org.keycloak.models.map.storage.jpa.JpaMapStorageProviderFactory.CLONER;

/**
 * JPA {@link MapRealmEntity} implementation. Some fields are annotated with {@code @Column(insertable = false, updatable = false)}
 * to indicate that they are automatically generated from json fields. As such, these fields are non-insertable and non-updatable.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@Entity
@Table(name = "kc_realm",
    uniqueConstraints = {
        @UniqueConstraint(
                columnNames = {"name"}
        )
})
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonbType.class)})
@SuppressWarnings("ConstantConditions")
public class JpaRealmEntity extends MapRealmEntity.AbstractRealmEntity implements JpaRootVersionedEntity {

    @Id
    @Column
    private UUID id;

    //used for implicit optimistic locking
    @Version
    @Column
    private int version;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private final JpaRealmMetadata metadata;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private Integer entityVersion;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String name;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String displayName;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String displayNameHtml;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private Boolean enabled;

    @OneToMany(mappedBy = "root", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private final Set<JpaRealmAttributeEntity> attributes = new HashSet<>();

    @OneToMany(mappedBy = "root", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private final Set<JpaComponentEntity> components = new HashSet<>();

    /**
     * No-argument constructor, used by hibernate to instantiate entities.
     */
    public JpaRealmEntity() {
        this.metadata = new JpaRealmMetadata();
    }

    public JpaRealmEntity(final DeepCloner cloner) {
        this.metadata = new JpaRealmMetadata(cloner);
    }

    /**
     * Used by hibernate when calling cb.construct from read(QueryParameters) method.
     * It is used to select realm without metadata(json) field.
     */
    public JpaRealmEntity(final UUID id, final int version, final Integer entityVersion, final String name,
                          final String displayName, final String displayNameHtml, final Boolean enabled) {
        this.id = id;
        this.version = version;
        this.entityVersion = entityVersion;
        this.name = name;
        this.displayName = displayName;
        this.displayNameHtml = displayNameHtml;
        this.enabled = enabled;
        this.metadata = null;
    }

    public boolean isMetadataInitialized() {
        return metadata != null;
    }

    @Override
    public Integer getEntityVersion() {
        if (isMetadataInitialized()) return metadata.getEntityVersion();
        return entityVersion;
    }

    @Override
    public void setEntityVersion(Integer entityVersion) {
        metadata.setEntityVersion(entityVersion);
    }

    @Override
    public Integer getCurrentSchemaVersion() {
        return CURRENT_SCHEMA_VERSION_REALM;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public String getId() {
        return id == null ? null : id.toString();
    }

    @Override
    public void setId(String id) {
        String validatedId = UuidValidator.validateAndConvert(id);
        this.id = UUID.fromString(validatedId);
    }

    @Override
    public String getName() {
        if (isMetadataInitialized()) return this.metadata.getName();
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.metadata.setName(name);
    }

    @Override
    public String getDisplayName() {
        if (isMetadataInitialized()) return this.metadata.getDisplayName();
        return this.displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.metadata.setDisplayName(displayName);
    }

    @Override
    public String getDisplayNameHtml() {
        if (isMetadataInitialized()) return this.metadata.getDisplayNameHtml();
        return this.displayNameHtml;
    }

    @Override
    public void setDisplayNameHtml(String displayNameHtml) {
        this.metadata.setDisplayNameHtml(displayNameHtml);
    }

    @Override
    public Boolean isEnabled() {
        if (isMetadataInitialized()) return this.metadata.isEnabled();
        return this.enabled;
    }

    @Override
    public void setEnabled(Boolean enabled) {
        this.metadata.setEnabled(enabled);
    }

    @Override
    public Boolean isRegistrationAllowed() {
        return this.metadata.isRegistrationAllowed();
    }

    @Override
    public void setRegistrationAllowed(Boolean registrationAllowed) {
        this.metadata.setRegistrationAllowed(registrationAllowed);
    }

    @Override
    public Boolean isRegistrationEmailAsUsername() {
        return this.metadata.isRegistrationEmailAsUsername();
    }

    @Override
    public void setRegistrationEmailAsUsername(Boolean registrationEmailAsUsername) {
        this.metadata.setRegistrationEmailAsUsername(registrationEmailAsUsername);
    }

    @Override
    public Boolean isVerifyEmail() {
        return this.metadata.isVerifyEmail();
    }

    @Override
    public void setVerifyEmail(Boolean verifyEmail) {
        this.metadata.setVerifyEmail(verifyEmail);
    }

    @Override
    public Boolean isResetPasswordAllowed() {
        return this.metadata.isResetPasswordAllowed();
    }

    @Override
    public void setResetPasswordAllowed(Boolean resetPasswordAllowed) {
        this.metadata.setResetPasswordAllowed(resetPasswordAllowed);
    }

    @Override
    public Boolean isLoginWithEmailAllowed() {
        return this.metadata.isLoginWithEmailAllowed();
    }

    @Override
    public void setLoginWithEmailAllowed(Boolean loginWithEmailAllowed) {
        this.metadata.setLoginWithEmailAllowed(loginWithEmailAllowed);
    }

    @Override
    public Boolean isDuplicateEmailsAllowed() {
        return this.metadata.isDuplicateEmailsAllowed();
    }

    @Override
    public void setDuplicateEmailsAllowed(Boolean duplicateEmailsAllowed) {
        this.metadata.setDuplicateEmailsAllowed(duplicateEmailsAllowed);
    }

    @Override
    public Boolean isRememberMe() {
        return this.metadata.isRememberMe();
    }

    @Override
    public void setRememberMe(Boolean rememberMe) {
        this.metadata.setRememberMe(rememberMe);
    }

    @Override
    public Boolean isEditUsernameAllowed() {
        return this.metadata.isEditUsernameAllowed();
    }

    @Override
    public void setEditUsernameAllowed(Boolean editUsernameAllowed) {
        this.metadata.setEditUsernameAllowed(editUsernameAllowed);
    }

    @Override
    public Boolean isRevokeRefreshToken() {
        return this.metadata.isRevokeRefreshToken();
    }

    @Override
    public void setRevokeRefreshToken(Boolean revokeRefreshToken) {
        this.metadata.setRevokeRefreshToken(revokeRefreshToken);
    }

    @Override
    public Boolean isAdminEventsEnabled() {
        return this.metadata.isAdminEventsEnabled();
    }

    @Override
    public void setAdminEventsEnabled(Boolean adminEventsEnabled) {
        this.metadata.setAdminEventsEnabled(adminEventsEnabled);
    }

    @Override
    public Boolean isAdminEventsDetailsEnabled() {
        return this.metadata.isAdminEventsDetailsEnabled();
    }

    @Override
    public void setAdminEventsDetailsEnabled(Boolean adminEventsDetailsEnabled) {
        this.metadata.setAdminEventsDetailsEnabled(adminEventsDetailsEnabled);
    }

    @Override
    public Boolean isInternationalizationEnabled() {
        return this.metadata.isInternationalizationEnabled();
    }

    @Override
    public void setInternationalizationEnabled(Boolean internationalizationEnabled) {
        this.metadata.setInternationalizationEnabled(internationalizationEnabled);
    }

    @Override
    public Boolean isAllowUserManagedAccess() {
        return this.metadata.isAllowUserManagedAccess();
    }

    @Override
    public void setAllowUserManagedAccess(Boolean allowUserManagedAccess) {
        this.metadata.setAllowUserManagedAccess(allowUserManagedAccess);
    }

    @Override
    public Boolean isOfflineSessionMaxLifespanEnabled() {
        return this.metadata.isOfflineSessionMaxLifespanEnabled();
    }

    @Override
    public void setOfflineSessionMaxLifespanEnabled(Boolean offlineSessionMaxLifespanEnabled) {
        this.metadata.setOfflineSessionMaxLifespanEnabled(offlineSessionMaxLifespanEnabled);
    }

    @Override
    public Boolean isEventsEnabled() {
        return this.metadata.isEventsEnabled();
    }

    @Override
    public void setEventsEnabled(Boolean eventsEnabled) {
        this.metadata.setEventsEnabled(eventsEnabled);
    }

    @Override
    public Integer getRefreshTokenMaxReuse() {
        return this.metadata.getRefreshTokenMaxReuse();
    }

    @Override
    public void setRefreshTokenMaxReuse(Integer refreshTokenMaxReuse) {
        this.metadata.setRefreshTokenMaxReuse(refreshTokenMaxReuse);
    }

    @Override
    public Integer getSsoSessionIdleTimeout() {
        return this.metadata.getSsoSessionIdleTimeout();
    }

    @Override
    public void setSsoSessionIdleTimeout(Integer ssoSessionIdleTimeout) {
        this.metadata.setSsoSessionIdleTimeout(ssoSessionIdleTimeout);
    }

    @Override
    public Integer getSsoSessionMaxLifespan() {
        return this.metadata.getSsoSessionMaxLifespan();
    }

    @Override
    public void setSsoSessionMaxLifespan(Integer ssoSessionMaxLifespan) {
        this.metadata.setSsoSessionMaxLifespan(ssoSessionMaxLifespan);
    }

    @Override
    public Integer getSsoSessionIdleTimeoutRememberMe() {
        return this.metadata.getSsoSessionIdleTimeoutRememberMe();
    }

    @Override
    public void setSsoSessionIdleTimeoutRememberMe(Integer ssoSessionIdleTimeoutRememberMe) {
        this.metadata.setSsoSessionIdleTimeoutRememberMe(ssoSessionIdleTimeoutRememberMe);
    }

    @Override
    public Integer getSsoSessionMaxLifespanRememberMe() {
        return this.metadata.getSsoSessionMaxLifespanRememberMe();
    }

    @Override
    public void setSsoSessionMaxLifespanRememberMe(Integer ssoSessionMaxLifespanRememberMe) {
        this.metadata.setSsoSessionMaxLifespanRememberMe(ssoSessionMaxLifespanRememberMe);
    }

    @Override
    public Integer getOfflineSessionIdleTimeout() {
        return this.metadata.getOfflineSessionIdleTimeout();
    }

    @Override
    public void setOfflineSessionIdleTimeout(Integer offlineSessionIdleTimeout) {
        this.metadata.setOfflineSessionIdleTimeout(offlineSessionIdleTimeout);
    }

    @Override
    public Integer getAccessTokenLifespan() {
        return this.metadata.getAccessTokenLifespan();
    }

    @Override
    public void setAccessTokenLifespan(Integer accessTokenLifespan) {
        this.metadata.setAccessTokenLifespan(accessTokenLifespan);
    }

    @Override
    public Integer getAccessTokenLifespanForImplicitFlow() {
        return this.metadata.getAccessTokenLifespanForImplicitFlow();
    }

    @Override
    public void setAccessTokenLifespanForImplicitFlow(Integer accessTokenLifespanForImplicitFlow) {
        this.metadata.setAccessTokenLifespanForImplicitFlow(accessTokenLifespanForImplicitFlow);
    }

    @Override
    public Integer getAccessCodeLifespan() {
        return this.metadata.getAccessCodeLifespan();
    }

    @Override
    public void setAccessCodeLifespan(Integer accessCodeLifespan) {
        this.metadata.setAccessCodeLifespan(accessCodeLifespan);
    }

    @Override
    public Integer getAccessCodeLifespanUserAction() {
        return this.metadata.getAccessCodeLifespanUserAction();
    }

    @Override
    public void setAccessCodeLifespanUserAction(Integer accessCodeLifespanUserAction) {
        this.metadata.setAccessCodeLifespanUserAction(accessCodeLifespanUserAction);
    }

    @Override
    public Integer getAccessCodeLifespanLogin() {
        return this.metadata.getAccessCodeLifespanLogin();
    }

    @Override
    public void setAccessCodeLifespanLogin(Integer accessCodeLifespanLogin) {
        this.metadata.setAccessCodeLifespanLogin(accessCodeLifespanLogin);
    }

    @Override
    public Long getNotBefore() {
        return this.metadata.getNotBefore();
    }

    @Override
    public void setNotBefore(Long notBefore) {
        this.metadata.setNotBefore(notBefore);
    }

    @Override
    public Integer getClientSessionIdleTimeout() {
        return this.metadata.getClientSessionIdleTimeout();
    }

    @Override
    public void setClientSessionIdleTimeout(Integer clientSessionIdleTimeout) {
        this.metadata.setClientSessionIdleTimeout(clientSessionIdleTimeout);
    }

    @Override
    public Integer getClientSessionMaxLifespan() {
        return this.metadata.getClientSessionMaxLifespan();
    }

    @Override
    public void setClientSessionMaxLifespan(Integer clientSessionMaxLifespan) {
        this.metadata.setClientSessionMaxLifespan(clientSessionMaxLifespan);
    }

    @Override
    public Integer getClientOfflineSessionIdleTimeout() {
        return this.metadata.getClientOfflineSessionIdleTimeout();
    }

    @Override
    public void setClientOfflineSessionIdleTimeout(Integer clientOfflineSessionIdleTimeout) {
        this.metadata.setClientOfflineSessionIdleTimeout(clientOfflineSessionIdleTimeout);
    }

    @Override
    public Integer getClientOfflineSessionMaxLifespan() {
        return this.metadata.getClientOfflineSessionMaxLifespan();
    }

    @Override
    public void setClientOfflineSessionMaxLifespan(Integer clientOfflineSessionMaxLifespan) {
        this.metadata.setClientOfflineSessionMaxLifespan(clientOfflineSessionMaxLifespan);
    }

    @Override
    public Integer getActionTokenGeneratedByAdminLifespan() {
        return this.metadata.getActionTokenGeneratedByAdminLifespan();
    }

    @Override
    public void setActionTokenGeneratedByAdminLifespan(Integer actionTokenGeneratedByAdminLifespan) {
        this.metadata.setActionTokenGeneratedByAdminLifespan(actionTokenGeneratedByAdminLifespan);
    }

    @Override
    public Integer getOfflineSessionMaxLifespan() {
        return this.metadata.getOfflineSessionMaxLifespan();
    }

    @Override
    public void setOfflineSessionMaxLifespan(Integer offlineSessionMaxLifespan) {
        this.metadata.setOfflineSessionMaxLifespan(offlineSessionMaxLifespan);
    }

    @Override
    public Long getEventsExpiration() {
        return this.metadata.getEventsExpiration();
    }

    @Override
    public void setEventsExpiration(Long eventsExpiration) {
        this.metadata.setEventsExpiration(eventsExpiration);
    }

    @Override
    public String getPasswordPolicy() {
        return this.metadata.getPasswordPolicy();
    }

    @Override
    public void setPasswordPolicy(String passwordPolicy) {
        this.metadata.setPasswordPolicy(passwordPolicy);
    }

    @Override
    public String getSslRequired() {
        return this.metadata.getSslRequired();
    }

    @Override
    public void setSslRequired(String sslRequired) {
        this.metadata.setSslRequired(sslRequired);
    }

    @Override
    public String getLoginTheme() {
        return this.metadata.getLoginTheme();
    }

    @Override
    public void setLoginTheme(String loginTheme) {
        this.metadata.setLoginTheme(loginTheme);
    }

    @Override
    public String getAccountTheme() {
        return this.metadata.getAccountTheme();
    }

    @Override
    public void setAccountTheme(String accountTheme) {
        this.metadata.setAccountTheme(accountTheme);
    }

    @Override
    public String getAdminTheme() {
        return this.metadata.getAdminTheme();
    }

    @Override
    public void setAdminTheme(String adminTheme) {
        this.metadata.setAdminTheme(adminTheme);
    }

    @Override
    public String getEmailTheme() {
        return this.metadata.getEmailTheme();
    }

    @Override
    public void setEmailTheme(String emailTheme) {
        this.metadata.setEmailTheme(emailTheme);
    }

    @Override
    public String getMasterAdminClient() {
        return this.metadata.getMasterAdminClient();
    }

    @Override
    public void setMasterAdminClient(String masterAdminClient) {
        this.metadata.setMasterAdminClient(masterAdminClient);
    }

    @Override
    public String getDefaultRoleId() {
        return this.metadata.getDefaultRoleId();
    }

    @Override
    public void setDefaultRoleId(String defaultRoleId) {
        this.metadata.setDefaultRoleId(defaultRoleId);
    }

    @Override
    public String getDefaultLocale() {
        return this.metadata.getDefaultLocale();
    }

    @Override
    public void setDefaultLocale(String defaultLocale) {
        this.metadata.setDefaultLocale(defaultLocale);
    }

    @Override
    public String getBrowserFlow() {
        return this.metadata.getBrowserFlow();
    }

    @Override
    public void setBrowserFlow(String browserFlow) {
        this.metadata.setBrowserFlow(browserFlow);
    }

    @Override
    public String getRegistrationFlow() {
        return this.metadata.getRegistrationFlow();
    }

    @Override
    public void setRegistrationFlow(String registrationFlow) {
        this.metadata.setRegistrationFlow(registrationFlow);
    }

    @Override
    public String getDirectGrantFlow() {
        return this.metadata.getDirectGrantFlow();
    }

    @Override
    public void setDirectGrantFlow(String directGrantFlow) {
        this.metadata.setDirectGrantFlow(directGrantFlow);
    }

    @Override
    public String getResetCredentialsFlow() {
        return this.metadata.getResetCredentialsFlow();
    }

    @Override
    public void setResetCredentialsFlow(String resetCredentialsFlow) {
        this.metadata.setResetCredentialsFlow(resetCredentialsFlow);
    }

    @Override
    public String getClientAuthenticationFlow() {
        return this.metadata.getClientAuthenticationFlow();
    }

    @Override
    public void setClientAuthenticationFlow(String clientAuthenticationFlow) {
        this.metadata.setClientAuthenticationFlow(clientAuthenticationFlow);
    }

    @Override
    public String getDockerAuthenticationFlow() {
        return this.metadata.getDockerAuthenticationFlow();
    }

    @Override
    public void setDockerAuthenticationFlow(String dockerAuthenticationFlow) {
        this.metadata.setDockerAuthenticationFlow(dockerAuthenticationFlow);
    }

    @Override
    public MapOTPPolicyEntity getOTPPolicy() {
        return this.metadata.getOTPPolicy();
    }

    @Override
    public void setOTPPolicy(MapOTPPolicyEntity otpPolicy) {
        this.metadata.setOTPPolicy(otpPolicy);
    }

    @Override
    public MapWebAuthnPolicyEntity getWebAuthnPolicy() {
        return metadata.getWebAuthnPolicy();
    }

    @Override
    public void setWebAuthnPolicy(MapWebAuthnPolicyEntity webAuthnPolicy) {
        this.metadata.setWebAuthnPolicy(webAuthnPolicy);
    }

    @Override
    public MapWebAuthnPolicyEntity getWebAuthnPolicyPasswordless() {
        return this.metadata.getWebAuthnPolicyPasswordless();
    }

    @Override
    public void setWebAuthnPolicyPasswordless(MapWebAuthnPolicyEntity webAuthnPolicyPasswordless) {
        this.metadata.setWebAuthnPolicyPasswordless(webAuthnPolicyPasswordless);
    }

    @Override
    public Set<String> getDefaultClientScopeIds() {
        return this.metadata.getDefaultClientScopeIds();
    }

    @Override
    public void addDefaultClientScopeId(String scopeId) {
        this.metadata.addDefaultClientScopeId(scopeId);
    }

    @Override
    public Boolean removeDefaultClientScopeId(String scopeId) {
        return this.metadata.removeDefaultClientScopeId(scopeId);
    }

    @Override
    public Set<String> getOptionalClientScopeIds() {
        return this.metadata.getOptionalClientScopeIds();
    }

    @Override
    public void addOptionalClientScopeId(String scopeId) {
        this.metadata.addOptionalClientScopeId(scopeId);
    }

    @Override
    public Boolean removeOptionalClientScopeId(String scopeId) {
        return this.metadata.removeOptionalClientScopeId(scopeId);
    }

    @Override
    public Set<String> getDefaultGroupIds() {
        return this.metadata.getDefaultGroupIds();
    }

    @Override
    public void addDefaultGroupId(String groupId) {
        this.metadata.addDefaultGroupId(groupId);
    }

    @Override
    public void removeDefaultGroupId(String groupId) {
        this.metadata.removeDefaultGroupId(groupId);
    }

    @Override
    public Set<String> getEventsListeners() {
        return this.metadata.getEventsListeners();
    }

    @Override
    public void setEventsListeners(Set<String> eventsListeners) {
        this.metadata.setEventsListeners(eventsListeners);
    }

    @Override
    public Set<String> getEnabledEventTypes() {
        return this.metadata.getEnabledEventTypes();
    }

    @Override
    public void setEnabledEventTypes(Set<String> enabledEventTypes) {
        this.metadata.setEnabledEventTypes(enabledEventTypes);
    }

    @Override
    public Set<String> getSupportedLocales() {
        return this.metadata.getSupportedLocales();
    }

    @Override
    public void setSupportedLocales(Set<String> supportedLocales) {
        this.metadata.setSupportedLocales(supportedLocales);
    }

    @Override
    public Map<String, Map<String, String>> getLocalizationTexts() {
        return this.metadata.getLocalizationTexts();
    }

    @Override
    public Map<String, String> getLocalizationText(String locale) {
        return this.metadata.getLocalizationText(locale);
    }

    @Override
    public void setLocalizationText(String locale, Map<String, String> texts) {
        this.metadata.setLocalizationText(locale, texts);
    }

    @Override
    public Boolean removeLocalizationText(String locale) {
        return this.metadata.removeLocalizationText(locale);
    }

    @Override
    public Map<String, String> getBrowserSecurityHeaders() {
        return this.metadata.getBrowserSecurityHeaders();
    }

    @Override
    public void setBrowserSecurityHeaders(Map<String, String> headers) {
        this.metadata.setBrowserSecurityHeaders(headers);
    }

    @Override
    public void setBrowserSecurityHeader(String name, String value) {
        this.metadata.setBrowserSecurityHeader(name, value);
    }

    @Override
    public Map<String, String> getSmtpConfig() {
        return this.metadata.getSmtpConfig();
    }

    @Override
    public void setSmtpConfig(Map<String, String> smtpConfig) {
        this.metadata.setSmtpConfig(smtpConfig);
    }

    @Override
    public Set<MapRequiredCredentialEntity> getRequiredCredentials() {
        return this.metadata.getRequiredCredentials();
    }

    @Override
    public void addRequiredCredential(MapRequiredCredentialEntity requiredCredential) {
        this.metadata.addRequiredCredential(requiredCredential);
    }

    @Override
    public Set<MapComponentEntity> getComponents() {
        return this.components.stream().map(MapComponentEntity.class::cast).collect(Collectors.toSet());
    }

    @Override
    public Optional<MapComponentEntity> getComponent(String componentId) {
        return this.components.stream().filter(c -> Objects.equals(c.getId(), componentId)).findFirst().map(MapComponentEntity.class::cast);
    }

    @Override
    public void addComponent(MapComponentEntity component) {
        JpaComponentEntity jpaComponent = JpaComponentEntity.class.cast(CLONER.from(component));
        jpaComponent.setParent(this);
        jpaComponent.setEntityVersion(this.getEntityVersion());
        this.components.add(jpaComponent);
    }

    @Override
    public Boolean removeComponent(String componentId) {
        return this.components.removeIf(c -> Objects.equals(c.getId(), componentId));
    }

    @Override
    public Set<MapAuthenticationFlowEntity> getAuthenticationFlows() {
        return this.metadata.getAuthenticationFlows();
    }

    @Override
    public void addAuthenticationFlow(MapAuthenticationFlowEntity authenticationFlow) {
        this.metadata.addAuthenticationFlow(authenticationFlow);
    }

    @Override
    public Set<MapAuthenticationExecutionEntity> getAuthenticationExecutions() {
        return this.metadata.getAuthenticationExecutions();
    }

    @Override
    public void addAuthenticationExecution(MapAuthenticationExecutionEntity authenticationExecution) {
        this.metadata.addAuthenticationExecution(authenticationExecution);
    }

    @Override
    public Set<MapAuthenticatorConfigEntity> getAuthenticatorConfigs() {
        return this.metadata.getAuthenticatorConfigs();
    }

    @Override
    public void addAuthenticatorConfig(MapAuthenticatorConfigEntity authenticatorConfig) {
        this.metadata.addAuthenticatorConfig(authenticatorConfig);
    }

    @Override
    public Set<MapRequiredActionProviderEntity> getRequiredActionProviders() {
        return this.metadata.getRequiredActionProviders();
    }

    @Override
    public void addRequiredActionProvider(MapRequiredActionProviderEntity requiredActionProvider) {
        this.metadata.addRequiredActionProvider(requiredActionProvider);
    }

    @Override
    public Set<MapIdentityProviderEntity> getIdentityProviders() {
        return this.metadata.getIdentityProviders();
    }

    @Override
    public void addIdentityProvider(MapIdentityProviderEntity identityProvider) {
        this.metadata.addIdentityProvider(identityProvider);
    }

    @Override
    public void addIdentityProviderMapper(MapIdentityProviderMapperEntity identityProviderMapper) {
        this.metadata.addIdentityProviderMapper(identityProviderMapper);
    }

    @Override
    public Set<MapIdentityProviderMapperEntity> getIdentityProviderMappers() {
        return this.metadata.getIdentityProviderMappers();
    }

    @Override
    public Set<MapClientInitialAccessEntity> getClientInitialAccesses() {
        return this.metadata.getClientInitialAccesses();
    }

    @Override
    public void addClientInitialAccess(MapClientInitialAccessEntity clientInitialAccess) {
        this.metadata.addClientInitialAccess(clientInitialAccess);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> result = new HashMap<>();
        for (JpaRealmAttributeEntity attribute : this.attributes) {
            List<String> values = result.getOrDefault(attribute.getName(), new LinkedList<>());
            values.add(attribute.getValue());
            result.put(attribute.getName(), values);
        }
        return result;
    }

    @Override
    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes.clear();
        if (attributes != null) {
            for (Map.Entry<String, List<String>> attrEntry : attributes.entrySet()) {
                this.setAttribute(attrEntry.getKey(), attrEntry.getValue());
            }
        }
    }

    @Override
    public List<String> getAttribute(String name) {
        return this.attributes.stream()
                .filter(a -> Objects.equals(a.getName(), name))
                .map(JpaRealmAttributeEntity::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        this.removeAttribute(name);
        for (String value : values) {
            JpaRealmAttributeEntity attribute = new JpaRealmAttributeEntity(this, name, value);
            this.attributes.add(attribute);
        }
    }

    @Override
    public void removeAttribute(String name) {
        this.attributes.removeIf(attr -> Objects.equals(attr.getName(), name));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JpaRealmEntity)) return false;
        return Objects.equals(getId(), ((JpaRealmEntity) obj).getId());
    }
}
