package org.keycloak.services.scheduled;

import java.time.Instant;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.ServicesLogger;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;

public class StartIdPScheduledTasks implements ScheduledTask {


    public StartIdPScheduledTasks() {
    }

    /**
     * fide autoupdated IdPs in storage and create scheduled tasks based on refreshPeriod and lastRefreshTime
     * @param session
     */
    @Override
    public void run(KeycloakSession session) {
        TimerProvider timer = session.getProvider(TimerProvider.class);
        session.realms().getRealmsStream().forEach(realm -> realm.getAutoUpdatedIdentityProvidersStream().forEach(idp -> {
                    AutoUpdateIdentityProviders autoUpdateProvider = new AutoUpdateIdentityProviders(idp.getAlias(), realm.getId());
                    ClusterAwareScheduledTaskRunner taskRunner = new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), autoUpdateProvider, Long.valueOf(idp.getConfig().get(IdentityProviderModel.REFRESH_PERIOD)) * 1000);
                    long delay = idp.getConfig().get(IdentityProviderModel.LAST_REFRESH_TIME) == null ? Long.parseLong(idp.getConfig().get(IdentityProviderModel.REFRESH_PERIOD)) * 1000 : Long.parseLong(idp.getConfig().get(IdentityProviderModel.LAST_REFRESH_TIME) + Long.parseLong(idp.getConfig().get(IdentityProviderModel.REFRESH_PERIOD)) * 1000) - Instant.now().toEpochMilli();
                    timer.schedule(taskRunner, delay < 0 ? 0 : delay, Long.valueOf(idp.getConfig().get(IdentityProviderModel.REFRESH_PERIOD)) * 1000, realm.getId() + "_AutoUpdateIdP_" + idp.getAlias());
                })
        );

    }

}
