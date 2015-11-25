package org.keycloak.services.managers;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
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

    public static boolean setupDefaultRealm(KeycloakSessionFactory sessionFactory, String contextPath) {
        KeycloakSession session = sessionFactory.create();
        session.getTransaction().begin();

        try {
            String adminRealmName = Config.getAdminRealm();
            if (session.realms().getRealm(adminRealmName) != null) {
                return false;
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
            realm.setOfflineSessionIdleTimeout(Constants.DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT);
            realm.setAccessCodeLifespan(60);
            realm.setAccessCodeLifespanUserAction(300);
            realm.setAccessCodeLifespanLogin(1800);
            realm.setSslRequired(SslRequired.EXTERNAL);
            realm.setRegistrationAllowed(false);
            realm.setRegistrationEmailAsUsername(false);
            KeycloakModelUtils.generateRealmKeys(realm);

            session.getTransaction().commit();
            return true;
        } finally {
            session.close();
        }
    }

    public static boolean setupDefaultUser(KeycloakSessionFactory sessionFactory) {
        KeycloakSession session = sessionFactory.create();
        session.getTransaction().begin();

        try {
            RealmModel realm = session.realms().getRealm(Config.getAdminRealm());
            if (session.users().getUserByUsername("admin", realm) == null) {
                UserModel adminUser = session.users().addUser(realm, "admin");

                adminUser.setEnabled(true);
                UserCredentialModel usrCredModel = new UserCredentialModel();
                usrCredModel.setType(UserCredentialModel.PASSWORD);
                usrCredModel.setValue("admin");
                session.users().updateCredential(realm, adminUser, usrCredModel);
                adminUser.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);

                RoleModel adminRole = realm.getRole(AdminRoles.ADMIN);
                adminUser.grantRole(adminRole);

                ClientModel accountApp = realm.getClientNameMap().get(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
                for (String r : accountApp.getDefaultRoles()) {
                    adminUser.grantRole(accountApp.getRole(r));
                }
            }
            session.getTransaction().commit();
            return true;
        } finally {
            session.close();
        }
    }

}
