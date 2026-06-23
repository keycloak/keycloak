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

public class AdminUserProtectionRealmConfig extends ScimRealmConfig {

    public static final String ADMIN_MANAGE_USERS = "admin-manage-users";
    public static final String ADMIN_MANAGE_CLIENTS = "admin-manage-clients";
    public static final String ADMIN_MANAGE_REALM = "admin-manage-realm";
    public static final String ADMIN_MANAGE_IDENTITY_PROVIDERS = "admin-manage-identity-providers";
    public static final String ADMIN_VIEW_CLIENTS = "admin-view-clients";
    public static final String ADMIN_QUERY_USERS = "admin-query-users";
    public static final String ADMIN_IMPERSONATION = "admin-impersonation";
    public static final String ADMIN_MANAGE_CLIENTS_REVOKABLE = "admin-manage-clients-revokable";
    public static final String ADMIN_GROUP = "admin-group";
    public static final String ADMIN_VIA_GROUP = "admin-via-group";
    public static final String ADMIN_PARENT_GROUP = "admin-parent-group";
    public static final String ADMIN_CHILD_GROUP = "admin-child-group";
    public static final String ADMIN_VIA_NESTED_GROUP = "admin-via-nested-group";
    public static final String COMPOSITE_ADMIN_ROLE = "composite-admin-role";
    public static final String ADMIN_VIA_COMPOSITE = "admin-via-composite";
    public static final String REGULAR_USER = "regular-user";

    @Override
    public RealmBuilder configure(RealmBuilder realm) {
        return super.configure(realm)
                .groups(
                        GroupBuilder.create(ADMIN_GROUP)
                                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_EVENTS),
                        GroupBuilder.create(ADMIN_PARENT_GROUP)
                                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_AUTHORIZATION)
                                .subGroups(ADMIN_CHILD_GROUP)
                )
                .realmRoles(
                        RoleBuilder.create(COMPOSITE_ADMIN_ROLE)
                                .clientComposite(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.VIEW_AUTHORIZATION)
                )
                .users(
                        UserBuilder.create(ADMIN_MANAGE_USERS)
                                .enabled(true)
                                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_USERS),
                        UserBuilder.create(ADMIN_MANAGE_CLIENTS)
                                .enabled(true)
                                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_CLIENTS),
                        UserBuilder.create(ADMIN_MANAGE_REALM)
                                .enabled(true)
                                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_REALM),
                        UserBuilder.create(ADMIN_MANAGE_IDENTITY_PROVIDERS)
                                .enabled(true)
                                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_IDENTITY_PROVIDERS),
                        UserBuilder.create(ADMIN_VIEW_CLIENTS)
                                .enabled(true)
                                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.VIEW_CLIENTS),
                        UserBuilder.create(ADMIN_QUERY_USERS)
                                .enabled(true)
                                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.QUERY_USERS),
                        UserBuilder.create(ADMIN_IMPERSONATION)
                                .enabled(true)
                                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.IMPERSONATION),
                        UserBuilder.create(ADMIN_MANAGE_CLIENTS_REVOKABLE)
                                .enabled(true)
                                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_CLIENTS),
                        UserBuilder.create(ADMIN_VIA_GROUP)
                                .enabled(true)
                                .groups(ADMIN_GROUP),
                        UserBuilder.create(ADMIN_VIA_NESTED_GROUP)
                                .enabled(true)
                                .groups("/" + ADMIN_PARENT_GROUP + "/" + ADMIN_CHILD_GROUP),
                        UserBuilder.create(ADMIN_VIA_COMPOSITE)
                                .enabled(true)
                                .realmRoles(COMPOSITE_ADMIN_ROLE),
                        UserBuilder.create(REGULAR_USER)
                                .enabled(true)
                );
    }
}
