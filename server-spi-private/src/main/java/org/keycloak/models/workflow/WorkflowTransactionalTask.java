package org.keycloak.models.workflow;

import static org.keycloak.models.utils.KeycloakModelUtils.runJobInTransaction;

import java.util.Objects;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;

public abstract class WorkflowTransactionalTask implements Runnable, KeycloakSessionTask {

    private final KeycloakSessionFactory sessionFactory;
    private final KeycloakContext context;

    public WorkflowTransactionalTask(KeycloakSession session) {
        Objects.requireNonNull(session, "KeycloakSession must not be null");
        this.sessionFactory = session.getKeycloakSessionFactory();
        this.context = session.getContext();
    }

    @Override
    public void run() {
        runJobInTransaction(sessionFactory, context, this);
    }
}
