package org.keycloak.testframework.remote.providers.runonserver;

import org.keycloak.common.VerificationException;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;
import java.io.Serializable;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface RunOnServer extends Serializable {

    void run(KeycloakSession session) throws IOException, VerificationException;

}
