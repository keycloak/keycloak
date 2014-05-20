package org.keycloak.audit.email;

import org.keycloak.Config;
import org.keycloak.audit.AuditListener;
import org.keycloak.audit.AuditListenerFactory;
import org.keycloak.audit.EventType;
import org.keycloak.email.EmailProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderSession;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EmailAuditListenerFactory implements AuditListenerFactory {

    private static final Set<EventType> SUPPORTED_EVENTS = new HashSet<EventType>();
    static {
        Collections.addAll(SUPPORTED_EVENTS, EventType.LOGIN_ERROR, EventType.UPDATE_PASSWORD, EventType.REMOVE_TOTP, EventType.UPDATE_TOTP);
    }

    private Set<EventType> includedEvents = new HashSet<EventType>();

    @Override
    public AuditListener create(ProviderSession providerSession) {
        KeycloakSession keycloakSession = providerSession.getProvider(KeycloakSession.class);
        EmailProvider emailProvider = providerSession.getProvider(EmailProvider.class);
        return new EmailAuditListener(keycloakSession, emailProvider, includedEvents);
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
    public void close() {
    }

    @Override
    public String getId() {
        return "email";
    }

}
