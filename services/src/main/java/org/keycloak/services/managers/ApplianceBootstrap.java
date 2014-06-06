package org.keycloak.services.managers;

import java.util.Arrays;

import org.jboss.logging.Logger;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.AuthenticationProviderModel;
import org.keycloak.Config;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderSession;
import org.keycloak.provider.ProviderSessionFactory;
import org.keycloak.representations.idm.CredentialRepresentation;

import java.util.Collections;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplianceBootstrap {

    private static final Logger logger = Logger.getLogger(ApplianceBootstrap.class);

    public void bootstrap(ProviderSessionFactory factory, String contextPath) {
        ProviderSession providerSession = factory.createSession();
        KeycloakSession session = providerSession.getProvider(KeycloakSession.class);
        session.getTransaction().begin();

        try {
            bootstrap(session, contextPath);
            session.getTransaction().commit();
        } finally {
            providerSession.close();
        }
    }

    public void bootstrap(KeycloakSession session, String contextPath) {
        String adminRealmName = Config.getAdminRealm();
        if (session.getRealm(adminRealmName) != null) {
            return;
        }

        logger.info("Initializing " + adminRealmName + " realm");

        RealmManager manager = new RealmManager(session);
        manager.setContextPath(contextPath);
        RealmModel realm = manager.createRealm(adminRealmName, adminRealmName);
        realm.setName(adminRealmName);
        realm.setEnabled(true);
        realm.addRequiredCredential(CredentialRepresentation.PASSWORD);
        realm.setSsoSessionIdleTimeout(300);
        realm.setAccessTokenLifespan(60);
        realm.setSsoSessionMaxLifespan(36000);
        realm.setAccessCodeLifespan(60);
        realm.setAccessCodeLifespanUserAction(300);
        realm.setSslNotRequired(true);
        realm.setRegistrationAllowed(false);
        manager.generateRealmKeys(realm);
        realm.setAuthenticationProviders(Arrays.asList(AuthenticationProviderModel.DEFAULT_PROVIDER));

        realm.setAuditListeners(Collections.singleton("jboss-logging"));

        UserModel adminUser = realm.addUser("admin");
        adminUser.setEnabled(true);
        UserCredentialModel password = new UserCredentialModel();
        password.setType(UserCredentialModel.PASSWORD);
        password.setValue("admin");
        adminUser.updateCredential(password);
        adminUser.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);

        RoleModel adminRole = realm.getRole(AdminRoles.ADMIN);
        realm.grantRole(adminUser, adminRole);

        ApplicationModel accountApp = realm.getApplicationNameMap().get(Constants.ACCOUNT_MANAGEMENT_APP);
        for (String r : accountApp.getDefaultRoles()) {
            realm.grantRole(adminUser, accountApp.getRole(r));
        }
    }

}
