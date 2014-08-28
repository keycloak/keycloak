package org.keycloak.examples.providers.events;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SysoutEventListenerProviderFactory implements EventListenerProviderFactory {

    private Set<EventType> excludedEvents;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new SysoutEventListenerProvider(excludedEvents);
    }

    @Override
    public void init(Config.Scope config) {
        String excludes = config.get("excludes");
        if (excludes != null) {
            excludedEvents = new HashSet<EventType>();
            for (String e : excludes.split(",")) {
                excludedEvents.add(EventType.valueOf(e));
            }
        }
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "sysout";
    }

}
