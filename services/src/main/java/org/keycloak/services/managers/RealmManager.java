package org.keycloak.services.managers;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.OAuthClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;
import org.keycloak.representations.idm.SocialLinkRepresentation;
import org.keycloak.representations.idm.SocialMappingRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserRoleMappingRepresentation;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per request object
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmManager {
    protected static final Logger logger = Logger.getLogger(RealmManager.class);
    private static AtomicLong counter = new AtomicLong(1);

    public static String generateId() {
        return counter.getAndIncrement() + "-" + System.currentTimeMillis();
    }

    protected KeycloakSession identitySession;

    public RealmManager(KeycloakSession identitySession) {
        this.identitySession = identitySession;
    }

    public RealmModel getKeycloakAdminstrationRealm() {
        return getRealm(Constants.ADMIN_REALM);
    }

    public RealmModel getRealm(String id) {
        return identitySession.getRealm(id);
    }

    public RealmModel getRealmByName(String name) {
        return identitySession.getRealmByName(name);
    }

    public RealmModel createRealm(String name) {
        return createRealm(name, name);
    }

    public RealmModel createRealm(String id, String name) {
        if (id == null) id = generateId();
        RealmModel realm = identitySession.createRealm(id, name);
        realm.setName(name);
        realm.addRole(Constants.APPLICATION_ROLE);
        realm.addRole(Constants.IDENTITY_REQUESTER_ROLE);
        return realm;
    }

    public void generateRealmKeys(RealmModel realm) {
        KeyPair keyPair = null;
        try {
            keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        realm.setPrivateKey(keyPair.getPrivate());
        realm.setPublicKey(keyPair.getPublic());
    }

    public void updateRealm(RealmRepresentation rep, RealmModel realm) {
        if (rep.getRealm() != null) {
            logger.info("Updating realm name to " + rep.getRealm());
            realm.setName(rep.getRealm());
        }
        if (rep.isEnabled() != null) realm.setEnabled(rep.isEnabled());
        if (rep.isSocial() != null) realm.setSocial(rep.isSocial());
        if (rep.isRegistrationAllowed() != null) realm.setRegistrationAllowed(rep.isRegistrationAllowed());
        if (rep.isVerifyEmail() != null) realm.setVerifyEmail(rep.isVerifyEmail());
        if (rep.isResetPasswordAllowed() != null) realm.setResetPasswordAllowed(rep.isResetPasswordAllowed());
        if (rep.isUpdateProfileOnInitialSocialLogin() != null)
            realm.setUpdateProfileOnInitialSocialLogin(rep.isUpdateProfileOnInitialSocialLogin());
        if (rep.isSslNotRequired() != null) realm.setSslNotRequired((rep.isSslNotRequired()));
        if (rep.getAccessCodeLifespan() != null) realm.setAccessCodeLifespan(rep.getAccessCodeLifespan());
        if (rep.getAccessCodeLifespanUserAction() != null)
            realm.setAccessCodeLifespanUserAction(rep.getAccessCodeLifespanUserAction());
        if (rep.getTokenLifespan() != null) realm.setTokenLifespan(rep.getTokenLifespan());
        if (rep.getRequiredOAuthClientCredentials() != null) {
            realm.updateRequiredOAuthClientCredentials(rep.getRequiredOAuthClientCredentials());
        }
        if (rep.getRequiredCredentials() != null) {
            realm.updateRequiredCredentials(rep.getRequiredCredentials());
        }
        if (rep.getRequiredApplicationCredentials() != null) {
            realm.updateRequiredApplicationCredentials(rep.getRequiredApplicationCredentials());
        }

        realm.setPasswordPolicy(new PasswordPolicy(rep.getPasswordPolicy()));

        if (rep.getDefaultRoles() != null) {
            realm.updateDefaultRoles(rep.getDefaultRoles().toArray(new String[rep.getDefaultRoles().size()]));
        }

        if (rep.getAccountManagement() != null && rep.getAccountManagement()) {
            enableAccountManagement(realm);
        } else {
            disableAccountManagement(realm);
        }

        if (rep.getSmtpServer() != null) {
            realm.setSmtpConfig(new HashMap(rep.getSmtpServer()));
        }

        if (rep.getSocialProviders() != null) {
            realm.setSocialConfig(new HashMap(rep.getSocialProviders()));
        }

        if ("GENERATE".equals(rep.getPublicKey())) {
            generateRealmKeys(realm);
        }
    }

    public void enableAccountManagement(RealmModel realm) {
        ApplicationModel application = realm.getApplicationNameMap().get(Constants.ACCOUNT_APPLICATION);
        if (application == null) {
            application = realm.addApplication(Constants.ACCOUNT_APPLICATION);
            application.addDefaultRole(Constants.ACCOUNT_PROFILE_ROLE);
            application.addDefaultRole(Constants.ACCOUNT_MANAGE_ROLE);

            UserCredentialModel password = new UserCredentialModel();
            password.setType(UserCredentialModel.PASSWORD);
            password.setValue(UUID.randomUUID().toString()); // just a random password as we'll never access it

            realm.updateCredential(application.getApplicationUser(), password);

            RoleModel applicationRole = realm.getRole(Constants.APPLICATION_ROLE);
            realm.grantRole(application.getApplicationUser(), applicationRole);
        }
        application.setEnabled(true);
    }

    public void disableAccountManagement(RealmModel realm) {
        ApplicationModel application = realm.getApplicationNameMap().get(Constants.ACCOUNT_APPLICATION);
        if (application != null) {
            application.setEnabled(false); // TODO Should we delete the application instead?
        }
    }

    public RealmModel importRealm(RealmRepresentation rep, UserModel realmCreator) {
        String id = rep.getId();
        if (id == null) {
            id = generateId();
        }
        RealmModel realm = createRealm(id, rep.getRealm());
        importRealm(rep, realm);
        return realm;
    }


    public void importRealm(RealmRepresentation rep, RealmModel newRealm) {
        newRealm.setName(rep.getRealm());
        if (rep.isEnabled() != null) newRealm.setEnabled(rep.isEnabled());
        if (rep.isSocial() != null) newRealm.setSocial(rep.isSocial());

        if (rep.getTokenLifespan() != null) newRealm.setTokenLifespan(rep.getTokenLifespan());
        else newRealm.setTokenLifespan(300);

        if (rep.getAccessCodeLifespan() != null) newRealm.setAccessCodeLifespan(rep.getAccessCodeLifespan());
        else newRealm.setAccessCodeLifespan(60);

        if (rep.getAccessCodeLifespanUserAction() != null)
            newRealm.setAccessCodeLifespanUserAction(rep.getAccessCodeLifespanUserAction());
        else newRealm.setAccessCodeLifespanUserAction(300);

        if (rep.isSslNotRequired() != null) newRealm.setSslNotRequired(rep.isSslNotRequired());
        if (rep.isRegistrationAllowed() != null) newRealm.setRegistrationAllowed(rep.isRegistrationAllowed());
        if (rep.isVerifyEmail() != null) newRealm.setVerifyEmail(rep.isVerifyEmail());
        if (rep.isResetPasswordAllowed() != null) newRealm.setResetPasswordAllowed(rep.isResetPasswordAllowed());
        if (rep.isUpdateProfileOnInitialSocialLogin() != null)
            newRealm.setUpdateProfileOnInitialSocialLogin(rep.isUpdateProfileOnInitialSocialLogin());
        if (rep.getPrivateKey() == null || rep.getPublicKey() == null) {
            generateRealmKeys(newRealm);
        } else {
            newRealm.setPrivateKeyPem(rep.getPrivateKey());
            newRealm.setPublicKeyPem(rep.getPublicKey());
        }

        Map<String, UserModel> userMap = new HashMap<String, UserModel>();

        if (rep.getRequiredCredentials() != null) {
            for (String requiredCred : rep.getRequiredCredentials()) {
                addRequiredCredential(newRealm, requiredCred);
            }
        } else {
            addRequiredCredential(newRealm, CredentialRepresentation.PASSWORD);
        }

        if (rep.getRequiredApplicationCredentials() != null) {
            for (String requiredCred : rep.getRequiredApplicationCredentials()) {
                addResourceRequiredCredential(newRealm, requiredCred);
            }
        } else {
            addResourceRequiredCredential(newRealm, CredentialRepresentation.PASSWORD);
        }

        if (rep.getRequiredOAuthClientCredentials() != null) {
            for (String requiredCred : rep.getRequiredOAuthClientCredentials()) {
                addOAuthClientRequiredCredential(newRealm, requiredCred);
            }
        } else {
            addOAuthClientRequiredCredential(newRealm, CredentialRepresentation.PASSWORD);
        }

        newRealm.setPasswordPolicy(new PasswordPolicy(rep.getPasswordPolicy()));

        if (rep.getUsers() != null) {
            for (UserRepresentation userRep : rep.getUsers()) {
                UserModel user = createUser(newRealm, userRep);
                userMap.put(user.getLoginName(), user);
            }
        }

        if (rep.getRoles() != null) {
            for (RoleRepresentation roleRep : rep.getRoles()) {
                createRole(newRealm, roleRep);
            }
        }

        if (rep.getDefaultRoles() != null) {
            for (String roleString : rep.getDefaultRoles()) {
                newRealm.addDefaultRole(roleString.trim());
            }
        }

        if (rep.getApplications() != null) {
            Map<String, ApplicationModel> appMap = createApplications(rep, newRealm);
            for (ApplicationModel app : appMap.values()) {
                userMap.put(app.getApplicationUser().getLoginName(), app.getApplicationUser());
            }
        }
        if (rep.getOauthClients() != null) {
            Map<String, OAuthClientModel> oauthMap = createOAuthClients(rep, newRealm);
            for (OAuthClientModel app : oauthMap.values()) {
                userMap.put(app.getOAuthAgent().getLoginName(), app.getOAuthAgent());
            }

        }

        if (rep.getAccountManagement() != null && rep.getAccountManagement()) {
            enableAccountManagement(newRealm);
        }

        // Now that all possible users and applications are created (users, apps, and oauth clients), do role mappings and scope mappings

        Map<String, ApplicationModel> appMap = newRealm.getApplicationNameMap();

        if (rep.getApplicationRoleMappings() != null) {
            ApplicationManager manager = new ApplicationManager(this);
            for (Map.Entry<String, List<UserRoleMappingRepresentation>> entry : rep.getApplicationRoleMappings().entrySet()) {
                ApplicationModel app = appMap.get(entry.getKey());
                if (app == null) {
                    throw new RuntimeException("Unable to find application role mappings for app: " + entry.getKey());
                }
                manager.createRoleMappings(newRealm, app, entry.getValue());
            }
        }

        if (rep.getApplicationScopeMappings() != null) {
            ApplicationManager manager = new ApplicationManager(this);
            for (Map.Entry<String, List<ScopeMappingRepresentation>> entry : rep.getApplicationScopeMappings().entrySet()) {
                ApplicationModel app = appMap.get(entry.getKey());
                if (app == null) {
                    throw new RuntimeException("Unable to find application role mappings for app: " + entry.getKey());
                }
                manager.createScopeMappings(newRealm, app, entry.getValue());
            }
        }



        if (rep.getRoleMappings() != null) {
            for (UserRoleMappingRepresentation mapping : rep.getRoleMappings()) {
                UserModel user = userMap.get(mapping.getUsername());
                for (String roleString : mapping.getRoles()) {
                    RoleModel role = newRealm.getRole(roleString.trim());
                    if (role == null) {
                        role = newRealm.addRole(roleString.trim());
                    }
                    newRealm.grantRole(user, role);
                }
            }
        }

        if (rep.getScopeMappings() != null) {
            for (ScopeMappingRepresentation scope : rep.getScopeMappings()) {
                for (String roleString : scope.getRoles()) {
                    RoleModel role = newRealm.getRole(roleString.trim());
                    if (role == null) {
                        role = newRealm.addRole(roleString.trim());
                    }
                    UserModel user = userMap.get(scope.getUsername());
                    newRealm.addScopeMapping(user, role.getName());
                }

            }
        }

        if (rep.getSocialMappings() != null) {
            for (SocialMappingRepresentation socialMapping : rep.getSocialMappings()) {
                UserModel user = userMap.get(socialMapping.getUsername());
                for (SocialLinkRepresentation link : socialMapping.getSocialLinks()) {
                    SocialLinkModel mappingModel = new SocialLinkModel(link.getSocialProvider(), link.getSocialUsername());
                    newRealm.addSocialLink(user, mappingModel);
                }
            }
        }

        if (rep.getSmtpServer() != null) {
            newRealm.setSmtpConfig(new HashMap(rep.getSmtpServer()));
        }

        if (rep.getSocialProviders() != null) {
            newRealm.setSocialConfig(new HashMap(rep.getSocialProviders()));
        }
    }

    public void createRole(RealmModel newRealm, RoleRepresentation roleRep) {
        RoleModel role = newRealm.addRole(roleRep.getName());
        if (roleRep.getDescription() != null) role.setDescription(roleRep.getDescription());
    }

    public UserModel createUser(RealmModel newRealm, UserRepresentation userRep) {
        UserModel user = newRealm.addUser(userRep.getUsername());
        user.setEnabled(userRep.isEnabled());
        user.setEmail(userRep.getEmail());
        if (userRep.getAttributes() != null) {
            for (Map.Entry<String, String> entry : userRep.getAttributes().entrySet()) {
                user.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        if (userRep.getRequiredActions() != null) {
            for (String requiredAction : userRep.getRequiredActions()) {
                user.addRequiredAction(RequiredAction.valueOf(requiredAction));
            }
        }
        if (userRep.getCredentials() != null) {
            for (CredentialRepresentation cred : userRep.getCredentials()) {
                UserCredentialModel credential = fromRepresentation(cred);
                newRealm.updateCredential(user, credential);
            }
        }
        return user;
    }

    public static UserCredentialModel fromRepresentation(CredentialRepresentation cred) {
        UserCredentialModel credential = new UserCredentialModel();
        credential.setType(cred.getType());
        credential.setValue(cred.getValue());
        return credential;
    }

    /**
     * Query users based on a search string:
     * <p/>
     * "Bill Burke" first and last name
     * "bburke@redhat.com" email
     * "Burke" lastname or username
     *
     * @param searchString
     * @param realmModel
     * @return
     */
    public List<UserModel> searchUsers(String searchString, RealmModel realmModel) {
        if (searchString == null) {
            return Collections.emptyList();
        }
        return realmModel.searchForUser(searchString.trim());
    }

    public void addRequiredCredential(RealmModel newRealm, String requiredCred) {
        newRealm.addRequiredCredential(requiredCred);
    }

    public void addResourceRequiredCredential(RealmModel newRealm, String requiredCred) {
        newRealm.addRequiredResourceCredential(requiredCred);
    }

    public void addOAuthClientRequiredCredential(RealmModel newRealm, String requiredCred) {
        newRealm.addRequiredOAuthClientCredential(requiredCred);
    }


    protected Map<String, ApplicationModel> createApplications(RealmRepresentation rep, RealmModel realm) {
        Map<String, ApplicationModel> appMap = new HashMap<String, ApplicationModel>();
        RoleModel loginRole = realm.getRole(Constants.APPLICATION_ROLE);
        ApplicationManager manager = new ApplicationManager(this);
        for (ApplicationRepresentation resourceRep : rep.getApplications()) {
            ApplicationModel app = manager.createApplication(realm, loginRole, resourceRep);
            appMap.put(app.getName(), app);
        }
        return appMap;
    }

    protected Map<String, OAuthClientModel> createOAuthClients(RealmRepresentation realmRep, RealmModel realm) {
        Map<String, OAuthClientModel> appMap = new HashMap<String, OAuthClientModel>();
        OAuthClientManager manager = new OAuthClientManager(realm);
        for (OAuthClientRepresentation rep : realmRep.getOauthClients()) {
            OAuthClientModel app = manager.create(rep);
            appMap.put(app.getOAuthAgent().getLoginName(), app);
        }
        return appMap;
    }


    public static UserRepresentation toRepresentation(UserModel user) {
        UserRepresentation rep = new UserRepresentation();
        rep.setUsername(user.getLoginName());
        rep.setLastName(user.getLastName());
        rep.setFirstName(user.getFirstName());
        rep.setEmail(user.getEmail());
        rep.setEnabled(user.isEnabled());
        rep.setEmailVerified(user.isEmailVerified());
        rep.setTotp(user.isTotp());

        List<String> reqActions = new ArrayList<String>();
        for (RequiredAction ra : user.getRequiredActions()){
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
        return rep;
    }

    public static RealmRepresentation toRepresentation(RealmModel realm) {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setId(realm.getId());
        rep.setRealm(realm.getName());
        rep.setEnabled(realm.isEnabled());
        rep.setSocial(realm.isSocial());
        rep.setUpdateProfileOnInitialSocialLogin(realm.isUpdateProfileOnInitialSocialLogin());
        rep.setSslNotRequired(realm.isSslNotRequired());
        rep.setPublicKey(realm.getPublicKeyPem());
        rep.setPrivateKey(realm.getPrivateKeyPem());
        rep.setRegistrationAllowed(realm.isRegistrationAllowed());
        rep.setVerifyEmail(realm.isVerifyEmail());
        rep.setResetPasswordAllowed(realm.isResetPasswordAllowed());
        rep.setTokenLifespan(realm.getTokenLifespan());
        rep.setAccessCodeLifespan(realm.getAccessCodeLifespan());
        rep.setAccessCodeLifespanUserAction(realm.getAccessCodeLifespanUserAction());
        rep.setSmtpServer(realm.getSmtpConfig());
        rep.setSocialProviders(realm.getSocialConfig());
        if (realm.getPasswordPolicy() != null) {
            rep.setPasswordPolicy(realm.getPasswordPolicy().toString());
        }

        ApplicationModel accountManagementApplication = realm.getApplicationNameMap().get(Constants.ACCOUNT_APPLICATION);
        rep.setAccountManagement(accountManagementApplication != null && accountManagementApplication.isEnabled());

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
        List<RequiredCredentialModel> requiredResourceCredentialModels = realm.getRequiredApplicationCredentials();
        if (requiredResourceCredentialModels.size() > 0) {
            rep.setRequiredApplicationCredentials(new HashSet<String>());
            for (RequiredCredentialModel cred : requiredResourceCredentialModels) {
                rep.getRequiredApplicationCredentials().add(cred.getType());
            }
        }
        List<RequiredCredentialModel> requiredOAuthCredentialModels = realm.getRequiredOAuthClientCredentials();
        if (requiredOAuthCredentialModels.size() > 0) {
            rep.setRequiredOAuthClientCredentials(new HashSet<String>());
            for (RequiredCredentialModel cred : requiredOAuthCredentialModels) {
                rep.getRequiredOAuthClientCredentials().add(cred.getType());
            }
        }
        return rep;
    }


}
