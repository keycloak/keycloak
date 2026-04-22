package org.keycloak.testframework.remote.runonserver;

import org.keycloak.models.KeycloakSession;

/**
 * @deprecated transition class to ease test migration to the new test framework
 */
@Deprecated
public interface RunOnServer {

    void run(KeycloakSession session);
}
