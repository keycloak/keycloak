package org.keycloak.services.managers;

import java.util.Arrays;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.AuthenticationProviderModel;
import org.keycloak.models.Config;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;

import java.util.Collections;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplianceBootstrap {

    private static final Logger logger = Logger.getLogger(ApplianceBootstrap.class);

    public void bootstrap(KeycloakSessionFactory factory, String contextPath) {
        KeycloakSession session = factory.createSession();
        session.getTransaction().begin();

        try {
            bootstrap(session, contextPath);
            session.getTransaction().commit();
        } finally {
            session.close();
        }

    }

    public void bootstrap(KeycloakSession session, String contextPath) {
        if (session.getRealm(Config.getAdminRealm()) != null) {
            return;
        }

        String adminRealmName = Config.getAdminRealm();

        logger.info("Initializing " + adminRealmName + " realm");

        RealmManager manager = new RealmManager(session);
        RealmModel realm = manager.createRealm(adminRealmName, adminRealmName);
        realm.setName(adminRealmName);
        realm.setEnabled(true);
        realm.addRequiredCredential(CredentialRepresentation.PASSWORD);
        realm.setCentralLoginLifespan(3000);
        realm.setAccessTokenLifespan(60);
        realm.setRefreshTokenLifespan(3600);
        realm.setAccessCodeLifespan(60);
        realm.setAccessCodeLifespanUserAction(300);
        realm.setSslNotRequired(true);
        realm.setRegistrationAllowed(false);
        manager.generateRealmKeys(realm);
        realm.setAuthenticationProviders(Arrays.asList(AuthenticationProviderModel.DEFAULT_PROVIDER));

        ApplicationModel adminConsole = new ApplicationManager(manager).createApplication(realm, Constants.ADMIN_CONSOLE_APPLICATION);
        adminConsole.setBaseUrl(contextPath + "/admin/index.html");
        adminConsole.setEnabled(true);

        realm.setAuditListeners(Collections.singleton("jboss-logging"));

        RoleModel adminRole = realm.getRole(AdminRoles.ADMIN);

        realm.addScopeMapping(adminConsole, adminRole);

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
