/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.exportimport;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class ExportOptions {

    private boolean usersIncluded = true;
    private boolean clientsIncluded = true;
    private boolean groupsAndRolesIncluded = true;
    private boolean onlyServiceAccountsIncluded = false;
    private boolean partial;

    public ExportOptions() {
    }

    public ExportOptions(boolean users, boolean clients, boolean groupsAndRoles, boolean onlyServiceAccounts, boolean partial) {
        usersIncluded = users;
        clientsIncluded = clients;
        groupsAndRolesIncluded = groupsAndRoles;
        onlyServiceAccountsIncluded = onlyServiceAccounts;
        this.partial = partial;
    }

    public boolean isUsersIncluded() {
        return usersIncluded;
    }

    public boolean isClientsIncluded() {
        return clientsIncluded;
    }

    public boolean isGroupsAndRolesIncluded() {
        return groupsAndRolesIncluded;
    }

    public boolean isOnlyServiceAccountsIncluded() {
        return onlyServiceAccountsIncluded;
    }

    public void setUsersIncluded(boolean value) {
        usersIncluded = value;
    }

    public void setClientsIncluded(boolean value) {
        clientsIncluded = value;
    }

    public void setGroupsAndRolesIncluded(boolean value) {
        groupsAndRolesIncluded = value;
    }

    public void setOnlyServiceAccountsIncluded(boolean value) {
        onlyServiceAccountsIncluded = value;
    }

    public boolean isPartial() {
        return partial;
    }
}
