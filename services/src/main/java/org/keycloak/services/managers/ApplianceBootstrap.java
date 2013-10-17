package org.keycloak.services.managers;

import org.keycloak.models.*;
import org.keycloak.representations.idm.CredentialRepresentation;

import java.util.UUID;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplianceBootstrap {


    public void initKeycloakAdminRealm(RealmModel realm) {

    }

    public void bootstrap(KeycloakSession session) {
        RealmManager manager = new RealmManager(session);
        RealmModel realm = manager.createRealm(Constants.ADMIN_REALM, Constants.ADMIN_REALM);
        realm.setName(Constants.ADMIN_REALM);
        realm.setEnabled(true);
        realm.addRequiredCredential(CredentialRepresentation.PASSWORD);
        realm.addRequiredOAuthClientCredential(CredentialRepresentation.PASSWORD);
        realm.addRequiredResourceCredential(CredentialRepresentation.PASSWORD);
        realm.setTokenLifespan(300);
        realm.setAccessCodeLifespan(60);
        realm.setAccessCodeLifespanUserAction(300);
        realm.setSslNotRequired(true);
        realm.setCookieLoginAllowed(true);
        realm.setRegistrationAllowed(false);
        manager.generateRealmKeys(realm);
        initKeycloakAdminRealm(realm);

        ApplicationModel adminConsole = realm.addApplication(Constants.ADMIN_CONSOLE_APPLICATION);
        adminConsole.setEnabled(true);
        UserCredentialModel adminConsolePassword = new UserCredentialModel();
        adminConsolePassword.setType(UserCredentialModel.PASSWORD);
        adminConsolePassword.setValue(UUID.randomUUID().toString()); // just a random password as we'll never access it
        realm.updateCredential(adminConsole.getApplicationUser(), adminConsolePassword);

        RoleModel applicationRole = realm.getRole(Constants.APPLICATION_ROLE);
        realm.grantRole(adminConsole.getApplicationUser(), applicationRole);
        RoleModel adminRole = adminConsole.addRole(Constants.ADMIN_CONSOLE_ADMIN_ROLE);

        UserModel adminUser = realm.addUser("admin");
        adminUser.setEnabled(true);
        UserCredentialModel password = new UserCredentialModel();
        password.setType(UserCredentialModel.PASSWORD);
        password.setValue("admin");
        realm.updateCredential(adminUser, password);
        adminUser.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);

        adminConsole.grantRole(adminUser, adminRole);



    }
}
