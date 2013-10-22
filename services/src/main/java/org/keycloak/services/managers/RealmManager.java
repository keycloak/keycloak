package org.keycloak.services.managers;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.representations.idm.*;
import org.keycloak.models.UserModel.RequiredAction;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.*;
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

    public RealmModel createRealm(String name) {
        return createRealm(generateId(), name);
    }

    public RealmModel createRealm(String id, String name) {
        RealmModel realm = identitySession.createRealm(id, name);
        realm.setName(name);
        realm.addRole(Constants.WILDCARD_ROLE);
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
        if (rep.getRealm() != null) realm.setName(rep.getRealm());
        if (rep.isEnabled() != null) realm.setEnabled(rep.isEnabled());
        if (rep.isSocial() != null) realm.setSocial(rep.isSocial());
        if (rep.isCookieLoginAllowed() != null) realm.setCookieLoginAllowed(rep.isCookieLoginAllowed());
        if (rep.isRegistrationAllowed() != null) realm.setRegistrationAllowed(rep.isRegistrationAllowed());
        if (rep.isVerifyEmail() != null) realm.setVerifyEmail(rep.isVerifyEmail());
        if (rep.isResetPasswordAllowed() != null) realm.setResetPasswordAllowed(rep.isResetPasswordAllowed());
        if (rep.isAutomaticRegistrationAfterSocialLogin() != null)
            realm.setAutomaticRegistrationAfterSocialLogin(rep.isAutomaticRegistrationAfterSocialLogin());
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
        if (rep.getDefaultRoles() != null) {
            realm.updateDefaultRoles(rep.getDefaultRoles());
        }

        if (rep.isAccountManagement()) {
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
    }

    private void enableAccountManagement(RealmModel realm) {
        ApplicationModel application = realm.getApplicationById(Constants.ACCOUNT_MANAGEMENT_APPLICATION);
        if (application == null) {
            application = realm.addApplication(Constants.ACCOUNT_MANAGEMENT_APPLICATION);

            UserCredentialModel password = new UserCredentialModel();
            password.setType(UserCredentialModel.PASSWORD);
            password.setValue(UUID.randomUUID().toString()); // just a random password as we'll never access it

            realm.updateCredential(application.getApplicationUser(), password);

            RoleModel applicationRole = realm.getRole(Constants.APPLICATION_ROLE);
            realm.grantRole(application.getApplicationUser(), applicationRole);
        }
        application.setEnabled(true);
    }

    private void disableAccountManagement(RealmModel realm) {
        ApplicationModel application = realm.getApplicationNameMap().get(Constants.ACCOUNT_MANAGEMENT_APPLICATION);
        if (application != null) {
            application.setEnabled(false); // TODO Should we delete the application instead?
        }
    }

    public RealmModel importRealm(RealmRepresentation rep, UserModel realmCreator) {
        //verifyRealmRepresentation(rep);
        RealmModel realm = createRealm(rep.getRealm());
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
        if (rep.isCookieLoginAllowed() != null) newRealm.setCookieLoginAllowed(rep.isCookieLoginAllowed());
        if (rep.isRegistrationAllowed() != null) newRealm.setRegistrationAllowed(rep.isRegistrationAllowed());
        if (rep.isVerifyEmail() != null) newRealm.setVerifyEmail(rep.isVerifyEmail());
        if (rep.isResetPasswordAllowed() != null) newRealm.setResetPasswordAllowed(rep.isResetPasswordAllowed());
        if (rep.isAutomaticRegistrationAfterSocialLogin() != null)
            newRealm.setAutomaticRegistrationAfterSocialLogin(rep.isAutomaticRegistrationAfterSocialLogin());
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
            createApplications(rep, newRealm);
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

        if (rep.isAccountManagement() != null && rep.isAccountManagement()) {
            enableAccountManagement(newRealm);
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

        String search = searchString.trim();
        if (search.contains(" ")) { //first and last name
            String[] split = search.split(" ");
            if (split.length != 2) {
                return Collections.emptyList();
            }
            Map<String, String> attributes = new HashMap<String, String>();
            attributes.put(UserModel.FIRST_NAME, split[0]);
            attributes.put(UserModel.LAST_NAME, split[1]);
            return realmModel.searchForUserByAttributes(attributes);
        } else if (search.contains("@")) { // email
            Map<String, String> attributes = new HashMap<String, String>();
            attributes.put(UserModel.EMAIL, search);
            return realmModel.searchForUserByAttributes(attributes);
        } else { // username and lastname
            Map<String, String> attributes = new HashMap<String, String>();
            attributes.put(UserModel.LOGIN_NAME, search);
            List<UserModel> usernameQuery = realmModel.searchForUserByAttributes(attributes);
            attributes.clear();
            attributes.put(UserModel.LAST_NAME, search);
            List<UserModel> lastnameQuery = realmModel.searchForUserByAttributes(attributes);
            if (usernameQuery.size() == 0) {
                return lastnameQuery;
            } else if (lastnameQuery.size() == 0) {
                return usernameQuery;
            }
            List<UserModel> results = new ArrayList<UserModel>();
            results.addAll(usernameQuery);
            for (UserModel lastnameUser : lastnameQuery) {
                boolean found = false;
                for (UserModel usernameUser : usernameQuery) {
                    if (usernameUser.getLoginName().equals(lastnameUser.getLoginName())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    results.add(lastnameUser);
                }
            }
            return results;
        }
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


    protected void createApplications(RealmRepresentation rep, RealmModel realm) {
        RoleModel loginRole = realm.getRole(Constants.APPLICATION_ROLE);
        ApplicationManager manager = new ApplicationManager(this);
        for (ApplicationRepresentation resourceRep : rep.getApplications()) {
            manager.createApplication(realm, loginRole, resourceRep);
        }
    }

    public static UserRepresentation toRepresentation(UserModel user) {
        UserRepresentation rep = new UserRepresentation();
        rep.setUsername(user.getLoginName());
        rep.setLastName(user.getLastName());
        rep.setFirstName(user.getFirstName());
        rep.setEmail(user.getEmail());
        Map<String, String> attrs = new HashMap<String, String>();
        attrs.putAll(user.getAttributes());
        rep.setAttributes(attrs);
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
        rep.setAutomaticRegistrationAfterSocialLogin(realm.isAutomaticRegistrationAfterSocialLogin());
        rep.setSslNotRequired(realm.isSslNotRequired());
        rep.setCookieLoginAllowed(realm.isCookieLoginAllowed());
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

        ApplicationModel accountManagementApplication = realm.getApplicationNameMap().get(Constants.ACCOUNT_MANAGEMENT_APPLICATION);
        rep.setAccountManagement(accountManagementApplication != null && accountManagementApplication.isEnabled());

        List<RoleModel> defaultRoles = realm.getDefaultRoles();
        if (defaultRoles.size() > 0) {
            String[] d = new String[defaultRoles.size()];
            for (int i = 0; i < d.length; i++) {
                d[i] = defaultRoles.get(i).getName();
            }
            rep.setDefaultRoles(d);
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
