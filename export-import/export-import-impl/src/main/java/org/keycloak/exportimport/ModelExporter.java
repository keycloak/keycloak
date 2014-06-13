package org.keycloak.exportimport;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.exportimport.io.ExportWriter;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.AuthenticationLinkModel;
import org.keycloak.models.AuthenticationProviderModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.entities.ApplicationEntity;
import org.keycloak.models.entities.AuthenticationLinkEntity;
import org.keycloak.models.entities.AuthenticationProviderEntity;
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
public class ModelExporter {

    private static final Logger logger = Logger.getLogger(ModelExporter.class);

    private ExportWriter exportWriter;
    private ExportImportPropertiesManager propertiesManager;

    public void exportModel(KeycloakSession keycloakSession, ExportWriter exportWriter) {
        // Initialize needed objects
        this.exportWriter = exportWriter;
        this.propertiesManager = new ExportImportPropertiesManager();

        // Create separate files for "realms", "applications", "oauthClients", "roles" and finally "users". Users may be done in more files (pagination)
        exportRealms(keycloakSession, "realms.json");
        exportApplications(keycloakSession, "applications.json");
        exportOAuthClients(keycloakSession, "oauthClients.json");
        exportRoles(keycloakSession, "roles.json");
        exportUsers(keycloakSession, "users.json");
        exportUserFailures(keycloakSession, "userFailures.json");

        this.exportWriter.closeExportWriter();
    }

    protected void exportRealms(KeycloakSession keycloakSession, String fileName) {
        List<RealmModel> realms = keycloakSession.getRealms();

        // Convert models to entities, which will be written into JSON file
        List<RealmEntity> result = new LinkedList<RealmEntity>();
        for (RealmModel realmModel : realms) {
            RealmEntity entity = new RealmEntity();
            entity.setId(realmModel.getId());
            result.add(entity);

            // Export all basic properties from realm
            this.propertiesManager.setBasicPropertiesFromModel(realmModel, entity);

            // Export 'advanced' properties
            ApplicationModel realmAdminApp = realmModel.getMasterAdminApp();
            if (realmAdminApp != null) {
                entity.setAdminAppId(realmAdminApp.getId());
            }
            entity.setDefaultRoles(realmModel.getDefaultRoles());

            List<RequiredCredentialEntity> reqCredEntities = new ArrayList<RequiredCredentialEntity>();
            List<RequiredCredentialModel> requiredCredModels = realmModel.getRequiredCredentials();
            for (RequiredCredentialModel requiredCredModel : requiredCredModels) {
                RequiredCredentialEntity reqCredEntity = new RequiredCredentialEntity();
                this.propertiesManager.setBasicPropertiesFromModel(requiredCredModel, reqCredEntity);
                reqCredEntities.add(reqCredEntity);
            }
            entity.setRequiredCredentials(reqCredEntities);

            // password policy
            entity.setPasswordPolicy(realmModel.getPasswordPolicy().toString());

            // authentication providers
            List<AuthenticationProviderEntity> authProviderEntities = new ArrayList<AuthenticationProviderEntity>();
            for (AuthenticationProviderModel authProvider : realmModel.getAuthenticationProviders()) {
                AuthenticationProviderEntity authProviderEntity = new AuthenticationProviderEntity();
                this.propertiesManager.setBasicPropertiesFromModel(authProvider, authProviderEntity);
                authProviderEntities.add(authProviderEntity);

            }
            entity.setAuthenticationProviders(authProviderEntities);
        }

        this.exportWriter.writeEntities(fileName, result);
        logger.infof("Realms exported: " + result);
    }

    protected void exportApplications(KeycloakSession keycloakSession, String fileName) {
        List<ApplicationModel> allApplications = getAllApplications(keycloakSession);

        List<ApplicationEntity> result = new LinkedList<ApplicationEntity>();
        for (ApplicationModel appModel : allApplications) {
            ApplicationEntity appEntity = new ApplicationEntity();
            appEntity.setId(appModel.getId());
            result.add(appEntity);

            this.propertiesManager.setBasicPropertiesFromModel(appModel, appEntity);

            // Export 'advanced' properties of application
            appEntity.setRealmId(appModel.getRealm().getId());
            appEntity.setDefaultRoles(appModel.getDefaultRoles());

            List<String> scopeIds = getScopeIds(appModel);
            appEntity.setScopeIds(scopeIds);
        }

        this.exportWriter.writeEntities(fileName, result);
        logger.infof("Applications exported: " + result);
    }

