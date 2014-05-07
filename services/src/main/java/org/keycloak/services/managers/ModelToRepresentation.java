package org.keycloak.services.managers;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.AuthenticationProviderModel;
import org.keycloak.models.ClaimMask;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.AuthenticationProviderRepresentation;
import org.keycloak.representations.idm.ClaimRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmAuditRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.SocialLinkRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ModelToRepresentation {
    public static UserRepresentation toRepresentation(UserModel user) {
        UserRepresentation rep = new UserRepresentation();
        rep.setId(user.getId());
        rep.setUsername(user.getLoginName());
        rep.setLastName(user.getLastName());
        rep.setFirstName(user.getFirstName());
        rep.setEmail(user.getEmail());
        rep.setEnabled(user.isEnabled());
        rep.setEmailVerified(user.isEmailVerified());
        rep.setTotp(user.isTotp());

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

    public static RealmRepresentation toRepresentation(RealmModel realm) {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setId(realm.getId());
        rep.setRealm(realm.getName());
        rep.setEnabled(realm.isEnabled());
        rep.setSocial(realm.isSocial());
        rep.setNotBefore(realm.getNotBefore());
        rep.setUpdateProfileOnInitialSocialLogin(realm.isUpdateProfileOnInitialSocialLogin());
        rep.setSslNotRequired(realm.isSslNotRequired());
        rep.setPublicKey(realm.getPublicKeyPem());
        rep.setPrivateKey(realm.getPrivateKeyPem());
        rep.setRegistrationAllowed(realm.isRegistrationAllowed());
        rep.setRememberMe(realm.isRememberMe());
        rep.setBruteForceProtected(realm.isBruteForceProtected());
        rep.setMaxFailureWaitSeconds(realm.getMaxFailureWaitSeconds());
        rep.setMinimumQuickLoginWaitSeconds(realm.getMinimumQuickLoginWaitSeconds());
        rep.setWaitIncrementSeconds(realm.getWaitIncrementSeconds());
        rep.setQuickLoginCheckMilliSeconds(realm.getQuickLoginCheckMilliSeconds());
        rep.setMaxDeltaTimeSeconds(realm.getMaxDeltaTimeSeconds());
        rep.setFailureFactor(realm.getFailureFactor());
        rep.setVerifyEmail(realm.isVerifyEmail());
        rep.setResetPasswordAllowed(realm.isResetPasswordAllowed());
        rep.setAccessTokenLifespan(realm.getAccessTokenLifespan());
        rep.setCentralLoginLifespan(realm.getCentralLoginLifespan());
        rep.setRefreshTokenLifespan(realm.getRefreshTokenLifespan());
        rep.setAccessCodeLifespan(realm.getAccessCodeLifespan());
        rep.setAccessCodeLifespanUserAction(realm.getAccessCodeLifespanUserAction());
        rep.setSmtpServer(realm.getSmtpConfig());
        rep.setSocialProviders(realm.getSocialConfig());
        rep.setLdapServer(realm.getLdapServerConfig());
        rep.setAccountTheme(realm.getAccountTheme());
        rep.setLoginTheme(realm.getLoginTheme());
        if (realm.getPasswordPolicy() != null) {
            rep.setPasswordPolicy(realm.getPasswordPolicy().toString());
        }

        ApplicationModel accountManagementApplication = realm.getApplicationNameMap().get(Constants.ACCOUNT_MANAGEMENT_APP);

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

        List<AuthenticationProviderModel> authProviderModels = realm.getAuthenticationProviders();
        if (authProviderModels.size() > 0) {
            List<AuthenticationProviderRepresentation> authProviderReps = new ArrayList<AuthenticationProviderRepresentation>();
            for (AuthenticationProviderModel model : authProviderModels) {
                AuthenticationProviderRepresentation authProvRep = new AuthenticationProviderRepresentation();
                authProvRep.setProviderName(model.getProviderName());
                authProvRep.setPasswordUpdateSupported(model.isPasswordUpdateSupported());
                authProvRep.setConfig(model.getConfig());
                authProviderReps.add(authProvRep);
            }
            rep.setAuthenticationProviders(authProviderReps);
        }
        return rep;
    }

    public static RealmAuditRepresentation toAuditReprensetation(RealmModel realm) {
        RealmAuditRepresentation rep = new RealmAuditRepresentation();
        rep.setAuditEnabled(realm.isAuditEnabled());

        if (realm.getAuditExpiration() != 0) {
            rep.setAuditExpiration(realm.getAuditExpiration());
        }

        if (realm.getAuditListeners() != null) {
            rep.setAuditListeners(new LinkedList<String>(realm.getAuditListeners()));
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

    public static SocialLinkRepresentation toRepresentation(SocialLinkModel socialLink) {
        SocialLinkRepresentation rep = new SocialLinkRepresentation();
        rep.setSocialUsername(socialLink.getSocialUsername());
        rep.setSocialProvider(socialLink.getSocialProvider());
        rep.setSocialUserId(socialLink.getSocialUserId());
        return rep;
    }
}
