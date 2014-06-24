package org.keycloak.services.scheduled;

import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface ScheduledTask {

    public void run(KeycloakSession session);

}
