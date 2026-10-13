package org.keycloak.credential;

/**
 * The outcome of a credential update performed by a {@link CredentialInputUpdater}.
 *
 * @param handled whether the updater handled the input
 * @param storedCredential the credential written to the Keycloak credential store, or {@code null} if the updater
 * wrote it elsewhere or has no data about it
 */
public record CredentialUpdate(boolean handled, CredentialModel storedCredential) {

    public static final CredentialUpdate NOT_HANDLED = new CredentialUpdate(false, null);
}
