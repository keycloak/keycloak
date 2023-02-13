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

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AdminRoles {

    public static String APP_SUFFIX = "-realm";

    public static String ADMIN = "admin";

    // for admin client local to each realm
    public static String REALM_ADMIN = "realm-admin";

    public static String CREATE_REALM = "create-realm";
    public static String CREATE_CLIENT = "create-client";

    public static String VIEW_REALM = "view-realm";
    public static String VIEW_USERS = "view-users";
    public static String VIEW_CLIENTS = "view-clients";
    public static String VIEW_EVENTS = "view-events";
    public static String VIEW_IDENTITY_PROVIDERS = "view-identity-providers";
    public static String VIEW_AUTHORIZATION = "view-authorization";

    public static String MANAGE_REALM = "manage-realm";
    public static String MANAGE_USERS = "manage-users";
    public static String MANAGE_IDENTITY_PROVIDERS = "manage-identity-providers";
    public static String MANAGE_CLIENTS = "manage-clients";
    public static String MANAGE_EVENTS = "manage-events";
    public static String MANAGE_AUTHORIZATION = "manage-authorization";

    public static String QUERY_USERS = "query-users";
    public static String QUERY_CLIENTS = "query-clients";
    public static String QUERY_REALMS = "query-realms";
    public static String QUERY_GROUPS = "query-groups";

    public static String[] ALL_REALM_ROLES = {CREATE_CLIENT, VIEW_REALM, VIEW_USERS, VIEW_CLIENTS, VIEW_EVENTS, VIEW_IDENTITY_PROVIDERS, VIEW_AUTHORIZATION, MANAGE_REALM, MANAGE_USERS, MANAGE_CLIENTS, MANAGE_EVENTS, MANAGE_IDENTITY_PROVIDERS, MANAGE_AUTHORIZATION, QUERY_USERS, QUERY_CLIENTS, QUERY_REALMS, QUERY_GROUPS};
    public static String[] ALL_QUERY_ROLES = {QUERY_USERS, QUERY_CLIENTS, QUERY_REALMS, QUERY_GROUPS};

    public static Set<String> ALL_ROLES = new HashSet<>();
    static {
        for (String name : ALL_REALM_ROLES) {
            ALL_ROLES.add(name);
        }
        ALL_ROLES.add(ImpersonationConstants.IMPERSONATION_ROLE);
        ALL_ROLES.add(ADMIN);
        ALL_ROLES.add(CREATE_REALM);
        ALL_ROLES.add(CREATE_CLIENT);
        ALL_ROLES.add(REALM_ADMIN);
    }
}
