package org.keycloak.services.managers;

import org.keycloak.models.*;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.resources.SaasService;

import java.util.UUID;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplianceBootstrap {


    public static final String ADMIN_REALM = "Keycloak Adminstration";
    public static final String ADMIN_CONSOLE = "Admin Console";

    public void initKeycloakAdminRealm(RealmModel realm) {

    }

    public void bootstrap(KeycloakSession session) {
        RealmManager manager = new RealmManager(session);
        RealmModel realm = manager.createRealm(ADMIN_REALM, "Keycloak Adminstration");
        realm.setName("Keycloak Adminstration");
        realm.setEnabled(true);
        realm.addRequiredCredential(CredentialRepresentation.PASSWORD);
        realm.addRequiredOAuthClientCredential(CredentialRepresentation.PASSWORD);
        realm.addRequiredResourceCredential(CredentialRepresentation.PASSWORD);
        realm.setTokenLifespan(300);
        realm.setAccessCodeLifespan(60);
        realm.setSslNotRequired(true);
        realm.setCookieLoginAllowed(true);
        realm.setRegistrationAllowed(false);
        manager.generateRealmKeys(realm);
        initKeycloakAdminRealm(realm);

        ApplicationModel adminConsole = realm.addApplication(ADMIN_CONSOLE);
        adminConsole.setEnabled(true);
        UserCredentialModel adminConsolePassword = new UserCredentialModel();
        adminConsolePassword.setType(UserCredentialModel.PASSWORD);
        adminConsolePassword.setValue(UUID.randomUUID().toString()); // just a random password as we'll never access it
        realm.updateCredential(adminConsole.getApplicationUser(), adminConsolePassword);

        RoleModel applicationRole = realm.getRole(RealmManager.APPLICATION_ROLE);
        realm.grantRole(adminConsole.getApplicationUser(), applicationRole);
        RoleModel adminRole = adminConsole.addRole("admin");

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
