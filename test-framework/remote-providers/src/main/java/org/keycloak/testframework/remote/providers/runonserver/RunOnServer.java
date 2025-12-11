package org.keycloak.testframework.remote.providers.runonserver;

import java.io.IOException;
import java.io.Serializable;

import org.keycloak.common.VerificationException;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface RunOnServer extends Serializable {

    void run(KeycloakSession session) throws IOException, VerificationException;

}
