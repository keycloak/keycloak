package org.keycloak.testframework.remote.runonserver;

import org.keycloak.models.KeycloakSession;

/**
 * @deprecated Bridge class for legacy Arquillian testsuite.
 * This version will be removed when the legacy testsuite is fully migrated.
 */
@Deprecated
public interface RunOnServer {

    void run(KeycloakSession session);
}
