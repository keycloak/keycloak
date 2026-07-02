package org.keycloak.admin.api;

public enum SortOrder {
    ASC,
    DESC;

    public boolean isAscending() {
        return this == ASC;
    }
}
