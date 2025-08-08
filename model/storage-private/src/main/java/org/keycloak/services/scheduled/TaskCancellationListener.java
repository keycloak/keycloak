package org.keycloak.services.scheduled;

import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.timer.TimerProvider;

public class TaskCancellationListener implements ClusterListener {

    private final KeycloakSessionFactory sessionFactory;

    public TaskCancellationListener(KeycloakSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void eventReceived(ClusterEvent event) {
        final TaskCancellationEvent cancellationEvent = (TaskCancellationEvent) event;
        try (KeycloakSession session = sessionFactory.create()) {
            TimerProvider timerProvider = session.getProvider(TimerProvider.class);
            timerProvider.cancelTask(cancellationEvent.getTaskName());
        }

    }
}
