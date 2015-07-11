package org.keycloak.models;

import org.keycloak.Config;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ImpersonationServiceConstants {
    public static String IMPERSONATION_ALLOWED = "impersonation";

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
        ClientModel realmAdminApp = adminRealm.getClientByClientId(KeycloakModelUtils.getMasterRealmAdminApplicationClientId(realm));
        RoleModel impersonationRole = realmAdminApp.addRole(IMPERSONATION_ALLOWED);
        impersonationRole.setDescription("${role_" + IMPERSONATION_ALLOWED + "}");
        adminRole.addCompositeRole(impersonationRole);
    }

    public static void setupRealmRole(RealmModel realm) {
        if (realm.getName().equals(Config.getAdminRealm())) { return; } // don't need to do this for master realm
        String realmAdminApplicationClientId = Constants.REALM_MANAGEMENT_CLIENT_ID;
        ClientModel realmAdminApp = realm.getClientByClientId(realmAdminApplicationClientId);
        RoleModel impersonationRole = realmAdminApp.addRole(IMPERSONATION_ALLOWED);
        impersonationRole.setDescription("${role_" + IMPERSONATION_ALLOWED + "}");
        RoleModel adminRole = realmAdminApp.getRole(AdminRoles.REALM_ADMIN);
        adminRole.addCompositeRole(impersonationRole);
    }


    public static void setupImpersonationService(KeycloakSession session, RealmModel realm, String contextPath) {
        ClientModel client = realm.getClientNameMap().get(Constants.IMPERSONATION_SERVICE_CLIENT_ID);
        if (client == null) {
            client = KeycloakModelUtils.createClient(realm, Constants.IMPERSONATION_SERVICE_CLIENT_ID);
            client.setName("${client_" + Constants.IMPERSONATION_SERVICE_CLIENT_ID + "}");
            client.setEnabled(true);
            client.setFullScopeAllowed(false);
            String base = contextPath + "/realms/" + realm.getName() + "/impersonate";
            String redirectUri = base + "/*";
            client.addRedirectUri(redirectUri);
            client.setBaseUrl(base);

            setupMasterRealmRole(session.realms(), realm);
            setupRealmRole(realm);
        }
    }


}
