package org.keycloak.services.clientregistration;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.AppAuthManager;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientRegistrationService {

    private RealmModel realm;

    private EventBuilder event;

    @Context
    private KeycloakSession session;

    public ClientRegistrationService(RealmModel realm, EventBuilder event) {
        this.realm = realm;
        this.event = event;
    }

    @Path("{provider}")
    public Object getProvider(@PathParam("provider") String providerId) {
        ClientRegistrationProvider provider = session.getProvider(ClientRegistrationProvider.class, providerId);
        provider.setRealm(realm);
        provider.setEvent(event);
        return provider;
    }

}
