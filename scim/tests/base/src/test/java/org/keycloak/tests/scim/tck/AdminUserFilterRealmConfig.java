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
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.UserBuilder;

public class AdminUserFilterRealmConfig extends ScimRealmConfig {

    public static final String ADMIN_MANAGE_USERS = "admin-manage-users";
    public static final String ADMIN_VIEW_USERS = "admin-view-users";
    public static final String ADMIN_VIEW_REALM = "admin-view-realm";
    public static final String ADMIN_QUERY_USERS = "admin-query-users";
    public static final String ADMIN_VIEW_EVENTS = "admin-view-events";
    public static final String ADMIN_IMPERSONATION = "admin-impersonation";
    public static final String ADMIN_VIEW_REALM_REVOKABLE = "admin-view-realm-revokable";
    public static final String REGULAR_USER = "regular-user";

    @Override
    public RealmBuilder configure(RealmBuilder realm) {
        return super.configure(realm)
                .users(
                        UserBuilder.create(ADMIN_MANAGE_USERS)
                                .enabled(true)
                                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_USERS),
                        UserBuilder.create(ADMIN_VIEW_USERS)
                                .enabled(true)
                                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.VIEW_USERS),
                        UserBuilder.create(ADMIN_VIEW_REALM)
                                .enabled(true)
                                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.VIEW_REALM),
                        UserBuilder.create(ADMIN_QUERY_USERS)
                                .enabled(true)
                                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.QUERY_USERS),
                        UserBuilder.create(ADMIN_VIEW_EVENTS)
                                .enabled(true)
                                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.VIEW_EVENTS),
                        UserBuilder.create(ADMIN_IMPERSONATION)
                                .enabled(true)
                                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.IMPERSONATION),
                        UserBuilder.create(ADMIN_VIEW_REALM_REVOKABLE)
                                .enabled(true)
                                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.VIEW_REALM),
                        UserBuilder.create(REGULAR_USER)
                                .enabled(true)
                );
    }
}
