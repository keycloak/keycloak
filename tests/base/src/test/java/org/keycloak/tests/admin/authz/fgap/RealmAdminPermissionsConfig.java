/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.authz.fgap;

import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;

public class RealmAdminPermissionsConfig implements RealmConfig {

    @Override
    public RealmConfigBuilder configure(RealmConfigBuilder realm) {
        realm.addUser("myadmin")
                .name("My", "Admin")
                .email("myadmin@localhost")
                .emailVerified(true)
                .password("password")
                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, 
                        AdminRoles.QUERY_USERS,
                        AdminRoles.QUERY_GROUPS,
                        AdminRoles.QUERY_CLIENTS);
        realm.addClient("myclient")
                .secret("mysecret")
                .directAccessGrantsEnabled(true);
        realm.addClient("myresourceserver")
                .secret("mysecret")
                .directAccessGrantsEnabled(true)
                .authorizationServicesEnabled(true);
        return realm.adminPermissionsEnabled(true);
    }
}
