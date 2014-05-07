package org.keycloak.services.scheduled;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderSession;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface ScheduledTask {

    public void run(KeycloakSession keycloakSession, ProviderSession providerSession);

}