    protected void exportOAuthClients(KeycloakSession keycloakSession, String fileName) {
        List<RealmModel> realms = keycloakSession.getRealms();
        List<OAuthClientModel> allClients = new ArrayList<OAuthClientModel>();
        for (RealmModel realmModel : realms) {
            allClients.addAll(realmModel.getOAuthClients());
        }

        List<OAuthClientEntity> result = new LinkedList<OAuthClientEntity>();
        for (OAuthClientModel clientModel : allClients) {
            OAuthClientEntity clientEntity = new OAuthClientEntity();
            clientEntity.setId(clientModel.getId());
            result.add(clientEntity);

            this.propertiesManager.setBasicPropertiesFromModel(clientModel, clientEntity);

            // Export 'advanced' properties of client
            clientEntity.setName(clientModel.getClientId());
            clientEntity.setRealmId(clientModel.getRealm().getId());

            List<String> scopeIds = getScopeIds(clientModel);
            clientEntity.setScopeIds(scopeIds);
        }

        this.exportWriter.writeEntities(fileName, result);
        logger.infof("OAuth clients exported: " + result);
    }

    protected void exportRoles(KeycloakSession keycloakSession, String fileName) {
        List<RoleModel> allRoles = getAllRoles(keycloakSession);

        List<RoleEntity> result = new LinkedList<RoleEntity>();
        for (RoleModel roleModel : allRoles) {
            RoleEntity roleEntity = new RoleEntity();
            roleEntity.setId(roleModel.getId());
            result.add(roleEntity);

            roleEntity.setName(roleModel.getName());
            roleEntity.setDescription(roleModel.getDescription());

            RoleContainerModel roleContainer = roleModel.getContainer();
            if (roleContainer instanceof RealmModel) {
                RealmModel realm = (RealmModel)roleContainer;
                roleEntity.setRealmId(realm.getId());
            } else {
                ApplicationModel appModel = (ApplicationModel)roleContainer;
                roleEntity.setApplicationId(appModel.getId());
            }

            List<String> compositeRolesIds = null;
            for (RoleModel composite : roleModel.getComposites()) {

                // Lazy init
                if (compositeRolesIds == null) {
                    compositeRolesIds = new ArrayList<String>();
                }

                compositeRolesIds.add(composite.getId());
            }
            roleEntity.setCompositeRoleIds(compositeRolesIds);
        }

        this.exportWriter.writeEntities(fileName, result);

        logger.infof("%d roles exported: ", result.size());
        if (logger.isDebugEnabled()) {
            logger.debug("Exported roles: " + result);
        }
    }

