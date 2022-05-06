package org.keycloak.models;

import org.keycloak.provider.Provider;

/**
 * Support for elements in Keycloak's session that are deprecated.
 * This allows the deprecated implementations to be moved to the legacy module.
 *
 * @author Alexander Schwartz
 */
public interface LegacySessionSupportProvider extends Provider {

    @Deprecated
    UserCredentialManager userCredentialManager();
}
