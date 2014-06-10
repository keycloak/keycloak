package org.keycloak.services.managers;

import org.jboss.logging.Logger;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.AuthenticationLinkModel;
import org.keycloak.models.AuthenticationProviderModel;
import org.keycloak.models.ClientModel;
import org.keycloak.Config;
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
import org.keycloak.representations.idm.AuthenticationLinkRepresentation;
import org.keycloak.representations.idm.AuthenticationProviderRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.OAuthClientRepresentation;
import org.keycloak.representations.idm.RealmAuditRepresentation;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
    protected String contextPath = "";

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

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

        // setup defaults
        setupRealmDefaults(realm);

        setupMasterAdminManagement(realm);
        setupRealmAdminManagement(realm);
        setupAccountManagement(realm);
        setupAdminConsole(realm);

        realm.setAuditListeners(Collections.singleton("jboss-logging"));

        return realm;
    }

    protected void setupAdminConsole(RealmModel realm) {
        ApplicationModel adminConsole = realm.getApplicationByName(Constants.ADMIN_CONSOLE_APPLICATION);
        if (adminConsole == null) adminConsole = new ApplicationManager(this).createApplication(realm, Constants.ADMIN_CONSOLE_APPLICATION);
        String baseUrl = contextPath + "/admin/" + realm.getName() + "/console";
        adminConsole.setBaseUrl(baseUrl + "/index.html");
        adminConsole.setEnabled(true);
        adminConsole.setPublicClient(true);
        adminConsole.addRedirectUri(baseUrl + "/*");

        RoleModel adminRole;
        if (realm.getName().equals(Config.getAdminRealm())) {
            adminRole = realm.getRole(AdminRoles.ADMIN);
        } else {
            String realmAdminApplicationName = getRealmAdminApplicationName(realm);
            ApplicationModel realmAdminApp = realm.getApplicationByName(realmAdminApplicationName);
            adminRole = realmAdminApp.getRole(AdminRoles.REALM_ADMIN);
        }
        adminConsole.addScopeMapping(adminRole);
    }

    public String getMasterRealmAdminApplicationName(RealmModel realm) {
        return realm.getName() + "-realm";
    }

    public String getRealmAdminApplicationName(RealmModel realm) {
        return "realm-management";
    }



    protected void setupRealmDefaults(RealmModel realm) {
        // brute force
        realm.setBruteForceProtected(false); // default settings off for now todo set it on
        realm.setMaxFailureWaitSeconds(900);
        realm.setMinimumQuickLoginWaitSeconds(60);
        realm.setWaitIncrementSeconds(60);
        realm.setQuickLoginCheckMilliSeconds(1000);
        realm.setMaxDeltaTimeSeconds(60 * 60 * 12); // 12 hours
        realm.setFailureFactor(30);
    }

    public boolean removeRealm(RealmModel realm) {
        boolean removed = identitySession.removeRealm(realm.getId());
        if (removed) {
            getKeycloakAdminstrationRealm().removeApplication(realm.getMasterAdminApp().getId());
        }
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
        if (rep.isBruteForceProtected() != null) realm.setBruteForceProtected(rep.isBruteForceProtected());
        if (rep.getMaxFailureWaitSeconds() != null) realm.setMaxFailureWaitSeconds(rep.getMaxFailureWaitSeconds());
        if (rep.getMinimumQuickLoginWaitSeconds() != null) realm.setMinimumQuickLoginWaitSeconds(rep.getMinimumQuickLoginWaitSeconds());
        if (rep.getWaitIncrementSeconds() != null) realm.setWaitIncrementSeconds(rep.getWaitIncrementSeconds());
        if (rep.getQuickLoginCheckMilliSeconds() != null) realm.setQuickLoginCheckMilliSeconds(rep.getQuickLoginCheckMilliSeconds());
        if (rep.getMaxDeltaTimeSeconds() != null) realm.setMaxDeltaTimeSeconds(rep.getMaxDeltaTimeSeconds());
        if (rep.getFailureFactor() != null) realm.setFailureFactor(rep.getFailureFactor());
        if (rep.isPasswordCredentialGrantAllowed() != null) realm.setPasswordCredentialGrantAllowed(rep.isPasswordCredentialGrantAllowed());
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
        if (rep.getSsoSessionIdleTimeout() != null) realm.setSsoSessionIdleTimeout(rep.getSsoSessionIdleTimeout());
        if (rep.getSsoSessionMaxLifespan() != null) realm.setSsoSessionMaxLifespan(rep.getSsoSessionMaxLifespan());
        if (rep.getRequiredCredentials() != null) {
            realm.updateRequiredCredentials(rep.getRequiredCredentials());
        }
        if (rep.getLoginTheme() != null) realm.setLoginTheme(rep.getLoginTheme());
        if (rep.getAccountTheme() != null) realm.setAccountTheme(rep.getAccountTheme());
        if (rep.getAdminTheme() != null) realm.setAdminTheme(rep.getAdminTheme());
        if (rep.getEmailTheme() != null) realm.setEmailTheme(rep.getEmailTheme());

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

        if (rep.getLdapServer() != null) {
            realm.setLdapServerConfig(new HashMap(rep.getLdapServer()));
        }
        if (rep.getAuthenticationProviders() != null) {
            List<AuthenticationProviderModel> authProviderModels = convertAuthenticationProviders(rep.getAuthenticationProviders());
            realm.setAuthenticationProviders(authProviderModels);
        }

        if ("GENERATE".equals(rep.getPublicKey())) {
            generateRealmKeys(realm);
        }
    }

    public void updateRealmAudit(RealmAuditRepresentation rep, RealmModel realm) {
        realm.setAuditEnabled(rep.isAuditEnabled());
        realm.setAuditExpiration(rep.getAuditExpiration() != null ? rep.getAuditExpiration() : 0);
        if (rep.getAuditListeners() != null) {
            realm.setAuditListeners(new HashSet<String>(rep.getAuditListeners()));
        }
    }

    private void setupMasterAdminManagement(RealmModel realm) {
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

        ApplicationModel realmAdminApp = applicationManager.createApplication(adminRealm, getMasterRealmAdminApplicationName(realm));
        realmAdminApp.setBearerOnly(true);
        realm.setMasterAdminApp(realmAdminApp);

        for (String r : AdminRoles.ALL_REALM_ROLES) {
            RoleModel role = realmAdminApp.addRole(r);
            adminRole.addCompositeRole(role);
        }
    }

    private void setupRealmAdminManagement(RealmModel realm) {
        if (realm.getName().equals(Config.getAdminRealm())) { return; } // don't need to do this for master realm

        ApplicationManager applicationManager = new ApplicationManager(new RealmManager(identitySession));

        String realmAdminApplicationName = getRealmAdminApplicationName(realm);
        ApplicationModel realmAdminApp = realm.getApplicationByName(realmAdminApplicationName);
        if (realmAdminApp == null) {
            realmAdminApp = applicationManager.createApplication(realm, realmAdminApplicationName);
        }
        RoleModel adminRole = realmAdminApp.addRole(AdminRoles.REALM_ADMIN);
        realmAdminApp.setBearerOnly(true);

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
            String base = contextPath + "/realms/" + realm.getName() + "/account";
            String redirectUri = base + "/*";
            application.addRedirectUri(redirectUri);
            application.setBaseUrl(base);

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
        if (rep.isBruteForceProtected() != null) newRealm.setBruteForceProtected(rep.isBruteForceProtected());
        if (rep.getMaxFailureWaitSeconds() != null) newRealm.setMaxFailureWaitSeconds(rep.getMaxFailureWaitSeconds());
        if (rep.getMinimumQuickLoginWaitSeconds() != null) newRealm.setMinimumQuickLoginWaitSeconds(rep.getMinimumQuickLoginWaitSeconds());
        if (rep.getWaitIncrementSeconds() != null) newRealm.setWaitIncrementSeconds(rep.getWaitIncrementSeconds());
        if (rep.getQuickLoginCheckMilliSeconds() != null) newRealm.setQuickLoginCheckMilliSeconds(rep.getQuickLoginCheckMilliSeconds());
        if (rep.getMaxDeltaTimeSeconds() != null) newRealm.setMaxDeltaTimeSeconds(rep.getMaxDeltaTimeSeconds());
        if (rep.getFailureFactor() != null) newRealm.setFailureFactor(rep.getFailureFactor());

        if (rep.getNotBefore() != null) newRealm.setNotBefore(rep.getNotBefore());

        if (rep.getAccessTokenLifespan() != null) newRealm.setAccessTokenLifespan(rep.getAccessTokenLifespan());
        else newRealm.setAccessTokenLifespan(300);

        if (rep.getSsoSessionIdleTimeout() != null) newRealm.setSsoSessionIdleTimeout(rep.getSsoSessionIdleTimeout());
        else newRealm.setSsoSessionIdleTimeout(600);
        if (rep.getSsoSessionMaxLifespan() != null) newRealm.setSsoSessionMaxLifespan(rep.getSsoSessionMaxLifespan());
        else newRealm.setSsoSessionMaxLifespan(36000);

        if (rep.getAccessCodeLifespan() != null) newRealm.setAccessCodeLifespan(rep.getAccessCodeLifespan());
        else newRealm.setAccessCodeLifespan(60);

        if (rep.getAccessCodeLifespanUserAction() != null)
            newRealm.setAccessCodeLifespanUserAction(rep.getAccessCodeLifespanUserAction());
        else newRealm.setAccessCodeLifespanUserAction(300);

        if (rep.isSslNotRequired() != null) newRealm.setSslNotRequired(rep.isSslNotRequired());
        if (rep.isPasswordCredentialGrantAllowed() != null) newRealm.setPasswordCredentialGrantAllowed(rep.isPasswordCredentialGrantAllowed());
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
        if (rep.getAdminTheme() != null) newRealm.setAdminTheme(rep.getAdminTheme());
        if (rep.getEmailTheme() != null) newRealm.setEmailTheme(rep.getEmailTheme());

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
                    user.grantRole(role);
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
                    client.addScopeMapping(role);
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
        if (rep.getLdapServer() != null) {
            newRealm.setLdapServerConfig(new HashMap(rep.getLdapServer()));
        }

        if (rep.getAuthenticationProviders() != null) {
            List<AuthenticationProviderModel> authProviderModels = convertAuthenticationProviders(rep.getAuthenticationProviders());
            newRealm.setAuthenticationProviders(authProviderModels);
        }  else {
            List<AuthenticationProviderModel> authProviderModels = Arrays.asList(AuthenticationProviderModel.DEFAULT_PROVIDER);
            newRealm.setAuthenticationProviders(authProviderModels);
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
                user.updateCredential(credential);
            }
        }
        if (userRep.getAuthenticationLink() != null) {
            AuthenticationLinkRepresentation link = userRep.getAuthenticationLink();
            AuthenticationLinkModel authLink = new AuthenticationLinkModel(link.getAuthProvider(), link.getAuthUserId());
            newRealm.setAuthenticationLink(user, authLink);
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

    protected List<AuthenticationProviderModel> convertAuthenticationProviders(List<AuthenticationProviderRepresentation> authenticationProviders) {
        List<AuthenticationProviderModel> result = new ArrayList<AuthenticationProviderModel>();

        for (AuthenticationProviderRepresentation representation : authenticationProviders) {
            AuthenticationProviderModel model = new AuthenticationProviderModel(representation.getProviderName(),
                    representation.isPasswordUpdateSupported(), representation.getConfig());
            result.add(model);
        }
        return result;
    }


}
