package org.keycloak.services.managers;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Config;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.utils.KeycloakModelUtils;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per request object
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmManager {
    protected static final Logger logger = Logger.getLogger(RealmManager.class);

    protected KeycloakSession identitySession;

    public RealmManager(KeycloakSession identitySession) {
        this.identitySession = identitySession;
    }

    public RealmModel getKeycloakAdminstrationRealm() {
        return getRealm(Config.getAdminRealm());
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
        if (id == null) id = KeycloakModelUtils.generateId();
        RealmModel realm = identitySession.createRealm(id, name);
        realm.setName(name);

        setupAdminManagement(realm);
        setupAccountManagement(realm);

        return realm;
    }

    public boolean removeRealm(RealmModel realm) {
        boolean removed = identitySession.removeRealm(realm.getId());

        RealmModel adminRealm = getKeycloakAdminstrationRealm();
        RoleModel adminRole = adminRealm.getRole(AdminRoles.ADMIN);

        ApplicationModel realmAdminApp = adminRealm.getApplicationByName(AdminRoles.getAdminApp(realm));
        for (RoleModel r : realmAdminApp.getRoles()) {
            adminRole.removeCompositeRole(r);
        }

        adminRealm.removeApplication(realmAdminApp.getId());

        return removed;
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
            realm.setName(rep.getRealm());
        }
        if (rep.isEnabled() != null) realm.setEnabled(rep.isEnabled());
        if (rep.isSocial() != null) realm.setSocial(rep.isSocial());
        if (rep.isRegistrationAllowed() != null) realm.setRegistrationAllowed(rep.isRegistrationAllowed());
        if (rep.isRememberMe() != null) realm.setRememberMe(rep.isRememberMe());
        if (rep.isVerifyEmail() != null) realm.setVerifyEmail(rep.isVerifyEmail());
        if (rep.isResetPasswordAllowed() != null) realm.setResetPasswordAllowed(rep.isResetPasswordAllowed());
        if (rep.isUpdateProfileOnInitialSocialLogin() != null)
            realm.setUpdateProfileOnInitialSocialLogin(rep.isUpdateProfileOnInitialSocialLogin());
        if (rep.isSslNotRequired() != null) realm.setSslNotRequired((rep.isSslNotRequired()));
        if (rep.getAccessCodeLifespan() != null) realm.setAccessCodeLifespan(rep.getAccessCodeLifespan());
        if (rep.getAccessCodeLifespanUserAction() != null)
            realm.setAccessCodeLifespanUserAction(rep.getAccessCodeLifespanUserAction());
        if (rep.getNotBefore() != null) realm.setNotBefore(rep.getNotBefore());
        if (rep.getAccessTokenLifespan() != null) realm.setAccessTokenLifespan(rep.getAccessTokenLifespan());
        if (rep.getRefreshTokenLifespan() != null) realm.setRefreshTokenLifespan(rep.getRefreshTokenLifespan());
        if (rep.getCentralLoginLifespan() != null) realm.setCentralLoginLifespan(rep.getCentralLoginLifespan());
        if (rep.getRequiredCredentials() != null) {
            realm.updateRequiredCredentials(rep.getRequiredCredentials());
        }
        if (rep.getLoginTheme() != null) realm.setLoginTheme(rep.getLoginTheme());
        if (rep.getAccountTheme() != null) realm.setAccountTheme(rep.getAccountTheme());

        if (rep.getPasswordPolicy() != null) realm.setPasswordPolicy(new PasswordPolicy(rep.getPasswordPolicy()));

        if (rep.getDefaultRoles() != null) {
            realm.updateDefaultRoles(rep.getDefaultRoles().toArray(new String[rep.getDefaultRoles().size()]));
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

    private void setupAdminManagement(RealmModel realm) {
        RealmModel adminRealm;
        RoleModel adminRole;

        if (realm.getName().equals(Config.getAdminRealm())) {
            adminRealm = realm;

            adminRole = realm.addRole(AdminRoles.ADMIN);

            RoleModel createRealmRole = realm.addRole(AdminRoles.CREATE_REALM);
            adminRole.addCompositeRole(createRealmRole);
        } else {
            adminRealm = identitySession.getRealmByName(Config.getAdminRealm());
            adminRole = adminRealm.getRole(AdminRoles.ADMIN);
        }

        ApplicationManager applicationManager = new ApplicationManager(new RealmManager(identitySession));
        ApplicationModel realmAdminApp = applicationManager.createApplication(adminRealm, AdminRoles.getAdminApp(realm));

        for (String r : AdminRoles.ALL_REALM_ROLES) {
            RoleModel role = realmAdminApp.addRole(r);
            adminRole.addCompositeRole(role);
        }
    }

    private void setupAccountManagement(RealmModel realm) {
        ApplicationModel application = realm.getApplicationNameMap().get(Constants.ACCOUNT_MANAGEMENT_APP);
        if (application == null) {
            application = new ApplicationManager(this).createApplication(realm, Constants.ACCOUNT_MANAGEMENT_APP);
            application.setEnabled(true);

            for (String role : AccountRoles.ALL) {
                application.addDefaultRole(role);
            }
        }
    }

    public RealmModel importRealm(RealmRepresentation rep) {
        String id = rep.getId();
        if (id == null) {
            id = KeycloakModelUtils.generateId();
        }
        RealmModel realm = createRealm(id, rep.getRealm());
        importRealm(rep, realm);
        return realm;
    }

    public void importRealm(RealmRepresentation rep, RealmModel newRealm) {
        newRealm.setName(rep.getRealm());
        if (rep.isEnabled() != null) newRealm.setEnabled(rep.isEnabled());
        if (rep.isSocial() != null) newRealm.setSocial(rep.isSocial());

        if (rep.getNotBefore() != null) newRealm.setNotBefore(rep.getNotBefore());

        if (rep.getAccessTokenLifespan() != null) newRealm.setAccessTokenLifespan(rep.getAccessTokenLifespan());
        else newRealm.setAccessTokenLifespan(300);

        if (rep.getRefreshTokenLifespan() != null) newRealm.setRefreshTokenLifespan(rep.getRefreshTokenLifespan());
        else newRealm.setRefreshTokenLifespan(36000);
        if (rep.getCentralLoginLifespan() != null) newRealm.setCentralLoginLifespan(rep.getCentralLoginLifespan());
        else newRealm.setCentralLoginLifespan(300);

        if (rep.getAccessCodeLifespan() != null) newRealm.setAccessCodeLifespan(rep.getAccessCodeLifespan());
        else newRealm.setAccessCodeLifespan(60);

        if (rep.getAccessCodeLifespanUserAction() != null)
            newRealm.setAccessCodeLifespanUserAction(rep.getAccessCodeLifespanUserAction());
        else newRealm.setAccessCodeLifespanUserAction(300);

        if (rep.isSslNotRequired() != null) newRealm.setSslNotRequired(rep.isSslNotRequired());
        if (rep.isRegistrationAllowed() != null) newRealm.setRegistrationAllowed(rep.isRegistrationAllowed());
        if (rep.isRememberMe() != null) newRealm.setRememberMe(rep.isRememberMe());
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
        if (rep.getLoginTheme() != null) newRealm.setLoginTheme(rep.getLoginTheme());
        if (rep.getAccountTheme() != null) newRealm.setAccountTheme(rep.getAccountTheme());

        Map<String, UserModel> userMap = new HashMap<String, UserModel>();

        if (rep.getRequiredCredentials() != null) {
            for (String requiredCred : rep.getRequiredCredentials()) {
                addRequiredCredential(newRealm, requiredCred);
            }
        } else {
            addRequiredCredential(newRealm, CredentialRepresentation.PASSWORD);
        }

        if (rep.getPasswordPolicy() != null) newRealm.setPasswordPolicy(new PasswordPolicy(rep.getPasswordPolicy()));

        if (rep.getUsers() != null) {
            for (UserRepresentation userRep : rep.getUsers()) {
                UserModel user = createUser(newRealm, userRep);
                userMap.put(user.getLoginName(), user);
            }
        }

        if (rep.getApplications() != null) {
            Map<String, ApplicationModel> appMap = createApplications(rep, newRealm);
        }

        if (rep.getRoles() != null) {
            if (rep.getRoles().getRealm() != null) { // realm roles
                for (RoleRepresentation roleRep : rep.getRoles().getRealm()) {
                    createRole(newRealm, roleRep);
                }
            }
            if (rep.getRoles().getApplication() != null) {
                for (Map.Entry<String, List<RoleRepresentation>> entry : rep.getRoles().getApplication().entrySet()) {
                    ApplicationModel app = newRealm.getApplicationByName(entry.getKey());
                    if (app == null) {
                        throw new RuntimeException("App doesn't exist in role definitions: " + entry.getKey());
                    }
                    for (RoleRepresentation roleRep : entry.getValue()) {
                        RoleModel role = app.addRole(roleRep.getName());
                        role.setDescription(roleRep.getDescription());
                    }
                }
            }
            // now that all roles are created, re-iterate and set up composites
            if (rep.getRoles().getRealm() != null) { // realm roles
                for (RoleRepresentation roleRep : rep.getRoles().getRealm()) {
                    RoleModel role = newRealm.getRole(roleRep.getName());
                    addComposites(role, roleRep, newRealm);
                }
            }
            if (rep.getRoles().getApplication() != null) {
                for (Map.Entry<String, List<RoleRepresentation>> entry : rep.getRoles().getApplication().entrySet()) {
                    ApplicationModel app = newRealm.getApplicationByName(entry.getKey());
                    if (app == null) {
                        throw new RuntimeException("App doesn't exist in role definitions: " + entry.getKey());
                    }
                    for (RoleRepresentation roleRep : entry.getValue()) {
                        RoleModel role = app.getRole(roleRep.getName());
                        addComposites(role, roleRep, newRealm);
                    }
                }
            }
        }


        if (rep.getDefaultRoles() != null) {
            for (String roleString : rep.getDefaultRoles()) {
                newRealm.addDefaultRole(roleString.trim());
            }
        }

        if (rep.getOauthClients() != null) {
            createOAuthClients(rep, newRealm);
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
                    ClientModel client = newRealm.findClient(scope.getClient());
                    newRealm.addScopeMapping(client, role);
                }

            }
        }

        if (rep.getSocialMappings() != null) {
            for (SocialMappingRepresentation socialMapping : rep.getSocialMappings()) {
                UserModel user = userMap.get(socialMapping.getUsername());
                for (SocialLinkRepresentation link : socialMapping.getSocialLinks()) {
                    SocialLinkModel mappingModel = new SocialLinkModel(link.getSocialProvider(), link.getSocialUserId(), link.getSocialUsername());
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

    public void addComposites(RoleModel role, RoleRepresentation roleRep, RealmModel realm) {
        if (roleRep.getComposites() == null) return;
        if (roleRep.getComposites().getRealm() != null) {
            for (String roleStr : roleRep.getComposites().getRealm()) {
                RoleModel realmRole = realm.getRole(roleStr);
                if (realmRole == null) throw new RuntimeException("Unable to find composite realm role: " + roleStr);
                role.addCompositeRole(realmRole);
            }
        }
        if (roleRep.getComposites().getApplication() != null) {
            for (Map.Entry<String, List<String>> entry : roleRep.getComposites().getApplication().entrySet()) {
                ApplicationModel app = realm.getApplicationByName(entry.getKey());
                if (app == null) {
                    throw new RuntimeException("App doesn't exist in role definitions: " + roleRep.getName());
                }
                for (String roleStr : entry.getValue()) {
                    RoleModel appRole = app.getRole(roleStr);
                    if (appRole == null) throw new RuntimeException("Unable to find composite app role: " + roleStr);
                    role.addCompositeRole(appRole);
                }

            }

        }

    }

    public void createRole(RealmModel newRealm, RoleRepresentation roleRep) {
        RoleModel role = newRealm.addRole(roleRep.getName());
        if (roleRep.getDescription() != null) role.setDescription(roleRep.getDescription());
    }

    public void createRole(RealmModel newRealm, ApplicationModel app, RoleRepresentation roleRep) {
        RoleModel role = app.addRole(roleRep.getName());
        if (roleRep.getDescription() != null) role.setDescription(roleRep.getDescription());
    }


    public UserModel createUser(RealmModel newRealm, UserRepresentation userRep) {
        UserModel user = newRealm.addUser(userRep.getUsername());
        user.setEnabled(userRep.isEnabled());
        user.setEmail(userRep.getEmail());
        user.setFirstName(userRep.getFirstName());
        user.setLastName(userRep.getLastName());
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

    protected Map<String, ApplicationModel> createApplications(RealmRepresentation rep, RealmModel realm) {
        Map<String, ApplicationModel> appMap = new HashMap<String, ApplicationModel>();
        ApplicationManager manager = new ApplicationManager(this);
        for (ApplicationRepresentation resourceRep : rep.getApplications()) {
            ApplicationModel app = manager.createApplication(realm, resourceRep);
            appMap.put(app.getName(), app);
        }
        return appMap;
    }

    protected void createOAuthClients(RealmRepresentation realmRep, RealmModel realm) {
        OAuthClientManager manager = new OAuthClientManager(realm);
        for (OAuthClientRepresentation rep : realmRep.getOauthClients()) {
            OAuthClientModel app = manager.create(rep);
        }
    }


}
