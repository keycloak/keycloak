/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models;

import org.keycloak.Config;
import org.keycloak.models.utils.KeycloakModelUtils;

import static org.keycloak.models.AdminRoles.IMPERSONATION;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ImpersonationConstants {

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
        ClientModel realmAdminApp = adminRealm.getClientByClientId(KeycloakModelUtils.getMasterRealmAdminManagementClientId(realm.getName()));
        if (realmAdminApp.getRole(IMPERSONATION) != null) return;
        RoleModel impersonationRole = realmAdminApp.addRole(IMPERSONATION);
        impersonationRole.setDescription("${role_" + IMPERSONATION + "}");
        adminRole.addCompositeRole(impersonationRole);
    }

    public static void setupRealmRole(RealmModel realm) {
        if (realm.getName().equals(Config.getAdminRealm())) { return; } // don't need to do this for master realm
        String realmAdminApplicationClientId = Constants.REALM_MANAGEMENT_CLIENT_ID;
        ClientModel realmAdminApp = realm.getClientByClientId(realmAdminApplicationClientId);
        if (realmAdminApp.getRole(IMPERSONATION) != null) return;
        RoleModel impersonationRole = realmAdminApp.addRole(IMPERSONATION);
        impersonationRole.setDescription("${role_" + IMPERSONATION + "}");
        RoleModel adminRole = realmAdminApp.getRole(AdminRoles.REALM_ADMIN);
        adminRole.addCompositeRole(impersonationRole);
    }


    public static void setupImpersonationService(KeycloakSession session, RealmModel realm) {
        setupMasterRealmRole(session.realms(), realm);
        setupRealmRole(realm);
    }


}
