package org.keycloak.services.scheduled;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;

public class OpenIdFederationClientExpirationTask implements ScheduledTask {

    protected static final Logger logger = Logger.getLogger(OpenIdFederationClientExpirationTask.class);

    protected final String id;
    protected final String realmId;

    public OpenIdFederationClientExpirationTask(String id, String realmId) {
        this.id = id;
        this.realmId = realmId;
    }

    @Override
    public void run(KeycloakSession session) {
        logger.info(" OpenId Federation Client with id= " + id + " has expired.");
        RealmModel realm = session.realms().getRealm(realmId);
        if ( realm == null) {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.cancelTaskAndNotify("OpenIdFederationClientExpirationTask_" + id);
            return;
        }
        ClientModel client = session.clients().getClientById(realm, id);
        if (client == null || client.getAttribute(OIDCConfigAttributes.EXPIRATION_TIME) == null ) {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.cancelTaskAndNotify("OpenIdFederationClientExpirationTask_" + id);
        } else {
            client.setEnabled(false);
            client.updateClient();
        }
    }
}

