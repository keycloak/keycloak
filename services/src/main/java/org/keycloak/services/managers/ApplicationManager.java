package org.keycloak.services.managers;

import org.keycloak.models.*;
import org.keycloak.representations.idm.*;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplicationManager {

    protected RealmManager realmManager;

    public ApplicationManager(RealmManager realmManager) {
        this.realmManager = realmManager;
    }

    public ApplicationModel createApplication(RealmModel realm, RoleModel loginRole, ApplicationRepresentation resourceRep) {
        ApplicationModel applicationModel = realm.addApplication(resourceRep.getName());
        applicationModel.setEnabled(resourceRep.isEnabled());
        applicationModel.setManagementUrl(resourceRep.getAdminUrl());
        applicationModel.setSurrogateAuthRequired(resourceRep.isSurrogateAuthRequired());
        applicationModel.setBaseUrl(resourceRep.getBaseUrl());
        applicationModel.updateApplication();

        UserModel resourceUser = applicationModel.getApplicationUser();
        if (resourceRep.getCredentials() != null) {
            for (CredentialRepresentation cred : resourceRep.getCredentials()) {
                UserCredentialModel credential = new UserCredentialModel();
                credential.setType(cred.getType());
                credential.setValue(cred.getValue());
                realm.updateCredential(resourceUser, credential);
            }
        }
        realm.grantRole(resourceUser, loginRole);


        if (resourceRep.getRoles() != null) {
            for (RoleRepresentation roleRep : resourceRep.getRoles()) {
                RoleModel role = applicationModel.addRole(roleRep.getName());
                if (roleRep.getDescription() != null) role.setDescription(roleRep.getDescription());
            }
        }
        if (resourceRep.getRoleMappings() != null) {
            for (UserRoleMappingRepresentation mapping : resourceRep.getRoleMappings()) {
                UserModel user = realm.getUser(mapping.getUsername());
                for (String roleString : mapping.getRoles()) {
                    RoleModel role = applicationModel.getRole(roleString.trim());
                    if (role == null) {
                        role = applicationModel.addRole(roleString.trim());
                    }
                    realm.grantRole(user, role);
                }
            }
        }
        if (resourceRep.getScopeMappings() != null) {
            for (ScopeMappingRepresentation mapping : resourceRep.getScopeMappings()) {
                UserModel user = realm.getUser(mapping.getUsername());
                for (String roleString : mapping.getRoles()) {
                    RoleModel role = applicationModel.getRole(roleString.trim());
                    if (role == null) {
                        role = applicationModel.addRole(roleString.trim());
                    }
                    applicationModel.addScopeMapping(user, role.getName());
                }
            }
        }
        if (resourceRep.isUseRealmMappings()) realm.addScopeMapping(applicationModel.getApplicationUser(), "*");
        return applicationModel;
    }

    public ApplicationModel createApplication(RealmModel realm, ApplicationRepresentation resourceRep) {
        RoleModel loginRole = realm.getRole(Constants.APPLICATION_ROLE);
        return createApplication(realm, loginRole, resourceRep);
    }

    public void updateApplication(ApplicationRepresentation rep, ApplicationModel resource) {
        resource.setName(rep.getName());
        resource.setEnabled(rep.isEnabled());
        resource.setManagementUrl(rep.getAdminUrl());
        resource.setBaseUrl(rep.getBaseUrl());
        resource.setSurrogateAuthRequired(rep.isSurrogateAuthRequired());
        resource.updateApplication();

    }

    public ApplicationRepresentation toRepresentation(ApplicationModel applicationModel) {
        ApplicationRepresentation rep = new ApplicationRepresentation();
        rep.setId(applicationModel.getId());
        rep.setName(applicationModel.getName());
        rep.setEnabled(applicationModel.isEnabled());
        rep.setAdminUrl(applicationModel.getManagementUrl());
        rep.setSurrogateAuthRequired(applicationModel.isSurrogateAuthRequired());
        rep.setBaseUrl(applicationModel.getBaseUrl());
        return rep;

    }
}