    protected void exportUsers(KeycloakSession keycloakSession, String fileName) {
        List<RealmModel> realms = keycloakSession.getRealms();
        List<UserEntity> result = new LinkedList<UserEntity>();

        for (RealmModel realm : realms) {
            List<UserModel> userModels = realm.getUsers();
            for (UserModel userModel : userModels) {
                UserEntity userEntity = new UserEntity();
                userEntity.setId(userModel.getId());
                result.add(userEntity);

                this.propertiesManager.setBasicPropertiesFromModel(userModel, userEntity);

                userEntity.setLoginName(userModel.getLoginName());
                userEntity.setRealmId(realm.getId());

                // authentication links
                AuthenticationLinkModel authLink = userModel.getAuthenticationLink();
                if (authLink != null) {
                    AuthenticationLinkEntity authLinkEntity = new AuthenticationLinkEntity();
                    this.propertiesManager.setBasicPropertiesFromModel(authLink, authLinkEntity);

                    userEntity.setAuthenticationLink(authLinkEntity);
                }

                // social links
                Set<SocialLinkModel> socialLinks = realm.getSocialLinks(userModel);
                if (socialLinks != null && !socialLinks.isEmpty()) {
                    List<SocialLinkEntity> socialLinkEntities = new ArrayList<SocialLinkEntity>();
                    for (SocialLinkModel socialLink : socialLinks) {
                        SocialLinkEntity socialLinkEntity = new SocialLinkEntity();
                        this.propertiesManager.setBasicPropertiesFromModel(socialLink, socialLinkEntity);

                        socialLinkEntities.add(socialLinkEntity);
                    }

                    userEntity.setSocialLinks(socialLinkEntities);
                }

                // required actions
                Set<UserModel.RequiredAction> requiredActions = userModel.getRequiredActions();
                if (requiredActions != null && !requiredActions.isEmpty()) {
                    userEntity.setRequiredActions(new ArrayList<UserModel.RequiredAction>(requiredActions));
                }

                // attributes
                userEntity.setAttributes(userModel.getAttributes());

                // roleIds
                Set<RoleModel> roles = userModel.getRoleMappings();
                List<String> roleIds = new ArrayList<String>();
                for (RoleModel role : roles) {
                    roleIds.add(role.getId());
                }
                userEntity.setRoleIds(roleIds);

                // credentials
                List<UserCredentialValueModel> credentials = userModel.getCredentialsDirectly();
                List<CredentialEntity> credEntities = new ArrayList<CredentialEntity>();
                for (UserCredentialValueModel credModel : credentials) {
                    CredentialEntity credEntity = new CredentialEntity();
                    this.propertiesManager.setBasicPropertiesFromModel(credModel, credEntity);
                    credEntities.add(credEntity);
                }

                userEntity.setCredentials(credEntities);
            }
        }

        this.exportWriter.writeEntities(fileName, result);

        logger.infof("%d users exported: ", result.size());
        if (logger.isDebugEnabled()) {
            logger.debug("Exported users: " + result);
        }
    }


     // Does it makes sense to export user failures ?
    protected void exportUserFailures(KeycloakSession keycloakSession, String fileName) {
        List<RealmModel> realms = keycloakSession.getRealms();
        List<UsernameLoginFailureModel> allFailures = new ArrayList<UsernameLoginFailureModel>();
        for (RealmModel realmModel : realms) {
            allFailures.addAll(realmModel.getAllUserLoginFailures());
        }

        List<UsernameLoginFailureEntity> result = new LinkedList<UsernameLoginFailureEntity>();
        for (UsernameLoginFailureModel failureModel : allFailures) {
            UsernameLoginFailureEntity failureEntity = new UsernameLoginFailureEntity();
            this.propertiesManager.setBasicPropertiesFromModel(failureModel, failureEntity);
            result.add(failureEntity);

            failureEntity.setUsername(failureModel.getUsername());
            failureEntity.setNumFailures(failureModel.getNumFailures());
        }

        this.exportWriter.writeEntities(fileName, result);
    }

    private List<String> getScopeIds(ClientModel clientModel) {
        Set<RoleModel> allScopes = clientModel.getScopeMappings();
        List<String> scopeIds = new ArrayList<String>();
        for (RoleModel role : allScopes) {
            scopeIds.add(role.getId());
        }
        return scopeIds;
    }

    private List<ApplicationModel> getAllApplications(KeycloakSession keycloakSession) {
        List<RealmModel> realms = keycloakSession.getRealms();
        List<ApplicationModel> allApplications = new ArrayList<ApplicationModel>();
        for (RealmModel realmModel : realms) {
            allApplications.addAll(realmModel.getApplications());
        }
        return allApplications;
    }

    private List<RoleModel> getAllRoles(KeycloakSession keycloakSession) {
        List<RoleModel> allRoles = new ArrayList<RoleModel>();

        List<RealmModel> realms = keycloakSession.getRealms();
        for (RealmModel realmModel : realms) {
            allRoles.addAll(realmModel.getRoles());
        }

        List<ApplicationModel> allApplications = getAllApplications(keycloakSession);
        for (ApplicationModel appModel : allApplications) {
            allRoles.addAll(appModel.getRoles());
        }

        return allRoles;
    }

}
