package org.keycloak.events.email;

import org.keycloak.Config;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EmailEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final Set<EventType> SUPPORTED_EVENTS = new HashSet<EventType>();
    static {
        Collections.addAll(SUPPORTED_EVENTS, EventType.LOGIN_ERROR, EventType.UPDATE_PASSWORD, EventType.REMOVE_TOTP, EventType.UPDATE_TOTP);
    }

    private Set<EventType> includedEvents = new HashSet<EventType>();

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        EmailTemplateProvider emailTemplateProvider = session.getProvider(EmailTemplateProvider.class);
        return new EmailEventListenerProvider(session, emailTemplateProvider, includedEvents);
    }

    @Override
    public void init(Config.Scope config) {
        String[] include = config.getArray("include-events");
        if (include != null) {
            for (String i : include) {
                includedEvents.add(EventType.valueOf(i.toUpperCase()));
            }
        } else {
            includedEvents.addAll(SUPPORTED_EVENTS);
        }

        String[] exclude = config.getArray("exclude-events");
        if (exclude != null) {
            for (String e : exclude) {
                includedEvents.remove(EventType.valueOf(e.toUpperCase()));
            }
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "email";
    }

}
