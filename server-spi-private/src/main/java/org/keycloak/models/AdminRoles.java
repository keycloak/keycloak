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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AdminRoles {

    public static final String APP_SUFFIX = "-realm";

    public static final String ADMIN = "admin";

    // for admin client local to each realm
    public static final String REALM_ADMIN = "realm-admin";

    public static final String CREATE_REALM = "create-realm";
    public static final String CREATE_CLIENT = "create-client";

    public static final String VIEW_REALM = "view-realm";
    public static final String VIEW_USERS = "view-users";
    public static final String VIEW_CLIENTS = "view-clients";
    public static final String VIEW_EVENTS = "view-events";
    public static final String VIEW_IDENTITY_PROVIDERS = "view-identity-providers";
    public static final String VIEW_AUTHORIZATION = "view-authorization";
    @Deprecated(since = "26.4", forRemoval = true)
    public static final String VIEW_SYSTEM = "view-system";

    public static final String MANAGE_REALM = "manage-realm";
    public static final String MANAGE_USERS = "manage-users";
    public static final String MANAGE_IDENTITY_PROVIDERS = "manage-identity-providers";
    public static final String MANAGE_CLIENTS = "manage-clients";
    public static final String MANAGE_EVENTS = "manage-events";
    public static final String MANAGE_AUTHORIZATION = "manage-authorization";

    public static final String QUERY_USERS = "query-users";
    public static final String QUERY_CLIENTS = "query-clients";
    public static final String QUERY_REALMS = "query-realms";
    public static final String QUERY_GROUPS = "query-groups";

    public static final String IMPERSONATION = "impersonation";

    public static String[] ALL_REALM_ROLES = {CREATE_CLIENT, VIEW_REALM, VIEW_USERS, VIEW_CLIENTS, VIEW_EVENTS, VIEW_IDENTITY_PROVIDERS, VIEW_AUTHORIZATION, MANAGE_REALM, MANAGE_USERS, MANAGE_CLIENTS, MANAGE_EVENTS, MANAGE_IDENTITY_PROVIDERS, MANAGE_AUTHORIZATION, QUERY_USERS, QUERY_CLIENTS, QUERY_REALMS, QUERY_GROUPS};
    public static String[] ALL_QUERY_ROLES = {QUERY_USERS, QUERY_CLIENTS, QUERY_REALMS, QUERY_GROUPS};

    public static Set<String> ALL_ROLES = new HashSet<>();
    static {
        ALL_ROLES.addAll(Arrays.asList(ALL_REALM_ROLES));
        ALL_ROLES.add(IMPERSONATION);
        ALL_ROLES.add(ADMIN);
        ALL_ROLES.add(CREATE_REALM);
        ALL_ROLES.add(REALM_ADMIN);
    }
}
