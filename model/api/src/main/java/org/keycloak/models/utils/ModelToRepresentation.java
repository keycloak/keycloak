package org.keycloak.models.utils;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserConsentRepresentation;
import org.keycloak.representations.idm.UserFederationMapperRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.util.MultivaluedHashMap;
import org.keycloak.util.Time;

import java.util.ArrayList;
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
public class ModelToRepresentation {
    public static UserRepresentation toRepresentation(UserModel user) {
        UserRepresentation rep = new UserRepresentation();
        rep.setId(user.getId());
        rep.setUsername(user.getUsername());
        rep.setCreatedTimestamp(user.getCreatedTimestamp());
        rep.setLastName(user.getLastName());
        rep.setFirstName(user.getFirstName());
        rep.setEmail(user.getEmail());
        rep.setEnabled(user.isEnabled());
        rep.setEmailVerified(user.isEmailVerified());
        rep.setTotp(user.isTotp());
        rep.setFederationLink(user.getFederationLink());

        List<String> reqActions = new ArrayList<String>();
        Set<String> requiredActions = user.getRequiredActions();
        for (String ra : requiredActions){
            reqActions.add(ra);
        }

        rep.setRequiredActions(reqActions);

        if (user.getAttributes() != null && !user.getAttributes().isEmpty()) {
            Map<String, Object> attrs = new HashMap<>();
            attrs.putAll(user.getAttributes());
            rep.setAttributes(attrs);
        }
        return rep;
    }

    public static RoleRepresentation toRepresentation(RoleModel role) {
        RoleRepresentation rep = new RoleRepresentation();
        rep.setId(role.getId());
        rep.setName(role.getName());
        rep.setDescription(role.getDescription());
        rep.setComposite(role.isComposite());
        return rep;
    }

    public static RealmRepresentation toRepresentation(RealmModel realm, boolean internal) {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setId(realm.getId());
        rep.setRealm(realm.getName());
        rep.setEnabled(realm.isEnabled());
        rep.setNotBefore(realm.getNotBefore());
        rep.setSslRequired(realm.getSslRequired().name().toLowerCase());
        rep.setPublicKey(realm.getPublicKeyPem());
        if (internal) {
            rep.setPrivateKey(realm.getPrivateKeyPem());
            String privateKeyPem = realm.getPrivateKeyPem();
            if (realm.getCertificatePem() == null && privateKeyPem != null) {
                KeycloakModelUtils.generateRealmCertificate(realm);
            }
            rep.setCodeSecret(realm.getCodeSecret());
        }
        rep.setCertificate(realm.getCertificatePem());
        rep.setRegistrationAllowed(realm.isRegistrationAllowed());
        rep.setRegistrationEmailAsUsername(realm.isRegistrationEmailAsUsername());
        rep.setRememberMe(realm.isRememberMe());
        rep.setBruteForceProtected(realm.isBruteForceProtected());
        rep.setMaxFailureWaitSeconds(realm.getMaxFailureWaitSeconds());
        rep.setMinimumQuickLoginWaitSeconds(realm.getMinimumQuickLoginWaitSeconds());
        rep.setWaitIncrementSeconds(realm.getWaitIncrementSeconds());
        rep.setQuickLoginCheckMilliSeconds(realm.getQuickLoginCheckMilliSeconds());
        rep.setMaxDeltaTimeSeconds(realm.getMaxDeltaTimeSeconds());
        rep.setFailureFactor(realm.getFailureFactor());

        rep.setEventsEnabled(realm.isEventsEnabled());
        if (realm.getEventsExpiration() != 0) {
            rep.setEventsExpiration(realm.getEventsExpiration());
        }
        if (realm.getEventsListeners() != null) {
            rep.setEventsListeners(new LinkedList<String>(realm.getEventsListeners()));
        }
        if (realm.getEnabledEventTypes() != null) {
            rep.setEnabledEventTypes(new LinkedList<String>(realm.getEnabledEventTypes()));
        }

        rep.setVerifyEmail(realm.isVerifyEmail());
        rep.setResetPasswordAllowed(realm.isResetPasswordAllowed());
        rep.setEditUsernameAllowed(realm.isEditUsernameAllowed());
        rep.setAccessTokenLifespan(realm.getAccessTokenLifespan());
        rep.setSsoSessionIdleTimeout(realm.getSsoSessionIdleTimeout());
        rep.setSsoSessionMaxLifespan(realm.getSsoSessionMaxLifespan());
        rep.setAccessCodeLifespan(realm.getAccessCodeLifespan());
        rep.setAccessCodeLifespanUserAction(realm.getAccessCodeLifespanUserAction());
        rep.setAccessCodeLifespanLogin(realm.getAccessCodeLifespanLogin());
        rep.setSmtpServer(realm.getSmtpConfig());
        rep.setBrowserSecurityHeaders(realm.getBrowserSecurityHeaders());
        rep.setAccountTheme(realm.getAccountTheme());
        rep.setLoginTheme(realm.getLoginTheme());
        rep.setAdminTheme(realm.getAdminTheme());
        rep.setEmailTheme(realm.getEmailTheme());
        if (realm.getPasswordPolicy() != null) {
            rep.setPasswordPolicy(realm.getPasswordPolicy().toString());
        }

        List<String> defaultRoles = realm.getDefaultRoles();
        if (!defaultRoles.isEmpty()) {
            List<String> roleStrings = new ArrayList<String>();
            roleStrings.addAll(defaultRoles);
            rep.setDefaultRoles(roleStrings);
        }

        List<RequiredCredentialModel> requiredCredentialModels = realm.getRequiredCredentials();
        if (requiredCredentialModels.size() > 0) {
            rep.setRequiredCredentials(new HashSet<String>());
            for (RequiredCredentialModel cred : requiredCredentialModels) {
                rep.getRequiredCredentials().add(cred.getType());
            }
        }

        List<UserFederationProviderModel> fedProviderModels = realm.getUserFederationProviders();
        if (fedProviderModels.size() > 0) {
            List<UserFederationProviderRepresentation> fedProviderReps = new ArrayList<UserFederationProviderRepresentation>();
            for (UserFederationProviderModel model : fedProviderModels) {
                UserFederationProviderRepresentation fedProvRep = toRepresentation(model);
                fedProviderReps.add(fedProvRep);
            }
            rep.setUserFederationProviders(fedProviderReps);
        }

        for (UserFederationMapperModel mapper : realm.getUserFederationMappers()) {
            rep.addUserFederationMapper(toRepresentation(realm, mapper));
        }

        for (IdentityProviderModel provider : realm.getIdentityProviders()) {
            rep.addIdentityProvider(toRepresentation(provider));
        }

        for (IdentityProviderMapperModel mapper : realm.getIdentityProviderMappers()) {
            rep.addIdentityProviderMapper(toRepresentation(mapper));
        }

        rep.setInternationalizationEnabled(realm.isInternationalizationEnabled());
        rep.getSupportedLocales().addAll(realm.getSupportedLocales());
        rep.setDefaultLocale(realm.getDefaultLocale());

        return rep;
    }

