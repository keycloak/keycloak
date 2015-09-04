package org.keycloak.models;

import org.keycloak.Config;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ImpersonationConstants {
    public static String IMPERSONATION_ROLE = "impersonation";


    public static void setupMasterRealmRole(RealmProvider model, RealmModel realm) {
        RealmModel adminRealm;
        RoleModel adminRole;

        if (realm.getName().equals(Config.getAdminRealm())) {
            adminRealm = realm;
            adminRole = realm.getRole(AdminRoles.ADMIN);
        } else {
            adminRealm = model.getRealmByName(Config.getAdminRealm());
            adminRole = adminRealm.getRole(AdminRoles.ADMIN);
        }
        ClientModel realmAdminApp = adminRealm.getClientByClientId(KeycloakModelUtils.getMasterRealmAdminApplicationClientId(realm.getName()));
        if (realmAdminApp.getRole(IMPERSONATION_ROLE) != null) return;
        RoleModel impersonationRole = realmAdminApp.addRole(IMPERSONATION_ROLE);
        impersonationRole.setDescription("${role_" + IMPERSONATION_ROLE + "}");
        adminRole.addCompositeRole(impersonationRole);
    }

    public static void setupRealmRole(RealmModel realm) {
        if (realm.getName().equals(Config.getAdminRealm())) { return; } // don't need to do this for master realm
        String realmAdminApplicationClientId = Constants.REALM_MANAGEMENT_CLIENT_ID;
        ClientModel realmAdminApp = realm.getClientByClientId(realmAdminApplicationClientId);
        if (realmAdminApp.getRole(IMPERSONATION_ROLE) != null) return;
        RoleModel impersonationRole = realmAdminApp.addRole(IMPERSONATION_ROLE);
        impersonationRole.setDescription("${role_" + IMPERSONATION_ROLE + "}");
        RoleModel adminRole = realmAdminApp.getRole(AdminRoles.REALM_ADMIN);
        adminRole.addCompositeRole(impersonationRole);
    }


    public static void setupImpersonationService(KeycloakSession session, RealmModel realm) {
        setupMasterRealmRole(session.realms(), realm);
        setupRealmRole(realm);
    }


}
