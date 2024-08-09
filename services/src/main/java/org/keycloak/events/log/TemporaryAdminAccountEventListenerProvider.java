package org.keycloak.events.log;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;

import static org.keycloak.models.Constants.TEMP_ADMIN_ATTR_NAME;

public class TemporaryAdminAccountEventListenerProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(TemporaryAdminAccountEventListenerProvider.class);

    private final KeycloakSession session;
    private final RealmProvider realmModel;

    public TemporaryAdminAccountEventListenerProvider(KeycloakSession session) {
        this.session = session;
        this.realmModel = session.realms();
    }

    @Override
    public void onEvent(Event event) {
        RealmModel realm = this.realmModel.getRealm(event.getRealmId());
        UserModel user = this.session.users().getUserById(realm, event.getUserId());
        ClientModel client = this.session.clients().getClientByClientId(realm, event.getClientId());

        if (EventType.LOGIN.equals(event.getType()) && Boolean.parseBoolean(user.getFirstAttribute(TEMP_ADMIN_ATTR_NAME))) {
            log.warn(user.getUsername() + " is a temporary admin user account. To harden security, create a permanent account and delete the temporary one.");
        }

        if (EventType.CLIENT_LOGIN.equals(event.getType()) && Boolean.parseBoolean(client.getAttribute(TEMP_ADMIN_ATTR_NAME))) {
            log.warn(client.getClientId() + " is a temporary admin service account. To harden security, create a permanent account and delete the temporary one.");
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
    }

    @Override
    public void close() {
    }

}