    public static RealmEventsConfigRepresentation toEventsConfigReprensetation(RealmModel realm) {
        RealmEventsConfigRepresentation rep = new RealmEventsConfigRepresentation();
        rep.setEventsEnabled(realm.isEventsEnabled());

        if (realm.getEventsExpiration() != 0) {
            rep.setEventsExpiration(realm.getEventsExpiration());
        }

        if (realm.getEventsListeners() != null) {
            rep.setEventsListeners(new LinkedList<String>(realm.getEventsListeners()));
        }
        
        if(realm.getEnabledEventTypes() != null) {
            rep.setEnabledEventTypes(new LinkedList<String>(realm.getEnabledEventTypes()));
        }
        
        rep.setAdminEventsEnabled(realm.isAdminEventsEnabled());
        
        rep.setAdminEventsDetailsEnabled(realm.isAdminEventsDetailsEnabled());
        
        return rep;
    }

    public static CredentialRepresentation toRepresentation(UserCredentialModel cred) {
        CredentialRepresentation rep = new CredentialRepresentation();
        rep.setType(CredentialRepresentation.SECRET);
        rep.setValue(cred.getValue());
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
        for (ClientSessionModel clientSession : session.getClientSessions()) {
            ClientModel client = clientSession.getClient();
            rep.getClients().put(client.getId(), client.getClientId());
        }
        return rep;
    }

    public static ClientRepresentation toRepresentation(ClientModel clientModel) {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setId(clientModel.getId());
        rep.setClientId(clientModel.getClientId());
        rep.setName(clientModel.getName());
        rep.setEnabled(clientModel.isEnabled());
        rep.setAdminUrl(clientModel.getManagementUrl());
        rep.setPublicClient(clientModel.isPublicClient());
        rep.setFrontchannelLogout(clientModel.isFrontchannelLogout());
        rep.setProtocol(clientModel.getProtocol());
        rep.setAttributes(clientModel.getAttributes());
        rep.setFullScopeAllowed(clientModel.isFullScopeAllowed());
        rep.setBearerOnly(clientModel.isBearerOnly());
        rep.setConsentRequired(clientModel.isConsentRequired());
        rep.setDirectGrantsOnly(clientModel.isDirectGrantsOnly());
        rep.setSurrogateAuthRequired(clientModel.isSurrogateAuthRequired());
        rep.setBaseUrl(clientModel.getBaseUrl());
        rep.setNotBefore(clientModel.getNotBefore());
        rep.setNodeReRegistrationTimeout(clientModel.getNodeReRegistrationTimeout());

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

        return rep;
    }

    public static UserFederationProviderRepresentation toRepresentation(UserFederationProviderModel model) {
        UserFederationProviderRepresentation rep = new UserFederationProviderRepresentation();
        rep.setId(model.getId());
        rep.setConfig(model.getConfig());
        rep.setProviderName(model.getProviderName());
        rep.setPriority(model.getPriority());
        rep.setDisplayName(model.getDisplayName());
        rep.setFullSyncPeriod(model.getFullSyncPeriod());
        rep.setChangedSyncPeriod(model.getChangedSyncPeriod());
        rep.setLastSync(model.getLastSync());
        return rep;
    }

