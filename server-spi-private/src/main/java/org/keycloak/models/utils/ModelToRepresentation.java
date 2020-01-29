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

package org.keycloak.models.utils;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialModel;
import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.models.*;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.*;
import org.keycloak.representations.idm.authorization.*;
import org.keycloak.storage.StorageId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ModelToRepresentation {
    public static void buildGroupPath(StringBuilder sb, GroupModel group) {
        if (group.getParent() != null) {
            buildGroupPath(sb, group.getParent());
        }
        sb.append('/').append(group.getName());
    }

    public static String buildGroupPath(GroupModel group) {
        StringBuilder sb = new StringBuilder();
        buildGroupPath(sb, group);
        return sb.toString();
    }


    public static GroupRepresentation toRepresentation(GroupModel group, boolean full) {
        GroupRepresentation rep = new GroupRepresentation();
        rep.setId(group.getId());
        rep.setName(group.getName());
        rep.setPath(buildGroupPath(group));
        if (!full) return rep;
        // Role mappings
        Set<RoleModel> roles = group.getRoleMappings();
        List<String> realmRoleNames = new ArrayList<>();
        Map<String, List<String>> clientRoleNames = new HashMap<>();
        for (RoleModel role : roles) {
            if (role.getContainer() instanceof RealmModel) {
                realmRoleNames.add(role.getName());
            } else {
                ClientModel client = (ClientModel)role.getContainer();
                String clientId = client.getClientId();
                List<String> currentClientRoles = clientRoleNames.computeIfAbsent(clientId, k -> new ArrayList<>());
                currentClientRoles.add(role.getName());
            }
        }
        rep.setRealmRoles(realmRoleNames);
        rep.setClientRoles(clientRoleNames);
        Map<String, List<String>> attributes = group.getAttributes();
        rep.setAttributes(attributes);
        return rep;
    }

    public static List<GroupRepresentation> searchForGroupByName(RealmModel realm, boolean full, String search, Integer first, Integer max) {
        List<GroupRepresentation> result = new LinkedList<>();
        List<GroupModel> groups = realm.searchForGroupByName(search, first, max);
        if (Objects.isNull(groups)) return result;
        for (GroupModel group : groups) {
            GroupRepresentation rep = toGroupHierarchy(group, full);
            result.add(rep);
        }
        return result;
    }

    public static List<GroupRepresentation> searchForGroupByName(UserModel user, boolean full, String search, Integer first, Integer max) {
        return user.getGroups(search, first, max).stream()
                .map(group -> toRepresentation(group, full))
                .collect(Collectors.toList());
    }

    public static List<GroupRepresentation> toGroupHierarchy(RealmModel realm, boolean full, Integer first, Integer max) {
        List<GroupRepresentation> hierarchy = new LinkedList<>();
        List<GroupModel> groups = realm.getTopLevelGroups(first, max);
        if (Objects.isNull(groups)) return hierarchy;
        for (GroupModel group : groups) {
            GroupRepresentation rep = toGroupHierarchy(group, full);
            hierarchy.add(rep);
        }
        return hierarchy;
    }

    public static List<GroupRepresentation> toGroupHierarchy(UserModel user, boolean full, Integer first, Integer max) {
        return user.getGroups(first, max).stream()
                .map(group -> toRepresentation(group, full))
                .collect(Collectors.toList());
    }

    public static List<GroupRepresentation> toGroupHierarchy(RealmModel realm, boolean full) {
        List<GroupRepresentation> hierarchy = new LinkedList<>();
        List<GroupModel> groups = realm.getTopLevelGroups();
        if (Objects.isNull(groups)) return hierarchy;
        for (GroupModel group : groups) {
            GroupRepresentation rep = toGroupHierarchy(group, full);
            hierarchy.add(rep);
        }
        return hierarchy;
    }

    public static List<GroupRepresentation> toGroupHierarchy(UserModel user, boolean full) {
        return user.getGroups().stream()
                .map(group -> toRepresentation(group, full))
                .collect(Collectors.toList());
    }

    public static GroupRepresentation toGroupHierarchy(GroupModel group, boolean full) {
        GroupRepresentation rep = toRepresentation(group, full);
        List<GroupRepresentation> subGroups = new LinkedList<>();
        for (GroupModel subGroup : group.getSubGroups()) {
            subGroups.add(toGroupHierarchy(subGroup, full));
        }
        rep.setSubGroups(subGroups);
        return rep;
    }

    public static UserRepresentation toRepresentation(KeycloakSession session, RealmModel realm, UserModel user) {
        UserRepresentation rep = new UserRepresentation();
        rep.setId(user.getId());
        String providerId = StorageId.resolveProviderId(user);
        rep.setOrigin(providerId);
        rep.setUsername(user.getUsername());
        rep.setCreatedTimestamp(user.getCreatedTimestamp());
        rep.setLastName(user.getLastName());
        rep.setFirstName(user.getFirstName());
        rep.setEmail(user.getEmail());
        rep.setEnabled(user.isEnabled());
        rep.setEmailVerified(user.isEmailVerified());
        rep.setTotp(session.userCredentialManager().isConfiguredFor(realm, user, OTPCredentialModel.TYPE));
        rep.setDisableableCredentialTypes(session.userCredentialManager().getDisableableCredentialTypes(realm, user));
        rep.setFederationLink(user.getFederationLink());

        rep.setNotBefore(session.users().getNotBeforeOfUser(realm, user));

        Set<String> requiredActions = user.getRequiredActions();
        List<String> reqActions = new ArrayList<>(requiredActions);

        rep.setRequiredActions(reqActions);

        if (user.getAttributes() != null && !user.getAttributes().isEmpty()) {
            Map<String, List<String>> attrs = new HashMap<>(user.getAttributes());
            rep.setAttributes(attrs);
        }

        return rep;
    }

    public static UserRepresentation toBriefRepresentation(UserModel user) {
        UserRepresentation rep = new UserRepresentation();
        rep.setId(user.getId());
        rep.setUsername(user.getUsername());
        rep.setCreatedTimestamp(user.getCreatedTimestamp());
        rep.setLastName(user.getLastName());
        rep.setFirstName(user.getFirstName());
        rep.setEmail(user.getEmail());
        rep.setEnabled(user.isEnabled());
        rep.setEmailVerified(user.isEmailVerified());
        rep.setFederationLink(user.getFederationLink());

        return rep;
    }

    public static EventRepresentation toRepresentation(Event event) {
        EventRepresentation rep = new EventRepresentation();
        rep.setTime(event.getTime());
        rep.setType(event.getType().toString());
        rep.setRealmId(event.getRealmId());
        rep.setClientId(event.getClientId());
        rep.setUserId(event.getUserId());
        rep.setSessionId(event.getSessionId());
        rep.setIpAddress(event.getIpAddress());
        rep.setError(event.getError());
        rep.setDetails(event.getDetails());
        return rep;
    }

    public static AdminEventRepresentation toRepresentation(AdminEvent adminEvent) {
        AdminEventRepresentation rep = new AdminEventRepresentation();
        rep.setTime(adminEvent.getTime());
        rep.setRealmId(adminEvent.getRealmId());
        if (adminEvent.getAuthDetails() != null) {
            rep.setAuthDetails(toRepresentation(adminEvent.getAuthDetails()));
        }
        rep.setOperationType(adminEvent.getOperationType().toString());
        if (adminEvent.getResourceTypeAsString() != null) {
            rep.setResourceType(adminEvent.getResourceTypeAsString());
        }
        rep.setResourcePath(adminEvent.getResourcePath());
        rep.setRepresentation(adminEvent.getRepresentation());
        rep.setError(adminEvent.getError());

        return rep;
    }

    public static AuthDetailsRepresentation toRepresentation(AuthDetails authDetails) {
        AuthDetailsRepresentation rep = new AuthDetailsRepresentation();
        rep.setRealmId(authDetails.getRealmId());
        rep.setClientId(authDetails.getClientId());
        rep.setUserId(authDetails.getUserId());
        rep.setIpAddress(authDetails.getIpAddress());
        return rep;
    }

    public static RoleRepresentation toRepresentation(RoleModel role) {
        RoleRepresentation rep = new RoleRepresentation();
        rep.setId(role.getId());
        rep.setName(role.getName());
        rep.setDescription(role.getDescription());
        rep.setComposite(role.isComposite());
        rep.setClientRole(role.isClientRole());
        rep.setContainerId(role.getContainerId());
        rep.setAttributes(role.getAttributes());
        return rep;
    }

    public static RoleRepresentation toBriefRepresentation(RoleModel role) {
        RoleRepresentation rep = new RoleRepresentation();
        rep.setId(role.getId());
        rep.setName(role.getName());
        rep.setDescription(role.getDescription());
        rep.setComposite(role.isComposite());
        rep.setClientRole(role.isClientRole());
        rep.setContainerId(role.getContainerId());
        return rep;
    }

    public static RealmRepresentation toRepresentation(RealmModel realm, boolean internal) {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setId(realm.getId());
        rep.setRealm(realm.getName());
        rep.setDisplayName(realm.getDisplayName());
        rep.setDisplayNameHtml(realm.getDisplayNameHtml());
        rep.setEnabled(realm.isEnabled());
        rep.setNotBefore(realm.getNotBefore());
        rep.setSslRequired(realm.getSslRequired().name().toLowerCase());
        rep.setRegistrationAllowed(realm.isRegistrationAllowed());
        rep.setRegistrationEmailAsUsername(realm.isRegistrationEmailAsUsername());
        rep.setRememberMe(realm.isRememberMe());
        rep.setBruteForceProtected(realm.isBruteForceProtected());
        rep.setPermanentLockout(realm.isPermanentLockout());
        rep.setMaxFailureWaitSeconds(realm.getMaxFailureWaitSeconds());
        rep.setMinimumQuickLoginWaitSeconds(realm.getMinimumQuickLoginWaitSeconds());
        rep.setWaitIncrementSeconds(realm.getWaitIncrementSeconds());
        rep.setQuickLoginCheckMilliSeconds(realm.getQuickLoginCheckMilliSeconds());
        rep.setMaxDeltaTimeSeconds(realm.getMaxDeltaTimeSeconds());
        rep.setFailureFactor(realm.getFailureFactor());
        rep.setUserManagedAccessAllowed(realm.isUserManagedAccessAllowed());

        rep.setEventsEnabled(realm.isEventsEnabled());
        if (realm.getEventsExpiration() != 0) {
            rep.setEventsExpiration(realm.getEventsExpiration());
        }
        if (realm.getEventsListeners() != null) {
            rep.setEventsListeners(new LinkedList<>(realm.getEventsListeners()));
        }
        if (realm.getEnabledEventTypes() != null) {
            rep.setEnabledEventTypes(new LinkedList<>(realm.getEnabledEventTypes()));
        }

        rep.setAdminEventsEnabled(realm.isAdminEventsEnabled());
        rep.setAdminEventsDetailsEnabled(realm.isAdminEventsDetailsEnabled());

        rep.setVerifyEmail(realm.isVerifyEmail());
        rep.setLoginWithEmailAllowed(realm.isLoginWithEmailAllowed());
        rep.setDuplicateEmailsAllowed(realm.isDuplicateEmailsAllowed());
        rep.setResetPasswordAllowed(realm.isResetPasswordAllowed());
        rep.setEditUsernameAllowed(realm.isEditUsernameAllowed());
        rep.setDefaultSignatureAlgorithm(realm.getDefaultSignatureAlgorithm());
        rep.setRevokeRefreshToken(realm.isRevokeRefreshToken());
        rep.setRefreshTokenMaxReuse(realm.getRefreshTokenMaxReuse());
        rep.setAccessTokenLifespan(realm.getAccessTokenLifespan());
        rep.setAccessTokenLifespanForImplicitFlow(realm.getAccessTokenLifespanForImplicitFlow());
        rep.setSsoSessionIdleTimeout(realm.getSsoSessionIdleTimeout());
        rep.setSsoSessionMaxLifespan(realm.getSsoSessionMaxLifespan());
        rep.setSsoSessionIdleTimeoutRememberMe(realm.getSsoSessionIdleTimeoutRememberMe());
        rep.setSsoSessionMaxLifespanRememberMe(realm.getSsoSessionMaxLifespanRememberMe());
        rep.setOfflineSessionIdleTimeout(realm.getOfflineSessionIdleTimeout());
        // KEYCLOAK-7688 Offline Session Max for Offline Token
        rep.setOfflineSessionMaxLifespanEnabled(realm.isOfflineSessionMaxLifespanEnabled());
        rep.setOfflineSessionMaxLifespan(realm.getOfflineSessionMaxLifespan());
        rep.setAccessCodeLifespan(realm.getAccessCodeLifespan());
        rep.setAccessCodeLifespanUserAction(realm.getAccessCodeLifespanUserAction());
        rep.setAccessCodeLifespanLogin(realm.getAccessCodeLifespanLogin());
        rep.setActionTokenGeneratedByAdminLifespan(realm.getActionTokenGeneratedByAdminLifespan());
        rep.setActionTokenGeneratedByUserLifespan(realm.getActionTokenGeneratedByUserLifespan());
        rep.setSmtpServer(new HashMap<>(realm.getSmtpConfig()));
        rep.setBrowserSecurityHeaders(realm.getBrowserSecurityHeaders());
        rep.setAccountTheme(realm.getAccountTheme());
        rep.setLoginTheme(realm.getLoginTheme());
        rep.setAdminTheme(realm.getAdminTheme());
        rep.setEmailTheme(realm.getEmailTheme());
        if (realm.getPasswordPolicy() != null) {
            rep.setPasswordPolicy(realm.getPasswordPolicy().toString());
        }
        OTPPolicy otpPolicy = realm.getOTPPolicy();
        rep.setOtpPolicyAlgorithm(otpPolicy.getAlgorithm());
        rep.setOtpPolicyPeriod(otpPolicy.getPeriod());
        rep.setOtpPolicyDigits(otpPolicy.getDigits());
        rep.setOtpPolicyInitialCounter(otpPolicy.getInitialCounter());
        rep.setOtpPolicyType(otpPolicy.getType());
        rep.setOtpPolicyLookAheadWindow(otpPolicy.getLookAheadWindow());
        rep.setOtpSupportedApplications(otpPolicy.getSupportedApplications());

        WebAuthnPolicy webAuthnPolicy = realm.getWebAuthnPolicy();
        rep.setWebAuthnPolicyRpEntityName(webAuthnPolicy.getRpEntityName());
        rep.setWebAuthnPolicySignatureAlgorithms(webAuthnPolicy.getSignatureAlgorithm());
        rep.setWebAuthnPolicyRpId(webAuthnPolicy.getRpId());
        rep.setWebAuthnPolicyAttestationConveyancePreference(webAuthnPolicy.getAttestationConveyancePreference());
        rep.setWebAuthnPolicyAuthenticatorAttachment(webAuthnPolicy.getAuthenticatorAttachment());
        rep.setWebAuthnPolicyRequireResidentKey(webAuthnPolicy.getRequireResidentKey());
        rep.setWebAuthnPolicyUserVerificationRequirement(webAuthnPolicy.getUserVerificationRequirement());
        rep.setWebAuthnPolicyCreateTimeout(webAuthnPolicy.getCreateTimeout());
        rep.setWebAuthnPolicyAvoidSameAuthenticatorRegister(webAuthnPolicy.isAvoidSameAuthenticatorRegister());
        rep.setWebAuthnPolicyAcceptableAaguids(webAuthnPolicy.getAcceptableAaguids());

        webAuthnPolicy = realm.getWebAuthnPolicyPasswordless();
        rep.setWebAuthnPolicyPasswordlessRpEntityName(webAuthnPolicy.getRpEntityName());
        rep.setWebAuthnPolicyPasswordlessSignatureAlgorithms(webAuthnPolicy.getSignatureAlgorithm());
        rep.setWebAuthnPolicyPasswordlessRpId(webAuthnPolicy.getRpId());
        rep.setWebAuthnPolicyPasswordlessAttestationConveyancePreference(webAuthnPolicy.getAttestationConveyancePreference());
        rep.setWebAuthnPolicyPasswordlessAuthenticatorAttachment(webAuthnPolicy.getAuthenticatorAttachment());
        rep.setWebAuthnPolicyPasswordlessRequireResidentKey(webAuthnPolicy.getRequireResidentKey());
        rep.setWebAuthnPolicyPasswordlessUserVerificationRequirement(webAuthnPolicy.getUserVerificationRequirement());
        rep.setWebAuthnPolicyPasswordlessCreateTimeout(webAuthnPolicy.getCreateTimeout());
        rep.setWebAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister(webAuthnPolicy.isAvoidSameAuthenticatorRegister());
        rep.setWebAuthnPolicyPasswordlessAcceptableAaguids(webAuthnPolicy.getAcceptableAaguids());

        if (realm.getBrowserFlow() != null) rep.setBrowserFlow(realm.getBrowserFlow().getAlias());
        if (realm.getRegistrationFlow() != null) rep.setRegistrationFlow(realm.getRegistrationFlow().getAlias());
        if (realm.getDirectGrantFlow() != null) rep.setDirectGrantFlow(realm.getDirectGrantFlow().getAlias());
        if (realm.getResetCredentialsFlow() != null) rep.setResetCredentialsFlow(realm.getResetCredentialsFlow().getAlias());
        if (realm.getClientAuthenticationFlow() != null) rep.setClientAuthenticationFlow(realm.getClientAuthenticationFlow().getAlias());
        if (realm.getDockerAuthenticationFlow() != null) rep.setDockerAuthenticationFlow(realm.getDockerAuthenticationFlow().getAlias());

        List<String> defaultRoles = realm.getDefaultRoles();
        if (!defaultRoles.isEmpty()) {
            List<String> roleStrings = new ArrayList<>(defaultRoles);
            rep.setDefaultRoles(roleStrings);
        }
        List<GroupModel> defaultGroups = realm.getDefaultGroups();
        if (!defaultGroups.isEmpty()) {
            List<String> groupPaths = new LinkedList<>();
            for (GroupModel group : defaultGroups) {
                groupPaths.add(ModelToRepresentation.buildGroupPath(group));
            }
            rep.setDefaultGroups(groupPaths);
        }

        List<RequiredCredentialModel> requiredCredentialModels = realm.getRequiredCredentials();
        if (!requiredCredentialModels.isEmpty()) {
            rep.setRequiredCredentials(new HashSet<>());
            for (RequiredCredentialModel cred : requiredCredentialModels) {
                rep.getRequiredCredentials().add(cred.getType());
            }
        }

        for (IdentityProviderModel provider : realm.getIdentityProviders()) {
            rep.addIdentityProvider(toRepresentation(realm, provider));
        }

        for (IdentityProviderMapperModel mapper : realm.getIdentityProviderMappers()) {
            rep.addIdentityProviderMapper(toRepresentation(mapper));
        }

        rep.setInternationalizationEnabled(realm.isInternationalizationEnabled());
        if (realm.getSupportedLocales() != null) {
            rep.setSupportedLocales(new HashSet<>());
            rep.getSupportedLocales().addAll(realm.getSupportedLocales());
        }
        rep.setDefaultLocale(realm.getDefaultLocale());
        if (internal) {
            exportAuthenticationFlows(realm, rep);
            exportRequiredActions(realm, rep);
            exportGroups(realm, rep);
        }

        Map<String, String> attributes = realm.getAttributes();
        rep.setAttributes(attributes);

        if (!internal) {
            rep = StripSecretsUtils.strip(rep);
        }

        return rep;
    }

    public static void exportGroups(RealmModel realm, RealmRepresentation rep) {
        List<GroupRepresentation> groups = toGroupHierarchy(realm, true);
        rep.setGroups(groups);
    }

    public static void exportAuthenticationFlows(RealmModel realm, RealmRepresentation rep) {
        rep.setAuthenticationFlows(new LinkedList<>());
        rep.setAuthenticatorConfig(new LinkedList<>());

        List<AuthenticationFlowModel> authenticationFlows = new ArrayList<>(realm.getAuthenticationFlows());
        //ensure consistent ordering of authenticationFlows.
        Collections.sort(authenticationFlows, new Comparator<AuthenticationFlowModel>() {
            @Override
            public int compare(AuthenticationFlowModel left, AuthenticationFlowModel right) {
                String l = left.getAlias() != null ? left.getAlias() : "\0";
                String r = right.getAlias() != null ? right.getAlias() : "\0";
                return l.compareTo(r);
            }
        });

        for (AuthenticationFlowModel model : authenticationFlows) {
            AuthenticationFlowRepresentation flowRep = toRepresentation(realm, model);
            rep.getAuthenticationFlows().add(flowRep);
        }

        List<AuthenticatorConfigModel> authenticatorConfigs = new ArrayList<>(realm.getAuthenticatorConfigs());
        //ensure consistent ordering of authenticatorConfigs.
        Collections.sort(authenticatorConfigs, new Comparator<AuthenticatorConfigModel>() {
            @Override
            public int compare(AuthenticatorConfigModel left, AuthenticatorConfigModel right) {
                String l = left.getAlias() != null ? left.getAlias() : "\0";
                String r = right.getAlias() != null ? right.getAlias() : "\0";
                return l.compareTo(r);
            }
        });

        for (AuthenticatorConfigModel model : authenticatorConfigs) {
            rep.getAuthenticatorConfig().add(toRepresentation(model));
        }

    }

    public static void exportRequiredActions(RealmModel realm, RealmRepresentation rep) {

        rep.setRequiredActions(new LinkedList<>());

        realm.getRequiredActionProviders().forEach(action -> rep.getRequiredActions().add(toRepresentation(action)));
    }

    public static RealmEventsConfigRepresentation toEventsConfigReprensetation(RealmModel realm) {
        RealmEventsConfigRepresentation rep = new RealmEventsConfigRepresentation();
        rep.setEventsEnabled(realm.isEventsEnabled());

        if (realm.getEventsExpiration() != 0) {
            rep.setEventsExpiration(realm.getEventsExpiration());
        }

        if (realm.getEventsListeners() != null) {
            rep.setEventsListeners(new LinkedList<>(realm.getEventsListeners()));
        }

        if (realm.getEnabledEventTypes() != null) {
            rep.setEnabledEventTypes(new LinkedList<>(realm.getEnabledEventTypes()));
        }

        rep.setAdminEventsEnabled(realm.isAdminEventsEnabled());

        rep.setAdminEventsDetailsEnabled(realm.isAdminEventsDetailsEnabled());

        return rep;
    }

    public static CredentialRepresentation toRepresentation(UserCredentialModel cred) {
        CredentialRepresentation rep = new CredentialRepresentation();
        rep.setType(CredentialRepresentation.SECRET);
        rep.setValue(cred.getChallengeResponse());
        return rep;
    }

    public static CredentialRepresentation toRepresentation(CredentialModel cred) {
        CredentialRepresentation rep = new CredentialRepresentation();
        rep.setId(cred.getId());
        rep.setType(cred.getType());
        rep.setUserLabel(cred.getUserLabel());
        rep.setCreatedDate(cred.getCreatedDate());
        rep.setSecretData(cred.getSecretData());
        rep.setCredentialData(cred.getCredentialData());
        return rep;
    }

    public static FederatedIdentityRepresentation toRepresentation(FederatedIdentityModel socialLink) {
        FederatedIdentityRepresentation rep = new FederatedIdentityRepresentation();
        rep.setUserName(socialLink.getUserName());
        rep.setIdentityProvider(socialLink.getIdentityProvider());
        rep.setUserId(socialLink.getUserId());
        return rep;
    }

    public static UserSessionRepresentation toRepresentation(UserSessionModel session) {
        UserSessionRepresentation rep = new UserSessionRepresentation();
        rep.setId(session.getId());
        rep.setStart(Time.toMillis(session.getStarted()));
        rep.setLastAccess(Time.toMillis(session.getLastSessionRefresh()));
        rep.setUsername(session.getUser().getUsername());
        rep.setUserId(session.getUser().getId());
        rep.setIpAddress(session.getIpAddress());
        for (AuthenticatedClientSessionModel clientSession : session.getAuthenticatedClientSessions().values()) {
            ClientModel client = clientSession.getClient();
            rep.getClients().put(client.getId(), client.getClientId());
        }
        return rep;
    }

    public static ClientScopeRepresentation toRepresentation(ClientScopeModel clientScopeModel) {
        ClientScopeRepresentation rep = new ClientScopeRepresentation();
        rep.setId(clientScopeModel.getId());
        rep.setName(clientScopeModel.getName());
        rep.setDescription(clientScopeModel.getDescription());
        rep.setProtocol(clientScopeModel.getProtocol());
        if (!clientScopeModel.getProtocolMappers().isEmpty()) {
            List<ProtocolMapperRepresentation> mappings = new LinkedList<>();
            for (ProtocolMapperModel model : clientScopeModel.getProtocolMappers()) {
                mappings.add(toRepresentation(model));
            }
            rep.setProtocolMappers(mappings);
        }

        rep.setAttributes(new HashMap<>(clientScopeModel.getAttributes()));

        return rep;
    }


    public static ClientRepresentation toRepresentation(ClientModel clientModel, KeycloakSession session) {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setId(clientModel.getId());
        String providerId = StorageId.resolveProviderId(clientModel);
        rep.setOrigin(providerId);
        rep.setClientId(clientModel.getClientId());
        rep.setName(clientModel.getName());
        rep.setDescription(clientModel.getDescription());
        rep.setEnabled(clientModel.isEnabled());
        rep.setAlwaysDisplayInConsole(clientModel.isAlwaysDisplayInConsole());
        rep.setAdminUrl(clientModel.getManagementUrl());
        rep.setPublicClient(clientModel.isPublicClient());
        rep.setFrontchannelLogout(clientModel.isFrontchannelLogout());
        rep.setProtocol(clientModel.getProtocol());
        rep.setAttributes(clientModel.getAttributes());
        rep.setAuthenticationFlowBindingOverrides(clientModel.getAuthenticationFlowBindingOverrides());
        rep.setFullScopeAllowed(clientModel.isFullScopeAllowed());
        rep.setBearerOnly(clientModel.isBearerOnly());
        rep.setConsentRequired(clientModel.isConsentRequired());
        rep.setStandardFlowEnabled(clientModel.isStandardFlowEnabled());
        rep.setImplicitFlowEnabled(clientModel.isImplicitFlowEnabled());
        rep.setDirectAccessGrantsEnabled(clientModel.isDirectAccessGrantsEnabled());
        rep.setServiceAccountsEnabled(clientModel.isServiceAccountsEnabled());
        rep.setSurrogateAuthRequired(clientModel.isSurrogateAuthRequired());
        rep.setRootUrl(clientModel.getRootUrl());
        rep.setBaseUrl(clientModel.getBaseUrl());
        rep.setNotBefore(clientModel.getNotBefore());
        rep.setNodeReRegistrationTimeout(clientModel.getNodeReRegistrationTimeout());
        rep.setClientAuthenticatorType(clientModel.getClientAuthenticatorType());

        rep.setDefaultClientScopes(new LinkedList<>(clientModel.getClientScopes(true, false).keySet()));
        rep.setOptionalClientScopes(new LinkedList<>(clientModel.getClientScopes(false, false).keySet()));

        Set<String> redirectUris = clientModel.getRedirectUris();
        if (redirectUris != null) {
            rep.setRedirectUris(new LinkedList<>(redirectUris));
        }

        Set<String> webOrigins = clientModel.getWebOrigins();
        if (webOrigins != null) {
            rep.setWebOrigins(new LinkedList<>(webOrigins));
        }

        if (!clientModel.getDefaultRoles().isEmpty()) {
            rep.setDefaultRoles(clientModel.getDefaultRoles().toArray(new String[0]));
        }

        if (!clientModel.getRegisteredNodes().isEmpty()) {
            rep.setRegisteredNodes(new HashMap<>(clientModel.getRegisteredNodes()));
        }

        if (!clientModel.getProtocolMappers().isEmpty()) {
            List<ProtocolMapperRepresentation> mappings = new LinkedList<>();
            for (ProtocolMapperModel model : clientModel.getProtocolMappers()) {
                mappings.add(toRepresentation(model));
            }
            rep.setProtocolMappers(mappings);
        }

        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        ResourceServer resourceServer = authorization.getStoreFactory().getResourceServerStore().findById(clientModel.getId());

        if (resourceServer != null) {
            rep.setAuthorizationServicesEnabled(true);
        }

        return rep;
    }

    public static IdentityProviderRepresentation toRepresentation(RealmModel realm, IdentityProviderModel identityProviderModel) {
        IdentityProviderRepresentation providerRep = new IdentityProviderRepresentation();

        providerRep.setInternalId(identityProviderModel.getInternalId());
        providerRep.setProviderId(identityProviderModel.getProviderId());
        providerRep.setAlias(identityProviderModel.getAlias());
        providerRep.setDisplayName(identityProviderModel.getDisplayName());
        providerRep.setEnabled(identityProviderModel.isEnabled());
        providerRep.setLinkOnly(identityProviderModel.isLinkOnly());
        providerRep.setStoreToken(identityProviderModel.isStoreToken());
        providerRep.setTrustEmail(identityProviderModel.isTrustEmail());
        providerRep.setAuthenticateByDefault(identityProviderModel.isAuthenticateByDefault());
        Map<String, String> config = new HashMap<>(identityProviderModel.getConfig());
        providerRep.setConfig(config);
        providerRep.setAddReadTokenRoleOnCreate(identityProviderModel.isAddReadTokenRoleOnCreate());

        String firstBrokerLoginFlowId = identityProviderModel.getFirstBrokerLoginFlowId();
        if (firstBrokerLoginFlowId != null) {
            AuthenticationFlowModel flow = realm.getAuthenticationFlowById(firstBrokerLoginFlowId);
            if (flow == null) {
                throw new ModelException("Couldn't find authentication flow with id " + firstBrokerLoginFlowId);
            }
            providerRep.setFirstBrokerLoginFlowAlias(flow.getAlias());
        }

        String postBrokerLoginFlowId = identityProviderModel.getPostBrokerLoginFlowId();
        if (postBrokerLoginFlowId != null) {
            AuthenticationFlowModel flow = realm.getAuthenticationFlowById(postBrokerLoginFlowId);
            if (flow == null) {
                throw new ModelException("Couldn't find authentication flow with id " + postBrokerLoginFlowId);
            }
            providerRep.setPostBrokerLoginFlowAlias(flow.getAlias());
        }

        return providerRep;
    }

    public static ProtocolMapperRepresentation toRepresentation(ProtocolMapperModel model) {
        ProtocolMapperRepresentation rep = new ProtocolMapperRepresentation();
        rep.setId(model.getId());
        rep.setProtocol(model.getProtocol());
        Map<String, String> config = new HashMap<>(model.getConfig());
        rep.setConfig(config);
        rep.setName(model.getName());
        rep.setProtocolMapper(model.getProtocolMapper());
        return rep;
    }

    public static IdentityProviderMapperRepresentation toRepresentation(IdentityProviderMapperModel model) {
        IdentityProviderMapperRepresentation rep = new IdentityProviderMapperRepresentation();
        rep.setId(model.getId());
        rep.setIdentityProviderMapper(model.getIdentityProviderMapper());
        rep.setIdentityProviderAlias(model.getIdentityProviderAlias());
        Map<String, String> config = new HashMap<>(model.getConfig());
        rep.setConfig(config);
        rep.setName(model.getName());
        return rep;
    }

    public static UserConsentRepresentation toRepresentation(UserConsentModel model) {
        String clientId = model.getClient().getClientId();

        List<String> grantedClientScopes = new LinkedList<>();
        for (ClientScopeModel clientScope : model.getGrantedClientScopes()) {
            if (clientScope instanceof ClientModel) {
                grantedClientScopes.add(((ClientModel) clientScope).getClientId());
            } else {
                grantedClientScopes.add(clientScope.getName());
            }
        }

        UserConsentRepresentation consentRep = new UserConsentRepresentation();
        consentRep.setClientId(clientId);
        consentRep.setGrantedClientScopes(grantedClientScopes);
        consentRep.setCreatedDate(model.getCreatedDate());
        consentRep.setLastUpdatedDate(model.getLastUpdatedDate());
        return consentRep;
    }

    public static AuthenticationFlowRepresentation toRepresentation(RealmModel realm, AuthenticationFlowModel model) {
        AuthenticationFlowRepresentation rep = new AuthenticationFlowRepresentation();
        rep.setId(model.getId());
        rep.setBuiltIn(model.isBuiltIn());
        rep.setTopLevel(model.isTopLevel());
        rep.setProviderId(model.getProviderId());
        rep.setAlias(model.getAlias());
        rep.setDescription(model.getDescription());
        rep.setAuthenticationExecutions(new LinkedList<>());
        for (AuthenticationExecutionModel execution : realm.getAuthenticationExecutions(model.getId())) {
            rep.getAuthenticationExecutions().add(toRepresentation(realm, execution));
        }
        return rep;

    }

    public static AuthenticationExecutionExportRepresentation toRepresentation(RealmModel realm, AuthenticationExecutionModel model) {
        AuthenticationExecutionExportRepresentation rep = new AuthenticationExecutionExportRepresentation();
        if (model.getAuthenticatorConfig() != null) {
            AuthenticatorConfigModel config = realm.getAuthenticatorConfigById(model.getAuthenticatorConfig());
            rep.setAuthenticatorConfig(config.getAlias());
        }
        rep.setAuthenticator(model.getAuthenticator());
        rep.setAutheticatorFlow(model.isAuthenticatorFlow());
        if (model.getFlowId() != null) {
            AuthenticationFlowModel flow = realm.getAuthenticationFlowById(model.getFlowId());
            rep.setFlowAlias(flow.getAlias());
        }
        rep.setPriority(model.getPriority());
        rep.setRequirement(model.getRequirement().name());
        return rep;
    }

    public static AuthenticatorConfigRepresentation toRepresentation(AuthenticatorConfigModel model) {
        AuthenticatorConfigRepresentation rep = new AuthenticatorConfigRepresentation();
        rep.setId(model.getId());
        rep.setAlias(model.getAlias());
        rep.setConfig(model.getConfig());
        return rep;
    }

    public static RequiredActionProviderRepresentation toRepresentation(RequiredActionProviderModel model) {
        RequiredActionProviderRepresentation rep = new RequiredActionProviderRepresentation();
        rep.setAlias(model.getAlias());
        rep.setDefaultAction(model.isDefaultAction());
        rep.setEnabled(model.isEnabled());
        rep.setConfig(model.getConfig());
        rep.setName(model.getName());
        rep.setProviderId(model.getProviderId());
        rep.setPriority(model.getPriority());
        return rep;
    }

    public static List<ConfigPropertyRepresentation> toRepresentation(List<ProviderConfigProperty> configProperties) {
        List<ConfigPropertyRepresentation> propertiesRep = new LinkedList<>();
        for (ProviderConfigProperty prop : configProperties) {
            ConfigPropertyRepresentation propRep = toRepresentation(prop);
            propertiesRep.add(propRep);
        }
        return propertiesRep;
    }

    public static ConfigPropertyRepresentation toRepresentation(ProviderConfigProperty prop) {
        ConfigPropertyRepresentation propRep = new ConfigPropertyRepresentation();
        propRep.setName(prop.getName());
        propRep.setLabel(prop.getLabel());
        propRep.setType(prop.getType());
        propRep.setDefaultValue(prop.getDefaultValue());
        propRep.setOptions(prop.getOptions());
        propRep.setHelpText(prop.getHelpText());
        propRep.setSecret(prop.isSecret());
        return propRep;
    }

    public static ComponentRepresentation toRepresentation(KeycloakSession session, ComponentModel component, boolean internal) {
        ComponentRepresentation rep = toRepresentationWithoutConfig(component);
        if (!internal) {
            rep = StripSecretsUtils.strip(session, rep);
        }
        return rep;
    }

    public static ComponentRepresentation toRepresentationWithoutConfig(ComponentModel component) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setId(component.getId());
        rep.setName(component.getName());
        rep.setProviderId(component.getProviderId());
        rep.setProviderType(component.getProviderType());
        rep.setSubType(component.getSubType());
        rep.setParentId(component.getParentId());
        rep.setConfig(new MultivaluedHashMap<>(component.getConfig()));
        return rep;
    }

    public static ScopeRepresentation toRepresentation(Scope model) {
        ScopeRepresentation scope = new ScopeRepresentation();

        scope.setId(model.getId());
        scope.setName(model.getName());
        scope.setDisplayName(model.getDisplayName());
        scope.setIconUri(model.getIconUri());

        return scope;
    }

    public static ResourceServerRepresentation toRepresentation(ResourceServer model, ClientModel client) {
        ResourceServerRepresentation server = new ResourceServerRepresentation();

        server.setId(model.getId());
        server.setClientId(model.getId());
        server.setName(client.getClientId());
        server.setAllowRemoteResourceManagement(model.isAllowRemoteResourceManagement());
        server.setPolicyEnforcementMode(model.getPolicyEnforcementMode());
        server.setDecisionStrategy(model.getDecisionStrategy());

        return server;
    }

    public static <R extends AbstractPolicyRepresentation> R toRepresentation(Policy policy, AuthorizationProvider authorization) {
        return toRepresentation(policy, authorization, false, true);
    }

    public static <R extends AbstractPolicyRepresentation> R toRepresentation(Policy policy, AuthorizationProvider authorization, boolean genericRepresentation, boolean export) {
        return toRepresentation(policy, authorization, genericRepresentation, export, false);
    }
    
    public static <R extends AbstractPolicyRepresentation> R toRepresentation(Policy policy, AuthorizationProvider authorization, boolean genericRepresentation, boolean export, boolean allFields) {
        PolicyProviderFactory providerFactory = authorization.getProviderFactory(policy.getType());
        R representation;

        if (genericRepresentation || export) {
            representation = (R) new PolicyRepresentation();
            PolicyRepresentation.class.cast(representation).setConfig(policy.getConfig());
            if (export) {
                providerFactory.onExport(policy, PolicyRepresentation.class.cast(representation), authorization);
            }
        } else {
            try {
                representation = (R) providerFactory.toRepresentation(policy, authorization);
            } catch (Exception cause) {
                throw new RuntimeException("Could not create policy [" + policy.getType() + "] representation", cause);
            }
        }

        representation.setId(policy.getId());
        representation.setName(policy.getName());
        representation.setDescription(policy.getDescription());
        representation.setType(policy.getType());
        representation.setDecisionStrategy(policy.getDecisionStrategy());
        representation.setLogic(policy.getLogic());
        
        if (allFields) {
            representation.setResourcesData(policy.getResources().stream().map(
                    resource -> toRepresentation(resource, resource.getResourceServer(), authorization, true)).collect(Collectors.toSet()));
            representation.setScopesData(policy.getScopes().stream().map(
                    resource -> toRepresentation(resource)).collect(Collectors.toSet()));
        }

        return representation;
    }

    public static ResourceRepresentation toRepresentation(Resource model, ResourceServer resourceServer, AuthorizationProvider authorization) {
        return toRepresentation(model, resourceServer, authorization, true);
    }

    public static ResourceRepresentation toRepresentation(Resource model, ResourceServer resourceServer, AuthorizationProvider authorization, Boolean deep) {
        ResourceRepresentation resource = new ResourceRepresentation();

        resource.setId(model.getId());
        resource.setType(model.getType());
        resource.setName(model.getName());
        resource.setDisplayName(model.getDisplayName());
        resource.setUris(model.getUris());
        resource.setIconUri(model.getIconUri());
        resource.setOwnerManagedAccess(model.isOwnerManagedAccess());

        ResourceOwnerRepresentation owner = new ResourceOwnerRepresentation();

        owner.setId(model.getOwner());

        KeycloakSession keycloakSession = authorization.getKeycloakSession();
        RealmModel realm = authorization.getRealm();

        if (owner.getId().equals(resourceServer.getId())) {
            ClientModel clientModel = realm.getClientById(resourceServer.getId());
            owner.setName(clientModel.getClientId());
        } else {
            UserModel userModel = keycloakSession.users().getUserById(owner.getId(), realm);

            if (userModel == null) {
                throw new RuntimeException("Could not find the user [" + owner.getId() + "] who owns the Resource [" + resource.getId() + "].");
            }

            owner.setName(userModel.getUsername());
        }

        resource.setOwner(owner);

        if (deep) {
            resource.setScopes(model.getScopes().stream().map(model1 -> {
                ScopeRepresentation scope = new ScopeRepresentation();
                scope.setId(model1.getId());
                scope.setName(model1.getName());
                String iconUri = model1.getIconUri();
                if (iconUri != null) {
                    scope.setIconUri(iconUri);
                }
                return scope;
            }).collect(Collectors.toSet()));

            resource.setAttributes(new HashMap<>(model.getAttributes()));
        }

        return resource;
    }

    public static PermissionTicketRepresentation toRepresentation(PermissionTicket ticket, AuthorizationProvider authorization) {
        return toRepresentation(ticket, authorization, false);
    }

    public static PermissionTicketRepresentation toRepresentation(PermissionTicket ticket, AuthorizationProvider authorization, boolean returnNames) {
        PermissionTicketRepresentation representation = new PermissionTicketRepresentation();

        representation.setId(ticket.getId());
        representation.setGranted(ticket.isGranted());
        representation.setOwner(ticket.getOwner());
        representation.setRequester(ticket.getRequester());

        Resource resource = ticket.getResource();

        representation.setResource(resource.getId());

        if (returnNames) {
            representation.setResourceName(resource.getName());
            KeycloakSession keycloakSession = authorization.getKeycloakSession();
            RealmModel realm = authorization.getRealm();
            UserModel owner = keycloakSession.users().getUserById(ticket.getOwner(), realm);
            UserModel requester = keycloakSession.users().getUserById(ticket.getRequester(), realm);
            representation.setRequesterName(requester.getUsername());
            representation.setOwnerName(owner.getUsername());
        }

        Scope scope = ticket.getScope();

        if (scope != null) {
            representation.setScope(scope.getId());
            if (returnNames) {
                representation.setScopeName(scope.getName());
            }
        }

        return representation;
    }
}
