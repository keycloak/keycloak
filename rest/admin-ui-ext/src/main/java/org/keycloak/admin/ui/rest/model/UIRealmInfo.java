package org.keycloak.admin.ui.rest.model;

/**
 * Information about a realm, which is available for each admin of a realm, not only for admins allowed to view the realm.
 */
public class UIRealmInfo {
    private boolean userProfileProvidersEnabled;

    public boolean isUserProfileProvidersEnabled() {
        return userProfileProvidersEnabled;
    }

    public void setUserProfileProvidersEnabled(final boolean userProfileProvidersEnabled) {
        this.userProfileProvidersEnabled = userProfileProvidersEnabled;
    }
}
