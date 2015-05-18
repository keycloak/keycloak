package org.keycloak.events;

import org.keycloak.events.admin.AdminEvent;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface EventListenerProvider extends Provider {

    public void onEvent(Event event);

    public void onEvent(AdminEvent event, boolean includeRepresentation);

}
