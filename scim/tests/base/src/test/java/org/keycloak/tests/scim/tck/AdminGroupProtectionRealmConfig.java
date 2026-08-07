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

package org.keycloak.tests.scim.tck;

import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.testframework.realm.GroupBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RoleBuilder;
import org.keycloak.testframework.realm.UserBuilder;

public class AdminGroupProtectionRealmConfig extends ScimRealmConfig {

    public static final String ADMIN_GROUP = "admin-group";
    public static final String ADMIN_PARENT_GROUP = "admin-parent-group";
    public static final String ADMIN_CHILD_GROUP = "admin-child-group";
    public static final String COMPOSITE_ADMIN_ROLE = "composite-admin-role";
    public static final String ADMIN_VIA_COMPOSITE_GROUP = "admin-via-composite-group";
    public static final String REGULAR_GROUP = "regular-group";
    public static final String REGULAR_USER = "regular-user";
    public static final String ADMIN_USER = "admin-user";

    @Override
    public RealmBuilder configure(RealmBuilder realm) {
        return super.configure(realm)
                .groups(
                        GroupBuilder.create(ADMIN_GROUP)
                                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_EVENTS),
                        GroupBuilder.create(ADMIN_PARENT_GROUP)
                                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_AUTHORIZATION)
                                .subGroups(ADMIN_CHILD_GROUP),
                        GroupBuilder.create(ADMIN_VIA_COMPOSITE_GROUP)
                                .realmRoles(COMPOSITE_ADMIN_ROLE),
                        GroupBuilder.create(REGULAR_GROUP)
                )
                .realmRoles(
                        RoleBuilder.create(COMPOSITE_ADMIN_ROLE)
                                .clientComposite(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.VIEW_AUTHORIZATION)
                )
                .users(
                        UserBuilder.create(REGULAR_USER)
                                .enabled(true),
                        UserBuilder.create(ADMIN_USER)
                                .enabled(true)
                                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_USERS)
                );
    }
}