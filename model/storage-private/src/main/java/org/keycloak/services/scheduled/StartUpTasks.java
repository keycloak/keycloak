package org.keycloak.services.scheduled;

import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;

public class StartUpTasks implements ScheduledTask {

    @Override
    public void run(KeycloakSession session) {
        TimerProvider timer = session.getProvider(TimerProvider.class);
        session.realms().getRealmsStream().forEach(realm -> {
            session.clients().getClientsStream(realm).forEach(clientModel -> {
                if (clientModel.getAttributes().get(OIDCConfigAttributes.EXPIRATION_TIME) != null && clientModel.isEnabled()) {
                    OpenIdFederationClientExpirationTask federationTask = new OpenIdFederationClientExpirationTask(clientModel.getId(), realm.getId());
                    long expiration = Long.valueOf(clientModel.getAttribute(OIDCConfigAttributes.EXPIRATION_TIME)) * 1000 - Time.currentTimeMillis();
                    ClusterAwareScheduledTaskRunner taskRunner = new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), federationTask, expiration > 60 * 1000 ? expiration : 60 * 1000);
                    timer.scheduleOnce(taskRunner, expiration > 60 * 1000 ? expiration : 60 * 1000, "OpenidFederationExplicitClient_" + clientModel.getId());
                }
            });
        });
    }
}
