package org.keycloak.exportimport.util;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class ExportOptions {

    private boolean usersIncluded = true;
    private boolean clientsIncluded = true;
    private boolean groupsAndRolesIncluded = true;

    public ExportOptions() {
    }

    public ExportOptions(boolean users, boolean clients, boolean groupsAndRoles) {
        usersIncluded = users;
        clientsIncluded = clients;
        groupsAndRolesIncluded = groupsAndRoles;
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

    public void setUsersIncluded(boolean value) {
        usersIncluded = value;
    }

    public void setClientsIncluded(boolean value) {
        clientsIncluded = value;
    }

    public void setGroupsAndRolesIncluded(boolean value) {
        groupsAndRolesIncluded = value;
    }
}
