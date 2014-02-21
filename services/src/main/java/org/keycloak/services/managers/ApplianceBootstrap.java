package org.keycloak.services.managers;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplianceBootstrap {

    private static final Logger logger = Logger.getLogger(ApplianceBootstrap.class);

    public void bootstrap(KeycloakSessionFactory factory) {
        KeycloakSession session = factory.createSession();
        session.getTransaction().begin();

        try {
            bootstrap(session);
            session.getTransaction().commit();
        } finally {
            session.close();
        }

    }

    public void bootstrap(KeycloakSession session) {
        if (session.getRealm(Constants.ADMIN_REALM) != null) {
            return;
        }

        logger.info("Initializing " + Constants.ADMIN_REALM + " realm");

        RealmManager manager = new RealmManager(session);
        RealmModel realm = manager.createRealm(Constants.ADMIN_REALM, Constants.ADMIN_REALM);
        realm.setName(Constants.ADMIN_REALM);
        realm.setEnabled(true);
        realm.addRequiredCredential(CredentialRepresentation.PASSWORD);
        realm.addRequiredOAuthClientCredential(CredentialRepresentation.PASSWORD);
        realm.addRequiredResourceCredential(CredentialRepresentation.PASSWORD);
        realm.setCentralLoginLifespan(3000);
        realm.setAccessTokenLifespan(60);
        realm.setRefreshTokenLifespan(3600);
        realm.setAccessCodeLifespan(60);
        realm.setAccessCodeLifespanUserAction(300);
        realm.setSslNotRequired(true);
        realm.setRegistrationAllowed(false);
        manager.generateRealmKeys(realm);

        realm.setLoginTheme("keycloak");
        realm.setAccountTheme("keycloak");

        ApplicationModel adminConsole = new ApplicationManager(manager).createApplication(realm, Constants.ADMIN_CONSOLE_APPLICATION);
        adminConsole.setBaseUrl("/auth/admin/index.html");
        adminConsole.setEnabled(true);

        RoleModel adminRole = realm.getRole(AdminRoles.ADMIN);

        adminConsole.addScope(adminRole);

        UserModel adminUser = realm.addUser("admin");
        adminUser.setEnabled(true);
        UserCredentialModel password = new UserCredentialModel();
        password.setType(UserCredentialModel.PASSWORD);
        password.setValue("admin");
        realm.updateCredential(adminUser, password);
        adminUser.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);

        realm.grantRole(adminUser, adminRole);

        ApplicationModel accountApp = realm.getApplicationNameMap().get(Constants.ACCOUNT_MANAGEMENT_APP);
        for (String r : accountApp.getDefaultRoles()) {
            realm.grantRole(adminUser, accountApp.getRole(r));
        }
    }

}
