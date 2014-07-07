package org.keycloak.exportimport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.exportimport.io.ImportReader;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.AuthenticationLinkModel;
import org.keycloak.models.AuthenticationProviderModel;
import org.keycloak.models.ClientModel;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.entities.ApplicationEntity;
import org.keycloak.models.entities.AuthenticationLinkEntity;
import org.keycloak.models.entities.AuthenticationProviderEntity;
import org.keycloak.models.entities.ClientEntity;
import org.keycloak.models.entities.CredentialEntity;
import org.keycloak.models.entities.OAuthClientEntity;
import org.keycloak.models.entities.RealmEntity;
import org.keycloak.models.entities.RequiredCredentialEntity;
import org.keycloak.models.entities.RoleEntity;
import org.keycloak.models.entities.SocialLinkEntity;
import org.keycloak.models.entities.UserEntity;
import org.keycloak.models.entities.UsernameLoginFailureEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ModelImporter {

    private static final Logger logger = Logger.getLogger(ModelImporter.class);

    private ImportReader importReader;
    private ExportImportPropertiesManager propertiesManager;

    public void importModel(KeycloakSession session, ImportReader importReader) {
        // Initialize needed objects
        this.importReader = importReader;
        this.propertiesManager = new ExportImportPropertiesManager();

        // Delete all the data from current model
        session.removeAllData();

        importRealms(session, "realms.json");
        importApplications(session, "applications.json");
        importRoles(session, "roles.json");

        // Now we have all realms,applications and roles filled. So fill other objects (default roles, scopes etc)
        importRealmsStep2(session, "realms.json");
        importApplicationsStep2(session, "applications.json");

        importOAuthClients(session, "oauthClients.json");
        importUsers(session, "users.json");
        importUserFailures(session, "userFailures.json");

        this.importReader.closeImportReader();
    }

    protected void importRealms(KeycloakSession session, String fileName) {
        List<RealmEntity> realms =  this.importReader.readEntities(fileName, RealmEntity.class);

        for (RealmEntity realmEntity : realms) {
            RealmModel realm = session.createRealm(realmEntity.getId(), realmEntity.getName());

            this.propertiesManager.setBasicPropertiesToModel(realm, realmEntity);

            Set<String> reqCredModels = new HashSet<String>();
            for (RequiredCredentialEntity requiredCredEntity : realmEntity.getRequiredCredentials()) {
                reqCredModels.add(requiredCredEntity.getType());
            }
            realm.updateRequiredCredentials(reqCredModels);

            // AdminApp and defaultRoles are set in step2

            // password policy
            realm.setPasswordPolicy(new PasswordPolicy(realmEntity.getPasswordPolicy()));

            // authentication providers
            List<AuthenticationProviderModel> authProviderModels = new ArrayList<AuthenticationProviderModel>();
            for (AuthenticationProviderEntity authProviderEntity : realmEntity.getAuthenticationProviders()) {
                AuthenticationProviderModel authProvider = new AuthenticationProviderModel();
                this.propertiesManager.setBasicPropertiesToModel(authProvider, authProviderEntity);
                authProviderModels.add(authProvider);

            }
            realm.setAuthenticationProviders(authProviderModels);
        }

        logger.infof("Realms imported: " + realms);
    }

    protected void importApplications(KeycloakSession session, String fileName) {
        List<ApplicationEntity> apps =  this.importReader.readEntities(fileName, ApplicationEntity.class);
        for (ApplicationEntity appEntity : apps) {
            RealmModel realm = session.getRealm(appEntity.getRealmId());
            ApplicationModel app = realm.addApplication(appEntity.getId(), appEntity.getName());

            this.propertiesManager.setBasicPropertiesToModel(app , appEntity);

            // scopeIds and default roles will be done in step2
        }

        logger.infof("Applications imported: " + apps);
    }

    protected void importRoles(KeycloakSession session, String fileName) {
        // helper map for composite roles
        Map<String, RoleEntity> rolesMap = new HashMap<String, RoleEntity>();

        List<RoleEntity> roles =  this.importReader.readEntities(fileName, RoleEntity.class);
        for (RoleEntity roleEntity : roles) {
            RoleModel role = null;
            if (roleEntity.getRealmId() != null) {
                RealmModel realm = session.getRealm(roleEntity.getRealmId());
                role = realm.addRole(roleEntity.getId(), roleEntity.getName());
            } else if (roleEntity.getApplicationId() != null) {
                ApplicationModel app = findApplicationById(session, roleEntity.getApplicationId());
                role = app.addRole(roleEntity.getId(), roleEntity.getName());
            } else {
                throw new IllegalStateException("Role " + roleEntity.getId() + " doesn't have realmId nor applicationId");
            }

            role.setDescription(roleEntity.getDescription());

            rolesMap.put(roleEntity.getId(), roleEntity);
        }

        // All roles were added. Fill composite roles now
        for (RealmModel realm : session.getRealms()) {

            // realm roles
            fillCompositeRoles(rolesMap, realm, realm);

            // app roles
            for (ApplicationModel app : realm.getApplications()) {
                fillCompositeRoles(rolesMap, app, realm);
            }
        }

        logger.infof("%d roles imported: ", roles.size());
        if (logger.isDebugEnabled()) {
            logger.debug("Imported roles: " + roles);
        }
    }

    private void fillCompositeRoles(Map<String, RoleEntity> rolesMap, RoleContainerModel roleContainer, RealmModel realm) {
        for (RoleModel role : roleContainer.getRoles()) {
            RoleEntity roleEntity = rolesMap.get(role.getId());

            if (roleEntity.getCompositeRoleIds() == null) {
                continue;
            }

            for (String compositeRoleId : roleEntity.getCompositeRoleIds()) {
                RoleModel compositeRole = realm.getRoleById(compositeRoleId);
                role.addCompositeRole(compositeRole);
            }
        }
    }

    protected void importRealmsStep2(KeycloakSession session, String fileName) {
        List<RealmEntity> realms =  this.importReader.readEntities(fileName, RealmEntity.class);
        RealmModel adminRealm = session.getRealm(Config.getAdminRealm());

        for (RealmEntity realmEntity : realms) {
            RealmModel realm = session.getRealm(realmEntity.getId());

            // admin app
            String adminAppId = realmEntity.getAdminAppId();
            if (adminAppId != null) {
                realm.setMasterAdminApp(adminRealm.getApplicationById(adminAppId));
            }

            // Default roles
            realm.updateDefaultRoles(realmEntity.getDefaultRoles().toArray(new String[] {}));
        }
    }

    protected void importApplicationsStep2(KeycloakSession session, String fileName) {
        List<ApplicationEntity> apps =  this.importReader.readEntities(fileName, ApplicationEntity.class);
        for (ApplicationEntity appEntity : apps) {
            RealmModel realm = session.getRealm(appEntity.getRealmId());
            ApplicationModel application = realm.getApplicationById(appEntity.getId());

            // Default roles
            application.updateDefaultRoles(appEntity.getDefaultRoles().toArray(new String[] {}));

            // Scopes
            addScopes(realm, application, appEntity);
        }
    }

    private void addScopes(RealmModel realm, ClientModel client, ClientEntity clientEntity) {
        for (String scopeId : clientEntity.getScopeIds()) {
            RoleModel scope = realm.getRoleById(scopeId);
            client.addScopeMapping(scope);
        }
    }

    protected void importOAuthClients(KeycloakSession session, String fileName) {
        List<OAuthClientEntity> clients =  this.importReader.readEntities(fileName, OAuthClientEntity.class);
        for (OAuthClientEntity clientEntity : clients) {
            RealmModel realm = session.getRealm(clientEntity.getRealmId());
            OAuthClientModel client = realm.addOAuthClient(clientEntity.getId(), clientEntity.getName());

            this.propertiesManager.setBasicPropertiesToModel(client, clientEntity);

            client.setClientId(clientEntity.getName());

            // Scopes. All roles are already added at this point
            addScopes(realm, client, clientEntity);
        }

        logger.info("OAuth clients imported: " + clients);
    }

    protected ApplicationModel findApplicationById(KeycloakSession session, String applicationId) {
        for (RealmModel realm : session.getRealms()) {
            ApplicationModel appModel = realm.getApplicationById(applicationId);
            if (appModel != null) {
                return appModel;
            }
        }

        return null;
    }

    public void importUsers(KeycloakSession session, String fileName) {
        List<UserEntity> users = this.importReader.readEntities(fileName, UserEntity.class);
        for (UserEntity userEntity : users) {
            RealmModel realm = session.getRealm(userEntity.getRealmId());
            UserModel user = realm.addUser(userEntity.getId(), userEntity.getUsername());

            // We need to remove defaultRoles here as realm.addUser is automatically adding them. We may add them later during roles mapping processing
            for (RoleModel role : user.getRoleMappings()) {
                user.deleteRoleMapping(role);
            }

            this.propertiesManager.setBasicPropertiesToModel(user, userEntity);

            // authentication links
            AuthenticationLinkEntity authLinkEntity = userEntity.getAuthenticationLink();
            if (authLinkEntity != null) {
                AuthenticationLinkModel authLinkModel = new AuthenticationLinkModel();
                this.propertiesManager.setBasicPropertiesToModel(authLinkModel, authLinkEntity);

                user.setAuthenticationLink(authLinkModel);
            }

            // social links
            List<SocialLinkEntity> socialLinks = userEntity.getSocialLinks();
            if (socialLinks != null && !socialLinks.isEmpty()) {
                for (SocialLinkEntity socialLinkEntity : socialLinks) {
                    SocialLinkModel socialLink = new SocialLinkModel();
                    this.propertiesManager.setBasicPropertiesToModel(socialLink, socialLinkEntity);

                    realm.addSocialLink(user, socialLink);
                }
            }

            // required actions
            List<UserModel.RequiredAction> requiredActions = userEntity.getRequiredActions();
            if (requiredActions != null && !requiredActions.isEmpty()) {
                for (UserModel.RequiredAction reqAction : requiredActions) {
                    user.addRequiredAction(reqAction);
                }
            }

            // attributes
            if (userEntity.getAttributes() != null) {
                for (Map.Entry<String, String> attr : userEntity.getAttributes().entrySet()) {
                    user.setAttribute(attr.getKey(), attr.getValue());
                }
            }

            // roles
            if (userEntity.getRoleIds() != null) {
                for (String roleId : userEntity.getRoleIds()) {
                    RoleModel role = realm.getRoleById(roleId);
                    user.grantRole(role);
                }
            }

            // credentials
            List<CredentialEntity> credentials = userEntity.getCredentials();
            if (credentials != null) {
                for (CredentialEntity credEntity : credentials) {
                    UserCredentialValueModel credModel = new UserCredentialValueModel();
                    this.propertiesManager.setBasicPropertiesToModel(credModel, credEntity);

                    user.updateCredentialDirectly(credModel);
                }
            }
        }

        logger.infof("%d users imported: ", users.size());
        if (logger.isDebugEnabled()) {
            logger.debug("Imported users: " + users);
        }
    }

    public void importUserFailures(KeycloakSession session, String fileName) {
        List<UsernameLoginFailureEntity> userFailures = this.importReader.readEntities(fileName, UsernameLoginFailureEntity.class);
        for (UsernameLoginFailureEntity entity : userFailures) {
            RealmModel realm = session.getRealm(entity.getRealmId());
            UsernameLoginFailureModel model = realm.addUserLoginFailure(entity.getUsername());

            this.propertiesManager.setBasicPropertiesToModel(model , entity);

            for (int i=0 ; i<entity.getNumFailures() ; i++) {
                model.incrementFailures();
            }
        }
    }
}
