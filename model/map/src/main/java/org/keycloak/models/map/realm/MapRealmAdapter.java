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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static java.util.Objects.nonNull;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.keycloak.Config;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.ClientInitialAccessModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuth2DeviceConfig;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.ParConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.WebAuthnPolicy;
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
import org.keycloak.models.utils.ComponentUtil;

public abstract class MapRealmAdapter<K> extends AbstractRealmModel<MapRealmEntity<K>> implements RealmModel {

    private static final String ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN = "actionTokenGeneratedByUserLifespan";
    private static final String DEFAULT_SIGNATURE_ALGORITHM = "defaultSignatureAlgorithm";
    private static final String BRUTE_FORCE_PROTECTED = "bruteForceProtected";
    private static final String PERMANENT_LOCKOUT = "permanentLockout";
    private static final String MAX_FAILURE_WAIT_SECONDS = "maxFailureWaitSeconds";
    private static final String WAIT_INCREMENT_SECONDS = "waitIncrementSeconds";
    private static final String QUICK_LOGIN_CHECK_MILLISECONDS = "quickLoginCheckMilliSeconds";
    private static final String MINIMUM_QUICK_LOGIN_WAIT_SECONDS = "minimumQuickLoginWaitSeconds";
    private static final String MAX_DELTA_SECONDS = "maxDeltaTimeSeconds";
    private static final String FAILURE_FACTOR = "failureFactor";

    private PasswordPolicy passwordPolicy;

