package org.keycloak.services.clientregistration;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OIDCClientRegistrationProvider implements ClientRegistrationProvider {

    private KeycloakSession session;
    private RealmModel realm;
    private EventBuilder event;

    public OIDCClientRegistrationProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void close() {
    }

    @Override
    public void setRealm(RealmModel realm) {
        this.realm = realm;
    }

    @Override
    public void setEvent(EventBuilder event) {
        this.event = event;
    }

}
