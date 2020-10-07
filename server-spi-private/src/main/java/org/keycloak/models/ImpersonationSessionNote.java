package org.keycloak.models;

/**
 * Session note metadata for impersonation details stored in user session notes.
 */
public enum ImpersonationSessionNote implements UserSessionNoteDescriptor {
    IMPERSONATOR_ID("Impersonator User ID"),
    IMPERSONATOR_USERNAME("Impersonator Username");

    final String displayName;

    ImpersonationSessionNote(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTokenClaim() {
        return this.toString().toLowerCase().replace('_', '.');
    }
}
