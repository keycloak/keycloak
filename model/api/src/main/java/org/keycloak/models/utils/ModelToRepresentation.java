package org.keycloak.models.utils;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClaimMask;
import org.keycloak.models.ClientIdentityProviderMappingModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.representations.idm.ClaimRepresentation;
import org.keycloak.representations.idm.ClientIdentityProviderMappingRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OAuthClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;

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
        rep.setLastName(user.getLastName());
        rep.setFirstName(user.getFirstName());
        rep.setEmail(user.getEmail());
        rep.setEnabled(user.isEnabled());
        rep.setEmailVerified(user.isEmailVerified());
        rep.setTotp(user.isTotp());
        rep.setFederationLink(user.getFederationLink());

        List<String> reqActions = new ArrayList<String>();
        for (UserModel.RequiredAction ra : user.getRequiredActions()){
            reqActions.add(ra.name());
        }

        rep.setRequiredActions(reqActions);

        if (user.getAttributes() != null && !user.getAttributes().isEmpty()) {
            Map<String, String> attrs = new HashMap<String, String>();
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
        rep.setPasswordCredentialGrantAllowed(realm.isPasswordCredentialGrantAllowed());
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

        rep.setVerifyEmail(realm.isVerifyEmail());
        rep.setResetPasswordAllowed(realm.isResetPasswordAllowed());
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

        for (IdentityProviderModel provider : realm.getIdentityProviders()) {
            rep.addIdentityProvider(toRepresentation(provider));
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
        return rep;
    }

    public static CredentialRepresentation toRepresentation(UserCredentialModel cred) {
        CredentialRepresentation rep = new CredentialRepresentation();
        rep.setType(CredentialRepresentation.SECRET);
        rep.setValue(cred.getValue());
        return rep;
    }

    public static ClaimRepresentation toRepresentation(ClientModel model) {
        ClaimRepresentation rep = new ClaimRepresentation();
        rep.setAddress(ClaimMask.hasAddress(model.getAllowedClaimsMask()));
        rep.setEmail(ClaimMask.hasEmail(model.getAllowedClaimsMask()));
        rep.setGender(ClaimMask.hasGender(model.getAllowedClaimsMask()));
        rep.setLocale(ClaimMask.hasLocale(model.getAllowedClaimsMask()));
        rep.setName(ClaimMask.hasName(model.getAllowedClaimsMask()));
        rep.setPhone(ClaimMask.hasPhone(model.getAllowedClaimsMask()));
        rep.setPicture(ClaimMask.hasPicture(model.getAllowedClaimsMask()));
        rep.setProfile(ClaimMask.hasProfile(model.getAllowedClaimsMask()));
        rep.setWebsite(ClaimMask.hasWebsite(model.getAllowedClaimsMask()));
        rep.setUsername(ClaimMask.hasUsername(model.getAllowedClaimsMask()));
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
        rep.setStart(((long)session.getStarted()) * 1000L);
        rep.setLastAccess(((long)session.getLastSessionRefresh())* 1000L);
        rep.setUser(session.getUser().getUsername());
        rep.setIpAddress(session.getIpAddress());
        for (ClientSessionModel clientSession : session.getClientSessions()) {
            ClientModel client = clientSession.getClient();
            if (client instanceof ApplicationModel) {
                rep.getApplications().put(client.getId(), client.getClientId());
            } else if (client instanceof OAuthClientModel) {
                rep.getClients().put(client.getId(), client.getClientId());
            }
        }
        return rep;
    }

    public static ApplicationRepresentation toRepresentation(ApplicationModel applicationModel) {
        ApplicationRepresentation rep = new ApplicationRepresentation();
        rep.setId(applicationModel.getId());
        rep.setName(applicationModel.getName());
        rep.setEnabled(applicationModel.isEnabled());
        rep.setAdminUrl(applicationModel.getManagementUrl());
        rep.setPublicClient(applicationModel.isPublicClient());
        rep.setFrontchannelLogout(applicationModel.isFrontchannelLogout());
        rep.setProtocol(applicationModel.getProtocol());
        rep.setAttributes(applicationModel.getAttributes());
        rep.setFullScopeAllowed(applicationModel.isFullScopeAllowed());
        rep.setBearerOnly(applicationModel.isBearerOnly());
        rep.setSurrogateAuthRequired(applicationModel.isSurrogateAuthRequired());
        rep.setBaseUrl(applicationModel.getBaseUrl());
        rep.setNotBefore(applicationModel.getNotBefore());
        rep.setNodeReRegistrationTimeout(applicationModel.getNodeReRegistrationTimeout());

        Set<String> redirectUris = applicationModel.getRedirectUris();
        if (redirectUris != null) {
            rep.setRedirectUris(new LinkedList<String>(redirectUris));
        }

        Set<String> webOrigins = applicationModel.getWebOrigins();
        if (webOrigins != null) {
            rep.setWebOrigins(new LinkedList<String>(webOrigins));
        }

        if (!applicationModel.getDefaultRoles().isEmpty()) {
            rep.setDefaultRoles(applicationModel.getDefaultRoles().toArray(new String[0]));
        }

        if (!applicationModel.getRegisteredNodes().isEmpty()) {
            rep.setRegisteredNodes(new HashMap<String, Integer>(applicationModel.getRegisteredNodes()));
        }

        if (!applicationModel.getIdentityProviders().isEmpty()) {
            rep.setIdentityProviders(toRepresentation(applicationModel.getIdentityProviders()));
        }

        if (!applicationModel.getProtocolMappers().isEmpty()) {
            List<ProtocolMapperRepresentation> mappings = new LinkedList<ProtocolMapperRepresentation>();
            for (ProtocolMapperModel model : applicationModel.getProtocolMappers()) {
                mappings.add(toRepresentation(model));
            }
            rep.setProtocolMappers(mappings);
        }

        return rep;
    }

    private static List<ClientIdentityProviderMappingRepresentation> toRepresentation(List<ClientIdentityProviderMappingModel> identityProviders) {
        ArrayList<ClientIdentityProviderMappingRepresentation> representations = new ArrayList<ClientIdentityProviderMappingRepresentation>();

        for (ClientIdentityProviderMappingModel model : identityProviders) {
            ClientIdentityProviderMappingRepresentation representation = new ClientIdentityProviderMappingRepresentation();

            representation.setId(model.getIdentityProvider());
            representation.setRetrieveToken(model.isRetrieveToken());

            representations.add(representation);
        }

        return representations;
    }

    public static OAuthClientRepresentation toRepresentation(OAuthClientModel model) {
        OAuthClientRepresentation rep = new OAuthClientRepresentation();
        rep.setId(model.getId());
        rep.setName(model.getClientId());
        rep.setEnabled(model.isEnabled());
        rep.setPublicClient(model.isPublicClient());
        rep.setFrontchannelLogout(model.isFrontchannelLogout());
        rep.setProtocol(model.getProtocol());
        rep.setAttributes(model.getAttributes());
        rep.setFullScopeAllowed(model.isFullScopeAllowed());
        rep.setDirectGrantsOnly(model.isDirectGrantsOnly());
        Set<String> redirectUris = model.getRedirectUris();
        if (redirectUris != null) {
            rep.setRedirectUris(new LinkedList<String>(redirectUris));
        }

        Set<String> webOrigins = model.getWebOrigins();
        if (webOrigins != null) {
            rep.setWebOrigins(new LinkedList<String>(webOrigins));
        }
        rep.setNotBefore(model.getNotBefore());

        if (!model.getIdentityProviders().isEmpty()) {
            rep.setIdentityProviders(toRepresentation(model.getIdentityProviders()));
        }

        if (!model.getProtocolMappers().isEmpty()) {
                List<ProtocolMapperRepresentation> mappings = new LinkedList<ProtocolMapperRepresentation>();
                for (ProtocolMapperModel mapper : model.getProtocolMappers()) {
                    mappings.add(toRepresentation(mapper));
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

    public static IdentityProviderRepresentation toRepresentation(IdentityProviderModel identityProviderModel) {
        IdentityProviderRepresentation providerRep = new IdentityProviderRepresentation();

        providerRep.setInternalId(identityProviderModel.getInternalId());
        providerRep.setProviderId(identityProviderModel.getProviderId());
        providerRep.setAlias(identityProviderModel.getAlias());
        providerRep.setEnabled(identityProviderModel.isEnabled());
        providerRep.setStoreToken(identityProviderModel.isStoreToken());
        providerRep.setUpdateProfileFirstLogin(identityProviderModel.isUpdateProfileFirstLogin());
        providerRep.setAuthenticateByDefault(identityProviderModel.isAuthenticateByDefault());
        providerRep.setConfig(identityProviderModel.getConfig());

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

}
