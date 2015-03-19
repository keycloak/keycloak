package org.keycloak.services.managers;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.enums.SslRequired;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.CredentialRepresentation;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplianceBootstrap {

    private static final Logger logger = Logger.getLogger(ApplianceBootstrap.class);

    public void bootstrap(KeycloakSessionFactory sessionFactory, String contextPath) {
        KeycloakSession session = sessionFactory.create();
        session.getTransaction().begin();

        try {
            bootstrap(session, contextPath);
            session.getTransaction().commit();
        } finally {
            session.close();
        }
    }

    public void bootstrap(KeycloakSession session, String contextPath) {
        String adminRealmName = Config.getAdminRealm();
        if (session.realms().getRealm(adminRealmName) != null) {
            return;
        }

        logger.info("Initializing " + adminRealmName + " realm");

        RealmManager manager = new RealmManager(session);
        manager.setContextPath(contextPath);
        RealmModel realm = manager.createRealm(adminRealmName, adminRealmName);
        realm.setName(adminRealmName);
        realm.setEnabled(true);
        realm.addRequiredCredential(CredentialRepresentation.PASSWORD);
        realm.setSsoSessionIdleTimeout(1800);
        realm.setAccessTokenLifespan(60);
        realm.setSsoSessionMaxLifespan(36000);
        realm.setAccessCodeLifespan(60);
        realm.setAccessCodeLifespanUserAction(300);
        realm.setAccessCodeLifespanLogin(1800);
        realm.setSslRequired(SslRequired.EXTERNAL);
        realm.setRegistrationAllowed(false);
        realm.setRegistrationEmailAsUsername(false);
        KeycloakModelUtils.generateRealmKeys(realm);

        UserModel adminUser = session.users().addUser(realm, "admin");
        adminUser.setEnabled(true);
        UserCredentialModel password = new UserCredentialModel();
        password.setType(UserCredentialModel.PASSWORD);
        password.setValue("admin");
        session.users().updateCredential(realm, adminUser, password);
        adminUser.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);

        RoleModel adminRole = realm.getRole(AdminRoles.ADMIN);
        adminUser.grantRole(adminRole);

        ApplicationModel accountApp = realm.getApplicationNameMap().get(Constants.ACCOUNT_MANAGEMENT_APP);
        for (String r : accountApp.getDefaultRoles()) {
            adminUser.grantRole(accountApp.getRole(r));
        }
    }

}
