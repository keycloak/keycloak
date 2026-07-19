/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.organization.authz.fgap;

import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;

public class OrganizationFgapConfig implements RealmConfig {

    @Override
    public RealmBuilder configure(RealmBuilder realm) {
        realm.users(UserBuilder.create()
                .username("myadmin")
                .name("My", "Admin")
                .email("myadmin@localhost")
                .emailVerified(true)
                .password("password")
                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                        AdminRoles.QUERY_USERS,
                        AdminRoles.QUERY_ORGANIZATIONS).build());
        realm.clients(ClientBuilder.create()
                .clientId("myclient")
                .secret("mysecret")
                .directAccessGrantsEnabled(true).build());
        return realm
                .adminPermissionsEnabled(true)
                .organizationsEnabled(true);
    }
}