    public MapRealmAdapter(KeycloakSession session, MapRealmEntity<K> entity) {
        super(session, entity);
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public void setName(String name) {
        entity.setName(name);
    }

    @Override
    public String getDisplayName() {
        return entity.getDisplayName();
    }

    @Override
    public void setDisplayName(String displayName) {
        entity.setDisplayName(displayName);
    }

    @Override
    public String getDisplayNameHtml() {
        return entity.getDisplayNameHtml();
    }

    @Override
    public void setDisplayNameHtml(String displayNameHtml) {
        entity.setDisplayNameHtml(displayNameHtml);
    }

    @Override
    public boolean isEnabled() {
        return entity.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        entity.setEnabled(enabled);
    }

    @Override
    public SslRequired getSslRequired() {
        return entity.getSslRequired() == null ? null : SslRequired.valueOf(entity.getSslRequired());
    }

    @Override
    public void setSslRequired(SslRequired sslRequired) {
        entity.setSslRequired(sslRequired.name());
    }

    @Override
    public boolean isRegistrationAllowed() {
        return entity.isRegistrationAllowed();
    }

    @Override
    public void setRegistrationAllowed(boolean registrationAllowed) {
        entity.setRegistrationAllowed(registrationAllowed);
    }

    @Override
    public boolean isRegistrationEmailAsUsername() {
        return entity.isRegistrationEmailAsUsername();
    }

    @Override
    public void setRegistrationEmailAsUsername(boolean registrationEmailAsUsername) {
        entity.setRegistrationEmailAsUsername(registrationEmailAsUsername);
    }

    @Override
    public boolean isRememberMe() {
        return entity.isRememberMe();
    }

    @Override
    public void setRememberMe(boolean rememberMe) {
        entity.setRememberMe(rememberMe);
    }

    @Override
    public boolean isEditUsernameAllowed() {
        return entity.isEditUsernameAllowed();
    }

    @Override
    public void setEditUsernameAllowed(boolean editUsernameAllowed) {
        entity.setEditUsernameAllowed(editUsernameAllowed);
    }

    @Override
    public boolean isUserManagedAccessAllowed() {
        return entity.isAllowUserManagedAccess();
    }

    @Override
    public void setUserManagedAccessAllowed(boolean userManagedAccessAllowed) {
        entity.setAllowUserManagedAccess(userManagedAccessAllowed);
    }

    @Override
    public void setAttribute(String name, String value) {
        entity.setAttribute(name, Collections.singletonList(value));
    }

    @Override
    public void removeAttribute(String name) {
        entity.removeAttribute(name);
    }

    @Override
    public String getAttribute(String name) {
        List<String> attribute = entity.getAttribute(name);
        if (attribute.isEmpty()) return null;
        return attribute.get(0);
    }

    @Override
    public Map<String, String> getAttributes() {
        return entity.getAttributes().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, 
            entry -> {
                if (entry.getValue().isEmpty()) return null;
                return entry.getValue().get(0);
            })
        );
    }

    @Override
    public boolean isVerifyEmail() {
        return entity.isVerifyEmail();
    }

    @Override
    public void setVerifyEmail(boolean verifyEmail) {
        entity.setVerifyEmail(verifyEmail);
    }

    @Override
    public boolean isLoginWithEmailAllowed() {
        return entity.isLoginWithEmailAllowed();
    }

    @Override
    public void setLoginWithEmailAllowed(boolean loginWithEmailAllowed) {
        entity.setLoginWithEmailAllowed(loginWithEmailAllowed);
    }

    @Override
    public boolean isDuplicateEmailsAllowed() {
        return entity.isDuplicateEmailsAllowed();
    }

    @Override
    public void setDuplicateEmailsAllowed(boolean duplicateEmailsAllowed) {
        entity.setDuplicateEmailsAllowed(duplicateEmailsAllowed);
    }

    @Override
    public boolean isResetPasswordAllowed() {
        return entity.isResetPasswordAllowed();
    }

    @Override
    public void setResetPasswordAllowed(boolean resetPasswordAllowed) {
        entity.setResetPasswordAllowed(resetPasswordAllowed);
    }

    @Override
    public boolean isRevokeRefreshToken() {
        return entity.isRevokeRefreshToken();
    }

    @Override
    public void setRevokeRefreshToken(boolean revokeRefreshToken) {
        entity.setRevokeRefreshToken(revokeRefreshToken);
    }

    @Override
    public int getRefreshTokenMaxReuse() {
        return entity.getRefreshTokenMaxReuse();
    }

    @Override
    public void setRefreshTokenMaxReuse(int revokeRefreshTokenCount) {
        entity.setRefreshTokenMaxReuse(revokeRefreshTokenCount);
    }

    @Override
    public int getSsoSessionIdleTimeout() {
        return entity.getSsoSessionIdleTimeout();
    }

    @Override
    public void setSsoSessionIdleTimeout(int seconds) {
        entity.setSsoSessionIdleTimeout(seconds);
    }

    @Override
    public int getSsoSessionMaxLifespan() {
        return entity.getSsoSessionMaxLifespan();
    }

    @Override
    public void setSsoSessionMaxLifespan(int seconds) {
        entity.setSsoSessionMaxLifespan(seconds);
    }

    @Override
    public int getSsoSessionIdleTimeoutRememberMe() {
        return entity.getSsoSessionIdleTimeoutRememberMe();
    }

    @Override
    public void setSsoSessionIdleTimeoutRememberMe(int seconds) {
        entity.setSsoSessionIdleTimeoutRememberMe(seconds);
    }

    @Override
    public int getSsoSessionMaxLifespanRememberMe() {
        return entity.getSsoSessionMaxLifespanRememberMe();
    }

    @Override
    public void setSsoSessionMaxLifespanRememberMe(int seconds) {
        entity.setSsoSessionMaxLifespanRememberMe(seconds);
    }

    @Override
    public int getOfflineSessionIdleTimeout() {
        return entity.getOfflineSessionIdleTimeout();
    }

    @Override
    public void setOfflineSessionIdleTimeout(int seconds) {
        entity.setOfflineSessionIdleTimeout(seconds);
    }

    @Override
    public int getAccessTokenLifespan() {
        return entity.getAccessTokenLifespan();
    }

    @Override
    public int getClientSessionIdleTimeout() {
        return entity.getClientSessionIdleTimeout();
    }

    @Override
    public void setClientSessionIdleTimeout(int seconds) {
        entity.setClientSessionIdleTimeout(seconds);
    }

    @Override
    public int getClientSessionMaxLifespan() {
        return entity.getClientSessionMaxLifespan();
    }

    @Override
    public void setClientSessionMaxLifespan(int seconds) {
        entity.setClientSessionMaxLifespan(seconds);
    }

    @Override
    public int getClientOfflineSessionIdleTimeout() {
        return entity.getClientOfflineSessionIdleTimeout();
    }

    @Override
    public void setClientOfflineSessionIdleTimeout(int seconds) {
        entity.setClientOfflineSessionIdleTimeout(seconds);
    }

    @Override
    public int getClientOfflineSessionMaxLifespan() {
        return entity.getClientOfflineSessionMaxLifespan();
    }

    @Override
    public void setClientOfflineSessionMaxLifespan(int seconds) {
        entity.setClientOfflineSessionMaxLifespan(seconds);
    }

    @Override
    public void setAccessTokenLifespan(int seconds) {
        entity.setAccessTokenLifespan(seconds);
    }

    @Override
    public int getAccessTokenLifespanForImplicitFlow() {
        return entity.getAccessTokenLifespanForImplicitFlow();
    }

    @Override
    public void setAccessTokenLifespanForImplicitFlow(int seconds) {
        entity.setAccessTokenLifespanForImplicitFlow(seconds);
    }

    @Override
    public int getAccessCodeLifespan() {
        return entity.getAccessCodeLifespan();
    }

    @Override
    public void setAccessCodeLifespan(int seconds) {
        entity.setAccessCodeLifespan(seconds);
    }

    @Override
    public int getAccessCodeLifespanUserAction() {
        return entity.getAccessCodeLifespanUserAction();
    }

    @Override
    public void setAccessCodeLifespanUserAction(int seconds) {
        entity.setAccessCodeLifespanUserAction(seconds);
    }

    @Override
    public int getAccessCodeLifespanLogin() {
        return entity.getAccessCodeLifespanLogin();
    }

    @Override
    public void setAccessCodeLifespanLogin(int seconds) {
        entity.setAccessCodeLifespanLogin(seconds);
    }

    @Override
    public int getActionTokenGeneratedByAdminLifespan() {
        return entity.getActionTokenGeneratedByAdminLifespan();
    }

    @Override
    public void setActionTokenGeneratedByAdminLifespan(int seconds) {
        entity.setActionTokenGeneratedByAdminLifespan(seconds);
    }

    @Override
    public int getActionTokenGeneratedByUserLifespan() {
        return getAttribute(ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN, getAccessCodeLifespanUserAction());
    }

    @Override
    public void setActionTokenGeneratedByUserLifespan(int seconds) {
        setAttribute(ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN, seconds);
    }

    @Override
    public int getActionTokenGeneratedByUserLifespan(String actionTokenType) {
        return getAttribute(ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + "." + actionTokenType, getAccessCodeLifespanUserAction());
    }

    @Override
    public void setActionTokenGeneratedByUserLifespan(String actionTokenType, Integer seconds) {
        if (actionTokenType != null && ! actionTokenType.isEmpty() && seconds != null) {
            setAttribute(ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + "." + actionTokenType, seconds);
        }
    }

    @Override
    public Map<String, Integer> getUserActionTokenLifespans() {
        Map<String, Integer> tokenLifespans = entity.getAttributes().entrySet().stream()
                .filter(Objects::nonNull)
                .filter(entry -> nonNull(entry.getValue()) && ! entry.getValue().isEmpty())
                .filter(entry -> entry.getKey().startsWith(ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + "."))
                .collect(Collectors.toMap(
                        entry -> entry.getKey().substring(ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN.length() + 1),
                        entry -> Integer.valueOf(entry.getValue().get(0))));

        return Collections.unmodifiableMap(tokenLifespans);
    }

    @Override
    public Stream<RequiredCredentialModel> getRequiredCredentialsStream() {
        return entity.getRequiredCredentials().map(MapRequiredCredentialEntity::toModel);
    }

    @Override
    public void addRequiredCredential(String cred) {
        RequiredCredentialModel model = RequiredCredentialModel.BUILT_IN.get(cred);
        if (model == null) {
            throw new RuntimeException("Unknown credential type " + cred);
        }
        entity.addRequiredCredential(MapRequiredCredentialEntity.fromModel(model));
    }

    @Override
    public void updateRequiredCredentials(Set<String> credentials) {
        credentials.stream()
                .map(RequiredCredentialModel.BUILT_IN::get)
                .peek(c -> { if (c == null) throw new RuntimeException("Unknown credential type " + c.getType()); })
                .map(MapRequiredCredentialEntity::fromModel)
                .forEach(this::updateRequiredCredential);
    }

    private void updateRequiredCredential(MapRequiredCredentialEntity requiredCredential) {
        entity.updateRequiredCredential(requiredCredential);
    }

    @Override
    public PasswordPolicy getPasswordPolicy() {
        if (passwordPolicy == null) {
            passwordPolicy = PasswordPolicy.parse(session, entity.getPasswordPolicy());
        }
        return passwordPolicy;
    }

    @Override
    public void setPasswordPolicy(PasswordPolicy policy) {
        this.passwordPolicy = policy;
        entity.setPasswordPolicy(policy.toString());
    }

    @Override
    public OTPPolicy getOTPPolicy() {
        return MapOTPPolicyEntity.toModel(entity.getOTPPolicy());
    }

    @Override
    public void setOTPPolicy(OTPPolicy policy) {
        entity.setOTPPolicy(MapOTPPolicyEntity.fromModel(policy));
    }

    @Override
    public RoleModel getRoleById(String id) {
        return session.roles().getRoleById(this, id);
    }

    @Override
    public Stream<GroupModel> getDefaultGroupsStream() {
        return entity.getDefaultGroupIds().map(this::getGroupById);
    }

    @Override
    public void addDefaultGroup(GroupModel group) {
        entity.addDefaultGroup(group.getId());
    }

    @Override
    public void removeDefaultGroup(GroupModel group) {
        entity.removeDefaultGroup(group.getId());
    }

    @Override
    public Stream<ClientModel> getClientsStream() {
        return session.clients().getClientsStream(this);
    }

    @Override
    public Stream<ClientModel> getClientsStream(Integer firstResult, Integer maxResults) {
        return session.clients().getClientsStream(this, firstResult, maxResults);
    }

    @Override
    public Long getClientsCount() {
        return session.clients().getClientsCount(this);
    }

    @Override
    public Stream<ClientModel> getAlwaysDisplayInConsoleClientsStream() {
        return session.clients().getAlwaysDisplayInConsoleClientsStream(this);
    }

    @Override
    public ClientModel addClient(String name) {
        return session.clients().addClient(this, name);
    }

    @Override
    public ClientModel addClient(String id, String clientId) {
        return session.clients().addClient(this, id, clientId);
    }

    @Override
    public boolean removeClient(String id) {
        return session.clients().removeClient(this, id);
    }

    @Override
    public ClientModel getClientById(String id) {
        return session.clients().getClientById(this, id);
    }

    @Override
    public ClientModel getClientByClientId(String clientId) {
        return session.clients().getClientByClientId(this, clientId);
    }

    @Override
    public Stream<ClientModel> searchClientByClientIdStream(String clientId, Integer firstResult, Integer maxResults) {
        return session.clients().searchClientsByClientIdStream(this, clientId, firstResult, maxResults);
    }

    @Override
    public Stream<ClientModel> searchClientByAttributes(Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        return session.clients().searchClientsByAttributes(this, attributes, firstResult, maxResults);
    }

    @Override
    public Map<String, String> getSmtpConfig() {
        return Collections.unmodifiableMap(entity.getSmtpConfig());
    }

    @Override
    public void setSmtpConfig(Map<String, String> smtpConfig) {
        entity.setSmtpConfig(smtpConfig);
    }

    @Override
    public AuthenticationFlowModel getBrowserFlow() {
        return getAuthenticationFlowById(entity.getBrowserFlow());
    }

    @Override
    public void setBrowserFlow(AuthenticationFlowModel flow) {
        entity.setBrowserFlow(flow.getId());
    }

    @Override
    public AuthenticationFlowModel getRegistrationFlow() {
        return getAuthenticationFlowById(entity.getRegistrationFlow());
    }

    @Override
    public void setRegistrationFlow(AuthenticationFlowModel flow) {
        entity.setRegistrationFlow(flow.getId());
    }

    @Override
    public AuthenticationFlowModel getDirectGrantFlow() {
        return getAuthenticationFlowById(entity.getDirectGrantFlow());
    }

    @Override
    public void setDirectGrantFlow(AuthenticationFlowModel flow) {
        entity.setDirectGrantFlow(flow.getId());
    }

    @Override
    public AuthenticationFlowModel getResetCredentialsFlow() {
        return getAuthenticationFlowById(entity.getResetCredentialsFlow());
    }

    @Override
    public void setResetCredentialsFlow(AuthenticationFlowModel flow) {
        entity.setResetCredentialsFlow(flow.getId());
    }

    @Override
    public AuthenticationFlowModel getClientAuthenticationFlow() {
        return getAuthenticationFlowById(entity.getClientAuthenticationFlow());
    }

    @Override
    public void setClientAuthenticationFlow(AuthenticationFlowModel flow) {
        entity.setClientAuthenticationFlow(flow.getId());
    }

    @Override
    public AuthenticationFlowModel getDockerAuthenticationFlow() {
        return getAuthenticationFlowById(entity.getDockerAuthenticationFlow());
    }

    @Override
    public void setDockerAuthenticationFlow(AuthenticationFlowModel flow) {
        entity.setDockerAuthenticationFlow(flow.getId());
    }

    @Override
    public Stream<AuthenticationFlowModel> getAuthenticationFlowsStream() {
        return entity.getAuthenticationFlows().map(MapAuthenticationFlowEntity::toModel);
    }

    @Override
    public AuthenticationFlowModel getFlowByAlias(String alias) {
        return entity.getAuthenticationFlows()
                .filter(flow -> Objects.equals(flow.getAlias(), alias))
                .findFirst()
                .map(MapAuthenticationFlowEntity::toModel)
                .orElse(null);
    }

    @Override
    public AuthenticationFlowModel addAuthenticationFlow(AuthenticationFlowModel model) {
        MapAuthenticationFlowEntity authenticationFlowEntity = MapAuthenticationFlowEntity.fromModel(model);
        entity.addAuthenticationFlow(authenticationFlowEntity);
        model.setId(authenticationFlowEntity.getId());
        return model;
    }

    @Override
    public AuthenticationFlowModel getAuthenticationFlowById(String flowId) {
        if (flowId == null) return null;
        return MapAuthenticationFlowEntity.toModel(entity.getAuthenticationFlow(flowId));
    }

    @Override
    public void removeAuthenticationFlow(AuthenticationFlowModel model) {
        entity.removeAuthenticationFlow(model.getId());
    }

    @Override
    public void updateAuthenticationFlow(AuthenticationFlowModel model) {
        entity.updateAuthenticationFlow(MapAuthenticationFlowEntity.fromModel(model));
    }

    @Override
    public Stream<AuthenticationExecutionModel> getAuthenticationExecutionsStream(String flowId) {
        return entity.getAuthenticationExecutions()
                .filter(execution -> Objects.equals(flowId, execution.getParentFlowId()))
                .map(MapAuthenticationExecutionEntity::toModel)
                .sorted(AuthenticationExecutionModel.ExecutionComparator.SINGLETON);
    }

    @Override
    public AuthenticationExecutionModel getAuthenticationExecutionById(String id) {
        if (id == null) return null;
        return MapAuthenticationExecutionEntity.toModel(entity.getAuthenticationExecution(id));
    }

    @Override
    public AuthenticationExecutionModel getAuthenticationExecutionByFlowId(String flowId) {
        return entity.getAuthenticationExecutions()
                .filter(execution -> Objects.equals(flowId, execution.getFlowId()))
                .findAny()
                .map(MapAuthenticationExecutionEntity::toModel)
                .orElse(null);
    }

    @Override
    public AuthenticationExecutionModel addAuthenticatorExecution(AuthenticationExecutionModel model) {
        MapAuthenticationExecutionEntity executionEntity = MapAuthenticationExecutionEntity.fromModel(model);
        entity.addAuthenticatonExecution(executionEntity);
        model.setId(executionEntity.getId());
        return model;
    }

    @Override
    public void updateAuthenticatorExecution(AuthenticationExecutionModel model) {
        entity.updateAuthenticatonExecution(MapAuthenticationExecutionEntity.fromModel(model));
    }

    @Override
    public void removeAuthenticatorExecution(AuthenticationExecutionModel model) {
        entity.removeAuthenticatonExecution(model.getId());
    }

    @Override
    public Stream<AuthenticatorConfigModel> getAuthenticatorConfigsStream() {
        return entity.getAuthenticatorConfigs().map(MapAuthenticatorConfigEntity::toModel);
    }

    @Override
    public AuthenticatorConfigModel addAuthenticatorConfig(AuthenticatorConfigModel model) {
        MapAuthenticatorConfigEntity authenticatorConfig = MapAuthenticatorConfigEntity.fromModel(model);
        entity.addAuthenticatorConfig(authenticatorConfig);
        model.setId(authenticatorConfig.getId());
        return model;
    }

    @Override
    public void updateAuthenticatorConfig(AuthenticatorConfigModel model) {
        entity.updateAuthenticatorConfig(MapAuthenticatorConfigEntity.fromModel(model));
    }

    @Override
    public void removeAuthenticatorConfig(AuthenticatorConfigModel model) {
        entity.removeAuthenticatorConfig(model.getId());
    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfigById(String id) {
        if (id == null) return null;
        return MapAuthenticatorConfigEntity.toModel(entity.getAuthenticatorConfig(id));
    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfigByAlias(String alias) {
        return entity.getAuthenticatorConfigs()
                .filter(config -> Objects.equals(config.getAlias(), alias))
                .findFirst()
                .map(MapAuthenticatorConfigEntity::toModel)
                .orElse(null);
    }

    @Override
    public Stream<RequiredActionProviderModel> getRequiredActionProvidersStream() {
        return entity.getRequiredActionProviders()
                .map(MapRequiredActionProviderEntity::toModel)
                .sorted(RequiredActionProviderModel.RequiredActionComparator.SINGLETON);
    }

    @Override
    public RequiredActionProviderModel addRequiredActionProvider(RequiredActionProviderModel model) {
        MapRequiredActionProviderEntity requiredActionProvider = MapRequiredActionProviderEntity.fromModel(model);
        entity.addRequiredActionProvider(requiredActionProvider);
        model.setId(requiredActionProvider.getId());
        return model;
    }

    @Override
    public void updateRequiredActionProvider(RequiredActionProviderModel model) {
        entity.updateRequiredActionProvider(MapRequiredActionProviderEntity.fromModel(model));
    }

    @Override
    public void removeRequiredActionProvider(RequiredActionProviderModel model) {
        entity.removeRequiredActionProvider(model.getId());
    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderById(String id) {
        if (id == null) return null;
        return MapRequiredActionProviderEntity.toModel(entity.getRequiredActionProvider(id));
    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderByAlias(String alias) {
        return entity.getRequiredActionProviders()
                .filter(actionProvider -> Objects.equals(actionProvider.getAlias(), alias))
                .findFirst()
                .map(MapRequiredActionProviderEntity::toModel)
                .orElse(null);
    }

    @Override
    public Stream<IdentityProviderModel> getIdentityProvidersStream() {
        return entity.getIdentityProviders().map(MapIdentityProviderEntity::toModel);
    }

    @Override
    public IdentityProviderModel getIdentityProviderByAlias(String alias) {
        return entity.getIdentityProviders()
                .filter(identityProvider -> Objects.equals(identityProvider.getAlias(), alias))
                .findFirst()
                .map(MapIdentityProviderEntity::toModel)
                .orElse(null);
    }

    @Override
    public void addIdentityProvider(IdentityProviderModel model) {
        entity.addIdentityProvider(MapIdentityProviderEntity.fromModel(model));
    }

    @Override
    public void removeIdentityProviderByAlias(String alias) {
        IdentityProviderModel model = getIdentityProviderByAlias(alias);
        entity.removeIdentityProvider(model.getInternalId());

        // TODO: Sending an event should be extracted to store layer
        session.getKeycloakSessionFactory().publish(new RealmModel.IdentityProviderRemovedEvent() {

            @Override
            public RealmModel getRealm() {
                return MapRealmAdapter.this;
            }

            @Override
            public IdentityProviderModel getRemovedIdentityProvider() {
                return model;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });
        // TODO: ^^^^^^^ Up to here
    }

    @Override
    public void updateIdentityProvider(IdentityProviderModel identityProvider) {
        entity.updateIdentityProvider(MapIdentityProviderEntity.fromModel(identityProvider));

        // TODO: Sending an event should be extracted to store layer
        session.getKeycloakSessionFactory().publish(new RealmModel.IdentityProviderUpdatedEvent() {

            @Override
            public RealmModel getRealm() {
                return MapRealmAdapter.this;
            }

            @Override
            public IdentityProviderModel getUpdatedIdentityProvider() {
                return identityProvider;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });
        // TODO: ^^^^^^^ Up to here
    }

    @Override
    public Stream<IdentityProviderMapperModel> getIdentityProviderMappersStream() {
        return entity.getIdentityProviderMappers().map(MapIdentityProviderMapperEntity::toModel);
    }

    @Override
    public Stream<IdentityProviderMapperModel> getIdentityProviderMappersByAliasStream(String brokerAlias) {
        return entity.getIdentityProviderMappers()
                .filter(mapper -> Objects.equals(mapper.getIdentityProviderAlias(), brokerAlias))
                .map(MapIdentityProviderMapperEntity::toModel);
    }

    @Override
    public IdentityProviderMapperModel addIdentityProviderMapper(IdentityProviderMapperModel model) {
        MapIdentityProviderMapperEntity identityProviderMapper = MapIdentityProviderMapperEntity.fromModel(model);
        entity.addIdentityProviderMapper(identityProviderMapper);
        model.setId(identityProviderMapper.getId());
        return model;
    }

    @Override
    public void removeIdentityProviderMapper(IdentityProviderMapperModel model) {
        entity.removeIdentityProviderMapper(model.getId());
    }

    @Override
    public void updateIdentityProviderMapper(IdentityProviderMapperModel model) {
        entity.updateIdentityProviderMapper(MapIdentityProviderMapperEntity.fromModel(model));
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperById(String id) {
        if (id == null) return null;
        return MapIdentityProviderMapperEntity.toModel(entity.getIdentityProviderMapper(id));
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperByName(String brokerAlias, String name) {
        return entity.getIdentityProviderMappers()
                .filter(identityProviderMapper -> Objects.equals(identityProviderMapper.getIdentityProviderAlias(), brokerAlias) 
                        && Objects.equals(identityProviderMapper.getName(), name))
                .findFirst()
                .map(MapIdentityProviderMapperEntity::toModel)
                .orElse(null);
    }

    @Override
    public ComponentModel addComponentModel(ComponentModel model) {
        model = importComponentModel(model);
        ComponentUtil.notifyCreated(session, this, model);
        return model;
    }

    /**
     * Copied from jpa RealmAdapter: This just exists for testing purposes
     */
    private static final String COMPONENT_PROVIDER_EXISTS_DISABLED = "component.provider.exists.disabled";

    @Override
    public ComponentModel importComponentModel(ComponentModel model) {
        try {
            ComponentFactory componentFactory = ComponentUtil.getComponentFactory(session, model);
            if (componentFactory == null && System.getProperty(COMPONENT_PROVIDER_EXISTS_DISABLED) == null) {
                throw new IllegalArgumentException("Invalid component type");
            }
            componentFactory.validateConfiguration(session, this, model);
        } catch (IllegalArgumentException | ComponentValidationException e) {
            if (System.getProperty(COMPONENT_PROVIDER_EXISTS_DISABLED) == null) {
                throw e;
            }
        }

        MapComponentEntity component = MapComponentEntity.fromModel(model);
        if (model.getParentId() == null) {
            component.setParentId(getId());
            model.setParentId(getId());
        }
        entity.addComponent(component);
        model.setId(component.getId());
        return model;
    }

    @Override
    public void updateComponent(ComponentModel component) {
        ComponentUtil.getComponentFactory(session, component).validateConfiguration(session, this, component);

        MapComponentEntity old = entity.getComponent(component.getId());
        if (old == null) return;

        entity.updateComponent(MapComponentEntity.fromModel(component));
        ComponentUtil.notifyUpdated(session, this, MapComponentEntity.toModel(old), component);
    }

    @Override
    public void removeComponent(ComponentModel component) {
        if (entity.getComponent(component.getId()) == null) return;

        session.users().preRemove(this, component);
        ComponentUtil.notifyPreRemove(session, this, component);
        removeComponents(component.getId());
        entity.removeComponent(component.getId());
    }

    @Override
    public void removeComponents(String parentId) {
        entity.getComponents()
            .filter(c -> Objects.equals(parentId, c.getParentId()))
            .map(MapComponentEntity::toModel)
            .collect(Collectors.toSet())  // This is necessary to read out all the components before removing them
            .forEach(c -> {
                session.users().preRemove(this, c);
                ComponentUtil.notifyPreRemove(session, this, c);
                entity.removeComponent(c.getId());
            });
    }

    @Override
    public Stream<ComponentModel> getComponentsStream() {
        return entity.getComponents().map(MapComponentEntity::toModel);
    }

    @Override
    public Stream<ComponentModel> getComponentsStream(String parentId) {
        return entity.getComponents()
            .filter(c -> Objects.equals(parentId, c.getParentId()))
            .map(MapComponentEntity::toModel);
    }

    @Override
    public Stream<ComponentModel> getComponentsStream(String parentId, String providerType) {
        return entity.getComponents()
                .filter(c -> Objects.equals(parentId, c.getParentId()))
                .filter(c -> Objects.equals(providerType, c.getProviderType()))
                .map(MapComponentEntity::toModel);
    }

    @Override
    public ComponentModel getComponent(String id) {
        return MapComponentEntity.toModel(entity.getComponent(id));
    }

    @Override
    public String getLoginTheme() {
        return entity.getLoginTheme();
    }

    @Override
    public void setLoginTheme(String name) {
        entity.setLoginTheme(name);
    }

    @Override
    public String getAccountTheme() {
        return entity.getAccountTheme();
    }

    @Override
    public void setAccountTheme(String name) {
        entity.setAccountTheme(name);
    }

    @Override
    public String getAdminTheme() {
        return entity.getAdminTheme();
    }

    @Override
    public void setAdminTheme(String name) {
        entity.setAdminTheme(name);
    }

    @Override
    public String getEmailTheme() {
        return entity.getEmailTheme();
    }

    @Override
    public void setEmailTheme(String name) {
        entity.setEmailTheme(name);
    }

    @Override
    public int getNotBefore() {
        return entity.getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        entity.setNotBefore(notBefore);
    }

    @Override
    public boolean isEventsEnabled() {
        return entity.isEventsEnabled();
    }

    @Override
    public void setEventsEnabled(boolean enabled) {
        entity.setEventsEnabled(enabled);
    }

    @Override
    public long getEventsExpiration() {
        return entity.getEventsExpiration();
    }

    @Override
    public void setEventsExpiration(long expiration) {
        entity.setEventsExpiration(expiration);
    }

    @Override
    public Stream<String> getEventsListenersStream() {
        return entity.getEventsListeners().stream();
    }

    @Override
    public void setEventsListeners(Set<String> listeners) {
        entity.setEventsListeners(listeners);
    }

    @Override
    public Stream<String> getEnabledEventTypesStream() {
        return entity.getEnabledEventTypes().stream();
    }

    @Override
    public void setEnabledEventTypes(Set<String> enabledEventTypes) {
        entity.setEnabledEventTypes(enabledEventTypes);
    }

    @Override
    public boolean isAdminEventsEnabled() {
        return entity.isAdminEventsEnabled();
    }

    @Override
    public void setAdminEventsEnabled(boolean enabled) {
        entity.setAdminEventsEnabled(enabled);
    }

    @Override
    public boolean isAdminEventsDetailsEnabled() {
        return entity.isAdminEventsDetailsEnabled();
    }

    @Override
    public void setAdminEventsDetailsEnabled(boolean enabled) {
        entity.setAdminEventsDetailsEnabled(enabled);
    }

    @Override
    public ClientModel getMasterAdminClient() {
        String masterAdminClientId = entity.getMasterAdminClient();
        if (masterAdminClientId == null) {
            return null;
        }
        RealmModel masterRealm = getName().equals(Config.getAdminRealm())
          ? this
          : session.realms().getRealm(Config.getAdminRealm());
        return session.clients().getClientById(masterRealm, masterAdminClientId);
    }

    @Override
    public void setMasterAdminClient(ClientModel client) {
        String id = client == null ? null : client.getId();
        entity.setMasterAdminClient(id);
    }

    @Override
    public RoleModel getDefaultRole() {
        return session.roles().getRoleById(this, entity.getDefaultRoleId());
    }

    @Override
    public void setDefaultRole(RoleModel role) {
        entity.setDefaultRoleId(role.getId());
    }

    @Override
    public boolean isIdentityFederationEnabled() {
        return entity.getIdentityProviders().findFirst().isPresent();
    }

    @Override
    public boolean isInternationalizationEnabled() {
        return entity.isInternationalizationEnabled();
    }

    @Override
    public void setInternationalizationEnabled(boolean enabled) {
        entity.setInternationalizationEnabled(enabled);
    }

    @Override
    public Stream<String> getSupportedLocalesStream() {
        return entity.getSupportedLocales().stream();
    }

    @Override
    public void setSupportedLocales(Set<String> locales) {
        entity.setSupportedLocales(locales);
    }

    @Override
    public String getDefaultLocale() {
        return entity.getDefaultLocale();
    }

    @Override
    public void setDefaultLocale(String locale) {
        entity.setDefaultLocale(locale);
    }

    @Override
    public GroupModel createGroup(String id, String name, GroupModel toParent) {
        return session.groups().createGroup(this, id, name, toParent);
    }

    @Override
    public GroupModel getGroupById(String id) {
        return session.groups().getGroupById(this, id);
    }

    @Override
    public Stream<GroupModel> getGroupsStream() {
        return session.groups().getGroupsStream(this);
    }

    @Override
    public Long getGroupsCount(Boolean onlyTopGroups) {
        return session.groups().getGroupsCount(this, onlyTopGroups);
    }

    @Override
    public Long getGroupsCountByNameContaining(String search) {
        return session.groups().getGroupsCountByNameContaining(this, search);
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream() {
        return session.groups().getTopLevelGroupsStream(this);
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(Integer first, Integer max) {
        return session.groups().getTopLevelGroupsStream(this, first, max);
    }

    @Override
    public Stream<GroupModel> searchForGroupByNameStream(String search, Integer first, Integer max) {
        return session.groups().searchForGroupByNameStream(this, search, first, max);
    }

    @Override
    public boolean removeGroup(GroupModel group) {
        return session.groups().removeGroup(this, group);
    }

    @Override
    public void moveGroup(GroupModel group, GroupModel toParent) {
        session.groups().moveGroup(this, group, toParent);
    }

    @Override
    public Stream<ClientScopeModel> getClientScopesStream() {
        return session.clientScopes().getClientScopesStream(this);
    }

    @Override
    public ClientScopeModel addClientScope(String name) {
        return session.clientScopes().addClientScope(this, name);
    }

    @Override
    public ClientScopeModel addClientScope(String id, String name) {
        return session.clientScopes().addClientScope(this, id, name);
    }

    @Override
    public boolean removeClientScope(String id) {
        return session.clientScopes().removeClientScope(this, id);
    }

    @Override
    public ClientScopeModel getClientScopeById(String id) {
        return session.clientScopes().getClientScopeById(this, id);
    }

    @Override
    public void addDefaultClientScope(ClientScopeModel clientScope, boolean defaultScope) {
        if (defaultScope) {
            entity.addDefaultClientScope(clientScope.getId());
        } else {
            entity.addOptionalClientScope(clientScope.getId());
        }
    }

    @Override
    public void removeDefaultClientScope(ClientScopeModel clientScope) {
        entity.removeDefaultOrOptionalClientScope(clientScope.getId());
    }

    @Override
    public Stream<ClientScopeModel> getDefaultClientScopesStream(boolean defaultScope) {
        if (defaultScope) {
            return entity.getDefaultClientScopeIds().map(this::getClientScopeById);
        } else {
            return entity.getOptionalClientScopeIds().map(this::getClientScopeById);
        }
    }

    @Override
    public void patchRealmLocalizationTexts(String locale, Map<String, String> localizationTexts) {
        Map<String, Map<String, String>> realmLocalizationTexts = entity.getLocalizationTexts();

        if (realmLocalizationTexts.containsKey(locale)) {
            Map<String, String> currentTexts = realmLocalizationTexts.get(locale);
            currentTexts.putAll(localizationTexts);
            entity.updateLocalizationTexts(locale, currentTexts);
        } else {
            entity.addLocalizationTexts(locale, localizationTexts);
        }
    }

    @Override
    public boolean removeRealmLocalizationTexts(String locale) {
        if (locale == null) return false;
        return entity.removeLocalizationTexts(locale);
    }

    @Override
    public Map<String, Map<String, String>> getRealmLocalizationTexts() {
        return entity.getLocalizationTexts();
    }

    @Override
    public Map<String, String> getRealmLocalizationTextsByLocale(String locale) {
        return entity.getLocalizationText(locale);
    }

    @Override
    public RoleModel getRole(String name) {
        return session.roles().getRealmRole(this, name);
    }

    @Override
    public RoleModel addRole(String name) {
        return session.roles().addRealmRole(this, name);
    }

    @Override
    public RoleModel addRole(String id, String name) {
        return session.roles().addRealmRole(this, id, name);
    }

    @Override
    public boolean removeRole(RoleModel role) {
        return session.roles().removeRole(role);
    }

    @Override
    public Stream<RoleModel> getRolesStream() {
        return session.roles().getRealmRolesStream(this);
    }

    @Override
    public Stream<RoleModel> getRolesStream(Integer firstResult, Integer maxResults) {
        return session.roles().getRealmRolesStream(this, firstResult, maxResults);
    }

    @Override
    public Stream<RoleModel> searchForRolesStream(String search, Integer first, Integer max) {
        return session.roles().searchForRolesStream(this, search, first, max);
    }

    @Override
    @Deprecated
    public Stream<String> getDefaultRolesStream() {
        return getDefaultRole().getCompositesStream().filter(this::isRealmRole).map(RoleModel::getName);
    }

    private boolean isRealmRole(RoleModel role) {
        return ! role.isClientRole();
    }

    @Override
    @Deprecated
    public void addDefaultRole(String name) {
        getDefaultRole().addCompositeRole(getOrAddRoleId(name));
    }

    private RoleModel getOrAddRoleId(String name) {
        RoleModel role = getRole(name);
        if (role == null) {
            role = addRole(name);
        }
        return role;
    }

    @Override
    @Deprecated
    public void removeDefaultRoles(String... defaultRoles) {
        for (String defaultRole : defaultRoles) {
            getDefaultRole().removeCompositeRole(getRole(defaultRole));
        }
    }

    @Override
    public boolean isBruteForceProtected() {
        return getAttribute(BRUTE_FORCE_PROTECTED, false);
    }

    @Override
    public void setBruteForceProtected(boolean value) {
        setAttribute(BRUTE_FORCE_PROTECTED, value);
    }

    @Override
    public boolean isPermanentLockout() {
        return getAttribute(PERMANENT_LOCKOUT, false);
    }

    @Override
    public void setPermanentLockout(final boolean val) {
        setAttribute(PERMANENT_LOCKOUT, val);
    }

    @Override
    public int getMaxFailureWaitSeconds() {
        return getAttribute(MAX_FAILURE_WAIT_SECONDS, 0);
    }

    @Override
    public void setMaxFailureWaitSeconds(int val) {
        setAttribute(MAX_FAILURE_WAIT_SECONDS, val);
    }

    @Override
    public int getWaitIncrementSeconds() {
        return getAttribute(WAIT_INCREMENT_SECONDS, 0);
    }

    @Override
    public void setWaitIncrementSeconds(int val) {
        setAttribute(WAIT_INCREMENT_SECONDS, val);
    }

    @Override
    public int getMinimumQuickLoginWaitSeconds() {
        return getAttribute(MINIMUM_QUICK_LOGIN_WAIT_SECONDS, 0);
    }

    @Override
    public void setMinimumQuickLoginWaitSeconds(int val) {
        setAttribute(MINIMUM_QUICK_LOGIN_WAIT_SECONDS, val);
    }

    @Override
    public long getQuickLoginCheckMilliSeconds() {
        return getAttribute(QUICK_LOGIN_CHECK_MILLISECONDS, 0L);
    }

    @Override
    public void setQuickLoginCheckMilliSeconds(long val) {
        setAttribute(QUICK_LOGIN_CHECK_MILLISECONDS, val);
    }

    @Override
    public int getMaxDeltaTimeSeconds() {
        return getAttribute(MAX_DELTA_SECONDS, 0);
    }

    @Override
    public void setMaxDeltaTimeSeconds(int val) {
        setAttribute(MAX_DELTA_SECONDS, val);
    }

    @Override
    public int getFailureFactor() {
        return getAttribute(FAILURE_FACTOR, 0);
    }

    @Override
    public void setFailureFactor(int failureFactor) {
        setAttribute(FAILURE_FACTOR, failureFactor);
    }

    @Override
    public String getDefaultSignatureAlgorithm() {
        return getAttribute(DEFAULT_SIGNATURE_ALGORITHM);
    }

    @Override
    public void setDefaultSignatureAlgorithm(String defaultSignatureAlgorithm) {
        setAttribute(DEFAULT_SIGNATURE_ALGORITHM, defaultSignatureAlgorithm);
    }

    @Override
    public boolean isOfflineSessionMaxLifespanEnabled() {
        return entity.isOfflineSessionMaxLifespanEnabled();
    }

    @Override
    public void setOfflineSessionMaxLifespanEnabled(boolean offlineSessionMaxLifespanEnabled) {
        entity.setOfflineSessionMaxLifespanEnabled(offlineSessionMaxLifespanEnabled);
    }

    @Override
    public int getOfflineSessionMaxLifespan() {
        return entity.getOfflineSessionMaxLifespan();
    }

    @Override
    public void setOfflineSessionMaxLifespan(int seconds) {
        entity.setOfflineSessionMaxLifespan(seconds);
    }

    @Override
    public WebAuthnPolicy getWebAuthnPolicy() {
        return MapWebAuthnPolicyEntity.toModel(entity.getWebAuthnPolicy());
    }

    @Override
    public void setWebAuthnPolicy(WebAuthnPolicy policy) {
        entity.setWebAuthnPolicy(MapWebAuthnPolicyEntity.fromModel(policy));
    }

    @Override
    public WebAuthnPolicy getWebAuthnPolicyPasswordless() {
        return MapWebAuthnPolicyEntity.toModel(entity.getWebAuthnPolicyPasswordless());
    }

    @Override
    public void setWebAuthnPolicyPasswordless(WebAuthnPolicy policy) {
        entity.setWebAuthnPolicyPasswordless(MapWebAuthnPolicyEntity.fromModel(policy));
    }

    @Override
    public Map<String, String> getBrowserSecurityHeaders() {
        return Collections.unmodifiableMap(entity.getBrowserSecurityHeaders());
    }

    @Override
    public void setBrowserSecurityHeaders(Map<String, String> headers) {
        entity.setBrowserSecurityHeaders(headers);
    }

    @Override
    public ClientInitialAccessModel createClientInitialAccessModel(int expiration, int count) {
        MapClientInitialAccessEntity clientInitialAccess = MapClientInitialAccessEntity.createEntity(expiration, count);
        entity.addClientInitialAccess(clientInitialAccess);
        return MapClientInitialAccessEntity.toModel(clientInitialAccess);
    }

    @Override
    public ClientInitialAccessModel getClientInitialAccessModel(String id) {
        return MapClientInitialAccessEntity.toModel(entity.getClientInitialAccess(id));
    }

    @Override
    public void removeClientInitialAccessModel(String id) {
        entity.removeClientInitialAccess(id);
    }

    @Override
    public Stream<ClientInitialAccessModel> getClientInitialAccesses() {
        return entity.getClientInitialAccesses().stream().map(MapClientInitialAccessEntity::toModel);
    }

    @Override
    public void decreaseRemainingCount(ClientInitialAccessModel model) {
        MapClientInitialAccessEntity clientInitialAccess = entity.getClientInitialAccess(model.getId());
        clientInitialAccess.setRemainingCount(model.getRemainingCount() - 1);
        entity.updateClientInitialAccess(clientInitialAccess);
    }

    @Override
    public OAuth2DeviceConfig getOAuth2DeviceConfig() {
        return new OAuth2DeviceConfig(this);
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), hashCode());
    }

    public CibaConfig getCibaPolicy() {
        return new CibaConfig(this);
    }

    public ParConfig getParPolicy() {
        return new ParConfig(this);
    }
}
