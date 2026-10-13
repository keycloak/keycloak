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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.keycloak.Config;

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
    public static final String VIEW_ORGANIZATIONS = "view-organizations";

    public static final String MANAGE_REALM = "manage-realm";
    public static final String MANAGE_USERS = "manage-users";
    public static final String MANAGE_IDENTITY_PROVIDERS = "manage-identity-providers";
    public static final String MANAGE_CLIENTS = "manage-clients";
    public static final String MANAGE_EVENTS = "manage-events";
    public static final String MANAGE_AUTHORIZATION = "manage-authorization";
    public static final String MANAGE_ORGANIZATIONS = "manage-organizations";

    public static final String QUERY_USERS = "query-users";
    public static final String QUERY_CLIENTS = "query-clients";
    public static final String QUERY_REALMS = "query-realms";
    public static final String QUERY_GROUPS = "query-groups";
    public static final String QUERY_ORGANIZATIONS = "query-organizations";

    public static final String IMPERSONATION = "impersonation";

    public static final String[] ALL_REALM_ROLES = {CREATE_CLIENT, VIEW_REALM, VIEW_USERS, VIEW_CLIENTS, VIEW_EVENTS, VIEW_IDENTITY_PROVIDERS, VIEW_AUTHORIZATION, VIEW_ORGANIZATIONS, MANAGE_REALM, MANAGE_USERS, MANAGE_CLIENTS, MANAGE_EVENTS, MANAGE_IDENTITY_PROVIDERS, MANAGE_AUTHORIZATION, MANAGE_ORGANIZATIONS, QUERY_USERS, QUERY_CLIENTS, QUERY_REALMS, QUERY_GROUPS, QUERY_ORGANIZATIONS};
    public static final String[] ALL_QUERY_ROLES = {QUERY_USERS, QUERY_CLIENTS, QUERY_REALMS, QUERY_GROUPS, QUERY_ORGANIZATIONS};

    public static final Set<String> REALM_LEVEL_ROLES = Set.of(ADMIN, CREATE_REALM);
    public static final Set<String> ALL_ROLES;
    public static final Set<String> REALM_MANAGEMENT_ROLES;

    static {
        Set<String> allRoles = new HashSet<>(Arrays.asList(ALL_REALM_ROLES));
        allRoles.addAll(REALM_LEVEL_ROLES);
        allRoles.add(IMPERSONATION);
        allRoles.add(REALM_ADMIN);
        ALL_ROLES = Collections.unmodifiableSet(allRoles);

        Set<String> realmManagementRoles = new HashSet<>(Set.of(ALL_REALM_ROLES));
        realmManagementRoles.addAll(Set.of(IMPERSONATION, REALM_ADMIN));
        REALM_MANAGEMENT_ROLES = Collections.unmodifiableSet(realmManagementRoles);
    }

    public static boolean isAdminRole(RoleModel role) {
        if (role == null) {
            return false;
        }

        if (!ALL_ROLES.contains(role.getName())) {
            return false;
        }

        RoleContainerModel container = role.getContainer();

        if (container instanceof RealmModel r) {
            return isAdminRealm(r.getName());
        }

        if (container instanceof ClientModel c) {
            return isAdminClient(c.getRealm(), c.getClientId());
        }

        return false;
    }

    public static boolean isAdminRealm(String realmName) {
        return Config.getAdminRealm().equals(realmName);
    }

    public static boolean isAdminClient(RealmModel realm, String clientId) {
        if (Constants.REALM_MANAGEMENT_CLIENT_ID.equals(clientId)) {
            return true;
        }
        return isAdminRealm(realm.getName()) && clientId.endsWith(APP_SUFFIX);
    }

    public static boolean isAdminRoleOrComposite(RoleModel role) {
        return isAdminRole(role, new HashSet<>());
    }

    public static boolean groupHasAdminRoles(GroupModel group) {
        GroupModel current = group;
        while (current != null) {
            if (current.getRoleMappingsStream().anyMatch(AdminRoles::isAdminRoleOrComposite)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private static boolean isAdminRole(RoleModel role, Set<String> visited) {
        if (!visited.add(role.getId())) {
            return false;
        }
        if (isAdminRole(role)) {
            return true;
        }
        if (!role.isComposite()) {
            return false;
        }
        return role.getCompositesStream().anyMatch(child -> isAdminRole(child, visited));
    }

    public static boolean containsAdminRole(RoleModel role) {
        return containsAdminRole(role, new HashSet<>());
    }

    private static boolean containsAdminRole(RoleModel role, Set<String> visited) {
        if (isAdminRole(role)) {
            return true;
        }
        if (role == null || !role.isComposite() || !visited.add(role.getId())) {
            return false;
        }
        return role.getCompositesStream().anyMatch(r -> containsAdminRole(r, visited));
    }
}
