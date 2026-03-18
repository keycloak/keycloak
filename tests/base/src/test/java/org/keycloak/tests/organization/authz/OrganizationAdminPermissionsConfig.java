/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.organization.authz;

import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;

public class OrganizationAdminPermissionsConfig implements RealmConfig {

    @Override
    public RealmConfigBuilder configure(RealmConfigBuilder realm) {
        realm.addUser("myadmin")
                .name("My", "Admin")
                .email("myadmin@localhost")
                .emailVerified(true)
                .password("password")
                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                        AdminRoles.QUERY_USERS,
                        AdminRoles.MANAGE_ORGANIZATIONS);
        realm.addClient("myclient")
                .secret("mysecret")
                .directAccessGrantsEnabled(true);
        return realm
                .adminPermissionsEnabled(true)
                .organizationsEnabled(true);
    }
}
