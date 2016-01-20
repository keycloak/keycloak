package org.keycloak.services.scheduled;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.ServicesLogger;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ScheduledTaskRunner implements Runnable {

    private static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    private final KeycloakSessionFactory sessionFactory;
    private final ScheduledTask task;

    public ScheduledTaskRunner(KeycloakSessionFactory sessionFactory, ScheduledTask task) {
        this.sessionFactory = sessionFactory;
        this.task = task;
    }

    @Override
    public void run() {
        KeycloakSession session = sessionFactory.create();
        try {
            session.getTransaction().begin();
            task.run(session);
            session.getTransaction().commit();

            logger.debug("Executed scheduled task " + task.getClass().getSimpleName());
        } catch (Throwable t) {
            logger.failedToRunScheduledTask(t, task.getClass().getSimpleName());

            session.getTransaction().rollback();
        } finally {
            try {
                session.close();
            } catch (Throwable t) {
                logger.failedToCloseProviderSession(t);
            }
        }
    }

}
