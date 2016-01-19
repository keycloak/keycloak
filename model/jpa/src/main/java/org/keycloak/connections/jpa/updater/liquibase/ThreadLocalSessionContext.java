package org.keycloak.connections.jpa.updater.liquibase;

import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ThreadLocalSessionContext {

    private static final ThreadLocal<KeycloakSession> currentSession = new ThreadLocal<KeycloakSession>();

    public static KeycloakSession getCurrentSession() {
        return currentSession.get();
    }

    public static void setCurrentSession(KeycloakSession session) {
        currentSession.set(session);
    }

    public static void removeCurrentSession() {
        currentSession.remove();
    }
}
