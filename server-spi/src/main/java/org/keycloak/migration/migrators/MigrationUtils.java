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

package org.keycloak.migration.migrators;

import org.keycloak.Config;
import org.keycloak.models.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MigrationUtils {

    public static void addAdminRole(RealmModel realm, String roleName) {
        ClientModel client = realm.getMasterAdminClient();
        if (client.getRole(roleName) == null) {
            RoleModel role = client.addRole(roleName);
            role.setDescription("${role_" + roleName + "}");
            role.setScopeParamRequired(false);

            client.getRealm().getRole(AdminRoles.ADMIN).addCompositeRole(role);
        }

        if (!realm.getName().equals(Config.getAdminRealm())) {
            client = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
            if (client.getRole(roleName) == null) {
                RoleModel role = client.addRole(roleName);
                role.setDescription("${role_" + roleName + "}");
                role.setScopeParamRequired(false);

                client.getRole(AdminRoles.REALM_ADMIN).addCompositeRole(role);
            }
        }
    }

    public static void updateOTPRequiredAction(RequiredActionProviderModel otpAction) {
        if (otpAction == null) return;
        if (!otpAction.getProviderId().equals(UserModel.RequiredAction.CONFIGURE_TOTP.name())) return;
        if (!otpAction.getName().equals("Configure Totp")) return;

        otpAction.setName("Configure OTP");
    }

}
