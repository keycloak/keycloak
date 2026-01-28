package org.keycloak.models;

/**
 * Describes a user session note for simple and generic {@link ProtocolMapperModel} creation.
 */
public interface UserSessionNoteDescriptor {
    /**
     * @return A human-readable name for the session note. This should tell the end user what the session note contains
     */
    String getDisplayName();

    /**
     * @return Token claim name/path to store the user session note value in.
     */
    String getTokenClaim();
}
