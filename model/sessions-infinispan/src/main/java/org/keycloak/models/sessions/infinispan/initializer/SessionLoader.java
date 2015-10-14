package org.keycloak.models.sessions.infinispan.initializer;

import java.io.Serializable;

import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface SessionLoader extends Serializable {

    int getSessionsCount(KeycloakSession session);

    boolean loadSessions(KeycloakSession session, int first, int max);
}