    public static UserFederationMapperRepresentation toRepresentation(RealmModel realm, UserFederationMapperModel model) {
        UserFederationMapperRepresentation rep = new UserFederationMapperRepresentation();
        rep.setId(model.getId());
        rep.setName(model.getName());
        rep.setFederationMapperType(model.getFederationMapperType());
        Map<String, String> config = new HashMap<String, String>();
        config.putAll(model.getConfig());
        rep.setConfig(config);

        UserFederationProviderModel fedProvider = KeycloakModelUtils.findUserFederationProviderById(model.getFederationProviderId(), realm);
        if (fedProvider == null) {
            throw new ModelException("Couldn't find federation provider with ID " + model.getId());
        }
        rep.setFederationProviderDisplayName(fedProvider.getDisplayName());

        return rep;
    }

    public static IdentityProviderRepresentation toRepresentation(IdentityProviderModel identityProviderModel) {
        IdentityProviderRepresentation providerRep = new IdentityProviderRepresentation();

        providerRep.setInternalId(identityProviderModel.getInternalId());
        providerRep.setProviderId(identityProviderModel.getProviderId());
        providerRep.setAlias(identityProviderModel.getAlias());
        providerRep.setEnabled(identityProviderModel.isEnabled());
        providerRep.setStoreToken(identityProviderModel.isStoreToken());
        providerRep.setUpdateProfileFirstLoginMode(identityProviderModel.getUpdateProfileFirstLoginMode());
        providerRep.setTrustEmail(identityProviderModel.isTrustEmail());
        providerRep.setAuthenticateByDefault(identityProviderModel.isAuthenticateByDefault());
        providerRep.setConfig(identityProviderModel.getConfig());
        providerRep.setAddReadTokenRoleOnCreate(identityProviderModel.isAddReadTokenRoleOnCreate());

        return providerRep;
    }

    public static ProtocolMapperRepresentation toRepresentation(ProtocolMapperModel model) {
        ProtocolMapperRepresentation rep = new ProtocolMapperRepresentation();
        rep.setId(model.getId());
        rep.setProtocol(model.getProtocol());
        Map<String, String> config = new HashMap<String, String>();
        config.putAll(model.getConfig());
        rep.setConfig(config);
        rep.setName(model.getName());
        rep.setProtocolMapper(model.getProtocolMapper());
        rep.setConsentText(model.getConsentText());
        rep.setConsentRequired(model.isConsentRequired());
        return rep;
    }

    public static IdentityProviderMapperRepresentation toRepresentation(IdentityProviderMapperModel model) {
        IdentityProviderMapperRepresentation rep = new IdentityProviderMapperRepresentation();
        rep.setId(model.getId());
        rep.setIdentityProviderMapper(model.getIdentityProviderMapper());
        rep.setIdentityProviderAlias(model.getIdentityProviderAlias());
        Map<String, String> config = new HashMap<String, String>();
        config.putAll(model.getConfig());
        rep.setConfig(config);
        rep.setName(model.getName());
        return rep;
    }

    public static UserConsentRepresentation toRepresentation(UserConsentModel model) {
        String clientId = model.getClient().getClientId();

        Map<String, List<String>> grantedProtocolMappers = new HashMap<String, List<String>>();
        for (ProtocolMapperModel protocolMapper : model.getGrantedProtocolMappers()) {
            String protocol = protocolMapper.getProtocol();
            List<String> currentProtocolMappers = grantedProtocolMappers.get(protocol);
            if (currentProtocolMappers == null) {
                currentProtocolMappers = new LinkedList<String>();
                grantedProtocolMappers.put(protocol, currentProtocolMappers);
            }
            currentProtocolMappers.add(protocolMapper.getName());
        }

        List<String> grantedRealmRoles = new LinkedList<String>();
        Map<String, List<String>> grantedClientRoles = new HashMap<String, List<String>>();
        for (RoleModel role : model.getGrantedRoles()) {
            if (role.getContainer() instanceof RealmModel) {
                grantedRealmRoles.add(role.getName());
            } else {
                ClientModel client2 = (ClientModel) role.getContainer();

                String clientId2 = client2.getClientId();
                List<String> currentClientRoles = grantedClientRoles.get(clientId2);
                if (currentClientRoles == null) {
                    currentClientRoles = new LinkedList<String>();
                    grantedClientRoles.put(clientId2, currentClientRoles);
                }
                currentClientRoles.add(role.getName());
            }
        }


        UserConsentRepresentation consentRep = new UserConsentRepresentation();
        consentRep.setClientId(clientId);
        consentRep.setGrantedProtocolMappers(grantedProtocolMappers);
        consentRep.setGrantedRealmRoles(grantedRealmRoles);
        consentRep.setGrantedClientRoles(grantedClientRoles);
        return consentRep;
    }

}
