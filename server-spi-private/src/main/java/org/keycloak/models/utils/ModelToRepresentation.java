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

import org.jboss.logging.Logger;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.common.Profile;
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
import org.keycloak.utils.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ModelToRepresentation {

    public static Set<String> REALM_EXCLUDED_ATTRIBUTES = new HashSet<>();
    static {
        REALM_EXCLUDED_ATTRIBUTES.add("displayName");
        REALM_EXCLUDED_ATTRIBUTES.add("displayNameHtml");
        REALM_EXCLUDED_ATTRIBUTES.add("defaultSignatureAlgorithm");
        REALM_EXCLUDED_ATTRIBUTES.add("bruteForceProtected");
        REALM_EXCLUDED_ATTRIBUTES.add("permanentLockout");
        REALM_EXCLUDED_ATTRIBUTES.add("maxFailureWaitSeconds");
        REALM_EXCLUDED_ATTRIBUTES.add("waitIncrementSeconds");
        REALM_EXCLUDED_ATTRIBUTES.add("quickLoginCheckMilliSeconds");
        REALM_EXCLUDED_ATTRIBUTES.add("minimumQuickLoginWaitSeconds");
        REALM_EXCLUDED_ATTRIBUTES.add("maxDeltaTimeSeconds");
        REALM_EXCLUDED_ATTRIBUTES.add("failureFactor");
        REALM_EXCLUDED_ATTRIBUTES.add("actionTokenGeneratedByAdminLifespan");
        REALM_EXCLUDED_ATTRIBUTES.add("actionTokenGeneratedByUserLifespan");
        REALM_EXCLUDED_ATTRIBUTES.add("offlineSessionMaxLifespanEnabled");
        REALM_EXCLUDED_ATTRIBUTES.add("offlineSessionMaxLifespan");

        REALM_EXCLUDED_ATTRIBUTES.add("webAuthnPolicyRpEntityName");
        REALM_EXCLUDED_ATTRIBUTES.add("webAuthnPolicySignatureAlgorithms");
        REALM_EXCLUDED_ATTRIBUTES.add("webAuthnPolicyRpId");
        REALM_EXCLUDED_ATTRIBUTES.add("webAuthnPolicyAttestationConveyancePreference");
        REALM_EXCLUDED_ATTRIBUTES.add("webAuthnPolicyAuthenticatorAttachment");
        REALM_EXCLUDED_ATTRIBUTES.add("webAuthnPolicyRequireResidentKey");
        REALM_EXCLUDED_ATTRIBUTES.add("webAuthnPolicyUserVerificationRequirement");
        REALM_EXCLUDED_ATTRIBUTES.add("webAuthnPolicyCreateTimeout");
        REALM_EXCLUDED_ATTRIBUTES.add("webAuthnPolicyAvoidSameAuthenticatorRegister");
        REALM_EXCLUDED_ATTRIBUTES.add("webAuthnPolicyAcceptableAaguids");

        REALM_EXCLUDED_ATTRIBUTES.add("webAuthnPolicyRpEntityNamePasswordless");
        REALM_EXCLUDED_ATTRIBUTES.add("webAuthnPolicySignatureAlgorithmsPasswordless");
        REALM_EXCLUDED_ATTRIBUTES.add("webAuthnPolicyRpIdPasswordless");
        REALM_EXCLUDED_ATTRIBUTES.add("webAuthnPolicyAttestationConveyancePreferencePasswordless");
        REALM_EXCLUDED_ATTRIBUTES.add("webAuthnPolicyAuthenticatorAttachmentPasswordless");
        REALM_EXCLUDED_ATTRIBUTES.add("webAuthnPolicyRequireResidentKeyPasswordless");
        REALM_EXCLUDED_ATTRIBUTES.add("webAuthnPolicyUserVerificationRequirementPasswordless");
        REALM_EXCLUDED_ATTRIBUTES.add("webAuthnPolicyCreateTimeoutPasswordless");
        REALM_EXCLUDED_ATTRIBUTES.add("webAuthnPolicyAvoidSameAuthenticatorRegisterPasswordless");
        REALM_EXCLUDED_ATTRIBUTES.add("webAuthnPolicyAcceptableAaguidsPasswordless");

        REALM_EXCLUDED_ATTRIBUTES.add(Constants.CLIENT_POLICIES);
        REALM_EXCLUDED_ATTRIBUTES.add(Constants.CLIENT_PROFILES);
    }

    private static final Logger LOG = Logger.getLogger(ModelToRepresentation.class);

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
        Set<RoleModel> roles = group.getRoleMappingsStream().collect(Collectors.toSet());
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

    public static Stream<GroupRepresentation> searchForGroupByName(RealmModel realm, boolean full, String search, Integer first, Integer max) {
        return realm.searchForGroupByNameStream(search, first, max)
                .map(g -> toGroupHierarchy(g, full, search));
    }

    public static Stream<GroupRepresentation> searchForGroupByName(UserModel user, boolean full, String search, Integer first, Integer max) {
        return user.getGroupsStream(search, first, max)
                .map(group -> toRepresentation(group, full));
    }

    public static Stream<GroupRepresentation> toGroupHierarchy(RealmModel realm, boolean full, Integer first, Integer max) {
        return realm.getTopLevelGroupsStream(first, max)
                .map(g -> toGroupHierarchy(g, full));
    }

    public static Stream<GroupRepresentation> toGroupHierarchy(UserModel user, boolean full, Integer first, Integer max) {
        return user.getGroupsStream(null, first, max)
                .map(group -> toRepresentation(group, full));
    }

    public static Stream<GroupRepresentation> toGroupHierarchy(RealmModel realm, boolean full) {
        return realm.getTopLevelGroupsStream()
                .map(g -> toGroupHierarchy(g, full));
    }

    public static Stream<GroupRepresentation> toGroupHierarchy(UserModel user, boolean full) {
        return user.getGroupsStream()
                .map(group -> toRepresentation(group, full));
    }

    public static GroupRepresentation toGroupHierarchy(GroupModel group, boolean full) {
        return toGroupHierarchy(group, full, null);
    }

    public static GroupRepresentation toGroupHierarchy(GroupModel group, boolean full, String search) {
        GroupRepresentation rep = toRepresentation(group, full);
        List<GroupRepresentation> subGroups = group.getSubGroupsStream()
                .filter(g -> groupMatchesSearchOrIsPathElement(g, search))
                .map(subGroup -> toGroupHierarchy(subGroup, full, search)).collect(Collectors.toList());
        rep.setSubGroups(subGroups);
        return rep;
    }

    private static boolean groupMatchesSearchOrIsPathElement(GroupModel group, String search) {
        if (StringUtil.isBlank(search)) {
            return true;
        }
        if (group.getName().contains(search)) {
            return true;
        }
        return group.getSubGroupsStream().findAny().isPresent();
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
        rep.setDisableableCredentialTypes(session.userCredentialManager()
                .getDisableableCredentialTypesStream(realm, user).collect(Collectors.toSet()));
        rep.setFederationLink(user.getFederationLink());
        rep.setNotBefore(session.users().getNotBeforeOfUser(realm, user));
        rep.setRequiredActions(user.getRequiredActionsStream().collect(Collectors.toList()));

        Map<String, List<String>> attributes = user.getAttributes();
        Map<String, List<String>> copy = null;

        if (attributes != null) {
            copy = new HashMap<>(attributes);
            copy.remove(UserModel.LAST_NAME);
            copy.remove(UserModel.FIRST_NAME);
            copy.remove(UserModel.EMAIL);
            copy.remove(UserModel.USERNAME);
        }
        if (attributes != null && !copy.isEmpty()) {
            Map<String, List<String>> attrs = new HashMap<>(copy);
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

    public static RealmRepresentation toBriefRepresentation(RealmModel realm) {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setId(realm.getId());
        rep.setRealm(realm.getName());
        rep.setDisplayName(realm.getDisplayName());
        rep.setDisplayNameHtml(realm.getDisplayNameHtml());
        rep.setEnabled(realm.isEnabled());
        return rep;
    }

    public static RealmRepresentation toRepresentation(KeycloakSession session, RealmModel realm, boolean internal) {
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
        if (Profile.isFeatureEnabled(Profile.Feature.AUTHORIZATION)) {
            rep.setUserManagedAccessAllowed(realm.isUserManagedAccessAllowed());
        } else {
            rep.setUserManagedAccessAllowed(false);
        }

        rep.setEventsEnabled(realm.isEventsEnabled());
        if (realm.getEventsExpiration() != 0) {
            rep.setEventsExpiration(realm.getEventsExpiration());
        }

        rep.setEventsListeners(realm.getEventsListenersStream().collect(Collectors.toList()));

        rep.setEnabledEventTypes(realm.getEnabledEventTypesStream().collect(Collectors.toList()));

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
        rep.setClientSessionIdleTimeout(realm.getClientSessionIdleTimeout());
        rep.setClientSessionMaxLifespan(realm.getClientSessionMaxLifespan());
        rep.setClientOfflineSessionIdleTimeout(realm.getClientOfflineSessionIdleTimeout());
        rep.setClientOfflineSessionMaxLifespan(realm.getClientOfflineSessionMaxLifespan());
        rep.setAccessCodeLifespan(realm.getAccessCodeLifespan());
        rep.setAccessCodeLifespanUserAction(realm.getAccessCodeLifespanUserAction());
        rep.setAccessCodeLifespanLogin(realm.getAccessCodeLifespanLogin());
        rep.setActionTokenGeneratedByAdminLifespan(realm.getActionTokenGeneratedByAdminLifespan());
        rep.setActionTokenGeneratedByUserLifespan(realm.getActionTokenGeneratedByUserLifespan());
        rep.setOAuth2DeviceCodeLifespan(realm.getOAuth2DeviceConfig().getLifespan());
        rep.setOAuth2DevicePollingInterval(realm.getOAuth2DeviceConfig().getPoolingInterval());
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

        CibaConfig cibaPolicy = realm.getCibaPolicy();
        Map<String, String> attrMap = Optional.ofNullable(rep.getAttributes()).orElse(new HashMap<>());
        attrMap.put(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE, cibaPolicy.getBackchannelTokenDeliveryMode());
        attrMap.put(CibaConfig.CIBA_EXPIRES_IN, String.valueOf(cibaPolicy.getExpiresIn()));
        attrMap.put(CibaConfig.CIBA_INTERVAL, String.valueOf(cibaPolicy.getPoolingInterval()));
        attrMap.put(CibaConfig.CIBA_AUTH_REQUESTED_USER_HINT, cibaPolicy.getAuthRequestedUserHint());

        ParConfig parPolicy = realm.getParPolicy();
        attrMap.put(ParConfig.PAR_REQUEST_URI_LIFESPAN, String.valueOf(parPolicy.getRequestUriLifespan()));

        rep.setAttributes(attrMap);

        if (realm.getBrowserFlow() != null) rep.setBrowserFlow(realm.getBrowserFlow().getAlias());
        if (realm.getRegistrationFlow() != null) rep.setRegistrationFlow(realm.getRegistrationFlow().getAlias());
        if (realm.getDirectGrantFlow() != null) rep.setDirectGrantFlow(realm.getDirectGrantFlow().getAlias());
        if (realm.getResetCredentialsFlow() != null) rep.setResetCredentialsFlow(realm.getResetCredentialsFlow().getAlias());
        if (realm.getClientAuthenticationFlow() != null) rep.setClientAuthenticationFlow(realm.getClientAuthenticationFlow().getAlias());
        if (realm.getDockerAuthenticationFlow() != null) rep.setDockerAuthenticationFlow(realm.getDockerAuthenticationFlow().getAlias());

        rep.setDefaultRole(toBriefRepresentation(realm.getDefaultRole()));

        List<String> defaultGroups = realm.getDefaultGroupsStream()
                .map(ModelToRepresentation::buildGroupPath).collect(Collectors.toList());
        if (!defaultGroups.isEmpty()) {
            rep.setDefaultGroups(defaultGroups);
        }

        Set<String> reqCredentials = realm.getRequiredCredentialsStream()
                .map(RequiredCredentialModel::getType)
                .collect(Collectors.toSet());
        if (!reqCredentials.isEmpty()) {
            rep.setRequiredCredentials(reqCredentials);
        }

        List<IdentityProviderRepresentation> identityProviders = realm.getIdentityProvidersStream()
                .map(provider -> toRepresentation(realm, provider)).collect(Collectors.toList());
        rep.setIdentityProviders(identityProviders);

        List<IdentityProviderMapperRepresentation> identityProviderMappers = realm.getIdentityProviderMappersStream()
                .map(ModelToRepresentation::toRepresentation).collect(Collectors.toList());
        rep.setIdentityProviderMappers(identityProviderMappers);

        rep.setInternationalizationEnabled(realm.isInternationalizationEnabled());
        rep.setSupportedLocales(realm.getSupportedLocalesStream().collect(Collectors.toSet()));
        rep.setDefaultLocale(realm.getDefaultLocale());
        if (internal) {
            exportAuthenticationFlows(realm, rep);
            exportRequiredActions(realm, rep);
            exportGroups(realm, rep);
        }

        session.clientPolicy().updateRealmRepresentationFromModel(realm, rep);

        // Append realm attributes to representation
        rep.getAttributes().putAll(stripRealmAttributesIncludedAsFields(realm.getAttributes()));

        if (!internal) {
            rep = StripSecretsUtils.strip(rep);
        }

        return rep;
    }

    public static Map<String, String> stripRealmAttributesIncludedAsFields(Map<String, String> attributes) {
        Map<String, String> a = new HashMap<>();

        for (Map.Entry<String, String> e : attributes.entrySet()) {
            if (REALM_EXCLUDED_ATTRIBUTES.contains(e.getKey())) {
                continue;
            }

            if (e.getKey().startsWith("_browser_header")) {
                continue;
            }

            a.put(e.getKey(), e.getValue());
        }

        return a;
    }

    public static void exportGroups(RealmModel realm, RealmRepresentation rep) {
        rep.setGroups(toGroupHierarchy(realm, true).collect(Collectors.toList()));
    }

    public static void exportAuthenticationFlows(RealmModel realm, RealmRepresentation rep) {
        List<AuthenticationFlowRepresentation> authenticationFlows = realm.getAuthenticationFlowsStream()
                .sorted(AuthenticationFlowModel.AuthenticationFlowComparator.SINGLETON)
                .map(flow -> toRepresentation(realm, flow))
                .collect(Collectors.toList());
        rep.setAuthenticationFlows(authenticationFlows);

        List<AuthenticatorConfigRepresentation> authenticationConfigs = realm.getAuthenticatorConfigsStream()
                .sorted(AuthenticatorConfigModel.AuthenticationConfigComparator.SINGLETON)
                .map(ModelToRepresentation::toRepresentation)
                .collect(Collectors.toList());
        rep.setAuthenticatorConfig(authenticationConfigs);
    }

    public static void exportRequiredActions(RealmModel realm, RealmRepresentation rep) {
        rep.setRequiredActions(realm.getRequiredActionProvidersStream()
                .map(ModelToRepresentation::toRepresentation).collect(Collectors.toList()));
    }

    public static RealmEventsConfigRepresentation toEventsConfigReprensetation(RealmModel realm) {
        RealmEventsConfigRepresentation rep = new RealmEventsConfigRepresentation();
        rep.setEventsEnabled(realm.isEventsEnabled());

        if (realm.getEventsExpiration() != 0) {
            rep.setEventsExpiration(realm.getEventsExpiration());
        }

        rep.setEventsListeners(realm.getEventsListenersStream().collect(Collectors.toList()));

        rep.setEnabledEventTypes(realm.getEnabledEventTypesStream().collect(Collectors.toList()));

        rep.setAdminEventsEnabled(realm.isAdminEventsEnabled());

        rep.setAdminEventsDetailsEnabled(realm.isAdminEventsDetailsEnabled());

        return rep;
    }

    /**
     * Handles exceptions that occur when transforming the model to a representation and will remove
     * all null objects from the stream.
     *
     * Entities that have been removed from the store or where a lazy loading exception occurs will not show up
     * in the output stream.
     */
    public static <M, R> Stream<R> filterValidRepresentations(Stream<M> models, Function<M, R> transformer) {
        return models.map(m -> {
                    try {
                        return transformer.apply(m);
                    } catch (ModelIllegalStateException e) {
                        LOG.warn("unable to retrieve model information, skipping entity", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull);
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
        List<ProtocolMapperRepresentation> mappings = clientScopeModel.getProtocolMappersStream()
                .map(ModelToRepresentation::toRepresentation).collect(Collectors.toList());
        if (!mappings.isEmpty())
            rep.setProtocolMappers(mappings);

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

        rep.setDefaultClientScopes(new LinkedList<>(clientModel.getClientScopes(true).keySet()));
        rep.setOptionalClientScopes(new LinkedList<>(clientModel.getClientScopes(false).keySet()));

        Set<String> redirectUris = clientModel.getRedirectUris();
        if (redirectUris != null) {
            rep.setRedirectUris(new LinkedList<>(redirectUris));
        }

        Set<String> webOrigins = clientModel.getWebOrigins();
        if (webOrigins != null) {
            rep.setWebOrigins(new LinkedList<>(webOrigins));
        }

        if (!clientModel.getRegisteredNodes().isEmpty()) {
            rep.setRegisteredNodes(new HashMap<>(clientModel.getRegisteredNodes()));
        }

        List<ProtocolMapperRepresentation> mappings = clientModel.getProtocolMappersStream()
                .map(ModelToRepresentation::toRepresentation).collect(Collectors.toList());
        if (!mappings.isEmpty())
            rep.setProtocolMappers(mappings);

        if (Profile.isFeatureEnabled(Profile.Feature.AUTHORIZATION)) {
            AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
            ResourceServer resourceServer = authorization.getStoreFactory().getResourceServerStore().findById(clientModel.getId());

            if (resourceServer != null) {
                rep.setAuthorizationServicesEnabled(true);
            }
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
        rep.setAuthenticationExecutions(realm.getAuthenticationExecutionsStream(model.getId())
                .map(e -> toRepresentation(realm, e)).collect(Collectors.toList()));
        return rep;
    }

    public static AuthenticationExecutionExportRepresentation toRepresentation(RealmModel realm, AuthenticationExecutionModel model) {
        AuthenticationExecutionExportRepresentation rep = new AuthenticationExecutionExportRepresentation();
        if (model.getAuthenticatorConfig() != null) {
            AuthenticatorConfigModel config = realm.getAuthenticatorConfigById(model.getAuthenticatorConfig());
            rep.setAuthenticatorConfig(config.getAlias());
        }
        rep.setAuthenticator(model.getAuthenticator());
        rep.setAuthenticatorFlow(model.isAuthenticatorFlow());
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

    public static ResourceRepresentation toRepresentation(Resource model, String resourceServer, AuthorizationProvider authorization) {
        return toRepresentation(model, resourceServer, authorization, true);
    }

    public static ResourceRepresentation toRepresentation(Resource model, String resourceServer, AuthorizationProvider authorization, Boolean deep) {
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

        if (owner.getId().equals(resourceServer)) {
            ClientModel clientModel = realm.getClientById(resourceServer);
            owner.setName(clientModel.getClientId());
        } else {
            UserModel userModel = keycloakSession.users().getUserById(realm, owner.getId());

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
            UserModel userOwner = keycloakSession.users().getUserById(realm, ticket.getOwner());
            UserModel requester = keycloakSession.users().getUserById(realm, ticket.getRequester());
            representation.setRequesterName(requester.getUsername());
            if (userOwner != null) {
                representation.setOwnerName(userOwner.getUsername());
            } else {
                ClientModel clientOwner = realm.getClientById(ticket.getOwner());
                representation.setOwnerName(clientOwner.getClientId());
            }
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
